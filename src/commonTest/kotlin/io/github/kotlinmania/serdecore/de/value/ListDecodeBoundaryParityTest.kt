package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.mutableListDeserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ListDecodeBoundaryParityTest {
    private class Codec(
        private val trace: MutableList<Int>,
    ) : Deserialize<Int> {
        val typedError = SerdeError("codec error")

        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            I32Deserialize.deserialize(deserializer).flatMap { value ->
                trace.add(value)
                when (value) {
                    -2 -> error("codec panic")
                    -1 -> SerdeResult.failure(typedError)
                    else -> SerdeResult.success(value)
                }
            }
    }

    private class Access(
        private val input: List<Int>,
        private val mode: Int,
    ) : SeqAccess {
        private var index = 0
        var polls = 0
        var hints = 0

        override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> {
            polls++
            if (mode == 2) error("access panic")
            if (index == input.size) return SerdeResult.success(SeqElement.None)
            return seed.deserialize(I32Deserializer.new(input[index++])).map { SeqElement.Some(it) }
        }

        override fun sizeHint(): Int {
            hints++
            if (mode == 1) error("hint panic")
            return input.size - index
        }
    }

    private class Driver(
        private val access: Access,
    ) : Deserializer by I32Deserializer.new(0) {
        override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSeq(access)
    }

    private class Fixture(
        input: List<Int>,
        mode: Int = 0,
    ) {
        private val trace = mutableListOf<Int>()
        val codec = Codec(trace)
        private val access = Access(input, mode)

        fun decode(): SerdeResult<MutableList<Int>> = mutableListDeserialize(codec).deserialize(Driver(access))

        fun assertObservations(
            polls: Int,
            hints: Int,
            trace: List<Int>,
        ) {
            assertEquals(polls, access.polls)
            assertEquals(hints, access.hints)
            assertEquals(trace, this.trace)
        }
    }

    @Test
    fun successRetainsOrder() {
        val fixture = Fixture(listOf(1, 2))
        assertEquals(listOf(1, 2), fixture.decode().getOrThrow())
        fixture.assertObservations(3, 1, listOf(1, 2))
    }

    @Test
    fun emptySucceeds() {
        val fixture = Fixture(emptyList())
        assertEquals(emptyList(), fixture.decode().getOrThrow())
        fixture.assertObservations(1, 1, emptyList())
    }

    @Test
    fun hintPanicPrecedesAccess() {
        val fixture = Fixture(listOf(1, 2), mode = 1)
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("hint panic", failure.message)
        fixture.assertObservations(0, 1, emptyList())
    }

    @Test
    fun accessPanicPrecedesCodec() {
        val fixture = Fixture(listOf(1, 2), mode = 2)
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("access panic", failure.message)
        fixture.assertObservations(1, 1, emptyList())
    }

    @Test
    fun firstCodecPanicStopsInput() {
        val fixture = Fixture(listOf(-2, 3))
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("codec panic", failure.message)
        fixture.assertObservations(1, 1, listOf(-2))
    }

    @Test
    fun secondCodecPanicStopsInput() {
        val fixture = Fixture(listOf(1, -2, 3))
        val failure = assertFailsWith<IllegalStateException> { fixture.decode() }
        assertEquals("codec panic", failure.message)
        fixture.assertObservations(2, 1, listOf(1, -2))
    }

    @Test
    fun firstTypedErrorStopsInput() {
        val fixture = Fixture(listOf(-1, 3))
        assertSame(fixture.codec.typedError, fixture.decode().exceptionOrNull())
        fixture.assertObservations(1, 1, listOf(-1))
    }

    @Test
    fun secondTypedErrorStopsInput() {
        val fixture = Fixture(listOf(1, -1, 3))
        assertSame(fixture.codec.typedError, fixture.decode().exceptionOrNull())
        fixture.assertObservations(2, 1, listOf(1, -1))
    }
}
