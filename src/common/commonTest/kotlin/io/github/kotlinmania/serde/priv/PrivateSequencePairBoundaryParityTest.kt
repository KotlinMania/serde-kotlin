// port-lint: source serde/src/private/de.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.de.seedFromDeserialize
import io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry
import io.github.kotlinmania.serdecore.priv.contentaccess.newI32Content
import io.github.kotlinmania.serdecore.priv.contentaccess.newMapContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newSeqContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newSomeContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStringContent
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class SequenceBoundaryPanic : RuntimeException("sequence callback panic")

private fun <T> sequenceBoundarySeed(action: (Deserializer) -> SerdeResult<T>): DeserializeSeed<T> =
    object : DeserializeSeed<T> {
        override fun <D> deserialize(deserializer: D): SerdeResult<T>
            where D : Deserializer = action(deserializer)
    }

private fun sequenceBoundaryDeserializer(content: Content, borrowed: Boolean): Deserializer =
    if (borrowed) ContentRefDeserializer.new(content) else ContentDeserializer.new(content)

// A content map is exposed as MapAccess; its private adapter also implements
// Deserializer, exactly as private/de.rs does. Exercise that adapter's sequence
// implementation without broadening ContentDeserializer's accepted input kinds.
private fun <T> mapBoundarySequence(content: Content.Map, borrowed: Boolean, visitor: Visitor<T>): SerdeResult<T> =
    sequenceBoundaryDeserializer(content, borrowed).deserializeMap(object : Visitor<T> {
        override fun expecting(): String = "a map adapter"
        override fun <A> visitMap(access: A): SerdeResult<T> where A : MapAccess =
            assertIs<Deserializer>(access).deserializeSeq(visitor)
    })

