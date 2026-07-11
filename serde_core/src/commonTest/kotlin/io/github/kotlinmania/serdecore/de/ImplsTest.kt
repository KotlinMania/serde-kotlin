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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

public class ImplsTest {
    @Test
    public fun boolDeserializesFromBool() {
        assertEquals(true, BooleanDeserialize.deserialize(BoolDeserializer(true)).getOrThrow())
        assertEquals(false, BooleanDeserialize.deserialize(BoolDeserializer(false)).getOrThrow())
    }

    @Test
    public fun unitDeserializesFromUnitAndInPlace() {
        assertEquals(Unit, UnitDeserialize.deserialize(UnitDeserializer).getOrThrow())

        var called = false
        UnitDeserialize.deserializeInPlace(UnitDeserializer) { called = true }.getOrThrow()
        assertTrue(called)
    }

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
    public fun pathDeserializesBorrowedStringAndBytes() {
        assertEquals(
            PathValue("/usr/local/lib"),
            PathDeserialize.deserialize(BorrowedStrDeserializer("/usr/local/lib")).getOrThrow(),
        )
        assertEquals(
            PathValue("/usr/local/lib"),
            PathDeserialize.deserialize(BorrowedBytesDeserializer("/usr/local/lib".encodeToByteArray())).getOrThrow(),
        )
    }

    @Test
    public fun pathBufDeserializesStringAndBytes() {
        assertEquals(
            PathValue("/usr/local/lib"),
            PathBufDeserialize.deserialize(StrDeserializer("/usr/local/lib")).getOrThrow(),
        )
        assertEquals(
            PathValue("/usr/local/lib"),
            PathBufDeserialize.deserialize(StringDeserializer("/usr/local/lib")).getOrThrow(),
        )
        assertEquals(
            PathValue("/usr/local/lib"),
            PathBufDeserialize.deserialize("/usr/local/lib".encodeToByteArray().intoDeserializer()).getOrThrow(),
        )
        assertEquals(
            PathValue("/usr/local/lib"),
            PathBufDeserialize.deserialize(BytesDeserializer("/usr/local/lib".encodeToByteArray())).getOrThrow(),
        )
    }

    @Test
    public fun boxedPathDeserializesStringAndBytes() {
        assertEquals(
            BoxedPathValue(PathValue("/usr/local/lib")),
            BoxedPathDeserialize.deserialize(StrDeserializer("/usr/local/lib")).getOrThrow(),
        )
        assertEquals(
            BoxedPathValue(PathValue("/usr/local/lib")),
            BoxedPathDeserialize.deserialize(StringDeserializer("/usr/local/lib")).getOrThrow(),
        )
        assertEquals(
            BoxedPathValue(PathValue("/usr/local/lib")),
            BoxedPathDeserialize.deserialize("/usr/local/lib".encodeToByteArray().intoDeserializer()).getOrThrow(),
        )
        assertEquals(
            BoxedPathValue(PathValue("/usr/local/lib")),
            BoxedPathDeserialize.deserialize(BytesDeserializer("/usr/local/lib".encodeToByteArray())).getOrThrow(),
        )
    }

