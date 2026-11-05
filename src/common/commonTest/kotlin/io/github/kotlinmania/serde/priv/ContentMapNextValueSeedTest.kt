// port-lint: tests test_suite/tests/test_de.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.UnitDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry
import io.github.kotlinmania.serdecore.priv.contentaccess.newByteBufContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newI32Content
import io.github.kotlinmania.serdecore.priv.contentaccess.newMapContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newSomeContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStringContent
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val MISSING_VALUE_MESSAGE = "MapAccess.nextValue called before nextKey"

private class SeedCallbackBugException(
    message: String,
) : RuntimeException(message)

private class ValueVisitorBugException(
    message: String,
) : RuntimeException(message)

private class DecodedWitness(
    val tag: String,
    val number: Int,
)

private class RecordingIntSeed : DeserializeSeed<Int> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<Int>
        where D : Deserializer {
        calls += 1
        return I32Deserialize.deserialize(deserializer)
    }
}

private class OutcomeWitnessSeed(
    private val label: String,
) : DeserializeSeed<DecodedWitness> {
    var calls: Int = 0
        private set

    var lastReturnedSuccess: SerdeResult<DecodedWitness>? = null
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<DecodedWitness>
        where D : Deserializer {
        calls += 1
        return I32Deserialize.deserialize(deserializer).flatMap { decoded ->
            val result = SerdeResult.success(DecodedWitness(label, decoded))
            lastReturnedSuccess = result
            result
        }
    }
}

private class FailingValueSeed<T>(
    private val failure: SerdeResult<Nothing>,
    private val readIntFirst: Boolean = false,
) : DeserializeSeed<T> {
    var calls: Int = 0
        private set

    var observedInt: Int? = null
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<T>
        where D : Deserializer {
        calls += 1
        if (readIntFirst) {
            observedInt = I32Deserialize.deserialize(deserializer).getOrThrow()
        }
        return failure
    }
}

private class ThrowingValueSeed<T>(
    private val exception: RuntimeException,
) : DeserializeSeed<T> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<T>
        where D : Deserializer {
        calls += 1
        throw exception
    }
}

private class VisitorThrowingValueSeed<T>(
    private val exception: RuntimeException,
) : DeserializeSeed<T> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<T>
        where D : Deserializer {
        calls += 1
        return deserializer.deserializeAny(
            object : Visitor<T> {
                override fun expecting(): String = "a value whose visitor throws"

                override fun visitI32(v: Int): SerdeResult<T> = throw exception
            },
        )
    }
}

private class NullableIntWitnessSeed : DeserializeSeed<Int?> {
    var calls: Int = 0
        private set

    val nullSuccess: SerdeResult<Int?> = SerdeResult.success(null)

    override fun <D> deserialize(deserializer: D): SerdeResult<Int?>
        where D : Deserializer {
        calls += 1
        return nullableDeserialize(I32Deserialize).deserialize(deserializer).flatMap { decoded ->
            if (decoded == null) {
                nullSuccess
            } else {
                SerdeResult.success(decoded)
            }
        }
    }
}

private data object DispatchKindSeed : DeserializeSeed<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeAny(
            object : Visitor<String> {
                override fun expecting(): String = "a string or byte sequence"

                override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success("str:$v")

                override fun visitString(v: String): SerdeResult<String> = SerdeResult.success("string:$v")

                override fun visitBytes(v: ByteArray): SerdeResult<String> =
                    SerdeResult.success("bytes:${v.joinToString(",")}")

                override fun visitByteBuf(v: ByteArray): SerdeResult<String> =
                    SerdeResult.success("byteBuf:${v.joinToString(",")}")
            },
        )
}

private fun <R> runMapVisitorScenario(
    deserializer: Deserializer,
    useDeserializeAny: Boolean = false,
    scenario: (MapAccess) -> SerdeResult<R>,
): R {
    var visitorCalls = 0
    var callbackThrowable: Throwable? = null
    val visitor =
        object : Visitor<R> {
            override fun expecting(): String = "a content map"

            override fun <A> visitMap(access: A): SerdeResult<R>
                where A : MapAccess {
                visitorCalls += 1
                return try {
                    scenario(access)
                } catch (t: Throwable) {
                    callbackThrowable = t
                    throw t
                }
            }
        }
    val outcome =
        if (useDeserializeAny) {
            deserializer.deserializeAny(visitor)
        } else {
            deserializer.deserializeMap(visitor)
        }
    callbackThrowable?.let { throw it }
    assertEquals(1, visitorCalls)
    return outcome.getOrThrow()
}

