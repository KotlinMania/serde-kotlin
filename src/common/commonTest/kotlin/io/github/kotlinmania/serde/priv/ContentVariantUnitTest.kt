// port-lint: tests test_suite/tests/test_annotations.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.BooleanDeserialize
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.U32Deserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.seedFromDeserialize
import io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry
import io.github.kotlinmania.serdecore.priv.contentaccess.newBoolContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newByteBufContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newBytesContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newCharContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newF64Content
import io.github.kotlinmania.serdecore.priv.contentaccess.newI32Content
import io.github.kotlinmania.serdecore.priv.contentaccess.newMapContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newNewtypeContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newSeqContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newSomeContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStrContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStringContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newU32Content
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokensError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private sealed class VariantUnitEnum {
    data object Unit : VariantUnitEnum()

    data class Newtype(
        val text: String,
    ) : VariantUnitEnum()

    data class Tuple(
        val code: UInt,
        val label: String,
    ) : VariantUnitEnum()

    data class Struct(
        val index: UInt,
        val active: Boolean,
    ) : VariantUnitEnum()

    companion object : Deserialize<VariantUnitEnum> {
        val VARIANTS: List<String> = listOf("Unit", "Newtype", "Tuple", "Struct")

        override fun <D> deserialize(deserializer: D): SerdeResult<VariantUnitEnum>
            where D : Deserializer =
            deserializer.deserializeEnum("VariantUnitEnum", VARIANTS, VariantUnitEnumVisitor)
    }
}

private enum class VariantUnitTag {
    Unit,
    Newtype,
    Tuple,
    Struct,
}

private data object VariantUnitTagSeed : DeserializeSeed<VariantUnitTag> {
    override fun <D> deserialize(deserializer: D): SerdeResult<VariantUnitTag>
        where D : Deserializer =
        deserializer.deserializeIdentifier(
            object : Visitor<VariantUnitTag> {
                override fun expecting(): String = "variant identifier"

                override fun visitStr(v: String): SerdeResult<VariantUnitTag> =
                    when (v) {
                        "Unit" -> {
                            SerdeResult.success(VariantUnitTag.Unit)
                        }

                        "Newtype" -> {
                            SerdeResult.success(VariantUnitTag.Newtype)
                        }

                        "Tuple" -> {
                            SerdeResult.success(VariantUnitTag.Tuple)
                        }

                        "Struct" -> {
                            SerdeResult.success(VariantUnitTag.Struct)
                        }

                        else -> {
                            SerdeResult.failure(
                                SerdeError.unknownVariant(v, VariantUnitEnum.VARIANTS),
                            )
                        }
                    }
            },
        )
}

private data object VariantUnitEnumVisitor : Visitor<VariantUnitEnum> {
    override fun expecting(): String = "enum VariantUnitEnum"

    override fun <A> visitEnum(access: A): SerdeResult<VariantUnitEnum>
        where A : EnumAccess =
        serdeCatching {
            val (tag, variantAccess) = access.variantSeed(VariantUnitTagSeed).getOrThrow()
            when (tag) {
                VariantUnitTag.Unit -> {
                    variantAccess.unitVariant().getOrThrow()
                    VariantUnitEnum.Unit
                }

                VariantUnitTag.Newtype -> {
                    val text =
                        variantAccess
                            .newtypeVariantSeed(seedFromDeserialize(StringDeserialize))
                            .getOrThrow()
                    VariantUnitEnum.Newtype(text)
                }

                VariantUnitTag.Tuple -> {
                    variantAccess
                        .tupleVariant(2, VariantUnitTupleVisitor)
                        .getOrThrow()
                }

                VariantUnitTag.Struct -> {
                    variantAccess
                        .structVariant(listOf("index", "active"), VariantUnitStructVisitor)
                        .getOrThrow()
                }
            }
        }
}

private data object VariantUnitTupleVisitor : Visitor<VariantUnitEnum> {
    override fun expecting(): String = "tuple variant VariantUnitEnum::Tuple"

