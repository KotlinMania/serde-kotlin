// port-lint: source de/value.rs
package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.*
import io.github.kotlinmania.serde.serdeCatching

/**
 * Building blocks for deserializing basic values using the `IntoDeserializer` interface.
 *
 * ```kotlin
 * // Upstream uses derive support and string parsing. In Kotlin, the same shape is a
 * // `deserialize` call driven by a value deserializer.
 * //
 * // enum class Setting { On, Off }
 * //
 * // fun parseSetting(s: String): SerdeResult<Setting> =
 * //     Setting.deserialize(s.intoDeserializer())
 * ```
 */

// //////////////////////////////////////////////////////////////////////////////

/**
 * A minimal representation of all possible errors that can occur using the `IntoDeserializer`
 * interface.
 *
 * Not a `Throwable` subclass — avoids Swift export's Class Stdlib hazard
 * (unchecked-cast bridge on `Throwable.getStackTrace()`).
 */
class ValueError private constructor(
    private val err: String,
) {
    fun fmt(): String = err

    fun description(): String = err

    override fun toString(): String = fmt()

    override fun equals(other: Any?): Boolean = other is ValueError && other.err == err

    override fun hashCode(): Int = err.hashCode()

    companion object {
        fun custom(msg: String): ValueError = ValueError(msg)
    }
}


// //////////////////////////////////////////////////////////////////////////////

fun Unit.intoDeserializer(): UnitDeserializer = UnitDeserializer.new()

/**
 * A deserializer holding a `Unit`.
 */
class UnitDeserializer private constructor() :
    Deserializer,
    IntoDeserializer {
        companion object {
            fun new(): UnitDeserializer = UnitDeserializer()
        }

        override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()

        override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeUnitStruct(
            name: String,
            visitor: Visitor<V>,
        ): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeNewtypeStruct(
            name: String,
            visitor: Visitor<V>,
        ): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeTuple(
            len: Int,
            visitor: Visitor<V>,
        ): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeTupleStruct(
            name: String,
            len: Int,
            visitor: Visitor<V>,
        ): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeStruct(
            name: String,
            fields: List<String>,
            visitor: Visitor<V>,
        ): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeEnum(
            name: String,
            variants: List<String>,
            visitor: Visitor<V>,
        ): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

        override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = visitor.visitNone()

        override fun intoDeserializer(): Deserializer = this
    }

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer that cannot be instantiated.
 *
 * Available only when the upstream `unstable` feature is enabled. The `never` field has type
 * [Nothing], so this class has no constructible values.
 */
class NeverDeserializer private constructor(
    private val never: Nothing,
) : Deserializer,
    IntoDeserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = never

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun intoDeserializer(): Deserializer = this
}

// //////////////////////////////////////////////////////////////////////////////

private fun <V> forwardToAny(
    deserializer: Deserializer,
    visitor: Visitor<V>,
): SerdeResult<V> = deserializer.deserializeAny(visitor)

private class ExpectedInSeq(
    private val expected: Int,
) : Expected {
    override fun expecting(): String = if (expected == 1) "1 element in sequence" else "$expected elements in sequence"
}

private class ExpectedInMap(
    private val expected: Int,
) : Expected {
    override fun expecting(): String = if (expected == 1) "1 element in map" else "$expected elements in map"
}

abstract class PrimitiveDeserializer<T> :
    Deserializer,
    IntoDeserializer {
    protected abstract val value: T

    protected abstract fun <V> visit(
        visitor: Visitor<V>,
        value: T,
    ): SerdeResult<V>

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visit(visitor, value)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = forwardToAny(this, visitor)

    override fun intoDeserializer(): Deserializer = this
}

fun Boolean.intoDeserializer(): BoolDeserializer = BoolDeserializer.new(this)

class BoolDeserializer private constructor(
    override val value: Boolean,
) : PrimitiveDeserializer<Boolean>() {
    companion object {
        fun new(value: Boolean): BoolDeserializer = BoolDeserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Boolean,
    ): SerdeResult<V> = visitor.visitBool(value)
}

fun Byte.intoDeserializer(): I8Deserializer = I8Deserializer.new(this)

