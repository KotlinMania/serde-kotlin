// port-lint: tests test_suite/tests/test_de.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.de.seedFromDeserialize
import io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry
import io.github.kotlinmania.serdecore.priv.contentaccess.newByteBufContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newI32Content
import io.github.kotlinmania.serdecore.priv.contentaccess.newMapContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStringContent
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val MISSING_VALUE_BEFORE_KEY_MESSAGE = "MapAccess.nextValue called before nextKey"

private class KeySeedCallbackBugException(
    message: String,
) : RuntimeException(message)

private class KeyVisitorBugException(
    message: String,
) : RuntimeException(message)

private class ValueSeedCallbackBugException(
    message: String,
) : RuntimeException(message)

private class VisitorCallbackBugException(
    message: String,
) : RuntimeException(message)

private class KeyWitness(
    val label: String,
    val keyText: String,
)

private class RecordingStringKeySeed : DeserializeSeed<String> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer {
        calls += 1
        return StringDeserialize.deserialize(deserializer)
    }
}

private class RecordingIntValueSeed : DeserializeSeed<Int> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<Int>
        where D : Deserializer {
        calls += 1
        return I32Deserialize.deserialize(deserializer)
    }
}

private class KeyWitnessSeed(
    private val label: String,
) : DeserializeSeed<KeyWitness> {
    var calls: Int = 0
        private set

    var lastReturnedSuccess: SerdeResult<KeyWitness>? = null
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<KeyWitness>
        where D : Deserializer {
        calls += 1
        return StringDeserialize.deserialize(deserializer).flatMap { decoded ->
            val outcome = SerdeResult.success(KeyWitness(label, decoded))
            lastReturnedSuccess = outcome
            outcome
        }
    }
}

private class FailingSeed<T>(
    private val failure: SerdeResult<Nothing>,
    private val consumeStringFirst: Boolean = false,
    private val consumeIntFirst: Boolean = false,
) : DeserializeSeed<T> {
    var calls: Int = 0
        private set

    var observedString: String? = null
        private set

    var observedInt: Int? = null
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<T>
        where D : Deserializer {
        calls += 1
        if (consumeStringFirst) {
            observedString = StringDeserialize.deserialize(deserializer).getOrThrow()
        }
        if (consumeIntFirst) {
            observedInt = I32Deserialize.deserialize(deserializer).getOrThrow()
        }
        return failure
    }
}

private class ThrowingSeed<T>(
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

private class VisitorThrowingKeySeed<T>(
    private val exception: RuntimeException,
) : DeserializeSeed<T> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<T>
        where D : Deserializer {
        calls += 1
        return deserializer.deserializeAny(
            object : Visitor<T> {
                override fun expecting(): String = "a key whose visitor throws"

                override fun visitStr(v: String): SerdeResult<T> = throw exception

                override fun visitString(v: String): SerdeResult<T> = throw exception
            },
        )
    }
}

private class NullableStringSeed : DeserializeSeed<String?> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<String?>
        where D : Deserializer {
        calls += 1
        return nullableDeserialize(StringDeserialize).deserialize(deserializer)
    }
}

private class NullableIntSeed : DeserializeSeed<Int?> {
    var calls: Int = 0
        private set

    override fun <D> deserialize(deserializer: D): SerdeResult<Int?>
        where D : Deserializer {
        calls += 1
        return nullableDeserialize(I32Deserialize).deserialize(deserializer)
    }
}

private data object KeyDispatchKindSeed : DeserializeSeed<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeAny(
            object : Visitor<String> {
                override fun expecting(): String = "a key string or byte sequence"

                override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success("str:$v")

                override fun visitString(v: String): SerdeResult<String> = SerdeResult.success("string:$v")

                override fun visitBytes(v: ByteArray): SerdeResult<String> =
                    SerdeResult.success("bytes:${v.joinToString(",")}")

                override fun visitByteBuf(v: ByteArray): SerdeResult<String> =
                    SerdeResult.success("byteBuf:${v.joinToString(",")}")
            },
        )
}

