// port-lint: tests test_suite/tests/test_annotations.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertSerTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

private class DelegateProgrammingError(
    message: String,
) : RuntimeException(message)

private class FieldCallbackBugException(
    message: String,
) : RuntimeException(message)

private data class IntField(
    val number: Int,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI32(number)
}

private data class StringField(
    val text: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(text)
}

private data class BoolField(
    val flag: Boolean,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeBool(flag)
}

private class FailingFieldSerialize(
    private val failure: SerdeResult<Nothing>,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        return failure
    }
}

private class ThrowingFieldSerialize(
    private val throwable: RuntimeException,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        throw throwable
    }
}

private sealed class DelegateStepOutcome {
    data object Proceed : DelegateStepOutcome()

    class Fail(
        val failure: SerdeResult<Nothing>,
    ) : DelegateStepOutcome()

    class ThrowBug(
        val exception: RuntimeException,
    ) : DelegateStepOutcome()
}

private class RecordingDelegateMap(
    var onKey: DelegateStepOutcome = DelegateStepOutcome.Proceed,
    var onValue: DelegateStepOutcome = DelegateStepOutcome.Proceed,
    var onEntry: DelegateStepOutcome = DelegateStepOutcome.Proceed,
    private val driveEntryValueWithNoopSerializer: Boolean = false,
) : SerializeMap<String> {
    val recordedKeys: MutableList<Any> = ArrayList()
    val recordedValues: MutableList<Any> = ArrayList()
    val recordedEntries: MutableList<Pair<Any, Any>> = ArrayList()

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize {
        recordedKeys.add(key)
        return when (val step = onKey) {
            DelegateStepOutcome.Proceed -> SerdeResult.success(Unit)
            is DelegateStepOutcome.Fail -> step.failure
            is DelegateStepOutcome.ThrowBug -> throw step.exception
        }
    }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize {
        recordedValues.add(value)
        return when (val step = onValue) {
            DelegateStepOutcome.Proceed -> SerdeResult.success(Unit)
            is DelegateStepOutcome.Fail -> step.failure
            is DelegateStepOutcome.ThrowBug -> throw step.exception
        }
    }

    override fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize {
        recordedEntries.add(key to value)
        return when (val step = onEntry) {
            DelegateStepOutcome.Proceed -> {
                if (driveEntryValueWithNoopSerializer) {
                    value.serialize(NoopScalarSerializer).map { }
                } else {
                    SerdeResult.success(Unit)
                }
            }

            is DelegateStepOutcome.Fail -> {
                step.failure
            }

            is DelegateStepOutcome.ThrowBug -> {
                throw step.exception
            }
        }
    }

    override fun end(): SerdeResult<String> = SerdeResult.success("completed")
}