private fun assertMissingKeyAndDoubleReadThrowsAtBoundary(
    buildDeserializer: (Content) -> Deserializer,
) {
    val mapContent =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("first"), newI32Content(10)),
                ContentMapEntry(newStringContent("second"), newI32Content(20)),
                ContentMapEntry(newStringContent("third"), newI32Content(30)),
            ),
        )

    val decodedPairs =
        runMapVisitorScenario(buildDeserializer(mapContent)) { access ->
            val skippedBeforeFirstKey = RecordingIntSeed()
            val beforeKeyError =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(skippedBeforeFirstKey)
                }
            assertEquals(MISSING_VALUE_MESSAGE, beforeKeyError.message)
            assertEquals(0, skippedBeforeFirstKey.calls)

            assertEquals("first", access.nextKey(StringDeserialize).getOrThrow())
            val firstSeed = RecordingIntSeed()
            assertEquals(10, access.nextValueSeed(firstSeed).getOrThrow())
            assertEquals(1, firstSeed.calls)

            val skippedDoubleRead = RecordingIntSeed()
            val doubleReadError =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(skippedDoubleRead)
                }
            assertEquals(MISSING_VALUE_MESSAGE, doubleReadError.message)
            assertEquals(0, skippedDoubleRead.calls)

            val entryPair = access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow()
            assertEquals("second" to 20, entryPair)

            val skippedAfterEntry = RecordingIntSeed()
            val afterEntryError =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(skippedAfterEntry)
                }
            assertEquals(MISSING_VALUE_MESSAGE, afterEntryError.message)
            assertEquals(0, skippedAfterEntry.calls)

            assertEquals("third", access.nextKey(StringDeserialize).getOrThrow())
            val thirdValue = access.nextValue(I32Deserialize).getOrThrow()
            assertNull(access.nextKey(StringDeserialize).getOrThrow())

            val skippedAtEnd = RecordingIntSeed()
            val atEndError =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(skippedAtEnd)
                }
            assertEquals(MISSING_VALUE_MESSAGE, atEndError.message)
            assertEquals(0, skippedAtEnd.calls)

            SerdeResult.success(listOf("first" to 10, entryPair, "third" to thirdValue))
        }

    assertEquals(
        listOf("first" to 10, "second" to 20, "third" to 30),
        decodedPairs,
    )
}

private fun assertSlotConsumedAndOutcomeIdentityPreserved(
    buildDeserializer: (Content) -> Deserializer,
) {
    val mapContent =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("failEntry"), newI32Content(11)),
                ContentMapEntry(newStringContent("okEntry"), newI32Content(22)),
            ),
        )

    val returnedWitness =
        runMapVisitorScenario(buildDeserializer(mapContent), useDeserializeAny = true) { access ->
            assertEquals("failEntry", access.nextKey(StringDeserialize).getOrThrow())

            val cause = SerdeError.custom("inner seed decode cause")
            val expectedError = SerdeError("value seed ordinary failure", source = cause)
            val expectedFailure = SerdeResult.failure(expectedError)
            val failingSeed = FailingValueSeed<Int>(expectedFailure, readIntFirst = true)

            val failedOutcome = access.nextValueSeed(failingSeed)
            assertTrue(failedOutcome.isFailure)
            assertEquals(1, failingSeed.calls)
            assertEquals(11, failingSeed.observedInt)
            assertSame(expectedError, failedOutcome.exceptionOrNull())
            assertSame(expectedFailure, failedOutcome)

            val retryAfterFailure = RecordingIntSeed()
            val afterFailureMissing =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(retryAfterFailure)
                }
            assertEquals(MISSING_VALUE_MESSAGE, afterFailureMissing.message)
            assertEquals(0, retryAfterFailure.calls)

            assertEquals("okEntry", access.nextKey(StringDeserialize).getOrThrow())
            val okSeed = OutcomeWitnessSeed("decoded-ok")
            val okOutcome = access.nextValueSeed(okSeed)
            assertEquals(1, okSeed.calls)
            val expectedSuccess = checkNotNull(okSeed.lastReturnedSuccess)
            assertSame(expectedSuccess, okOutcome)
            assertEquals("decoded-ok", okOutcome.getOrThrow().tag)
            assertEquals(22, okOutcome.getOrThrow().number)

            assertNull(access.nextKey(StringDeserialize).getOrThrow())
            okOutcome
        }

    assertEquals("decoded-ok", returnedWitness.tag)
    assertEquals(22, returnedWitness.number)
}

