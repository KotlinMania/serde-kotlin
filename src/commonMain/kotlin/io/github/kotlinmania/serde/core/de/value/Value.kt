// port-lint: source serde_core/src/de/value.rs
package io.github.kotlinmania.serde.core.de.value

import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.EnumAccess
import io.github.kotlinmania.serde.core.de.Error as DeError
import io.github.kotlinmania.serde.core.de.Expected
import io.github.kotlinmania.serde.core.de.IntoDeserializer
import io.github.kotlinmania.serde.core.de.MapAccess
import io.github.kotlinmania.serde.core.de.SeqAccess
import io.github.kotlinmania.serde.core.de.Unexpected
import io.github.kotlinmania.serde.core.de.VariantAccess
import io.github.kotlinmania.serde.core.de.Visitor

/**
 * Building blocks for deserializing basic values using the `IntoDeserializer` interface.
 *
 * ```kotlin
 * // In Rust this example uses serde_derive and FromStr. In Kotlin, the same shape is a
 * // `deserialize` call driven by a value deserializer.
 * //
 * // enum class Setting { On, Off }
 * //
 * // fun parseSetting(s: String): Result<Setting> =
 * //     Setting.deserialize(s.intoDeserializer())
 * ```
 */

////////////////////////////////////////////////////////////////////////////////

/**
 * A minimal representation of all possible errors that can occur using the `IntoDeserializer`
 * interface.
 */
public class Error private constructor(
    private val err: String,
) : Exception(err) {
    public companion object {
        public fun custom(msg: Any?): Error = Error(msg.toString())
    }

    override fun toString(): String = err
}

////////////////////////////////////////////////////////////////////////////////

public fun Unit.intoDeserializer(): UnitDeserializer = UnitDeserializer.new()

/**
 * A deserializer holding a `Unit`.
 */
public class UnitDeserializer private constructor() : Deserializer, IntoDeserializer {
    public companion object {
        public fun new(): UnitDeserializer = UnitDeserializer()
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitUnit()

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> =
        visitor.visitNone()

    override fun intoDeserializer(): Deserializer = this
}

////////////////////////////////////////////////////////////////////////////////

private fun <V> forwardToAny(deserializer: Deserializer, visitor: Visitor<V>): Result<V> =
    deserializer.deserializeAny(visitor)

private class ExpectedInSeq(
    private val expected: Int,
) : Expected {
    override fun expecting(): String =
        if (expected == 1) "1 element in sequence" else "$expected elements in sequence"
}

private class ExpectedInMap(
    private val expected: Int,
) : Expected {
    override fun expecting(): String =
        if (expected == 1) "1 element in map" else "$expected elements in map"
}

public abstract class PrimitiveDeserializer<T> : Deserializer, IntoDeserializer {
    protected abstract val value: T
    protected abstract fun <V> visit(visitor: Visitor<V>, value: T): Result<V>

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> = visit(visitor, value)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = forwardToAny(this, visitor)

