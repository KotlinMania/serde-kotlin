package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IntoDeserializer
import io.github.kotlinmania.serdecore.de.SeqElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class SeqElementBoundaryParityTest {
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

    private fun sequence() =
        seqDeserializer(listOf(I32Deserializer.new(10), I32Deserializer.new(20)).iterator())

    @Test
    fun seedPanicPropagatesAfterConsumption() {
        val deserializer = sequence()
        val panicking = Seed(Mode.PANIC)
        val reading = Seed(Mode.READ)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextElementSeed(panicking) }
        assertEquals("seed panic", error.message)
        assertEquals(SeqElement.Some(20), deserializer.nextElementSeed(reading).getOrThrow())
        assertEquals(1, panicking.calls)
        assertEquals(1, reading.calls)
    }

    @Test
    fun conversionPanicPropagatesAfterConsumption() {
        var conversions = 0

        fun value(number: Int, panic: Boolean): IntoDeserializer =
            object : IntoDeserializer {
                override fun intoDeserializer(): Deserializer {
                    conversions++
                    if (panic) error("conversion panic")
                    return I32Deserializer.new(number)
                }
            }

        val deserializer = seqDeserializer(listOf(value(10, true), value(20, false)).iterator())
        val seed = Seed(Mode.READ)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextElementSeed(seed) }
        assertEquals("conversion panic", error.message)
        assertEquals(0, seed.calls)
        assertEquals(1, conversions)
        assertEquals(SeqElement.Some(20), deserializer.nextElementSeed(seed).getOrThrow())
        assertEquals(1, seed.calls)
        assertEquals(2, conversions)
    }

    @Test
    fun iteratorPanicDoesNotFuse() {
        val input =
            object : Iterator<I32Deserializer> {
                var attempts = 0

                override fun hasNext(): Boolean = attempts < 2

                override fun next(): I32Deserializer {
                    if (!hasNext()) throw NoSuchElementException()
                    attempts++
                    if (attempts == 1) error("iterator panic")
                    return I32Deserializer.new(7)
                }
            }
        val deserializer = seqDeserializer(input)
        val seed = Seed(Mode.READ)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextElementSeed(seed) }
        assertEquals("iterator panic", error.message)
        assertEquals(0, seed.calls)
        assertEquals(SeqElement.Some(7), deserializer.nextElementSeed(seed).getOrThrow())
        assertEquals(SeqElement.None, deserializer.nextElementSeed(seed).getOrThrow())
        assertEquals(1, seed.calls)
    }

    @Test
    fun typedErrorStaysErrorAndConsumesItem() {
        val deserializer = sequence()
        val failing = Seed(Mode.ERROR)
        val reading = Seed(Mode.READ)
        val result = assertIs<SerdeResult.Failure>(deserializer.nextElementSeed(failing))
        assertEquals("seed error", result.error.message)
        assertSame(failing.error, result.error)
        assertEquals(SeqElement.Some(20), deserializer.nextElementSeed(reading).getOrThrow())
        assertEquals(1, failing.calls)
        assertEquals(1, reading.calls)
    }

    @Test
    fun nullableDecodedItemIsSomeNull() {
        val deserializer = seqDeserializer(listOf(I32Deserializer.new(10)).iterator())
        val seed =
            object : DeserializeSeed<Int?> {
                override fun <D> deserialize(deserializer: D): SerdeResult<Int?> where D : Deserializer =
                    SerdeResult.success(null)
            }
        assertEquals(SeqElement.Some(null), deserializer.nextElementSeed(seed).getOrThrow())
        assertEquals(SeqElement.None, deserializer.nextElementSeed(seed).getOrThrow())
    }

    @Test
    fun emptyIteratorIsFusedBeforeSeed() {
        var probes = 0
        val input =
            object : Iterator<I32Deserializer> {
                override fun hasNext(): Boolean {
                    probes++
                    return false
                }

                override fun next(): I32Deserializer = throw NoSuchElementException()
            }
        val deserializer = seqDeserializer(input)
        val seed = Seed(Mode.PANIC)
        repeat(2) { assertEquals(SeqElement.None, deserializer.nextElementSeed(seed).getOrThrow()) }
        assertEquals(1, probes)
        assertEquals(0, seed.calls)
    }

    @Test
    fun countPrecedesTypedError() {
        val deserializer = sequence()
        val seed = Seed(Mode.ERROR)
        assertEquals("seed error", deserializer.nextElementSeed(seed).exceptionOrNull()?.message)
        assertEquals("invalid length 2, expected 1 element in sequence", deserializer.end().exceptionOrNull()?.message)
        assertEquals(1, seed.calls)
    }

    @Test
    fun countPrecedesSeedPanic() {
        val deserializer = sequence()
        val seed = Seed(Mode.PANIC)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextElementSeed(seed) }
        assertEquals("seed panic", error.message)
        assertEquals("invalid length 2, expected 1 element in sequence", deserializer.end().exceptionOrNull()?.message)
        assertEquals(1, seed.calls)
    }
}
