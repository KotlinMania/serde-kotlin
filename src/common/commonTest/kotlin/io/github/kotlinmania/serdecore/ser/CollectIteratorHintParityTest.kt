package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.priv.IteratorWithSizeHint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CollectIteratorHintParityTest {
    private class Item : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = error("begin failure must prevent item serialization")
    }

    private class HintIterator<T>(
        private val events: MutableList<String>,
        private val hint: () -> Pair<ULong, ULong?>,
    ) : IteratorWithSizeHint<T> {
        override fun sizeHint(): Pair<ULong, ULong?> {
            events.add("hint")
            return hint()
        }
        override fun hasNext(): Boolean = error("begin failure must prevent iterator reads")
        override fun next(): T = error("begin failure must prevent iterator reads")
    }

    private class Probe(
        private val events: MutableList<String>,
    ) : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
        val failure = SerdeResult.failure(SerdeError.custom("begin failure"))
        override fun <T> collectSeq(iter: Iterable<T>): SerdeResult<Unit> where T : Serialize = super<Serializer>.collectSeq(iter)
        override fun <T> collectSeq(iter: Sequence<T>): SerdeResult<Unit> where T : Serialize = super<Serializer>.collectSeq(iter)
        override fun <K, V> collectMap(iter: Iterable<Pair<K, V>>): SerdeResult<Unit> where K : Serialize, V : Serialize = super<Serializer>.collectMap(iter)
        override fun <K, V> collectMap(iter: Sequence<Pair<K, V>>): SerdeResult<Unit> where K : Serialize, V : Serialize = super<Serializer>.collectMap(iter)
        override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> {
            events.add("begin:$len")
            return failure
        }
        override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> {
            events.add("begin:$len")
            return failure
        }
    }

    private fun exercise(map: Boolean, sequence: Boolean, hint: () -> Pair<ULong, ULong?>, expected: Int?) {
        val events = mutableListOf<String>()
        val probe = Probe(events)
        fun <T> iterator(): Iterator<T> {
            events.add("factory")
            return HintIterator(events, hint)
        }
        val result =
            if (map) {
                if (sequence) {
                    probe.collectMap(Sequence { iterator<Pair<Item, Item>>() })
                } else {
                    probe.collectMap(Iterable { iterator<Pair<Item, Item>>() })
                }
            } else {
                if (sequence) {
                    probe.collectSeq(Sequence { iterator<Item>() })
                } else {
                    probe.collectSeq(Iterable { iterator<Item>() })
                }
            }
        assertSame(probe.failure, result)
        assertEquals(listOf("factory", "hint", "begin:$expected"), events)
    }

    @Test
    fun exactBoundsOfActualIteratorReachBothCollectionKinds() {
        for (map in listOf(false, true)) {
            for (sequence in listOf(false, true)) {
                for (length in listOf(0, 2, Int.MAX_VALUE)) {
                    exercise(map, sequence, { length.toULong() to length.toULong() }, length)
                }
                exercise(map, sequence, { 0uL to null }, null)
                exercise(map, sequence, { 1uL to 3uL }, null)
            }
        }
    }

    @Test
    fun iteratorEvidenceTakesPrecedenceOverOuterCollectionSize() {
        val events = mutableListOf<String>()
        val probe = Probe(events)
        val source =
            object : AbstractCollection<Item>() {
                override val size: Int get() = error("outer size must not replace actual iterator bounds")
                override fun iterator(): Iterator<Item> {
                    events.add("factory")
                    return HintIterator(events) { 2uL to 2uL }
                }
            }
        assertSame(probe.failure, probe.collectSeq(source))
        assertEquals(listOf("factory", "hint", "begin:2"), events)
    }

    @Test
    fun hintPanicPrecedesBeginAndRetainsIdentity() {
        val events = mutableListOf<String>()
        val panic = IllegalStateException("hint panic")
        val probe = Probe(events)
        val source =
            Sequence {
                events.add("factory")
                HintIterator<Item>(events) { throw panic }
            }
        assertSame(panic, assertFailsWith<IllegalStateException> { probe.collectSeq(source) })
        assertEquals(listOf("factory", "hint"), events)
    }

    @Test
    fun currentIntAdapterNeverWrapsUnrepresentableUnsignedLength() {
        // Full usize width still requires changing the public length representation.
        for (map in listOf(false, true)) {
            for (sequence in listOf(false, true)) {
                exercise(map, sequence, { ULong.MAX_VALUE to ULong.MAX_VALUE }, null)
            }
        }
    }

    @Test
    fun borrowedCollectionAdapterReportsRemainingLengthAfterConsumption() {
        val hints = mutableListOf<Pair<ULong, ULong?>>()
        val probe =
            object : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
                override fun <T> collectSeq(iter: Iterable<T>): SerdeResult<Unit> where T : Serialize {
                    val iterator = iter.iterator()
                    val sized = iterator as IteratorWithSizeHint<*>
                    hints.add(sized.sizeHint())
                    iterator.next()
                    hints.add(sized.sizeHint())
                    iterator.next()
                    hints.add(sized.sizeHint())
                    return SerdeResult.success(Unit)
                }
            }
        listOf(1, 2).serialize(probe, Serialize.I32).getOrThrow()
        assertEquals(listOf<Pair<ULong, ULong?>>(2uL to 2uL, 1uL to 1uL, 0uL to 0uL), hints)
    }
}
