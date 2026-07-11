// port-lint: tests test_suite/tests/test_enum_untagged.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class ContentVisitorTest {
    @Test
    fun contentAsStrAcceptsStringsAndUtf8Bytes() {
        assertEquals("tag", contentAsStr(Content.Str("tag")))
        assertEquals("tag", contentAsStr(Content.StringValue("tag")))
        assertEquals("tag", contentAsStr(Content.Bytes("tag".encodeToByteArray())))
        assertEquals("tag", contentAsStr(Content.ByteBuf("tag".encodeToByteArray())))
        assertNull(contentAsStr(Content.I32(1)))
    }

    @Test
    fun contentUnexpectedMapsBufferedKinds() {
        assertEquals(Unexpected.Bool(true), contentUnexpected(Content.Bool(true)))
        assertEquals(Unexpected.Unsigned(7UL), contentUnexpected(Content.U8(7u.toUByte())))
        assertEquals(Unexpected.Signed(-7), contentUnexpected(Content.I16((-7).toShort())))
        assertEquals(Unexpected.FloatValue(1.5), contentUnexpected(Content.F32(1.5f)))
        assertEquals(Unexpected.CharValue('x'), contentUnexpected(Content.Char('x')))
        assertEquals(Unexpected.Str("x"), contentUnexpected(Content.StringValue("x")))
        assertEquals(Unexpected.Option, contentUnexpected(Content.Some(Content.Unit)))
        assertEquals(Unexpected.NewtypeStruct, contentUnexpected(Content.Newtype(Content.Unit)))
        assertEquals(Unexpected.Seq, contentUnexpected(Content.Seq(emptyList())))
        assertEquals(Unexpected.Map, contentUnexpected(Content.Map(emptyList())))
    }

    @Test
    fun contentCloneDeepCopiesNestedBuffers() {
        val bytes = byteArrayOf(1, 2)
        val content = Content.Map(
            listOf(
                ContentMapEntry(
                    Content.ByteBuf(bytes),
                    Content.Seq(listOf(Content.Bytes(bytes))),
                ),
            ),
        )

        val cloned = assertIs<Content.Map>(contentClone(content))
        assertEquals(content, cloned)
        assertNotSame(content.value, cloned.value)

        bytes[0] = 9
        val clonedKey = assertIs<Content.ByteBuf>(cloned.value.single().key)
        val clonedValue = assertIs<Content.Seq>(cloned.value.single().value)
        val clonedNestedBytes = assertIs<Content.Bytes>(clonedValue.value.single())
        assertContentEquals(byteArrayOf(1, 2), clonedKey.value)
        assertContentEquals(byteArrayOf(1, 2), clonedNestedBytes.value)
    }

    @Test
    fun contentVisitorCapturesScalarValues() {
        val visitor = ContentVisitor()

        assertEquals(Content.Bool(true), visitor.visitBool(true).getOrThrow())
        assertEquals(Content.I8((-1).toByte()), visitor.visitI8((-1).toByte()).getOrThrow())
        assertEquals(Content.I16(2.toShort()), visitor.visitI16(2.toShort()).getOrThrow())
        assertEquals(Content.I32(3), visitor.visitI32(3).getOrThrow())
        assertEquals(Content.I64(4), visitor.visitI64(4).getOrThrow())
        assertEquals(Content.U8(5u.toUByte()), visitor.visitU8(5u.toUByte()).getOrThrow())
        assertEquals(Content.U16(6u.toUShort()), visitor.visitU16(6u.toUShort()).getOrThrow())
        assertEquals(Content.U32(7u), visitor.visitU32(7u).getOrThrow())
        assertEquals(Content.U64(8u), visitor.visitU64(8u).getOrThrow())
        assertEquals(Content.F32(1.25f), visitor.visitF32(1.25f).getOrThrow())
        assertEquals(Content.F64(2.5), visitor.visitF64(2.5).getOrThrow())
        assertEquals(Content.Char('c'), visitor.visitChar('c').getOrThrow())
        assertEquals(Content.StringValue("owned"), visitor.visitStr("owned").getOrThrow())
        assertEquals(Content.Str("borrowed"), visitor.visitBorrowedStr("borrowed").getOrThrow())
        assertEquals(Content.Unit, visitor.visitUnit().getOrThrow())
        assertEquals(Content.None, visitor.visitNone().getOrThrow())
    }

    @Test
    fun contentVisitorCopiesByteInputs() {
        val bytes = byteArrayOf(1, 2)
        val byteBuf = assertIs<Content.ByteBuf>(ContentVisitor().visitBytes(bytes).getOrThrow())
        val borrowed = assertIs<Content.Bytes>(ContentVisitor().visitBorrowedBytes(bytes).getOrThrow())
        val owned = assertIs<Content.ByteBuf>(ContentVisitor().visitByteBuf(bytes).getOrThrow())

        bytes[0] = 9
        assertContentEquals(byteArrayOf(1, 2), byteBuf.value)
        assertContentEquals(byteArrayOf(1, 2), borrowed.value)
        assertContentEquals(byteArrayOf(1, 2), owned.value)
    }

    @Test
    fun contentVisitorWrapsSomeAndNewtypeContent() {
        assertEquals(
            Content.Some(Content.StringValue("value")),
            ContentVisitor().visitSome(ScalarDeserializer("value")).getOrThrow(),
        )
        assertEquals(
            Content.Newtype(Content.StringValue("value")),
            ContentVisitor().visitNewtypeStruct(ScalarDeserializer("value")).getOrThrow(),
        )
    }

    @Test
    fun contentVisitorBuffersSequencesAndMaps() {
        val seq = ContentVisitor()
            .visitSeq(BufferedSeqAccess(mutableListOf(ScalarDeserializer("a"), ScalarDeserializer("b"))))
            .getOrThrow()

        assertEquals(
            Content.Seq(listOf(Content.StringValue("a"), Content.StringValue("b"))),
            seq,
        )

        val map = ContentVisitor()
            .visitMap(
                BufferedMapAccess(
                    mutableListOf(
                        ScalarDeserializer("k") to ScalarDeserializer("v"),
                    ),
                ),
            )
            .getOrThrow()

        assertEquals(
            Content.Map(listOf(ContentMapEntry(Content.StringValue("k"), Content.StringValue("v")))),
            map,
        )
    }

    @Test
    fun contentVisitorRejectsEnumInput() {
        val error = ContentVisitor()
            .visitEnum(RejectingEnumAccess)
            .exceptionOrNull()

        assertEquals(
            "untagged and internally tagged enums do not support enum input",
            error?.message,
        )
    }
}

private class ScalarDeserializer(
    private val value: String,
) : Deserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitStr(value)
    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSome(this)
    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = visitor.visitNewtypeStruct(this)
    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
}

private class BufferedSeqAccess(
    private val elements: MutableList<ScalarDeserializer>,
) : SeqAccess {
    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?> {
        val next = elements.removeFirstOrNull() ?: return SerdeResult.success(null)
        return seed.deserialize(next).map { it }
    }
}

private class BufferedMapAccess(
    private val entries: MutableList<Pair<ScalarDeserializer, ScalarDeserializer>>,
) : MapAccess {
    private var pendingValue: ScalarDeserializer? = null

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> {
        val (key, value) = entries.removeFirstOrNull() ?: return SerdeResult.success(null)
        pendingValue = value
        return seed.deserialize(key).map { it }
    }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
        val value = pendingValue
            ?: return SerdeResult.failure(SerdeError.custom("value requested before key"))
        pendingValue = null
        return seed.deserialize(value)
    }
}

private data object RejectingEnumAccess : EnumAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        SerdeResult.failure(SerdeError.custom("variant access was not expected"))
}
