package io.github.kotlinmania.serdecore.priv.contentaccess

import io.github.kotlinmania.serdecore.priv.Content as LegacyContent
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import io.github.kotlinmania.serdecore.priv.generated.content.__rtkChar
import io.github.kotlinmania.serdecore.priv.generatedContentCapacity
import io.github.kotlinmania.serdecore.de.UnexpectedScalarChar
import io.github.kotlinmania.serdecore.de.Unexpected
import org.rustlang.core.slice.SharedByteSlice

/** Access to the complete generated declaration across the core/root module boundary. */
fun newBoolContent(value: Boolean): Content.Bool = Content.Bool(value)
val Content.Bool.value: Boolean get() = __rtkField0
fun newU8Content(value: UByte): Content.U8 = Content.U8(value)
val Content.U8.value: UByte get() = __rtkField0
fun newU16Content(value: UShort): Content.U16 = Content.U16(value)
val Content.U16.value: UShort get() = __rtkField0
fun newU32Content(value: UInt): Content.U32 = Content.U32(value)
val Content.U32.value: UInt get() = __rtkField0
fun newU64Content(value: ULong): Content.U64 = Content.U64(value)
val Content.U64.value: ULong get() = __rtkField0
fun newI8Content(value: Byte): Content.I8 = Content.I8(value)
val Content.I8.value: Byte get() = __rtkField0
fun newI16Content(value: Short): Content.I16 = Content.I16(value)
val Content.I16.value: Short get() = __rtkField0
fun newI32Content(value: Int): Content.I32 = Content.I32(value)
val Content.I32.value: Int get() = __rtkField0
fun newI64Content(value: Long): Content.I64 = Content.I64(value)
val Content.I64.value: Long get() = __rtkField0
fun newF32Content(value: Float): Content.F32 = Content.F32(value)
val Content.F32.value: Float get() = __rtkField0
fun newF64Content(value: Double): Content.F64 = Content.F64(value)
val Content.F64.value: Double get() = __rtkField0

fun newCharContent(codePoint: Int): Content.Char = Content.Char(__rtkChar(codePoint))
val Content.Char.value: Int get() = __rtkField0.codePoint
fun scalarText(codePoint: Int): String = __rtkChar(codePoint).scalarString()
fun validScalar(codePoint: Int): Boolean = codePoint in 0..0x10ffff && codePoint !in 0xd800..0xdfff
private fun validText(value: String): String = value.encodeToByteArray(throwOnInvalidSequence = true).let { value }
fun newStringContent(value: String): Content.String = Content.String(StringBuilder(validText(value)))
val Content.String.value: String get() = __rtkField0.toString()
fun newStrContent(value: String): Content.Str = Content.Str(validText(value))
val Content.Str.value: String get() = __rtkField0
fun newByteBufContent(value: ByteArray): Content.ByteBuf = Content.ByteBuf(value.map { it.toUByte() }.toMutableList())
val Content.ByteBuf.value: ByteArray get() = __rtkField0.map { it.toByte() }.toByteArray()
fun newBytesContent(value: ByteArray): Content.Bytes = newBytesContent(SharedByteSlice.borrow(value))
fun newBytesContent(value: SharedByteSlice): Content.Bytes = Content.Bytes(value)
val Content.Bytes.value: SharedByteSlice get() = __rtkField0
fun SharedByteSlice.decodeToString(throwOnInvalidSequence: Boolean = true): String = copyBytes().decodeToString(throwOnInvalidSequence = throwOnInvalidSequence)
fun newSomeContent(value: Content): Content.Some = Content.Some(value)
val Content.Some.value: Content get() = __rtkField0
fun newNewtypeContent(value: Content): Content.Newtype = Content.Newtype(value)
val Content.Newtype.value: Content get() = __rtkField0
fun newSeqContent(value: List<Content>): Content.Seq = Content.Seq(value.toMutableList())
/** Transfers a capture buffer; the caller must stop mutating it after this call. */
fun takeSeqContent(value: MutableList<Content>): Content.Seq = Content.Seq(value)
val Content.Seq.value: List<Content> get() = __rtkField0
class ContentMapEntry(val key: Content, val value: Content)
fun newMapContent(value: List<ContentMapEntry>): Content.Map = Content.Map(value.map { it.key to it.value }.toMutableList())
/** Transfers a capture buffer without replacing its selected allocation policy. */
fun takeMapContent(value: MutableList<Pair<Content, Content>>): Content.Map = Content.Map(value)
val Content.Map.value: List<ContentMapEntry> get() = __rtkField0.map { ContentMapEntry(it.first, it.second) }

/** Legacy values are copied into owned storage; borrowed arrays retain their selected storage. */
fun LegacyContent.toGeneratedContent(): Content = when (this) {
    is LegacyContent.Bool -> newBoolContent(value)
    is LegacyContent.U8 -> newU8Content(value)
    is LegacyContent.U16 -> newU16Content(value)
    is LegacyContent.U32 -> newU32Content(value)
    is LegacyContent.U64 -> newU64Content(value)
    is LegacyContent.I8 -> newI8Content(value)
    is LegacyContent.I16 -> newI16Content(value)
    is LegacyContent.I32 -> newI32Content(value)
    is LegacyContent.I64 -> newI64Content(value)
    is LegacyContent.F32 -> newF32Content(value)
    is LegacyContent.F64 -> newF64Content(value)
    is LegacyContent.Char -> newCharContent(value.code)
    is LegacyContent.StringValue -> newStringContent(value)
    is LegacyContent.Str -> newStrContent(value)
    is LegacyContent.ByteBuf -> newByteBufContent(value)
    is LegacyContent.Bytes -> newBytesContent(value)
    LegacyContent.None -> Content.None
    LegacyContent.Unit -> Content.Unit
    is LegacyContent.Some -> newSomeContent(value.toGeneratedContent())
    is LegacyContent.Newtype -> newNewtypeContent(value.toGeneratedContent())
    is LegacyContent.Seq -> newSeqContent(value.map { it.toGeneratedContent() })
    is LegacyContent.Map -> newMapContent(value.map { ContentMapEntry(it.key.toGeneratedContent(), it.value.toGeneratedContent()) })
}

fun unexpectedChar(codePoint: Int): Unexpected = if (codePoint <= 0xffff) Unexpected.CharValue(codePoint.toChar()) else UnexpectedScalarChar(codePoint)
fun newContentBuffer(hint: Int?): ArrayList<Content> = generatedContentCapacity(hint, false)
fun newContentPairBuffer(hint: Int?): ArrayList<Pair<Content, Content>> = generatedContentCapacity(hint, true)
