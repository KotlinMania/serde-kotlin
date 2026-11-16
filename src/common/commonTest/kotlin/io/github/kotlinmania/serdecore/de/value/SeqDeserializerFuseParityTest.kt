package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SeqDeserializerFuseParityTest {
    private class ResumingInput : Iterator<I32Deserializer> {
        var checks = 0
            private set
        var reads = 0
            private set

        override fun hasNext(): Boolean {
            checks += 1
            return checks == 2
        }

        override fun next(): I32Deserializer {
            if (checks != 2 || reads != 0) throw NoSuchElementException()
            reads += 1
            return I32Deserializer.new(7)
        }
    }

    @Test
    fun exhaustionPreventsLaterIteratorValuesFromReappearing() {
        val input = ResumingInput()
        val deserializer = seqDeserializer(input)
        assertEquals(SeqElement.None, deserializer.nextElement(I32Deserialize).getOrThrow())
        assertEquals(SeqElement.None, deserializer.nextElement(I32Deserialize).getOrThrow())
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun endDoesNotResumeAnIteratorAfterObservedExhaustion() {
        val input = ResumingInput()
        val deserializer = seqDeserializer(input)
        assertEquals(SeqElement.None, deserializer.nextElement(I32Deserialize).getOrThrow())
        assertIs<SerdeResult.Success<Unit>>(deserializer.end())
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun visitorThatReadsEndDoesNotProduceAFabricatedLengthError() {
        val input = ResumingInput()
        val visitor =
            object : Visitor<Unit> {
                override fun expecting(): String = "empty sequence"

                override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Unit> =
                    access.nextElement(I32Deserialize).map {
                        assertEquals(SeqElement.None, it)
                    }
            }
        assertIs<SerdeResult.Success<Unit>>(seqDeserializer(input).deserializeAny(visitor))
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun exhaustedSequenceReportsExactZeroHint() {
        val input = ResumingInput()
        val deserializer = seqDeserializer(input)
        assertNull(deserializer.sizeHint())
        assertEquals(SeqElement.None, deserializer.nextElement(I32Deserialize).getOrThrow())
        assertEquals(0, deserializer.sizeHint())
        assertEquals(1, input.checks)
    }

    @Test
    fun endStillCountsUnconsumedElementsForTheLengthError() {
        val deserializer = seqDeserializer(listOf(I32Deserializer.new(1), I32Deserializer.new(2)).iterator())
        assertEquals(SeqElement.Some(1), deserializer.nextElement(I32Deserialize).getOrThrow())
        val failure = assertIs<SerdeResult.Failure>(deserializer.end())
        assertEquals("invalid length 2, expected 1 element in sequence", failure.error.message)
    }
}
