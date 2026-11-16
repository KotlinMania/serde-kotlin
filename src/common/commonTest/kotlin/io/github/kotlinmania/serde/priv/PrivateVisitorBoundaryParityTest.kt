// port-lint: source serde/src/private/de.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.de.seedFromDeserialize
import io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry
import io.github.kotlinmania.serdecore.priv.contentaccess.newI32Content
import io.github.kotlinmania.serdecore.priv.contentaccess.newMapContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newSeqContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStrContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newStringContent
import io.github.kotlinmania.serdecore.priv.contentaccess.value
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class VisitorBoundaryPanic : RuntimeException("visitor callback panic")

private fun <T> visitorBoundarySeed(action: (Deserializer) -> SerdeResult<T>): DeserializeSeed<T> =
    object : DeserializeSeed<T> {
        override fun <D> deserialize(deserializer: D): SerdeResult<T>
            where D : Deserializer = action(deserializer)
    }

private fun <T> visitorBoundaryDeserialize(action: (Deserializer) -> SerdeResult<T>): Deserialize<T> =
    object : Deserialize<T> {
        override fun <D> deserialize(deserializer: D): SerdeResult<T>
            where D : Deserializer = action(deserializer)
    }

private fun <T> visitorBoundaryEnum(action: (EnumAccess) -> SerdeResult<T>): Visitor<T> =
    object : Visitor<T> {
        override fun expecting(): String = "an enum"
        override fun <A> visitEnum(access: A): SerdeResult<T> where A : EnumAccess = action(access)
    }

private class BoundarySeqAccess(
    private val values: List<Content>,
    private val stop: (() -> SerdeResult<Nothing>)? = null,
) : SeqAccess {
    var calls = 0
    var hintCalls = 0
    private var index = 0

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> {
        calls += 1
        if (index == 1 && stop != null) return stop.invoke()
        if (index == values.size) return SerdeResult.success(SeqElement.None)
        return seed.deserialize(ContentDeserializer.new(values[index++])).map { SeqElement.Some(it) }
    }

    override fun sizeHint(): Int {
        hintCalls += 1
        return values.size - index
    }
}

private class BoundaryEntryAccess(
    private val entries: List<ContentMapEntry>,
    private val stop: (() -> SerdeResult<Nothing>)? = null,
) : MapAccess {
    var calls = 0
    var hintCalls = 0
    private var index = 0

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> = error("nextEntrySeed is implemented")
    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> = error("nextEntrySeed is implemented")

    override fun <K, V> nextEntrySeed(
        keySeed: DeserializeSeed<K>,
        valueSeed: DeserializeSeed<V>,
    ): SerdeResult<Pair<K, V>?> {
        calls += 1
        if (index == 1 && stop != null) return stop.invoke()
        if (index == entries.size) return SerdeResult.success(null)
        val entry = entries[index++]
        return keySeed.deserialize(ContentDeserializer.new(entry.key)).flatMap { key ->
            valueSeed.deserialize(ContentDeserializer.new(entry.value)).map { value -> key to value }
        }
    }

    override fun sizeHint(): Int {
        hintCalls += 1
        return entries.size - index
    }
}

private class BoundaryVariantAccess(
    private val unit: () -> SerdeResult<Unit>,
) : VariantAccess {
    override fun unitVariant(): SerdeResult<Unit> = unit()
    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> = error("unit variant only")
    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> = error("unit variant only")
    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = error("unit variant only")
}

private fun boundaryEnumAccess(variant: VariantAccess): EnumAccess =
    object : EnumAccess {
        override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
            seed.deserialize(ContentDeserializer.new(newStringContent("A"))).map { it to variant }
    }

