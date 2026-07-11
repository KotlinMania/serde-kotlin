// port-lint: source de/impls.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.priv.fromUtf8LossyNoAlloc
import io.github.kotlinmania.serde.serdeCatching
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// //////////////////////////////////////////////////////////////////////////////

private data object UnitVisitor : Visitor<Unit> {
    override fun expecting(): String = "unit"

    override fun visitUnit(): SerdeResult<Unit> = SerdeResult.success(Unit)
}

data object UnitDeserialize : Deserialize<Unit> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Unit>
        where D : Deserializer =
        deserializer.deserializeUnit(UnitVisitor)
}

// //////////////////////////////////////////////////////////////////////////////

private data object BoolVisitor : Visitor<Boolean> {
    override fun expecting(): String = "a boolean"

    override fun visitBool(v: Boolean): SerdeResult<Boolean> = SerdeResult.success(v)
}

data object BooleanDeserialize : Deserialize<Boolean> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Boolean>
        where D : Deserializer =
        deserializer.deserializeBool(BoolVisitor)
}

// //////////////////////////////////////////////////////////////////////////////

private fun invalidSigned(
    v: Long,
    exp: Expected,
): SerdeResult<Nothing> = SerdeResult.failure(SerdeError.invalidValue(Unexpected.Signed(v), exp))

private fun invalidUnsigned(
    v: ULong,
    exp: Expected,
): SerdeResult<Nothing> = SerdeResult.failure(SerdeError.invalidValue(Unexpected.Unsigned(v), exp))

private fun unsignedLessOrEqual(
    value: UInt,
    max: UInt,
): Boolean = (value.toInt() xor Int.MIN_VALUE) <= (max.toInt() xor Int.MIN_VALUE)

private fun unsignedLessOrEqual(
    value: ULong,
    max: ULong,
): Boolean = (value.toLong() xor Long.MIN_VALUE) <= (max.toLong() xor Long.MIN_VALUE)

private fun unsignedDecimal(value: ULong): String {
    val bits = value.toLong()
    if (bits >= 0) return bits.toString()

    val quotient = (bits ushr 1) / 5
    val remainder = bits - quotient * 10
    return quotient.toString() + remainder.toString()
}

private data object I8Visitor : Visitor<Byte> {
    override fun expecting(): String = "i8"

    override fun visitI8(v: Byte): SerdeResult<Byte> = SerdeResult.success(v)

    override fun visitI16(v: Short): SerdeResult<Byte> =
        if (v in Byte.MIN_VALUE..Byte.MAX_VALUE) SerdeResult.success(v.toByte()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): SerdeResult<Byte> =
        if (v in Byte.MIN_VALUE..Byte.MAX_VALUE) SerdeResult.success(v.toByte()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): SerdeResult<Byte> =
        if (v in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()) SerdeResult.success(v.toByte()) else invalidSigned(v, this)

    override fun visitU8(v: UByte): SerdeResult<Byte> =
        if (v.toInt() <= Byte.MAX_VALUE) SerdeResult.success(v.toByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU16(v: UShort): SerdeResult<Byte> =
        if (v.toInt() <= Byte.MAX_VALUE) SerdeResult.success(v.toByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU32(v: UInt): SerdeResult<Byte> =
        if (v.toLong() <= Byte.MAX_VALUE.toLong()) SerdeResult.success(v.toByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): SerdeResult<Byte> =
        if (unsignedLessOrEqual(v, Byte.MAX_VALUE.toULong())) SerdeResult.success(v.toByte()) else invalidUnsigned(v, this)
}

data object I8Deserialize : Deserialize<Byte> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Byte>
        where D : Deserializer = deserializer.deserializeI8(I8Visitor)
}

private data object I16Visitor : Visitor<Short> {
    override fun expecting(): String = "i16"

    override fun visitI16(v: Short): SerdeResult<Short> = SerdeResult.success(v)

    override fun visitI8(v: Byte): SerdeResult<Short> = SerdeResult.success(v.toShort())

    override fun visitI32(v: Int): SerdeResult<Short> =
        if (v in Short.MIN_VALUE..Short.MAX_VALUE) SerdeResult.success(v.toShort()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): SerdeResult<Short> =
        if (v in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()) SerdeResult.success(v.toShort()) else invalidSigned(v, this)

    override fun visitU8(v: UByte): SerdeResult<Short> = SerdeResult.success(v.toShort())

    override fun visitU16(v: UShort): SerdeResult<Short> =
        if (v.toInt() <= Short.MAX_VALUE) SerdeResult.success(v.toShort()) else invalidUnsigned(v.toULong(), this)

    override fun visitU32(v: UInt): SerdeResult<Short> =
        if (v.toLong() <= Short.MAX_VALUE.toLong()) SerdeResult.success(v.toShort()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): SerdeResult<Short> =
        if (unsignedLessOrEqual(v, Short.MAX_VALUE.toULong())) SerdeResult.success(v.toShort()) else invalidUnsigned(v, this)
}

data object I16Deserialize : Deserialize<Short> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Short>
        where D : Deserializer =
        deserializer.deserializeI16(I16Visitor)
}

private data object I32Visitor : Visitor<Int> {
    override fun expecting(): String = "i32"

    override fun visitI32(v: Int): SerdeResult<Int> = SerdeResult.success(v)

    override fun visitI8(v: Byte): SerdeResult<Int> = SerdeResult.success(v.toInt())

    override fun visitI16(v: Short): SerdeResult<Int> = SerdeResult.success(v.toInt())

    override fun visitI64(v: Long): SerdeResult<Int> =
        if (v in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) SerdeResult.success(v.toInt()) else invalidSigned(v, this)

    override fun visitU8(v: UByte): SerdeResult<Int> = SerdeResult.success(v.toInt())

    override fun visitU16(v: UShort): SerdeResult<Int> = SerdeResult.success(v.toInt())

    override fun visitU32(v: UInt): SerdeResult<Int> =
        if (unsignedLessOrEqual(v, Int.MAX_VALUE.toUInt())) SerdeResult.success(v.toInt()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): SerdeResult<Int> =
        if (unsignedLessOrEqual(v, Int.MAX_VALUE.toULong())) {
            SerdeResult.success(v.toInt())
        } else {
            invalidUnsigned(v, this)
        }
}

