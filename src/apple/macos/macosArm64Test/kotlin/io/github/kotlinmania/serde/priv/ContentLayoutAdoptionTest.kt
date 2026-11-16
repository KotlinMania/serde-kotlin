package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.U32Deserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ContentLayoutAdoptionTest {
    private class NegativeSequence : SeqAccess {
        override fun sizeHint(): Int = -1
        override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
            error("Invalid size evidence must be rejected before reading elements")
    }

    private class NegativeMap : MapAccess {
        override fun sizeHint(): Int = -1
        override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> =
            error("Invalid size evidence must be rejected before reading keys")
        override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> =
            error("Invalid size evidence must be rejected before reading values")
    }

    private fun rejected(result: SerdeResult<*>) {
        assertEquals("A Rust size hint cannot be negative", assertIs<SerdeResult.Failure>(result).error.message)
    }

    @Test
    fun sequenceCallerRejectsANegativeRustHintBeforeReading() {
        rejected(ContentVisitor.new().visitSeq(NegativeSequence()))
    }

    @Test
    fun mapCallerRejectsANegativeRustHintBeforeReading() {
        rejected(ContentVisitor.new().visitMap(NegativeMap()))
    }

    @Test
    fun taggedMapCallerRejectsANegativeRustHintBeforeReading() {
        rejected(TaggedContentVisitor.new("tag", "tagged content", U32Deserialize).visitMap(NegativeMap()))
    }
}
