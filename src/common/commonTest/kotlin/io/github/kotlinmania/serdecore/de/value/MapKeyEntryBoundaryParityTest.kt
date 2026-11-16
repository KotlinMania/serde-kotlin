package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IntoDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class MapKeyEntryBoundaryParityTest {
    private enum class Mode {
        READ,
        PANIC,
        ERROR,
    }

    private class Seed(
        private val mode: Mode,
        val error: SerdeError = SerdeError("seed error"),
    ) : DeserializeSeed<Int> {
        var calls = 0

        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer {
            calls++
            return when (mode) {
                Mode.READ -> I32Deserialize.deserialize(deserializer)
                Mode.PANIC -> error("seed panic")
                Mode.ERROR -> SerdeResult.failure(error)
            }
        }
    }

    private object NullSeed : DeserializeSeed<Int?> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int?> where D : Deserializer =
            SerdeResult.success(null)
    }

    private class Value(
        private val number: Int,
        private val panic: Boolean,
        private val converted: () -> Unit,
    ) : IntoDeserializer {
        override fun intoDeserializer(): Deserializer {
            converted()
            if (panic) error("conversion panic")
            return I32Deserializer.new(number)
        }
    }

    private fun map(two: Boolean = false): MapDeserializer<I32Deserializer, I32Deserializer> {
        val entries = mutableListOf(MapEntry(I32Deserializer.new(1), I32Deserializer.new(10)))
        if (two) entries.add(MapEntry(I32Deserializer.new(2), I32Deserializer.new(20)))
        return mapDeserializer(entries.iterator())
    }

    @Test
    fun keySeedPanicPreservesPendingValue() {
        val deserializer = map()
        val seed = Seed(Mode.PANIC)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextKeySeed(seed) }
        assertEquals("seed panic", error.message)
        assertEquals(10, deserializer.nextValue(I32Deserialize).getOrThrow())
        assertEquals(1, seed.calls)
    }

    @Test
    fun keyConversionPanicPreservesPendingValue() {
        var conversions = 0
        val key = Value(1, true) { conversions++ }
        val deserializer = mapDeserializer(listOf(MapEntry(key, I32Deserializer.new(10))).iterator())
        val seed = Seed(Mode.READ)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextKeySeed(seed) }
        assertEquals("conversion panic", error.message)
        assertEquals(10, deserializer.nextValue(I32Deserialize).getOrThrow())
        assertEquals(0, seed.calls)
        assertEquals(1, conversions)
    }

    @Test
    fun keyTypedErrorPreservesPendingValue() {
        val deserializer = map()
        val seed = Seed(Mode.ERROR)
        val result = assertIs<SerdeResult.Failure>(deserializer.nextKeySeed(seed))
        assertEquals("seed error", result.error.message)
        assertSame(seed.error, result.error)
        assertEquals(10, deserializer.nextValue(I32Deserialize).getOrThrow())
        assertEquals(1, seed.calls)
    }

    @Test
    fun keyEofDoesNotInvokeSeed() {
        val deserializer = mapDeserializer(emptyList<MapEntry<I32Deserializer, I32Deserializer>>().iterator())
        val seed = Seed(Mode.PANIC)
        repeat(2) { assertEquals(null, deserializer.nextKeySeed(seed).getOrThrow()) }
        assertEquals(0, seed.calls)
    }

    @Test
    fun entryKeyPanicSkipsValueSeed() {
        val deserializer = map(two = true)
        val keys = Seed(Mode.PANIC)
        val values = Seed(Mode.READ)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextEntrySeed(keys, values) }
        assertEquals("seed panic", error.message)
        assertEquals(1, keys.calls)
        assertEquals(0, values.calls)
        val reading = Seed(Mode.READ)
        assertEquals(Pair(2, 20), deserializer.nextEntrySeed(reading, values).getOrThrow())
        assertEquals(1, reading.calls)
        assertEquals(1, values.calls)
    }

    @Test
    fun entryValuePanicConsumesEntry() {
        val deserializer = map(two = true)
        val keys = Seed(Mode.READ)
        val values = Seed(Mode.PANIC)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextEntrySeed(keys, values) }
        assertEquals("seed panic", error.message)
        assertEquals(1, keys.calls)
        assertEquals(1, values.calls)
        val reading = Seed(Mode.READ)
        assertEquals(Pair(2, 20), deserializer.nextEntrySeed(keys, reading).getOrThrow())
        assertEquals(2, keys.calls)
        assertEquals(1, reading.calls)
    }

    @Test
    fun entryKeyErrorSkipsValueConversion() {
        var conversions = 0
        val deserializer =
            mapDeserializer(
                listOf(
                    MapEntry(I32Deserializer.new(1), Value(10, true) { conversions++ }),
                    MapEntry(I32Deserializer.new(2), Value(20, false) { conversions++ }),
                ).iterator(),
            )
        val keys = Seed(Mode.ERROR)
        val values = Seed(Mode.READ)
        val result = assertIs<SerdeResult.Failure>(deserializer.nextEntrySeed(keys, values))
        assertEquals("seed error", result.error.message)
        assertSame(keys.error, result.error)
        assertEquals(0, values.calls)
        assertEquals(0, conversions)
        val reading = Seed(Mode.READ)
        assertEquals(Pair(2, 20), deserializer.nextEntrySeed(reading, values).getOrThrow())
        assertEquals(1, reading.calls)
        assertEquals(1, values.calls)
        assertEquals(1, conversions)
    }

    @Test
    fun entryNullablePairIsPresent() {
        val deserializer = map()
        assertEquals(Pair(null, null), deserializer.nextEntrySeed(NullSeed, NullSeed).getOrThrow())
        assertEquals(null, deserializer.nextEntrySeed(NullSeed, NullSeed).getOrThrow())
    }

    @Test
    fun entryValueConversionPanicSkipsValueSeed() {
        var conversions = 0
        val deserializer =
            mapDeserializer(
                listOf(
                    MapEntry(I32Deserializer.new(1), Value(10, true) { conversions++ }),
                    MapEntry(I32Deserializer.new(2), Value(20, false) { conversions++ }),
                ).iterator(),
            )
        val keys = Seed(Mode.READ)
        val values = Seed(Mode.READ)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextEntrySeed(keys, values) }
        assertEquals("conversion panic", error.message)
        assertEquals(1, keys.calls)
        assertEquals(0, values.calls)
        assertEquals(1, conversions)
        assertEquals(Pair(2, 20), deserializer.nextEntrySeed(keys, values).getOrThrow())
        assertEquals(2, keys.calls)
        assertEquals(1, values.calls)
        assertEquals(2, conversions)
    }
}