private object NoopScalarSerializer : Serializer<Unit> {
    override fun serializeBool(v: Boolean): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeI8(v: Byte): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeI16(v: Short): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeI32(v: Int): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeI64(v: Long): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeU8(v: UByte): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeU16(v: UShort): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeU32(v: UInt): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeU64(v: ULong): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeF32(v: Float): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeF64(v: Double): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeChar(v: Char): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeStr(v: String): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeNone(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun <T> serializeSome(value: T): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeUnit(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeSeq(len: Int?): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeSeq<Unit>> =
        SerdeResult.failure(SerdeError.custom("unexpected seq"))

    override fun serializeTuple(len: Int): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeTuple<Unit>> =
        SerdeResult.failure(SerdeError.custom("unexpected tuple"))

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeTupleStruct<Unit>> =
        SerdeResult.failure(SerdeError.custom("unexpected tuple struct"))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeTupleVariant<Unit>> =
        SerdeResult.failure(SerdeError.custom("unexpected tuple variant"))

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> =
        SerdeResult.failure(SerdeError.custom("unexpected map"))

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeStruct<Unit>> =
        SerdeResult.failure(SerdeError.custom("unexpected struct"))

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<io.github.kotlinmania.serdecore.ser.SerializeStructVariant<Unit>> =
        SerdeResult.failure(SerdeError.custom("unexpected struct variant"))
}

private sealed class FlattenedVariantSample : Serialize {
    data object UnitCase : FlattenedVariantSample()

    data class NewtypeCase(
        val label: StringField,
    ) : FlattenedVariantSample()

    data class TupleCase(
        val first: IntField,
        val second: BoolField,
    ) : FlattenedVariantSample()

    data class StructCase(
        val id: IntField,
        val title: StringField,
    ) : FlattenedVariantSample()

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        when (this) {
            UnitCase -> {
                serializer.serializeUnitVariant("SampleEnum", 0u, "UnitCase")
            }

            is NewtypeCase -> {
                serializer.serializeNewtypeVariant("SampleEnum", 1u, "NewtypeCase", label)
            }

            is TupleCase -> {
                serdeCatching {
                    val tv = serializer.serializeTupleVariant("SampleEnum", 2u, "TupleCase", 2).getOrThrow()
                    tv.serializeField(first).getOrThrow()
                    tv.serializeField(second).getOrThrow()
                    tv.end().getOrThrow()
                }
            }

            is StructCase -> {
                serdeCatching {
                    val sv = serializer.serializeStructVariant("SampleEnum", 3u, "StructCase", 2).getOrThrow()
                    sv.serializeField("id", id).getOrThrow()
                    sv.serializeField("title", title).getOrThrow()
                    sv.end().getOrThrow()
                }
            }
        }
}

private class MultiFlattenedVariantsEnvelope(
    private val variants: List<FlattenedVariantSample>,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val map = serializer.serializeMap(null).getOrThrow()
            map.serializeEntry(StringField("header"), IntField(1)).getOrThrow()
            for (variant in variants) {
                variant.serialize(FlatMapSerializer(map)).getOrThrow()
            }
            map.serializeEntry(StringField("footer"), BoolField(true)).getOrThrow()
            map.end().getOrThrow()
        }
}

class FlatMapEnumVariantTest {
    @Test
    fun unitAndNewtypeVariantsPreserveDelegateFailureIdentityAndPropagateProgrammingExceptions() {
        val okDelegate = RecordingDelegateMap()
        FlatMapSerializer(okDelegate).serializeUnitVariant("Enum", 0u, "UnitVar").getOrThrow()
        val payload = StringField("hello")
        FlatMapSerializer(okDelegate).serializeNewtypeVariant("Enum", 1u, "NewtypeVar", payload).getOrThrow()

        assertEquals(2, okDelegate.recordedEntries.size)
        assertEquals(Content.String("UnitVar") to Content.Unit, okDelegate.recordedEntries[0])
        assertEquals(Content.String("NewtypeVar"), okDelegate.recordedEntries[1].first)
        assertSame(payload, okDelegate.recordedEntries[1].second)

        val unitBug = DelegateProgrammingError("unit delegate entry bug")
        val unitThrowDelegate = RecordingDelegateMap(onEntry = DelegateStepOutcome.ThrowBug(unitBug))
        val thrownUnitBug =
            assertFailsWith<DelegateProgrammingError> {
                FlatMapSerializer(unitThrowDelegate).serializeUnitVariant("Enum", 0u, "UnitVar")
            }
        assertSame(unitBug, thrownUnitBug)

        val unitError = SerdeError("unit entry failed", source = SerdeError.custom("unit cause"))
        val unitFailure = SerdeResult.failure(unitError)
        val unitFailDelegate = RecordingDelegateMap(onEntry = DelegateStepOutcome.Fail(unitFailure))
        val unitFailResult = FlatMapSerializer(unitFailDelegate).serializeUnitVariant("Enum", 0u, "UnitVar")
        assertSame(unitFailure, unitFailResult)
        assertSame(unitError, unitFailResult.exceptionOrNull())

        val newtypeError = SerdeError("newtype entry failed", source = SerdeError.custom("newtype cause"))
        val newtypeFailure = SerdeResult.failure(newtypeError)
        val newtypeFailDelegate = RecordingDelegateMap(onEntry = DelegateStepOutcome.Fail(newtypeFailure))
        val newtypeFailResult =
            FlatMapSerializer(newtypeFailDelegate).serializeNewtypeVariant("Enum", 1u, "NewtypeVar", payload)
        assertSame(newtypeFailure, newtypeFailResult)
        assertSame(newtypeError, newtypeFailResult.exceptionOrNull())

        val newtypeDelegateBug = DelegateProgrammingError("newtype delegate entry bug")
        val newtypeThrowDelegate = RecordingDelegateMap(onEntry = DelegateStepOutcome.ThrowBug(newtypeDelegateBug))
        val thrownNewtypeDelegateBug =
            assertFailsWith<DelegateProgrammingError> {
                FlatMapSerializer(newtypeThrowDelegate).serializeNewtypeVariant("Enum", 1u, "NewtypeVar", payload)
            }
        assertSame(newtypeDelegateBug, thrownNewtypeDelegateBug)

        val valueCallbackError = SerdeError.custom("newtype value failed")
        val valueCallbackFailure = SerdeResult.failure(valueCallbackError)
        val drivingDelegate1 = RecordingDelegateMap(driveEntryValueWithNoopSerializer = true)
        val valueFailResult =
            FlatMapSerializer(drivingDelegate1)
                .serializeNewtypeVariant("Enum", 1u, "NewtypeVar", FailingFieldSerialize(valueCallbackFailure))
        assertSame(valueCallbackFailure, valueFailResult)
        assertSame(valueCallbackError, valueFailResult.exceptionOrNull())

        val valueCallbackBug = FieldCallbackBugException("newtype value callback bug")
        val drivingDelegate2 = RecordingDelegateMap(driveEntryValueWithNoopSerializer = true)
        val thrownValueBug =
            assertFailsWith<FieldCallbackBugException> {
                FlatMapSerializer(drivingDelegate2)
                    .serializeNewtypeVariant("Enum", 1u, "NewtypeVar", ThrowingFieldSerialize(valueCallbackBug))
            }
        assertSame(valueCallbackBug, thrownValueBug)
    }