private fun assertNextKeySeedOutcomeIdentityAndPendingSlot(
    buildDeserializer: (Content) -> Deserializer,
    expectedTextKeyDispatch: String,
    expectedBytesKeyDispatch: String,
) {
    val mapContent =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("badKey"), newI32Content(10)),
                ContentMapEntry(newStringContent("okKey"), newI32Content(20)),
                ContentMapEntry(Content.None, newI32Content(30)),
                ContentMapEntry(newStringContent("dispatchKey"), newI32Content(40)),
                ContentMapEntry(newByteBufContent(byteArrayOf(1, 2)), newI32Content(50)),
            ),
        )

    val visitor =
        object : Visitor<List<String>> {
            override fun expecting(): String = "a content map for nextKeySeed"

            override fun <A> visitMap(access: A): SerdeResult<List<String>>
                where A : MapAccess {
                val cause = SerdeError.custom("key decode cause")
                val expectedError = SerdeError("key seed ordinary failure", source = cause)
                val expectedFailure = SerdeResult.failure(expectedError)
                val failingKeySeed = FailingSeed<String>(expectedFailure, consumeStringFirst = true)

                val failedKeyOutcome = access.nextKeySeed(failingKeySeed)
                assertTrue(failedKeyOutcome.isFailure)
                assertEquals(1, failingKeySeed.calls)
                assertEquals("badKey", failingKeySeed.observedString)
                assertSame(expectedError, failedKeyOutcome.exceptionOrNull())
                assertSame(expectedFailure, failedKeyOutcome)
                assertEquals(10, access.nextValue(I32Deserialize).getOrThrow())

                val witnessSeed = KeyWitnessSeed("witness")
                val okKeyOutcome = access.nextKeySeed(witnessSeed)
                assertEquals(1, witnessSeed.calls)
                val expectedSuccess = checkNotNull(witnessSeed.lastReturnedSuccess)
                assertSame(expectedSuccess, okKeyOutcome)
                val decodedWitness = checkNotNull(okKeyOutcome.getOrThrow())
                assertEquals("witness", decodedWitness.label)
                assertEquals("okKey", decodedWitness.keyText)
                assertEquals(20, access.nextValue(I32Deserialize).getOrThrow())

                val nullableKeySeed = NullableStringSeed()
                val noneKeyOutcome = access.nextKeySeed(nullableKeySeed)
                assertEquals(1, nullableKeySeed.calls)
                assertTrue(noneKeyOutcome.isSuccess)
                assertNull(noneKeyOutcome.getOrThrow())
                assertEquals(30, access.nextValue(I32Deserialize).getOrThrow())

                val textKeyDispatch = checkNotNull(access.nextKeySeed(KeyDispatchKindSeed).getOrThrow())
                assertEquals(40, access.nextValue(I32Deserialize).getOrThrow())

                val bytesKeyDispatch = checkNotNull(access.nextKeySeed(KeyDispatchKindSeed).getOrThrow())
                assertEquals(50, access.nextValue(I32Deserialize).getOrThrow())

                val neverCalledAtEof = ThrowingSeed<String>(KeySeedCallbackBugException("must not run at EOF"))
                assertNull(access.nextKeySeed(neverCalledAtEof).getOrThrow())
                assertNull(access.nextKeySeed(neverCalledAtEof).getOrThrow())
                assertEquals(0, neverCalledAtEof.calls)

                return SerdeResult.success(listOf(textKeyDispatch, bytesKeyDispatch))
            }
        }

    val dispatches = buildDeserializer(mapContent).deserializeMap(visitor).getOrThrow()
    assertEquals(listOf(expectedTextKeyDispatch, expectedBytesKeyDispatch), dispatches)
}

private fun assertNextKeySeedPropagatesThrownExceptionsAndRetainsPendingValue(
    buildDeserializer: (Content) -> Deserializer,
) {
    val mapContent =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("throwDirect"), newI32Content(101)),
                ContentMapEntry(newStringContent("throwVisitor"), newI32Content(102)),
                ContentMapEntry(newStringContent("tailKey"), newI32Content(103)),
            ),
        )

    val visitor =
        object : Visitor<List<Int>> {
            override fun expecting(): String = "a content map whose key seed throws"

            override fun <A> visitMap(access: A): SerdeResult<List<Int>>
                where A : MapAccess {
                val expectedSeedBug = KeySeedCallbackBugException("key seed invariant violation")
                val throwingSeed = ThrowingSeed<String>(expectedSeedBug)
                val thrownSeedBug =
                    assertFailsWith<KeySeedCallbackBugException> {
                        access.nextKeySeed(throwingSeed)
                    }
                assertSame(expectedSeedBug, thrownSeedBug)
                assertEquals(1, throwingSeed.calls)
                val firstPendingValue = access.nextValue(I32Deserialize).getOrThrow()
                assertEquals(101, firstPendingValue)

                val expectedVisitorBug = KeyVisitorBugException("key visitor invariant violation")
                val visitorThrowingSeed = VisitorThrowingKeySeed<String>(expectedVisitorBug)
                val thrownVisitorBug =
                    assertFailsWith<KeyVisitorBugException> {
                        access.nextKeySeed(visitorThrowingSeed)
                    }
                assertSame(expectedVisitorBug, thrownVisitorBug)
                assertEquals(1, visitorThrowingSeed.calls)
                val secondPendingValue = access.nextValue(I32Deserialize).getOrThrow()
                assertEquals(102, secondPendingValue)

                assertEquals("tailKey", access.nextKey(StringDeserialize).getOrThrow())
                val thirdValue = access.nextValue(I32Deserialize).getOrThrow()
                assertNull(access.nextKey(StringDeserialize).getOrThrow())

                return SerdeResult.success(listOf(firstPendingValue, secondPendingValue, thirdValue))
            }
        }

    val values = buildDeserializer(mapContent).deserializeAny(visitor).getOrThrow()
    assertEquals(listOf(101, 102, 103), values)
}