data object I32Deserialize : Deserialize<Int> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Int>
        where D : Deserializer = deserializer.deserializeI32(I32Visitor)
}

private data object I64Visitor : Visitor<Long> {
    override fun expecting(): String = "i64"

    override fun visitI64(v: Long): SerdeResult<Long> = SerdeResult.success(v)

    override fun visitI8(v: Byte): SerdeResult<Long> = SerdeResult.success(v.toLong())

    override fun visitI16(v: Short): SerdeResult<Long> = SerdeResult.success(v.toLong())

    override fun visitI32(v: Int): SerdeResult<Long> = SerdeResult.success(v.toLong())

    override fun visitU8(v: UByte): SerdeResult<Long> = SerdeResult.success(v.toLong())

    override fun visitU16(v: UShort): SerdeResult<Long> = SerdeResult.success(v.toLong())

    override fun visitU32(v: UInt): SerdeResult<Long> = SerdeResult.success(v.toLong())

    override fun visitU64(v: ULong): SerdeResult<Long> =
        if (unsignedLessOrEqual(v, Long.MAX_VALUE.toULong())) SerdeResult.success(v.toLong()) else invalidUnsigned(v, this)
}

data object I64Deserialize : Deserialize<Long> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Long>
        where D : Deserializer =
        deserializer.deserializeI64(I64Visitor)
}

private data object U8Visitor : Visitor<UByte> {
    override fun expecting(): String = "u8"

    override fun visitU8(v: UByte): SerdeResult<UByte> = SerdeResult.success(v)

    override fun visitI8(v: Byte): SerdeResult<UByte> = if (v >= 0) SerdeResult.success(v.toUByte()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): SerdeResult<UByte> =
        if (v in 0..UByte.MAX_VALUE.toInt()) SerdeResult.success(v.toUByte()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): SerdeResult<UByte> =
        if (v in 0..UByte.MAX_VALUE.toInt()) SerdeResult.success(v.toUByte()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): SerdeResult<UByte> =
        if (v in 0L..UByte.MAX_VALUE.toLong()) SerdeResult.success(v.toUByte()) else invalidSigned(v, this)

    override fun visitU16(v: UShort): SerdeResult<UByte> =
        if (v <= UByte.MAX_VALUE.toUShort()) SerdeResult.success(v.toUByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU32(v: UInt): SerdeResult<UByte> =
        if (unsignedLessOrEqual(v, UByte.MAX_VALUE.toUInt())) SerdeResult.success(v.toUByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): SerdeResult<UByte> =
        if (unsignedLessOrEqual(v, UByte.MAX_VALUE.toULong())) SerdeResult.success(v.toUByte()) else invalidUnsigned(v, this)
}

data object U8Deserialize : Deserialize<UByte> {
    override fun <D> deserialize(deserializer: D): SerdeResult<UByte>
        where D : Deserializer = deserializer.deserializeU8(U8Visitor)
}

private data object U16Visitor : Visitor<UShort> {
    override fun expecting(): String = "u16"

    override fun visitU16(v: UShort): SerdeResult<UShort> = SerdeResult.success(v)

    override fun visitU8(v: UByte): SerdeResult<UShort> = SerdeResult.success(v.toUShort())

    override fun visitI8(v: Byte): SerdeResult<UShort> = if (v >= 0) SerdeResult.success(v.toUShort()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): SerdeResult<UShort> =
        if (v >=
            0
        ) {
            SerdeResult.success(v.toUShort())
        } else {
            invalidSigned(v.toLong(), this)
        }

    override fun visitI32(v: Int): SerdeResult<UShort> =
        if (v in 0..UShort.MAX_VALUE.toInt()) SerdeResult.success(v.toUShort()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): SerdeResult<UShort> =
        if (v in 0L..UShort.MAX_VALUE.toLong()) SerdeResult.success(v.toUShort()) else invalidSigned(v, this)

    override fun visitU32(v: UInt): SerdeResult<UShort> =
        if (unsignedLessOrEqual(v, UShort.MAX_VALUE.toUInt())) SerdeResult.success(v.toUShort()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): SerdeResult<UShort> =
        if (unsignedLessOrEqual(v, UShort.MAX_VALUE.toULong())) SerdeResult.success(v.toUShort()) else invalidUnsigned(v, this)
}

data object U16Deserialize : Deserialize<UShort> {
    override fun <D> deserialize(deserializer: D): SerdeResult<UShort>
        where D : Deserializer =
        deserializer.deserializeU16(U16Visitor)
}

private data object U32Visitor : Visitor<UInt> {
    override fun expecting(): String = "u32"

    override fun visitU32(v: UInt): SerdeResult<UInt> = SerdeResult.success(v)

    override fun visitU8(v: UByte): SerdeResult<UInt> = SerdeResult.success(v.toUInt())

    override fun visitU16(v: UShort): SerdeResult<UInt> = SerdeResult.success(v.toUInt())

    override fun visitI8(v: Byte): SerdeResult<UInt> = if (v >= 0) SerdeResult.success(v.toUInt()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): SerdeResult<UInt> = if (v >= 0) SerdeResult.success(v.toUInt()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): SerdeResult<UInt> = if (v >= 0) SerdeResult.success(v.toUInt()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): SerdeResult<UInt> =
        if (v in 0L..UInt.MAX_VALUE.toLong()) SerdeResult.success(v.toUInt()) else invalidSigned(v, this)

    override fun visitU64(v: ULong): SerdeResult<UInt> =
        if (unsignedLessOrEqual(v, UInt.MAX_VALUE.toULong())) SerdeResult.success(v.toUInt()) else invalidUnsigned(v, this)
}

data object U32Deserialize : Deserialize<UInt> {
    override fun <D> deserialize(deserializer: D): SerdeResult<UInt>
        where D : Deserializer =
        deserializer.deserializeU32(U32Visitor)
}

private data object U64Visitor : Visitor<ULong> {
    override fun expecting(): String = "u64"

    override fun visitU64(v: ULong): SerdeResult<ULong> = SerdeResult.success(v)

    override fun visitU8(v: UByte): SerdeResult<ULong> = SerdeResult.success(v.toULong())

