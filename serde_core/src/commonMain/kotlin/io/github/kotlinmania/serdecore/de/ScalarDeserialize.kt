package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.priv.contentaccess.validScalar
import org.rustlang.core.slice.SharedByteSlice

private object CharCodePointVisitor : Visitor<Int> {
    override fun expecting(): String = "a Unicode scalar character"
    override fun visitChar(v: Char): SerdeResult<Int> = visitCharCodePoint(v.code)
    override fun visitCharCodePoint(codePoint: Int): SerdeResult<Int> =
        if (validScalar(codePoint)) SerdeResult.success(codePoint) else SerdeResult.failure(SerdeError("Invalid Unicode scalar: $codePoint"))
    override fun visitStr(v: String): SerdeResult<Int> {
        val codePoint = when {
            v.length == 1 -> v[0].code
            v.length == 2 && v[0].code in 0xd800..0xdbff && v[1].code in 0xdc00..0xdfff ->
                0x10000 + ((v[0].code - 0xd800) shl 10) + v[1].code - 0xdc00
            else -> -1
        }
        return if (validScalar(codePoint)) SerdeResult.success(codePoint)
        else SerdeResult.failure(SerdeError.invalidValue(Unexpected.Str(v), this))
    }
}

/** Deserializes a character without narrowing it to one UTF-16 code unit. */
data object CharCodePointDeserialize : Deserialize<Int> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Int> = deserializer.deserializeChar(CharCodePointVisitor)
}

private object ByteSliceVisitor : Visitor<SharedByteSlice> {
    override fun expecting(): String = "a borrowed byte slice"
    override fun visitBorrowedByteSlice(v: SharedByteSlice): SerdeResult<SharedByteSlice> = SerdeResult.success(v)
    override fun visitBorrowedBytes(v: ByteArray): SerdeResult<SharedByteSlice> = SerdeResult.success(SharedByteSlice.borrow(v))
}

/** Retains the input region instead of materializing a legacy byte array. */
data object BorrowedByteSliceDeserialize : Deserialize<SharedByteSlice> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<SharedByteSlice> = deserializer.deserializeBytes(ByteSliceVisitor)
}