    @Test
    fun tupleAndStructVariantCreationPreservesInitialKeyFailureIdentityAndPropagatesExceptions() {
        val tupleKeyBug = DelegateProgrammingError("tuple serializeKey bug")
        val tupleThrowDelegate = RecordingDelegateMap(onKey = DelegateStepOutcome.ThrowBug(tupleKeyBug))
        val thrownTupleKeyBug =
            assertFailsWith<DelegateProgrammingError> {
                FlatMapSerializer(tupleThrowDelegate).serializeTupleVariant("Enum", 2u, "TupleVar", 2)
            }
        assertSame(tupleKeyBug, thrownTupleKeyBug)
        assertEquals(emptyList(), tupleThrowDelegate.recordedValues)

        val tupleKeyError = SerdeError("tuple key failed", source = SerdeError.custom("tuple key cause"))
        val tupleKeyFailure = SerdeResult.failure(tupleKeyError)
        val tupleFailDelegate = RecordingDelegateMap(onKey = DelegateStepOutcome.Fail(tupleKeyFailure))
        val tupleFailResult = FlatMapSerializer(tupleFailDelegate).serializeTupleVariant("Enum", 2u, "TupleVar", 2)
        assertSame(tupleKeyFailure, tupleFailResult)
        assertSame(tupleKeyError, tupleFailResult.exceptionOrNull())
        assertEquals(listOf<Any>(Content.String("TupleVar")), tupleFailDelegate.recordedKeys)
        assertEquals(emptyList(), tupleFailDelegate.recordedValues)

        val structKeyError = SerdeError("struct key failed", source = SerdeError.custom("struct key cause"))
        val structKeyFailure = SerdeResult.failure(structKeyError)
        val structFailDelegate = RecordingDelegateMap(onKey = DelegateStepOutcome.Fail(structKeyFailure))
        val structFailResult = FlatMapSerializer(structFailDelegate).serializeStructVariant("Enum", 3u, "StructVar", 2)
        assertSame(structKeyFailure, structFailResult)
        assertSame(structKeyError, structFailResult.exceptionOrNull())
        assertEquals(listOf<Any>(Content.String("StructVar")), structFailDelegate.recordedKeys)
        assertEquals(emptyList(), structFailDelegate.recordedValues)

        val structKeyBug = DelegateProgrammingError("struct serializeKey bug")
        val structThrowDelegate = RecordingDelegateMap(onKey = DelegateStepOutcome.ThrowBug(structKeyBug))
        val thrownStructKeyBug =
            assertFailsWith<DelegateProgrammingError> {
                FlatMapSerializer(structThrowDelegate).serializeStructVariant("Enum", 3u, "StructVar", 2)
            }
        assertSame(structKeyBug, thrownStructKeyBug)
        assertEquals(emptyList(), structThrowDelegate.recordedValues)
    }

