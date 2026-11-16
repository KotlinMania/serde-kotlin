package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import io.github.kotlinmania.serdecore.de.value.UnitDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals

class TupleDecodeBoundaryParityTest {
    private class Item(
        val value: Int,
    )

    private class Codec(
        private val trace: MutableList<String>,
    ) : Deserialize<Item> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Item> =
            I32Deserialize.deserialize(deserializer).flatMap { value ->
                trace.add("value:$value")
                when (value) {
                    -1 -> SerdeResult.failure(SerdeError.custom("codec error"))
                    -2 -> error("codec panic")
                    else -> SerdeResult.success(Item(value))
                }
            }
    }

    private class Driver(
        private val arity: Int,
        private val values: List<Deserializer>,
        private val mode: Int,
        private val at: Int,
        private val trace: MutableList<String>,
    ) : Deserializer by I32Deserializer.new(0) {
        override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> {
            assertEquals(arity, len)
            trace.add("tuple:$len")
            if (mode == 3) return SerdeResult.failure(SerdeError.custom("backend error"))
            if (mode == 4) error("backend panic")
            var index = 0
            return visitor.visitSeq(
                object : SeqAccess {
                    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> {
                        val current = index++
                        trace.add("pull:$current")
                        if (current == at) {
                            if (mode == 1) return SerdeResult.failure(SerdeError.custom("access error"))
                            if (mode == 2) error("access panic")
                        }
                        return if (current == values.size) {
                            SerdeResult.success(SeqElement.None)
                        } else {
                            seed.deserialize(values[current]).map { SeqElement.Some(it) }
                        }
                    }
                },
            )
        }
    }

    private fun check(
        arity: Int,
        input: List<Int>,
        mode: Int,
        at: Int,
        outcome: String,
        values: List<Int>,
        events: List<String>,
    ) {
        val trace = mutableListOf<String>()
        val codec = Codec(trace)
        val driver = Driver(arity, input.map { I32Deserializer.new(it) }, mode, at, trace)
        var decoded = emptyList<Int>()
        val actual =
            try {
                val result =
                    if (arity == 2) {
                        pairDeserialize(codec, codec).deserialize(driver).map { listOf(it.first.value, it.second.value) }
                    } else {
                        tripleDeserialize(codec, codec, codec).deserialize(driver).map { listOf(it.first.value, it.second.value, it.third.value) }
                    }
                when (result) {
                    is SerdeResult.Failure -> {
                        "error:${result.error}"
                    }

                    is SerdeResult.Success -> {
                        decoded = result.value
                        "ok"
                    }
                }
            } catch (exception: IllegalStateException) {
                "panic:${exception.message}"
            }
        assertEquals(outcome, actual)
        assertEquals(values, decoded)
        assertEquals(events, trace)
    }

    @Test
    fun success() {
        check(2, listOf(10, 20), 0, 0, "ok", listOf(10, 20), listOf("tuple:2", "pull:0", "value:10", "pull:1", "value:20"))
        check(3, listOf(10, 20, 30), 0, 0, "ok", listOf(10, 20, 30), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2", "value:30"))
    }

    @Test
    fun missingElements() {
        check(2, emptyList(), 0, 0, "error:invalid length 0, expected a tuple of size 2", emptyList(), listOf("tuple:2", "pull:0"))
        check(2, listOf(10), 0, 0, "error:invalid length 1, expected a tuple of size 2", emptyList(), listOf("tuple:2", "pull:0", "value:10", "pull:1"))
        check(3, emptyList(), 0, 0, "error:invalid length 0, expected a tuple of size 3", emptyList(), listOf("tuple:3", "pull:0"))
        check(3, listOf(10), 0, 0, "error:invalid length 1, expected a tuple of size 3", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1"))
        check(3, listOf(10, 20), 0, 0, "error:invalid length 2, expected a tuple of size 3", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2"))
    }

    @Test
    fun codecErrors() {
        check(2, listOf(-1, 20), 0, 0, "error:codec error", emptyList(), listOf("tuple:2", "pull:0", "value:-1"))
        check(2, listOf(10, -1), 0, 0, "error:codec error", emptyList(), listOf("tuple:2", "pull:0", "value:10", "pull:1", "value:-1"))
        check(3, listOf(-1, 20, 30), 0, 0, "error:codec error", emptyList(), listOf("tuple:3", "pull:0", "value:-1"))
        check(3, listOf(10, -1, 30), 0, 0, "error:codec error", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:-1"))
        check(3, listOf(10, 20, -1), 0, 0, "error:codec error", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2", "value:-1"))
    }

    @Test
    fun codecPanics() {
        check(2, listOf(-2, 20), 0, 0, "panic:codec panic", emptyList(), listOf("tuple:2", "pull:0", "value:-2"))
        check(2, listOf(10, -2), 0, 0, "panic:codec panic", emptyList(), listOf("tuple:2", "pull:0", "value:10", "pull:1", "value:-2"))
        check(3, listOf(-2, 20, 30), 0, 0, "panic:codec panic", emptyList(), listOf("tuple:3", "pull:0", "value:-2"))
        check(3, listOf(10, -2, 30), 0, 0, "panic:codec panic", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:-2"))
        check(3, listOf(10, 20, -2), 0, 0, "panic:codec panic", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2", "value:-2"))
    }

    @Test
    fun accessErrors() {
        check(2, listOf(10, 20), 1, 0, "error:access error", emptyList(), listOf("tuple:2", "pull:0"))
        check(2, listOf(10, 20), 1, 1, "error:access error", emptyList(), listOf("tuple:2", "pull:0", "value:10", "pull:1"))
        check(3, listOf(10, 20, 30), 1, 0, "error:access error", emptyList(), listOf("tuple:3", "pull:0"))
        check(3, listOf(10, 20, 30), 1, 1, "error:access error", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1"))
        check(3, listOf(10, 20, 30), 1, 2, "error:access error", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2"))
    }

    @Test
    fun accessPanics() {
        check(2, listOf(10, 20), 2, 0, "panic:access panic", emptyList(), listOf("tuple:2", "pull:0"))
        check(2, listOf(10, 20), 2, 1, "panic:access panic", emptyList(), listOf("tuple:2", "pull:0", "value:10", "pull:1"))
        check(3, listOf(10, 20, 30), 2, 0, "panic:access panic", emptyList(), listOf("tuple:3", "pull:0"))
        check(3, listOf(10, 20, 30), 2, 1, "panic:access panic", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1"))
        check(3, listOf(10, 20, 30), 2, 2, "panic:access panic", emptyList(), listOf("tuple:3", "pull:0", "value:10", "pull:1", "value:20", "pull:2"))
    }

    @Test
    fun backendErrors() {
        check(2, listOf(10, 20), 3, 0, "error:backend error", emptyList(), listOf("tuple:2"))
        check(3, listOf(10, 20, 30), 3, 0, "error:backend error", emptyList(), listOf("tuple:3"))
    }

    @Test
    fun backendPanics() {
        check(2, listOf(10, 20), 4, 0, "panic:backend panic", emptyList(), listOf("tuple:2"))
        check(3, listOf(10, 20, 30), 4, 0, "panic:backend panic", emptyList(), listOf("tuple:3"))
    }

    @Test
    fun nullablePresence() {
        val codec = nullableDeserialize(I32Deserialize)
        val pair =
            pairDeserialize(codec, codec)
                .deserialize(
                    Driver(2, listOf(UnitDeserializer.new(), UnitDeserializer.new()), 0, 0, mutableListOf()),
                ).getOrThrow()
        val triple =
            tripleDeserialize(codec, codec, codec)
                .deserialize(
                    Driver(3, listOf(UnitDeserializer.new(), UnitDeserializer.new(), UnitDeserializer.new()), 0, 0, mutableListOf()),
                ).getOrThrow()
        assertEquals(null to null, pair)
        assertEquals(Triple(null, null, null), triple)
    }
}
