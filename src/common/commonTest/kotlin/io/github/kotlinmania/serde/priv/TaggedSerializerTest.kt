// port-lint: tests test_suite/tests/test_enum_internally_tagged.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.ser.FormatArguments
import io.github.kotlinmania.serdecore.ser.FormatterSerializer
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdecore.ser.serialize
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertSerTokens
import io.github.kotlinmania.serdetest.assertSerTokensError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

private class DisplayFormattingBugException(
    message: String,
) : RuntimeException(message)

private class CountingDisplayValue(
    private val rendered: String,
) {
    var calls: Int = 0
        private set

    override fun toString(): String {
        calls += 1
        return rendered
    }
}

private class ThrowingDisplayValue(
    private val throwable: DisplayFormattingBugException,
) {
    var calls: Int = 0
        private set

    override fun toString(): String {
        calls += 1
        throw throwable
    }
}

private class CollectStrSerialize(
    private val value: Any,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.collectStr(value)
}

private object NoopSerializeMap : SerializeMap<Unit> {
    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize = SerdeResult.success(Unit)

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize = SerdeResult.success(Unit)

    override fun end(): SerdeResult<Unit> = SerdeResult.success(Unit)
}

private data class TaggedBoolVal(
    val value: Boolean,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedByteVal(
    val value: Byte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedShortVal(
    val value: Short,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedIntVal(
    val value: Int,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedLongVal(
    val value: Long,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedUByteVal(
    val value: UByte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedUShortVal(
    val value: UShort,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedUIntVal(
    val value: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedULongVal(
    val value: ULong,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedFloatVal(
    val value: Float,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedDoubleVal(
    val value: Double,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedCharVal(
    val value: Char,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedStringVal(
    val value: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private class TaggedBytesVal(
    private val value: ByteArray,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data object TaggedUnitVal : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeUnit()
}

private data object TaggedUnitStructVal : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeUnitStruct("Unit")
}

private data class TaggedOptionVal<T : Serialize>(
    val value: T?,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class TaggedNewtypeWrapper<T : Serialize>(
    val inner: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeNewtypeStruct("Newtype", inner)
}

private data class TaggedInnerStruct(
    val f: UByte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val state = serializer.serializeStruct("Struct", 1).getOrThrow()
            state.serializeField("f", TaggedUByteVal(f)).getOrThrow()
            state.end().getOrThrow()
        }
}

private class TaggedInnerMap(
    private val entries: List<Pair<String, String>>,
    private val knownLength: Boolean,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val map = serializer.serializeMap(if (knownLength) entries.size else null).getOrThrow()
            for ((k, v) in entries) {
                map.serializeEntry(TaggedStringVal(k), TaggedStringVal(v)).getOrThrow()
            }
            map.end().getOrThrow()
        }
}

private sealed class TaggedInnerEnum : Serialize {
    data object Unit : TaggedInnerEnum()

    data class Newtype(
        val value: UByte,
    ) : TaggedInnerEnum()

    data class Tuple(
        val first: UByte,
        val second: UByte,
    ) : TaggedInnerEnum()

    data class Struct(
        val f: UByte,
    ) : TaggedInnerEnum()

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        when (this) {
            Unit -> serializer.serializeUnitVariant("Enum", 0u, "Unit")
            is Newtype -> serializer.serializeNewtypeVariant("Enum", 1u, "Newtype", TaggedUByteVal(value))
            is Tuple ->
                serdeCatching {
                    val tv = serializer.serializeTupleVariant("Enum", 2u, "Tuple", 2).getOrThrow()
                    tv.serializeField(TaggedUByteVal(first)).getOrThrow()
                    tv.serializeField(TaggedUByteVal(second)).getOrThrow()
                    tv.end().getOrThrow()
                }
            is Struct ->
                serdeCatching {
                    val sv = serializer.serializeStructVariant("Enum", 3u, "Struct", 1).getOrThrow()
                    sv.serializeField("f", TaggedUByteVal(f)).getOrThrow()
                    sv.end().getOrThrow()
                }
        }
}

private fun <T : Serialize> taggedNewtype(
    typeIdent: String,
    variantIdent: String,
    tag: String,
    variantName: String,
    value: T,
): Serialize =
    object : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializeTaggedNewtype(
                serializer = serializer,
                typeIdent = typeIdent,
                variantIdent = variantIdent,
                tag = tag,
                variantName = variantName,
                value = value,
            )
    }

class TaggedSerializerTest {
    @Test
    fun collectStrFormatsDisplayValueBeforeStringRejectionMatchingStdAllocAndFlatMap() {
        val flatCounting = CountingDisplayValue("flat-rendered")
        val flatEnvelope =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val map = serializer.serializeMap(null).getOrThrow()
                        FormatArguments(flatCounting).serialize(FlatMapSerializer(map)).getOrThrow()
                        map.end().getOrThrow()
                    }
            }
        assertSerTokensError(
            flatEnvelope,
            listOf(Token.Map(null)),
            "can only flatten structs and maps (got a string)",
        )
        assertEquals(1, flatCounting.calls)

        val flatBug = DisplayFormattingBugException("flat display bug")
        val flatThrowing = ThrowingDisplayValue(flatBug)
        val thrownFlat =
            assertFailsWith<DisplayFormattingBugException> {
                FormatArguments(flatThrowing).serialize(FlatMapSerializer(NoopSerializeMap))
            }
        assertSame(flatBug, thrownFlat)
        assertEquals(1, flatThrowing.calls)

        val taggedCounting = CountingDisplayValue("tagged-rendered")
        assertSerTokensError(
            taggedNewtype(
                typeIdent = "InternallyTagged",
                variantIdent = "NewtypeDisplay",
                tag = "tag",
                variantName = "NewtypeDisplay",
                value = FormatArguments(taggedCounting),
            ),
            emptyList(),
            "cannot serialize tagged newtype variant InternallyTagged::NewtypeDisplay containing a string",
        )
        assertEquals(1, taggedCounting.calls)

        val directCounting = CountingDisplayValue("direct-collect-str")
        assertSerTokensError(
            taggedNewtype(
                typeIdent = "InternallyTagged",
                variantIdent = "DirectCollectStr",
                tag = "tag",
                variantName = "DirectCollectStr",
                value = CollectStrSerialize(directCounting),
            ),
            emptyList(),
            "cannot serialize tagged newtype variant InternallyTagged::DirectCollectStr containing a string",
        )
        assertEquals(1, directCounting.calls)

        val taggedBug = DisplayFormattingBugException("tagged display bug")
        val taggedThrowing = ThrowingDisplayValue(taggedBug)
        val thrownTagged =
            assertFailsWith<DisplayFormattingBugException> {
                serializeTaggedNewtype(
                    serializer = FormatterSerializer(StringBuilder()),
                    typeIdent = "InternallyTagged",
                    variantIdent = "NewtypeDisplay",
                    tag = "tag",
                    variantName = "NewtypeDisplay",
                    value = FormatArguments(taggedThrowing),
                )
            }
        assertSame(taggedBug, thrownTagged)
        assertEquals(1, taggedThrowing.calls)
    }

    @Test
    fun supportedTaggedNewtypeContainersAndEnumVariantsMatchUpstreamTokens() {
        assertSerTokens(
            taggedNewtype("InternallyTagged", "NewtypeUnit", "tag", "NewtypeUnit", TaggedUnitVal),
            listOf(
                Token.Map(1),
                Token.Str("tag"),
                Token.Str("NewtypeUnit"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype("InternallyTagged", "NewtypeUnitStruct", "tag", "NewtypeUnitStruct", TaggedUnitStructVal),
            listOf(
                Token.Map(1),
                Token.Str("tag"),
                Token.Str("NewtypeUnitStruct"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype(
                "InternallyTagged",
                "NewtypeNewtype",
                "tag",
                "NewtypeNewtype",
                TaggedNewtypeWrapper(TaggedInnerMap(emptyList(), knownLength = true)),
            ),
            listOf(
                Token.Map(1),
                Token.Str("tag"),
                Token.Str("NewtypeNewtype"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype(
                "InternallyTagged",
                "NewtypeMap",
                "tag",
                "NewtypeMap",
                TaggedInnerMap(listOf("field" to "value"), knownLength = true),
            ),
            listOf(
                Token.Map(2),
                Token.Str("tag"),
                Token.Str("NewtypeMap"),
                Token.Str("field"),
                Token.Str("value"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype(
                "InternallyTagged",
                "NewtypeMapUnknownLen",
                "tag",
                "NewtypeMapUnknownLen",
                TaggedInnerMap(listOf("field" to "value"), knownLength = false),
            ),
            listOf(
                Token.Map(null),
                Token.Str("tag"),
                Token.Str("NewtypeMapUnknownLen"),
                Token.Str("field"),
                Token.Str("value"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype("InternallyTagged", "NewtypeStruct", "tag", "NewtypeStruct", TaggedInnerStruct(6u)),
            listOf(
                Token.Struct("Struct", 2),
                Token.Str("tag"),
                Token.Str("NewtypeStruct"),
                Token.Str("f"),
                Token.U8(6u),
                Token.StructEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype("InternallyTagged", "NewtypeEnum", "tag", "NewtypeEnum", TaggedInnerEnum.Unit),
            listOf(
                Token.Map(2),
                Token.Str("tag"),
                Token.Str("NewtypeEnum"),
                Token.Str("Unit"),
                Token.UnitValue,
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype("InternallyTagged", "NewtypeEnum", "tag", "NewtypeEnum", TaggedInnerEnum.Newtype(1u)),
            listOf(
                Token.Map(2),
                Token.Str("tag"),
                Token.Str("NewtypeEnum"),
                Token.Str("Newtype"),
                Token.U8(1u),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype("InternallyTagged", "NewtypeEnum", "tag", "NewtypeEnum", TaggedInnerEnum.Tuple(1u, 1u)),
            listOf(
                Token.Map(2),
                Token.Str("tag"),
                Token.Str("NewtypeEnum"),
                Token.Str("Tuple"),
                Token.TupleStruct("Tuple", 2),
                Token.U8(1u),
                Token.U8(1u),
                Token.TupleStructEnd,
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            taggedNewtype("InternallyTagged", "NewtypeEnum", "tag", "NewtypeEnum", TaggedInnerEnum.Struct(1u)),
            listOf(
                Token.Map(2),
                Token.Str("tag"),
                Token.Str("NewtypeEnum"),
                Token.Str("Struct"),
                Token.Struct("Struct", 1),
                Token.Str("f"),
                Token.U8(1u),
                Token.StructEnd,
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun unsupportedTaggedNewtypePayloadsMatchUpstreamDiagnostics() {
        val cases: List<Pair<Serialize, String>> =
            listOf(
                TaggedBoolVal(true) to "cannot serialize tagged newtype variant Outer::Bad containing a boolean",
                TaggedByteVal(1) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedShortVal(1) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedIntVal(1) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedLongVal(1L) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedUByteVal(1u) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedUShortVal(1u) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedUIntVal(1u) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedULongVal(1uL) to "cannot serialize tagged newtype variant Outer::Bad containing an integer",
                TaggedFloatVal(1.5f) to "cannot serialize tagged newtype variant Outer::Bad containing a float",
                TaggedDoubleVal(1.5) to "cannot serialize tagged newtype variant Outer::Bad containing a float",
                TaggedCharVal('x') to "cannot serialize tagged newtype variant Outer::Bad containing a char",
                TaggedStringVal("bar") to "cannot serialize tagged newtype variant Outer::Bad containing a string",
                TaggedBytesVal(byteArrayOf(1, 2)) to
                    "cannot serialize tagged newtype variant Outer::Bad containing a byte array",
                TaggedOptionVal<TaggedIntVal>(null) to
                    "cannot serialize tagged newtype variant Outer::Bad containing an optional",
                TaggedOptionVal(TaggedIntVal(1)) to
                    "cannot serialize tagged newtype variant Outer::Bad containing an optional",
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                        listOf(1, 2).serialize(serializer)
                } to "cannot serialize tagged newtype variant Outer::Bad containing a sequence",
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                        serdeCatching {
                            val tuple = serializer.serializeTuple(1).getOrThrow()
                            tuple.serializeElement(TaggedIntVal(1)).getOrThrow()
                            tuple.end().getOrThrow()
                        }
                } to "cannot serialize tagged newtype variant Outer::Bad containing a tuple",
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                        serdeCatching {
                            val ts = serializer.serializeTupleStruct("Pair", 1).getOrThrow()
                            ts.serializeField(TaggedIntVal(1)).getOrThrow()
                            ts.end().getOrThrow()
                        }
                } to "cannot serialize tagged newtype variant Outer::Bad containing a tuple struct",
            )

        for ((payload, expectedError) in cases) {
            assertSerTokensError(
                taggedNewtype("Outer", "Bad", "tag", "Bad", payload),
                emptyList(),
                expectedError,
            )
        }
    }
}