    @Test
    fun tupleVariantHelperPreservesFieldAndEndFailuresDropsFailedFieldsAndDelegatesExactContentSeq() {
        val delegate = RecordingDelegateMap()
        val builder = FlatMapSerializer(delegate).serializeTupleVariant("OuterEnum", 2u, "TupleVar", 3).getOrThrow()
        assertEquals(listOf<Any>(Content.String("TupleVar")), delegate.recordedKeys)

        builder.serializeField(IntField(10)).getOrThrow()

        val fieldError = SerdeError("tuple field failed", source = SerdeError.custom("field cause"))
        val fieldFailure = SerdeResult.failure(fieldError)
        val failingField = FailingFieldSerialize(fieldFailure)
        val fieldFailResult = builder.serializeField(failingField)
        assertEquals(1, failingField.calls)
        assertSame(fieldFailure, fieldFailResult)
        assertSame(fieldError, fieldFailResult.exceptionOrNull())

        val fieldBug = FieldCallbackBugException("tuple field callback bug")
        val throwingField = ThrowingFieldSerialize(fieldBug)
        val thrownFieldBug =
            assertFailsWith<FieldCallbackBugException> {
                builder.serializeField(throwingField)
            }
        assertEquals(1, throwingField.calls)
        assertSame(fieldBug, thrownFieldBug)

        builder.serializeField(StringField("kept")).getOrThrow()
        assertEquals(emptyList(), delegate.recordedValues)

        builder.end().getOrThrow()
        assertEquals(
            listOf<Any>(Content.Seq(listOf(Content.I32(10), Content.String("kept")))),
            delegate.recordedValues,
        )

        val endError = SerdeError("tuple end failed", source = SerdeError.custom("end cause"))
        val endFailure = SerdeResult.failure(endError)
        val endFailDelegate = RecordingDelegateMap(onValue = DelegateStepOutcome.Fail(endFailure))
        val endFailBuilder =
            FlatMapSerializer(endFailDelegate).serializeTupleVariant("OuterEnum", 2u, "TupleVar", 1).getOrThrow()
        endFailBuilder.serializeField(BoolField(true)).getOrThrow()
        val endFailResult = endFailBuilder.end()
        assertSame(endFailure, endFailResult)
        assertSame(endError, endFailResult.exceptionOrNull())
        assertEquals(listOf<Any>(Content.Seq(listOf(Content.Bool(true)))), endFailDelegate.recordedValues)

        val endBug = DelegateProgrammingError("tuple end serializeValue bug")
        val endThrowDelegate = RecordingDelegateMap(onValue = DelegateStepOutcome.ThrowBug(endBug))
        val endThrowBuilder =
            FlatMapSerializer(endThrowDelegate).serializeTupleVariant("OuterEnum", 2u, "TupleVar", 1).getOrThrow()
        endThrowBuilder.serializeField(IntField(7)).getOrThrow()
        val thrownEndBug =
            assertFailsWith<DelegateProgrammingError> {
                endThrowBuilder.end()
            }
        assertSame(endBug, thrownEndBug)
    }

