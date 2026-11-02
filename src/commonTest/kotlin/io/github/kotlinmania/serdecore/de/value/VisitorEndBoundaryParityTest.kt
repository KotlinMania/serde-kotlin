package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.unwrapOrElse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VisitorEndBoundaryParityTest {
    private enum class Mode {
        PANIC,
        ERROR,
        EMPTY,
        PARTIAL,
        ALL,
    }

    private class Input<T>(
        private val values: List<T>,
        private val read: () -> Unit,
    ) : Iterator<T> {
        private var index = 0

        override fun hasNext(): Boolean = index < values.size

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            read()
            return values[index++]
        }
    }

    private class PanicInput<T> : Iterator<T> {
        private var fired = false

        override fun hasNext(): Boolean = !fired

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            fired = true
            error("iterator panic")
        }
    }

    private object PairSum : Visitor<Int> {
        override fun expecting(): String = "pair"

        override fun <A> visitSeq(access: A): SerdeResult<Int> where A : SeqAccess =
            access.nextElement(I32Deserialize).flatMap { key ->
                access.nextElement(I32Deserialize).map { value ->
                    key.unwrapOrElse { error("missing key") } + value.unwrapOrElse { error("missing value") }
                }
            }
    }

    private object PairSeed : DeserializeSeed<Int> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            deserializer.deserializeTuple(2, PairSum)
    }

    private class Visit(
        private val mode: Mode,
        private val pairs: Boolean = false,
    ) : Visitor<Int> {
        override fun expecting(): String = "container"

        override fun <A> visitSeq(access: A): SerdeResult<Int> where A : SeqAccess {
            when (mode) {
                Mode.PANIC -> error("visitor panic")
                Mode.ERROR -> return SerdeResult.failure(SerdeError("visitor error"))
                Mode.EMPTY -> return SerdeResult.success(0)
                else -> Unit
            }
            var sum = 0
            while (true) {
                val next = if (pairs) access.nextElementSeed(PairSeed) else access.nextElement(I32Deserialize)
                when (next) {
                    is SerdeResult.Failure -> {
                        return next
                    }

                    is SerdeResult.Success -> {
                        when (val item = next.value) {
                            SeqElement.None -> return SerdeResult.success(sum)
                            is SeqElement.Some -> sum += item.value
                        }
                    }
                }
                if (mode == Mode.PARTIAL) return SerdeResult.success(sum)
            }
        }

        override fun <A> visitMap(access: A): SerdeResult<Int> where A : MapAccess {
            when (mode) {
                Mode.PANIC -> error("visitor panic")
                Mode.ERROR -> return SerdeResult.failure(SerdeError("visitor error"))
                Mode.EMPTY -> return SerdeResult.success(0)
                else -> Unit
            }
            var sum = 0
            while (true) {
                when (val next = access.nextEntry(I32Deserialize, I32Deserialize)) {
                    is SerdeResult.Failure -> {
                        return next
                    }

                    is SerdeResult.Success -> {
                        val pair = next.value ?: return SerdeResult.success(sum)
                        sum += pair.first + pair.second
                    }
                }
                if (mode == Mode.PARTIAL) return SerdeResult.success(sum)
            }
        }
    }

    private fun run(
        mode: Mode,
        lane: Int,
        read: () -> Unit,
    ): SerdeResult<Int> =
        if (lane == 0) {
            seqDeserializer(Input(listOf(I32Deserializer.new(10), I32Deserializer.new(20)), read)).deserializeAny(Visit(mode))
        } else {
            val input =
                Input(
                    listOf(MapEntry(I32Deserializer.new(1), I32Deserializer.new(10)), MapEntry(I32Deserializer.new(2), I32Deserializer.new(20))),
                    read,
                )
            val map = mapDeserializer(input)
            if (lane == 1) map.deserializeAny(Visit(mode, true)) else map.deserializeSeq(Visit(mode, true))
        }

    private fun visitorPanic(lane: Int) {
        var reads = 0
        val error = assertFailsWith<IllegalStateException> { run(Mode.PANIC, lane) { reads++ } }
        assertEquals("visitor panic", error.message)
        assertEquals(0, reads)
    }

    @Test
    fun sequenceVisitorPanicSkipsEnd() = visitorPanic(0)

    @Test
    fun mapVisitorPanicSkipsEnd() = visitorPanic(1)

    @Test
    fun mapSequenceVisitorPanicSkipsEnd() = visitorPanic(2)

    @Test
    fun sequenceEndIteratorPanicPropagates() {
        val seq = seqDeserializer(PanicInput<I32Deserializer>())
        val error = assertFailsWith<IllegalStateException> { seq.deserializeAny(Visit(Mode.EMPTY)) }
        assertEquals("iterator panic", error.message)
    }

    @Test
    fun mapEndIteratorPanicPropagates() {
        val map = mapDeserializer(PanicInput<MapEntry<I32Deserializer, I32Deserializer>>())
        val error = assertFailsWith<IllegalStateException> { map.deserializeAny(Visit(Mode.EMPTY)) }
        assertEquals("iterator panic", error.message)
    }

    @Test
    fun typedVisitorErrorsSkipEnd() {
        for (lane in 0..2) {
            var reads = 0
            assertEquals("visitor error", run(Mode.ERROR, lane) { reads++ }.exceptionOrNull()?.message)
            assertEquals(0, reads)
        }
    }

    @Test
    fun partialVisitorsRetainLengthErrors() {
        for (lane in 0..2) {
            var reads = 0
            val container = if (lane == 0) "sequence" else "map"
            assertEquals("invalid length 2, expected 1 element in $container", run(Mode.PARTIAL, lane) { reads++ }.exceptionOrNull()?.message)
            assertEquals(2, reads)
        }
    }

    @Test
    fun completeVisitorsSucceed() {
        for (lane in 0..2) {
            var reads = 0
            assertEquals(if (lane == 0) 30 else 33, run(Mode.ALL, lane) { reads++ }.getOrThrow())
            assertEquals(2, reads)
        }
    }
}
