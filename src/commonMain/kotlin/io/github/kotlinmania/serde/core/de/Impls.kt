// port-lint: source serde_core/src/de/impls.rs
package io.github.kotlinmania.serde.core.de

import io.github.kotlinmania.serde.core.`private`.fromUtf8LossyNoAlloc
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

////////////////////////////////////////////////////////////////////////////////

private data object UnitVisitor : Visitor<Unit> {
    override fun expecting(): String = "unit"

    override fun visitUnit(): Result<Unit> = Result.success(Unit)
}

public data object UnitDeserialize : Deserialize<Unit> {
    override fun <D> deserialize(deserializer: D): Result<Unit>
        where D : Deserializer =
        deserializer.deserializeUnit(UnitVisitor)
}

////////////////////////////////////////////////////////////////////////////////

private data object BoolVisitor : Visitor<Boolean> {
    override fun expecting(): String = "a boolean"

    override fun visitBool(v: Boolean): Result<Boolean> = Result.success(v)
}

public data object BooleanDeserialize : Deserialize<Boolean> {
    override fun <D> deserialize(deserializer: D): Result<Boolean>
        where D : Deserializer =
        deserializer.deserializeBool(BoolVisitor)
}

////////////////////////////////////////////////////////////////////////////////

private fun invalidSigned(v: Long, exp: Expected): Result<Nothing> =
    Result.failure(Error.invalidValue(Unexpected.Signed(v), exp))

private fun invalidUnsigned(v: ULong, exp: Expected): Result<Nothing> =
    Result.failure(Error.invalidValue(Unexpected.Unsigned(v), exp))

private data object I8Visitor : Visitor<Byte> {
    override fun expecting(): String = "i8"

    override fun visitI8(v: Byte): Result<Byte> = Result.success(v)

    override fun visitI16(v: Short): Result<Byte> =
        if (v in Byte.MIN_VALUE..Byte.MAX_VALUE) Result.success(v.toByte()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): Result<Byte> =
        if (v in Byte.MIN_VALUE..Byte.MAX_VALUE) Result.success(v.toByte()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): Result<Byte> =
        if (v in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()) Result.success(v.toByte()) else invalidSigned(v, this)