private fun assertSlotConsumedAndUncheckedExceptionPropagated(
    buildDeserializer: (Content) -> Deserializer,
) {
    val mapContent =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("directThrow"), newI32Content(41)),
                ContentMapEntry(newStringContent("visitorThrow"), newI32Content(42)),
                ContentMapEntry(newStringContent("tailOk"), newI32Content(43)),
            ),
        )

    val tailDecoded =
        runMapVisitorScenario(buildDeserializer(mapContent)) { access ->
            assertEquals("directThrow", access.nextKey(StringDeserialize).getOrThrow())
            val expectedSeedBug = SeedCallbackBugException("seed callback invariant violation")
            val throwingSeed = ThrowingValueSeed<Int>(expectedSeedBug)
            val thrownSeedBug =
                assertFailsWith<SeedCallbackBugException> {
                    access.nextValueSeed(throwingSeed)
                }
            assertSame(expectedSeedBug, thrownSeedBug)
            assertEquals(1, throwingSeed.calls)

            val retryAfterDirectThrow = RecordingIntSeed()
            val afterDirectThrowMissing =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(retryAfterDirectThrow)
                }
            assertEquals(MISSING_VALUE_MESSAGE, afterDirectThrowMissing.message)
            assertEquals(0, retryAfterDirectThrow.calls)

            assertEquals("visitorThrow", access.nextKey(StringDeserialize).getOrThrow())
            val expectedVisitorBug = ValueVisitorBugException("nested visitor invariant violation")
            val visitorThrowingSeed = VisitorThrowingValueSeed<Int>(expectedVisitorBug)
            val thrownVisitorBug =
                assertFailsWith<ValueVisitorBugException> {
                    access.nextValueSeed(visitorThrowingSeed)
                }
            assertSame(expectedVisitorBug, thrownVisitorBug)
            assertEquals(1, visitorThrowingSeed.calls)

            val retryAfterVisitorThrow = RecordingIntSeed()
            val afterVisitorThrowMissing =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(retryAfterVisitorThrow)
                }
            assertEquals(MISSING_VALUE_MESSAGE, afterVisitorThrowMissing.message)
            assertEquals(0, retryAfterVisitorThrow.calls)

            assertEquals("tailOk", access.nextKey(StringDeserialize).getOrThrow())
            val tailValue = access.nextValue(I32Deserialize).getOrThrow()
            assertNull(access.nextKey(StringDeserialize).getOrThrow())
            SerdeResult.success(tailValue)
        }

    assertEquals(43, tailDecoded)
}

private fun assertUnitAndNoneSlotsAndDispatchPreserved(
    mapContent: Content,
    buildDeserializer: (Content) -> Deserializer,
    expectedTextDispatch: String,
    expectedBytesDispatch: String,
) {
    val summary =
        runMapVisitorScenario(buildDeserializer(mapContent)) { access ->
            assertEquals("unitSlot", access.nextKey(StringDeserialize).getOrThrow())
            val unitResult = access.nextValue(UnitDeserialize)
            assertEquals(Unit, unitResult.getOrThrow())

            val duplicateAfterUnit =
                assertFailsWith<IllegalStateException> {
                    access.nextValue(UnitDeserialize)
                }
            assertEquals(MISSING_VALUE_MESSAGE, duplicateAfterUnit.message)

            assertEquals("noneSlot", access.nextKey(StringDeserialize).getOrThrow())
            val nullableSeed = NullableIntWitnessSeed()
            val noneResult = access.nextValueSeed(nullableSeed)
            assertEquals(1, nullableSeed.calls)
            assertSame(nullableSeed.nullSuccess, noneResult)
            assertNull(noneResult.getOrThrow())

            val duplicateAfterNone =
                assertFailsWith<IllegalStateException> {
                    access.nextValueSeed(nullableSeed)
                }
            assertEquals(MISSING_VALUE_MESSAGE, duplicateAfterNone.message)
            assertEquals(1, nullableSeed.calls)

            assertEquals("someSlot", access.nextKey(StringDeserialize).getOrThrow())
            val someResult = access.nextValueSeed(nullableSeed)
            assertEquals(2, nullableSeed.calls)
            assertEquals(99, someResult.getOrThrow())

            assertEquals("textSlot", access.nextKey(StringDeserialize).getOrThrow())
            val textDispatch = access.nextValueSeed(DispatchKindSeed).getOrThrow()

            assertEquals("bytesSlot", access.nextKey(StringDeserialize).getOrThrow())
            val bytesDispatch = access.nextValueSeed(DispatchKindSeed).getOrThrow()

            assertNull(access.nextKey(StringDeserialize).getOrThrow())
            SerdeResult.success(listOf(textDispatch, bytesDispatch))
        }

    assertEquals(listOf(expectedTextDispatch, expectedBytesDispatch), summary)
}

