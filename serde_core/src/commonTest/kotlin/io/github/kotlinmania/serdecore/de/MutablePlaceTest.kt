package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import kotlin.test.*

class MutablePlaceTest {
    @Test fun replacementWritesTheCallerSlotAndRetainsFailureIdentity() {
        var value = 4
        val place = mutablePlace({ value }, { value = it })
        I32Deserialize.deserializeInPlace(I32Deserializer.new(9), place).getOrThrow()
        assertEquals(9, value)
        val error = SerdeError.custom("rejected")
        val failed = object : Deserialize<Int> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Int> = SerdeResult.failure(error)
        }
        assertSame(error, assertIs<SerdeResult.Failure>(failed.deserializeInPlace(I32Deserializer.new(0), place)).error)
        assertEquals(9, value)
    }

    @Test fun placeAndDefaultSetterPropagateExceptions() {
        val panic = IllegalStateException("sentinel")
        val throwing = object : Deserialize<Int> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Int> = throw panic
        }
        var value = 4
        assertSame(panic, assertFailsWith<IllegalStateException> {
            throwing.deserializeInPlace(I32Deserializer.new(0), mutablePlace({ value }, { value = it }))
        })
        assertSame(panic, assertFailsWith<IllegalStateException> {
            throwing.deserializeInPlace(I32Deserializer.new(0)) { value = it }
        })
        assertEquals(4, value)
        assertSame(panic, assertFailsWith<IllegalStateException> {
            I32Deserialize.deserializeInPlace(I32Deserializer.new(0), mutablePlace({ value }, { throw panic }))
        })
    }

    @Test fun defaultPlaceDispatchesToTheExistingSetterOverrideOnce() {
        var calls = 0
        var value = 4
        val codec = object : Deserialize<Int> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Int> = error("wrong dispatch")
            override fun <D : Deserializer> deserializeInPlace(deserializer: D, place: (Int) -> Unit): SerdeResult<Unit> {
                calls += 1
                place(42)
                return SerdeResult.success(Unit)
            }
        }
        codec.deserializeInPlace(I32Deserializer.new(0), mutablePlace({ value }, { value = it })).getOrThrow()
        assertEquals(1, calls)
        assertEquals(42, value)
    }

    @Test fun listsReuseExistingElementsAndPreserveCallerStorage() {
        val trace = mutableListOf<String>()
        val first = PlaceItem(1)
        val original = mutableListOf(first, PlaceItem(2))
        var values = original
        mutableListDeserialize(PlaceItemCodec(trace)).deserializeInPlace(
            DirectPlaceSequence(listOf(10, 20, 30)), mutablePlace({ values }, { values = it }),
        ).getOrThrow()
        assertSame(original, values)
        assertSame(first, values[0])
        assertEquals(listOf(10, 20, 30), values.map { it.value })
        assertEquals(listOf("in:1:10", "in:2:20", "new:30"), trace)
    }

    @Test fun oldListSetterStillReplacesTheList() {
        val original = mutableListOf(1, 2)
        var values = original
        mutableListDeserialize(I32Deserialize).deserializeInPlace(
            listOf(9.intoDeserializer()).intoDeserializer(),
        ) { values = it }.getOrThrow()
        assertNotSame(original, values)
        assertEquals(listOf(1, 2), original)
        assertEquals(listOf(9), values)
    }

    @Test fun nullableCallerSlotCanBeReplacedWithNull() {
        var value: Int? = 4
        val codec = object : Deserialize<Int?> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Int?> = SerdeResult.success(null)
        }
        codec.deserializeInPlace(I32Deserializer.new(0), mutablePlace({ value }, { value = it })).getOrThrow()
        assertNull(value)
    }

    @Test fun vectorMutationAndDispatchObservations() {
        assertEquals(listOf(
            "empty|state=|trace=|result=ok",
            "truncate|state=10|trace=in:1:10|result=ok",
            "equal|state=10,20|trace=in:1:10;in:2:20|result=ok",
            "append|state=10,20,30|trace=in:1:10;new:20;new:30|result=ok",
            "empty-append|state=10,20|trace=new:10;new:20|result=ok",
            "error-existing|state=10,700,3|trace=in:1:10;in:2:-1|result=error:rejected",
            "error-append|state=10|trace=in:1:10;new:-1|result=error:rejected",
            "panic-existing|state=10,800,3|trace=in:1:10;in:2:-2|result=panic",
            "panic-append|state=10|trace=in:1:10;new:-2|result=panic",
        ), mutablePlaceObservations())
    }

    @Test fun existingValueSequenceStillHasItsOwnExceptionConversion() {
        val values = mutableListOf(PlaceItem(1))
        val result = mutableListDeserialize(PlaceItemCodec(mutableListOf())).deserializeInPlace(
            listOf((-2).intoDeserializer()).intoDeserializer(), mutablePlace({ values }, { error("list replaced") }),
        )
        assertEquals("sentinel", assertIs<SerdeResult.Failure>(result).error.message)
        assertEquals(800, values.single().value)
    }
}