class I8Deserializer private constructor(
    override val value: Byte,
) : PrimitiveDeserializer<Byte>() {
    companion object {
        fun new(value: Byte): I8Deserializer = I8Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Byte,
    ): SerdeResult<V> = visitor.visitI8(value)
}

fun Short.intoDeserializer(): I16Deserializer = I16Deserializer.new(this)

class I16Deserializer private constructor(
    override val value: Short,
) : PrimitiveDeserializer<Short>() {
    companion object {
        fun new(value: Short): I16Deserializer = I16Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Short,
    ): SerdeResult<V> = visitor.visitI16(value)
}

fun Int.intoDeserializer(): I32Deserializer = I32Deserializer.new(this)

class I32Deserializer private constructor(
    override val value: Int,
) : PrimitiveDeserializer<Int>() {
    companion object {
        fun new(value: Int): I32Deserializer = I32Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Int,
    ): SerdeResult<V> = visitor.visitI32(value)
}

fun Long.intoDeserializer(): I64Deserializer = I64Deserializer.new(this)

class I64Deserializer private constructor(
    override val value: Long,
) : PrimitiveDeserializer<Long>() {
    companion object {
        fun new(value: Long): I64Deserializer = I64Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Long,
    ): SerdeResult<V> = visitor.visitI64(value)
}

fun String.intoI128Deserializer(): I128Deserializer = I128Deserializer.new(this)

/**
 * A deserializer holding a 128-bit signed integer.
 */
class I128Deserializer private constructor(
    override val value: String,
) : PrimitiveDeserializer<String>() {
    companion object {
        fun new(value: String): I128Deserializer = I128Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: String,
    ): SerdeResult<V> = visitor.visitI128(value)
}

fun Long.intoIsizeDeserializer(): IsizeDeserializer = IsizeDeserializer.new(this)

/**
 * A deserializer holding a platform-sized signed integer.
 */
class IsizeDeserializer private constructor(
    override val value: Long,
) : PrimitiveDeserializer<Long>() {
    companion object {
        fun new(value: Long): IsizeDeserializer = IsizeDeserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Long,
    ): SerdeResult<V> = visitor.visitI64(value)
}

fun UByte.intoDeserializer(): U8Deserializer = U8Deserializer.new(this)

class U8Deserializer private constructor(
    override val value: UByte,
) : PrimitiveDeserializer<UByte>() {
    companion object {
        fun new(value: UByte): U8Deserializer = U8Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: UByte,
    ): SerdeResult<V> = visitor.visitU8(value)
}

fun UShort.intoDeserializer(): U16Deserializer = U16Deserializer.new(this)

class U16Deserializer private constructor(
    override val value: UShort,
) : PrimitiveDeserializer<UShort>() {
    companion object {
        fun new(value: UShort): U16Deserializer = U16Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: UShort,
    ): SerdeResult<V> = visitor.visitU16(value)
}

fun UInt.intoDeserializer(): U32Deserializer = U32Deserializer.new(this)

/**
 * A deserializer holding a `UInt`.
 *
 * This deserializer also implements `EnumAccess`, matching the upstream behavior of allowing a
 * `u32` to serve as an enum discriminant.
 */
