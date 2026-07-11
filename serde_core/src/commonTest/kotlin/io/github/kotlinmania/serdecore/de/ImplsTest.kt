// port-lint: tests test_suite/tests/test_de.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

public class ImplsTest {
    @Test
    public fun stringDeserializesFromStringStrAndChar() {
        assertEquals("abc", StringDeserialize.deserialize(StringDeserializer("abc")).getOrThrow())
        assertEquals("abc", StringDeserialize.deserialize(StrDeserializer("abc")).getOrThrow())
        assertEquals("a", StringDeserialize.deserialize(CharDeserializer('a')).getOrThrow())
    }

    @Test
    public fun charDeserializesFromCharStrAndString() {
        assertEquals('a', CharDeserialize.deserialize(CharDeserializer('a')).getOrThrow())
        assertEquals('a', CharDeserialize.deserialize(StrDeserializer("a")).getOrThrow())
        assertEquals('a', CharDeserialize.deserialize(StringDeserializer("a")).getOrThrow())
    }

    @Test
    public fun stringDeserializeInPlaceUsesStringVisitor() {
        var place = "overwritten"

        StringDeserialize.deserializeInPlace(StrDeserializer("abc")) { place = it }.getOrThrow()
        assertEquals("abc", place)

        StringDeserialize.deserializeInPlace(StringDeserializer("owned")) { place = it }.getOrThrow()
        assertEquals("owned", place)

        StringDeserialize.deserializeInPlace(BytesDeserializer("bytes".encodeToByteArray())) { place = it }.getOrThrow()
        assertEquals("bytes", place)
    }

    @Test
    public fun stringFromUnitFailsWithStringExpectation() {
        val failure = StringDeserialize.deserialize(UnitDeserializer).exceptionOrNull()

        assertTrue(failure?.message?.contains("expected a string") == true)
    }

    @Test
    public fun stringFromBorrowedStrDeserializesOwnedValue() {
        assertEquals("owned", StringDeserialize.deserialize(BorrowedStrDeserializer("owned")).getOrThrow())
    }

    @Test
    public fun optionDeserializesUnitNoneAndSome() {
        val deserialize = nullableDeserialize(I32Deserialize)

        assertNull(deserialize.deserialize(OptionDeserializer(OptionMode.UnitValue)).getOrThrow())
        assertNull(deserialize.deserialize(OptionDeserializer(OptionMode.NoneValue)).getOrThrow())
        assertEquals(1, deserialize.deserialize(OptionDeserializer(OptionMode.SomeValue(1))).getOrThrow())
    }

    @Test
    public fun untaggedOptionMapsFailedValueToNone() {
        val deserialize = nullableDeserialize(I32Deserialize)

        assertEquals(
            7,
            deserialize.deserialize(OptionDeserializer(OptionMode.UntaggedValue(IntDeserializer(7)))).getOrThrow(),
        )
        assertNull(
            deserialize
                .deserialize(OptionDeserializer(OptionMode.UntaggedValue(StringDeserializer("not an int"))))
                .getOrThrow(),
        )
    }

    @Test
    public fun resultDeserializesOkAndErrNewtypeVariants() {
        val deserialize = resultDeserialize(I32Deserialize, I32Deserialize)

        assertEquals(
            ResultValue.Ok(0),
            deserialize.deserialize(ResultEnumDeserializer("Ok", 0)).getOrThrow(),
        )
        assertEquals(
            ResultValue.Err(1),
            deserialize.deserialize(ResultEnumDeserializer("Err", 1)).getOrThrow(),
        )
    }

    @Test
    public fun boundDeserializesUnitAndNewtypeVariants() {
        assertEquals(
            BoundValue.Unbounded,
            boundDeserialize(UnitDeserialize).deserialize(BoundEnumDeserializer("Unbounded", null)).getOrThrow(),
        )
        assertEquals(
            BoundValue.Included(0.toUByte()),
            boundDeserialize(U8Deserialize)
                .deserialize(BoundEnumDeserializer("Included", 0.toUByte().intoDeserializer()))
                .getOrThrow(),
        )
        assertEquals(
            BoundValue.Excluded(0.toUByte()),
            boundDeserialize(U8Deserialize)
                .deserialize(BoundEnumDeserializer("Excluded", 0.toUByte().intoDeserializer()))
                .getOrThrow(),
        )
    }