    override fun visitU16(v: UShort): SerdeResult<ULong> = SerdeResult.success(v.toULong())

    override fun visitU32(v: UInt): SerdeResult<ULong> = SerdeResult.success(v.toULong())

    override fun visitI8(v: Byte): SerdeResult<ULong> = if (v >= 0) SerdeResult.success(v.toULong()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): SerdeResult<ULong> = if (v >= 0) SerdeResult.success(v.toULong()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): SerdeResult<ULong> = if (v >= 0) SerdeResult.success(v.toULong()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): SerdeResult<ULong> = if (v >= 0) SerdeResult.success(v.toULong()) else invalidSigned(v, this)
}

data object U64Deserialize : Deserialize<ULong> {
    override fun <D> deserialize(deserializer: D): SerdeResult<ULong>
        where D : Deserializer =
        deserializer.deserializeU64(U64Visitor)
}

private data object F32Visitor : Visitor<Float> {
    override fun expecting(): String = "f32"

    override fun visitF32(v: Float): SerdeResult<Float> = SerdeResult.success(v)

    override fun visitF64(v: Double): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitI8(v: Byte): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitI16(v: Short): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitI32(v: Int): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitI64(v: Long): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitU8(v: UByte): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitU16(v: UShort): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitU32(v: UInt): SerdeResult<Float> = SerdeResult.success(v.toFloat())

    override fun visitU64(v: ULong): SerdeResult<Float> = SerdeResult.success(v.toFloat())
}

data object F32Deserialize : Deserialize<Float> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Float>
        where D : Deserializer =
        deserializer.deserializeF32(F32Visitor)
}

private data object F64Visitor : Visitor<Double> {
    override fun expecting(): String = "f64"

    override fun visitF64(v: Double): SerdeResult<Double> = SerdeResult.success(v)

    override fun visitF32(v: Float): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitI8(v: Byte): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitI16(v: Short): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitI32(v: Int): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitI64(v: Long): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitU8(v: UByte): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitU16(v: UShort): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitU32(v: UInt): SerdeResult<Double> = SerdeResult.success(v.toDouble())

    override fun visitU64(v: ULong): SerdeResult<Double> = SerdeResult.success(v.toDouble())
}

data object F64Deserialize : Deserialize<Double> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Double>
        where D : Deserializer =
        deserializer.deserializeF64(F64Visitor)
}

private const val I128_MAX_AS_U128: String = "170141183460469231731687303715884105727"

private fun compareUnsignedDecimal(
    a: String,
    b: String,
): Int = if (a.length != b.length) a.length - b.length else a.compareTo(b)

private fun invalidOther(
    label: String,
    exp: Expected,
): SerdeResult<Nothing> = SerdeResult.failure(SerdeError.invalidValue(Unexpected.Other(label), exp))

private data object I128Visitor : Visitor<String> {
    override fun expecting(): String = "i128"

    override fun visitI128(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitI8(v: Byte): SerdeResult<String> = SerdeResult.success(v.toLong().toString())

    override fun visitI16(v: Short): SerdeResult<String> = SerdeResult.success(v.toLong().toString())

    override fun visitI32(v: Int): SerdeResult<String> = SerdeResult.success(v.toLong().toString())

    override fun visitI64(v: Long): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun visitU8(v: UByte): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v.toULong()))

    override fun visitU16(v: UShort): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v.toULong()))

    override fun visitU32(v: UInt): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v.toULong()))

    override fun visitU64(v: ULong): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v))

    override fun visitU128(v: String): SerdeResult<String> =
        if (compareUnsignedDecimal(v, I128_MAX_AS_U128) <= 0) SerdeResult.success(v) else invalidOther("u128", this)
}

data object I128Deserialize : Deserialize<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeI128(I128Visitor)
}

private data object U128Visitor : Visitor<String> {
    override fun expecting(): String = "u128"

    override fun visitU128(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitU8(v: UByte): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v.toULong()))

    override fun visitU16(v: UShort): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v.toULong()))

    override fun visitU32(v: UInt): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v.toULong()))

    override fun visitU64(v: ULong): SerdeResult<String> = SerdeResult.success(unsignedDecimal(v))

    override fun visitI8(v: Byte): SerdeResult<String> =
        if (v >=
            0
        ) {
            SerdeResult.success(v.toLong().toString())
        } else {
            invalidSigned(v.toLong(), this)
        }

    override fun visitI16(v: Short): SerdeResult<String> =
        if (v >=
            0
        ) {
            SerdeResult.success(v.toLong().toString())
        } else {
            invalidSigned(v.toLong(), this)
        }

    override fun visitI32(v: Int): SerdeResult<String> =
        if (v >=
            0
        ) {
            SerdeResult.success(v.toLong().toString())
        } else {
            invalidSigned(v.toLong(), this)
        }

    override fun visitI64(v: Long): SerdeResult<String> = if (v >= 0) SerdeResult.success(v.toString()) else invalidSigned(v, this)

    override fun visitI128(v: String): SerdeResult<String> = if (!v.startsWith('-')) SerdeResult.success(v) else invalidOther("i128", this)
}

data object U128Deserialize : Deserialize<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeU128(U128Visitor)
}

// //////////////////////////////////////////////////////////////////////////////

private data object CharVisitor : Visitor<Char> {
    override fun expecting(): String = "a character"

    override fun visitChar(v: Char): SerdeResult<Char> = SerdeResult.success(v)

    override fun visitStr(v: String): SerdeResult<Char> =
        if (v.length == 1) SerdeResult.success(v[0]) else SerdeResult.failure(SerdeError.invalidValue(Unexpected.Str(v), this))
}

data object CharDeserialize : Deserialize<Char> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Char>
        where D : Deserializer =
        deserializer.deserializeChar(CharVisitor)
}

// //////////////////////////////////////////////////////////////////////////////

private data object StringVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitString(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitBytes(v: ByteArray): SerdeResult<String> =
        serdeCatching { v.decodeToString(throwOnInvalidSequence = true) }
            .recoverCatching { throw SerdeException(SerdeError.invalidValue(Unexpected.Bytes(v), this)) }

    override fun visitByteBuf(v: ByteArray): SerdeResult<String> = visitBytes(v)
}

