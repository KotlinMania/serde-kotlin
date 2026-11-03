package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MapEntryDefaultParityTest {
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

    private object NullSeed : DeserializeSeed<Int?> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int?> where D : Deserializer = SerdeResult.success(null)
    }

    private class Access(
        empty: Boolean = false,
        private val panicAt: Int = 0,
    ) : MapAccess {
        val inner =
            mapDeserializer(
                (if (empty) emptyList() else listOf(MapEntry(I32Deserializer.new(1), I32Deserializer.new(10)))).iterator(),
            )
        private var keyCalls = 0
        private var valueCalls = 0

        override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> {
            keyCalls++
            if (panicAt == 1) error("key access panic")
            return inner.nextKeySeed(seed)
        }

        override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
            valueCalls++
            if (panicAt == 2) error("value access panic")
            return inner.nextValueSeed(seed)
        }

        fun assertCounts(keys: Int, values: Int) {
            assertEquals(keys, keyCalls)
            assertEquals(values, valueCalls)
        }
    }

    @Test
    fun keySeedPanicSkipsValue() {
        val access = Access()
        val failure = assertFailsWith<IllegalStateException> { access.nextEntrySeed(PanicSeed, ReadSeed) }
        assertEquals("seed panic", failure.message)
        access.assertCounts(1, 0)
        assertEquals(10, access.inner.nextValue(I32Deserialize).getOrThrow())
    }

    @Test
    fun valueSeedPanicConsumesValue() {
        val access = Access()
        val failure = assertFailsWith<IllegalStateException> { access.nextEntrySeed(ReadSeed, PanicSeed) }
        assertEquals("seed panic", failure.message)
        access.assertCounts(1, 1)
        assertEquals(null, access.inner.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
    }

    @Test
    fun keyAccessPanicSkipsValue() {
        val access = Access(panicAt = 1)
        val failure = assertFailsWith<IllegalStateException> { access.nextEntrySeed(ReadSeed, ReadSeed) }
        assertEquals("key access panic", failure.message)
        access.assertCounts(1, 0)
    }

    @Test
    fun valueAccessPanicRetainsPendingValue() {
        val access = Access(panicAt = 2)
        val failure = assertFailsWith<IllegalStateException> { access.nextEntrySeed(ReadSeed, ReadSeed) }
        assertEquals("value access panic", failure.message)
        access.assertCounts(1, 1)
        assertEquals(10, access.inner.nextValue(I32Deserialize).getOrThrow())
    }

    @Test
    fun typedKeyErrorSkipsValue() {
        val access = Access()
        val seed = ErrorSeed()
        assertSame(seed.typedError, access.nextEntrySeed(seed, ReadSeed).exceptionOrNull())
        access.assertCounts(1, 0)
        assertEquals(10, access.inner.nextValue(I32Deserialize).getOrThrow())
    }

    @Test
    fun typedValueErrorConsumesValue() {
        val access = Access()
        val seed = ErrorSeed()
        assertSame(seed.typedError, access.nextEntrySeed(ReadSeed, seed).exceptionOrNull())
        access.assertCounts(1, 1)
        assertEquals(null, access.inner.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
    }

    @Test
    fun emptySkipsValue() {
        val access = Access(empty = true)
        assertEquals(null, access.nextEntrySeed(ReadSeed, ReadSeed).getOrThrow())
        access.assertCounts(1, 0)
    }

    @Test
    fun successPreservesPair() {
        val access = Access()
        assertEquals(Pair(1, 10), access.nextEntrySeed(ReadSeed, ReadSeed).getOrThrow())
        access.assertCounts(1, 1)
    }

    @Test
    fun nullableValuePreservesPresentPair() {
        val access = Access()
        assertEquals(Pair(1, null), access.nextEntrySeed(ReadSeed, NullSeed).getOrThrow())
        access.assertCounts(1, 1)
    }
}