private fun assertNextEntrySeedShortCircuitIdentityAndThrownPropagation(
    buildDeserializer: (Content) -> Deserializer,
) {
    val mapContent =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("keyFail"), newI32Content(11)),
                ContentMapEntry(newStringContent("keyThrow"), newI32Content(12)),
                ContentMapEntry(newStringContent("valFail"), newI32Content(13)),
                ContentMapEntry(newStringContent("valThrow"), newI32Content(14)),
                ContentMapEntry(Content.None, Content.None),
                ContentMapEntry(newStringContent("okEntry"), newI32Content(16)),
            ),
        )

    val visitor =
        object : Visitor<Pair<String, Int>> {
            override fun expecting(): String = "a content map for nextEntrySeed"

            override fun <A> visitMap(access: A): SerdeResult<Pair<String, Int>>
                where A : MapAccess {
                val keyFailureError = SerdeError("entry key ordinary failure")
                val expectedKeyFailure = SerdeResult.failure(keyFailureError)
                val failingKeySeed = FailingSeed<String>(expectedKeyFailure, consumeStringFirst = true)
                val skippedOnKeyFailure = ThrowingSeed<Int>(ValueSeedCallbackBugException("must skip on key failure"))

                val keyFailureResult = access.nextEntrySeed(failingKeySeed, skippedOnKeyFailure)
                assertTrue(keyFailureResult.isFailure)
                assertEquals(1, failingKeySeed.calls)
                assertEquals("keyFail", failingKeySeed.observedString)
                assertEquals(0, skippedOnKeyFailure.calls)
                assertSame(keyFailureError, keyFailureResult.exceptionOrNull())
                assertSame(expectedKeyFailure, keyFailureResult)

                val missingAfterKeyFailure =
                    assertFailsWith<IllegalStateException> {
                        access.nextValue(I32Deserialize)
                    }
                assertEquals(MISSING_VALUE_BEFORE_KEY_MESSAGE, missingAfterKeyFailure.message)

                val expectedKeyBug = KeySeedCallbackBugException("entry key seed bug")
                val throwingKeySeed = ThrowingSeed<String>(expectedKeyBug)
                val skippedOnKeyThrow = ThrowingSeed<Int>(ValueSeedCallbackBugException("must skip on key throw"))
                val thrownKeyBug =
                    assertFailsWith<KeySeedCallbackBugException> {
                        access.nextEntrySeed(throwingKeySeed, skippedOnKeyThrow)
                    }
                assertSame(expectedKeyBug, thrownKeyBug)
                assertEquals(1, throwingKeySeed.calls)
                assertEquals(0, skippedOnKeyThrow.calls)

                val missingAfterKeyThrow =
                    assertFailsWith<IllegalStateException> {
                        access.nextValue(I32Deserialize)
                    }
                assertEquals(MISSING_VALUE_BEFORE_KEY_MESSAGE, missingAfterKeyThrow.message)

                val recordingKeySeed = RecordingStringKeySeed()
                val valueFailureError = SerdeError("entry value ordinary failure")
                val expectedValueFailure = SerdeResult.failure(valueFailureError)
                val failingValueSeed = FailingSeed<Int>(expectedValueFailure, consumeIntFirst = true)
                val valueFailureResult = access.nextEntrySeed(recordingKeySeed, failingValueSeed)
                assertTrue(valueFailureResult.isFailure)
                assertEquals(1, recordingKeySeed.calls)
                assertEquals(1, failingValueSeed.calls)
                assertEquals(13, failingValueSeed.observedInt)
                assertSame(valueFailureError, valueFailureResult.exceptionOrNull())
                assertSame(expectedValueFailure, valueFailureResult)

                val expectedValueBug = ValueSeedCallbackBugException("entry value seed bug")
                val throwingValueSeed = ThrowingSeed<Int>(expectedValueBug)
                val thrownValueBug =
                    assertFailsWith<ValueSeedCallbackBugException> {
                        access.nextEntrySeed(recordingKeySeed, throwingValueSeed)
                    }
                assertSame(expectedValueBug, thrownValueBug)
                assertEquals(2, recordingKeySeed.calls)
                assertEquals(1, throwingValueSeed.calls)

                val nullableKeySeed = NullableStringSeed()
                val nullableValueSeed = NullableIntSeed()
                val nullablePair = access.nextEntrySeed(nullableKeySeed, nullableValueSeed).getOrThrow()
                assertEquals(null to null, nullablePair)
                assertEquals(1, nullableKeySeed.calls)
                assertEquals(1, nullableValueSeed.calls)

                val okValueSeed = RecordingIntValueSeed()
                val finalEntry = checkNotNull(access.nextEntrySeed(recordingKeySeed, okValueSeed).getOrThrow())
                assertEquals("okEntry" to 16, finalEntry)
                assertEquals(3, recordingKeySeed.calls)
                assertEquals(1, okValueSeed.calls)

                val neverCalledKeyAtEof = ThrowingSeed<String>(KeySeedCallbackBugException("must not run at EOF"))
                val neverCalledValAtEof = ThrowingSeed<Int>(ValueSeedCallbackBugException("must not run at EOF"))
                assertNull(access.nextEntrySeed(neverCalledKeyAtEof, neverCalledValAtEof).getOrThrow())
                assertEquals(0, neverCalledKeyAtEof.calls)
                assertEquals(0, neverCalledValAtEof.calls)

                return SerdeResult.success(finalEntry)
            }
        }

    val finalPair = buildDeserializer(mapContent).deserializeMap(visitor).getOrThrow()
    assertEquals("okEntry" to 16, finalPair)
}