private class StringInPlaceVisitor(
    private val place: (String) -> Unit,
) : Visitor<Unit> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): SerdeResult<Unit> {
        place(v)
        return SerdeResult.success(Unit)
    }

    override fun visitString(v: String): SerdeResult<Unit> {
        place(v)
        return SerdeResult.success(Unit)
    }

    override fun visitBytes(v: ByteArray): SerdeResult<Unit> =
        serdeCatching {
            place(v.decodeToString(throwOnInvalidSequence = true))
        }.recoverCatching {
            throw SerdeException(SerdeError.invalidValue(Unexpected.Bytes(v), this))
        }

    override fun visitByteBuf(v: ByteArray): SerdeResult<Unit> = visitBytes(v)
}

data object StringDeserialize : Deserialize<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeString(StringVisitor)

    override fun <D> deserializeInPlace(
        deserializer: D,
        place: (String) -> Unit,
    ): SerdeResult<Unit>
        where D : Deserializer =
        deserializer.deserializeString(StringInPlaceVisitor(place))
}

// //////////////////////////////////////////////////////////////////////////////

private data object StrVisitor : Visitor<String> {
    override fun expecting(): String = "a borrowed string"

    override fun visitBorrowedStr(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitBorrowedBytes(v: ByteArray): SerdeResult<String> =
        serdeCatching { v.decodeToString(throwOnInvalidSequence = true) }
            .recoverCatching { throw SerdeException(SerdeError.invalidValue(Unexpected.Bytes(v), this)) }
}

data object BorrowedStrDeserialize : Deserialize<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeStr(StrVisitor)
}

// //////////////////////////////////////////////////////////////////////////////

private data object BytesVisitor : Visitor<ByteArray> {
    override fun expecting(): String = "a borrowed byte array"

    override fun visitBorrowedBytes(v: ByteArray): SerdeResult<ByteArray> = SerdeResult.success(v)

    override fun visitBorrowedStr(v: String): SerdeResult<ByteArray> = SerdeResult.success(v.encodeToByteArray())
}

data object BorrowedBytesDeserialize : Deserialize<ByteArray> {
    override fun <D> deserialize(deserializer: D): SerdeResult<ByteArray>
        where D : Deserializer =
        deserializer.deserializeBytes(BytesVisitor)
}

// //////////////////////////////////////////////////////////////////////////////

class CStringValue(
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is CStringValue && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = bytes.decodeToString()
}

private data object CStringVisitor : Visitor<CStringValue> {
    override fun expecting(): String = "byte array"

    override fun <A> visitSeq(access: A): SerdeResult<CStringValue>
        where A : SeqAccess =
        serdeCatching {
            val values = mutableListOf<Byte>()
            while (true) {
                val value = access.nextElementSeed(SeedFromDeserialize(U8Deserialize)).getOrThrow() ?: break
                values += value.toByte()
            }
            cStringFromBytes(values.toByteArray())
        }

    override fun visitBytes(v: ByteArray): SerdeResult<CStringValue> = serdeCatching { cStringFromBytes(v) }

    override fun visitByteBuf(v: ByteArray): SerdeResult<CStringValue> = visitBytes(v)

    override fun visitStr(v: String): SerdeResult<CStringValue> = serdeCatching { cStringFromBytes(v.encodeToByteArray()) }

    override fun visitString(v: String): SerdeResult<CStringValue> = visitStr(v)
}

data object CStringDeserialize : Deserialize<CStringValue> {
    override fun <D> deserialize(deserializer: D): SerdeResult<CStringValue>
        where D : Deserializer =
        deserializer.deserializeByteBuf(CStringVisitor)
}

private fun cStringFromBytes(bytes: ByteArray): CStringValue {
    val nulIndex = bytes.indexOf(0)
    if (nulIndex >= 0) {
        throw SerdeException(SerdeError.custom("nul byte found in provided data at position: $nulIndex"))
    }
    return CStringValue(bytes.copyOf())
}

// //////////////////////////////////////////////////////////////////////////////

private class OptionVisitor<T>(
    private val valueDeserialize: Deserialize<T>,
) : Visitor<T?> {
    override fun expecting(): String = "option"

    override fun visitUnit(): SerdeResult<T?> = SerdeResult.success(null)

    override fun visitNone(): SerdeResult<T?> = SerdeResult.success(null)

    override fun <D> visitSome(deserializer: D): SerdeResult<T?>
        where D : Deserializer =
        valueDeserialize.deserialize(deserializer).map { it }

    override fun <D> privateVisitUntaggedOption(deserializer: D): SerdeResult<T?>
        where D : Deserializer =
        SerdeResult.success(valueDeserialize.deserialize(deserializer).getOrNull())
}

fun <T> nullableDeserialize(valueDeserialize: Deserialize<T>): Deserialize<T?> =
    object : Deserialize<T?> {
        override fun <D> deserialize(deserializer: D): SerdeResult<T?>
            where D : Deserializer =
            deserializer.deserializeOption(OptionVisitor(valueDeserialize))
    }

// //////////////////////////////////////////////////////////////////////////////

private class SeedFromDeserialize<T>(
    private val deserialize: Deserialize<T>,
) : DeserializeSeed<T> {
    override fun <D> deserialize(deserializer: D): SerdeResult<T>
        where D : Deserializer = deserialize.deserialize(deserializer)
}

fun <T> mutableListDeserialize(elementDeserialize: Deserialize<T>): Deserialize<MutableList<T>> =
    object : Deserialize<MutableList<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<MutableList<T>>
            where D : Deserializer =
            deserializer.deserializeSeq(
                object : Visitor<MutableList<T>> {
                    override fun expecting(): String = "a sequence"

                    override fun <A> visitSeq(access: A): SerdeResult<MutableList<T>>
                        where A : SeqAccess =
                        serdeCatching {
                            val hint = access.sizeHint() ?: 0
                            val values = ArrayList<T>(hint)
                            val seed = SeedFromDeserialize(elementDeserialize)
                            while (true) {
                                val next = access.nextElementSeed(seed).getOrThrow() ?: break
                                values.add(next)
                            }
                            values
                        }
                },
            )
    }

// //////////////////////////////////////////////////////////////////////////////

