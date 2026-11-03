package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import io.github.kotlinmania.serdecore.de.value.UnitDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class TripleInPlaceParityTest {
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
            assertEquals(3, len)
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

    private fun check(input: List<Int>, state: Triple<Int, Int, Int>, events: List<String>, outcome: String) {
        val trace = mutableListOf<String>()
        val first = Item(1)
        val second = Item(2)
        val third = Item(3)
        var triple = Triple(first, second, third)
        val codec = Codec(trace)
        val actual =
            try {
                when (
                    val result =
                        tripleDeserialize(codec, codec, codec).deserializeInPlace(
                            Driver(input.map { I32Deserializer.new(it) }),
                            mutablePlace({ triple }, { triple = it }),
                        )
                ) {
                    is SerdeResult.Success -> "ok"
                    is SerdeResult.Failure -> "error:${result.error}"
                }
            } catch (exception: IllegalStateException) {
                assertEquals("sentinel", exception.message)
                "panic"
            }
        assertEquals(state, Triple(triple.first.value, triple.second.value, triple.third.value))
        assertSame(first, triple.first)
        assertSame(second, triple.second)
        assertSame(third, triple.third)
        assertEquals(events, trace)
        assertEquals(outcome, actual)
    }

    @Test
    fun success() = check(listOf(10, 20, 30), Triple(10, 20, 30), listOf("in:1:10", "in:2:20", "in:3:30"), "ok")

    @Test
    fun empty() = check(emptyList(), Triple(1, 2, 3), emptyList(), "error:invalid length 0, expected a tuple of size 3")

    @Test
    fun missingSecond() = check(listOf(10), Triple(10, 2, 3), listOf("in:1:10"), "error:invalid length 1, expected a tuple of size 3")

    @Test
    fun missingThird() = check(listOf(10, 20), Triple(10, 20, 3), listOf("in:1:10", "in:2:20"), "error:invalid length 2, expected a tuple of size 3")

    @Test
    fun firstError() = check(listOf(-1, 20, 30), Triple(700, 2, 3), listOf("in:1:-1"), "error:rejected")

    @Test
    fun secondError() = check(listOf(10, -1, 30), Triple(10, 700, 3), listOf("in:1:10", "in:2:-1"), "error:rejected")

    @Test
    fun thirdError() = check(listOf(10, 20, -1), Triple(10, 20, 700), listOf("in:1:10", "in:2:20", "in:3:-1"), "error:rejected")

    @Test
    fun firstPanic() = check(listOf(-2, 20, 30), Triple(800, 2, 3), listOf("in:1:-2"), "panic")

    @Test
    fun secondPanic() = check(listOf(10, -2, 30), Triple(10, 800, 3), listOf("in:1:10", "in:2:-2"), "panic")

    @Test
    fun thirdPanic() = check(listOf(10, 20, -2), Triple(10, 20, 800), listOf("in:1:10", "in:2:20", "in:3:-2"), "panic")

    @Test
    fun primitiveFields() {
        var triple = Triple(1, 2, 3)
        tripleDeserialize(I32Deserialize, I32Deserialize, I32Deserialize)
            .deserializeInPlace(
                Driver(listOf(I32Deserializer.new(10), I32Deserializer.new(20), I32Deserializer.new(30))),
                mutablePlace({ triple }, { triple = it }),
            ).getOrThrow()
        assertEquals(Triple(10, 20, 30), triple)
    }

    @Test
    fun primitivePartial() {
        var triple = Triple(1, 2, 3)
        val original = triple
        val result =
            tripleDeserialize(I32Deserialize, I32Deserialize, I32Deserialize)
                .deserializeInPlace(
                    Driver(listOf(I32Deserializer.new(10), I32Deserializer.new(20))),
                    mutablePlace({ triple }, { triple = it }),
                )
        val error = assertIs<SerdeResult.Failure>(result).error
        assertEquals("invalid length 2, expected a tuple of size 3", error.toString())
        assertEquals(Triple(10, 20, 3), triple)
        assertEquals(Triple(1, 2, 3), original)
    }

    @Test
    fun nullableFields() {
        var triple: Triple<Int?, Int?, Int?> = Triple(1, 2, 3)
        val codec = nullableDeserialize(I32Deserialize)
        tripleDeserialize(codec, codec, codec)
            .deserializeInPlace(
                Driver(listOf(UnitDeserializer.new(), UnitDeserializer.new(), UnitDeserializer.new())),
                mutablePlace({ triple }, { triple = it }),
            ).getOrThrow()
        assertEquals(Triple(null, null, null), triple)
    }
}