    override fun intoDeserializer(): Deserializer = this
}

public fun Boolean.intoDeserializer(): BoolDeserializer = BoolDeserializer.new(this)
public class BoolDeserializer private constructor(override val value: Boolean) : PrimitiveDeserializer<Boolean>() {
    public companion object {
        public fun new(value: Boolean): BoolDeserializer = BoolDeserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Boolean): Result<V> = visitor.visitBool(value)
}

public fun Byte.intoDeserializer(): I8Deserializer = I8Deserializer.new(this)
public class I8Deserializer private constructor(override val value: Byte) : PrimitiveDeserializer<Byte>() {
    public companion object {
        public fun new(value: Byte): I8Deserializer = I8Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Byte): Result<V> = visitor.visitI8(value)
}

public fun Short.intoDeserializer(): I16Deserializer = I16Deserializer.new(this)
public class I16Deserializer private constructor(override val value: Short) : PrimitiveDeserializer<Short>() {
    public companion object {
        public fun new(value: Short): I16Deserializer = I16Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Short): Result<V> = visitor.visitI16(value)
}

public fun Int.intoDeserializer(): I32Deserializer = I32Deserializer.new(this)
public class I32Deserializer private constructor(override val value: Int) : PrimitiveDeserializer<Int>() {
    public companion object {
        public fun new(value: Int): I32Deserializer = I32Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Int): Result<V> = visitor.visitI32(value)
}

public fun Long.intoDeserializer(): I64Deserializer = I64Deserializer.new(this)
public class I64Deserializer private constructor(override val value: Long) : PrimitiveDeserializer<Long>() {
    public companion object {
        public fun new(value: Long): I64Deserializer = I64Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Long): Result<V> = visitor.visitI64(value)
}

public fun UByte.intoDeserializer(): U8Deserializer = U8Deserializer.new(this)
public class U8Deserializer private constructor(override val value: UByte) : PrimitiveDeserializer<UByte>() {
    public companion object {
        public fun new(value: UByte): U8Deserializer = U8Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: UByte): Result<V> = visitor.visitU8(value)
}

public fun UShort.intoDeserializer(): U16Deserializer = U16Deserializer.new(this)
public class U16Deserializer private constructor(override val value: UShort) : PrimitiveDeserializer<UShort>() {
    public companion object {
        public fun new(value: UShort): U16Deserializer = U16Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: UShort): Result<V> = visitor.visitU16(value)
}

public fun UInt.intoDeserializer(): U32Deserializer = U32Deserializer.new(this)

/**
 * A deserializer holding a `UInt`.
 *
 * This deserializer also implements `EnumAccess`, matching the upstream behavior of allowing a
 * `u32` to serve as an enum discriminant.
 */
public class U32Deserializer private constructor(
    private val value: UInt,
) : Deserializer, EnumAccess, IntoDeserializer {
    public companion object {
        public fun new(value: UInt): U32Deserializer = U32Deserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitU32(value)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> {
        name.hashCode()
        variants.hashCode()
        return visitor.visitEnum(this)
    }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): Result<Pair<V, VariantAccess>> =
        seed.deserialize(this).map { it to Private.UnitOnly }

    override fun intoDeserializer(): Deserializer = this
}

public fun ULong.intoDeserializer(): U64Deserializer = U64Deserializer.new(this)
public class U64Deserializer private constructor(override val value: ULong) : PrimitiveDeserializer<ULong>() {
    public companion object {
        public fun new(value: ULong): U64Deserializer = U64Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: ULong): Result<V> = visitor.visitU64(value)
}

public fun Float.intoDeserializer(): F32Deserializer = F32Deserializer.new(this)
public class F32Deserializer private constructor(override val value: Float) : PrimitiveDeserializer<Float>() {
    public companion object {
        public fun new(value: Float): F32Deserializer = F32Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Float): Result<V> = visitor.visitF32(value)
}

public fun Double.intoDeserializer(): F64Deserializer = F64Deserializer.new(this)
public class F64Deserializer private constructor(override val value: Double) : PrimitiveDeserializer<Double>() {
    public companion object {
        public fun new(value: Double): F64Deserializer = F64Deserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Double): Result<V> = visitor.visitF64(value)
}

public fun Char.intoDeserializer(): CharDeserializer = CharDeserializer.new(this)
public class CharDeserializer private constructor(override val value: Char) : PrimitiveDeserializer<Char>() {
    public companion object {
        public fun new(value: Char): CharDeserializer = CharDeserializer(value)
    }

