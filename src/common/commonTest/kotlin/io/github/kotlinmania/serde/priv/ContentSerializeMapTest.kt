// port-lint: tests test_suite/tests/test_ser.rs
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
import kotlin.test.assertTrue

private class CallbackProgrammingError(
    message: String,
) : RuntimeException(message)

private class RecordingScalarSerialize(
    private val text: String,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        return serializer.serializeStr(text)
    }
}

private class RecordingIntSerialize(
    private val number: Int,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        return serializer.serializeI32(number)
    }
}

private class FailingSerialize(
    private val failure: SerdeResult<Nothing>,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        return failure
    }
}

private class ThrowingSerialize(
    private val throwable: RuntimeException,
) : Serialize {
    var calls: Int = 0
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        calls += 1
        throw throwable
    }
}

private interface BufferedMapScenario {
    fun <Ok> execute(map: SerializeMap<Ok>)
}

private class BufferedMapFieldProbe(
    private val lenHint: Int?,
    private val scenario: BufferedMapScenario,
) : Serialize {
    var callbacksRun: Int = 0
        private set

    var callbackThrowable: Throwable? = null
        private set

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeMap(lenHint).flatMap { map ->
            callbacksRun += 1
            try {
                scenario.execute(map)
            } catch (t: Throwable) {
                callbackThrowable = t
                throw t
            }
            map.end()
        }
}

private class FlattenedTupleVariantCarrier(
    private val field: Serialize,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val outerMap = serializer.serializeMap(null).getOrThrow()
            val flat = FlatMapSerializer(outerMap)
            val tupleVariant = flat.serializeTupleVariant("OuterEnum", 0u, "TupleCarrier", 1).getOrThrow()
            tupleVariant.serializeField(field).getOrThrow()
            tupleVariant.end().getOrThrow()
            outerMap.end().getOrThrow()
        }
}

private class FlattenedStructVariantCarrier(
    private val field: Serialize,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serdeCatching {
            val outerMap = serializer.serializeMap(null).getOrThrow()
            val flat = FlatMapSerializer(outerMap)
            val structVariant = flat.serializeStructVariant("OuterEnum", 1u, "StructCarrier", 1).getOrThrow()
            structVariant.serializeField("bufferedMap", field).getOrThrow()
            structVariant.end().getOrThrow()
            outerMap.end().getOrThrow()
        }
}

private fun assertViaTupleVariantContentSerializer(
    lenHint: Int?,
    expectedInnerMapTokens: List<Token>,
    scenario: BufferedMapScenario,
) {
    val probe = BufferedMapFieldProbe(lenHint, scenario)
    val expectedTokens =
        buildList {
            add(Token.Map(null))
            add(Token.Str("TupleCarrier"))
            add(Token.Seq(1))
            addAll(expectedInnerMapTokens)
            add(Token.SeqEnd)
            add(Token.MapEnd)
        }
    try {
        assertSerTokens(FlattenedTupleVariantCarrier(probe), expectedTokens)
    } catch (outerFailure: Throwable) {
        probe.callbackThrowable?.let { throw it }
        throw outerFailure
    }
    probe.callbackThrowable?.let { throw it }
    assertEquals(1, probe.callbacksRun)
}

private fun assertViaStructVariantContentSerializer(
    lenHint: Int?,
    expectedInnerMapTokens: List<Token>,
    scenario: BufferedMapScenario,
) {
    val probe = BufferedMapFieldProbe(lenHint, scenario)
    val expectedTokens =
        buildList {
            add(Token.Map(null))
            add(Token.Str("StructCarrier"))
            add(Token.Struct("StructCarrier", 1))
            add(Token.Str("bufferedMap"))
            addAll(expectedInnerMapTokens)
            add(Token.StructEnd)
            add(Token.MapEnd)
        }
    try {
        assertSerTokens(FlattenedStructVariantCarrier(probe), expectedTokens)
    } catch (outerFailure: Throwable) {
        probe.callbackThrowable?.let { throw it }
        throw outerFailure
    }
    probe.callbackThrowable?.let { throw it }
    assertEquals(1, probe.callbacksRun)
}