fun <T> mutableSetDeserialize(elementDeserialize: Deserialize<T>): Deserialize<MutableSet<T>> =
    object : Deserialize<MutableSet<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<MutableSet<T>>
            where D : Deserializer =
            deserializer.deserializeSeq(
                object : Visitor<MutableSet<T>> {
                    override fun expecting(): String = "a sequence"

                    override fun <A> visitSeq(access: A): SerdeResult<MutableSet<T>>
                        where A : SeqAccess =
                        serdeCatching {
                            val hint = access.sizeHint() ?: 0
                            val values = LinkedHashSet<T>(hint)
                            val seed = SeedFromDeserialize(elementDeserialize)
                            while (true) {
                                val next = access.nextElementSeed(seed).getOrThrow() ?: break
                                values.add(next)
                            }
                            values
                        }
                },
            )
    }

fun <T> setDeserialize(elementDeserialize: Deserialize<T>): Deserialize<Set<T>> =
    object : Deserialize<Set<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Set<T>>
            where D : Deserializer =
            mutableSetDeserialize(elementDeserialize).deserialize(deserializer)
    }

// //////////////////////////////////////////////////////////////////////////////

fun <K, V> mutableMapDeserialize(
    keyDeserialize: Deserialize<K>,
    valueDeserialize: Deserialize<V>,
): Deserialize<MutableMap<K, V>> =
    object : Deserialize<MutableMap<K, V>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<MutableMap<K, V>>
            where D : Deserializer =
            deserializer.deserializeMap(
                object : Visitor<MutableMap<K, V>> {
                    override fun expecting(): String = "a map"

                    override fun <A> visitMap(access: A): SerdeResult<MutableMap<K, V>>
                        where A : MapAccess =
                        serdeCatching {
                            val hint = access.sizeHint() ?: 0
                            val values = LinkedHashMap<K, V>(hint)
                            val keySeed = SeedFromDeserialize(keyDeserialize)
                            val valueSeed = SeedFromDeserialize(valueDeserialize)
                            while (true) {
                                val entry = access.nextEntrySeed(keySeed, valueSeed).getOrThrow() ?: break
                                values[entry.first] = entry.second
                            }
                            values
                        }
                },
            )
    }

fun <K, V> mapDeserialize(
    keyDeserialize: Deserialize<K>,
    valueDeserialize: Deserialize<V>,
): Deserialize<Map<K, V>> =
    object : Deserialize<Map<K, V>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Map<K, V>>
            where D : Deserializer =
            mutableMapDeserialize(keyDeserialize, valueDeserialize).deserialize(deserializer)
    }

// //////////////////////////////////////////////////////////////////////////////

fun <T0, T1> pairDeserialize(
    firstDeserialize: Deserialize<T0>,
    secondDeserialize: Deserialize<T1>,
): Deserialize<Pair<T0, T1>> =
    object : Deserialize<Pair<T0, T1>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Pair<T0, T1>>
            where D : Deserializer =
            deserializer.deserializeTuple(
                2,
                object : Visitor<Pair<T0, T1>> {
                    override fun expecting(): String = "a tuple of size 2"

                    override fun <A> visitSeq(access: A): SerdeResult<Pair<T0, T1>>
                        where A : SeqAccess =
                        serdeCatching {
                            val first =
                                access.nextElementSeed(SeedFromDeserialize(firstDeserialize)).getOrThrow()
                                    ?: throw SerdeException(SerdeError.invalidLength(0, this))
                            val second =
                                access.nextElementSeed(SeedFromDeserialize(secondDeserialize)).getOrThrow()
                                    ?: throw SerdeException(SerdeError.invalidLength(1, this))
                            first to second
                        }
                },
            )
    }

fun <T0, T1, T2> tripleDeserialize(
    firstDeserialize: Deserialize<T0>,
    secondDeserialize: Deserialize<T1>,
    thirdDeserialize: Deserialize<T2>,
): Deserialize<Triple<T0, T1, T2>> =
    object : Deserialize<Triple<T0, T1, T2>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Triple<T0, T1, T2>>
            where D : Deserializer =
            deserializer.deserializeTuple(
                3,
                object : Visitor<Triple<T0, T1, T2>> {
                    override fun expecting(): String = "a tuple of size 3"

                    override fun <A> visitSeq(access: A): SerdeResult<Triple<T0, T1, T2>>
                        where A : SeqAccess =
                        serdeCatching {
                            val first =
                                access.nextElementSeed(SeedFromDeserialize(firstDeserialize)).getOrThrow()
                                    ?: throw SerdeException(SerdeError.invalidLength(0, this))
                            val second =
                                access.nextElementSeed(SeedFromDeserialize(secondDeserialize)).getOrThrow()
                                    ?: throw SerdeException(SerdeError.invalidLength(1, this))
                            val third =
                                access.nextElementSeed(SeedFromDeserialize(thirdDeserialize)).getOrThrow()
                                    ?: throw SerdeException(SerdeError.invalidLength(2, this))
                            Triple(first, second, third)
                        }
                },
            )
    }

// //////////////////////////////////////////////////////////////////////////////

data class RangeValue<T>(
    val start: T,
    val end: T,
)

data class RangeInclusiveValue<T>(
    val start: T,
    val end: T,
)

data class RangeFromValue<T>(
    val start: T,
)

data class RangeToValue<T>(
    val end: T,
)

fun <T> rangeDeserialize(valueDeserialize: Deserialize<T>): Deserialize<RangeValue<T>> =
    object : Deserialize<RangeValue<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<RangeValue<T>>
            where D : Deserializer =
            deserializer.deserializeStruct(
                "Range",
                listOf("start", "end"),
                RangeVisitor("struct Range", valueDeserialize) { start, end -> RangeValue(start, end) },
            )
    }

fun <T> rangeInclusiveDeserialize(valueDeserialize: Deserialize<T>): Deserialize<RangeInclusiveValue<T>> =
    object : Deserialize<RangeInclusiveValue<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<RangeInclusiveValue<T>>
            where D : Deserializer =
            deserializer.deserializeStruct(
                "RangeInclusive",
                listOf("start", "end"),
                RangeVisitor("struct RangeInclusive", valueDeserialize) { start, end -> RangeInclusiveValue(start, end) },
            )
    }

