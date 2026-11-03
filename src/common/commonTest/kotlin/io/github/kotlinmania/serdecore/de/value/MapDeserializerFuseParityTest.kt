package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.UnitDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MapDeserializerFuseParityTest {
    private class ResumingInput : Iterator<MapEntry<I32Deserializer, I32Deserializer>> {
        var checks = 0
            private set
        var reads = 0
            private set

        override fun hasNext(): Boolean {
            checks += 1
            return checks == 2
        }

        override fun next(): MapEntry<I32Deserializer, I32Deserializer> {
            if (checks != 2 || reads != 0) throw NoSuchElementException()
            reads += 1
            return MapEntry(I32Deserializer.new(7), I32Deserializer.new(9))
        }
    }

    @Test
    fun keyAccessCannotResumeAfterEnd() {
        val input = ResumingInput()
        val deserializer = mapDeserializer(input)
        assertNull(deserializer.nextKey(I32Deserialize).getOrThrow())
        assertNull(deserializer.nextKey(I32Deserialize).getOrThrow())
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun entryAccessCannotResumeAfterEnd() {
        val input = ResumingInput()
        val deserializer = mapDeserializer(input)
        assertNull(deserializer.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
        assertNull(deserializer.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun sequenceViewCannotResumeAfterEnd() {
        val input = ResumingInput()
        val deserializer = mapDeserializer(input)
        assertEquals(SeqElement.None, deserializer.nextElement(UnitDeserialize).getOrThrow())
        assertEquals(SeqElement.None, deserializer.nextElement(UnitDeserialize).getOrThrow())
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun endDoesNotResumeAfterKeyAccessReportedEnd() {
        val input = ResumingInput()
        val deserializer = mapDeserializer(input)
        assertNull(deserializer.nextKey(I32Deserialize).getOrThrow())
        assertIs<SerdeResult.Success<Unit>>(deserializer.end())
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun mapVisitorReadingEndDoesNotGetAFabricatedLengthError() {
        val input = ResumingInput()
        val visitor =
            object : Visitor<Unit> {
                override fun expecting(): String = "empty map"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<Unit> =
                    access.nextKey(I32Deserialize).map { assertNull(it) }
            }
        assertIs<SerdeResult.Success<Unit>>(mapDeserializer(input).deserializeAny(visitor))
        assertEquals(1, input.checks)
        assertEquals(0, input.reads)
    }

    @Test
    fun exhaustedMapReportsExactZeroHint() {
        val input = ResumingInput()
        val deserializer = mapDeserializer(input)
        assertNull(deserializer.sizeHint())
        assertNull(deserializer.nextKey(I32Deserialize).getOrThrow())
        assertEquals(0, deserializer.sizeHint())
        assertEquals(1, input.checks)
    }

    @Test
    fun endStillCountsUnconsumedEntriesForLengthError() {
        val entries =
            listOf(
                MapEntry(I32Deserializer.new(1), I32Deserializer.new(10)),
                MapEntry(I32Deserializer.new(2), I32Deserializer.new(20)),
            )
        val deserializer = mapDeserializer(entries.iterator())
        assertEquals<Pair<Int, Int>?>(1 to 10, deserializer.nextEntry(I32Deserialize, I32Deserialize).getOrThrow())
        val failure = assertIs<SerdeResult.Failure>(deserializer.end())
        assertEquals("invalid length 2, expected 1 element in map", failure.error.message)
    }
}