/** Callback failure propagation and nullable tag presence. */
class PrivateVisitorBoundaryParityTest {
    @Test
    fun contentCollectionReadsTheHintOnceAndPreservesOrderAndNullKeys() {
        val sequence = BoundarySeqAccess(listOf(newI32Content(1), Content.None, newI32Content(2)))
        val seq = assertIs<Content.Seq>(ContentVisitor.new().visitSeq(sequence).getOrThrow())
        assertEquals(listOf(1, null, 2), seq.value.map { nullableDeserialize(I32Deserialize).deserialize(ContentDeserializer.new(it)).getOrThrow() })
        assertEquals(1, sequence.hintCalls)
        assertEquals(4, sequence.calls)
        val entries =
            BoundaryEntryAccess(
                listOf(
                    ContentMapEntry(Content.None, newI32Content(1)),
                    ContentMapEntry(newStringContent("a"), newI32Content(2)),
                    ContentMapEntry(newStringContent("a"), newI32Content(3)),
                ),
            )
        val map = assertIs<Content.Map>(ContentVisitor.new().visitMap(entries).getOrThrow())
        assertEquals(3, map.value.size)
        assertSame(Content.None, map.value[0].key)
        assertEquals(listOf(1, 2, 3), map.value.map { I32Deserialize.deserialize(ContentDeserializer.new(it.value)).getOrThrow() })
        assertEquals(1, entries.hintCalls)
        assertEquals(4, entries.calls)
    }

