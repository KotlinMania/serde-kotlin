// port-lint: tests test_suite/tests/test_annotations.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

private data class ByteVal(
    val value: Byte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class ShortVal(
    val value: Short,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class LongVal(
    val value: Long,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class UByteVal(
    val value: UByte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class UShortVal(
    val value: UShort,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class FloatVal(
    val value: Float,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class DoubleVal(
    val value: Double,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class CharVal(
    val value: Char,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private class BytesVal(
    private val value: ByteArray,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class StringVal(
    val value: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class UIntVal(
    val value: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class ULongVal(
    val value: ULong,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class IntVal(
    val value: Int,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class BoolVal(
    val value: Boolean,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data object UnitVal : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeUnit()
}

private data object UnitStructVal : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeUnitStruct("Unit")
}

private data class OptionVal<T : Serialize>(
    val value: T?,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = value.serialize(serializer)
}

private data class NewtypeWrapper<T : Serialize>(
    val inner: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeNewtypeStruct("NewtypeWrapper", inner)
}

private data class FirstFlat(
    val a: UInt,
    val b: Boolean,
    val c: List<String>,
    val d: String,
    val e: ULong?,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val state = serializer.serializeStruct("First", 5).getOrThrow()
            state.serializeField("a", UIntVal(a)).getOrThrow()
            state.serializeField("b", BoolVal(b)).getOrThrow()
            state
                .serializeField(
                    "c",
                    object : Serialize {
                        override fun <R> serialize(serializer: Serializer<R>): SerdeResult<R> = c.serialize(serializer)
                    },
                ).getOrThrow()
            state.serializeField("d", StringVal(d)).getOrThrow()
            state.serializeField("e", OptionVal(e?.let(::ULongVal))).getOrThrow()
            state.end().getOrThrow()
        }
}

private data class SecondFlat(
    val f: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val state = serializer.serializeStruct("Second", 1).getOrThrow()
            state.serializeField("f", UIntVal(f)).getOrThrow()
            state.end().getOrThrow()
        }
}

private data class ComplexOuter(
    val y: UInt,
    val first: FirstFlat,
    val second: SecondFlat,
    val z: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val map = serializer.serializeMap(null).getOrThrow()
            map.serializeEntry(StringVal("y"), UIntVal(y)).getOrThrow()
            first.serialize(FlatMapSerializer(map)).getOrThrow()
            second.serialize(FlatMapSerializer(map)).getOrThrow()
            map.serializeEntry(StringVal("z"), UIntVal(z)).getOrThrow()
            map.end().getOrThrow()
        }
}

private data class SplitKeyValueMap<K : Serialize, V : Serialize>(
    val entries: List<Pair<K, V>>,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val map = serializer.serializeMap(entries.size).getOrThrow()
            for ((k, v) in entries) {
                map.serializeKey(k).getOrThrow()
                map.serializeValue(v).getOrThrow()
            }
            map.end().getOrThrow()
        }
}

private data class EntryMap<K : Serialize, V : Serialize>(
    val entries: List<Pair<K, V>>,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val map = serializer.serializeMap(entries.size).getOrThrow()
            for ((k, v) in entries) {
                map.serializeEntry(k, v).getOrThrow()
            }
            map.end().getOrThrow()
        }
}

private sealed class ExternallyTaggedEnum : Serialize {
    data object Unit : ExternallyTaggedEnum()

    data class Newtype(
        val entries: List<Pair<StringVal, StringVal>>,
    ) : ExternallyTaggedEnum()

    data class Tuple(
        val first: UInt,
        val second: UInt,
    ) : ExternallyTaggedEnum()

    data class Struct(
        val index: UInt,
        val value: UInt,
    ) : ExternallyTaggedEnum()

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        when (this) {
            Unit -> {
                serializer.serializeUnitVariant("Enum", 0u, "Unit")
            }

            is Newtype -> {
                serializer.serializeNewtypeVariant("Enum", 1u, "Newtype", EntryMap(entries))
            }

            is Tuple -> {
                serdeCatching {
                    val tv = serializer.serializeTupleVariant("Enum", 2u, "Tuple", 2).getOrThrow()
                    tv.serializeField(UIntVal(first)).getOrThrow()
                    tv.serializeField(UIntVal(second)).getOrThrow()
                    tv.end().getOrThrow()
                }
            }

            is Struct -> {
                serdeCatching {
                    val sv = serializer.serializeStructVariant("Enum", 3u, "Struct", 2).getOrThrow()
                    sv.serializeField("index", UIntVal(index)).getOrThrow()
                    sv.serializeField("value", UIntVal(value)).getOrThrow()
                    sv.end().getOrThrow()
                }
            }
        }
}

private data class FlattenedEnumContainer(
    val data: ExternallyTaggedEnum,
    val extra: List<Pair<StringVal, StringVal>>,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val map = serializer.serializeMap(null).getOrThrow()
            data.serialize(FlatMapSerializer(map)).getOrThrow()
            EntryMap(extra).serialize(FlatMapSerializer(map)).getOrThrow()
            map.end().getOrThrow()
        }
}

private class ConcreteRecordingMap(
    private val failOnKey: String? = null,
    private val failOnValueForKey: String? = null,
) : SerializeMap<List<Pair<String, String>>> {
    private val recorded: MutableList<Pair<String, String>> = ArrayList()
    private var pendingKey: String? = null
    var ended: Boolean = false
        private set

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            val rendered = renderToText(key).getOrThrow()
            if (rendered == failOnKey) {
                return SerdeResult.failure(SerdeError.custom("rejected key $rendered"))
            }
            pendingKey = rendered
        }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            val currentKey = checkNotNull(pendingKey) { "missing pending key" }
            pendingKey = null
            if (currentKey == failOnValueForKey) {
                return SerdeResult.failure(SerdeError.custom("rejected value for $currentKey"))
            }
            val rendered = renderToText(value).getOrThrow()
            recorded.add(currentKey to rendered)
        }

    override fun end(): SerdeResult<List<Pair<String, String>>> {
        ended = true
        return SerdeResult.success(recorded.toList())
    }

    private fun <T : Serialize> renderToText(item: T): SerdeResult<String> {
        val out = StringBuilder()
        return item.serialize(FormatterSerializer(out)).map { out.toString() }
    }
}

class FlatMapSerializerTest {
    @Test
    fun concreteSerializeMapInfersTypeArgumentsWithoutExplicitErrorType() {
        val concreteMap = ConcreteRecordingMap()
        val flat = FlatMapSerializer(concreteMap)
        assertSame(concreteMap, flat.map)

        val inner = SecondFlat(42u)
        assertIs<SerdeResult.Success<Unit>>(inner.serialize(flat))
        assertFalse(concreteMap.ended)
        assertEquals(listOf("f" to "42"), concreteMap.end().getOrThrow())
    }

    @Test
    fun flattenComplexStructsPreservesFieldOrderAndOuterFields() {
        val outer =
            ComplexOuter(
                y = 0u,
                first =
                    FirstFlat(
                        a = 1u,
                        b = true,
                        c = listOf("a", "b"),
                        d = "c",
                        e = 2uL,
                    ),
                second = SecondFlat(3u),
                z = 4u,
            )

        assertSerTokens(
            outer,
            listOf(
                Token.Map(null),
                Token.Str("y"),
                Token.U32(0u),
                Token.Str("a"),
                Token.U32(1u),
                Token.Str("b"),
                Token.Bool(true),
                Token.Str("c"),
                Token.Seq(2),
                Token.Str("a"),
                Token.Str("b"),
                Token.SeqEnd,
                Token.Str("d"),
                Token.Str("c"),
                Token.Str("e"),
                Token.Some,
                Token.U64(2uL),
                Token.Str("f"),
                Token.U32(3u),
                Token.Str("z"),
                Token.U32(4u),
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun flattenMapsWithNonStringKeysAndSplitKeyValueCalls() {
        val value =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val map = serializer.serializeMap(null).getOrThrow()
                        map.serializeEntry(StringVal("name"), StringVal("peter")).getOrThrow()
                        map.serializeEntry(StringVal("age"), UIntVal(3u)).getOrThrow()
                        EntryMap(listOf(UIntVal(0u) to UIntVal(42u)))
                            .serialize(FlatMapSerializer(map))
                            .getOrThrow()
                        SplitKeyValueMap(listOf(UIntVal(7u) to UIntVal(99u)))
                            .serialize(FlatMapSerializer(map))
                            .getOrThrow()
                        map.end().getOrThrow()
                    }
            }

        assertSerTokens(
            value,
            listOf(
                Token.Map(null),
                Token.Str("name"),
                Token.Str("peter"),
                Token.Str("age"),
                Token.U32(3u),
                Token.U32(0u),
                Token.U32(42u),
                Token.U32(7u),
                Token.U32(99u),
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun flattenOptionUnitUnitStructAndNewtypeWrapper() {
        fun wrappedSecond(value: IntVal): NewtypeWrapper<Serialize> =
            NewtypeWrapper(
                object : Serialize {
                    override fun <R> serialize(serializer: Serializer<R>): SerdeResult<R> =
                        serdeCatching {
                            val st = serializer.serializeStruct("Inner2", 1).getOrThrow()
                            st.serializeField("inner2", value).getOrThrow()
                            st.end().getOrThrow()
                        }
                },
            )

        fun container(
            first: SecondFlat?,
            second: IntVal?,
        ): Serialize =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val map = serializer.serializeMap(null).getOrThrow()
                        OptionVal(first).serialize(FlatMapSerializer(map)).getOrThrow()
                        UnitVal.serialize(FlatMapSerializer(map)).getOrThrow()
                        UnitStructVal.serialize(FlatMapSerializer(map)).getOrThrow()
                        OptionVal(
                            second?.let(::wrappedSecond),
                        ).serialize(FlatMapSerializer(map)).getOrThrow()
                        map.end().getOrThrow()
                    }
            }

        assertSerTokens(
            container(SecondFlat(1u), IntVal(2)),
            listOf(
                Token.Map(null),
                Token.Str("f"),
                Token.U32(1u),
                Token.Str("inner2"),
                Token.I32(2),
                Token.MapEnd,
            ),
        )
        assertSerTokens(
            container(SecondFlat(1u), null),
            listOf(
                Token.Map(null),
                Token.Str("f"),
                Token.U32(1u),
                Token.MapEnd,
            ),
        )
        assertSerTokens(
            container(null, null),
            listOf(
                Token.Map(null),
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun flattenExternallyTaggedEnumUnitNewtypeTupleAndStructVariants() {
        val extra = listOf(StringVal("extra") to StringVal("extra value"))

        assertSerTokens(
            FlattenedEnumContainer(ExternallyTaggedEnum.Unit, extra),
            listOf(
                Token.Map(null),
                Token.Str("Unit"),
                Token.UnitValue,
                Token.Str("extra"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            FlattenedEnumContainer(
                ExternallyTaggedEnum.Newtype(listOf(StringVal("key") to StringVal("value"))),
                extra,
            ),
            listOf(
                Token.Map(null),
                Token.Str("Newtype"),
                Token.Map(1),
                Token.Str("key"),
                Token.Str("value"),
                Token.MapEnd,
                Token.Str("extra"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            FlattenedEnumContainer(ExternallyTaggedEnum.Tuple(0u, 42u), extra),
            listOf(
                Token.Map(null),
                Token.Str("Tuple"),
                Token.Seq(2),
                Token.U32(0u),
                Token.U32(42u),
                Token.SeqEnd,
                Token.Str("extra"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )

        assertSerTokens(
            FlattenedEnumContainer(ExternallyTaggedEnum.Struct(index = 0u, value = 42u), extra),
            listOf(
                Token.Map(null),
                Token.Str("Struct"),
                Token.Struct("Struct", 2),
                Token.Str("index"),
                Token.U32(0u),
                Token.Str("value"),
                Token.U32(42u),
                Token.StructEnd,
                Token.Str("extra"),
                Token.Str("extra value"),
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun flattenUnsupportedTypesMatchUpstreamErrorMessages() {
        fun outerWithFlattened(inner: Serialize): Serialize =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val map = serializer.serializeMap(null).getOrThrow()
                        map.serializeEntry(StringVal("outer"), StringVal("foo")).getOrThrow()
                        inner.serialize(FlatMapSerializer(map)).getOrThrow()
                        map.end().getOrThrow()
                    }
            }

        val prefix =
            listOf(
                Token.Map(null),
                Token.Str("outer"),
                Token.Str("foo"),
            )

        val cases: List<Pair<Serialize, String>> =
            listOf(
                BoolVal(true) to "can only flatten structs and maps (got a boolean)",
                ByteVal(1) to "can only flatten structs and maps (got an integer)",
                ShortVal(1) to "can only flatten structs and maps (got an integer)",
                IntVal(1) to "can only flatten structs and maps (got an integer)",
                LongVal(1L) to "can only flatten structs and maps (got an integer)",
                UByteVal(1u) to "can only flatten structs and maps (got an integer)",
                UShortVal(1u) to "can only flatten structs and maps (got an integer)",
                UIntVal(1u) to "can only flatten structs and maps (got an integer)",
                ULongVal(1uL) to "can only flatten structs and maps (got an integer)",
                FloatVal(1.5f) to "can only flatten structs and maps (got a float)",
                DoubleVal(1.5) to "can only flatten structs and maps (got a float)",
                CharVal('x') to "can only flatten structs and maps (got a char)",
                StringVal("bar") to "can only flatten structs and maps (got a string)",
                FormatArguments("bar") to "can only flatten structs and maps (got a string)",
                BytesVal(byteArrayOf(1, 2)) to "can only flatten structs and maps (got a byte array)",
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                        listOf(1, 2).serialize(serializer)
                } to "can only flatten structs and maps (got a sequence)",
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                        serdeCatching {
                            val tuple = serializer.serializeTuple(1).getOrThrow()
                            tuple.serializeElement(IntVal(1)).getOrThrow()
                            tuple.end().getOrThrow()
                        }
                } to "can only flatten structs and maps (got a tuple)",
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                        serdeCatching {
                            val ts = serializer.serializeTupleStruct("Pair", 1).getOrThrow()
                            ts.serializeField(IntVal(1)).getOrThrow()
                            ts.end().getOrThrow()
                        }
                } to "can only flatten structs and maps (got a tuple struct)",
            )

        for ((inner, expectedError) in cases) {
            assertSerTokensError(outerWithFlattened(inner), prefix, expectedError)
        }
    }

    @Test
    fun flattenTupleVariantBuffersNestedCompoundFieldsViaContentSeq() {
        val stringField =
            object : Serialize {
                override fun <R> serialize(serializer: Serializer<R>): SerdeResult<R> =
                    listOf("x", "y").serialize(serializer)
            }

        val nestedTupleVariant =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val tv = serializer.serializeTupleVariant("OuterEnum", 0u, "Nested", 3).getOrThrow()
                        tv.serializeField(SecondFlat(7u)).getOrThrow()
                        tv
                            .serializeField(
                                stringField,
                            ).getOrThrow()
                        tv.serializeField(OptionVal(IntVal(9))).getOrThrow()
                        tv.end().getOrThrow()
                    }
            }

        assertSerTokens(
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val map = serializer.serializeMap(null).getOrThrow()
                        nestedTupleVariant.serialize(FlatMapSerializer(map)).getOrThrow()
                        map.serializeEntry(StringVal("tail"), BoolVal(false)).getOrThrow()
                        map.end().getOrThrow()
                    }
            },
            listOf(
                Token.Map(null),
                Token.Str("Nested"),
                Token.Seq(3),
                Token.Struct("Second", 1),
                Token.Str("f"),
                Token.U32(7u),
                Token.StructEnd,
                Token.Seq(2),
                Token.Str("x"),
                Token.Str("y"),
                Token.SeqEnd,
                Token.Some,
                Token.I32(9),
                Token.SeqEnd,
                Token.Str("tail"),
                Token.Bool(false),
                Token.MapEnd,
            ),
        )
    }

    @Test
    fun flattenPropagatesSerializeMapAndFieldErrorsWithoutEndingOuterMap() {
        val failingKeyMap = ConcreteRecordingMap(failOnKey = "Tuple")
        val keyFailure =
            assertIs<SerdeResult.Failure>(
                ExternallyTaggedEnum.Tuple(1u, 2u).serialize(FlatMapSerializer(failingKeyMap)),
            )
        assertEquals("rejected key Tuple", keyFailure.error.message)
        assertFalse(failingKeyMap.ended)

        val failingTupleEndMap = ConcreteRecordingMap(failOnValueForKey = "Tuple")
        val tupleEndFailure =
            assertIs<SerdeResult.Failure>(
                ExternallyTaggedEnum.Tuple(1u, 2u).serialize(FlatMapSerializer(failingTupleEndMap)),
            )
        assertEquals("rejected value for Tuple", tupleEndFailure.error.message)
        assertFalse(failingTupleEndMap.ended)

        val failingStructEndMap = ConcreteRecordingMap(failOnValueForKey = "Struct")
        val structEndFailure =
            assertIs<SerdeResult.Failure>(
                ExternallyTaggedEnum.Struct(1u, 2u).serialize(FlatMapSerializer(failingStructEndMap)),
            )
        assertEquals("rejected value for Struct", structEndFailure.error.message)
        assertFalse(failingStructEndMap.ended)

        val failingValueMap = ConcreteRecordingMap(failOnValueForKey = "f")
        val valueFailure =
            assertIs<SerdeResult.Failure>(
                SecondFlat(9u).serialize(FlatMapSerializer(failingValueMap)),
            )
        assertEquals("rejected value for f", valueFailure.error.message)
        assertFalse(failingValueMap.ended)

        val failingSplitMapKey = ConcreteRecordingMap(failOnKey = "k")
        val splitKeyFailure =
            assertIs<SerdeResult.Failure>(
                SplitKeyValueMap(listOf(StringVal("k") to StringVal("v")))
                    .serialize(FlatMapSerializer(failingSplitMapKey)),
            )
        assertEquals("rejected key k", splitKeyFailure.error.message)
        assertFalse(failingSplitMapKey.ended)

        val failingSplitMapValue = ConcreteRecordingMap(failOnValueForKey = "k")
        val splitValueFailure =
            assertIs<SerdeResult.Failure>(
                SplitKeyValueMap(listOf(StringVal("k") to StringVal("v")))
                    .serialize(FlatMapSerializer(failingSplitMapValue)),
            )
        assertEquals("rejected value for k", splitValueFailure.error.message)
        assertFalse(failingSplitMapValue.ended)

        val failingTupleField =
            object : Serialize {
                override fun <R> serialize(serializer: Serializer<R>): SerdeResult<R> =
                    SerdeResult.failure(SerdeError.custom("boom in tuple variant field"))
            }

        val failingTupleElement =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val tv = serializer.serializeTupleVariant("Enum", 0u, "BrokenTuple", 2).getOrThrow()
                        tv.serializeField(UIntVal(1u)).getOrThrow()
                        tv
                            .serializeField(
                                failingTupleField,
                            ).getOrThrow()
                        tv.end().getOrThrow()
                    }
            }
        assertSerTokensError(
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serdeCatching {
                        val map = serializer.serializeMap(null).getOrThrow()
                        failingTupleElement.serialize(FlatMapSerializer(map)).getOrThrow()
                        map.end().getOrThrow()
                    }
            },
            listOf(
                Token.Map(null),
                Token.Str("BrokenTuple"),
            ),
            "boom in tuple variant field",
        )
    }
}