    @Test
    public fun rangeDeserializesFromStructAndSequence() {
        assertEquals(
            RangeValue(1u, 2u),
            rangeDeserialize(U32Deserialize)
                .deserialize(
                    mapOf(
                        "start".intoDeserializer() to 1u.intoDeserializer(),
                        "end".intoDeserializer() to 2u.intoDeserializer(),
                    ).intoDeserializer(),
                ).getOrThrow(),
        )
        assertEquals(
            RangeValue(1u, 2u),
            rangeDeserialize(U32Deserialize)
                .deserialize(listOf(1UL.intoDeserializer(), 2UL.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
    }

    @Test
    public fun rangeInclusiveDeserializesFromStructAndSequence() {
        assertEquals(
            RangeInclusiveValue(1u, 2u),
            rangeInclusiveDeserialize(U32Deserialize)
                .deserialize(
                    mapOf(
                        "start".intoDeserializer() to 1u.intoDeserializer(),
                        "end".intoDeserializer() to 2u.intoDeserializer(),
                    ).intoDeserializer(),
                ).getOrThrow(),
        )
        assertEquals(
            RangeInclusiveValue(1u, 2u),
            rangeInclusiveDeserialize(U32Deserialize)
                .deserialize(listOf(1UL.intoDeserializer(), 2UL.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
    }

    @Test
    public fun rangeFromDeserializesFromStructAndSequence() {
        assertEquals(
            RangeFromValue(1u),
            rangeFromDeserialize(U32Deserialize)
                .deserialize(mapOf("start".intoDeserializer() to 1u.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
        assertEquals(
            RangeFromValue(1u),
            rangeFromDeserialize(U32Deserialize)
                .deserialize(listOf(1u.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
    }

    @Test
    public fun rangeToDeserializesFromStructAndSequence() {
        assertEquals(
            RangeToValue(2u),
            rangeToDeserialize(U32Deserialize)
                .deserialize(mapOf("end".intoDeserializer() to 2u.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
        assertEquals(
            RangeToValue(2u),
            rangeToDeserialize(U32Deserialize)
                .deserialize(listOf(2u.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
    }

    @Test
    public fun durationDeserializesFromStructAndSequence() {
        val expected = 1.seconds + 2.nanoseconds

        assertEquals(
            expected,
            DurationDeserialize
                .deserialize(
                    mapOf(
                        "secs".intoDeserializer() to 1UL.intoDeserializer(),
                        "nanos".intoDeserializer() to 2u.intoDeserializer(),
                    ).intoDeserializer(),
                ).getOrThrow(),
        )
        assertEquals(
            expected,
            DurationDeserialize
                .deserialize(listOf(1L.intoDeserializer(), 2L.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
    }

    @Test
    public fun systemTimeDeserializesFromStructAndSequence() {
        val expected = Instant.fromEpochSeconds(1, 2)

        assertEquals(
            expected,
            SystemTimeDeserialize
                .deserialize(
                    mapOf(
                        "secs_since_epoch".intoDeserializer() to 1UL.intoDeserializer(),
                        "nanos_since_epoch".intoDeserializer() to 2u.intoDeserializer(),
                    ).intoDeserializer(),
                ).getOrThrow(),
        )
        assertEquals(
            expected,
            SystemTimeDeserialize
                .deserialize(listOf(1L.intoDeserializer(), 2L.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
    }

    @Test
    public fun vecDeserializesNestedSequences() {
        val deserialize = mutableListDeserialize(mutableListDeserialize(I32Deserialize))
        val deserializer =
            listOf(
                emptyList<I32Deserializer>().intoDeserializer(),
                listOf(1.intoDeserializer()).intoDeserializer(),
                listOf(2.intoDeserializer(), 3.intoDeserializer()).intoDeserializer(),
            ).intoDeserializer()

        assertEquals(
            mutableListOf(
                mutableListOf(),
                mutableListOf(1),
                mutableListOf(2, 3),
            ),
            deserialize.deserialize(deserializer).getOrThrow(),
        )
    }

    @Test
    public fun setDeserializesSequenceAndDeduplicatesInEncounterOrder() {
        val deserialize = mutableSetDeserialize(I32Deserialize)
        val deserializer =
            listOf(
                2.intoDeserializer(),
                1.intoDeserializer(),
                2.intoDeserializer(),
            ).intoDeserializer()

        assertEquals(linkedSetOf(2, 1), deserialize.deserialize(deserializer).getOrThrow())
    }

    @Test
    public fun setDeserializesNestedSequences() {
        val deserialize = mutableSetDeserialize(mutableSetDeserialize(I32Deserialize))
        val deserializer =
            listOf(
                emptyList<I32Deserializer>().intoDeserializer(),
                listOf(1.intoDeserializer()).intoDeserializer(),
                listOf(2.intoDeserializer(), 3.intoDeserializer()).intoDeserializer(),
            ).intoDeserializer()

        assertEquals(
            linkedSetOf(
                linkedSetOf(),
                linkedSetOf(1),
                linkedSetOf(2, 3),
            ),
            deserialize.deserialize(deserializer).getOrThrow(),
        )
    }

    @Test
    public fun mapDeserializesNestedMaps() {
        val deserialize = mutableMapDeserialize(I32Deserialize, mutableMapDeserialize(I32Deserialize, I32Deserialize))
        val deserializer =
            mapOf(
                1.intoDeserializer() to emptyMap<I32Deserializer, I32Deserializer>().intoDeserializer(),
                2.intoDeserializer() to
                    mapOf(
                        3.intoDeserializer() to 4.intoDeserializer(),
                        5.intoDeserializer() to 6.intoDeserializer(),
                    ).intoDeserializer(),
            ).intoDeserializer()

        assertEquals(
            linkedMapOf(
                1 to linkedMapOf(),
                2 to linkedMapOf(3 to 4, 5 to 6),
            ),
            deserialize.deserialize(deserializer).getOrThrow(),
        )
    }
}

private sealed class OptionMode {
    data object UnitValue : OptionMode()
    data object NoneValue : OptionMode()

    data class SomeValue(
        val value: Int,
    ) : OptionMode()

    data class UntaggedValue(
        val deserializer: Deserializer,
    ) : OptionMode()
}

private class OptionDeserializer(
    private val mode: OptionMode,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeOption(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        when (val current = mode) {
            OptionMode.UnitValue -> visitor.visitUnit()
            OptionMode.NoneValue -> visitor.visitNone()
            is OptionMode.SomeValue -> visitor.visitSome(IntDeserializer(current.value))
            is OptionMode.UntaggedValue -> visitor.privateVisitUntaggedOption(current.deserializer)
        }
}

private class ResultEnumDeserializer(
    private val variant: String,
    private val value: Int,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeEnum("Result", listOf("Ok", "Err"), visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(ResultEnumAccess(variant, value))
}

private class ResultEnumAccess(
    private val variant: String,
    private val value: Int,
) : EnumAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(variant.intoDeserializer()).map { it to ResultVariantAccess(value) }
}

private class ResultVariantAccess(
    private val value: Int,
) : VariantAccess {
    override fun unitVariant(): SerdeResult<Unit> = SerdeResult.failure(SerdeError.custom("unit variant was not expected"))

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
        seed.deserialize(IntDeserializer(value))

    override fun <V> tupleVariant(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("tuple variant was not expected"))

    override fun <V> structVariant(
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("struct variant was not expected"))
}

private class BoundEnumDeserializer(
    private val variant: String,
    private val value: Deserializer?,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeEnum("Bound", listOf("Unbounded", "Included", "Excluded"), visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(BoundEnumAccess(variant, value))
}

private class BoundEnumAccess(
    private val variant: String,
    private val value: Deserializer?,
) : EnumAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(variant.intoDeserializer()).map { it to BoundVariantAccess(value) }
}

private class BoundVariantAccess(
    private val value: Deserializer?,
) : VariantAccess {
    override fun unitVariant(): SerdeResult<Unit> =
        if (value == null) {
            SerdeResult.success(Unit)
        } else {
            SerdeResult.failure(SerdeError.custom("unit variant was not expected"))
        }

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> {
        val deserializer = value ?: return SerdeResult.failure(SerdeError.custom("newtype variant was not expected"))
        return seed.deserialize(deserializer)
    }

    override fun <V> tupleVariant(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("tuple variant was not expected"))

    override fun <V> structVariant(
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("struct variant was not expected"))
}

private class IntDeserializer(
    private val value: Int,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeI32(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = visitor.visitI32(value)
}

private class StringDeserializer(
    private val value: String,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeString(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = visitor.visitString(value)
}

private class StrDeserializer(
    private val value: String,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = visitor.visitStr(value)
}

private class BorrowedStrDeserializer(
    private val value: String,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBorrowedStr(value)
}

private class CharDeserializer(
    private val value: Char,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeChar(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = visitor.visitChar(value)
}

private class BytesDeserializer(
    private val value: ByteArray,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeByteBuf(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = visitor.visitByteBuf(value)
}

private data object UnitDeserializer : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeUnit(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()
}

private abstract class ForwardingDeserializer : Deserializer {
    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
}
