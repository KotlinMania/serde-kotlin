package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.U32Deserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.mutableListDeserialize
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.de.pairDeserialize
import io.github.kotlinmania.serdecore.de.value.SeqAccessDeserializer
import io.github.kotlinmania.serdecore.priv.Content
import io.github.kotlinmania.serdecore.priv.ContentMapEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NullableContentSequenceTest {
    private val nullableU32 = nullableDeserialize(U32Deserialize)
    private val sequence = Content.Seq(listOf(Content.None, Content.Some(Content.U32(7u)), Content.None))

    @Test
    fun ownedContentRetainsNullableSequenceElements() {
        val actual = mutableListDeserialize(nullableU32).deserialize(ContentDeserializer.new(sequence)).getOrThrow()
        assertEquals(listOf(null, 7u, null), actual)
    }

    @Test
    fun borrowedContentRetainsNullableSequenceElements() {
        val actual = mutableListDeserialize(nullableU32).deserialize(ContentRefDeserializer.new(sequence)).getOrThrow()
        assertEquals(listOf(null, 7u, null), actual)
    }

    @Test
    fun ownedContentMapSequenceRetainsNullablePairFields() {
        val content = Content.Map(listOf(ContentMapEntry(Content.None, Content.Some(Content.U32(7u)))))
        assertNullablePair(ContentDeserializer.new(content))
    }

    @Test
    fun borrowedContentMapSequenceRetainsNullablePairFields() {
        val content = Content.Map(listOf(ContentMapEntry(Content.None, Content.Some(Content.U32(7u)))))
        assertNullablePair(ContentRefDeserializer.new(content))
    }

    private fun assertNullablePair(deserializer: Deserializer) {
        val actual =
            deserializer
                .deserializeMap(
                    object : Visitor<List<Pair<UInt?, UInt?>>> {
                        override fun expecting(): String = "a map"

                        override fun <A> visitMap(access: A): SerdeResult<List<Pair<UInt?, UInt?>>>
                            where A : MapAccess =
                            mutableListDeserialize(pairDeserialize(nullableU32, nullableU32))
                                .deserialize(SeqAccessDeserializer.new(assertIs<SeqAccess>(access)))
                    },
                ).getOrThrow()
        assertEquals(listOf<Pair<UInt?, UInt?>>(null to 7u), actual)
    }
}
