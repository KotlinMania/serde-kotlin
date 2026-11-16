// port-lint: tests test_suite/tests/test_enum_internally_tagged.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.SerializeSeq
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTuple
import io.github.kotlinmania.serdecore.ser.SerializeTupleStruct
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertSerTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class DelegateSerializeValueBugException(
    message: String,
) : RuntimeException(message)

private class DelegateEndBugException(
    message: String,
) : RuntimeException(message)

private data class EndProbeInt(
    val value: Int,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI32(value)
}

private data class EndProbeStr(
    val value: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(value)
}

private sealed class DelegateValueAction {
    data object Succeed : DelegateValueAction()

    class ReturnFailure(
        val failure: SerdeResult<Nothing>,
    ) : DelegateValueAction()

    class ThrowBug(
        val exception: RuntimeException,
    ) : DelegateValueAction()
}

private sealed class DelegateEndAction<out Ok> {
    class ReturnOutcome<out Ok>(
        val outcome: SerdeResult<Ok>,
    ) : DelegateEndAction<Ok>()

    class ThrowBug(
        val exception: RuntimeException,
    ) : DelegateEndAction<Nothing>()
}

private class RecordingVariantDelegateMap<Ok>(
    private val valueAction: DelegateValueAction = DelegateValueAction.Succeed,
    private val endAction: DelegateEndAction<Ok>,
) : SerializeMap<Ok> {
    var entryCalls: Int = 0
        private set

    var keyCalls: Int = 0
        private set

    var valueCalls: Int = 0
        private set

    var endCalls: Int = 0
        private set

    val recordedEntries: MutableList<Pair<Any, Any>> = ArrayList()
    val recordedKeys: MutableList<Any> = ArrayList()
    val recordedValues: MutableList<Any> = ArrayList()

    override fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize {
        entryCalls += 1
        recordedEntries += key to value
        return SerdeResult.success(Unit)
    }

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize {
        keyCalls += 1
        recordedKeys += key
        return SerdeResult.success(Unit)
    }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize {
        valueCalls += 1
        recordedValues += value
        return when (val action = valueAction) {
            DelegateValueAction.Succeed -> SerdeResult.success(Unit)
            is DelegateValueAction.ReturnFailure -> action.failure
            is DelegateValueAction.ThrowBug -> throw action.exception
        }
    }

    override fun end(): SerdeResult<Ok> {
        endCalls += 1
        return when (val action = endAction) {
            is DelegateEndAction.ReturnOutcome -> action.outcome
            is DelegateEndAction.ThrowBug -> throw action.exception
        }
    }
}

private class SingleMapDelegateSerializer<Ok>(
    private val delegateMap: RecordingVariantDelegateMap<Ok>,
) : Serializer<Ok> {
    val requestedMapLengths: MutableList<Int?> = ArrayList()

    private fun unexpected(kind: String): SerdeResult<Nothing> =
        SerdeResult.failure(SerdeError.custom("unexpected serializer call: $kind"))

    override fun serializeBool(v: Boolean): SerdeResult<Ok> = unexpected("bool")

    override fun serializeI8(v: Byte): SerdeResult<Ok> = unexpected("i8")

    override fun serializeI16(v: Short): SerdeResult<Ok> = unexpected("i16")

    override fun serializeI32(v: Int): SerdeResult<Ok> = unexpected("i32")

    override fun serializeI64(v: Long): SerdeResult<Ok> = unexpected("i64")

    override fun serializeU8(v: UByte): SerdeResult<Ok> = unexpected("u8")

    override fun serializeU16(v: UShort): SerdeResult<Ok> = unexpected("u16")

    override fun serializeU32(v: UInt): SerdeResult<Ok> = unexpected("u32")

    override fun serializeU64(v: ULong): SerdeResult<Ok> = unexpected("u64")

    override fun serializeF32(v: Float): SerdeResult<Ok> = unexpected("f32")

    override fun serializeF64(v: Double): SerdeResult<Ok> = unexpected("f64")

    override fun serializeChar(v: Char): SerdeResult<Ok> = unexpected("char")

    override fun serializeStr(v: String): SerdeResult<Ok> = unexpected("str")

    override fun serializeBytes(v: ByteArray): SerdeResult<Ok> = unexpected("bytes")

    override fun serializeNone(): SerdeResult<Ok> = unexpected("none")

    override fun <T> serializeSome(value: T): SerdeResult<Ok>
        where T : Serialize = unexpected("some")

    override fun serializeUnit(): SerdeResult<Ok> = unexpected("unit")

    override fun serializeUnitStruct(name: String): SerdeResult<Ok> = unexpected("unitStruct")

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Ok> = unexpected("unitVariant")

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Ok>
        where T : Serialize = unexpected("newtypeStruct")

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Ok>
        where T : Serialize = unexpected("newtypeVariant")

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Ok>> = unexpected("seq")

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Ok>> = unexpected("tuple")

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Ok>> = unexpected("tupleStruct")

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Ok>> = unexpected("tupleVariant")

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Ok>> {
        requestedMapLengths += len
        return SerdeResult.success(delegateMap)
    }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Ok>> = unexpected("struct")

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Ok>> = unexpected("structVariant")
}

