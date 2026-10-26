package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.*
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import kotlin.test.*

class GeneratedSeedAdoptionTest {
    @Test fun consumedSeedPreservesPlaceDispatchAndAllOutcomes() {
        for (mode in listOf("success", "failure", "panic")) {
            var value = 4
            var calls = 0
            val place = mutablePlace({ value }, { value = it })
            val input = I32Deserializer.new(9)
            val failure = SerdeResult.failure(SerdeError.custom("rejected"))
            val panic = IllegalStateException("sentinel")
            val codec = object : Deserialize<Int> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Int> = error("wrong dispatch")
                override fun <D : Deserializer> deserializeInPlace(deserializer: D, place: MutablePlace<Int>): SerdeResult<Unit> {
                    assertSame<Deserializer>(input, deserializer)
                    calls += 1
                    place.value = 9
                    return when (mode) {
                        "failure" -> failure
                        "panic" -> throw panic
                        else -> SerdeResult.success(Unit)
                    }
                }
            }
            val seed = InPlaceSeed(place, codec)
            assertSame(place, seed.__rtkField0)
            when (mode) {
                "failure" -> assertSame(failure, seed.deserialize(input))
                "panic" -> assertSame(panic, assertFailsWith<IllegalStateException> { seed.deserialize(input) })
                else -> seed.deserialize(input).getOrThrow()
            }
            assertEquals(9, value)
            assertEquals("consuming Rust value already moved", assertFailsWith<IllegalStateException> {
                seed.deserialize(input)
            }.message)
            assertEquals(1, calls)
            assertEquals(9, value)
        }
    }

    @Test fun productionListConsumerSuppliesAConsumingSeed() {
        val values = mutableListOf(4)
        var calls = 0
        val format = object : Deserializer by I32Deserializer.new(0) {
            override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSeq(object : SeqAccess {
                private var exhausted = false
                override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> {
                    if (exhausted) return SerdeResult.success(SeqElement.None)
                    exhausted = true
                    calls += 1
                    val result = seed.deserialize(I32Deserializer.new(9))
                    assertEquals("consuming Rust value already moved", assertFailsWith<IllegalStateException> {
                        seed.deserialize(I32Deserializer.new(10))
                    }.message)
                    return result.map { SeqElement.Some(it) }
                }
            })
        }
        mutableListDeserialize(I32Deserialize).deserializeInPlace(
            format, mutablePlace({ values }, { error("list replaced") }),
        ).getOrThrow()
        assertEquals(listOf(9), values)
        assertEquals(1, calls)
    }
}
