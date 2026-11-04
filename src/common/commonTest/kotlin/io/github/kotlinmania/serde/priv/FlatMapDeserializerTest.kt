// port-lint: tests test_suite/tests/test_annotations.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.U32Deserialize
import io.github.kotlinmania.serdecore.de.UnitDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.mapDeserialize
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.de.seedFromDeserialize
import io.github.kotlinmania.serdecore.priv.SharedByteSlice
import io.github.kotlinmania.serdecore.priv.contentaccess.newBoolContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newBytesContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newI32Content
import io.github.kotlinmania.serdecore.priv.contentaccess.newSeqContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStringContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newU32Content
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdecore.ser.serialize
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokens
import io.github.kotlinmania.serdetest.assertDeTokensError
import io.github.kotlinmania.serdetest.assertTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private sealed class ExternallyTaggedDeEnum : Serialize {
    data object Unit : ExternallyTaggedDeEnum()

    data class Newtype(
        val entries: Map<String, String>,
    ) : ExternallyTaggedDeEnum()

    data class Tuple(
        val first: UInt,
        val second: UInt,
    ) : ExternallyTaggedDeEnum()

    data class Struct(
        val index: UInt,
        val value: UInt,
    ) : ExternallyTaggedDeEnum()

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        when (this) {
            Unit -> {
                serializer.serializeUnitVariant("Enum", 0u, "Unit")
            }

            is Newtype -> {
                serializer.serializeNewtypeVariant(
                    "Enum",
                    1u,
                    "Newtype",
                    object : Serialize {
                        override fun <R> serialize(serializer: Serializer<R>): SerdeResult<R> = entries.serialize(serializer)
                    },
                )
            }

            is Tuple -> {
                serdeCatching {
                    val tv = serializer.serializeTupleVariant("Enum", 2u, "Tuple", 2).getOrThrow()
                    tv.serializeField(UIntEntry(first)).getOrThrow()
                    tv.serializeField(UIntEntry(second)).getOrThrow()
                    tv.end().getOrThrow()
                }
            }

            is Struct -> {
                serdeCatching {
                    val sv = serializer.serializeStructVariant("Enum", 3u, "Struct", 2).getOrThrow()
                    sv.serializeField("index", UIntEntry(index)).getOrThrow()
                    sv.serializeField("value", UIntEntry(value)).getOrThrow()
                    sv.end().getOrThrow()
                }
            }
        }

    companion object : Deserialize<ExternallyTaggedDeEnum> {
        val VARIANTS: List<String> = listOf("Unit", "Newtype", "Tuple", "Struct")

        override fun <D> deserialize(deserializer: D): SerdeResult<ExternallyTaggedDeEnum>
            where D : Deserializer =
            deserializer.deserializeEnum("Enum", VARIANTS, ExternallyTaggedDeEnumVisitor)
    }
}