    override fun <S> visitSeq(access: S): SerdeResult<VariantUnitEnum>
        where S : SeqAccess =
        serdeCatching {
            val code =
                when (val elem = access.nextElement(U32Deserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(0, this@VariantUnitTupleVisitor).raise()
                }
            val label =
                when (val elem = access.nextElement(StringDeserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(1, this@VariantUnitTupleVisitor).raise()
                }
            VariantUnitEnum.Tuple(code = code, label = label)
        }
}

private data object VariantUnitStructVisitor : Visitor<VariantUnitEnum> {
    override fun expecting(): String = "struct variant VariantUnitEnum::Struct"

    override fun <S> visitSeq(access: S): SerdeResult<VariantUnitEnum>
        where S : SeqAccess =
        serdeCatching {
            val index =
                when (val elem = access.nextElement(U32Deserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(0, this@VariantUnitStructVisitor).raise()
                }
            val active =
                when (val elem = access.nextElement(BooleanDeserialize).getOrThrow()) {
                    is SeqElement.Some -> elem.value
                    SeqElement.None -> SerdeError.invalidLength(1, this@VariantUnitStructVisitor).raise()
                }
            VariantUnitEnum.Struct(index = index, active = active)
        }

    override fun <M> visitMap(access: M): SerdeResult<VariantUnitEnum>
        where M : MapAccess =
        serdeCatching {
            var index: UInt? = null
            var active: Boolean? = null
            while (true) {
                val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                when (key) {
                    "index" -> {
                        if (index != null) {
                            SerdeError.duplicateField("index").raise()
                        }
                        index = access.nextValue(U32Deserialize).getOrThrow()
                    }

                    "active" -> {
                        if (active != null) {
                            SerdeError.duplicateField("active").raise()
                        }
                        active = access.nextValue(BooleanDeserialize).getOrThrow()
                    }

                    else -> {
                        access.nextValue(IgnoredAny).getOrThrow()
                    }
                }
            }
            VariantUnitEnum.Struct(
                index = index ?: SerdeError.missingField("index").raise(),
                active = active ?: SerdeError.missingField("active").raise(),
            )
        }
}

private data class FlattenedUnitEnumHolder(
    val inner: VariantUnitEnum,
) {
    companion object : Deserialize<FlattenedUnitEnumHolder> {
        override fun <D> deserialize(deserializer: D): SerdeResult<FlattenedUnitEnumHolder>
            where D : Deserializer =
            deserializer.deserializeMap(
                object : Visitor<FlattenedUnitEnumHolder> {
                    override fun expecting(): String = "struct FlattenedUnitEnumHolder"

                    override fun <M> visitMap(access: M): SerdeResult<FlattenedUnitEnumHolder>
                        where M : MapAccess =
                        serdeCatching {
                            val buffer = FlatMapBuffer()
                            while (true) {
                                val k = access.nextKeySeed(ContentVisitor.new()).getOrThrow() ?: break
                                val v = access.nextValueSeed(ContentVisitor.new()).getOrThrow()
                                buffer.add(k, v)
                            }
                            FlattenedUnitEnumHolder(
                                VariantUnitEnum.deserialize(FlatMapDeserializer(buffer)).getOrThrow(),
                            )
                        }
                },
            )
    }
}

private fun enumMapContent(
    variantName: String,
    payload: Content,
): Content.Map =
    newMapContent(
        listOf(
            ContentMapEntry(
                key = newStrContent(variantName),
                value = payload,
            ),
        ),
    )

class ContentVariantUnitTest {
    @Test
    fun ownedAndBorrowedUnitVariantAcceptsExplicitUnitAbsentPayloadAndPreservesEmptyMapAsymmetry() {
        for (absentTag in listOf(newStrContent("Unit"), newStringContent("Unit"))) {
            assertEquals(
                VariantUnitEnum.Unit,
                VariantUnitEnum.deserialize(ContentDeserializer.new(absentTag)).getOrThrow(),
            )
            assertEquals(
                VariantUnitEnum.Unit,
                VariantUnitEnum.deserialize(ContentRefDeserializer.new(absentTag)).getOrThrow(),
            )
        }

        assertEquals(
            VariantUnitEnum.Unit,
            VariantUnitEnumVisitor.visitEnum(EnumDeserializer.new(newStrContent("Unit"), null)).getOrThrow(),
        )

        val explicitUnitMap = enumMapContent("Unit", Content.Unit)
        assertEquals(
            VariantUnitEnum.Unit,
            VariantUnitEnum.deserialize(ContentDeserializer.new(explicitUnitMap)).getOrThrow(),
        )
        assertEquals(
            VariantUnitEnum.Unit,
            VariantUnitEnum.deserialize(ContentRefDeserializer.new(explicitUnitMap)).getOrThrow(),
        )

        val emptyPayloadMap = enumMapContent("Unit", newMapContent(emptyList()))
        assertEquals(
            VariantUnitEnum.Unit,
            VariantUnitEnum.deserialize(ContentDeserializer.new(emptyPayloadMap)).getOrThrow(),
        )
        val borrowedEmptyMapFailure =
            assertIs<SerdeResult.Failure>(
                VariantUnitEnum.deserialize(ContentRefDeserializer.new(emptyPayloadMap)),
            )
        assertEquals("invalid type: map, expected unit", borrowedEmptyMapFailure.error.message)
    }

    @Test
    fun ownedContentDeserializerUnitVariantRejectsNonUnitPayloadsWithUpstreamDiagnostics() {
        val rejectedPayloads =
            listOf(
                newBoolContent(true) to "invalid type: boolean `true`, expected unit",
                newI32Content(-7) to "invalid type: integer `-7`, expected unit",
                newU32Content(42u) to "invalid type: integer `42`, expected unit",
                newF64Content(1.5) to "invalid type: floating point `1.5`, expected unit",
                newCharContent('z'.code) to "invalid type: character `z`, expected unit",
                newStrContent("bad") to "invalid type: string \"bad\", expected unit",
                newStringContent("owned_bad") to "invalid type: string \"owned_bad\", expected unit",
                newBytesContent(byteArrayOf(1, 2)) to "invalid type: byte array, expected unit",
                newByteBufContent(byteArrayOf(3, 4)) to "invalid type: byte array, expected unit",
                newSeqContent(emptyList()) to "invalid type: sequence, expected unit",
                newSeqContent(listOf(newU32Content(1u))) to "invalid type: sequence, expected unit",
                newMapContent(
                    listOf(ContentMapEntry(newStrContent("k"), newStrContent("v"))),
                ) to "invalid type: map, expected unit",
                newNewtypeContent(Content.Unit) to "invalid type: newtype struct, expected unit",
                newSomeContent(Content.Unit) to "invalid type: Option value, expected unit",
                Content.None to "invalid type: Option value, expected unit",
            )

        for ((payload, expectedMessage) in rejectedPayloads) {
            val result =
                VariantUnitEnum.deserialize(
                    ContentDeserializer.new(enumMapContent("Unit", payload)),
                )
            val failure = assertIs<SerdeResult.Failure>(result)
            assertEquals(expectedMessage, failure.error.message)
        }

        assertDeTokensError(
            FlattenedUnitEnumHolder,
            listOf(
                Token.Map(null),
                Token.Str("Unit"),
                Token.I32(123),
                Token.MapEnd,
            ),
            "invalid type: integer `123`, expected unit",
        )
        assertDeTokensError(
            FlattenedUnitEnumHolder,
            listOf(
                Token.Map(null),
                Token.Str("Unit"),
                Token.Seq(1),
                Token.Bool(false),
                Token.SeqEnd,
                Token.MapEnd,
            ),
            "invalid type: sequence, expected unit",
        )
    }

    @Test
    fun borrowedContentRefDeserializerUnitVariantRejectsNonUnitPayloadsIncludingEmptyMap() {
        val rejectedPayloads =
            listOf(
                newBoolContent(false) to "invalid type: boolean `false`, expected unit",
                newI32Content(-99) to "invalid type: integer `-99`, expected unit",
                newU32Content(7u) to "invalid type: integer `7`, expected unit",
                newF64Content(2.25) to "invalid type: floating point `2.25`, expected unit",
                newCharContent('q'.code) to "invalid type: character `q`, expected unit",
                newStrContent("borrowed_bad") to "invalid type: string \"borrowed_bad\", expected unit",
                newStringContent("string_bad") to "invalid type: string \"string_bad\", expected unit",
                newBytesContent(byteArrayOf(9)) to "invalid type: byte array, expected unit",
                newByteBufContent(byteArrayOf(8)) to "invalid type: byte array, expected unit",
                newSeqContent(emptyList()) to "invalid type: sequence, expected unit",
                newSeqContent(listOf(newStrContent("item"))) to "invalid type: sequence, expected unit",
                newMapContent(emptyList()) to "invalid type: map, expected unit",
                newMapContent(
                    listOf(ContentMapEntry(newStrContent("k"), newU32Content(1u))),
                ) to "invalid type: map, expected unit",
                newNewtypeContent(Content.Unit) to "invalid type: newtype struct, expected unit",
                newSomeContent(Content.Unit) to "invalid type: Option value, expected unit",
                Content.None to "invalid type: Option value, expected unit",
            )

        for ((payload, expectedMessage) in rejectedPayloads) {
            val result =
                VariantUnitEnum.deserialize(
                    ContentRefDeserializer.new(enumMapContent("Unit", payload)),
                )
            val failure = assertIs<SerdeResult.Failure>(result)
            assertEquals(expectedMessage, failure.error.message)
        }
    }

    @Test
    fun legitimateNonUnitEnumVariantsRemainUnaffectedAcrossOwnedAndBorrowedPaths() {
        val newtypeContent = enumMapContent("Newtype", newStringContent("payload"))
        val tupleContent =
            enumMapContent(
                "Tuple",
                newSeqContent(listOf(newU32Content(11u), newStrContent("ok"))),
            )
        val structMapContent =
            enumMapContent(
                "Struct",
                newMapContent(
                    listOf(
                        ContentMapEntry(newStrContent("index"), newU32Content(5u)),
                        ContentMapEntry(newStrContent("active"), newBoolContent(true)),
                    ),
                ),
            )
        val structSeqContent =
            enumMapContent(
                "Struct",
                newSeqContent(listOf(newU32Content(9u), newBoolContent(false))),
            )

        for (deserializerFactory in listOf<(Content) -> Deserializer>(
            { ContentDeserializer.new(it) },
            { ContentRefDeserializer.new(it) },
        )) {
            assertEquals(
                VariantUnitEnum.Newtype("payload"),
                VariantUnitEnum.deserialize(deserializerFactory(newtypeContent)).getOrThrow(),
            )
            assertEquals(
                VariantUnitEnum.Tuple(11u, "ok"),
                VariantUnitEnum.deserialize(deserializerFactory(tupleContent)).getOrThrow(),
            )
            assertEquals(
                VariantUnitEnum.Struct(index = 5u, active = true),
                VariantUnitEnum.deserialize(deserializerFactory(structMapContent)).getOrThrow(),
            )
            assertEquals(
                VariantUnitEnum.Struct(index = 9u, active = false),
                VariantUnitEnum.deserialize(deserializerFactory(structSeqContent)).getOrThrow(),
            )

            val missingNewtype =
                assertIs<SerdeResult.Failure>(
                    VariantUnitEnum.deserialize(deserializerFactory(newStrContent("Newtype"))),
                )
            assertEquals("invalid type: unit variant, expected newtype variant", missingNewtype.error.message)

            val missingTuple =
                assertIs<SerdeResult.Failure>(
                    VariantUnitEnum.deserialize(deserializerFactory(newStrContent("Tuple"))),
                )
            assertEquals("invalid type: unit variant, expected tuple variant", missingTuple.error.message)

            val missingStruct =
                assertIs<SerdeResult.Failure>(
                    VariantUnitEnum.deserialize(deserializerFactory(newStrContent("Struct"))),
                )
            assertEquals("invalid type: unit variant, expected struct variant", missingStruct.error.message)
        }
    }
}
