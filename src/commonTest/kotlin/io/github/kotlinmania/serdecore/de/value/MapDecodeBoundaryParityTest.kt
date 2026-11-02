package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.mutableMapDeserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MapDecodeBoundaryParityTest {
    private class Codec(
        private val label: String,
        private val trace: MutableList<String>,
    ) : Deserialize<Int> {
        val typedError = SerdeError("$label error")

        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            I32Deserialize.deserialize(deserializer).flatMap { value ->
                trace.add("$label:$value")
                when (value) {
                    -2 -> error("$label panic")
                    -1 -> SerdeResult.failure(typedError)
                    else -> SerdeResult.success(value)
                }
            }
    }

    private class Access(
        input: List<Pair<Int, Int>>,
        private val mode: Int,
    ) : MapAccess {
        private val inner =
            mapDeserializer(input.map { MapEntry(it.first.intoDeserializer(), it.second.intoDeserializer()) }.iterator())
        private var remaining = input.size
        var keys = 0
        var values = 0
        var hints = 0

        override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> {
            keys++
            if (mode == 2) error("key access panic")
            if (remaining > 0) remaining--
            return inner.nextKeySeed(seed)
        }

        override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
            values++
            if (mode == 3) error("value access panic")
            return inner.nextValueSeed(seed)
        }

        override fun sizeHint(): Int {
            hints++
            if (mode == 1) error("hint panic")
            return remaining
        }
    }

    private class Driver(
        private val access: Access,
    ) : Deserializer by I32Deserializer.new(0) {
        override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = visitor.visitMap(access)
    }

    private class Fixture(
        input: List<Pair<Int, Int>>,
        mode: Int = 0,
    ) {
        private val trace = mutableListOf<String>()
        val keyCodec = Codec("key", trace)
        val valueCodec = Codec("value", trace)
        private val access = Access(input, mode)

        fun decode(): SerdeResult<MutableMap<Int, Int>> =
            mutableMapDeserialize(keyCodec, valueCodec).deserialize(Driver(access))

        fun assertObservations(
            keys: Int,
            values: Int,
            hints: Int,
            trace: List<String>,
        ) {
            assertEquals(keys, access.keys)
            assertEquals(values, access.values)
            assertEquals(hints, access.hints)
            assertEquals(trace, this.trace)
        }
    }

    @Test
    fun successPreservesEntries() {
        val fixture = Fixture(listOf(1 to 10, 2 to 20))
        assertEquals(mapOf(1 to 10, 2 to 20), fixture.decode().getOrThrow())
        fixture.assertObservations(3, 2, 1, listOf("key:1", "value:10", "key:2", "value:20"))
    }

    @Test
    fun duplicateKeyKeepsLastValue() {
        val fixture = Fixture(listOf(1 to 10, 1 to 20))
        assertEquals(mapOf(1 to 20), fixture.decode().getOrThrow())
        fixture.assertObservations(3, 2, 1, listOf("key:1", "value:10", "key:1", "value:20"))
    }

    @Test
    fun emptySucceeds() {
        val fixture = Fixture(emptyList())
        assertEquals(emptyMap(), fixture.decode().getOrThrow())
        fixture.assertObservations(1, 0, 1, emptyList())
    }

    @Test
    fun hintPanicPrecedesAccess() {
        val fixture = Fixture(listOf(1 to 10), mode = 1)
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("hint panic", failure.message)
        fixture.assertObservations(0, 0, 1, emptyList())
    }

    @Test
    fun keyAccessPanicSkipsValue() {
        val fixture = Fixture(listOf(1 to 10), mode = 2)
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("key access panic", failure.message)
        fixture.assertObservations(1, 0, 1, emptyList())
    }

    @Test
    fun valueAccessPanicFollowsKey() {
        val fixture = Fixture(listOf(1 to 10), mode = 3)
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("value access panic", failure.message)
        fixture.assertObservations(1, 1, 1, listOf("key:1"))
    }

    @Test
    fun keyCodecPanicSkipsValue() {
        val fixture = Fixture(listOf(-2 to 10, 2 to 20))
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("key panic", failure.message)
        fixture.assertObservations(1, 0, 1, listOf("key:-2"))
    }

    @Test
    fun valueCodecPanicStopsSuffix() {
        val fixture = Fixture(listOf(1 to -2, 2 to 20))
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("value panic", failure.message)
        fixture.assertObservations(1, 1, 1, listOf("key:1", "value:-2"))
    }

    @Test
    fun typedKeyErrorSkipsValue() {
        val fixture = Fixture(listOf(-1 to 10, 2 to 20))
        assertSame(fixture.keyCodec.typedError, fixture.decode().exceptionOrNull())
        fixture.assertObservations(1, 0, 1, listOf("key:-1"))
    }

    @Test
    fun typedValueErrorStopsSuffix() {
        val fixture = Fixture(listOf(1 to -1, 2 to 20))
        assertSame(fixture.valueCodec.typedError, fixture.decode().exceptionOrNull())
        fixture.assertObservations(1, 1, 1, listOf("key:1", "value:-1"))
    }
}
