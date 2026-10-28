package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.SeqDeserializer
import io.github.kotlinmania.serdecore.de.value.UnitDeserializer
import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import io.github.kotlinmania.serdecore.de.value.seqDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class NullableSequenceTest {
    private val nullableU32 = nullableDeserialize(U32Deserialize)

    @Test
    fun standaloneNullableUnitRemainsNull() {
        assertNull(nullableU32.deserialize(UnitDeserializer.new()).getOrThrow())
    }

    @Test
    fun singleNullableUnitIsAnElement() {
        val actual = mutableListDeserialize(nullableU32).deserialize(listOf(UnitDeserializer.new()).intoDeserializer()).getOrThrow()
        assertEquals(listOf<UInt?>(null), actual)
    }

    @Test
    fun emptySequenceRemainsEmpty() {
        val actual = mutableListDeserialize(nullableU32).deserialize(emptyList<OptionalU32>().intoDeserializer()).getOrThrow()
        assertEquals(emptyList(), actual)
    }

    @Test
    fun nullableListRetainsLeadingMiddleAndTrailingNulls() {
        val actual = mutableListDeserialize(nullableU32).deserialize(values(null, 7u, null, 9u, null)).getOrThrow()
        assertEquals(listOf(null, 7u, null, 9u, null), actual)
    }

    @Test
    fun nullableListDoesNotNeedASizeHint() {
        val access = seqDeserializer(listOf(OptionalU32(null), OptionalU32(7u), OptionalU32(null)).iterator())
        assertNull(access.sizeHint())
        val actual = mutableListDeserialize(nullableU32).deserialize(access).getOrThrow()
        assertEquals(listOf(null, 7u, null), actual)
    }

    @Test
    fun nullableSetRetainsNullAndFollowingValues() {
        val actual = mutableSetDeserialize(nullableU32).deserialize(values(null, 7u, null)).getOrThrow()
        assertEquals(setOf(null, 7u), actual)
    }

    @Test
    fun nullableTupleFieldsArePresent() {
        val pair = pairDeserialize(nullableU32, nullableU32).deserialize(values(null, 7u)).getOrThrow()
        assertEquals(null to 7u, pair)
        val triple = tripleDeserialize(nullableU32, nullableU32, nullableU32).deserialize(values(null, 7u, null)).getOrThrow()
        assertEquals(Triple(null, 7u, null), triple)
    }

    @Test
    fun nullableArrayElementsArePresent() {
        val actual = arrayDeserialize(3, nullableU32).deserialize(values(null, 7u, null)).getOrThrow()
        assertEquals(ArrayValue(listOf(null, 7u, null)), actual)
    }

    @Test
    fun missingTupleAndArrayElementsStillFail() {
        val pair = pairDeserialize(nullableU32, nullableU32).deserialize(values(null))
        assertEquals("invalid length 1, expected a tuple of size 2", assertIs<SerdeResult.Failure>(pair).error.message)
        val array = arrayDeserialize(2, nullableU32).deserialize(values(null))
        assertEquals("invalid length 1, expected an array of length 2", assertIs<SerdeResult.Failure>(array).error.message)
    }

    @Test
    fun nullableRangeFieldsArePresent() {
        assertEquals(RangeValue(null, 7u), rangeDeserialize(nullableU32).deserialize(values(null, 7u)).getOrThrow())
        assertEquals(RangeInclusiveValue(null, 7u), rangeInclusiveDeserialize(nullableU32).deserialize(values(null, 7u)).getOrThrow())
        assertEquals(RangeFromValue<UInt?>(null), rangeFromDeserialize(nullableU32).deserialize(values(null)).getOrThrow())
        assertEquals(RangeToValue<UInt?>(null), rangeToDeserialize(nullableU32).deserialize(values(null)).getOrThrow())
    }

    @Test
    fun sequenceOverloadsDistinguishPresentNullFromEnd() {
        val access = values(null, 7u, null)
        val seed = seedFromDeserialize(nullableU32)
        assertEquals(3, access.sizeHint())
        assertEquals(SeqElement.Some<UInt?>(null), access.nextElement(nullableU32).getOrThrow())
        assertEquals(2, access.sizeHint())
        assertEquals(SeqElement.Some<UInt?>(7u), access.nextElement(seed).getOrThrow())
        assertEquals(1, access.sizeHint())
        assertEquals(SeqElement.Some<UInt?>(null), access.nextElementSeed(seed).getOrThrow())
        assertEquals(0, access.sizeHint())
        assertEquals(SeqElement.None, access.nextElementSeed(seed).getOrThrow())
        assertEquals(SeqElement.None, access.nextElement(nullableU32).getOrThrow())
        assertEquals(0, access.sizeHint())
    }

    @Test
    fun exhaustedSequenceDoesNotInvokeSeedAndFailureIsPreserved() {
        val error = SerdeError.custom("element rejected")
        var calls = 0
        val seed = object : DeserializeSeed<UInt?> {
            override fun <D> deserialize(deserializer: D): SerdeResult<UInt?>
                where D : Deserializer {
                calls += 1
                return SerdeResult.failure(error)
            }
        }
        val access = values(null)
        assertSame(error, assertIs<SerdeResult.Failure>(access.nextElementSeed(seed)).error)
        assertEquals(SeqElement.None, access.nextElementSeed(seed).getOrThrow())
        assertEquals(1, calls)
    }

    @Test
    fun mapAsSequencePreservesNullablePairFields() {
        val input = mapOf(OptionalU32(null) to OptionalU32(7u)).intoDeserializer()
        val actual = mutableListDeserialize(pairDeserialize(nullableU32, nullableU32)).deserialize(input).getOrThrow()
        assertEquals(listOf<Pair<UInt?, UInt?>>(null to 7u), actual)
    }

    @Test
    fun mapAsSequencePreservesNullableSeedResults() {
        val nullablePair = object : Deserialize<Unit?> {
            override fun <D> deserialize(deserializer: D): SerdeResult<Unit?>
                where D : Deserializer =
                pairDeserialize(nullableU32, nullableU32).deserialize(deserializer).map { null }
        }
        val input = mapOf(OptionalU32(null) to OptionalU32(7u)).intoDeserializer()
        val actual = mutableListDeserialize(nullablePair).deserialize(input).getOrThrow()
        assertEquals(listOf<Unit?>(null), actual)
    }

    @Test
    fun ignoredSequenceConsumesAllNullableValues() {
        val access = values(null, 7u, null)
        assertEquals(IgnoredAny, IgnoredAny.deserialize(access).getOrThrow())
        assertEquals(0, access.sizeHint())
    }

    private fun values(vararg values: UInt?): SeqDeserializer<OptionalU32> = values.map(::OptionalU32).intoDeserializer()
}

private class OptionalU32(private val value: UInt?) : IntoDeserializer {
    override fun intoDeserializer(): Deserializer =
        object : Deserializer by UnitDeserializer.new() {
            override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
                if (value == null) visitor.visitNone() else visitor.visitSome(value.intoDeserializer())

            override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
        }
}
