package io.github.kotlinmania.serde

import io.github.kotlinmania.serde.priv.ContentDeserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.mutableListDeserialize
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.de.pairDeserialize
import io.github.kotlinmania.serdecore.priv.Content
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UntaggedContentTest {
    @Test
    fun failedAttemptDoesNotConsumeTheNextAttempt() {
        val source = Content.Seq(listOf(Content.I32(1), Content.I32(2)))
        val captured = UntaggedContent.read(ContentDeserializer.new(source)).getOrThrow()
        assertIs<SerdeResult.Failure>(captured.decode(pairDeserialize(I32Deserialize, StringDeserialize)))
        val list = mutableListDeserialize(I32Deserialize)
        assertEquals(listOf(1, 2), captured.decode(list).getOrThrow())
        assertEquals(listOf(1, 2), captured.decode(list).getOrThrow())
    }

    @Test
    fun nullableElementsAreValuesRatherThanEndOfSequence() {
        val source = Content.Seq(listOf(Content.None, Content.Some(Content.I32(7)), Content.None))
        val captured = UntaggedContent.read(ContentDeserializer.new(source)).getOrThrow()
        assertEquals(listOf(null, 7, null), captured.decode(mutableListDeserialize(nullableDeserialize(I32Deserialize))).getOrThrow())
    }
}