fun <T> rangeFromDeserialize(valueDeserialize: Deserialize<T>): Deserialize<RangeFromValue<T>> =
    object : Deserialize<RangeFromValue<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<RangeFromValue<T>>
            where D : Deserializer =
            deserializer.deserializeStruct(
                "RangeFrom",
                listOf("start"),
                RangeFromVisitor("struct RangeFrom", valueDeserialize),
            )
    }

fun <T> rangeToDeserialize(valueDeserialize: Deserialize<T>): Deserialize<RangeToValue<T>> =
    object : Deserialize<RangeToValue<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<RangeToValue<T>>
            where D : Deserializer =
            deserializer.deserializeStruct(
                "RangeTo",
                listOf("end"),
                RangeToVisitor("struct RangeTo", valueDeserialize),
            )
    }

private class RangeVisitor<T, R>(
    private val expecting: String,
    private val valueDeserialize: Deserialize<T>,
    private val build: (T, T) -> R,
) : Visitor<R> {
    private data class Slot<T>(
        val value: T,
    )

    override fun expecting(): String = expecting

    override fun <A> visitSeq(access: A): SerdeResult<R>
        where A : SeqAccess =
        serdeCatching {
            val seed = SeedFromDeserialize(valueDeserialize)
            val start =
                access.nextElementSeed(seed).getOrThrow()
                    ?: throw SerdeException(SerdeError.invalidLength(0, this))
            val end =
                access.nextElementSeed(seed).getOrThrow()
                    ?: throw SerdeException(SerdeError.invalidLength(1, this))
            build(start, end)
        }

    override fun <A> visitMap(access: A): SerdeResult<R>
        where A : MapAccess =
        serdeCatching {
            var start: Slot<T>? = null
            var end: Slot<T>? = null
            val fieldSeed =
                SeedFromDeserialize(
                    fieldIdentifierDeserialize(
                        expectingMessage = "`start` or `end`",
                        fields = listOf("start", "end"),
                    ),
                )
            val valueSeed = SeedFromDeserialize(valueDeserialize)
            while (true) {
                val key = access.nextKeySeed(fieldSeed).getOrThrow() ?: break
                when (key) {
                    "start" -> {
                        if (start != null) throw SerdeException(SerdeError.duplicateField("start"))
                        start = Slot(access.nextValueSeed(valueSeed).getOrThrow())
                    }
                    "end" -> {
                        if (end != null) throw SerdeException(SerdeError.duplicateField("end"))
                        end = Slot(access.nextValueSeed(valueSeed).getOrThrow())
                    }
                    else -> throw SerdeException(SerdeError.unknownField(key, listOf("start", "end")))
                }
            }
            val startValue = start ?: throw SerdeException(SerdeError.missingField("start"))
            val endValue = end ?: throw SerdeException(SerdeError.missingField("end"))
            build(startValue.value, endValue.value)
        }
}

private class RangeFromVisitor<T>(
    private val expecting: String,
    private val valueDeserialize: Deserialize<T>,
) : Visitor<RangeFromValue<T>> {
    private data class Slot<T>(
        val value: T,
    )

    override fun expecting(): String = expecting

    override fun <A> visitSeq(access: A): SerdeResult<RangeFromValue<T>>
        where A : SeqAccess =
        serdeCatching {
            val start =
                access.nextElementSeed(SeedFromDeserialize(valueDeserialize)).getOrThrow()
                    ?: throw SerdeException(SerdeError.invalidLength(0, this))
            RangeFromValue(start)
        }

    override fun <A> visitMap(access: A): SerdeResult<RangeFromValue<T>>
        where A : MapAccess =
        serdeCatching {
            var start: Slot<T>? = null
            val fieldSeed =
                SeedFromDeserialize(
                    fieldIdentifierDeserialize(
                        expectingMessage = "`start`",
                        fields = listOf("start"),
                    ),
                )
            val valueSeed = SeedFromDeserialize(valueDeserialize)
            while (true) {
                val key = access.nextKeySeed(fieldSeed).getOrThrow() ?: break
                when (key) {
                    "start" -> {
                        if (start != null) throw SerdeException(SerdeError.duplicateField("start"))
                        start = Slot(access.nextValueSeed(valueSeed).getOrThrow())
                    }
                    else -> throw SerdeException(SerdeError.unknownField(key, listOf("start")))
                }
            }
            val startValue = start ?: throw SerdeException(SerdeError.missingField("start"))
            RangeFromValue(startValue.value)
        }
}

private class RangeToVisitor<T>(
    private val expecting: String,
    private val valueDeserialize: Deserialize<T>,
) : Visitor<RangeToValue<T>> {
    private data class Slot<T>(
        val value: T,
    )

    override fun expecting(): String = expecting

    override fun <A> visitSeq(access: A): SerdeResult<RangeToValue<T>>
        where A : SeqAccess =
        serdeCatching {
            val end =
                access.nextElementSeed(SeedFromDeserialize(valueDeserialize)).getOrThrow()
                    ?: throw SerdeException(SerdeError.invalidLength(0, this))
            RangeToValue(end)
        }

    override fun <A> visitMap(access: A): SerdeResult<RangeToValue<T>>
        where A : MapAccess =
        serdeCatching {
            var end: Slot<T>? = null
            val fieldSeed =
                SeedFromDeserialize(
                    fieldIdentifierDeserialize(
                        expectingMessage = "`end`",
                        fields = listOf("end"),
                    ),
                )
            val valueSeed = SeedFromDeserialize(valueDeserialize)
            while (true) {
                val key = access.nextKeySeed(fieldSeed).getOrThrow() ?: break
                when (key) {
                    "end" -> {
                        if (end != null) throw SerdeException(SerdeError.duplicateField("end"))
                        end = Slot(access.nextValueSeed(valueSeed).getOrThrow())
                    }
                    else -> throw SerdeException(SerdeError.unknownField(key, listOf("end")))
                }
            }
            val endValue = end ?: throw SerdeException(SerdeError.missingField("end"))
            RangeToValue(endValue.value)
        }
}

// //////////////////////////////////////////////////////////////////////////////

