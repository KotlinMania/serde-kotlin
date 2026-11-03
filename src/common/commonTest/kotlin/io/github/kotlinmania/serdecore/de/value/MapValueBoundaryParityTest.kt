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

class MapValueBoundaryParityTest {
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

    private fun map() =
        mapDeserializer(
            listOf(MapEntry(I32Deserializer.new(1), I32Deserializer.new(10))).iterator(),
        )

    @Test
    fun valueBeforeKeyIsProgrammingPanic() {
        val deserializer = map()
        val seed = Seed(Mode.READ)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextValueSeed(seed) }
        assertEquals("MapAccess::next_value called before next_key", error.message)
        assertEquals(0, seed.calls)
    }

    @Test
    fun secondValueIsProgrammingPanic() {
        val deserializer = map()
        val seed = Seed(Mode.READ)
        assertEquals(1, deserializer.nextKey(I32Deserialize).getOrThrow())
        assertEquals(10, deserializer.nextValueSeed(seed).getOrThrow())
        val error = assertFailsWith<IllegalStateException> { deserializer.nextValueSeed(seed) }
        assertEquals("MapAccess::next_value called before next_key", error.message)
        assertEquals(1, seed.calls)
    }

    @Test
    fun seedPanicPropagatesAndConsumesValue() {
        val deserializer = map()
        val seed = Seed(Mode.PANIC)
        assertEquals(1, deserializer.nextKey(I32Deserialize).getOrThrow())
        val first = assertFailsWith<IllegalStateException> { deserializer.nextValueSeed(seed) }
        assertEquals("seed panic", first.message)
        val second = assertFailsWith<IllegalStateException> { deserializer.nextValueSeed(seed) }
        assertEquals("MapAccess::next_value called before next_key", second.message)
        assertEquals(1, seed.calls)
    }

    @Test
    fun conversionPanicPropagatesAndConsumesValue() {
        var conversions = 0
        val value =
            object : IntoDeserializer {
                override fun intoDeserializer(): Deserializer {
                    conversions++
                    error("value conversion panic")
                }
            }
        val deserializer = mapDeserializer(listOf(MapEntry(I32Deserializer.new(1), value)).iterator())
        val seed = Seed(Mode.READ)
        assertEquals(1, deserializer.nextKey(I32Deserialize).getOrThrow())
        val first = assertFailsWith<IllegalStateException> { deserializer.nextValueSeed(seed) }
        assertEquals("value conversion panic", first.message)
        val second = assertFailsWith<IllegalStateException> { deserializer.nextValueSeed(seed) }
        assertEquals("MapAccess::next_value called before next_key", second.message)
        assertEquals(1, conversions)
        assertEquals(0, seed.calls)
    }

    @Test
    fun typedSeedErrorRemainsError() {
        val deserializer = map()
        val seed = Seed(Mode.ERROR)
        assertEquals(1, deserializer.nextKey(I32Deserialize).getOrThrow())
        val result = assertIs<SerdeResult.Failure>(deserializer.nextValueSeed(seed))
        assertEquals("seed error", result.error.message)
        assertSame(seed.error, result.error)
        assertEquals(1, seed.calls)
    }

    @Test
    fun nullableDecodedValueIsSuccess() {
        val deserializer = map()
        val seed =
            object : DeserializeSeed<Int?> {
                override fun <D> deserialize(deserializer: D): SerdeResult<Int?> where D : Deserializer =
                    SerdeResult.success(null)
            }
        assertEquals(1, deserializer.nextKey(I32Deserialize).getOrThrow())
        val result = assertIs<SerdeResult.Success<Int?>>(deserializer.nextValueSeed(seed))
        assertEquals(null, result.value)
    }

    @Test
    fun entryAccessDoesNotPopulateValueSlot() {
        val deserializer = map()
        val seed = Seed(Mode.READ)
        assertEquals(Pair(1, 10), deserializer.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
        val error = assertFailsWith<IllegalStateException> { deserializer.nextValueSeed(seed) }
        assertEquals("MapAccess::next_value called before next_key", error.message)
        assertEquals(0, seed.calls)
    }

    @Test
    fun nextEntryAvailableAfterTypedError() {
        val deserializer =
            mapDeserializer(
                listOf(
                    MapEntry(I32Deserializer.new(1), I32Deserializer.new(10)),
                    MapEntry(I32Deserializer.new(2), I32Deserializer.new(20)),
                ).iterator(),
            )
        val failing = Seed(Mode.ERROR)
        val reading = Seed(Mode.READ)
        assertEquals(1, deserializer.nextKey(I32Deserialize).getOrThrow())
        assertEquals("seed error", deserializer.nextValueSeed(failing).exceptionOrNull()?.message)
        assertEquals(2, deserializer.nextKey(I32Deserialize).getOrThrow())
        assertEquals(20, deserializer.nextValueSeed(reading).getOrThrow())
        assertEquals(1, failing.calls)
        assertEquals(1, reading.calls)
    }
}
