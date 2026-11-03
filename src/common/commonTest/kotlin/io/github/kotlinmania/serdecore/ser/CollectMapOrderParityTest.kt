package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CollectMapOrderParityTest {
    private enum class Mode {
        NORMAL,
        FACTORY_PANIC,
        HINT_PANIC,
        BEGIN_ERROR,
        BEGIN_PANIC,
        NEXT_PANIC,
        END_ERROR,
        END_PANIC,
    }

    private data class Item(
        val value: Int,
    ) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI32(value)
    }

    private object Trace {
        val complete =
            listOf(
                "yield:1:10",
                "key",
                "i32:1",
                "value",
                "i32:10",
                "yield:2:20",
                "key",
                "i32:2",
                "value",
                "i32:20",
                "eof",
                "end",
            )
        val one = listOf("yield:1:10", "key", "i32:1", "value", "i32:10", "eof", "end")

        fun expected(known: Boolean, len: Int, tail: List<String>): List<String> =
            listOf("factory", "begin:${if (known) len.toString() else "?"}") + tail
    }

    private class Fixture(
        private val values: List<Pair<Int, Int>>,
        val mode: Mode,
        private val known: Boolean,
    ) {
        val events = mutableListOf<String>()
        private var hints = 0
        val beginError = SerdeError("begin error")
        val itemError = SerdeError("item error")
        val endError = SerdeError("end error")

        fun hint(): Int {
            hints++
            if (mode == Mode.HINT_PANIC) error("hint panic")
            return values.size
        }

        fun iterator(): Iterator<Pair<Item, Item>> {
            events.add("factory")
            if (mode == Mode.FACTORY_PANIC) error("factory panic")
            return object : Iterator<Pair<Item, Item>> {
                private var index = 0

                override fun hasNext(): Boolean {
                    val more = index < values.size
                    if (!more) events.add("eof")
                    return more
                }

                override fun next(): Pair<Item, Item> {
                    if (!hasNext()) throw NoSuchElementException()
                    if (mode == Mode.NEXT_PANIC) {
                        events.add("next panic")
                        error("next panic")
                    }
                    val value = values[index++]
                    events.add("yield:${value.first}:${value.second}")
                    return Item(value.first) to Item(value.second)
                }
            }
        }

        fun run(): SerdeResult<Unit> {
            val serializer = Probe(this)
            return if (known) serializer.collectMap(SizedInput(this)) else serializer.collectMap(Sequence { iterator() })
        }

        fun verify(len: Int, tail: List<String>) {
            assertEquals(Trace.expected(known, len, tail), events)
            assertEquals(if (known) 1 else 0, hints)
        }

        fun verifyEarly(expectedHints: Int) {
            assertEquals(listOf("factory"), events)
            assertEquals(expectedHints, hints)
        }
    }

    private class SizedInput(
        private val fixture: Fixture,
    ) : AbstractCollection<Pair<Item, Item>>() {
        override val size: Int get() = fixture.hint()

        override fun iterator(): Iterator<Pair<Item, Item>> = fixture.iterator()
    }

    private class Probe(
        private val fixture: Fixture,
    ) : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
        override fun <K, V> collectMap(iter: Iterable<Pair<K, V>>): SerdeResult<Unit>
            where K : Serialize,
                  V : Serialize =
            super<Serializer>.collectMap(iter)

        override fun <K, V> collectMap(iter: Sequence<Pair<K, V>>): SerdeResult<Unit>
            where K : Serialize,
                  V : Serialize =
            super<Serializer>.collectMap(iter)

        override fun serializeI32(v: Int): SerdeResult<Unit> {
            fixture.events.add("i32:$v")
            return when (v) {
                -1 -> SerdeResult.failure(fixture.itemError)
                -2 -> error("item panic")
                else -> SerdeResult.success(Unit)
            }
        }

        override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> {
            fixture.events.add("begin:${len ?: "?"}")
            return when (fixture.mode) {
                Mode.BEGIN_ERROR -> SerdeResult.failure(fixture.beginError)
                Mode.BEGIN_PANIC -> error("begin panic")
                else -> SerdeResult.success(Compound(fixture, this))
            }
        }
    }

    private class Compound(
        private val fixture: Fixture,
        private val probe: Probe,
    ) : SerializeMap<Unit> {
        override fun <T> serializeKey(key: T): SerdeResult<Unit> where T : Serialize {
            fixture.events.add("key")
            return key.serialize(probe)
        }

        override fun <T> serializeValue(value: T): SerdeResult<Unit> where T : Serialize {
            fixture.events.add("value")
            return value.serialize(probe)
        }

        override fun end(): SerdeResult<Unit> {
            fixture.events.add("end")
            return when (fixture.mode) {
                Mode.END_ERROR -> SerdeResult.failure(fixture.endError)
                Mode.END_PANIC -> error("end panic")
                else -> SerdeResult.success(Unit)
            }
        }
    }

    @Test
    fun successOrder() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1 to 10, 2 to 20), Mode.NORMAL, known)
            assertEquals(Unit, fixture.run().getOrThrow())
            fixture.verify(2, Trace.complete)
        }
    }

    @Test
    fun emptyOrder() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(emptyList(), Mode.NORMAL, known)
            assertEquals(Unit, fixture.run().getOrThrow())
            fixture.verify(0, listOf("eof", "end"))
        }
    }

    @Test
    fun factoryAndHintPanicsPrecedeBegin() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1 to 10, 2 to 20), Mode.FACTORY_PANIC, known)
            assertEquals("factory panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verifyEarly(0)
        }
        val fixture = Fixture(listOf(1 to 10, 2 to 20), Mode.HINT_PANIC, true)
        assertEquals("hint panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
        fixture.verifyEarly(1)
    }

    @Test
    fun beginErrorCreatesIteratorWithoutReading() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1 to 10, 2 to 20), Mode.BEGIN_ERROR, known)
            assertSame(fixture.beginError, fixture.run().exceptionOrNull())
            fixture.verify(2, emptyList())
        }
    }

    @Test
    fun beginPanicCreatesIteratorWithoutReading() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1 to 10, 2 to 20), Mode.BEGIN_PANIC, known)
            assertEquals("begin panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verify(2, emptyList())
        }
    }

    @Test
    fun nextPanicSkipsEntriesAndEnd() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1 to 10, 2 to 20), Mode.NEXT_PANIC, known)
            assertEquals("next panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verify(2, listOf("next panic"))
        }
    }

    @Test
    fun keyOrValueErrorStopsSuffixAndEnd() {
        for (known in listOf(true, false)) {
            for (key in listOf(true, false)) {
                val pair = if (key) -1 to 10 else 1 to -1
                val tail = if (key) listOf("yield:-1:10", "key", "i32:-1") else listOf("yield:1:-1", "key", "i32:1", "value", "i32:-1")
                val fixture = Fixture(listOf(pair, 2 to 20), Mode.NORMAL, known)
                assertSame(fixture.itemError, fixture.run().exceptionOrNull())
                fixture.verify(2, tail)
            }
        }
    }

    @Test
    fun keyOrValuePanicStopsSuffixAndEnd() {
        for (known in listOf(true, false)) {
            for (key in listOf(true, false)) {
                val pair = if (key) -2 to 10 else 1 to -2
                val tail = if (key) listOf("yield:-2:10", "key", "i32:-2") else listOf("yield:1:-2", "key", "i32:1", "value", "i32:-2")
                val fixture = Fixture(listOf(pair, 2 to 20), Mode.NORMAL, known)
                assertEquals("item panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
                fixture.verify(2, tail)
            }
        }
    }

    @Test
    fun endErrorIsReturned() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1 to 10), Mode.END_ERROR, known)
            assertSame(fixture.endError, fixture.run().exceptionOrNull())
            fixture.verify(1, Trace.one)
        }
    }

    @Test
    fun endPanicPropagates() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1 to 10), Mode.END_PANIC, known)
            assertEquals("end panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verify(1, Trace.one)
        }
    }
}