class ContentMapNextValueSeedTest {
    @Test
    fun ownedMapDeserializerNextValueSeedThrowsOnMissingPrecedingKeyAndDoubleValueRead() {
        assertMissingKeyAndDoubleReadThrowsAtBoundary { content ->
            ContentDeserializer.new(content)
        }
    }

    @Test
    fun ownedMapDeserializerNextValueSeedConsumesSlotAndPreservesOutcomeIdentityOnSeedResult() {
        assertSlotConsumedAndOutcomeIdentityPreserved { content ->
            ContentDeserializer.new(content)
        }
    }

    @Test
    fun ownedMapDeserializerNextValueSeedConsumesSlotAndPropagatesThrownExceptionIdentity() {
        assertSlotConsumedAndUncheckedExceptionPropagated { content ->
            ContentDeserializer.new(content)
        }
    }

    @Test
    fun ownedMapDeserializerNextValueSeedHandlesUnitAndNoneSlotsAndPreservesOwnedDecoding() {
        val mapContent =
            newMapContent(
                listOf(
                    ContentMapEntry(newStringContent("unitSlot"), Content.Unit),
                    ContentMapEntry(newStringContent("noneSlot"), Content.None),
                    ContentMapEntry(newStringContent("someSlot"), newSomeContent(newI32Content(99))),
                    ContentMapEntry(newStringContent("textSlot"), newStringContent("owned-text")),
                    ContentMapEntry(newStringContent("bytesSlot"), newByteBufContent(byteArrayOf(4, 5, 6))),
                ),
            )
        assertUnitAndNoneSlotsAndDispatchPreserved(
            mapContent = mapContent,
            buildDeserializer = { content -> ContentDeserializer.new(content) },
            expectedTextDispatch = "string:owned-text",
            expectedBytesDispatch = "byteBuf:4,5,6",
        )
    }

    @Test
    fun borrowedMapRefDeserializerNextValueSeedThrowsOnMissingPrecedingKeyAndDoubleValueRead() {
        assertMissingKeyAndDoubleReadThrowsAtBoundary { content ->
            ContentRefDeserializer.new(content)
        }
    }

    @Test
    fun borrowedMapRefDeserializerNextValueSeedConsumesSlotAndPreservesOutcomeIdentityOnSeedResult() {
        assertSlotConsumedAndOutcomeIdentityPreserved { content ->
            ContentRefDeserializer.new(content)
        }
    }

    @Test
    fun borrowedMapRefDeserializerNextValueSeedConsumesSlotAndPropagatesThrownExceptionIdentity() {
        assertSlotConsumedAndUncheckedExceptionPropagated { content ->
            ContentRefDeserializer.new(content)
        }
    }

    @Test
    fun borrowedMapRefDeserializerNextValueSeedHandlesUnitAndNoneSlotsAndPreservesBorrowedDecoding() {
        val mapContent =
            newMapContent(
                listOf(
                    ContentMapEntry(newStringContent("unitSlot"), Content.Unit),
                    ContentMapEntry(newStringContent("noneSlot"), Content.None),
                    ContentMapEntry(newStringContent("someSlot"), newSomeContent(newI32Content(99))),
                    ContentMapEntry(newStringContent("textSlot"), newStringContent("borrowed-text")),
                    ContentMapEntry(newStringContent("bytesSlot"), newByteBufContent(byteArrayOf(7, 8, 9))),
                ),
            )
        repeat(2) {
            assertUnitAndNoneSlotsAndDispatchPreserved(
                mapContent = mapContent,
                buildDeserializer = { content -> ContentRefDeserializer.new(content) },
                expectedTextDispatch = "str:borrowed-text",
                expectedBytesDispatch = "bytes:7,8,9",
            )
        }
    }
}