    override fun <V> visit(visitor: Visitor<V>, value: Char): Result<V> = visitor.visitChar(value)
}

////////////////////////////////////////////////////////////////////////////////

public fun String.intoDeserializer(): StringDeserializer = StringDeserializer.new(this)

/**
 * A deserializer holding a `String`.
 */
public class StringDeserializer private constructor(
    private val value: String,
) : Deserializer, EnumAccess, IntoDeserializer {
    public companion object {
        public fun new(value: String): StringDeserializer = StringDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitString(value)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> {
        name.hashCode()
        variants.hashCode()
        return visitor.visitEnum(this)
    }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): Result<Pair<V, VariantAccess>> =
        seed.deserialize(this).map { it to Private.UnitOnly }

    override fun intoDeserializer(): Deserializer = this
}

////////////////////////////////////////////////////////////////////////////////

public fun ByteArray.intoDeserializer(): BytesDeserializer = BytesDeserializer.new(this)

/**
 * A deserializer holding a `ByteArray`. Always calls `Visitor.visitBytes`.
 */
public class BytesDeserializer private constructor(
    private val value: ByteArray,
) : Deserializer, IntoDeserializer {
    public companion object {
        public fun new(value: ByteArray): BytesDeserializer = BytesDeserializer(value)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitBytes(value)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun intoDeserializer(): Deserializer = this
}

////////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer that iterates over a sequence.
 */
public class SeqDeserializer<T : IntoDeserializer>(
    private val iter: Iterator<T>,
) : Deserializer, SeqAccess, IntoDeserializer {
    private var count: Int = 0

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        runCatching {
            val v = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            v
        }

    public fun end(): Result<Unit> =
        runCatching {
            val remaining = iter.asSequence().count()
            if (remaining == 0) {
                Unit
            } else {
                throw DeError.invalidLength(count + remaining, ExpectedInSeq(count))
            }
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <R> nextElementSeed(seed: DeserializeSeed<R>): Result<R?> =
        runCatching {
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

public fun <T : IntoDeserializer> Iterable<T>.intoDeserializer(): SeqDeserializer<T> = SeqDeserializer(iterator())

////////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a `SeqAccess`.
 */
public class SeqAccessDeserializer<A : SeqAccess>(
    private val seq: A,
) : Deserializer {
    public companion object {
        public fun <A : SeqAccess> new(seq: A): SeqAccessDeserializer<A> = SeqAccessDeserializer(seq)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitSeq(seq)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}

////////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer that iterates over a map.
 */
public class MapDeserializer<K, V>(
    private val iter: Iterator<Pair<K, V>>,
) : Deserializer, MapAccess, SeqAccess, IntoDeserializer
    where K : IntoDeserializer, V : IntoDeserializer {
    private var pendingValue: V? = null
    private var count: Int = 0

    override fun <T> nextKeySeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            val next = if (iter.hasNext()) iter.next() else null
            if (next == null) {
                null
            } else {
                count += 1
                pendingValue = next.second
                seed.deserialize(next.first.intoDeserializer()).getOrThrow()
            }
        }

    override fun <T> nextValueSeed(seed: DeserializeSeed<T>): Result<T> =
        runCatching {
            val value = pendingValue
            pendingValue = null
            require(value != null) { "MapAccess.nextValue called before nextKey" }
            seed.deserialize(value.intoDeserializer()).getOrThrow()
        }

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
        runCatching {
            val next = if (iter.hasNext()) iter.next() else null
            if (next == null) {
                null
            } else {
                val pairDeserializer = PairDeserializer(next.first, next.second)
                seed.deserialize(pairDeserializer).getOrThrow()
            }
        }

    override fun sizeHint(): Int? = null

    override fun <R> deserializeAny(visitor: Visitor<R>): Result<R> =
        runCatching {
            val value = visitor.visitMap(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <R> deserializeSeq(visitor: Visitor<R>): Result<R> =
        runCatching {
            val value = visitor.visitSeq(this).getOrThrow()
            end().getOrThrow()
            value
        }

    override fun <R> deserializeTuple(len: Int, visitor: Visitor<R>): Result<R> {
        len.hashCode()
        return deserializeSeq(visitor)
    }

    public fun end(): Result<Unit> =
        runCatching {
            val remaining = iter.asSequence().count()
            if (remaining == 0) {
                Unit
            } else {
                throw DeError.invalidLength(count + remaining, ExpectedInMap(count))
            }
        }

    override fun <R> deserializeBool(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeI8(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeI16(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeI32(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeI64(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeU8(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeU16(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeU32(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeU64(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeF32(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeF64(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeChar(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeStr(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeString(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeBytes(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeByteBuf(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeOption(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeUnit(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeUnitStruct(name: String, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeNewtypeStruct(name: String, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeMap(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeIdentifier(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    override fun <R> deserializeIgnoredAny(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)

    private class PairDeserializer<K, V>(
        private val key: K,
        private val value: V,
    ) : Deserializer
        where K : IntoDeserializer, V : IntoDeserializer {
        override fun <R> deserializeAny(visitor: Visitor<R>): Result<R> =
            deserializeSeq(visitor)

        override fun <R> deserializeSeq(visitor: Visitor<R>): Result<R> =
            runCatching {
                val pairVisitor = PairVisitor(key, value)
                val pair = visitor.visitSeq(pairVisitor).getOrThrow()
                if (pairVisitor.hasRemaining()) {
                    val remaining = pairVisitor.sizeHint() ?: 0
                    throw DeError.invalidLength(2, ExpectedInSeq(2 - remaining))
                }
                pair
            }

        override fun <R> deserializeTuple(len: Int, visitor: Visitor<R>): Result<R> =
            if (len == 2) {
                deserializeSeq(visitor)
            } else {
                Result.failure(DeError.invalidLength(2, ExpectedInSeq(len)))
            }

        override fun <R> deserializeBool(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeI8(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeI16(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeI32(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeI64(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeU8(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeU16(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeU32(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeU64(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeF32(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeF64(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeChar(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeStr(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeString(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeBytes(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeByteBuf(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeOption(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeUnit(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeUnitStruct(name: String, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeNewtypeStruct(name: String, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeMap(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeIdentifier(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
        override fun <R> deserializeIgnoredAny(visitor: Visitor<R>): Result<R> = deserializeAny(visitor)
    }

    private class PairVisitor<K, V>(
        key: K,
        value: V,
    ) : SeqAccess
        where K : IntoDeserializer, V : IntoDeserializer {
        private var key: K? = key
        private var value: V? = value

        override fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?> =
            runCatching {
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

public fun <K : IntoDeserializer, V : IntoDeserializer> Map<K, V>.intoDeserializer(): MapDeserializer<K, V> =
    MapDeserializer(entries.map { it.key to it.value }.iterator())

////////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding a `MapAccess`.
 */
public class MapAccessDeserializer<A : MapAccess>(
    private val map: A,
) : Deserializer, EnumAccess {
    public companion object {
        public fun <A : MapAccess> new(map: A): MapAccessDeserializer<A> = MapAccessDeserializer(map)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitMap(map)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> =
        visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): Result<Pair<V, VariantAccess>> =
        runCatching {
            val key = map.nextKeySeed(seed).getOrThrow()
            if (key != null) {
                key to Private.MapAsEnum(map)
            } else {
                throw DeError.invalidType(Unexpected.Map, object : Expected {
                    override fun expecting(): String = "enum"
                })
            }
        }
}

////////////////////////////////////////////////////////////////////////////////

/**
 * A deserializer holding an `EnumAccess`.
 */
public class EnumAccessDeserializer<A : EnumAccess>(
    private val access: A,
) : Deserializer {
    public companion object {
        public fun <A : EnumAccess> new(access: A): EnumAccessDeserializer<A> = EnumAccessDeserializer(access)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitEnum(access)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}

////////////////////////////////////////////////////////////////////////////////

private object Private {
    object UnitOnly : VariantAccess {
        override fun unitVariant(): Result<Unit> = Result.success(Unit)

        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): Result<T> =
            Result.failure(DeError.invalidType(Unexpected.UnitVariant, object : Expected {
                override fun expecting(): String = "newtype variant"
            }))

        override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): Result<V> =
            Result.failure(DeError.invalidType(Unexpected.UnitVariant, object : Expected {
                override fun expecting(): String = "tuple variant"
            }))

        override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): Result<V> =
            Result.failure(DeError.invalidType(Unexpected.UnitVariant, object : Expected {
                override fun expecting(): String = "struct variant"
            }))
    }

    class MapAsEnum<A : MapAccess>(
        private val map: A,
    ) : VariantAccess {
        override fun unitVariant(): Result<Unit> =
            map.nextValueSeed(object : DeserializeSeed<Unit> {
                override fun <D> deserialize(deserializer: D): Result<Unit> where D : Deserializer = Result.success(Unit)
            })

        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): Result<T> =
            map.nextValueSeed(seed)

        override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): Result<V> =
            map.nextValueSeed(SeedTupleVariant(len, visitor))

        override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): Result<V> =
            map.nextValueSeed(SeedStructVariant(visitor))
    }

    private class SeedTupleVariant<V>(
        private val len: Int,
        private val visitor: Visitor<V>,
    ) : DeserializeSeed<V> {
        override fun <D> deserialize(deserializer: D): Result<V>
            where D : Deserializer =
            deserializer.deserializeTuple(len, visitor)
    }

    private class SeedStructVariant<V>(
        private val visitor: Visitor<V>,
    ) : DeserializeSeed<V> {
        override fun <D> deserialize(deserializer: D): Result<V>
            where D : Deserializer =
            deserializer.deserializeMap(visitor)
    }
}