private class SampleTupleVariantPayload(
    private val enumName: String,
    private val variantIndex: UInt,
    private val variantName: String,
    private val first: Int,
    private val second: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeTupleVariant(enumName, variantIndex, variantName, 2).flatMap { state ->
            state.serializeField(EndProbeInt(first)).flatMap {
                state.serializeField(EndProbeStr(second)).flatMap {
                    state.end()
                }
            }
        }
}

private class SampleStructVariantPayload(
    private val enumName: String,
    private val variantIndex: UInt,
    private val variantName: String,
    private val count: Int,
    private val label: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStructVariant(enumName, variantIndex, variantName, 2).flatMap { state ->
            state.serializeField("count", EndProbeInt(count)).flatMap {
                state.serializeField("label", EndProbeStr(label)).flatMap {
                    state.end()
                }
            }
        }
}

private fun <T : Serialize> taggedVariantEnvelope(
    typeIdent: String,
    variantIdent: String,
    tag: String,
    variantName: String,
    payload: T,
): Serialize =
    object : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializeTaggedNewtype(
                serializer = serializer,
                typeIdent = typeIdent,
                variantIdent = variantIdent,
                tag = tag,
                variantName = variantName,
                value = payload,
            )
    }

class VariantAsMapValueEndPropagationTest {
    @Test
    fun tupleVariantAsMapValueEndSkipsMapEndAndPreservesReturnedFailureIdentityOnSerializeValueError() {
        val cause = SerdeError.custom("tuple serializeValue inner cause")
        val expectedError = SerdeError("tuple serializeValue failed", source = cause)
        val expectedFailure = SerdeResult.failure(expectedError)
        val delegateMap =
            RecordingVariantDelegateMap(
                valueAction = DelegateValueAction.ReturnFailure(expectedFailure),
                endAction = DelegateEndAction.ReturnOutcome(SerdeResult.success("unreachable-ok")),
            )
        val serializer = SingleMapDelegateSerializer(delegateMap)

        val result =
            serializeTaggedNewtype(
                serializer = serializer,
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                value = SampleTupleVariantPayload("InnerEnum", 1u, "TupleCase", 7, "alpha"),
            )

        assertTrue(result.isFailure)
        assertEquals(listOf<Int?>(2), serializer.requestedMapLengths)
        assertEquals(1, delegateMap.entryCalls)
        assertEquals(listOf<Pair<Any, Any>>(Content.String("type") to Content.String("OuterNewtype")), delegateMap.recordedEntries)
        assertEquals(1, delegateMap.keyCalls)
        assertEquals(listOf<Any>(Content.String("TupleCase")), delegateMap.recordedKeys)
        assertEquals(1, delegateMap.valueCalls)
        assertEquals(
            listOf<Any>(
                Content.TupleStruct(
                    "TupleCase",
                    listOf(Content.I32(7), Content.String("alpha")),
                ),
            ),
            delegateMap.recordedValues,
        )
        assertEquals(0, delegateMap.endCalls)
        assertSame(expectedError, result.exceptionOrNull())
        assertSame(expectedFailure, result)
    }

    @Test
    fun tupleVariantAsMapValueEndSkipsMapEndAndPropagatesThrownExceptionFromSerializeValue() {
        val expectedBug = DelegateSerializeValueBugException("tuple serializeValue invariant violation")
        val delegateMap =
            RecordingVariantDelegateMap(
                valueAction = DelegateValueAction.ThrowBug(expectedBug),
                endAction = DelegateEndAction.ReturnOutcome(SerdeResult.success("unreachable-ok")),
            )
        val serializer = SingleMapDelegateSerializer(delegateMap)

        val thrown =
            assertFailsWith<DelegateSerializeValueBugException> {
                val unexpected =
                    serializeTaggedNewtype(
                        serializer = serializer,
                        typeIdent = "TaggedOuter",
                        variantIdent = "OuterNewtype",
                        tag = "type",
                        variantName = "OuterNewtype",
                        value = SampleTupleVariantPayload("InnerEnum", 1u, "TupleCase", 7, "alpha"),
                    )
                error("expected DelegateSerializeValueBugException, got $unexpected")
            }

        assertSame(expectedBug, thrown)
        assertEquals(1, delegateMap.valueCalls)
        assertEquals(0, delegateMap.endCalls)
    }

