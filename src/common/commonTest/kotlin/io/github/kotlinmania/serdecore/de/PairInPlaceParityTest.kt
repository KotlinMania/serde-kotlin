package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import io.github.kotlinmania.serdecore.de.value.UnitDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PairInPlaceParityTest {
    private class Item(
        var value: Int,
    )

    private class Codec(
        private val trace: MutableList<String>,
    ) : Deserialize<Item> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Item> =
            I32Deserialize.deserialize(deserializer).flatMap { value ->
                trace.add("new:$value")
                when (value) {
                    -1 -> SerdeResult.failure(SerdeError.custom("rejected"))
                    -2 -> error("sentinel")
                    else -> SerdeResult.success(Item(value))
                }
            }

        override fun <D : Deserializer> deserializeInPlace(
            deserializer: D,
            place: MutablePlace<Item>,
        ): SerdeResult<Unit> =
            I32Deserialize.deserialize(deserializer).flatMap { value ->
                trace.add("in:${place.value.value}:$value")
                place.value.value =
                    when (value) {
                        -1 -> 700
                        -2 -> 800
                        else -> value
                    }
                when (value) {
                    -1 -> SerdeResult.failure(SerdeError.custom("rejected"))
                    -2 -> error("sentinel")
                    else -> SerdeResult.success(Unit)
                }
            }
    }

    private class Driver(
        private val values: List<Deserializer>,
    ) : Deserializer by I32Deserializer.new(0) {
        override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> {
            assertEquals(2, len)
            var index = 0
            return visitor.visitSeq(
                object : SeqAccess {
                    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
                        if (index == values.size) {
                            SerdeResult.success(SeqElement.None)
                        } else {
                            seed.deserialize(values[index++]).map { SeqElement.Some(it) }
                        }
                },
            )
        }
    }

    private fun check(input: List<Int>, state: Pair<Int, Int>, events: List<String>, outcome: String) {
        val trace = mutableListOf<String>()
        val first = Item(1)
        val second = Item(2)
        var pair = first to second
        val codec = Codec(trace)
        val actual =
            try {
                when (
                    val result =
                        pairDeserialize(codec, codec).deserializeInPlace(
                            Driver(input.map { I32Deserializer.new(it) }),
                            mutablePlace({ pair }, { pair = it }),
                        )
                ) {
                    is SerdeResult.Success -> "ok"
                    is SerdeResult.Failure -> "error:${result.error}"
                }
            } catch (exception: IllegalStateException) {
                assertEquals("sentinel", exception.message)
                "panic"
            }
        assertEquals(state, pair.first.value to pair.second.value)
        assertSame(first, pair.first)
        assertSame(second, pair.second)
        assertEquals(events, trace)
        assertEquals(outcome, actual)
    }

    @Test
    fun success() = check(listOf(10, 20), 10 to 20, listOf("in:1:10", "in:2:20"), "ok")

    @Test
    fun empty() = check(emptyList(), 1 to 2, emptyList(), "error:invalid length 0, expected a tuple of size 2")

    @Test
    fun missingSecond() = check(listOf(10), 10 to 2, listOf("in:1:10"), "error:invalid length 1, expected a tuple of size 2")

    @Test
    fun firstError() = check(listOf(-1, 20), 700 to 2, listOf("in:1:-1"), "error:rejected")

    @Test
    fun secondError() = check(listOf(10, -1), 10 to 700, listOf("in:1:10", "in:2:-1"), "error:rejected")

    @Test
    fun firstPanic() = check(listOf(-2, 20), 800 to 2, listOf("in:1:-2"), "panic")

    @Test
    fun secondPanic() = check(listOf(10, -2), 10 to 800, listOf("in:1:10", "in:2:-2"), "panic")

    @Test
    fun primitiveFields() {
        var pair = 1 to 2
        val original = pair
        pairDeserialize(I32Deserialize, I32Deserialize)
            .deserializeInPlace(
                Driver(listOf(I32Deserializer.new(10), I32Deserializer.new(20))),
                mutablePlace({ pair }, { pair = it }),
            ).getOrThrow()
        assertEquals(10 to 20, pair)
        assertEquals(1 to 2, original)
    }

    @Test
    fun nullableFields() {
        var pair: Pair<Int?, Int?> = 1 to 2
        pairDeserialize(nullableDeserialize(I32Deserialize), nullableDeserialize(I32Deserialize))
            .deserializeInPlace(
                Driver(listOf(UnitDeserializer.new(), UnitDeserializer.new())),
                mutablePlace({ pair }, { pair = it }),
            ).getOrThrow()
        assertEquals(null to null, pair)
    }
}
