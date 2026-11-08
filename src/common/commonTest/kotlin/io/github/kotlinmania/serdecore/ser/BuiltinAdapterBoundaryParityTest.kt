package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.RangeFromValue
import io.github.kotlinmania.serdecore.de.RangeInclusiveValue
import io.github.kotlinmania.serdecore.de.RangeToValue
import io.github.kotlinmania.serdecore.de.RangeValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class BuiltinAdapterBoundaryParityTest {
    private class Probe(
        private val beginPanic: Throwable? = null,
        private val callbackPanic: Throwable? = null,
        private val returnedFailure: SerdeResult<Nothing>? = null,
    ) : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
        val events = mutableListOf<String>()
        private val compound = object : SerializeTuple<Unit>, SerializeStruct<Unit> {
            override fun <T> serializeElement(value: T): SerdeResult<Unit> where T : Serialize = element()
            override fun <T> serializeField(key: String, value: T): SerdeResult<Unit> where T : Serialize = element()
            private fun element(): SerdeResult<Unit> {
                events.add("element")
                callbackPanic?.let { throw it }
                return returnedFailure ?: SerdeResult.success(Unit)
            }
            override fun end(): SerdeResult<Unit> {
                events.add("end")
                return SerdeResult.success(Unit)
            }
        }
        override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> {
            events.add("begin")
            beginPanic?.let { throw it }
            return SerdeResult.success(compound)
        }
        override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Unit>> {
            events.add("begin")
            beginPanic?.let { throw it }
            return SerdeResult.success(compound)
        }
    }

    private fun adapters(): List<(Serializer<Unit>) -> SerdeResult<Unit>> = listOf(
        { arrayOf(1, 2).serialize(it) },
        { Pair(1, 2).serialize(it) },
        { Triple(1, 2, 3).serialize(it) },
        { RangeValue(1, 2).serialize(it) },
        { RangeFromValue(1).serialize(it) },
        { RangeInclusiveValue(1, 2).serialize(it) },
        { RangeToValue(2).serialize(it) },
        { 1.seconds.serialize(it) },
        { Instant.fromEpochSeconds(1).serialize(it) },
    )

    @Test
    fun compoundAdaptersPropagateBeginAndCallbackPanicsUnchanged() {
        for (adapter in adapters()) {
            val begin = IllegalStateException("begin panic")
            assertSame(begin, assertFailsWith<IllegalStateException> { adapter(Probe(beginPanic = begin)) })
            // Even this exception type is a thrown callback, not a returned Serde failure.
            val callback = SerdeException(SerdeError.custom("callback panic"))
            val probe = Probe(callbackPanic = callback)
            assertSame(callback, assertFailsWith<SerdeException> { adapter(probe) })
            assertEquals(listOf("begin", "element"), probe.events)
        }
    }

    @Test
    fun returnedFailureKeepsIdentityAndSkipsSuffixAndEnd() {
        for (adapter in adapters()) {
            val failure = SerdeResult.failure(SerdeError.custom("element error"))
            val probe = Probe(returnedFailure = failure)
            assertSame(failure, adapter(probe))
            assertEquals(listOf("begin", "element"), probe.events)
        }
    }

    @Test
    fun iterableAdapterDoesNotReadItemsBeforeBeginFailure() {
        val events = mutableListOf<String>()
        val failure = SerdeResult.failure(SerdeError.custom("begin error"))
        val input = Iterable {
            events.add("factory")
            object : Iterator<Int> {
                override fun hasNext(): Boolean = error("must not read before begin")
                override fun next(): Int = error("must not consume before begin")
            }
        }
        val probe = object : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
            override fun <T> collectSeq(iter: Iterable<T>): SerdeResult<Unit> where T : Serialize = super<Serializer>.collectSeq(iter)
            override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> {
                events.add("begin:$len")
                return failure
            }
        }
        assertSame(failure, input.serialize(probe))
        assertEquals(listOf("factory", "begin:null"), events)
    }

    @Test
    fun mapAdapterDoesNotReadEntriesBeforeBeginFailure() {
        val events = mutableListOf<String>()
        val failure = SerdeResult.failure(SerdeError.custom("begin error"))
        val input = object : AbstractMap<Int, Int>() {
            override val entries: Set<Map.Entry<Int, Int>> = object : AbstractSet<Map.Entry<Int, Int>>() {
                override val size: Int get() { events.add("hint"); return 2 }
                override fun iterator(): Iterator<Map.Entry<Int, Int>> {
                    events.add("factory")
                    return object : Iterator<Map.Entry<Int, Int>> {
                        override fun hasNext(): Boolean = error("must not read before begin")
                        override fun next(): Map.Entry<Int, Int> = error("must not consume before begin")
                    }
                }
            }
        }
        val probe = object : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
            override fun <K, V> collectMap(iter: Iterable<Pair<K, V>>): SerdeResult<Unit> where K : Serialize, V : Serialize = super<Serializer>.collectMap(iter)
            override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> {
                events.add("begin:$len")
                return failure
            }
        }
        assertSame(failure, input.serialize(probe))
        assertEquals(listOf("factory", "hint", "begin:2"), events)
    }
}