private fun assertOuterMapVisitationPropagationAndTrailingLength(
    buildDeserializer: (Content) -> Deserializer,
) {
    val twoEntryMap =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("a"), newI32Content(1)),
                ContentMapEntry(newStringContent("b"), newI32Content(2)),
            ),
        )
    val threeEntryMap =
        newMapContent(
            listOf(
                ContentMapEntry(newStringContent("a"), newI32Content(1)),
                ContentMapEntry(newStringContent("b"), newI32Content(2)),
                ContentMapEntry(newStringContent("c"), newI32Content(3)),
            ),
        )

    val prematureValueVisitor =
        object : Visitor<Int> {
            override fun expecting(): String = "a map visitor calling nextValue before nextKey"

            override fun <A> visitMap(access: A): SerdeResult<Int>
                where A : MapAccess = access.nextValue(I32Deserialize)
        }

    val mapBoundaryMisuse =
        assertFailsWith<IllegalStateException> {
            buildDeserializer(twoEntryMap).deserializeMap(prematureValueVisitor)
        }
    assertEquals(MISSING_VALUE_BEFORE_KEY_MESSAGE, mapBoundaryMisuse.message)

    val structBoundaryMisuse =
        assertFailsWith<IllegalStateException> {
            buildDeserializer(twoEntryMap).deserializeStruct("Sample", listOf("a", "b"), prematureValueVisitor)
        }
    assertEquals(MISSING_VALUE_BEFORE_KEY_MESSAGE, structBoundaryMisuse.message)

    val expectedVisitorBug = VisitorCallbackBugException("outer visitor bug with unread entries")
    val throwingVisitor =
        object : Visitor<Int> {
            override fun expecting(): String = "a map visitor throwing unchecked exception"

            override fun <A> visitMap(access: A): SerdeResult<Int>
                where A : MapAccess = throw expectedVisitorBug
        }

    val propagatedBug =
        assertFailsWith<VisitorCallbackBugException> {
            buildDeserializer(threeEntryMap).deserializeAny(throwingVisitor)
        }
    assertSame(expectedVisitorBug, propagatedBug)

    val expectedVisitorError = SerdeError("visitor ordinary failure before consuming all entries")
    val expectedVisitorFailure = SerdeResult.failure(expectedVisitorError)
    val failingPartialVisitor =
        object : Visitor<Int> {
            override fun expecting(): String = "a map visitor failing after one entry"

            override fun <A> visitMap(access: A): SerdeResult<Int>
                where A : MapAccess {
                assertEquals("a" to 1, access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                return expectedVisitorFailure
            }
        }

    val returnedFailure = buildDeserializer(threeEntryMap).deserializeMap(failingPartialVisitor)
    assertSame(expectedVisitorFailure, returnedFailure)
    assertSame(expectedVisitorError, returnedFailure.exceptionOrNull())

    val readOneEntryVisitor =
        object : Visitor<Int> {
            override fun expecting(): String = "a map visitor consuming only one entry"

            override fun <A> visitMap(access: A): SerdeResult<Int>
                where A : MapAccess {
                val first = checkNotNull(access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                return SerdeResult.success(first.second)
            }
        }
    val singularRemainingFailure = buildDeserializer(twoEntryMap).deserializeMap(readOneEntryVisitor)
    assertEquals(
        "invalid length 2, expected 1 element in map",
        singularRemainingFailure.exceptionOrNull()?.message,
    )

    val readTwoEntriesVisitor =
        object : Visitor<Int> {
            override fun expecting(): String = "a map visitor consuming two entries"

            override fun <A> visitMap(access: A): SerdeResult<Int>
                where A : MapAccess {
                val first = checkNotNull(access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                val second = checkNotNull(access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                return SerdeResult.success(first.second + second.second)
            }
        }
    val pluralRemainingFailure = buildDeserializer(threeEntryMap).deserializeAny(readTwoEntriesVisitor)
    assertEquals(
        "invalid length 3, expected 2 elements in map",
        pluralRemainingFailure.exceptionOrNull()?.message,
    )
    assertEquals(3, buildDeserializer(twoEntryMap).deserializeMap(readTwoEntriesVisitor).getOrThrow())
}

class ContentMapKeyEntryVisitationTest {
    @Test
    fun ownedMapDeserializerNextKeySeedPreservesPendingValueAndOutcomeIdentity() {
        assertNextKeySeedOutcomeIdentityAndPendingSlot(
            buildDeserializer = { content -> ContentDeserializer.new(content) },
            expectedTextKeyDispatch = "string:dispatchKey",
            expectedBytesKeyDispatch = "byteBuf:1,2",
        )
    }

    @Test
    fun borrowedMapRefDeserializerNextKeySeedPreservesPendingValueAndOutcomeIdentity() {
        repeat(2) {
            assertNextKeySeedOutcomeIdentityAndPendingSlot(
                buildDeserializer = { content -> ContentRefDeserializer.new(content) },
                expectedTextKeyDispatch = "str:dispatchKey",
                expectedBytesKeyDispatch = "bytes:1,2",
            )
        }
    }

    @Test
    fun ownedMapDeserializerNextKeySeedPropagatesThrownExceptionAndRetainsPendingSlot() {
        assertNextKeySeedPropagatesThrownExceptionsAndRetainsPendingValue { content ->
            ContentDeserializer.new(content)
        }
    }

    @Test
    fun borrowedMapRefDeserializerNextKeySeedPropagatesThrownExceptionAndRetainsPendingSlot() {
        repeat(2) {
            assertNextKeySeedPropagatesThrownExceptionsAndRetainsPendingValue { content ->
                ContentRefDeserializer.new(content)
            }
        }
    }

    @Test
    fun ownedMapDeserializerNextEntrySeedPreservesShortCircuitOutcomeAndThrownIdentity() {
        assertNextEntrySeedShortCircuitIdentityAndThrownPropagation { content ->
            ContentDeserializer.new(content)
        }
    }

    @Test
    fun borrowedMapRefDeserializerNextEntrySeedPreservesShortCircuitOutcomeAndThrownIdentity() {
        repeat(2) {
            assertNextEntrySeedShortCircuitIdentityAndThrownPropagation { content ->
                ContentRefDeserializer.new(content)
            }
        }
    }

    @Test
    fun ownedContentMapOuterVisitationPropagatesExceptionsFailureIdentityAndTrailingLength() {
        assertOuterMapVisitationPropagationAndTrailingLength { content ->
            ContentDeserializer.new(content)
        }
    }

    @Test
    fun borrowedContentMapRefOuterVisitationPropagatesExceptionsFailureIdentityAndTrailingLength() {
        repeat(2) {
            assertOuterMapVisitationPropagationAndTrailingLength { content ->
                ContentRefDeserializer.new(content)
            }
        }
    }

    @Test
    fun ownedVariantStructMapVisitationPropagatesExceptionsFailureIdentityAndTrailingLength() {
        val twoFieldMap =
            newMapContent(
                listOf(
                    ContentMapEntry(newStringContent("x"), newI32Content(7)),
                    ContentMapEntry(newStringContent("y"), newI32Content(8)),
                ),
            )
        val threeFieldMap =
            newMapContent(
                listOf(
                    ContentMapEntry(newStringContent("x"), newI32Content(7)),
                    ContentMapEntry(newStringContent("y"), newI32Content(8)),
                    ContentMapEntry(newStringContent("z"), newI32Content(9)),
                ),
            )
        val fields = listOf("x", "y")

        fun <R> decodeStructVariant(
            payload: Content,
            visitor: Visitor<R>,
        ): SerdeResult<R> {
            val enumDe = EnumDeserializer.new(newStringContent("Point"), payload)
            val (variantName, variantAccess) =
                enumDe.variantSeed(seedFromDeserialize(StringDeserialize)).getOrThrow()
            assertEquals("Point", variantName)
            return variantAccess.structVariant(fields, visitor)
        }

        val prematureValueVisitor =
            object : Visitor<Int> {
                override fun expecting(): String = "a struct variant visitor calling nextValue first"

                override fun <A> visitMap(access: A): SerdeResult<Int>
                    where A : MapAccess = access.nextValue(I32Deserialize)
            }
        val misuse =
            assertFailsWith<IllegalStateException> {
                decodeStructVariant(twoFieldMap, prematureValueVisitor)
            }
        assertEquals(MISSING_VALUE_BEFORE_KEY_MESSAGE, misuse.message)

        val expectedBug = VisitorCallbackBugException("struct variant visitor bug")
        val throwingVisitor =
            object : Visitor<Int> {
                override fun expecting(): String = "a struct variant visitor throwing bug"

                override fun <A> visitMap(access: A): SerdeResult<Int>
                    where A : MapAccess = throw expectedBug
            }
        val thrownBug =
            assertFailsWith<VisitorCallbackBugException> {
                decodeStructVariant(threeFieldMap, throwingVisitor)
            }
        assertSame(expectedBug, thrownBug)

        val expectedError = SerdeError("struct variant visitor ordinary failure")
        val expectedFailure = SerdeResult.failure(expectedError)
        val failingPartialVisitor =
            object : Visitor<Int> {
                override fun expecting(): String = "a struct variant visitor failing early"

                override fun <A> visitMap(access: A): SerdeResult<Int>
                    where A : MapAccess {
                    assertEquals("x" to 7, access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                    return expectedFailure
                }
            }
        val failureOutcome = decodeStructVariant(threeFieldMap, failingPartialVisitor)
        assertSame(expectedFailure, failureOutcome)
        assertSame(expectedError, failureOutcome.exceptionOrNull())

        val readOneFieldVisitor =
            object : Visitor<Int> {
                override fun expecting(): String = "a struct variant visitor reading one field"

                override fun <A> visitMap(access: A): SerdeResult<Int>
                    where A : MapAccess {
                    val first = checkNotNull(access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                    return SerdeResult.success(first.second)
                }
            }
        assertEquals(
            "invalid length 2, expected 1 element in map",
            decodeStructVariant(twoFieldMap, readOneFieldVisitor).exceptionOrNull()?.message,
        )

        val readTwoFieldsVisitor =
            object : Visitor<Int> {
                override fun expecting(): String = "a struct variant visitor reading two fields"

                override fun <A> visitMap(access: A): SerdeResult<Int>
                    where A : MapAccess {
                    val first = checkNotNull(access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                    val second = checkNotNull(access.nextEntry(StringDeserialize, I32Deserialize).getOrThrow())
                    return SerdeResult.success(first.second + second.second)
                }
            }
        assertEquals(
            "invalid length 3, expected 2 elements in map",
            decodeStructVariant(threeFieldMap, readTwoFieldsVisitor).exceptionOrNull()?.message,
        )
        assertEquals(15, decodeStructVariant(twoFieldMap, readTwoFieldsVisitor).getOrThrow())
    }
}
