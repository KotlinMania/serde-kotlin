package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CollectSeqOrderParityTest {
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
        val complete = listOf("yield:1", "element", "value:1", "yield:2", "element", "value:2", "eof", "end")
        val one = listOf("yield:1", "element", "value:1", "eof", "end")

        fun expected(known: Boolean, len: Int, tail: List<String>): List<String> =
            listOf("factory", "begin:${if (known) len.toString() else "?"}") + tail
    }

    private class Fixture(
        private val values: List<Int>,
        val mode: Mode,
        private val known: Boolean,
    ) {
        val events = mutableListOf<String>()
        private var hints = 0
        val beginError = SerdeError("begin error")
        val elementError = SerdeError("element error")
        val endError = SerdeError("end error")

        fun hint(): Int {
            hints++
            if (mode == Mode.HINT_PANIC) error("hint panic")
            return values.size
        }

        fun iterator(): Iterator<Item> {
            events.add("factory")
            if (mode == Mode.FACTORY_PANIC) error("factory panic")
            return object : Iterator<Item> {
                private var index = 0

                override fun hasNext(): Boolean {
                    val more = index < values.size
                    if (!more) events.add("eof")
                    return more
                }

                override fun next(): Item {
                    if (!hasNext()) throw NoSuchElementException()
                    if (mode == Mode.NEXT_PANIC) {
                        events.add("next panic")
                        error("next panic")
                    }
                    val value = values[index++]
                    events.add("yield:$value")
                    return Item(value)
                }
            }
        }

        fun run(): SerdeResult<Unit> {
            val serializer = Probe(this)
            return if (known) serializer.collectSeq(SizedInput(this)) else serializer.collectSeq(Sequence { iterator() })
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
    ) : AbstractCollection<Item>() {
        override val size: Int get() = fixture.hint()

        override fun iterator(): Iterator<Item> = fixture.iterator()
    }

    private class Probe(
        private val fixture: Fixture,
    ) : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
        override fun <T> collectSeq(iter: Iterable<T>): SerdeResult<Unit> where T : Serialize =
            super<Serializer>.collectSeq(iter)

        override fun <T> collectSeq(iter: Sequence<T>): SerdeResult<Unit> where T : Serialize =
            super<Serializer>.collectSeq(iter)

        override fun serializeI32(v: Int): SerdeResult<Unit> {
            fixture.events.add("value:$v")
            return when (v) {
                -1 -> SerdeResult.failure(fixture.elementError)
                -2 -> error("element panic")
                else -> SerdeResult.success(Unit)
            }
        }

        override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> {
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
    ) : SerializeSeq<Unit> {
        override fun <T> serializeElement(value: T): SerdeResult<Unit> where T : Serialize {
            fixture.events.add("element")
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
            val fixture = Fixture(listOf(1, 2), Mode.NORMAL, known)
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
            val fixture = Fixture(listOf(1, 2), Mode.FACTORY_PANIC, known)
            assertEquals("factory panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verifyEarly(0)
        }
        val fixture = Fixture(listOf(1, 2), Mode.HINT_PANIC, true)
        assertEquals("hint panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
        fixture.verifyEarly(1)
    }

    @Test
    fun beginErrorCreatesIteratorWithoutReading() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1, 2), Mode.BEGIN_ERROR, known)
            assertSame(fixture.beginError, fixture.run().exceptionOrNull())
            fixture.verify(2, emptyList())
        }
    }

    @Test
    fun beginPanicCreatesIteratorWithoutReading() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1, 2), Mode.BEGIN_PANIC, known)
            assertEquals("begin panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verify(2, emptyList())
        }
    }

    @Test
    fun nextPanicSkipsElementsAndEnd() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1, 2), Mode.NEXT_PANIC, known)
            assertEquals("next panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verify(2, listOf("next panic"))
        }
    }

    @Test
    fun elementErrorStopsSuffixAndEnd() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(-1, 2), Mode.NORMAL, known)
            assertSame(fixture.elementError, fixture.run().exceptionOrNull())
            fixture.verify(2, listOf("yield:-1", "element", "value:-1"))
        }
    }

    @Test
    fun elementPanicStopsSuffixAndEnd() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(-2, 2), Mode.NORMAL, known)
            assertEquals("element panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verify(2, listOf("yield:-2", "element", "value:-2"))
        }
    }

    @Test
    fun endErrorIsReturned() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1), Mode.END_ERROR, known)
            assertSame(fixture.endError, fixture.run().exceptionOrNull())
            fixture.verify(1, Trace.one)
        }
    }

    @Test
    fun endPanicPropagates() {
        for (known in listOf(true, false)) {
            val fixture = Fixture(listOf(1), Mode.END_PANIC, known)
            assertEquals("end panic", assertFailsWith<IllegalStateException> { fixture.run() }.message)
            fixture.verify(1, Trace.one)
        }
    }
}