    override fun visitU8(v: UByte): Result<Byte> =
        if (v.toInt() <= Byte.MAX_VALUE) Result.success(v.toByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU16(v: UShort): Result<Byte> =
        if (v.toInt() <= Byte.MAX_VALUE) Result.success(v.toByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU32(v: UInt): Result<Byte> =
        if (v.toLong() <= Byte.MAX_VALUE.toLong()) Result.success(v.toByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): Result<Byte> =
        if (v <= Byte.MAX_VALUE.toULong()) Result.success(v.toByte()) else invalidUnsigned(v, this)
}

public data object I8Deserialize : Deserialize<Byte> {
    override fun <D> deserialize(deserializer: D): Result<Byte>
        where D : Deserializer =
        deserializer.deserializeI8(I8Visitor)
}

private data object I16Visitor : Visitor<Short> {
    override fun expecting(): String = "i16"

    override fun visitI16(v: Short): Result<Short> = Result.success(v)

    override fun visitI8(v: Byte): Result<Short> = Result.success(v.toShort())

    override fun visitI32(v: Int): Result<Short> =
        if (v in Short.MIN_VALUE..Short.MAX_VALUE) Result.success(v.toShort()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): Result<Short> =
        if (v in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()) Result.success(v.toShort()) else invalidSigned(v, this)

    override fun visitU8(v: UByte): Result<Short> = Result.success(v.toShort())

    override fun visitU16(v: UShort): Result<Short> =
        if (v.toInt() <= Short.MAX_VALUE) Result.success(v.toShort()) else invalidUnsigned(v.toULong(), this)

    override fun visitU32(v: UInt): Result<Short> =
        if (v.toLong() <= Short.MAX_VALUE.toLong()) Result.success(v.toShort()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): Result<Short> =
        if (v <= Short.MAX_VALUE.toULong()) Result.success(v.toShort()) else invalidUnsigned(v, this)
}

public data object I16Deserialize : Deserialize<Short> {
    override fun <D> deserialize(deserializer: D): Result<Short>
        where D : Deserializer =
        deserializer.deserializeI16(I16Visitor)
}

private data object I32Visitor : Visitor<Int> {
    override fun expecting(): String = "i32"

    override fun visitI32(v: Int): Result<Int> = Result.success(v)

    override fun visitI8(v: Byte): Result<Int> = Result.success(v.toInt())

    override fun visitI16(v: Short): Result<Int> = Result.success(v.toInt())

    override fun visitI64(v: Long): Result<Int> =
        if (v in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) Result.success(v.toInt()) else invalidSigned(v, this)

    override fun visitU8(v: UByte): Result<Int> = Result.success(v.toInt())

    override fun visitU16(v: UShort): Result<Int> = Result.success(v.toInt())

    override fun visitU32(v: UInt): Result<Int> =
        if (v <= Int.MAX_VALUE.toUInt()) Result.success(v.toInt()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): Result<Int> =
        if (v <= Int.MAX_VALUE.toULong()) Result.success(v.toInt()) else invalidUnsigned(v, this)
}

public data object I32Deserialize : Deserialize<Int> {
    override fun <D> deserialize(deserializer: D): Result<Int>
        where D : Deserializer =
        deserializer.deserializeI32(I32Visitor)
}

private data object I64Visitor : Visitor<Long> {
    override fun expecting(): String = "i64"

    override fun visitI64(v: Long): Result<Long> = Result.success(v)

    override fun visitI8(v: Byte): Result<Long> = Result.success(v.toLong())

    override fun visitI16(v: Short): Result<Long> = Result.success(v.toLong())

    override fun visitI32(v: Int): Result<Long> = Result.success(v.toLong())

    override fun visitU8(v: UByte): Result<Long> = Result.success(v.toLong())

    override fun visitU16(v: UShort): Result<Long> = Result.success(v.toLong())

    override fun visitU32(v: UInt): Result<Long> = Result.success(v.toLong())

    override fun visitU64(v: ULong): Result<Long> =
        if (v <= Long.MAX_VALUE.toULong()) Result.success(v.toLong()) else invalidUnsigned(v, this)
}

public data object I64Deserialize : Deserialize<Long> {
    override fun <D> deserialize(deserializer: D): Result<Long>
        where D : Deserializer =
        deserializer.deserializeI64(I64Visitor)
}

private data object U8Visitor : Visitor<UByte> {
    override fun expecting(): String = "u8"

    override fun visitU8(v: UByte): Result<UByte> = Result.success(v)

    override fun visitI8(v: Byte): Result<UByte> =
        if (v >= 0) Result.success(v.toUByte()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): Result<UByte> =
        if (v in 0..UByte.MAX_VALUE.toInt()) Result.success(v.toUByte()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): Result<UByte> =
        if (v in 0..UByte.MAX_VALUE.toInt()) Result.success(v.toUByte()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): Result<UByte> =
        if (v in 0L..UByte.MAX_VALUE.toLong()) Result.success(v.toUByte()) else invalidSigned(v, this)

    override fun visitU16(v: UShort): Result<UByte> =
        if (v <= UByte.MAX_VALUE.toUShort()) Result.success(v.toUByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU32(v: UInt): Result<UByte> =
        if (v <= UByte.MAX_VALUE.toUInt()) Result.success(v.toUByte()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): Result<UByte> =
        if (v <= UByte.MAX_VALUE.toULong()) Result.success(v.toUByte()) else invalidUnsigned(v, this)
}

public data object U8Deserialize : Deserialize<UByte> {
    override fun <D> deserialize(deserializer: D): Result<UByte>
        where D : Deserializer =
        deserializer.deserializeU8(U8Visitor)
}

private data object U16Visitor : Visitor<UShort> {
    override fun expecting(): String = "u16"

    override fun visitU16(v: UShort): Result<UShort> = Result.success(v)

    override fun visitU8(v: UByte): Result<UShort> = Result.success(v.toUShort())

    override fun visitI8(v: Byte): Result<UShort> =
        if (v >= 0) Result.success(v.toUShort()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): Result<UShort> =
        if (v >= 0) Result.success(v.toUShort()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): Result<UShort> =
        if (v in 0..UShort.MAX_VALUE.toInt()) Result.success(v.toUShort()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): Result<UShort> =
        if (v in 0L..UShort.MAX_VALUE.toLong()) Result.success(v.toUShort()) else invalidSigned(v, this)

    override fun visitU32(v: UInt): Result<UShort> =
        if (v <= UShort.MAX_VALUE.toUInt()) Result.success(v.toUShort()) else invalidUnsigned(v.toULong(), this)

    override fun visitU64(v: ULong): Result<UShort> =
        if (v <= UShort.MAX_VALUE.toULong()) Result.success(v.toUShort()) else invalidUnsigned(v, this)
}

public data object U16Deserialize : Deserialize<UShort> {
    override fun <D> deserialize(deserializer: D): Result<UShort>
        where D : Deserializer =
        deserializer.deserializeU16(U16Visitor)
}

private data object U32Visitor : Visitor<UInt> {
    override fun expecting(): String = "u32"

    override fun visitU32(v: UInt): Result<UInt> = Result.success(v)

    override fun visitU8(v: UByte): Result<UInt> = Result.success(v.toUInt())

    override fun visitU16(v: UShort): Result<UInt> = Result.success(v.toUInt())

    override fun visitI8(v: Byte): Result<UInt> =
        if (v >= 0) Result.success(v.toUInt()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): Result<UInt> =
        if (v >= 0) Result.success(v.toUInt()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): Result<UInt> =
        if (v >= 0) Result.success(v.toUInt()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): Result<UInt> =
        if (v in 0L..UInt.MAX_VALUE.toLong()) Result.success(v.toUInt()) else invalidSigned(v, this)

    override fun visitU64(v: ULong): Result<UInt> =
        if (v <= UInt.MAX_VALUE.toULong()) Result.success(v.toUInt()) else invalidUnsigned(v, this)
}

public data object U32Deserialize : Deserialize<UInt> {
    override fun <D> deserialize(deserializer: D): Result<UInt>
        where D : Deserializer =
        deserializer.deserializeU32(U32Visitor)
}

private data object U64Visitor : Visitor<ULong> {
    override fun expecting(): String = "u64"

    override fun visitU64(v: ULong): Result<ULong> = Result.success(v)

    override fun visitU8(v: UByte): Result<ULong> = Result.success(v.toULong())

    override fun visitU16(v: UShort): Result<ULong> = Result.success(v.toULong())

    override fun visitU32(v: UInt): Result<ULong> = Result.success(v.toULong())

    override fun visitI8(v: Byte): Result<ULong> =
        if (v >= 0) Result.success(v.toULong()) else invalidSigned(v.toLong(), this)

    override fun visitI16(v: Short): Result<ULong> =
        if (v >= 0) Result.success(v.toULong()) else invalidSigned(v.toLong(), this)

    override fun visitI32(v: Int): Result<ULong> =
        if (v >= 0) Result.success(v.toULong()) else invalidSigned(v.toLong(), this)

    override fun visitI64(v: Long): Result<ULong> =
        if (v >= 0) Result.success(v.toULong()) else invalidSigned(v, this)
}

public data object U64Deserialize : Deserialize<ULong> {
    override fun <D> deserialize(deserializer: D): Result<ULong>
        where D : Deserializer =
        deserializer.deserializeU64(U64Visitor)
}

private data object F32Visitor : Visitor<Float> {
    override fun expecting(): String = "f32"

    override fun visitF32(v: Float): Result<Float> = Result.success(v)

    override fun visitF64(v: Double): Result<Float> = Result.success(v.toFloat())

    override fun visitI8(v: Byte): Result<Float> = Result.success(v.toFloat())
    override fun visitI16(v: Short): Result<Float> = Result.success(v.toFloat())
    override fun visitI32(v: Int): Result<Float> = Result.success(v.toFloat())
    override fun visitI64(v: Long): Result<Float> = Result.success(v.toFloat())
    override fun visitU8(v: UByte): Result<Float> = Result.success(v.toFloat())
    override fun visitU16(v: UShort): Result<Float> = Result.success(v.toFloat())
    override fun visitU32(v: UInt): Result<Float> = Result.success(v.toFloat())
    override fun visitU64(v: ULong): Result<Float> = Result.success(v.toFloat())
}

public data object F32Deserialize : Deserialize<Float> {
    override fun <D> deserialize(deserializer: D): Result<Float>
        where D : Deserializer =
        deserializer.deserializeF32(F32Visitor)
}

private data object F64Visitor : Visitor<Double> {
    override fun expecting(): String = "f64"

    override fun visitF64(v: Double): Result<Double> = Result.success(v)

    override fun visitF32(v: Float): Result<Double> = Result.success(v.toDouble())

    override fun visitI8(v: Byte): Result<Double> = Result.success(v.toDouble())
    override fun visitI16(v: Short): Result<Double> = Result.success(v.toDouble())
    override fun visitI32(v: Int): Result<Double> = Result.success(v.toDouble())
    override fun visitI64(v: Long): Result<Double> = Result.success(v.toDouble())
    override fun visitU8(v: UByte): Result<Double> = Result.success(v.toDouble())
    override fun visitU16(v: UShort): Result<Double> = Result.success(v.toDouble())
    override fun visitU32(v: UInt): Result<Double> = Result.success(v.toDouble())
    override fun visitU64(v: ULong): Result<Double> = Result.success(v.toDouble())
}

public data object F64Deserialize : Deserialize<Double> {
    override fun <D> deserialize(deserializer: D): Result<Double>
        where D : Deserializer =
        deserializer.deserializeF64(F64Visitor)
}

////////////////////////////////////////////////////////////////////////////////

private data object CharVisitor : Visitor<Char> {
    override fun expecting(): String = "a character"

    override fun visitChar(v: Char): Result<Char> = Result.success(v)

    override fun visitStr(v: String): Result<Char> =
        if (v.length == 1) Result.success(v[0]) else Result.failure(Error.invalidValue(Unexpected.Str(v), this))
}

public data object CharDeserialize : Deserialize<Char> {
    override fun <D> deserialize(deserializer: D): Result<Char>
        where D : Deserializer =
        deserializer.deserializeChar(CharVisitor)
}

////////////////////////////////////////////////////////////////////////////////

private data object StringVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): Result<String> = Result.success(v)

    override fun visitString(v: String): Result<String> = Result.success(v)

    override fun visitBytes(v: ByteArray): Result<String> =
        runCatching { v.decodeToString(throwOnInvalidSequence = true) }
            .recoverCatching { throw Error.invalidValue(Unexpected.Bytes(v), this) }

    override fun visitByteBuf(v: ByteArray): Result<String> = visitBytes(v)
}

public data object StringDeserialize : Deserialize<String> {
    override fun <D> deserialize(deserializer: D): Result<String>
        where D : Deserializer =
        deserializer.deserializeString(StringVisitor)