    @Test
    fun contentCollectionStopsAtTheFirstFailureOrPanic() {
        for (throwPanic in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("access stopped"))
            val panic = VisitorBoundaryPanic()
            val stop = { if (throwPanic) throw panic else failure }
            val sequence = BoundarySeqAccess(listOf(newI32Content(1), newI32Content(2)), stop)
            val entries =
                BoundaryEntryAccess(
                    listOf(
                        ContentMapEntry(newStringContent("a"), newI32Content(1)),
                        ContentMapEntry(newStringContent("b"), newI32Content(2)),
                    ),
                    stop,
                )
            if (throwPanic) {
                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { ContentVisitor.new().visitSeq(sequence) })
                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { ContentVisitor.new().visitMap(entries) })
            } else {
                assertSame(failure, ContentVisitor.new().visitSeq(sequence))
                assertSame(failure, ContentVisitor.new().visitMap(entries))
            }
            assertEquals(2, sequence.calls)
            assertEquals(2, entries.calls)
            assertEquals(1, sequence.hintCalls)
            assertEquals(1, entries.hintCalls)
        }
    }

    @Test
    fun taggedMapPreservesPresentNullAndOrdinaryDuplicateKeys() {
        val source =
            newMapContent(
                listOf(
                    ContentMapEntry(Content.None, newI32Content(1)),
                    ContentMapEntry(newStringContent("x"), newI32Content(2)),
                    ContentMapEntry(newStringContent("kind"), Content.None),
                    ContentMapEntry(newStringContent("x"), newI32Content(3)),
                ),
            )
        val result =
            ContentDeserializer
                .new(source)
                .deserializeMap(
                    TaggedContentVisitor.new("kind", "tagged content", nullableDeserialize(StringDeserialize)),
                ).getOrThrow()
        assertNull(result.first)
        val map = assertIs<Content.Map>(result.second)
        assertEquals(3, map.value.size)
        assertSame(Content.None, map.value[0].key)
        assertEquals(listOf(1, 2, 3), map.value.map { I32Deserialize.deserialize(ContentDeserializer.new(it.value)).getOrThrow() })
        assertEquals(listOf("x", "x"), map.value.drop(1).map { StringDeserialize.deserialize(ContentDeserializer.new(it.key)).getOrThrow() })
    }

    @Test
    fun duplicateNullTagIsRejectedBeforeTheSecondValueCallback() {
        var calls = 0
        val tag =
            visitorBoundaryDeserialize { deserializer ->
                calls += 1
                nullableDeserialize(StringDeserialize).deserialize(deserializer)
            }
        val source =
            newMapContent(
                listOf(
                    ContentMapEntry(newStringContent("kind"), Content.None),
                    ContentMapEntry(newStringContent("kind"), newStringContent("A")),
                ),
            )
        val result = ContentDeserializer.new(source).deserializeMap(TaggedContentVisitor.new("kind", "tagged content", tag))
        assertEquals("duplicate field `kind`", result.exceptionOrNull()?.message)
        assertEquals(1, calls)
    }

    @Test
    fun taggedSequenceDistinguishesNullFromMissingAndBuffersTheRemainder() {
        val visitor = TaggedContentVisitor.new("kind", "tagged content", nullableDeserialize(StringDeserialize))
        val result =
            ContentDeserializer
                .new(newSeqContent(listOf(Content.None, newI32Content(4), newI32Content(5))))
                .deserializeSeq(visitor)
                .getOrThrow()
        assertNull(result.first)
        val remainder = assertIs<Content.Seq>(result.second)
        assertEquals(listOf(4, 5), remainder.value.map { I32Deserialize.deserialize(ContentDeserializer.new(it)).getOrThrow() })
        assertEquals(
            "missing field `kind`",
            ContentDeserializer
                .new(newSeqContent(emptyList()))
                .deserializeSeq(visitor)
                .exceptionOrNull()
                ?.message,
        )
        assertEquals(
            "missing field `kind`",
            ContentDeserializer
                .new(newMapContent(emptyList()))
                .deserializeMap(visitor)
                .exceptionOrNull()
                ?.message,
        )
    }

    @Test
    fun taggedValueFailureAndPanicPropagateWithoutConversion() {
        for (sequence in listOf(false, true)) {
            for (throwPanic in listOf(false, true)) {
                val failure = SerdeResult.failure(SerdeError.custom("tag stopped"))
                val panic = VisitorBoundaryPanic()
                val tag = visitorBoundaryDeserialize<String> { if (throwPanic) throw panic else failure }
                val visitor = TaggedContentVisitor.new("kind", "tagged content", tag)
                val action = {
                    if (sequence) {
                        ContentDeserializer.new(newSeqContent(listOf(newStringContent("A")))).deserializeSeq(visitor)
                    } else {
                        ContentDeserializer.new(newMapContent(listOf(ContentMapEntry(newStringContent("kind"), newStringContent("A"))))).deserializeMap(visitor)
                    }
                }
                if (throwPanic) {
                    assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { action() })
                } else {
                    assertSame(failure, action())
                }
            }
        }
    }

    @Test
    fun contentEnumsValidateCardinalityBeforeInvokingVisitors() {
        for (borrowed in listOf(false, true)) {
            var calls = 0
            val visitor =
                visitorBoundaryEnum<Unit> {
                    calls += 1
                    SerdeResult.success(Unit)
                }
            for (entries in listOf(
                emptyList(),
                listOf(
                    ContentMapEntry(newStringContent("A"), Content.Unit),
                    ContentMapEntry(newStringContent("B"), Content.Unit),
                ),
            )) {
                val content = newMapContent(entries)
                val deserializer: Deserializer = if (borrowed) ContentRefDeserializer.new(content) else ContentDeserializer.new(content)
                assertEquals("invalid value: map, expected map with a single key", deserializer.deserializeEnum("E", listOf("A", "B"), visitor).exceptionOrNull()?.message)
            }
            assertEquals(0, calls)
        }
    }

    @Test
    fun contentEnumsPreserveNullableSuccessFailureAndPanic() {
        for (borrowed in listOf(false, true)) {
            for (content in listOf(newStringContent("A"), newStrContent("A"), newMapContent(listOf(ContentMapEntry(newStringContent("A"), Content.Unit))))) {
                fun deserializer(): Deserializer = if (borrowed) ContentRefDeserializer.new(content) else ContentDeserializer.new(content)
                val success = SerdeResult.success<String?>(null)
                val failure = SerdeResult.failure(SerdeError.custom("enum stopped"))
                val panic = VisitorBoundaryPanic()
                assertSame(success, deserializer().deserializeEnum("E", listOf("A"), visitorBoundaryEnum { success }))
                assertSame(failure, deserializer().deserializeEnum("E", listOf("A"), visitorBoundaryEnum<Unit> { failure }))
                assertSame(
                    panic,
                    assertFailsWith<VisitorBoundaryPanic> {
                        deserializer().deserializeEnum("E", listOf("A"), visitorBoundaryEnum<Unit> { throw panic })
                    },
                )
            }
        }
    }

    @Test
    fun flattenAccessPreservesBorrowingClaimingAndPendingValueOrder() {
        for (struct in listOf(false, true)) {
            for (throwPanic in listOf(false, true)) {
                val buffer = FlatMapBuffer()
                buffer.add(newStringContent("a"), newI32Content(11))
                buffer.add(newStringContent("b"), newI32Content(22))
                buffer.add(newStringContent("other"), newI32Content(33))
                val failure = SerdeResult.failure(SerdeError.custom("flatten callback stopped"))
                val panic = VisitorBoundaryPanic()
                val visitor =
                    object : Visitor<Unit> {
                        override fun expecting(): String = "flattened fields"
                        override fun <A> visitMap(access: A): SerdeResult<Unit> where A : MapAccess {
                            val key =
                                visitorBoundarySeed<String> {
                                    if (struct) assertNull(buffer.get(0)) else assertNotNull(buffer.get(0))
                                    if (throwPanic) throw panic else failure
                                }
                            if (throwPanic) {
                                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { access.nextKeySeed(key) })
                            } else {
                                assertSame(failure, access.nextKeySeed(key))
                            }
                            val value =
                                visitorBoundarySeed<Int> { deserializer ->
                                    assertEquals(11, I32Deserialize.deserialize(deserializer).getOrThrow())
                                    if (throwPanic) throw panic else failure
                                }
                            if (throwPanic) {
                                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { access.nextValueSeed(value) })
                            } else {
                                assertSame(failure, access.nextValueSeed(value))
                            }
                            assertEquals("value is missing", access.nextValue(I32Deserialize).exceptionOrNull()?.message)
                            assertEquals("b", access.nextKey(StringDeserialize).getOrThrow())
                            assertEquals(22, access.nextValue(I32Deserialize).getOrThrow())
                            if (!struct) {
                                assertEquals("other", access.nextKey(StringDeserialize).getOrThrow())
                                assertEquals(33, access.nextValue(I32Deserialize).getOrThrow())
                            }
                            assertNull(access.nextKey(StringDeserialize).getOrThrow())
                            return SerdeResult.success(Unit)
                        }
                    }
                val result =
                    if (struct) {
                        FlatMapDeserializer(buffer).deserializeStruct("S", listOf("a", "b"), visitor)
                    } else {
                        FlatMapDeserializer(buffer).deserializeMap(visitor)
                    }
                assertTrue(result.isSuccess)
                if (struct) {
                    assertNull(buffer.get(0))
                    assertNull(buffer.get(1))
                } else {
                    assertNotNull(buffer.get(0))
                    assertNotNull(buffer.get(1))
                }
                assertNotNull(buffer.get(2))
            }
        }
    }

    @Test
    fun flattenMapEntriesDistinguishNullKeysFromExhaustion() {
        val buffer = FlatMapBuffer()
        buffer.add(Content.None, newI32Content(7))
        val result =
            FlatMapDeserializer(buffer).deserializeMap(
                object : Visitor<Unit> {
                    override fun expecting(): String = "nullable keys"
                    override fun <A> visitMap(access: A): SerdeResult<Unit> where A : MapAccess {
                        val keys = seedFromDeserialize(nullableDeserialize(StringDeserialize))
                        val values = seedFromDeserialize(I32Deserialize)
                        assertEquals(null to 7, access.nextEntrySeed(keys, values).getOrThrow())
                        assertNull(access.nextEntrySeed(keys, values).getOrThrow())
                        return SerdeResult.success(Unit)
                    }
                },
            )
        assertTrue(result.isSuccess)
        assertNotNull(buffer.get(0))
    }

    @Test
    fun flattenedEnumClaimsExactlyTheFirstMatchingEntryBeforeCallbacks() {
        for (throwPanic in listOf(false, true)) {
            val buffer = FlatMapBuffer()
            buffer.add(newStringContent("other"), Content.Unit)
            buffer.add(newStringContent("A"), Content.Unit)
            buffer.add(newStringContent("A"), Content.Unit)
            val failure = SerdeResult.failure(SerdeError.custom("enum stopped"))
            val panic = VisitorBoundaryPanic()
            val visitor =
                visitorBoundaryEnum<Unit> {
                    assertNotNull(buffer.get(0))
                    assertNull(buffer.get(1))
                    assertNotNull(buffer.get(2))
                    if (throwPanic) throw panic else failure
                }
            if (throwPanic) {
                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { FlatMapDeserializer(buffer).deserializeEnum("E", listOf("A"), visitor) })
            } else {
                assertSame(failure, FlatMapDeserializer(buffer).deserializeEnum("E", listOf("A"), visitor))
            }
            assertNull(buffer.get(1))
            assertNotNull(buffer.get(2))
        }
        var calls = 0
        val result =
            FlatMapDeserializer(FlatMapBuffer()).deserializeEnum(
                "E",
                listOf("A"),
                visitorBoundaryEnum<Unit> {
                    calls += 1
                    SerdeResult.success(Unit)
                },
            )
        assertEquals("no variant of enum E found in flattened data", result.exceptionOrNull()?.message)
        assertEquals(0, calls)
    }

    @Test
    fun adjacentVariantReadsTheUnitOnlyAfterVariantSuccess() {
        for (throwPanic in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("variant stopped"))
            val panic = VisitorBoundaryPanic()
            var unitCalls = 0
            val variant =
                BoundaryVariantAccess {
                    unitCalls += 1
                    SerdeResult.success(Unit)
                }
            val visitor = AdjacentlyTaggedEnumVariantVisitor("E", visitorBoundaryDeserialize<String> { if (throwPanic) throw panic else failure })
            if (throwPanic) {
                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { visitor.visitEnum(boundaryEnumAccess(variant)) })
            } else {
                assertSame(failure, visitor.visitEnum(boundaryEnumAccess(variant)))
            }
            assertEquals(0, unitCalls)
        }
    }

    @Test
    fun adjacentVariantPreservesUnitFailureAndPanic() {
        for (throwPanic in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("unit stopped"))
            val panic = VisitorBoundaryPanic()
            var unitCalls = 0
            val variant =
                BoundaryVariantAccess {
                    unitCalls += 1
                    if (throwPanic) throw panic else failure
                }
            val visitor = AdjacentlyTaggedEnumVariantVisitor("E", StringDeserialize)
            if (throwPanic) {
                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { visitor.visitEnum(boundaryEnumAccess(variant)) })
            } else {
                assertSame(failure, visitor.visitEnum(boundaryEnumAccess(variant)))
            }
            assertEquals(1, unitCalls)
        }
        val visitor = AdjacentlyTaggedEnumVariantVisitor("E", visitorBoundaryDeserialize<String?> { SerdeResult.success(null) })
        assertNull(visitor.visitEnum(boundaryEnumAccess(BoundaryVariantAccess { SerdeResult.success(Unit) })).getOrThrow())
    }

    @Test
    fun internallyTaggedUnitDrainsMapsButDoesNotReadSequences() {
        val visitor = InternallyTaggedUnitVisitor.new("E", "A")
        val sequence = BoundarySeqAccess(listOf(newI32Content(1)))
        assertTrue(visitor.visitSeq(sequence).isSuccess)
        assertEquals(0, sequence.calls)
        assertEquals(0, sequence.hintCalls)
        val entries =
            BoundaryEntryAccess(
                listOf(
                    ContentMapEntry(Content.None, newI32Content(1)),
                    ContentMapEntry(newStringContent("x"), Content.None),
                ),
            )
        assertTrue(visitor.visitMap(entries).isSuccess)
        assertEquals(3, entries.calls)
        for (throwPanic in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("entry stopped"))
            val panic = VisitorBoundaryPanic()
            val access =
                BoundaryEntryAccess(
                    listOf(
                        ContentMapEntry(Content.None, newI32Content(1)),
                        ContentMapEntry(newStringContent("x"), Content.None),
                    ),
                ) { if (throwPanic) throw panic else failure }
            if (throwPanic) {
                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { visitor.visitMap(access) })
            } else {
                assertSame(failure, visitor.visitMap(access))
            }
            assertEquals(2, access.calls)
        }
    }

    @Test
    fun taggedMapPreservesKeyAndOrdinaryValueFailuresAndPanics() {
        for (failOnKey in listOf(false, true)) {
            for (throwPanic in listOf(false, true)) {
                val failure = SerdeResult.failure(SerdeError.custom("map access stopped"))
                val panic = VisitorBoundaryPanic()
                var keys = 0
                var values = 0
                val access =
                    object : MapAccess {
                        override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> {
                            keys += 1
                            if (keys == 2 && failOnKey) return if (throwPanic) throw panic else failure
                            check(keys <= 2) { "read past the first failure" }
                            return seed.deserialize(ContentDeserializer.new(newStringContent(if (keys == 1) "kind" else "x")))
                        }
                        override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
                            values += 1
                            if (values == 2) return if (throwPanic) throw panic else failure
                            return seed.deserialize(ContentDeserializer.new(newStringContent("A")))
                        }
                    }
                val visitor = TaggedContentVisitor.new("kind", "tagged content", StringDeserialize)
                if (throwPanic) {
                    assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { visitor.visitMap(access) })
                } else {
                    assertSame(failure, visitor.visitMap(access))
                }
                assertEquals(2, keys)
                assertEquals(if (failOnKey) 1 else 2, values)
            }
        }
    }

    @Test
    fun taggedSequenceRemainderFailureAndPanicPropagateAfterTheTag() {
        for (throwPanic in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("sequence remainder stopped"))
            val panic = VisitorBoundaryPanic()
            val access =
                BoundarySeqAccess(listOf(newStringContent("A"), newI32Content(1))) {
                    if (throwPanic) throw panic else failure
                }
            val visitor = TaggedContentVisitor.new("kind", "tagged content", StringDeserialize)
            if (throwPanic) {
                assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { visitor.visitSeq(access) })
            } else {
                assertSame(failure, visitor.visitSeq(access))
            }
            assertEquals(2, access.calls)
            assertEquals(1, access.hintCalls)
        }
    }

    @Test
    fun invalidSizeHintsRemainTypedFailuresBeforeAnyAccess() {
        var reads = 0
        val sequence =
            object : SeqAccess {
                override fun sizeHint(): Int = -1
                override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> {
                    reads += 1
                    error("invalid hint must be rejected first")
                }
            }
        val map =
            object : MapAccess {
                override fun sizeHint(): Int = -1
                override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> {
                    reads += 1
                    error("invalid hint must be rejected first")
                }
                override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
                    reads += 1
                    error("invalid hint must be rejected first")
                }
            }
        val results =
            listOf(
                ContentVisitor.new().visitSeq(sequence),
                ContentVisitor.new().visitMap(map),
                TaggedContentVisitor.new("kind", "tagged content", StringDeserialize).visitMap(map),
            )
        for (result in results) assertEquals("A Rust size hint cannot be negative", result.exceptionOrNull()?.message)
        assertEquals(0, reads)
    }

    @Test
    fun thrownSizeHintCallbacksEscapeUnchanged() {
        val panic = VisitorBoundaryPanic()
        val sequence =
            object : SeqAccess {
                override fun sizeHint(): Int = throw panic
                override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> = error("sizeHint failed")
            }
        val map =
            object : MapAccess {
                override fun sizeHint(): Int = throw panic
                override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> = error("sizeHint failed")
                override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> = error("sizeHint failed")
            }
        assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { ContentVisitor.new().visitSeq(sequence) })
        assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { ContentVisitor.new().visitMap(map) })
        assertSame(panic, assertFailsWith<VisitorBoundaryPanic> { TaggedContentVisitor.new("kind", "tagged content", StringDeserialize).visitMap(map) })
    }
}
