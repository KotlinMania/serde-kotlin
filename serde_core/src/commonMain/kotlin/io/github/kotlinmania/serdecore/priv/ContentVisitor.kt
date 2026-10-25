// port-lint: source serde/src/private/de.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serdecore.priv.generated.content.Content
import io.github.kotlinmania.serdecore.priv.contentaccess.*
import io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry
import io.github.kotlinmania.serdecore.priv.contentaccess.newContentBuffer
import org.rustlang.core.slice.SharedByteSlice

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.Visitor

internal fun contentAsStr(content: Content): String? =
    when (content) {
        is Content.Str -> content.value
        is Content.String -> content.value
        is Content.Bytes -> serdeCatching { content.value.decodeToString() }.getOrNull()
        is Content.ByteBuf -> content.value.decodeToStringOrNull()
        else -> null
    }

internal fun contentClone(content: Content): Content =
    when (content) {
        is Content.Bool -> newBoolContent(content.value)
        is Content.U8 -> newU8Content(content.value)
        is Content.U16 -> newU16Content(content.value)
        is Content.U32 -> newU32Content(content.value)
        is Content.U64 -> newU64Content(content.value)
        is Content.I8 -> newI8Content(content.value)
        is Content.I16 -> newI16Content(content.value)
        is Content.I32 -> newI32Content(content.value)
        is Content.I64 -> newI64Content(content.value)
        is Content.F32 -> newF32Content(content.value)
        is Content.F64 -> newF64Content(content.value)
        is Content.Char -> newCharContent(content.value)
        is Content.String -> newStringContent(content.value)
        is Content.Str -> newStrContent(content.value)
        is Content.ByteBuf -> newByteBufContent(content.value.copyOf())
        is Content.Bytes -> newBytesContent(content.value)
        Content.None -> Content.None
        is Content.Some -> newSomeContent(contentClone(content.value))
        Content.Unit -> Content.Unit
        is Content.Newtype -> newNewtypeContent(contentClone(content.value))
        is Content.Seq -> newSeqContent(content.value.map(::contentClone))
        is Content.Map ->
            newMapContent(
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
        is Content.Char -> unexpectedChar(content.value)
        is Content.String -> Unexpected.Str(content.value)
        is Content.Str -> Unexpected.Str(content.value)
        is Content.ByteBuf -> Unexpected.Bytes(content.value)
        is Content.Bytes -> Unexpected.Bytes(content.value.copyBytes())
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

    override fun visitBool(v: Boolean): SerdeResult<Content> = SerdeResult.success(newBoolContent(v))

    override fun visitI8(v: Byte): SerdeResult<Content> = SerdeResult.success(newI8Content(v))

    override fun visitI16(v: Short): SerdeResult<Content> = SerdeResult.success(newI16Content(v))

    override fun visitI32(v: Int): SerdeResult<Content> = SerdeResult.success(newI32Content(v))

    override fun visitI64(v: Long): SerdeResult<Content> = SerdeResult.success(newI64Content(v))

    override fun visitU8(v: UByte): SerdeResult<Content> = SerdeResult.success(newU8Content(v))

    override fun visitU16(v: UShort): SerdeResult<Content> = SerdeResult.success(newU16Content(v))

    override fun visitU32(v: UInt): SerdeResult<Content> = SerdeResult.success(newU32Content(v))

    override fun visitU64(v: ULong): SerdeResult<Content> = SerdeResult.success(newU64Content(v))

    override fun visitF32(v: Float): SerdeResult<Content> = SerdeResult.success(newF32Content(v))

    override fun visitF64(v: Double): SerdeResult<Content> = SerdeResult.success(newF64Content(v))

    override fun visitChar(v: Char): SerdeResult<Content> = visitCharCodePoint(v.code)

    override fun visitCharCodePoint(codePoint: Int): SerdeResult<Content> = serdeCatching { newCharContent(codePoint) }

    override fun visitBorrowedByteSlice(v: SharedByteSlice): SerdeResult<Content> = SerdeResult.success(newBytesContent(v))

    override fun visitStr(v: String): SerdeResult<Content> = serdeCatching { newStringContent(v) }

    override fun visitBorrowedStr(v: String): SerdeResult<Content> = serdeCatching { newStrContent(v) }

    override fun visitString(v: String): SerdeResult<Content> = serdeCatching { newStringContent(v) }

    override fun visitBytes(v: ByteArray): SerdeResult<Content> = SerdeResult.success(newByteBufContent(v.copyOf()))

    override fun visitBorrowedBytes(v: ByteArray): SerdeResult<Content> = SerdeResult.success(newBytesContent(v))

    override fun visitByteBuf(v: ByteArray): SerdeResult<Content> = SerdeResult.success(newByteBufContent(v.copyOf()))

    override fun visitUnit(): SerdeResult<Content> = SerdeResult.success(Content.Unit)

    override fun visitNone(): SerdeResult<Content> = SerdeResult.success(Content.None)

    override fun <D> visitSome(deserializer: D): SerdeResult<Content>
        where D : Deserializer =
        deserialize(deserializer).map { newSomeContent(it) }

    override fun <D> visitNewtypeStruct(deserializer: D): SerdeResult<Content>
        where D : Deserializer =
        deserialize(deserializer).map { newNewtypeContent(it) }

    override fun <A> visitSeq(access: A): SerdeResult<Content>
        where A : SeqAccess =
        serdeCatching {
            val elements = newContentBuffer(access.sizeHint())
            while (true) {
                when (val next = access.nextElementSeed(new()).getOrThrow()) {
                    is SeqElement.Some -> elements += next.value
                    SeqElement.None -> break
                }
            }
            takeSeqContent(elements)
        }

    override fun <A> visitMap(access: A): SerdeResult<Content>
        where A : MapAccess =
        serdeCatching {
            val entries = newContentPairBuffer(access.sizeHint())
            while (true) {
                val next = access.nextEntrySeed(new(), new()).getOrThrow() ?: break
                entries += next
            }
            takeMapContent(entries)
        }

    override fun <A> visitEnum(access: A): SerdeResult<Content>
        where A : EnumAccess =
        SerdeResult.failure(SerdeError.custom("untagged and internally tagged enums do not support enum input"))
}

private fun ByteArray.decodeToStringOrNull(): String? =
    try {
        decodeToString(throwOnInvalidSequence = true)
    } catch (_: Throwable) {
        null
    }