data object DurationDeserialize : Deserialize<Duration> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Duration>
        where D : Deserializer =
        deserializer.deserializeStruct(
            "Duration",
            listOf("secs", "nanos"),
            object : Visitor<Duration> {
                override fun expecting(): String = "struct Duration"

                override fun <A> visitSeq(access: A): SerdeResult<Duration>
                    where A : SeqAccess =
                    serdeCatching {
                        val secs =
                            access.nextElementSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                                ?: throw SerdeException(SerdeError.invalidLength(0, this))
                        val nanos =
                            access.nextElementSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                                ?: throw SerdeException(SerdeError.invalidLength(1, this))
                        durationFromParts(secs, nanos)
                    }

                override fun <A> visitMap(access: A): SerdeResult<Duration>
                    where A : MapAccess =
                    serdeCatching {
                        var secs: ULong? = null
                        var nanos: UInt? = null
                        val fieldSeed =
                            SeedFromDeserialize(
                                fieldIdentifierDeserialize(
                                    expectingMessage = "`secs` or `nanos`",
                                    fields = listOf("secs", "nanos"),
                                ),
                            )
                        while (true) {
                            val key = access.nextKeySeed(fieldSeed).getOrThrow() ?: break
                            when (key) {
                                "secs" -> {
                                    if (secs != null) throw SerdeException(SerdeError.duplicateField("secs"))
                                    secs = access.nextValueSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                                }
                                "nanos" -> {
                                    if (nanos != null) throw SerdeException(SerdeError.duplicateField("nanos"))
                                    nanos = access.nextValueSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                                }
                                else -> throw SerdeException(SerdeError.unknownField(key, listOf("secs", "nanos")))
                            }
                        }
                        val s = secs ?: throw SerdeException(SerdeError.missingField("secs"))
                        val n = nanos ?: throw SerdeException(SerdeError.missingField("nanos"))
                        durationFromParts(s, n)
                    }
            },
        )
}

private fun checkUnsignedSecondOverflow(
    secs: ULong,
    nanos: UInt,
    message: String,
) {
    val carry = (nanos.toLong() / 1_000_000_000L).toULong()
    if (!unsignedLessOrEqual(secs, ULong.MAX_VALUE - carry)) {
        throw SerdeException(SerdeError.custom(message))
    }
}

private fun durationFromParts(
    secs: ULong,
    nanos: UInt,
): Duration {
    checkUnsignedSecondOverflow(secs, nanos, "overflow deserializing Duration")
    if (!unsignedLessOrEqual(secs, Long.MAX_VALUE.toULong())) {
        throw SerdeException(SerdeError.custom("overflow deserializing Duration"))
    }
    return secs.toLong().seconds + nanos.toLong().nanoseconds
}

private fun instantFromEpochParts(
    secs: ULong,
    nanos: UInt,
): Instant {
    checkUnsignedSecondOverflow(secs, nanos, "overflow deserializing SystemTime epoch offset")
    if (!unsignedLessOrEqual(secs, Long.MAX_VALUE.toULong())) {
        throw SerdeException(SerdeError.custom("overflow deserializing SystemTime"))
    }
    return try {
        Instant.fromEpochSeconds(secs.toLong(), nanos.toInt())
    } catch (_: IllegalArgumentException) {
        throw SerdeException(SerdeError.custom("overflow deserializing SystemTime"))
    }
}

// //////////////////////////////////////////////////////////////////////////////

data object SystemTimeDeserialize : Deserialize<Instant> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Instant>
        where D : Deserializer =
        deserializer.deserializeStruct(
            "SystemTime",
            listOf("secs_since_epoch", "nanos_since_epoch"),
            object : Visitor<Instant> {
                override fun expecting(): String = "struct SystemTime"

                override fun <A> visitSeq(access: A): SerdeResult<Instant>
                    where A : SeqAccess =
                    serdeCatching {
                        val secs =
                            access.nextElementSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                                ?: throw SerdeException(SerdeError.invalidLength(0, this))
                        val nanos =
                            access.nextElementSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                                ?: throw SerdeException(SerdeError.invalidLength(1, this))
                        instantFromEpochParts(secs, nanos)
                    }

                override fun <A> visitMap(access: A): SerdeResult<Instant>
                    where A : MapAccess =
                    serdeCatching {
                        var secs: ULong? = null
                        var nanos: UInt? = null
                        val fieldSeed =
                            SeedFromDeserialize(
                                fieldIdentifierDeserialize(
                                    expectingMessage = "`secs_since_epoch` or `nanos_since_epoch`",
                                    fields = listOf("secs_since_epoch", "nanos_since_epoch"),
                                ),
                            )
                        while (true) {
                            val key = access.nextKeySeed(fieldSeed).getOrThrow() ?: break
                            when (key) {
                                "secs_since_epoch" -> {
                                    if (secs != null) throw SerdeException(SerdeError.duplicateField("secs_since_epoch"))
                                    secs = access.nextValueSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                                }
                                "nanos_since_epoch" -> {
                                    if (nanos != null) throw SerdeException(SerdeError.duplicateField("nanos_since_epoch"))
                                    nanos = access.nextValueSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                                }
                                else -> throw SerdeException(
                                    SerdeError.unknownField(
                                        key,
                                        listOf("secs_since_epoch", "nanos_since_epoch"),
                                    ),
                                )
                            }
                        }
                        val s = secs ?: throw SerdeException(SerdeError.missingField("secs_since_epoch"))
                        val n = nanos ?: throw SerdeException(SerdeError.missingField("nanos_since_epoch"))
                        instantFromEpochParts(s, n)
                    }
            },
        )
}

// //////////////////////////////////////////////////////////////////////////////

sealed class ResultValue<out T, out E> {
    data class Ok<T>(
        val value: T,
    ) : ResultValue<T, Nothing>()

    data class Err<E>(
        val error: E,
    ) : ResultValue<Nothing, E>()
}

fun <T, E> resultDeserialize(
    okDeserialize: Deserialize<T>,
    errDeserialize: Deserialize<E>,
): Deserialize<ResultValue<T, E>> =
    object : Deserialize<ResultValue<T, E>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<ResultValue<T, E>>
            where D : Deserializer =
            deserializer.deserializeEnum(
                "Result",
                listOf("Ok", "Err"),
                ResultVisitor(okDeserialize, errDeserialize),
            )
    }

