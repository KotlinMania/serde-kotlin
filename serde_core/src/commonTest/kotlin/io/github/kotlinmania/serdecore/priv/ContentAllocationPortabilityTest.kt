package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ContentAllocationPortabilityTest {
    @Test fun unavailableLayoutUsesGrowingStorageWithoutGuessingCapacity() {
        for (hint in listOf(null, 0, 2, Int.MAX_VALUE)) {
            val values = contentBuffer<Content>(hint, null)
            assertEquals(0, values.size)
            values.add(Content.None)
            values.add(Content.I32(7))
            assertEquals(listOf(Content.None, Content.I32(7)), values)
        }
        assertFailsWith<IllegalStateException> { rustContentSizes("jvm") }
        assertFailsWith<IllegalStateException> { rustContentSizes("unmeasured-native-target") }
    }

    @Test fun captureAcceptsUnknownZeroAndLargePositiveHintsOnTheCurrentTarget() {
        for (hint in listOf(null, 0, 2, Int.MAX_VALUE)) {
            val seq = ContentVisitor.new().visitSeq(object : SeqAccess {
                override fun sizeHint(): Int? = hint
                override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
                    SerdeResult.success(SeqElement.None)
            }).getOrThrow()
            assertEquals(emptyList(), assertIs<Content.Seq>(seq).value)
            val map = ContentVisitor.new().visitMap(object : MapAccess {
                override fun sizeHint(): Int? = hint
                override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> = SerdeResult.success(null)
                override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> = error("No key was supplied")
            }).getOrThrow()
            assertEquals(emptyList(), assertIs<Content.Map>(map).value)
        }
    }

    @Test fun invalidHintsStillRejectBeforeAnyElementRead() {
        assertFailsWith<IllegalArgumentException> { contentBuffer<Content>(-1, null) }
        assertFailsWith<IllegalArgumentException> { contentBuffer<Content>(-1, 32uL) }
        val seq = ContentVisitor.new().visitSeq(object : SeqAccess {
            override fun sizeHint(): Int = -1
            override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
                error("The invalid hint must fail before iteration")
        })
        assertEquals("A Rust size hint cannot be negative", assertIs<SerdeResult.Failure>(seq).error.message)
    }
}