class ContentSerializeMapTest {
    @Test
    fun serializeValueWithoutPendingKeyThrowsIllegalStateExceptionAndSkipsValueSerializer() {
        assertViaTupleVariantContentSerializer(
            lenHint = 2,
            expectedInnerMapTokens = listOf(Token.Map(0), Token.MapEnd),
            scenario =
                object : BufferedMapScenario {
                    override fun <Ok> execute(map: SerializeMap<Ok>) {
                        val skippedInitialValue = RecordingIntSerialize(10)
                        val initialMissingKey =
                            assertFailsWith<IllegalStateException> {
                                map.serializeValue(skippedInitialValue)
                            }
                        assertEquals("serializeValue called before serializeKey", initialMissingKey.message)
                        assertEquals(0, skippedInitialValue.calls)
                    }
                },
        )

        assertViaStructVariantContentSerializer(
            lenHint = 3,
            expectedInnerMapTokens =
                listOf(
                    Token.Map(2),
                    Token.Str("first"),
                    Token.I32(1),
                    Token.Str("second"),
                    Token.I32(2),
                    Token.MapEnd,
                ),
            scenario =
                object : BufferedMapScenario {
                    override fun <Ok> execute(map: SerializeMap<Ok>) {
                        map.serializeKey(RecordingScalarSerialize("first")).getOrThrow()
                        map.serializeValue(RecordingIntSerialize(1)).getOrThrow()

                        val skippedAfterPair = RecordingIntSerialize(2)
                        val afterPairMissingKey =
                            assertFailsWith<IllegalStateException> {
                                map.serializeValue(skippedAfterPair)
                            }
                        assertEquals("serializeValue called before serializeKey", afterPairMissingKey.message)
                        assertEquals(0, skippedAfterPair.calls)

                        map.serializeEntry(RecordingScalarSerialize("second"), RecordingIntSerialize(2)).getOrThrow()
                        val skippedAfterEntry = RecordingIntSerialize(3)
                        val afterEntryMissingKey =
                            assertFailsWith<IllegalStateException> {
                                map.serializeValue(skippedAfterEntry)
                            }
                        assertEquals("serializeValue called before serializeKey", afterEntryMissingKey.message)
                        assertEquals(0, skippedAfterEntry.calls)
                    }
                },
        )
    }

    @Test
    fun pendingKeyIsConsumedBeforeFailingOrThrowingValueAndFailureIdentityIsPreserved() {
        assertViaTupleVariantContentSerializer(
            lenHint = null,
            expectedInnerMapTokens =
                listOf(
                    Token.Map(2),
                    Token.Str("before"),
                    Token.I32(10),
                    Token.Str("after"),
                    Token.I32(20),
                    Token.MapEnd,
                ),
            scenario =
                object : BufferedMapScenario {
                    override fun <Ok> execute(map: SerializeMap<Ok>) {
                        map.serializeEntry(RecordingScalarSerialize("before"), RecordingIntSerialize(10)).getOrThrow()

                        val claimedKey = RecordingScalarSerialize("claimedOnSerdeError")
                        map.serializeKey(claimedKey).getOrThrow()
                        assertEquals(1, claimedKey.calls)

                        val cause = SerdeError.custom("inner value cause")
                        val expectedError = SerdeError("value serialization failed", source = cause)
                        val expectedFailure = SerdeResult.failure(expectedError)
                        val failingValue = FailingSerialize(expectedFailure)

                        val valueResult = map.serializeValue(failingValue)
                        assertTrue(valueResult.isFailure)
                        assertEquals(1, failingValue.calls)
                        assertSame(expectedFailure, valueResult)
                        assertSame(expectedError, valueResult.exceptionOrNull())

                        val retryAfterErrorValue = RecordingIntSerialize(99)
                        val missingAfterFailedValue =
                            assertFailsWith<IllegalStateException> {
                                map.serializeValue(retryAfterErrorValue)
                            }
                        assertEquals("serializeValue called before serializeKey", missingAfterFailedValue.message)
                        assertEquals(0, retryAfterErrorValue.calls)

                        map.serializeKey(RecordingScalarSerialize("claimedOnCallbackThrow")).getOrThrow()
                        val expectedBug = CallbackProgrammingError("value callback invariant violation")
                        val throwingValue = ThrowingSerialize(expectedBug)

                        val thrownFromValue =
                            assertFailsWith<CallbackProgrammingError> {
                                map.serializeValue(throwingValue)
                            }
                        assertSame(expectedBug, thrownFromValue)
                        assertEquals(1, throwingValue.calls)

                        val retryAfterThrownValue = RecordingIntSerialize(77)
                        val missingAfterThrownValue =
                            assertFailsWith<IllegalStateException> {
                                map.serializeValue(retryAfterThrownValue)
                            }
                        assertEquals("serializeValue called before serializeKey", missingAfterThrownValue.message)
                        assertEquals(0, retryAfterThrownValue.calls)

                        map.serializeKey(RecordingScalarSerialize("after")).getOrThrow()
                        map.serializeValue(RecordingIntSerialize(20)).getOrThrow()
                    }
                },
        )
    }