    @Test
    fun tupleVariantAsMapValueEndCallsMapEndOncePreservesEndOutcomesPropagatesEndThrowAndKeepsTokens() {
        val envelope =
            taggedVariantEnvelope(
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                payload = SampleTupleVariantPayload("InnerEnum", 1u, "TupleCase", 7, "alpha"),
            )
        assertSerTokens(
            envelope,
            listOf(
                Token.Map(2),
                Token.Str("type"),
                Token.Str("OuterNewtype"),
                Token.Str("TupleCase"),
                Token.TupleStruct("TupleCase", 2),
                Token.I32(7),
                Token.Str("alpha"),
                Token.TupleStructEnd,
                Token.MapEnd,
            ),
        )

        val expectedSuccess = SerdeResult.success("tuple-map-ok-witness")
        val okDelegate = RecordingVariantDelegateMap(endAction = DelegateEndAction.ReturnOutcome(expectedSuccess))
        val okResult =
            serializeTaggedNewtype(
                serializer = SingleMapDelegateSerializer(okDelegate),
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                value = SampleTupleVariantPayload("InnerEnum", 1u, "TupleCase", 7, "alpha"),
            )
        assertEquals(1, okDelegate.valueCalls)
        assertEquals(1, okDelegate.endCalls)
        assertEquals("tuple-map-ok-witness", okResult.getOrThrow())

        val endCause = SerdeError.custom("tuple end inner cause")
        val expectedEndError = SerdeError("tuple map end failed", source = endCause)
        val expectedEndFailure = SerdeResult.failure(expectedEndError)
        val failEndDelegate =
            RecordingVariantDelegateMap<String>(endAction = DelegateEndAction.ReturnOutcome(expectedEndFailure))
        val endFailResult =
            serializeTaggedNewtype(
                serializer = SingleMapDelegateSerializer(failEndDelegate),
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                value = SampleTupleVariantPayload("InnerEnum", 1u, "TupleCase", 7, "alpha"),
            )
        assertTrue(endFailResult.isFailure)
        assertEquals(1, failEndDelegate.valueCalls)
        assertEquals(1, failEndDelegate.endCalls)
        assertSame(expectedEndError, endFailResult.exceptionOrNull())

        val expectedEndBug = DelegateEndBugException("tuple map end invariant violation")
        val throwEndDelegate =
            RecordingVariantDelegateMap<String>(endAction = DelegateEndAction.ThrowBug(expectedEndBug))
        val thrownEnd =
            assertFailsWith<DelegateEndBugException> {
                val unexpected =
                    serializeTaggedNewtype(
                        serializer = SingleMapDelegateSerializer(throwEndDelegate),
                        typeIdent = "TaggedOuter",
                        variantIdent = "OuterNewtype",
                        tag = "type",
                        variantName = "OuterNewtype",
                        value = SampleTupleVariantPayload("InnerEnum", 1u, "TupleCase", 7, "alpha"),
                    )
                error("expected DelegateEndBugException, got $unexpected")
            }
        assertSame(expectedEndBug, thrownEnd)
        assertEquals(1, throwEndDelegate.valueCalls)
        assertEquals(1, throwEndDelegate.endCalls)
        assertSame(expectedEndFailure, endFailResult)
        assertSame(expectedSuccess, okResult)
    }

    @Test
    fun structVariantAsMapValueEndSkipsMapEndAndPreservesReturnedFailureIdentityOnSerializeValueError() {
        val cause = SerdeError.custom("struct serializeValue inner cause")
        val expectedError = SerdeError("struct serializeValue failed", source = cause)
        val expectedFailure = SerdeResult.failure(expectedError)
        val delegateMap =
            RecordingVariantDelegateMap(
                valueAction = DelegateValueAction.ReturnFailure(expectedFailure),
                endAction = DelegateEndAction.ReturnOutcome(SerdeResult.success("unreachable-ok")),
            )
        val serializer = SingleMapDelegateSerializer(delegateMap)

        val result =
            serializeTaggedNewtype(
                serializer = serializer,
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                value = SampleStructVariantPayload("InnerEnum", 2u, "StructCase", 7, "alpha"),
            )

        assertTrue(result.isFailure)
        assertEquals(listOf<Int?>(2), serializer.requestedMapLengths)
        assertEquals(1, delegateMap.entryCalls)
        assertEquals(listOf<Pair<Any, Any>>(Content.String("type") to Content.String("OuterNewtype")), delegateMap.recordedEntries)
        assertEquals(1, delegateMap.keyCalls)
        assertEquals(listOf<Any>(Content.String("StructCase")), delegateMap.recordedKeys)
        assertEquals(1, delegateMap.valueCalls)
        assertEquals(
            listOf<Any>(
                Content.Struct(
                    "StructCase",
                    listOf(
                        "count" to Content.I32(7),
                        "label" to Content.String("alpha"),
                    ),
                ),
            ),
            delegateMap.recordedValues,
        )
        assertEquals(0, delegateMap.endCalls)
        assertSame(expectedError, result.exceptionOrNull())
        assertSame(expectedFailure, result)
    }