private class PlaceItem(var value: Int)

private class PlaceItemCodec(private val trace: MutableList<String>) : Deserialize<PlaceItem> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<PlaceItem> =
        I32Deserialize.deserialize(deserializer).flatMap { n ->
            trace.add("new:$n")
            when (n) {
                -1 -> SerdeResult.failure(SerdeError.custom("rejected"))
                -2 -> throw IllegalStateException("sentinel")
                else -> SerdeResult.success(PlaceItem(n))
            }
        }

    override fun <D : Deserializer> deserializeInPlace(deserializer: D, place: MutablePlace<PlaceItem>): SerdeResult<Unit> =
        I32Deserialize.deserialize(deserializer).flatMap { n ->
            trace.add("in:${place.value.value}:$n")
            place.value.value = when (n) { -1 -> 700; -2 -> 800; else -> n }
            when (n) {
                -1 -> SerdeResult.failure(SerdeError.custom("rejected"))
                -2 -> throw IllegalStateException("sentinel")
                else -> SerdeResult.success(Unit)
            }
        }
}

/** A format driver that forwards errors/exceptions; only the production Vec consumer is under test. */
private class DirectPlaceSequence(private val values: List<Int>) : Deserializer by I32Deserializer.new(0) {
    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSeq(object : SeqAccess {
        private var index = 0
        override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
            if (index == values.size) SerdeResult.success(SeqElement.None)
            else seed.deserialize(I32Deserializer.new(values[index++])).map { SeqElement.Some(it) }
        override fun sizeHint(): Int = values.size - index
    })
}

fun mutablePlaceObservations(): List<String> = listOf(
    Triple("empty", emptyList(), emptyList()),
    Triple("truncate", listOf(1, 2, 3), listOf(10)),
    Triple("equal", listOf(1, 2), listOf(10, 20)),
    Triple("append", listOf(1), listOf(10, 20, 30)),
    Triple("empty-append", emptyList(), listOf(10, 20)),
    Triple("error-existing", listOf(1, 2, 3), listOf(10, -1, 30)),
    Triple("error-append", listOf(1), listOf(10, -1)),
    Triple("panic-existing", listOf(1, 2, 3), listOf(10, -2, 30)),
    Triple("panic-append", listOf(1), listOf(10, -2)),
).map { (label, before, input) ->
    val trace = mutableListOf<String>()
    val values = before.map { PlaceItem(it) }.toMutableList()
    val result = try {
        when (val r = mutableListDeserialize(PlaceItemCodec(trace)).deserializeInPlace(
            DirectPlaceSequence(input), mutablePlace({ values }, { error("list replaced") }),
        )) {
            is SerdeResult.Success -> "ok"
            is SerdeResult.Failure -> "error:${r.error.message}"
        }
    } catch (e: IllegalStateException) {
        check(e.message == "sentinel")
        "panic"
    }
    "$label|state=${values.joinToString(",") { it.value.toString() }}|trace=${trace.joinToString(";")}|result=$result"
}