    @Test
    fun serializeKeyAndSerializeEntryPropagateCallbackExceptionsAndPreserveErrorAndOrderingSemantics() {
        assertViaStructVariantContentSerializer(
            lenHint = 4,
            expectedInnerMapTokens =
                listOf(
                    Token.Map(1),
                    Token.Str("retainedPending"),
                    Token.I32(11),
                    Token.MapEnd,
                ),
            scenario =
                object : BufferedMapScenario {
                    override fun <Ok> execute(map: SerializeMap<Ok>) {
                        val keyError = SerdeError("key serialization failed", source = SerdeError.custom("bad key"))
                        val keyFailure = SerdeResult.failure(keyError)
                        val failingKey = FailingSerialize(keyFailure)
                        val failedKeyResult = map.serializeKey(failingKey)
                        assertEquals(1, failingKey.calls)
                        assertSame(keyFailure, failedKeyResult)
                        assertSame(keyError, failedKeyResult.exceptionOrNull())

                        val skippedAfterFailedKey = RecordingIntSerialize(1)
                        assertFailsWith<IllegalStateException> {
                            map.serializeValue(skippedAfterFailedKey)
                        }
                        assertEquals(0, skippedAfterFailedKey.calls)

                        map.serializeKey(RecordingScalarSerialize("retainedPending")).getOrThrow()
                        val failedOverwriteKey =
                            FailingSerialize(SerdeResult.failure(SerdeError.custom("overwrite key failed")))
                        assertEquals("overwrite key failed", map.serializeKey(failedOverwriteKey).exceptionOrNull()?.message)

                        val keyBug = IllegalArgumentException("key callback bug")
                        val thrownKeyBug =
                            assertFailsWith<IllegalArgumentException> {
                                map.serializeKey(ThrowingSerialize(keyBug))
                            }
                        assertSame(keyBug, thrownKeyBug)

                        map.serializeValue(RecordingIntSerialize(11)).getOrThrow()

                        val entryKeyError = SerdeError.custom("entry key failed")
                        val entryKeyFailure = SerdeResult.failure(entryKeyError)
                        val skippedEntryValueOnKeyError = RecordingIntSerialize(22)
                        val entryKeyResult = map.serializeEntry(FailingSerialize(entryKeyFailure), skippedEntryValueOnKeyError)
                        assertSame(entryKeyFailure, entryKeyResult)
                        assertSame(entryKeyError, entryKeyResult.exceptionOrNull())
                        assertEquals(0, skippedEntryValueOnKeyError.calls)

                        val entryKeyBug = CallbackProgrammingError("entry key callback bug")
                        val skippedEntryValueOnKeyThrow = RecordingIntSerialize(33)
                        val thrownEntryKeyBug =
                            assertFailsWith<CallbackProgrammingError> {
                                map.serializeEntry(ThrowingSerialize(entryKeyBug), skippedEntryValueOnKeyThrow)
                            }
                        assertSame(entryKeyBug, thrownEntryKeyBug)
                        assertEquals(0, skippedEntryValueOnKeyThrow.calls)

                        val validEntryKey1 = RecordingScalarSerialize("entryValueErrorKey")
                        val entryValueError = SerdeError.custom("entry value failed")
                        val entryValueFailure = SerdeResult.failure(entryValueError)
                        val entryValueResult = map.serializeEntry(validEntryKey1, FailingSerialize(entryValueFailure))
                        assertEquals(1, validEntryKey1.calls)
                        assertSame(entryValueFailure, entryValueResult)
                        assertSame(entryValueError, entryValueResult.exceptionOrNull())

                        val validEntryKey2 = RecordingScalarSerialize("entryValueThrowKey")
                        val entryValueBug = UnsupportedOperationException("entry value callback bug")
                        val thrownEntryValueBug =
                            assertFailsWith<UnsupportedOperationException> {
                                map.serializeEntry(validEntryKey2, ThrowingSerialize(entryValueBug))
                            }
                        assertEquals(1, validEntryKey2.calls)
                        assertSame(entryValueBug, thrownEntryValueBug)

                        val skippedAfterEntryFailures = RecordingIntSerialize(44)
                        assertFailsWith<IllegalStateException> {
                            map.serializeValue(skippedAfterEntryFailures)
                        }
                        assertEquals(0, skippedAfterEntryFailures.calls)

                        val flatDelegate = FlatMapSerializer(map).serializeMap(null).getOrThrow()
                        assertFailsWith<IllegalStateException> {
                            flatDelegate.serializeValue(RecordingIntSerialize(55))
                        }
                        val flatBug = CallbackProgrammingError("flat delegate callback bug")
                        val thrownFlatBug =
                            assertFailsWith<CallbackProgrammingError> {
                                flatDelegate.serializeEntry(RecordingScalarSerialize("flat"), ThrowingSerialize(flatBug))
                            }
                        assertSame(flatBug, thrownFlatBug)
                    }
                },
        )
    }

