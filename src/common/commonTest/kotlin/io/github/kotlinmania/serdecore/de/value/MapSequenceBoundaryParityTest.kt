package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IntoDeserializer
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.unwrapOrElse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MapSequenceBoundaryParityTest {
    private object PanicSeed : DeserializeSeed<Int> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer = error("seed panic")
    }

    private object ErrorSeed : DeserializeSeed<Int> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            SerdeResult.failure(SerdeError("seed error"))
    }

    private object NullSeed : DeserializeSeed<Int?> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int?> where D : Deserializer =
            SerdeResult.success(null)
    }

    private class VisitorSeed<R>(
        private val visitor: Visitor<R>,
    ) : DeserializeSeed<R> {
        override fun <D> deserialize(deserializer: D): SerdeResult<R> where D : Deserializer =
            deserializer.deserializeSeq(visitor)
    }

    private enum class Mode {
        PANIC,
        KEY_PANIC,
        VALUE_PANIC,
        PARTIAL,
        TYPED_KEY_ERROR,
    }

    private class PairVisit(
        private val mode: Mode,
    ) : Visitor<Int> {
        override fun expecting(): String = "pair"

        override fun <A> visitSeq(access: A): SerdeResult<Int> where A : SeqAccess =
            when (mode) {
                Mode.PANIC -> {
                    error("visitor panic")
                }

                Mode.KEY_PANIC -> {
                    val error = assertFailsWith<IllegalStateException> { access.nextElementSeed(PanicSeed) }
                    assertEquals("seed panic", error.message)
                    assertEquals(1, access.sizeHint())
                    val value = access.nextElement(I32Deserialize).getOrThrow().unwrapOrElse { error("missing value") }
                    assertEquals(10, value)
                    assertEquals(0, access.sizeHint())
                    assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
                    SerdeResult.success(value)
                }

                Mode.VALUE_PANIC -> {
                    val key = access.nextElement(I32Deserialize).getOrThrow().unwrapOrElse { error("missing key") }
                    assertEquals(1, key)
                    val error = assertFailsWith<IllegalStateException> { access.nextElementSeed(PanicSeed) }
                    assertEquals("seed panic", error.message)
                    assertEquals(0, access.sizeHint())
                    assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
                    SerdeResult.success(key)
                }

                Mode.PARTIAL -> {
                    access.nextElement(I32Deserialize).map { it.unwrapOrElse { error("missing key") } }
                }

                Mode.TYPED_KEY_ERROR -> {
                    access.nextElementSeed(ErrorSeed).map { it.unwrapOrElse { error("missing key") } }
                }
            }
    }

    private object NullPairVisit : Visitor<Pair<Int?, Int?>> {
        override fun expecting(): String = "nullable pair"

        override fun <A> visitSeq(access: A): SerdeResult<Pair<Int?, Int?>> where A : SeqAccess {
            assertEquals(SeqElement.Some(null), access.nextElementSeed(NullSeed).getOrThrow())
            assertEquals(SeqElement.Some(null), access.nextElementSeed(NullSeed).getOrThrow())
            assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
            return SerdeResult.success(Pair(null, null))
        }
    }

    private fun map(two: Boolean = false): MapDeserializer<I32Deserializer, I32Deserializer> {
        val entries = mutableListOf(MapEntry(I32Deserializer.new(1), I32Deserializer.new(10)))
        if (two) entries.add(MapEntry(I32Deserializer.new(2), I32Deserializer.new(20)))
        return mapDeserializer(entries.iterator())
    }

    @Test
    fun outerSeedPanicConsumesEntry() {
        val deserializer = map(two = true)
        val error = assertFailsWith<IllegalStateException> { deserializer.nextElementSeed(PanicSeed) }
        assertEquals("seed panic", error.message)
        assertEquals(Pair(2, 20), deserializer.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
    }

    @Test
    fun outerSeedErrorRemainsError() {
        val deserializer = map(two = true)
        assertEquals("seed error", deserializer.nextElementSeed(ErrorSeed).exceptionOrNull()?.message)
        assertEquals(Pair(2, 20), deserializer.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
    }

    @Test
    fun pairVisitorPanicPropagates() {
        val deserializer = map()
        val seed = VisitorSeed(PairVisit(Mode.PANIC))
        val error = assertFailsWith<IllegalStateException> { deserializer.nextElementSeed(seed) }
        assertEquals("visitor panic", error.message)
    }

    @Test
    fun pairKeyPanicConsumesKeySlot() {
        val deserializer = map()
        assertEquals(SeqElement.Some(10), deserializer.nextElementSeed(VisitorSeed(PairVisit(Mode.KEY_PANIC))).getOrThrow())
    }

    @Test
    fun pairValuePanicConsumesValueSlot() {
        val deserializer = map()
        assertEquals(SeqElement.Some(1), deserializer.nextElementSeed(VisitorSeed(PairVisit(Mode.VALUE_PANIC))).getOrThrow())
    }

    @Test
    fun partialPairRetainsLengthError() {
        val deserializer = map()
        assertEquals(
            "invalid length 2, expected 1 element in sequence",
            deserializer.nextElementSeed(VisitorSeed(PairVisit(Mode.PARTIAL))).exceptionOrNull()?.message,
        )
    }

    @Test
    fun typedPairKeyErrorSkipsValue() {
        var conversions = 0
        val value =
            object : IntoDeserializer {
                override fun intoDeserializer(): Deserializer {
                    conversions++
                    error("unexpected value conversion")
                }
            }
        val deserializer = mapDeserializer(listOf(MapEntry(I32Deserializer.new(1), value)).iterator())
        assertEquals("seed error", deserializer.nextElementSeed(VisitorSeed(PairVisit(Mode.TYPED_KEY_ERROR))).exceptionOrNull()?.message)
        assertEquals(0, conversions)
    }

    @Test
    fun nullablePairIsPresent() {
        val deserializer = map()
        assertEquals(SeqElement.Some(Pair(null, null)), deserializer.nextElementSeed(VisitorSeed(NullPairVisit)).getOrThrow())
        assertEquals(SeqElement.None, deserializer.nextElementSeed(VisitorSeed(NullPairVisit)).getOrThrow())
    }
}