    override fun <D> deserializeInPlace(deserializer: D, place: (String) -> Unit): Result<Unit>
        where D : Deserializer =
        deserialize(deserializer).map { place(it) }
}

////////////////////////////////////////////////////////////////////////////////

private data object StrVisitor : Visitor<String> {
    override fun expecting(): String = "a borrowed string"

    override fun visitBorrowedStr(v: String): Result<String> = Result.success(v)

    override fun visitBorrowedBytes(v: ByteArray): Result<String> =
        runCatching { v.decodeToString(throwOnInvalidSequence = true) }
            .recoverCatching { throw Error.invalidValue(Unexpected.Bytes(v), this) }
}

public data object BorrowedStrDeserialize : Deserialize<String> {
    override fun <D> deserialize(deserializer: D): Result<String>
        where D : Deserializer =
        deserializer.deserializeStr(StrVisitor)
}

////////////////////////////////////////////////////////////////////////////////

private data object BytesVisitor : Visitor<ByteArray> {
    override fun expecting(): String = "a borrowed byte array"

    override fun visitBorrowedBytes(v: ByteArray): Result<ByteArray> = Result.success(v)

    override fun visitBorrowedStr(v: String): Result<ByteArray> = Result.success(v.encodeToByteArray())
}

public data object BorrowedBytesDeserialize : Deserialize<ByteArray> {
    override fun <D> deserialize(deserializer: D): Result<ByteArray>
        where D : Deserializer =
        deserializer.deserializeBytes(BytesVisitor)
}

////////////////////////////////////////////////////////////////////////////////

public fun <T> nullableDeserialize(valueDeserialize: Deserialize<T>): Deserialize<T?> =
    object : Deserialize<T?> {
        override fun <D> deserialize(deserializer: D): Result<T?>
            where D : Deserializer =
            deserializer.deserializeOption(
                object : Visitor<T?> {
                    override fun expecting(): String = "option"

                    override fun visitUnit(): Result<T?> = Result.success(null)

                    override fun visitNone(): Result<T?> = Result.success(null)

                    override fun <D2> visitSome(deserializer: D2): Result<T?>
                        where D2 : Deserializer =
                        valueDeserialize.deserialize(deserializer).map { it }

                    override fun <D2> privateVisitUntaggedOption(deserializer: D2): Result<T?>
                        where D2 : Deserializer =
                        Result.success(valueDeserialize.deserialize(deserializer).getOrNull())
                },
            )
    }

////////////////////////////////////////////////////////////////////////////////

private class SeedFromDeserialize<T>(
    private val deserialize: Deserialize<T>,
) : DeserializeSeed<T> {
    override fun <D> deserialize(deserializer: D): Result<T>
        where D : Deserializer =
        deserialize.deserialize(deserializer)
}

public fun <T> mutableListDeserialize(elementDeserialize: Deserialize<T>): Deserialize<MutableList<T>> =
    object : Deserialize<MutableList<T>> {
        override fun <D> deserialize(deserializer: D): Result<MutableList<T>>
            where D : Deserializer =
            deserializer.deserializeSeq(
                object : Visitor<MutableList<T>> {
                    override fun expecting(): String = "a sequence"

                    override fun <A> visitSeq(seq: A): Result<MutableList<T>>
                        where A : SeqAccess =
                        runCatching {
                            val hint = seq.sizeHint() ?: 0
                            val values = ArrayList<T>(hint)
                            val seed = SeedFromDeserialize(elementDeserialize)
                            while (true) {
                                val next = seq.nextElementSeed(seed).getOrThrow() ?: break
                                values.add(next)
                            }
                            values
                        }
                },
            )
    }

////////////////////////////////////////////////////////////////////////////////

public fun <K, V> mutableMapDeserialize(
    keyDeserialize: Deserialize<K>,
    valueDeserialize: Deserialize<V>,
): Deserialize<MutableMap<K, V>> =
    object : Deserialize<MutableMap<K, V>> {
        override fun <D> deserialize(deserializer: D): Result<MutableMap<K, V>>
            where D : Deserializer =
            deserializer.deserializeMap(
                object : Visitor<MutableMap<K, V>> {
                    override fun expecting(): String = "a map"

                    override fun <A> visitMap(map: A): Result<MutableMap<K, V>>
                        where A : MapAccess =
                        runCatching {
                            val hint = map.sizeHint() ?: 0
                            val values = LinkedHashMap<K, V>(hint)
                            val keySeed = SeedFromDeserialize(keyDeserialize)
                            val valueSeed = SeedFromDeserialize(valueDeserialize)
                            while (true) {
                                val entry = map.nextEntrySeed(keySeed, valueSeed).getOrThrow() ?: break
                                values[entry.first] = entry.second
                            }
                            values
                        }
                },
            )
    }

////////////////////////////////////////////////////////////////////////////////

public fun <T0, T1> pairDeserialize(
    firstDeserialize: Deserialize<T0>,
    secondDeserialize: Deserialize<T1>,
): Deserialize<Pair<T0, T1>> =
    object : Deserialize<Pair<T0, T1>> {
        override fun <D> deserialize(deserializer: D): Result<Pair<T0, T1>>
            where D : Deserializer =
            deserializer.deserializeTuple(
                2,
                object : Visitor<Pair<T0, T1>> {
                    override fun expecting(): String = "a tuple of size 2"

                    override fun <A> visitSeq(seq: A): Result<Pair<T0, T1>>
                        where A : SeqAccess =
                        runCatching {
                            val first = seq.nextElementSeed(SeedFromDeserialize(firstDeserialize)).getOrThrow()
                                ?: throw Error.invalidLength(0, this)
                            val second = seq.nextElementSeed(SeedFromDeserialize(secondDeserialize)).getOrThrow()
                                ?: throw Error.invalidLength(1, this)
                            first to second
                        }
                },
            )
    }

public fun <T0, T1, T2> tripleDeserialize(
    firstDeserialize: Deserialize<T0>,
    secondDeserialize: Deserialize<T1>,
    thirdDeserialize: Deserialize<T2>,
): Deserialize<Triple<T0, T1, T2>> =
    object : Deserialize<Triple<T0, T1, T2>> {
        override fun <D> deserialize(deserializer: D): Result<Triple<T0, T1, T2>>
            where D : Deserializer =
            deserializer.deserializeTuple(
                3,
                object : Visitor<Triple<T0, T1, T2>> {
                    override fun expecting(): String = "a tuple of size 3"

                    override fun <A> visitSeq(seq: A): Result<Triple<T0, T1, T2>>
                        where A : SeqAccess =
                        runCatching {
                            val first = seq.nextElementSeed(SeedFromDeserialize(firstDeserialize)).getOrThrow()
                                ?: throw Error.invalidLength(0, this)
                            val second = seq.nextElementSeed(SeedFromDeserialize(secondDeserialize)).getOrThrow()
                                ?: throw Error.invalidLength(1, this)
                            val third = seq.nextElementSeed(SeedFromDeserialize(thirdDeserialize)).getOrThrow()
                                ?: throw Error.invalidLength(2, this)
                            Triple(first, second, third)
                        }
                },
            )
    }

////////////////////////////////////////////////////////////////////////////////

public data object DurationDeserialize : Deserialize<Duration> {
    override fun <D> deserialize(deserializer: D): Result<Duration>
        where D : Deserializer =
        deserializer.deserializeStruct(
            "Duration",
            listOf("secs", "nanos"),
            object : Visitor<Duration> {
                override fun expecting(): String = "struct Duration"

                override fun <A> visitSeq(seq: A): Result<Duration>
                    where A : SeqAccess =
                    runCatching {
                        val secs = seq.nextElementSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                            ?: throw Error.invalidLength(0, this)
                        val nanos = seq.nextElementSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                            ?: throw Error.invalidLength(1, this)
                        secs.toLong().seconds + nanos.toLong().nanoseconds
                    }

                override fun <A> visitMap(map: A): Result<Duration>
                    where A : MapAccess =
                    runCatching {
                        var secs: ULong? = null
                        var nanos: UInt? = null
                        val fieldSeed = SeedFromDeserialize(
                            fieldIdentifierDeserialize(
                                expectingMessage = "`secs` or `nanos`",
                                fields = listOf("secs", "nanos"),
                            ),
                        )
                        while (true) {
                            val key = map.nextKeySeed(fieldSeed).getOrThrow() ?: break
                            when (key) {
                                "secs" -> {
                                    if (secs != null) throw Error.duplicateField("secs")
                                    secs = map.nextValueSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                                }
                                "nanos" -> {
                                    if (nanos != null) throw Error.duplicateField("nanos")
                                    nanos = map.nextValueSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                                }
                                else -> throw Error.unknownField(key, listOf("secs", "nanos"))
                            }
                        }
                        val s = secs ?: throw Error.missingField("secs")
                        val n = nanos ?: throw Error.missingField("nanos")
                        s.toLong().seconds + n.toLong().nanoseconds
                    }
            },
        )
}

////////////////////////////////////////////////////////////////////////////////

public data object SystemTimeDeserialize : Deserialize<Instant> {
    override fun <D> deserialize(deserializer: D): Result<Instant>
        where D : Deserializer =
        deserializer.deserializeStruct(
            "SystemTime",
            listOf("secs_since_epoch", "nanos_since_epoch"),
            object : Visitor<Instant> {
                override fun expecting(): String = "struct SystemTime"

                override fun <A> visitSeq(seq: A): Result<Instant>
                    where A : SeqAccess =
                    runCatching {
                        val secs = seq.nextElementSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                            ?: throw Error.invalidLength(0, this)
                        val nanos = seq.nextElementSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                            ?: throw Error.invalidLength(1, this)
                        Instant.fromEpochSeconds(secs.toLong(), nanos.toInt())
                    }

                override fun <A> visitMap(map: A): Result<Instant>
                    where A : MapAccess =
                    runCatching {
                        var secs: ULong? = null
                        var nanos: UInt? = null
                        val fieldSeed = SeedFromDeserialize(
                            fieldIdentifierDeserialize(
                                expectingMessage = "`secs_since_epoch` or `nanos_since_epoch`",
                                fields = listOf("secs_since_epoch", "nanos_since_epoch"),
                            ),
                        )
                        while (true) {
                            val key = map.nextKeySeed(fieldSeed).getOrThrow() ?: break
                            when (key) {
                                "secs_since_epoch" -> {
                                    if (secs != null) throw Error.duplicateField("secs_since_epoch")
                                    secs = map.nextValueSeed(SeedFromDeserialize(U64Deserialize)).getOrThrow()
                                }
                                "nanos_since_epoch" -> {
                                    if (nanos != null) throw Error.duplicateField("nanos_since_epoch")
                                    nanos = map.nextValueSeed(SeedFromDeserialize(U32Deserialize)).getOrThrow()
                                }
                                else -> throw Error.unknownField(
                                    key,
                                    listOf("secs_since_epoch", "nanos_since_epoch"),
                                )
                            }
                        }
                        val s = secs ?: throw Error.missingField("secs_since_epoch")
                        val n = nanos ?: throw Error.missingField("nanos_since_epoch")
                        Instant.fromEpochSeconds(s.toLong(), n.toInt())
                    }
            },
        )
}

////////////////////////////////////////////////////////////////////////////////

private fun fieldIdentifierDeserialize(
    expectingMessage: String,
    fields: List<String>,
): Deserialize<String> =
    object : Deserialize<String> {
        override fun <D> deserialize(deserializer: D): Result<String>
            where D : Deserializer =
            deserializer.deserializeIdentifier(
                object : Visitor<String> {
                    override fun expecting(): String = expectingMessage

                    override fun visitStr(v: String): Result<String> =
                        if (v in fields) Result.success(v) else Result.failure(Error.unknownField(v, fields))

                    override fun visitBytes(v: ByteArray): Result<String> {
                        val s = v.decodeToString()
                        return if (s in fields) {
                            Result.success(s)
                        } else {
                            Result.failure(Error.unknownField(fromUtf8LossyNoAlloc(v), fields))
                        }
                    }
                },
            )
    }