/** Returned failures propagate directly; exceptions from visitors and seeds escape. */
class PrivateSequencePairBoundaryParityTest {
    @Test
    fun sequenceVisitorFailureSkipsTheTrailingLengthCheck() {
        for (borrowed in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("visitor stopped"))
            val source = newSeqContent(listOf(newI32Content(1), newI32Content(2)))
            val result = sequenceBoundaryDeserializer(source, borrowed).deserializeSeq(object : Visitor<Unit> {
                override fun expecting(): String = "a sequence"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess = failure
            })
            assertSame(failure, result)
        }
    }

    @Test
    fun sequenceVisitorPanicEscapesUnchanged() {
        for (borrowed in listOf(false, true)) {
            val panic = SequenceBoundaryPanic()
            assertSame(panic, assertFailsWith<SequenceBoundaryPanic> {
                sequenceBoundaryDeserializer(newSeqContent(listOf(newI32Content(1))), borrowed)
                    .deserializeSeq(object : Visitor<Unit> {
                        override fun expecting(): String = "a sequence"
                        override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess = throw panic
                    })
            })
        }
    }

    @Test
    fun sequenceSeedsConsumeBeforeReturnedFailureOrPanic() {
        for (borrowed in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("seed stopped"))
            val panic = SequenceBoundaryPanic()
            val source = newSeqContent(listOf(newI32Content(10), newI32Content(20), newI32Content(30)))
            val result = sequenceBoundaryDeserializer(source, borrowed).deserializeSeq(object : Visitor<Unit> {
                override fun expecting(): String = "three elements"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                    assertEquals(3, access.sizeHint())
                    assertSame(failure, access.nextElementSeed(sequenceBoundarySeed<Int> { failure }))
                    assertEquals(2, access.sizeHint())
                    assertSame(panic, assertFailsWith<SequenceBoundaryPanic> {
                        access.nextElementSeed(sequenceBoundarySeed<Int> { throw panic })
                    })
                    assertEquals(1, access.sizeHint())
                    assertEquals(SeqElement.Some(30), access.nextElement(I32Deserialize).getOrThrow())
                    assertEquals(0, access.sizeHint())
                    assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
                    return SerdeResult.success(Unit)
                }
            })
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun sequenceDistinguishesPresentNullFromExhaustion() {
        for (borrowed in listOf(false, true)) {
            val source = newSeqContent(listOf(Content.None, newSomeContent(newI32Content(7))))
            val result = sequenceBoundaryDeserializer(source, borrowed).deserializeSeq(object : Visitor<Unit> {
                override fun expecting(): String = "optional elements"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                    val seed = seedFromDeserialize(nullableDeserialize(I32Deserialize))
                    assertEquals(SeqElement.Some(null), access.nextElementSeed(seed).getOrThrow())
                    assertEquals(SeqElement.Some(7), access.nextElementSeed(seed).getOrThrow())
                    assertEquals(SeqElement.None, access.nextElementSeed(seed).getOrThrow())
                    return SerdeResult.success(Unit)
                }
            })
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun sequenceChecksTrailingLengthOnlyAfterSuccess() {
        for (borrowed in listOf(false, true)) {
            val source = newSeqContent(listOf(newI32Content(1), newI32Content(2), newI32Content(3)))
            val result = sequenceBoundaryDeserializer(source, borrowed).deserializeSeq(object : Visitor<Unit> {
                override fun expecting(): String = "one element"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                    assertEquals(SeqElement.Some(1), access.nextElement(I32Deserialize).getOrThrow())
                    return SerdeResult.success(Unit)
                }
            })
            assertEquals("invalid length 3, expected 1 element in sequence", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun mapSequenceSeedsAdvanceByWholePairsBeforeCallbacks() {
        for (borrowed in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("pair seed stopped"))
            val panic = SequenceBoundaryPanic()
            val source = newMapContent(listOf(
                ContentMapEntry(newStringContent("a"), newI32Content(10)),
                ContentMapEntry(newStringContent("b"), newI32Content(20)),
                ContentMapEntry(newStringContent("c"), newI32Content(30)),
            ))
            val result = mapBoundarySequence(source, borrowed, object : Visitor<Unit> {
                override fun expecting(): String = "map pairs"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                    assertEquals(3, access.sizeHint())
                    assertSame(failure, access.nextElementSeed(sequenceBoundarySeed<Unit> { failure }))
                    assertEquals(2, access.sizeHint())
                    assertSame(panic, assertFailsWith<SequenceBoundaryPanic> {
                        access.nextElementSeed(sequenceBoundarySeed<Unit> { throw panic })
                    })
                    assertEquals(1, access.sizeHint())
                    val last = access.nextElementSeed(sequenceBoundarySeed { pair ->
                        pair.deserializeTuple(2, object : Visitor<Pair<String, Int>> {
                            override fun expecting(): String = "key and value"
                            override fun <B> visitSeq(access: B): SerdeResult<Pair<String, Int>> where B : SeqAccess {
                                assertEquals(2, access.sizeHint())
                                assertEquals(SeqElement.Some("c"), access.nextElement(StringDeserialize).getOrThrow())
                                assertEquals(1, access.sizeHint())
                                assertEquals(SeqElement.Some(30), access.nextElement(I32Deserialize).getOrThrow())
                                assertEquals(0, access.sizeHint())
                                assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
                                return SerdeResult.success("c" to 30)
                            }
                        })
                    }).getOrThrow()
                    assertEquals(SeqElement.Some("c" to 30), last)
                    assertEquals(0, access.sizeHint())
                    assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
                    return SerdeResult.success(Unit)
                }
            })
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun pairSeedsTakeEachSlotBeforeFailureOrPanic() {
        for (borrowed in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("key stopped"))
            val panic = SequenceBoundaryPanic()
            val source = newMapContent(listOf(ContentMapEntry(newStringContent("key"), newI32Content(8))))
            val result = mapBoundarySequence(source, borrowed, object : Visitor<Unit> {
                override fun expecting(): String = "one pair"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                    val pair = access.nextElementSeed(sequenceBoundarySeed { pair ->
                        pair.deserializeSeq(object : Visitor<Unit> {
                            override fun expecting(): String = "a pair"
                            override fun <B> visitSeq(access: B): SerdeResult<Unit> where B : SeqAccess {
                                assertSame(failure, access.nextElementSeed(sequenceBoundarySeed<String> { failure }))
                                assertEquals(1, access.sizeHint())
                                assertSame(panic, assertFailsWith<SequenceBoundaryPanic> {
                                    access.nextElementSeed(sequenceBoundarySeed<Int> { throw panic })
                                })
                                assertEquals(0, access.sizeHint())
                                assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
                                return SerdeResult.success(Unit)
                            }
                        })
                    })
                    assertEquals(SeqElement.Some(Unit), pair.getOrThrow())
                    return SerdeResult.success(Unit)
                }
            })
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun pairVisitorFailureAndPanicSkipTheLengthCheck() {
        for (borrowed in listOf(false, true)) {
            for (throwPanic in listOf(false, true)) {
                val failure = SerdeResult.failure(SerdeError.custom("pair visitor stopped"))
                val panic = SequenceBoundaryPanic()
                val source = newMapContent(listOf(ContentMapEntry(newStringContent("key"), newI32Content(8))))
                val result = mapBoundarySequence(source, borrowed, object : Visitor<Unit> {
                    override fun expecting(): String = "one pair"
                    override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                        val seed = sequenceBoundarySeed { pair ->
                            pair.deserializeSeq(object : Visitor<Unit> {
                                override fun expecting(): String = "a pair"
                                override fun <B> visitSeq(access: B): SerdeResult<Unit> where B : SeqAccess =
                                    if (throwPanic) throw panic else failure
                            })
                        }
                        if (throwPanic) {
                            assertSame(panic, assertFailsWith<SequenceBoundaryPanic> { access.nextElementSeed(seed) })
                        } else {
                            assertSame(failure, access.nextElementSeed(seed))
                        }
                        assertEquals(SeqElement.None, access.nextElement(I32Deserialize).getOrThrow())
                        return SerdeResult.success(Unit)
                    }
                })
                assertTrue(result.isSuccess)
            }
        }
    }

    @Test
    fun pairValidatesTupleLengthBeforeCallingTheVisitor() {
        for (borrowed in listOf(false, true)) {
            var calls = 0
            val source = newMapContent(listOf(ContentMapEntry(newStringContent("key"), newI32Content(8))))
            val result = mapBoundarySequence(source, borrowed, object : Visitor<Unit> {
                override fun expecting(): String = "one pair"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                    val outcome = access.nextElementSeed(sequenceBoundarySeed { pair ->
                        pair.deserializeTuple(3, object : Visitor<Unit> {
                            override fun expecting(): String = "three elements"
                            override fun <B> visitSeq(access: B): SerdeResult<Unit> where B : SeqAccess {
                                calls += 1
                                return SerdeResult.success(Unit)
                            }
                        })
                    })
                    assertEquals("invalid length 2, expected 3 elements in sequence", outcome.exceptionOrNull()?.message)
                    return SerdeResult.success(Unit)
                }
            })
            assertTrue(result.isSuccess)
            assertEquals(0, calls)
        }
    }

    @Test
    fun pairReportsUnderconsumptionAfterSuccessfulVisitor() {
        for (borrowed in listOf(false, true)) {
            val source = newMapContent(listOf(ContentMapEntry(newStringContent("key"), newI32Content(8))))
            val result = mapBoundarySequence(source, borrowed, object : Visitor<Unit> {
                override fun expecting(): String = "one pair"
                override fun <A> visitSeq(access: A): SerdeResult<Unit> where A : SeqAccess {
                    val outcome = access.nextElementSeed(sequenceBoundarySeed { pair ->
                        pair.deserializeSeq(object : Visitor<Unit> {
                            override fun expecting(): String = "one element"
                            override fun <B> visitSeq(access: B): SerdeResult<Unit> where B : SeqAccess {
                                assertEquals(SeqElement.Some("key"), access.nextElement(StringDeserialize).getOrThrow())
                                return SerdeResult.success(Unit)
                            }
                        })
                    })
                    assertEquals("invalid length 2, expected 1 element in sequence", outcome.exceptionOrNull()?.message)
                    return SerdeResult.success(Unit)
                }
            })
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun sequenceAndPairPreserveNullableVisitorSuccessPayloads() {
        for (borrowed in listOf(false, true)) {
            for (payload in listOf(null, Any())) {
                val sequence = sequenceBoundaryDeserializer(newSeqContent(listOf(newI32Content(1))), borrowed)
                    .deserializeSeq(object : Visitor<Any?> {
                        override fun expecting(): String = "one element"
                        override fun <A> visitSeq(access: A): SerdeResult<Any?> where A : SeqAccess {
                            assertEquals(SeqElement.Some(1), access.nextElement(I32Deserialize).getOrThrow())
                            return SerdeResult.success(payload)
                        }
                    })
                assertSame(payload, sequence.getOrThrow())
                val source = newMapContent(listOf(ContentMapEntry(newStringContent("key"), newI32Content(8))))
                val pair = mapBoundarySequence(source, borrowed, object : Visitor<Any?> {
                    override fun expecting(): String = "one pair"
                    override fun <A> visitSeq(access: A): SerdeResult<Any?> where A : SeqAccess {
                        val result = access.nextElementSeed(sequenceBoundarySeed { deserializer ->
                            deserializer.deserializeSeq(object : Visitor<Any?> {
                                override fun expecting(): String = "key and value"
                                override fun <B> visitSeq(access: B): SerdeResult<Any?> where B : SeqAccess {
                                    assertEquals(SeqElement.Some("key"), access.nextElement(StringDeserialize).getOrThrow())
                                    assertEquals(SeqElement.Some(8), access.nextElement(I32Deserialize).getOrThrow())
                                    return SerdeResult.success(payload)
                                }
                            })
                        }).getOrThrow()
                        assertSame(payload, assertIs<SeqElement.Some<Any?>>(result).value)
                        return SerdeResult.success(payload)
                    }
                })
                assertSame(payload, pair.getOrThrow())
            }
        }
    }
}