    @Test
    fun structVariantHelperPreservesFieldAndEndFailuresDropsFailedFieldsAndDelegatesExactContentStruct() {
        val delegate = RecordingDelegateMap()
        val builder = FlatMapSerializer(delegate).serializeStructVariant("OuterEnum", 3u, "StructVar", 3).getOrThrow()
        assertEquals(listOf<Any>(Content.String("StructVar")), delegate.recordedKeys)

        builder.serializeField("first", IntField(42)).getOrThrow()

        val fieldError = SerdeError("struct field failed", source = SerdeError.custom("struct field cause"))
        val fieldFailure = SerdeResult.failure(fieldError)
        val failingField = FailingFieldSerialize(fieldFailure)
        val fieldFailResult = builder.serializeField("droppedTyped", failingField)
        assertEquals(1, failingField.calls)
        assertSame(fieldFailure, fieldFailResult)
        assertSame(fieldError, fieldFailResult.exceptionOrNull())

        val fieldBug = FieldCallbackBugException("struct field callback bug")
        val throwingField = ThrowingFieldSerialize(fieldBug)
        val thrownFieldBug =
            assertFailsWith<FieldCallbackBugException> {
                builder.serializeField("droppedThrown", throwingField)
            }
        assertEquals(1, throwingField.calls)
        assertSame(fieldBug, thrownFieldBug)

        builder.serializeField("second", StringField("ok")).getOrThrow()
        assertEquals(emptyList(), delegate.recordedValues)

        builder.end().getOrThrow()
        assertEquals(
            listOf<Any>(
                Content.Struct(
                    "StructVar",
                    listOf(
                        "first" to Content.I32(42),
                        "second" to Content.String("ok"),
                    ),
                ),
            ),
            delegate.recordedValues,
        )

        val endError = SerdeError("struct end failed", source = SerdeError.custom("struct end cause"))
        val endFailure = SerdeResult.failure(endError)
        val endFailDelegate = RecordingDelegateMap(onValue = DelegateStepOutcome.Fail(endFailure))
        val endFailBuilder =
            FlatMapSerializer(endFailDelegate).serializeStructVariant("OuterEnum", 3u, "StructVar", 1).getOrThrow()
        endFailBuilder.serializeField("flag", BoolField(false)).getOrThrow()
        val endFailResult = endFailBuilder.end()
        assertSame(endFailure, endFailResult)
        assertSame(endError, endFailResult.exceptionOrNull())
        assertEquals(
            listOf<Any>(Content.Struct("StructVar", listOf("flag" to Content.Bool(false)))),
            endFailDelegate.recordedValues,
        )

        val endBug = DelegateProgrammingError("struct end serializeValue bug")
        val endThrowDelegate = RecordingDelegateMap(onValue = DelegateStepOutcome.ThrowBug(endBug))
        val endThrowBuilder =
            FlatMapSerializer(endThrowDelegate).serializeStructVariant("OuterEnum", 3u, "StructVar", 1).getOrThrow()
        endThrowBuilder.serializeField("num", IntField(9)).getOrThrow()
        val thrownEndBug =
            assertFailsWith<DelegateProgrammingError> {
                endThrowBuilder.end()
            }
        assertSame(endBug, thrownEndBug)
    }

    @Test
    fun successfulFlattenedVariantsPreserveOutputAndFieldOrderThroughSerdeTestTokens() {
        val envelope =
            MultiFlattenedVariantsEnvelope(
                listOf(
                    FlattenedVariantSample.UnitCase,
                    FlattenedVariantSample.NewtypeCase(StringField("wrapped")),
                    FlattenedVariantSample.TupleCase(IntField(100), BoolField(true)),
                    FlattenedVariantSample.StructCase(IntField(200), StringField("named")),
                ),
            )

        assertSerTokens(
            envelope,
            listOf(
                Token.Map(null),
                Token.Str("header"),
                Token.I32(1),
                Token.Str("UnitCase"),
                Token.UnitValue,
                Token.Str("NewtypeCase"),
                Token.Str("wrapped"),
                Token.Str("TupleCase"),
                Token.Seq(2),
                Token.I32(100),
                Token.Bool(true),
                Token.SeqEnd,
                Token.Str("StructCase"),
                Token.Struct("StructCase", 2),
                Token.Str("id"),
                Token.I32(200),
                Token.Str("title"),
                Token.Str("named"),
                Token.StructEnd,
                Token.Str("footer"),
                Token.Bool(true),
                Token.MapEnd,
            ),
        )
    }
}
