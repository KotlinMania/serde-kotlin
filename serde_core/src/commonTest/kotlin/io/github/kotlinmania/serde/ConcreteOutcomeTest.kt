package io.github.kotlinmania.serde

import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.value.StrDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConcreteOutcomeTest {
    @Test
    fun textDecodingRetainsConcreteValue() {
        val outcome = SerdeDecoding.text(SerdeDecodeInput.text("hello"))
        val value: String? = outcome.value
        assertEquals("hello", value)
        assertTrue(outcome.isSuccess)
        assertNull(outcome.error)
    }

    @Test
    fun falseZeroAndEmptyAreSuccessfulValues() {
        assertEquals(false, SerdeDecoding.boolean(SerdeDecodeInput.boolean(false)).value)
        assertEquals(0L, SerdeDecoding.signed(SerdeDecodeInput.signed(0)).value)
        assertEquals("", SerdeDecoding.text(SerdeDecodeInput.text("")).value)
    }

    @Test
    fun unitHasExplicitCompletion() {
        val outcome = SerdeDecoding.unit(SerdeDecodeInput.unit())
        assertTrue(outcome.completed)
        assertNull(outcome.error)
    }

    @Test
    fun concreteDecodingPreservesOriginalDiagnostic() {
        val error = SerdeError.custom("source diagnostic")
        val decoder = object : Deserializer by StrDeserializer.new("unused") {
            override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(error)
        }
        val outcome = SerdeDecoding.text(SerdeDecodeInput.fromDeserializer(decoder))
        assertTrue(outcome.isFailure)
        assertNull(outcome.value)
        assertSame(error, outcome.error)
    }

    @Test
    fun failureFactoriesRetainCauseIdentity() {
        val source = SerdeError.custom("cause")
        val error = SerdeError("diagnostic", source)
        val outcome = SerdeLongOutcome.failure(error)
        assertNull(outcome.value)
        assertSame(error, outcome.error)
        assertSame(source, outcome.error?.source)
        assertFalse(SerdeUnitOutcome.failure(error).completed)
    }

    @Test
    fun throwingBoundaryPreservesOriginalDiagnostic() {
        val error = SerdeError.custom("original")
        val caught: SerdeResult<Unit> = serdeCatching { error.raise() }
        assertSame(error, caught.exceptionOrNull())
        assertSame(error, serdeCatching { SerdeStringOutcome.failure(error).getOrThrow() }.exceptionOrNull())
    }
}