private class ResultVisitor<T, E>(
    private val okDeserialize: Deserialize<T>,
    private val errDeserialize: Deserialize<E>,
) : Visitor<ResultValue<T, E>> {
    override fun expecting(): String = "enum Result"

    override fun <A> visitEnum(access: A): SerdeResult<ResultValue<T, E>>
        where A : EnumAccess =
        serdeCatching {
            val fieldSeed =
                SeedFromDeserialize(
                    fieldIdentifierDeserialize(
                        expectingMessage = "`Ok` or `Err`",
                        fields = listOf("Ok", "Err"),
                    ),
                )
            val (field, variant) = access.variantSeed(fieldSeed).getOrThrow()
            when (field) {
                "Ok" -> ResultValue.Ok(variant.newtypeVariant(SeedFromDeserialize(okDeserialize)).getOrThrow())
                "Err" -> ResultValue.Err(variant.newtypeVariant(SeedFromDeserialize(errDeserialize)).getOrThrow())
                else -> throw SerdeException(SerdeError.unknownVariant(field, listOf("Ok", "Err")))
            }
        }
}

// //////////////////////////////////////////////////////////////////////////////

data class PathValue(
    val value: String,
)

private data object PathVisitor : Visitor<PathValue> {
    override fun expecting(): String = "a borrowed path"

    override fun visitBorrowedStr(v: String): SerdeResult<PathValue> = SerdeResult.success(PathValue(v))

    override fun visitBorrowedBytes(v: ByteArray): SerdeResult<PathValue> =
        serdeCatching { PathValue(v.decodeToString(throwOnInvalidSequence = true)) }
            .recoverCatching { throw SerdeException(SerdeError.invalidValue(Unexpected.Bytes(v), this)) }
}

data object PathDeserialize : Deserialize<PathValue> {
    override fun <D> deserialize(deserializer: D): SerdeResult<PathValue>
        where D : Deserializer =
        deserializer.deserializeStr(PathVisitor)
}

private data object PathBufVisitor : Visitor<PathValue> {
    override fun expecting(): String = "path string"

    override fun visitStr(v: String): SerdeResult<PathValue> = SerdeResult.success(PathValue(v))

    override fun visitString(v: String): SerdeResult<PathValue> = SerdeResult.success(PathValue(v))

    override fun visitBytes(v: ByteArray): SerdeResult<PathValue> =
        serdeCatching { PathValue(v.decodeToString(throwOnInvalidSequence = true)) }
            .recoverCatching { throw SerdeException(SerdeError.invalidValue(Unexpected.Bytes(v), this)) }

    override fun visitByteBuf(v: ByteArray): SerdeResult<PathValue> = visitBytes(v)
}

data object PathBufDeserialize : Deserialize<PathValue> {
    override fun <D> deserialize(deserializer: D): SerdeResult<PathValue>
        where D : Deserializer =
        deserializer.deserializeString(PathBufVisitor)
}

data class BoxedPathValue(
    val path: PathValue,
)

data object BoxedPathDeserialize : Deserialize<BoxedPathValue> {
    override fun <D> deserialize(deserializer: D): SerdeResult<BoxedPathValue>
        where D : Deserializer =
        PathBufDeserialize.deserialize(deserializer).map(::BoxedPathValue)
}

// //////////////////////////////////////////////////////////////////////////////

sealed class BoundValue<out T> {
    data object Unbounded : BoundValue<Nothing>()

    data class Included<T>(
        val value: T,
    ) : BoundValue<T>()

    data class Excluded<T>(
        val value: T,
    ) : BoundValue<T>()
}

fun <T> boundDeserialize(valueDeserialize: Deserialize<T>): Deserialize<BoundValue<T>> =
    object : Deserialize<BoundValue<T>> {
        override fun <D> deserialize(deserializer: D): SerdeResult<BoundValue<T>>
            where D : Deserializer =
            deserializer.deserializeEnum(
                "Bound",
                listOf("Unbounded", "Included", "Excluded"),
                BoundVisitor(valueDeserialize),
            )
    }

private class BoundVisitor<T>(
    private val valueDeserialize: Deserialize<T>,
) : Visitor<BoundValue<T>> {
    override fun expecting(): String = "enum Bound"

    override fun <A> visitEnum(access: A): SerdeResult<BoundValue<T>>
        where A : EnumAccess =
        serdeCatching {
            val fieldSeed =
                SeedFromDeserialize(
                    fieldIdentifierDeserialize(
                        expectingMessage = "`Unbounded`, `Included` or `Excluded`",
                        fields = listOf("Unbounded", "Included", "Excluded"),
                    ),
                )
            val (field, variant) = access.variantSeed(fieldSeed).getOrThrow()
            when (field) {
                "Unbounded" -> {
                    variant.unitVariant().getOrThrow()
                    BoundValue.Unbounded
                }
                "Included" -> BoundValue.Included(variant.newtypeVariant(SeedFromDeserialize(valueDeserialize)).getOrThrow())
                "Excluded" -> BoundValue.Excluded(variant.newtypeVariant(SeedFromDeserialize(valueDeserialize)).getOrThrow())
                else -> throw SerdeException(SerdeError.unknownVariant(field, listOf("Unbounded", "Included", "Excluded")))
            }
        }
}

// //////////////////////////////////////////////////////////////////////////////

private fun fieldIdentifierDeserialize(
    expectingMessage: String,
    fields: List<String>,
): Deserialize<String> =
    object : Deserialize<String> {
        override fun <D> deserialize(deserializer: D): SerdeResult<String>
            where D : Deserializer =
            deserializer.deserializeIdentifier(
                object : Visitor<String> {
                    override fun expecting(): String = expectingMessage

                    override fun visitStr(v: String): SerdeResult<String> =
                        if (v in fields) SerdeResult.success(v) else SerdeResult.failure(SerdeError.unknownField(v, fields))

                    override fun visitBytes(v: ByteArray): SerdeResult<String> {
                        val s = v.decodeToString()
                        return if (s in fields) {
                            SerdeResult.success(s)
                        } else {
                            SerdeResult.failure(SerdeError.unknownField(fromUtf8LossyNoAlloc(v), fields))
                        }
                    }
                },
            )
    }