    @Test
    fun splitKeyValueAndDirectEntrySerializationPreserveOutputOrderAndExistingStateSemantics() {
        assertViaTupleVariantContentSerializer(
            lenHint = 4,
            expectedInnerMapTokens =
                listOf(
                    Token.Map(4),
                    Token.Str("alpha"),
                    Token.I32(1),
                    Token.Str("beta"),
                    Token.I32(2),
                    Token.Str("gamma"),
                    Token.I32(3),
                    Token.Str("delta"),
                    Token.I32(4),
                    Token.MapEnd,
                ),
            scenario =
                object : BufferedMapScenario {
                    override fun <Ok> execute(map: SerializeMap<Ok>) {
                        map.serializeKey(RecordingScalarSerialize("overwritten")).getOrThrow()
                        map.serializeKey(RecordingScalarSerialize("alpha")).getOrThrow()
                        map.serializeValue(RecordingIntSerialize(1)).getOrThrow()

                        map.serializeEntry(RecordingScalarSerialize("beta"), RecordingIntSerialize(2)).getOrThrow()

                        map.serializeKey(RecordingScalarSerialize("delta")).getOrThrow()
                        map.serializeEntry(RecordingScalarSerialize("gamma"), RecordingIntSerialize(3)).getOrThrow()
                        map.serializeValue(RecordingIntSerialize(4)).getOrThrow()

                        map.serializeKey(RecordingScalarSerialize("danglingAtEnd")).getOrThrow()
                    }
                },
        )
    }
}