    @Test
    public fun cStringDeserializesBytesStringAndSequence() {
        assertContentEquals(
            "abc".encodeToByteArray(),
            CStringDeserialize.deserialize("abc".encodeToByteArray().intoDeserializer()).getOrThrow().bytes,
        )
        assertContentEquals(
            "abc".encodeToByteArray(),
            CStringDeserialize.deserialize(StringDeserializer("abc")).getOrThrow().bytes,
        )
        assertContentEquals(
            "abc".encodeToByteArray(),
            CStringDeserialize
                .deserialize(
                    listOf(
                        'a'.code.toUByte().intoDeserializer(),
                        'b'.code.toUByte().intoDeserializer(),
                        'c'.code.toUByte().intoDeserializer(),
                    ).intoDeserializer(),
                ).getOrThrow()
                .bytes,
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
    public fun ignoredAnyDeserializesStringSequenceAndEnum() {
        assertEquals(IgnoredAny, IgnoredAny.deserialize(StrDeserializer("s")).getOrThrow())
        assertEquals(
            IgnoredAny,
            IgnoredAny
                .deserialize(listOf(true.intoDeserializer()).intoDeserializer())
                .getOrThrow(),
        )
        assertEquals(
            IgnoredAny,
            IgnoredAny.deserialize(IgnoredAnyEnumDeserializer("Rust")).getOrThrow(),
        )
    }

    @Test
    public fun netIpv4AddressDeserializesReadableAndCompact() {
        val expected = Ipv4Address(octets(1, 2, 3, 4))

        assertEquals(expected, Ipv4AddressDeserialize.deserialize(StrDeserializer("1.2.3.4")).getOrThrow())
        assertEquals(expected, Ipv4AddressDeserialize.deserialize(compactTuple(1, 2, 3, 4)).getOrThrow())
    }

    @Test
    public fun netIpv6AddressDeserializesReadableAndCompact() {
        val expected = Ipv6Address(octets(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1))
        val compact = compactTuple(49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54)

        assertEquals(expected, Ipv6AddressDeserialize.deserialize(StrDeserializer("::1")).getOrThrow())
        assertEquals(
            Ipv6Address(octets("1234567890123456")),
            Ipv6AddressDeserialize.deserialize(compact).getOrThrow(),
        )
    }

    @Test
    public fun netIpAddressDeserializesReadableAndCompact() {
        val ipv4 = Ipv4Address(octets("1234"))

        assertEquals(
            IpAddress.V4(Ipv4Address(octets(1, 2, 3, 4))),
            IpAddressDeserialize.deserialize(StrDeserializer("1.2.3.4")).getOrThrow(),
        )
        assertEquals(
            IpAddress.V4(ipv4),
            IpAddressDeserialize.deserialize(compactEnum("IpAddr", "V4", compactTuple(49, 50, 51, 52))).getOrThrow(),
        )
    }

    @Test
    public fun netSocketAddressDeserializesReadableAndCompact() {
        val ipv4 = Ipv4Address(octets("1234"))
        val ipv6 = Ipv6Address(octets("1234567890123456"))

        assertEquals(
            SocketAddress.V4(SocketAddressV4(Ipv4Address(octets(1, 2, 3, 4)), 1234u)),
            SocketAddressDeserialize.deserialize(StrDeserializer("1.2.3.4:1234")).getOrThrow(),
        )
        assertEquals(
            SocketAddressV4(Ipv4Address(octets(1, 2, 3, 4)), 1234u),
            SocketAddressV4Deserialize.deserialize(StrDeserializer("1.2.3.4:1234")).getOrThrow(),
        )
        assertEquals(
            SocketAddressV6(Ipv6Address(octets(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)), 1234u),
            SocketAddressV6Deserialize.deserialize(StrDeserializer("[::1]:1234")).getOrThrow(),
        )
        assertEquals(
            SocketAddress.V6(SocketAddressV6(ipv6, 1234u)),
            SocketAddressDeserialize
                .deserialize(compactEnum("SocketAddr", "V6", compactTuple(compactTuple("1234567890123456"), UShortDeserializer(1234u))))
                .getOrThrow(),
        )
        assertEquals(
            SocketAddress.V4(SocketAddressV4(ipv4, 1234u)),
            SocketAddressDeserialize
                .deserialize(compactEnum("SocketAddr", "V4", compactTuple(compactTuple("1234"), UShortDeserializer(1234u))))
                .getOrThrow(),
        )
        assertEquals(
            SocketAddressV4(ipv4, 1234u),
            SocketAddressV4Deserialize.deserialize(compactTuple(compactTuple("1234"), UShortDeserializer(1234u))).getOrThrow(),
        )
        assertEquals(
            SocketAddressV6(ipv6, 1234u),
            SocketAddressV6Deserialize.deserialize(compactTuple(compactTuple("1234567890123456"), UShortDeserializer(1234u))).getOrThrow(),
        )
    }

    @Test
    public fun osStringDeserializesUnixAndWindowsVariants() {
        assertEquals(
            OsStringValue.Unix(listOf(1u, 2u, 3u).map { it.toUByte() }),
            OsStringDeserialize
                .deserialize(
                    compactEnum(
                        "OsString",
                        "Unix",
                        compactTuple(
                            UByteDeserializer(1u.toUByte()),
                            UByteDeserializer(2u.toUByte()),
                            UByteDeserializer(3u.toUByte()),
                        ),
                    ),
                ).getOrThrow(),
        )
        assertEquals(
            OsStringValue.Windows(listOf(1u, 2u, 3u).map { it.toUShort() }),
            OsStringDeserialize
                .deserialize(
                    compactEnum(
                        "OsString",
                        "Windows",
                        compactTuple(
                            UShortDeserializer(1u.toUShort()),
                            UShortDeserializer(2u.toUShort()),
                            UShortDeserializer(3u.toUShort()),
                        ),
                    ),
                ).getOrThrow(),
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
    public fun fixedArrayDeserializesEmptyNestedAndTupleStructForms() {
        assertEquals(
            ArrayValue<Int>(emptyList()),
            emptyArrayDeserialize<Int>().deserialize(emptyList<I32Deserializer>().intoDeserializer()).getOrThrow(),
        )
        assertEquals(
            ArrayValue<Int>(emptyList()),
            emptyArrayDeserialize<Int>().deserialize(compactEmptyTuple()).getOrThrow(),
        )

        val deserialize =
            tripleDeserialize(
                emptyArrayDeserialize<Int>(),
                arrayDeserialize(1, I32Deserialize),
                arrayDeserialize(2, I32Deserialize),
            )
        val expected =
            Triple(
                ArrayValue<Int>(emptyList()),
                ArrayValue(listOf(1)),
                ArrayValue(listOf(2, 3)),
            )

        assertEquals(
            expected,
            deserialize
                .deserialize(
                    listOf(
                        emptyList<I32Deserializer>().intoDeserializer(),
                        listOf(1.intoDeserializer()).intoDeserializer(),
                        listOf(2.intoDeserializer(), 3.intoDeserializer()).intoDeserializer(),
                    ).intoDeserializer(),
                ).getOrThrow(),
        )
        assertEquals(
            expected,
            deserialize
                .deserialize(
                    compactTuple(
                        compactEmptyTuple(),
                        compactTuple(IntDeserializer(1)),
                        compactTuple(IntDeserializer(2), IntDeserializer(3)),
                    ),
                ).getOrThrow(),
        )
        assertEquals(
            ArrayValue<Int>(emptyList()),
            emptyArrayDeserialize<Int>().deserialize(EmptyTupleStructDeserializer).getOrThrow(),
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

private class UShortDeserializer(
    private val value: UShort,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeU16(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = visitor.visitU16(value)
}

private class BoolDeserializer(
    private val value: Boolean,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeBool(visitor)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBool(value)
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

private class BorrowedBytesDeserializer(
    private val value: ByteArray,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeBytes(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBorrowedBytes(value)
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

private data object EmptyTupleStructDeserializer : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeTupleStruct("Anything", 0, visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitSeq(CompactSeqAccess(emptyList()))
}

private fun octets(vararg values: Int): List<UByte> = values.map { it.toUByte() }

private fun octets(value: String): List<UByte> = value.encodeToByteArray().map { it.toInt().toUByte() }

private fun compactTuple(vararg values: Int): Deserializer =
    compactTuple(*values.map { UByteDeserializer(it.toUByte()) }.toTypedArray())

private fun compactTuple(value: String): Deserializer =
    compactTuple(*value.encodeToByteArray().map { UByteDeserializer(it.toInt().toUByte()) }.toTypedArray())

private fun compactEmptyTuple(): Deserializer = compactTuple(*emptyArray<Deserializer>())

private fun compactTuple(vararg values: Deserializer): Deserializer = CompactTupleDeserializer(values.toList())

private fun compactEnum(
    name: String,
    variant: String,
    value: Deserializer,
): Deserializer = CompactEnumDeserializer(name, variant, value)

private class UByteDeserializer(
    private val value: UByte,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeU8(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = visitor.visitU8(value)

    override fun isHumanReadable(): Boolean = false
}

private class CompactTupleDeserializer(
    private val values: List<Deserializer>,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeTuple(values.size, visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitSeq(CompactSeqAccess(values))

    override fun isHumanReadable(): Boolean = false
}

private class CompactSeqAccess(
    values: List<Deserializer>,
) : SeqAccess {
    private val iterator = values.iterator()

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?> =
        if (iterator.hasNext()) {
            seed.deserialize(iterator.next()).map { it }
        } else {
            SerdeResult.success(null)
        }

    override fun sizeHint(): Int? = null
}

private class CompactEnumDeserializer(
    private val name: String,
    private val variant: String,
    private val value: Deserializer,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeEnum(name, listOf("V4", "V6"), visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(CompactEnumAccess(variant, value))

    override fun isHumanReadable(): Boolean = false
}

private class CompactEnumAccess(
    private val variant: String,
    private val value: Deserializer,
) : EnumAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(variant.intoDeserializer()).map { it to CompactVariantAccess(value) }
}

private class CompactVariantAccess(
    private val value: Deserializer,
) : VariantAccess {
    override fun unitVariant(): SerdeResult<Unit> = SerdeResult.failure(SerdeError.custom("unit variant was not expected"))

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> = seed.deserialize(value)

    override fun <V> tupleVariant(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("tuple variant was not expected"))

    override fun <V> structVariant(
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("struct variant was not expected"))
}

private class IgnoredAnyEnumDeserializer(
    private val variant: String,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        visitor.visitEnum(IgnoredAnyEnumAccess(variant))

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)
}

private class IgnoredAnyEnumAccess(
    private val variant: String,
) : EnumAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(variant.intoDeserializer()).map { it to IgnoredAnyVariantAccess }
}

private data object IgnoredAnyVariantAccess : VariantAccess {
    override fun unitVariant(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
        seed.deserialize(UnitDeserializer)

    override fun <V> tupleVariant(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("tuple variant was not expected"))

    override fun <V> structVariant(
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("struct variant was not expected"))
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