    @Test
    fun structVariantAsMapValueEndSkipsMapEndAndPropagatesThrownExceptionFromSerializeValue() {
        val expectedBug = DelegateSerializeValueBugException("struct serializeValue invariant violation")
        val delegateMap =
            RecordingVariantDelegateMap(
                valueAction = DelegateValueAction.ThrowBug(expectedBug),
                endAction = DelegateEndAction.ReturnOutcome(SerdeResult.success("unreachable-ok")),
            )
        val serializer = SingleMapDelegateSerializer(delegateMap)

        val thrown =
            assertFailsWith<DelegateSerializeValueBugException> {
                val unexpected =
                    serializeTaggedNewtype(
                        serializer = serializer,
                        typeIdent = "TaggedOuter",
                        variantIdent = "OuterNewtype",
                        tag = "type",
                        variantName = "OuterNewtype",
                        value = SampleStructVariantPayload("InnerEnum", 2u, "StructCase", 7, "alpha"),
                    )
                error("expected DelegateSerializeValueBugException, got $unexpected")
            }

        assertSame(expectedBug, thrown)
        assertEquals(1, delegateMap.valueCalls)
        assertEquals(0, delegateMap.endCalls)
    }

    @Test
    fun structVariantAsMapValueEndCallsMapEndOncePreservesEndOutcomesPropagatesEndThrowAndKeepsTokens() {
        val envelope =
            taggedVariantEnvelope(
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                payload = SampleStructVariantPayload("InnerEnum", 2u, "StructCase", 7, "alpha"),
            )
        assertSerTokens(
            envelope,
            listOf(
                Token.Map(2),
                Token.Str("type"),
                Token.Str("OuterNewtype"),
                Token.Str("StructCase"),
                Token.Struct("StructCase", 2),
                Token.Str("count"),
                Token.I32(7),
                Token.Str("label"),
                Token.Str("alpha"),
                Token.StructEnd,
                Token.MapEnd,
            ),
        )

        val expectedSuccess = SerdeResult.success("struct-map-ok-witness")
        val okDelegate = RecordingVariantDelegateMap(endAction = DelegateEndAction.ReturnOutcome(expectedSuccess))
        val okResult =
            serializeTaggedNewtype(
                serializer = SingleMapDelegateSerializer(okDelegate),
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                value = SampleStructVariantPayload("InnerEnum", 2u, "StructCase", 7, "alpha"),
            )
        assertEquals(1, okDelegate.valueCalls)
        assertEquals(1, okDelegate.endCalls)
        assertEquals("struct-map-ok-witness", okResult.getOrThrow())

        val endCause = SerdeError.custom("struct end inner cause")
        val expectedEndError = SerdeError("struct map end failed", source = endCause)
        val expectedEndFailure = SerdeResult.failure(expectedEndError)
        val failEndDelegate =
            RecordingVariantDelegateMap<String>(endAction = DelegateEndAction.ReturnOutcome(expectedEndFailure))
        val endFailResult =
            serializeTaggedNewtype(
                serializer = SingleMapDelegateSerializer(failEndDelegate),
                typeIdent = "TaggedOuter",
                variantIdent = "OuterNewtype",
                tag = "type",
                variantName = "OuterNewtype",
                value = SampleStructVariantPayload("InnerEnum", 2u, "StructCase", 7, "alpha"),
            )
        assertTrue(endFailResult.isFailure)
        assertEquals(1, failEndDelegate.valueCalls)
        assertEquals(1, failEndDelegate.endCalls)
        assertSame(expectedEndError, endFailResult.exceptionOrNull())

        val expectedEndBug = DelegateEndBugException("struct map end invariant violation")
        val throwEndDelegate =
            RecordingVariantDelegateMap<String>(endAction = DelegateEndAction.ThrowBug(expectedEndBug))
        val thrownEnd =
            assertFailsWith<DelegateEndBugException> {
                val unexpected =
                    serializeTaggedNewtype(
                        serializer = SingleMapDelegateSerializer(throwEndDelegate),
                        typeIdent = "TaggedOuter",
                        variantIdent = "OuterNewtype",
                        tag = "type",
                        variantName = "OuterNewtype",
                        value = SampleStructVariantPayload("InnerEnum", 2u, "StructCase", 7, "alpha"),
                    )
                error("expected DelegateEndBugException, got $unexpected")
            }
        assertSame(expectedEndBug, thrownEnd)
        assertEquals(1, throwEndDelegate.valueCalls)
        assertEquals(1, throwEndDelegate.endCalls)
        assertSame(expectedEndFailure, endFailResult)
        assertSame(expectedSuccess, okResult)
    }
}