private data class UIntEntry(
    val value: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class StringEntry(
    val value: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private enum class ExternallyTaggedVariantId {
    Unit,
    Newtype,
    Tuple,
    Struct,
}

private data object ExternallyTaggedVariantSeed : DeserializeSeed<ExternallyTaggedVariantId> {
    override fun <D> deserialize(deserializer: D): SerdeResult<ExternallyTaggedVariantId>
        where D : Deserializer =
        deserializer.deserializeIdentifier(
            object : Visitor<ExternallyTaggedVariantId> {
                override fun expecting(): String = "variant identifier"

                override fun visitStr(v: String): SerdeResult<ExternallyTaggedVariantId> =
                    when (v) {
                        "Unit" -> {
                            SerdeResult.success(ExternallyTaggedVariantId.Unit)
                        }

                        "Newtype" -> {
                            SerdeResult.success(ExternallyTaggedVariantId.Newtype)
                        }

                        "Tuple" -> {
                            SerdeResult.success(ExternallyTaggedVariantId.Tuple)
                        }

                        "Struct" -> {
                            SerdeResult.success(ExternallyTaggedVariantId.Struct)
                        }

                        else -> {
                            SerdeResult.failure(
                                SerdeError.unknownVariant(
                                    v,
                                    ExternallyTaggedDeEnum.VARIANTS,
                                ),
                            )
                        }
                    }
            },
        )
}

private data object ExternallyTaggedDeEnumVisitor : Visitor<ExternallyTaggedDeEnum> {
    private val stringMapDeserialize = mapDeserialize(StringDeserialize, StringDeserialize)

    override fun expecting(): String = "enum Enum"

    override fun <A> visitEnum(access: A): SerdeResult<ExternallyTaggedDeEnum>
        where A : EnumAccess =
        serdeCatching {
            val (variant, variantAccess) = access.variantSeed(ExternallyTaggedVariantSeed).getOrThrow()
            when (variant) {
                ExternallyTaggedVariantId.Unit -> {
                    variantAccess.unitVariant().getOrThrow()
                    ExternallyTaggedDeEnum.Unit
                }

                ExternallyTaggedVariantId.Newtype -> {
                    val map =
                        variantAccess
                            .newtypeVariantSeed(seedFromDeserialize(stringMapDeserialize))
                            .getOrThrow()
                    ExternallyTaggedDeEnum.Newtype(map)
                }

                ExternallyTaggedVariantId.Tuple -> {
                    variantAccess
                        .tupleVariant(
                            2,
                            TupleVariantPayloadVisitor,
                        ).getOrThrow()
                }

                ExternallyTaggedVariantId.Struct -> {
                    variantAccess
                        .structVariant(
                            listOf("index", "value"),
                            StructVariantPayloadVisitor,
                        ).getOrThrow()
                }
            }
        }
}

private data object TupleVariantPayloadVisitor : Visitor<ExternallyTaggedDeEnum> {
    override fun expecting(): String = "tuple variant Enum::Tuple"

    override fun <S> visitSeq(access: S): SerdeResult<ExternallyTaggedDeEnum>
        where S : SeqAccess =
        serdeCatching {
            val first =
                when (val elem = access.nextElement(U32Deserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(0, this@TupleVariantPayloadVisitor).raise()
                }
            val second =
                when (val elem = access.nextElement(U32Deserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(1, this@TupleVariantPayloadVisitor).raise()
                }
            ExternallyTaggedDeEnum.Tuple(first, second)
        }
}

private data object StructVariantPayloadVisitor : Visitor<ExternallyTaggedDeEnum> {
    override fun expecting(): String = "struct variant Enum::Struct"

    override fun <S> visitSeq(access: S): SerdeResult<ExternallyTaggedDeEnum>
        where S : SeqAccess =
        serdeCatching {
            val index =
                when (val elem = access.nextElement(U32Deserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(0, this@StructVariantPayloadVisitor).raise()
                }
            val value =
                when (val elem = access.nextElement(U32Deserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(1, this@StructVariantPayloadVisitor).raise()
                }
            ExternallyTaggedDeEnum.Struct(index = index, value = value)
        }

    override fun <M> visitMap(access: M): SerdeResult<ExternallyTaggedDeEnum>
        where M : MapAccess =
        serdeCatching {
            var index: UInt? = null
            var value: UInt? = null
            while (true) {
                val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                when (key) {
                    "index" -> {
                        if (index != null) {
                            SerdeError.duplicateField("index").raise()
                        }
                        index = access.nextValue(U32Deserialize).getOrThrow()
                    }

                    "value" -> {
                        if (value != null) {
                            SerdeError.duplicateField("value").raise()
                        }
                        value = access.nextValue(U32Deserialize).getOrThrow()
                    }

                    else -> {
                        access.nextValue(IgnoredAny).getOrThrow()
                    }
                }
            }
            ExternallyTaggedDeEnum.Struct(
                index = index ?: SerdeError.missingField("index").raise(),
                value = value ?: SerdeError.missingField("value").raise(),
            )
        }
}

private data class FlattenedEnumWithExtra(
    val data: ExternallyTaggedDeEnum,
    val extra: Map<String, String>,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val map = serializer.serializeMap(null).getOrThrow()
            data.serialize(FlatMapSerializer(map)).getOrThrow()
            val extraSerializer = FlatMapSerializer(map)
            val innerMap = extraSerializer.serializeMap(extra.size).getOrThrow()
            for ((k, v) in extra) {
                innerMap.serializeEntry(StringEntry(k), StringEntry(v)).getOrThrow()
            }
            innerMap.end().getOrThrow()
            map.end().getOrThrow()
        }

    companion object : Deserialize<FlattenedEnumWithExtra> {
        private val extraMapDeserialize = mapDeserialize(StringDeserialize, StringDeserialize)

        override fun <D> deserialize(deserializer: D): SerdeResult<FlattenedEnumWithExtra>
            where D : Deserializer =
            deserializer.deserializeMap(
                object : Visitor<FlattenedEnumWithExtra> {
                    override fun expecting(): String = "struct Flatten"

                    override fun <M> visitMap(access: M): SerdeResult<FlattenedEnumWithExtra>
                        where M : MapAccess =
                        serdeCatching {
                            val buffer = collectFlatMapBuffer(access).getOrThrow()
                            val data = ExternallyTaggedDeEnum.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
                            val extra = extraMapDeserialize.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
                            FlattenedEnumWithExtra(data = data, extra = extra)
                        }
                },
            )
    }
}

private data class StrictDenyUnknownFlattenEnum(
    val data: ExternallyTaggedDeEnum,
) {
    companion object : Deserialize<StrictDenyUnknownFlattenEnum> {
        override fun <D> deserialize(deserializer: D): SerdeResult<StrictDenyUnknownFlattenEnum>
            where D : Deserializer =
            deserializer.deserializeMap(
                object : Visitor<StrictDenyUnknownFlattenEnum> {
                    override fun expecting(): String = "struct StrictDenyUnknownFlattenEnum"

                    override fun <M> visitMap(access: M): SerdeResult<StrictDenyUnknownFlattenEnum>
                        where M : MapAccess =
                        serdeCatching {
                            val buffer = collectFlatMapBuffer(access).getOrThrow()
                            val data = ExternallyTaggedDeEnum.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
                            denyRemainingUnknownFields(buffer).getOrThrow()
                            StrictDenyUnknownFlattenEnum(data)
                        }
                },
            )
    }
}

private data class TwoOptionalFlattenedEnums(
    val first: ExternallyTaggedDeEnum?,
    val second: ExternallyTaggedDeEnum?,
) {
    companion object : Deserialize<TwoOptionalFlattenedEnums> {
        private val optionalEnum = nullableDeserialize(ExternallyTaggedDeEnum)

        override fun <D> deserialize(deserializer: D): SerdeResult<TwoOptionalFlattenedEnums>
            where D : Deserializer =
            deserializer.deserializeMap(
                object : Visitor<TwoOptionalFlattenedEnums> {
                    override fun expecting(): String = "struct TwoOptionalFlattenedEnums"

                    override fun <M> visitMap(access: M): SerdeResult<TwoOptionalFlattenedEnums>
                        where M : MapAccess =
                        serdeCatching {
                            val buffer = collectFlatMapBuffer(access).getOrThrow()
                            val first = optionalEnum.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
                            val second = optionalEnum.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
                            denyRemainingUnknownFields(buffer).getOrThrow()
                            TwoOptionalFlattenedEnums(first = first, second = second)
                        }
                },
            )
    }
}

private sealed class SecondaryEnum {
    data class Secondary(
        val label: String,
    ) : SecondaryEnum()

    data object Other : SecondaryEnum()

    companion object : Deserialize<SecondaryEnum> {
        val VARIANTS: List<String> = listOf("Secondary", "Other")

        override fun <D> deserialize(deserializer: D): SerdeResult<SecondaryEnum>
            where D : Deserializer =
            deserializer.deserializeEnum(
                "SecondaryEnum",
                VARIANTS,
                object : Visitor<SecondaryEnum> {
                    override fun expecting(): String = "enum SecondaryEnum"

                    override fun <A> visitEnum(access: A): SerdeResult<SecondaryEnum>
                        where A : EnumAccess =
                        serdeCatching {
                            val (variantName, variantAccess) =
                                access
                                    .variantSeed(seedFromDeserialize(StringDeserialize))
                                    .getOrThrow()
                            when (variantName) {
                                "Secondary" -> {
                                    val label =
                                        variantAccess
                                            .newtypeVariantSeed(seedFromDeserialize(StringDeserialize))
                                            .getOrThrow()
                                    Secondary(label)
                                }

                                "Other" -> {
                                    variantAccess.unitVariant().getOrThrow()
                                    Other
                                }

                                else -> {
                                    SerdeError.unknownVariant(variantName, VARIANTS).raise()
                                }
                            }
                        }
                },
            )
    }
}

private data class FlattenedCountStruct(
    val count: Int,
) {
    companion object : Deserialize<FlattenedCountStruct> {
        val FIELDS: List<String> = listOf("count")

        override fun <D> deserialize(deserializer: D): SerdeResult<FlattenedCountStruct>
            where D : Deserializer =
            deserializer.deserializeStruct(
                "FlattenedCountStruct",
                FIELDS,
                object : Visitor<FlattenedCountStruct> {
                    override fun expecting(): String = "struct FlattenedCountStruct"

                    override fun <M> visitMap(access: M): SerdeResult<FlattenedCountStruct>
                        where M : MapAccess =
                        serdeCatching {
                            var count: Int? = null
                            while (true) {
                                val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                                when (key) {
                                    "count" -> {
                                        count = access.nextValue(I32Deserialize).getOrThrow()
                                    }

                                    else -> {
                                        access.nextValue(IgnoredAny).getOrThrow()
                                    }
                                }
                            }
                            FlattenedCountStruct(count ?: SerdeError.missingField("count").raise())
                        }
                },
            )
    }
}

private data class NewtypeEnumWrapper(
    val inner: ExternallyTaggedDeEnum,
) {
    companion object : Deserialize<NewtypeEnumWrapper> {
        private val visitor =
            object : Visitor<NewtypeEnumWrapper> {
                override fun expecting(): String = "newtype NewtypeEnumWrapper"

                override fun <InnerD> visitNewtypeStruct(deserializer: InnerD): SerdeResult<NewtypeEnumWrapper>
                        where InnerD : Deserializer =
                    ExternallyTaggedDeEnum.deserialize(deserializer).map(::NewtypeEnumWrapper)
            }

        override fun <D> deserialize(deserializer: D): SerdeResult<NewtypeEnumWrapper>
            where D : Deserializer =
            deserializer.deserializeNewtypeStruct(
                "NewtypeEnumWrapper",
                visitor,
            )
    }
}

private fun <M : MapAccess> collectFlatMapBuffer(access: M): SerdeResult<FlatMapBuffer> =
    serdeCatching {
        val buffer = FlatMapBuffer()
        while (true) {
            val key = access.nextKeySeed(ContentVisitor.new()).getOrThrow() ?: break
            val value = access.nextValueSeed(ContentVisitor.new()).getOrThrow()
            buffer.add(key, value)
        }
        buffer
    }

private fun denyRemainingUnknownFields(buffer: FlatMapBuffer): SerdeResult<Unit> =
    serdeCatching {
        for (index in 0 until buffer.size) {
            val entry = buffer.get(index) ?: continue
            val keyName = contentAsStr(entry.key)
            if (keyName != null) {
                SerdeError.custom("unknown field `$keyName`").raise()
            } else {
                SerdeError.custom("unexpected map key").raise()
            }
        }
    }

class FlatMapDeserializerTest {
    @Test
    fun recognizedEnumEntryConsumedOnceAcrossExternallyTaggedVariantsAndTrailingFlattenMap() {
        val extra = mapOf("extra_key" to "extra value")

        val unitValue = FlattenedEnumWithExtra(ExternallyTaggedDeEnum.Unit, extra)
        assertTokens(
            unitValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("Unit"),
                Token.UnitValue,
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )
        assertDeTokens(
            unitValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.Str("Unit"),
                Token.UnitValue,
                Token.MapEnd,
            ),
        )

        val newtypeValue =
            FlattenedEnumWithExtra(
                ExternallyTaggedDeEnum.Newtype(mapOf("key" to "value")),
                extra,
            )
        assertTokens(
            newtypeValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("Newtype"),
                Token.Map(1),
                Token.Str("key"),
                Token.Str("value"),
                Token.MapEnd,
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )
        assertDeTokens(
            newtypeValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.Str("Newtype"),
                Token.Map(1),
                Token.Str("key"),
                Token.Str("value"),
                Token.MapEnd,
                Token.MapEnd,
            ),
        )

        val tupleValue = FlattenedEnumWithExtra(ExternallyTaggedDeEnum.Tuple(0u, 42u), extra)
        assertTokens(
            tupleValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("Tuple"),
                Token.Seq(2),
                Token.U32(0u),
                Token.U32(42u),
                Token.SeqEnd,
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )
        assertDeTokens(
            tupleValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.Str("Tuple"),
                Token.Seq(2),
                Token.U32(0u),
                Token.U32(42u),
                Token.SeqEnd,
                Token.MapEnd,
            ),
        )

        val structValue = FlattenedEnumWithExtra(ExternallyTaggedDeEnum.Struct(index = 0u, value = 42u), extra)
        assertDeTokens(
            structValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("Struct"),
                Token.Seq(2),
                Token.U32(0u),
                Token.U32(42u),
                Token.SeqEnd,
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )
        assertDeTokens(
            structValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.Str("Struct"),
                Token.Seq(2),
                Token.U32(0u),
                Token.U32(42u),
                Token.SeqEnd,
                Token.MapEnd,
            ),
        )
        assertTokens(
            structValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("Struct"),
                Token.Struct("Struct", 2),
                Token.Str("index"),
                Token.U32(0u),
                Token.Str("value"),
                Token.U32(42u),
                Token.StructEnd,
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )
        assertDeTokens(
            structValue,
            FlattenedEnumWithExtra,
            listOf(
                Token.Map(null),
                Token.Str("extra_key"),
                Token.Str("extra value"),
                Token.Str("Struct"),
                Token.Struct("Struct", 2),
                Token.Str("index"),
                Token.U32(0u),
                Token.Str("value"),
                Token.U32(42u),
                Token.StructEnd,
                Token.MapEnd,
            ),
        )

        assertDeTokens(
            StrictDenyUnknownFlattenEnum(ExternallyTaggedDeEnum.Tuple(1u, 9u)),
            StrictDenyUnknownFlattenEnum,
            listOf(
                Token.Map(null),
                Token.Str("Tuple"),
                Token.Seq(2),
                Token.U32(1u),
                Token.U32(9u),
                Token.SeqEnd,
                Token.MapEnd,
            ),
        )
        assertDeTokensError(
            StrictDenyUnknownFlattenEnum,
            listOf(
                Token.Map(null),
                Token.Str("Tuple"),
                Token.Seq(2),
                Token.U32(1u),
                Token.U32(9u),
                Token.SeqEnd,
                Token.Str("leftover"),
                Token.U32(5u),
                Token.MapEnd,
            ),
            "unknown field `leftover`",
        )

        assertDeTokens(
            TwoOptionalFlattenedEnums(
                first = ExternallyTaggedDeEnum.Tuple(3u, 4u),
                second = null,
            ),
            TwoOptionalFlattenedEnums,
            listOf(
                Token.Map(null),
                Token.Str("Tuple"),
                Token.Seq(2),
                Token.U32(3u),
                Token.U32(4u),
                Token.SeqEnd,
                Token.MapEnd,
            ),
        )
        assertDeTokens(
            TwoOptionalFlattenedEnums(
                first = ExternallyTaggedDeEnum.Tuple(3u, 4u),
                second = ExternallyTaggedDeEnum.Unit,
            ),
            TwoOptionalFlattenedEnums,
            listOf(
                Token.Map(null),
                Token.Str("Tuple"),
                Token.Seq(2),
                Token.U32(3u),
                Token.U32(4u),
                Token.SeqEnd,
                Token.Str("Unit"),
                Token.UnitValue,
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun unmatchedEntriesRetainedInBufferForSubsequentFlattenedEnumStructAndMapFields() {
        val buffer = FlatMapBuffer()
        buffer.add(newStringContent("unmatched_head"), newStringContent("keep_head"))
        buffer.add(newU32Content(99u), newStringContent("non_string_key_value"))
        buffer.add(
            newStringContent("Tuple"),
            newSeqContent(listOf(newU32Content(10u), newU32Content(20u))),
        )
        val secondaryBytes = "Secondary".encodeToByteArray()
        buffer.add(
            newBytesContent(SharedByteSlice.borrow(secondaryBytes, 0, secondaryBytes.size)),
            newStringContent("from_utf8_bytes"),
        )
        buffer.add(newStringContent("count"), newI32Content(77))
        buffer.add(newStringContent("unmatched_tail"), newStringContent("keep_tail"))

        val firstEnum = ExternallyTaggedDeEnum.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
        assertEquals(ExternallyTaggedDeEnum.Tuple(10u, 20u), firstEnum)
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNull(buffer.get(2))
        assertNotNull(buffer.get(3))
        assertNotNull(buffer.get(4))
        assertNotNull(buffer.get(5))

        val noSecondPrimary = ExternallyTaggedDeEnum.deserialize(FlatMapDeserializer(buffer))
        val missingError = assertIs<SerdeResult.Failure>(noSecondPrimary)
        assertEquals("no variant of enum Enum found in flattened data", missingError.error.message)
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(3))
        assertNotNull(buffer.get(4))
        assertNotNull(buffer.get(5))

        val secondEnum = SecondaryEnum.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
        assertEquals(SecondaryEnum.Secondary("from_utf8_bytes"), secondEnum)
        assertNull(buffer.get(3))
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(4))
        assertNotNull(buffer.get(5))

        val structVal = FlattenedCountStruct.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
        assertEquals(FlattenedCountStruct(77), structVal)
        assertNull(buffer.get(4))
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(5))

        val remainingPairs =
            FlatMapDeserializer(buffer)
                .deserializeMap(
                    object : Visitor<List<Pair<String, String>>> {
                        override fun expecting(): String = "remaining flattened map"

                        override fun <M> visitMap(access: M): SerdeResult<List<Pair<String, String>>>
                            where M : MapAccess =
                            serdeCatching {
                                val pairs = ArrayList<Pair<String, String>>()
                                while (true) {
                                    val keyContent = access.nextKeySeed(ContentVisitor.new()).getOrThrow() ?: break
                                    val valStr = access.nextValue(StringDeserialize).getOrThrow()
                                    val renderedKey =
                                        contentAsStr(keyContent)
                                            ?: U32Deserialize.deserialize(ContentDeserializer.new(keyContent)).getOrThrow().toString()
                                    pairs.add(renderedKey to valStr)
                                }
                                pairs
                            }
                    },
                ).getOrThrow()

        assertEquals(
            listOf(
                "unmatched_head" to "keep_head",
                "99" to "non_string_key_value",
                "unmatched_tail" to "keep_tail",
            ),
            remainingPairs,
        )
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(5))
    }

    @Test
    fun visitorAndVariantAccessFailuresStillConsumeClaimedEnumEntryAsUpstreamDoes() {
        val directVisitorFailureBuffer = FlatMapBuffer()
        directVisitorFailureBuffer.add(newStringContent("Unit"), Content.Unit)
        directVisitorFailureBuffer.add(
            newStringContent("Tuple"),
            newSeqContent(listOf(newU32Content(5u), newU32Content(6u))),
        )
        directVisitorFailureBuffer.add(newStringContent("keep"), newStringContent("preserved"))

        val directFailure =
            assertIs<SerdeResult.Failure>(
                FlatMapDeserializer(directVisitorFailureBuffer).deserializeEnum(
                    "Enum",
                    ExternallyTaggedDeEnum.VARIANTS,
                    object : Visitor<String> {
                        override fun expecting(): String = "failing enum visitor"

                        override fun <A> visitEnum(access: A): SerdeResult<String>
                            where A : EnumAccess =
                            SerdeResult.failure(SerdeError.custom("visitor rejected enum immediately"))
                    },
                ),
            )
        assertEquals("visitor rejected enum immediately", directFailure.error.message)
        assertNull(directVisitorFailureBuffer.get(0))
        assertNotNull(directVisitorFailureBuffer.get(1))
        assertNotNull(directVisitorFailureBuffer.get(2))

        val nextVariant =
            ExternallyTaggedDeEnum
                .deserialize(FlatMapDeserializer(directVisitorFailureBuffer))
                .getOrThrow()
        assertEquals(ExternallyTaggedDeEnum.Tuple(5u, 6u), nextVariant)
        assertNull(directVisitorFailureBuffer.get(1))
        assertNotNull(directVisitorFailureBuffer.get(2))

        val seedFailureBuffer = FlatMapBuffer()
        seedFailureBuffer.add(newStringContent("Newtype"), newStringContent("ignored"))
        seedFailureBuffer.add(newStringContent("after"), newStringContent("still_present"))

        val seedFailure =
            assertIs<SerdeResult.Failure>(
                FlatMapDeserializer(seedFailureBuffer).deserializeEnum(
                    "Enum",
                    ExternallyTaggedDeEnum.VARIANTS,
                    object : Visitor<String> {
                        override fun expecting(): String = "failing variantSeed visitor"

                        override fun <A> visitEnum(access: A): SerdeResult<String>
                            where A : EnumAccess =
                            serdeCatching {
                                val (tag, variantAccess) =
                                    access
                                        .variantSeed(
                                            object : DeserializeSeed<String> {
                                                override fun <D> deserialize(deserializer: D): SerdeResult<String>
                                                    where D : Deserializer =
                                                    SerdeResult.failure(SerdeError.custom("variantSeed failed"))
                                            },
                                        ).getOrThrow()
                                variantAccess.unitVariant().getOrThrow()
                                tag
                            }
                    },
                ),
            )
        assertEquals("variantSeed failed", seedFailure.error.message)
        assertNull(seedFailureBuffer.get(0))
        assertNotNull(seedFailureBuffer.get(1))

        val payloadFailureBuffer = FlatMapBuffer()
        payloadFailureBuffer.add(
            newStringContent("Tuple"),
            newSeqContent(listOf(newU32Content(1u), newStringContent("not_a_u32"))),
        )
        payloadFailureBuffer.add(newStringContent("Struct"), newBoolContent(true))
        payloadFailureBuffer.add(newStringContent("extra"), newStringContent("untouched"))

        val badTupleResult = ExternallyTaggedDeEnum.deserialize(FlatMapDeserializer(payloadFailureBuffer))
        assertIs<SerdeResult.Failure>(badTupleResult)
        assertNull(payloadFailureBuffer.get(0))
        assertNotNull(payloadFailureBuffer.get(1))
        assertNotNull(payloadFailureBuffer.get(2))

        val badStructResult = ExternallyTaggedDeEnum.deserialize(FlatMapDeserializer(payloadFailureBuffer))
        val badStructFailure = assertIs<SerdeResult.Failure>(badStructResult)
        assertEquals(
            "invalid type: boolean `true`, expected struct variant",
            badStructFailure.error.message,
        )
        assertNull(payloadFailureBuffer.get(1))
        assertNotNull(payloadFailureBuffer.get(2))

        val exhaustedResult = ExternallyTaggedDeEnum.deserialize(FlatMapDeserializer(payloadFailureBuffer))
        val exhaustedFailure = assertIs<SerdeResult.Failure>(exhaustedResult)
        assertEquals("no variant of enum Enum found in flattened data", exhaustedFailure.error.message)
        assertNotNull(payloadFailureBuffer.get(2))
    }

    @Test
    fun existingMapBorrowingAndStructConsumptionBehaviorRemainsUnchanged() {
        val buffer = FlatMapBuffer()
        buffer.add(newStringContent("count"), newI32Content(12))
        buffer.add(newStringContent("Unit"), Content.Unit)
        buffer.add(newStringContent("extra"), newStringContent("borrowed"))

        val stringMapVisitor =
            object : Visitor<List<String>> {
                override fun expecting(): String = "borrowed keys"

                override fun <M> visitMap(access: M): SerdeResult<List<String>>
                    where M : MapAccess =
                    serdeCatching {
                        val keys = ArrayList<String>()
                        while (true) {
                            val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                            access.nextValue(IgnoredAny).getOrThrow()
                            keys.add(key)
                        }
                        keys
                    }
            }

        val borrowedViaMap = FlatMapDeserializer(buffer).deserializeMap(stringMapVisitor).getOrThrow()
        assertEquals(listOf("count", "Unit", "extra"), borrowedViaMap)
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(2))

        val borrowedViaAny = FlatMapDeserializer(buffer).deserializeAny(stringMapVisitor).getOrThrow()
        assertEquals(listOf("count", "Unit", "extra"), borrowedViaAny)
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(2))

        val structResult = FlattenedCountStruct.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
        assertEquals(FlattenedCountStruct(12), structResult)
        assertNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(2))

        val borrowedAfterStruct = FlatMapDeserializer(buffer).deserializeMap(stringMapVisitor).getOrThrow()
        assertEquals(listOf("Unit", "extra"), borrowedAfterStruct)

        val wrappedEnum = NewtypeEnumWrapper.deserialize(FlatMapDeserializer(buffer)).getOrThrow()
        assertEquals(NewtypeEnumWrapper(ExternallyTaggedDeEnum.Unit), wrappedEnum)
        assertNull(buffer.get(1))
        assertNotNull(buffer.get(2))

        val borrowedAfterEnum = FlatMapDeserializer(buffer).deserializeMap(stringMapVisitor).getOrThrow()
        assertEquals(listOf("extra"), borrowedAfterEnum)
        assertEquals(
            mapOf("extra" to "borrowed"),
            mapDeserialize(StringDeserialize, StringDeserialize)
                .deserialize(FlatMapDeserializer(buffer))
                .getOrThrow(),
        )
        assertNotNull(buffer.get(2))

        assertEquals(Unit, UnitDeserialize.deserialize(FlatMapDeserializer(buffer)).getOrThrow())
        assertEquals(IgnoredAny, IgnoredAny.deserialize(FlatMapDeserializer(buffer)).getOrThrow())
        val unsupportedFailure =
            assertIs<SerdeResult.Failure>(
                U32Deserialize.deserialize(FlatMapDeserializer(buffer)),
            )
        assertEquals("can only flatten structs and maps", unsupportedFailure.error.message)
    }
}