class U32Deserializer private constructor(
    private val value: UInt,
) : Deserializer,
    EnumAccess,
    IntoDeserializer {
    companion object {
        fun new(value: UInt): U32Deserializer = U32Deserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitU32(value)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(this).map(::unitOnly)

    override fun intoDeserializer(): Deserializer = this
}

fun ULong.intoDeserializer(): U64Deserializer = U64Deserializer.new(this)

class U64Deserializer private constructor(
    override val value: ULong,
) : PrimitiveDeserializer<ULong>() {
    companion object {
        fun new(value: ULong): U64Deserializer = U64Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: ULong,
    ): SerdeResult<V> = visitor.visitU64(value)
}

fun String.intoU128Deserializer(): U128Deserializer = U128Deserializer.new(this)

/**
 * A deserializer holding a 128-bit unsigned integer.
 */
class U128Deserializer private constructor(
    override val value: String,
) : PrimitiveDeserializer<String>() {
    companion object {
        fun new(value: String): U128Deserializer = U128Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: String,
    ): SerdeResult<V> = visitor.visitU128(value)
}

fun ULong.intoUsizeDeserializer(): UsizeDeserializer = UsizeDeserializer.new(this)

/**
 * A deserializer holding a platform-sized unsigned integer.
 */
class UsizeDeserializer private constructor(
    override val value: ULong,
) : PrimitiveDeserializer<ULong>() {
    companion object {
        fun new(value: ULong): UsizeDeserializer = UsizeDeserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: ULong,
    ): SerdeResult<V> = visitor.visitU64(value)
}

fun Float.intoDeserializer(): F32Deserializer = F32Deserializer.new(this)

class F32Deserializer private constructor(
    override val value: Float,
) : PrimitiveDeserializer<Float>() {
    companion object {
        fun new(value: Float): F32Deserializer = F32Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Float,
    ): SerdeResult<V> = visitor.visitF32(value)
}

fun Double.intoDeserializer(): F64Deserializer = F64Deserializer.new(this)

class F64Deserializer private constructor(
    override val value: Double,
) : PrimitiveDeserializer<Double>() {
    companion object {
        fun new(value: Double): F64Deserializer = F64Deserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Double,
    ): SerdeResult<V> = visitor.visitF64(value)
}

fun Char.intoDeserializer(): CharDeserializer = CharDeserializer.new(this)

class CharDeserializer private constructor(
    override val value: Char,
) : PrimitiveDeserializer<Char>() {
    companion object {
        fun new(value: Char): CharDeserializer = CharDeserializer(value)
    }

    override fun <V> visit(
        visitor: Visitor<V>,
        value: Char,
    ): SerdeResult<V> = visitor.visitChar(value)
}

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a string slice.
 */
class StrDeserializer private constructor(
    private val value: String,
) : Deserializer,
    EnumAccess,
    IntoDeserializer {
    companion object {
        fun new(value: String): StrDeserializer = StrDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitStr(value)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(this).map(::unitOnly)

    override fun intoDeserializer(): Deserializer = this
}

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a string borrowed from another deserializer.
 */
class BorrowedStrDeserializer private constructor(
    private val value: String,
) : Deserializer,
    EnumAccess,
    IntoDeserializer {
    companion object {
        fun new(value: String): BorrowedStrDeserializer = BorrowedStrDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBorrowedStr(value)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(this).map(::unitOnly)

    override fun intoDeserializer(): Deserializer = this
}

// //////////////////////////////////////////////////////////////////////////////

fun String.intoDeserializer(): StringDeserializer = StringDeserializer.new(this)

/**
 * A deserializer holding a `String`.
 */
class StringDeserializer private constructor(
    private val value: String,
) : Deserializer,
    EnumAccess,
    IntoDeserializer {
    companion object {
        fun new(value: String): StringDeserializer = StringDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitString(value)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(this).map(::unitOnly)

    override fun intoDeserializer(): Deserializer = this
}

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a string that may be borrowed or owned.
 */
class CowStrDeserializer private constructor(
    private val value: String,
    private val owned: Boolean,
) : Deserializer,
    EnumAccess,
    IntoDeserializer {
    companion object {
        fun new(
            value: String,
            owned: Boolean = false,
        ): CowStrDeserializer = CowStrDeserializer(value, owned)

        fun borrowed(value: String): CowStrDeserializer = CowStrDeserializer(value, false)

        fun owned(value: String): CowStrDeserializer = CowStrDeserializer(value, true)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        if (owned) {
            visitor.visitString(value)
        } else {
            visitor.visitStr(value)
        }

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        seed.deserialize(this).map(::unitOnly)

    override fun intoDeserializer(): Deserializer = this
}

// //////////////////////////////////////////////////////////////////////////////

fun ByteArray.intoDeserializer(): BytesDeserializer = BytesDeserializer.new(this)

/**
 * A deserializer holding a `ByteArray`. Always calls `Visitor.visitBytes`.
 */
class BytesDeserializer private constructor(
    private val value: ByteArray,
) : Deserializer,
    IntoDeserializer {
    companion object {
        fun new(value: ByteArray): BytesDeserializer = BytesDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBytes(value)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun intoDeserializer(): Deserializer = this
}

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a byte array borrowed from another deserializer. Always calls
 * `Visitor.visitBorrowedBytes`.
 */
class BorrowedBytesDeserializer private constructor(
    private val value: ByteArray,
) : Deserializer,
    IntoDeserializer {
    companion object {
        fun new(value: ByteArray): BorrowedBytesDeserializer = BorrowedBytesDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBorrowedBytes(value)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun intoDeserializer(): Deserializer = this
}

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer that iterates over a sequence.
 */
class SeqDeserializer<T : IntoDeserializer> internal constructor(
    private val iter: Iterator<T>,
) : Deserializer,
    SeqAccess,
    IntoDeserializer {
    private var count: Int = 0

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val v = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            v
        }

    fun end(): SerdeResult<Unit> =
        serdeCatching {
            val remaining = iter.asSequence().count()
            if (remaining != 0) {
                throw SerdeException(SerdeError.invalidLength(count + remaining, ExpectedInSeq(count)))
            }
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <R> nextElementSeed(seed: DeserializeSeed<R>): SerdeResult<R?> =
        serdeCatching {
            val next = if (iter.hasNext()) iter.next() else null
            if (next == null) {
                null
            } else {
                count += 1
                seed.deserialize(next.intoDeserializer()).getOrThrow()
            }
        }

    override fun sizeHint(): Int? = null

    override fun intoDeserializer(): Deserializer = this
}

fun <T : IntoDeserializer> Iterable<T>.intoDeserializer(): SeqDeserializer<T> = SeqDeserializer(iterator())

/**
 * Creates a [SeqDeserializer] from an iterator of values that can be converted
 * to deserializers. Use this instead of the constructor when constructing from
 * Swift or other interop boundaries.
 */
fun <T : IntoDeserializer> seqDeserializer(iter: Iterator<T>): SeqDeserializer<T> = SeqDeserializer(iter)

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a `SeqAccess`.
 */
class SeqAccessDeserializer<A : SeqAccess>(
    private val seq: A,
) : Deserializer {
    companion object {
        fun <A : SeqAccess> new(seq: A): SeqAccessDeserializer<A> = SeqAccessDeserializer(seq)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSeq(seq)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
}

// //////////////////////////////////////////////////////////////////////////////

/**
 * A key-value entry carried by [MapDeserializer]. Replaces a raw [Pair] in the
 * public constructor so the Swift Export bridge can express the generic
 * constraint without type-erasing to `Any?`.
 */
data class MapEntry<K, V>(
    val key: K,
    val value: V,
)

/**
 * A deserializer that iterates over a map.
 */
class MapDeserializer<K, V> internal constructor(
    private val iter: Iterator<MapEntry<K, V>>,
) : Deserializer,
    MapAccess,
    SeqAccess,
    IntoDeserializer
    where K : IntoDeserializer, V : IntoDeserializer {
    private var pendingValue: V? = null
    private var count: Int = 0

    private fun nextPair(): MapEntry<K, V>? {
        val next = if (iter.hasNext()) iter.next() else null
        if (next != null) {
            count += 1
        }
        return next
    }

    override fun <T> nextKeySeed(seed: DeserializeSeed<T>): SerdeResult<T?> =
        serdeCatching {
            val next = nextPair()
            if (next == null) {
                null
            } else {
                pendingValue = next.value
                seed.deserialize(next.key.intoDeserializer()).getOrThrow()
            }
        }

    override fun <T> nextValueSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
        serdeCatching {
            val value = pendingValue
            pendingValue = null
            require(value != null) { "MapAccess.nextValue called before nextKey" }
            seed.deserialize(value.intoDeserializer()).getOrThrow()
        }

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?> =
        serdeCatching {
            val next = nextPair()
            if (next == null) {
                null
            } else {
                val pairDeserializer = PairDeserializer(next.key, next.value)
                seed.deserialize(pairDeserializer).getOrThrow()
            }
        }

    override fun <K1, V1> nextEntrySeed(
        keySeed: DeserializeSeed<K1>,
        valueSeed: DeserializeSeed<V1>,
    ): SerdeResult<Pair<K1, V1>?> =
        serdeCatching {
            val next = nextPair()
            if (next == null) {
                null
            } else {
                val key = keySeed.deserialize(next.key.intoDeserializer()).getOrThrow()
                val value = valueSeed.deserialize(next.value.intoDeserializer()).getOrThrow()
                key to value
            }
        }

    override fun sizeHint(): Int? = null

    override fun <R> deserializeAny(visitor: Visitor<R>): SerdeResult<R> =
        serdeCatching {
            val value = visitor.visitMap(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <R> deserializeSeq(visitor: Visitor<R>): SerdeResult<R> =
        serdeCatching {
            val value = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <R> deserializeTuple(
        len: Int,
        visitor: Visitor<R>,
    ): SerdeResult<R> = deserializeSeq(visitor)

    fun end(): SerdeResult<Unit> =
        serdeCatching {
            val remaining = iter.asSequence().count()
            if (remaining != 0) {
                throw SerdeException(SerdeError.invalidLength(count + remaining, ExpectedInMap(count)))
            }
        }

    override fun <R> deserializeBool(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeI8(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeI16(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeI32(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeI64(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeI128(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeU8(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeU16(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeU32(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeU64(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeU128(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeF32(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeF64(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeChar(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeStr(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeString(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeBytes(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeByteBuf(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeOption(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeUnit(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeUnitStruct(
        name: String,
        visitor: Visitor<R>,
    ): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<R>,
    ): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<R>,
    ): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeMap(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<R>,
    ): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<R>,
    ): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeIdentifier(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    override fun <R> deserializeIgnoredAny(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

    private class PairDeserializer<K, V>(
        private val key: K,
        private val value: V,
    ) : Deserializer
        where K : IntoDeserializer, V : IntoDeserializer {
        override fun <R> deserializeAny(visitor: Visitor<R>): SerdeResult<R> = deserializeSeq(visitor)

        override fun <R> deserializeSeq(visitor: Visitor<R>): SerdeResult<R> =
            serdeCatching {
                val pairVisitor = PairVisitor(key, value)
                val pair = visitor.visitSeq(pairVisitor).getOrThrow()
                if (pairVisitor.hasRemaining()) {
                    val remaining = pairVisitor.sizeHint() ?: 0
                    throw SerdeException(SerdeError.invalidLength(2, ExpectedInSeq(2 - remaining)))
                }
                pair
            }

        override fun <R> deserializeTuple(
            len: Int,
            visitor: Visitor<R>,
        ): SerdeResult<R> =
            if (len == 2) {
                deserializeSeq(visitor)
            } else {
                SerdeResult.failure(SerdeError.invalidLength(2, ExpectedInSeq(len)))
            }

        override fun <R> deserializeBool(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeI8(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeI16(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeI32(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeI64(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeI128(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeU8(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeU16(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeU32(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeU64(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeU128(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeF32(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeF64(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeChar(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeStr(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeString(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeBytes(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeByteBuf(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeOption(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeUnit(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeUnitStruct(
            name: String,
            visitor: Visitor<R>,
        ): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeNewtypeStruct(
            name: String,
            visitor: Visitor<R>,
        ): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeTupleStruct(
            name: String,
            len: Int,
            visitor: Visitor<R>,
        ): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeMap(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeStruct(
            name: String,
            fields: List<String>,
            visitor: Visitor<R>,
        ): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeEnum(
            name: String,
            variants: List<String>,
            visitor: Visitor<R>,
        ): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeIdentifier(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)

        override fun <R> deserializeIgnoredAny(visitor: Visitor<R>): SerdeResult<R> = deserializeAny(visitor)
    }

    private class PairVisitor<K, V>(
        key: K,
        value: V,
    ) : SeqAccess
        where K : IntoDeserializer, V : IntoDeserializer {
        private var key: K? = key
        private var value: V? = value

        override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?> =
            serdeCatching {
                val k = key
                if (k != null) {
                    key = null
                    seed.deserialize(k.intoDeserializer()).getOrThrow()
                } else {
                    val v = value
                    if (v != null) {
                        value = null
                        seed.deserialize(v.intoDeserializer()).getOrThrow()
                    } else {
                        null
                    }
                }
            }

        fun hasRemaining(): Boolean = key != null || value != null

        override fun sizeHint(): Int? =
            when {
                key != null -> 2
                value != null -> 1
                else -> 0
            }
    }

    override fun intoDeserializer(): Deserializer = this
}

fun <K : IntoDeserializer, V : IntoDeserializer> Map<K, V>.intoDeserializer(): MapDeserializer<K, V> =
    MapDeserializer(entries.map { MapEntry(it.key, it.value) }.iterator())

/**
 * Creates a [MapDeserializer] from an iterator of entries. Use this instead of
 * the constructor when constructing from Swift or other interop boundaries.
 */
fun <K : IntoDeserializer, V : IntoDeserializer> mapDeserializer(
    iter: Iterator<MapEntry<K, V>>,
): MapDeserializer<K, V> = MapDeserializer(iter)

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a `MapAccess`.
 */
class MapAccessDeserializer<A : MapAccess>(
    private val map: A,
) : Deserializer,
    EnumAccess {
    companion object {
        fun <A : MapAccess> new(map: A): MapAccessDeserializer<A> = MapAccessDeserializer(map)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitMap(map)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        serdeCatching {
            val key = map.nextKeySeed(seed).getOrThrow()
            if (key != null) {
                key to mapAsEnum(map)
            } else {
                throw SerdeException(
                    SerdeError.invalidType(
                        Unexpected.Map,
                        object : Expected {
                            override fun expecting(): String = "enum"
                        },
                    ),
                )
            }
        }
}

// //////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding an `EnumAccess`.
 */
class EnumAccessDeserializer<A : EnumAccess>(
    private val access: A,
) : Deserializer {
    companion object {
        fun <A : EnumAccess> new(access: A): EnumAccessDeserializer<A> = EnumAccessDeserializer(access)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitEnum(access)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
}

// //////////////////////////////////////////////////////////////////////////////

private fun <T> unitOnly(value: T): Pair<T, VariantAccess> = value to Private.UnitOnly

private fun <A : MapAccess> mapAsEnum(map: A): VariantAccess = Private.MapAsEnum(map)

private object Private {
    object UnitOnly : VariantAccess {
        override fun unitVariant(): SerdeResult<Unit> = SerdeResult.success(Unit)

        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
            SerdeResult.failure(
                SerdeError.invalidType(
                    Unexpected.UnitVariant,
                    object : Expected {
                        override fun expecting(): String = "newtype variant"
                    },
                ),
            )

        override fun <V> tupleVariant(
            len: Int,
            visitor: Visitor<V>,
        ): SerdeResult<V> =
            SerdeResult.failure(
                SerdeError.invalidType(
                    Unexpected.UnitVariant,
                    object : Expected {
                        override fun expecting(): String = "tuple variant"
                    },
                ),
            )

        override fun <V> structVariant(
            fields: List<String>,
            visitor: Visitor<V>,
        ): SerdeResult<V> =
            SerdeResult.failure(
                SerdeError.invalidType(
                    Unexpected.UnitVariant,
                    object : Expected {
                        override fun expecting(): String = "struct variant"
                    },
                ),
            )
    }

    class MapAsEnum<A : MapAccess>(
        private val map: A,
    ) : VariantAccess {
        override fun unitVariant(): SerdeResult<Unit> =
            map.nextValueSeed(
                object : DeserializeSeed<Unit> {
                    override fun <D> deserialize(deserializer: D): SerdeResult<Unit> where D : Deserializer = SerdeResult.success(Unit)
                },
            )

        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> = map.nextValueSeed(seed)

        override fun <V> tupleVariant(
            len: Int,
            visitor: Visitor<V>,
        ): SerdeResult<V> = map.nextValueSeed(SeedTupleVariant(len, visitor))

        override fun <V> structVariant(
            fields: List<String>,
            visitor: Visitor<V>,
        ): SerdeResult<V> = map.nextValueSeed(SeedStructVariant(visitor))
    }

    private class SeedTupleVariant<V>(
        private val len: Int,
        private val visitor: Visitor<V>,
    ) : DeserializeSeed<V> {
        override fun <D> deserialize(deserializer: D): SerdeResult<V>
            where D : Deserializer =
            deserializer.deserializeTuple(len, visitor)
    }

    private class SeedStructVariant<V>(
        private val visitor: Visitor<V>,
    ) : DeserializeSeed<V> {
        override fun <D> deserialize(deserializer: D): SerdeResult<V>
            where D : Deserializer =
            deserializer.deserializeMap(visitor)
    }
}
