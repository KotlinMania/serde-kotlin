package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IntoDeserializer
import io.github.kotlinmania.serdecore.de.MapAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MapEnumSeedParityTest {
    private object ReadSeed : DeserializeSeed<Int> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            I32Deserialize.deserialize(deserializer)
    }

    private object PanicSeed : DeserializeSeed<Int> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer = error("seed panic")
    }

    private class ErrorSeed : DeserializeSeed<Int> {
        val typedError = SerdeError("seed error")

        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            SerdeResult.failure(typedError)
    }

    private class Access(
        private val panic: Boolean,
    ) : MapAccess {
        val typedError = SerdeError("access error")

        override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> {
            if (panic) error("access panic")
            return SerdeResult.failure(typedError)
        }

        override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> = error("unexpected value")
    }

    private class PanicValue : IntoDeserializer {
        var calls = 0

        override fun intoDeserializer(): Deserializer {
            calls++
            error("conversion panic")
        }
    }

    private fun map(): MapDeserializer<I32Deserializer, I32Deserializer> =
        mapDeserializer(listOf(MapEntry(I32Deserializer.new(1), I32Deserializer.new(10))).iterator())

    @Test
    fun seedPanicRetainsPendingValue() {
        val map = map()
        val failure = assertFailsWith<IllegalStateException> { MapAccessDeserializer.new(map).variantSeed(PanicSeed) }
        assertEquals("seed panic", failure.message)
        assertEquals(10, map.nextValue(I32Deserialize).getOrThrow())
    }

    @Test
    fun accessPanicPropagates() {
        val failure = assertFailsWith<IllegalStateException> { MapAccessDeserializer.new(Access(true)).variantSeed(ReadSeed) }
        assertEquals("access panic", failure.message)
    }

    @Test
    fun keyConversionPanicRetainsPendingValue() {
        val key = PanicValue()
        val map = mapDeserializer(listOf(MapEntry(key, I32Deserializer.new(10))).iterator())
        val failure = assertFailsWith<IllegalStateException> { MapAccessDeserializer.new(map).variantSeed(ReadSeed) }
        assertEquals("conversion panic", failure.message)
        assertEquals(1, key.calls)
        assertEquals(10, map.nextValue(I32Deserialize).getOrThrow())
    }

    @Test
    fun typedSeedErrorRetainsPendingValue() {
        val map = map()
        val seed = ErrorSeed()
        assertSame(seed.typedError, MapAccessDeserializer.new(map).variantSeed(seed).exceptionOrNull())
        assertEquals(10, map.nextValue(I32Deserialize).getOrThrow())
    }

    @Test
    fun emptyMapRetainsTypeError() {
        val map = mapDeserializer(emptyList<MapEntry<I32Deserializer, I32Deserializer>>().iterator())
        assertEquals(
            "invalid type: map, expected enum",
            MapAccessDeserializer
                .new(map)
                .variantSeed(ReadSeed)
                .exceptionOrNull()
                ?.message,
        )
    }

    @Test
    fun successRoutesPayload() {
        val (key, variant) = MapAccessDeserializer.new(map()).variantSeed(ReadSeed).getOrThrow()
        assertEquals(1, key)
        assertEquals(10, variant.newtypeVariantSeed(ReadSeed).getOrThrow())
    }

    @Test
    fun payloadConversionIsDeferred() {
        val value = PanicValue()
        val map = mapDeserializer(listOf(MapEntry(I32Deserializer.new(1), value)).iterator())
        val (key, variant) = MapAccessDeserializer.new(map).variantSeed(ReadSeed).getOrThrow()
        assertEquals(1, key)
        assertEquals(0, value.calls)
        val failure = assertFailsWith<IllegalStateException> { variant.newtypeVariantSeed(ReadSeed) }
        assertEquals("conversion panic", failure.message)
        assertEquals(1, value.calls)
    }

    @Test
    fun typedAccessErrorPropagates() {
        val map = Access(false)
        assertSame(map.typedError, MapAccessDeserializer.new(map).variantSeed(ReadSeed).exceptionOrNull())
    }
}
