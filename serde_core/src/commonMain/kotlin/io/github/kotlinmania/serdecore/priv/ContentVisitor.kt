// port-lint: source serde/src/private/de.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.Visitor

internal fun contentAsStr(content: Content): String? =
    when (content) {
        is Content.Str -> content.value
        is Content.StringValue -> content.value
        is Content.Bytes -> content.value.decodeToStringOrNull()
        is Content.ByteBuf -> content.value.decodeToStringOrNull()
        else -> null
    }

internal fun contentClone(content: Content): Content =
    when (content) {
        is Content.Bool -> Content.Bool(content.value)
        is Content.U8 -> Content.U8(content.value)
        is Content.U16 -> Content.U16(content.value)
        is Content.U32 -> Content.U32(content.value)
        is Content.U64 -> Content.U64(content.value)
        is Content.I8 -> Content.I8(content.value)
        is Content.I16 -> Content.I16(content.value)
        is Content.I32 -> Content.I32(content.value)
        is Content.I64 -> Content.I64(content.value)
        is Content.F32 -> Content.F32(content.value)
        is Content.F64 -> Content.F64(content.value)
        is Content.Char -> Content.Char(content.value)
        is Content.StringValue -> Content.StringValue(content.value)
        is Content.Str -> Content.Str(content.value)
        is Content.ByteBuf -> Content.ByteBuf(content.value.copyOf())
        is Content.Bytes -> Content.Bytes(content.value.copyOf())
        Content.None -> Content.None
        is Content.Some -> Content.Some(contentClone(content.value))
        Content.Unit -> Content.Unit
        is Content.Newtype -> Content.Newtype(contentClone(content.value))
        is Content.Seq -> Content.Seq(content.value.map(::contentClone))
        is Content.Map ->
            Content.Map(
                content.value.map { entry ->
                    ContentMapEntry(
                        contentClone(entry.key),
                        contentClone(entry.value),
                    )
                },
            )
    }

internal fun contentUnexpected(content: Content): Unexpected =
    when (content) {
        is Content.Bool -> Unexpected.Bool(content.value)
        is Content.U8 -> Unexpected.Unsigned(content.value.toULong())
        is Content.U16 -> Unexpected.Unsigned(content.value.toULong())
        is Content.U32 -> Unexpected.Unsigned(content.value.toULong())
        is Content.U64 -> Unexpected.Unsigned(content.value)
        is Content.I8 -> Unexpected.Signed(content.value.toLong())
        is Content.I16 -> Unexpected.Signed(content.value.toLong())
        is Content.I32 -> Unexpected.Signed(content.value.toLong())
        is Content.I64 -> Unexpected.Signed(content.value)
        is Content.F32 -> Unexpected.FloatValue(content.value.toDouble())
        is Content.F64 -> Unexpected.FloatValue(content.value)
        is Content.Char -> Unexpected.CharValue(content.value)
        is Content.StringValue -> Unexpected.Str(content.value)
        is Content.Str -> Unexpected.Str(content.value)
        is Content.ByteBuf -> Unexpected.Bytes(content.value)
        is Content.Bytes -> Unexpected.Bytes(content.value)
        Content.None,
        is Content.Some,
        -> Unexpected.Option
        Content.Unit -> Unexpected.UnitValue
        is Content.Newtype -> Unexpected.NewtypeStruct
        is Content.Seq -> Unexpected.Seq
        is Content.Map -> Unexpected.Map
    }

internal class ContentVisitor : Visitor<Content>, DeserializeSeed<Content> {
    companion object {
        fun new(): ContentVisitor = ContentVisitor()
    }

    override fun expecting(): String = "any value"

    override fun <D> deserialize(deserializer: D): SerdeResult<Content>
        where D : Deserializer =
        deserializer.deserializeAny(this)

    override fun visitBool(v: Boolean): SerdeResult<Content> = SerdeResult.success(Content.Bool(v))

    override fun visitI8(v: Byte): SerdeResult<Content> = SerdeResult.success(Content.I8(v))

    override fun visitI16(v: Short): SerdeResult<Content> = SerdeResult.success(Content.I16(v))

    override fun visitI32(v: Int): SerdeResult<Content> = SerdeResult.success(Content.I32(v))

    override fun visitI64(v: Long): SerdeResult<Content> = SerdeResult.success(Content.I64(v))

    override fun visitU8(v: UByte): SerdeResult<Content> = SerdeResult.success(Content.U8(v))

    override fun visitU16(v: UShort): SerdeResult<Content> = SerdeResult.success(Content.U16(v))

    override fun visitU32(v: UInt): SerdeResult<Content> = SerdeResult.success(Content.U32(v))

    override fun visitU64(v: ULong): SerdeResult<Content> = SerdeResult.success(Content.U64(v))

    override fun visitF32(v: Float): SerdeResult<Content> = SerdeResult.success(Content.F32(v))

    override fun visitF64(v: Double): SerdeResult<Content> = SerdeResult.success(Content.F64(v))

    override fun visitChar(v: Char): SerdeResult<Content> = SerdeResult.success(Content.Char(v))

    override fun visitStr(v: String): SerdeResult<Content> = SerdeResult.success(Content.StringValue(v))

    override fun visitBorrowedStr(v: String): SerdeResult<Content> = SerdeResult.success(Content.Str(v))

    override fun visitString(v: String): SerdeResult<Content> = SerdeResult.success(Content.StringValue(v))

    override fun visitBytes(v: ByteArray): SerdeResult<Content> = SerdeResult.success(Content.ByteBuf(v.copyOf()))

    override fun visitBorrowedBytes(v: ByteArray): SerdeResult<Content> = SerdeResult.success(Content.Bytes(v.copyOf()))

    override fun visitByteBuf(v: ByteArray): SerdeResult<Content> = SerdeResult.success(Content.ByteBuf(v.copyOf()))

    override fun visitUnit(): SerdeResult<Content> = SerdeResult.success(Content.Unit)

    override fun visitNone(): SerdeResult<Content> = SerdeResult.success(Content.None)

    override fun <D> visitSome(deserializer: D): SerdeResult<Content>
        where D : Deserializer =
        deserialize(deserializer).map { Content.Some(it) }

    override fun <D> visitNewtypeStruct(deserializer: D): SerdeResult<Content>
        where D : Deserializer =
        deserialize(deserializer).map { Content.Newtype(it) }

    override fun <A> visitSeq(access: A): SerdeResult<Content>
        where A : SeqAccess =
        serdeCatching {
            val elements = ArrayList<Content>(cautious<Content>(access.sizeHint()))
            while (true) {
                val next = access.nextElementSeed(new()).getOrThrow() ?: break
                elements += next
            }
            Content.Seq(elements)
        }

    override fun <A> visitMap(access: A): SerdeResult<Content>
        where A : MapAccess =
        serdeCatching {
            val entries = ArrayList<ContentMapEntry>(cautious<ContentMapEntry>(access.sizeHint()))
            while (true) {
                val next = access.nextEntrySeed(new(), new()).getOrThrow() ?: break
                entries += ContentMapEntry(next.first, next.second)
            }
            Content.Map(entries)
        }

    override fun <A> visitEnum(access: A): SerdeResult<Content>
        where A : EnumAccess =
        SerdeResult.failure(SerdeError.custom("untagged and internally tagged enums do not support enum input"))
}

private fun ByteArray.decodeToStringOrNull(): String? =
    try {
        decodeToString()
    } catch (_: Throwable) {
        null
    }
