// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * This interface represents a visitor that walks through a deserializer.
 */
public interface Visitor<Value> : Expected {
    /**
     * Format a message stating what data this Visitor expects to receive.
     */
    override fun expecting(): String

    /**
     * The input contains a boolean.
     */
    public fun visitBool(v: Boolean): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Bool(v), this))

    /**
     * The input contains a `Byte`.
     */
    public fun visitI8(v: Byte): Result<Value> = visitI64(v.toLong())

    /**
     * The input contains a `Short`.
     */
    public fun visitI16(v: Short): Result<Value> = visitI64(v.toLong())

    /**
     * The input contains an `Int`.
     */
    public fun visitI32(v: Int): Result<Value> = visitI64(v.toLong())

    /**
     * The input contains a `Long`.
     */
    public fun visitI64(v: Long): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Signed(v), this))

    /**
     * The input contains an `i128`.
     */
    public fun visitI128(v: String): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Other("integer `$v` as i128"), this))

    /**
     * The input contains a `UByte`.
     */
    public fun visitU8(v: UByte): Result<Value> = visitU64(v.toULong())

    /**
     * The input contains a `UShort`.
     */
    public fun visitU16(v: UShort): Result<Value> = visitU64(v.toULong())

    /**
     * The input contains a `UInt`.
     */
    public fun visitU32(v: UInt): Result<Value> = visitU64(v.toULong())

    /**
     * The input contains a `ULong`.
     */
    public fun visitU64(v: ULong): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Unsigned(v), this))

    /**
     * The input contains a `u128`.
     */
    public fun visitU128(v: String): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Other("integer `$v` as u128"), this))

    /**
     * The input contains a `Float`.
     */
    public fun visitF32(v: Float): Result<Value> = visitF64(v.toDouble())

    /**
     * The input contains a `Double`.
     */
    public fun visitF64(v: Double): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.FloatValue(v), this))

    /**
     * The input contains a `Char`.
     */
    public fun visitChar(v: Char): Result<Value> = visitStr(v.toString())

    /**
     * The input contains a string.
     */
    public fun visitStr(v: String): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Str(v), this))

    /**
     * The input contains a string that lives at least as long as the `Deserializer`.
     */
    public fun visitBorrowedStr(v: String): Result<Value> = visitStr(v)

    /**
     * The input contains a string and ownership of the string is being given to the `Visitor`.
     */
    public fun visitString(v: String): Result<Value> = visitStr(v)

    /**
     * The input contains a byte array.
     */
    public fun visitBytes(v: ByteArray): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Bytes(v), this))

    /**
     * The input contains a byte array that lives at least as long as the `Deserializer`.
     */
    public fun visitBorrowedBytes(v: ByteArray): Result<Value> = visitBytes(v)

    /**
     * The input contains a byte array and ownership of the byte array is being given to the
     * `Visitor`.
     */
    public fun visitByteBuf(v: ByteArray): Result<Value> = visitBytes(v)

    /**
     * The input contains an optional that is absent.
     */
    public fun visitNone(): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.Option, this))

    /**
     * The input contains an optional that is present.
     */
    public fun <D> visitSome(deserializer: D): Result<Value>
        where D : Deserializer {
        deserializer.hashCode()
        return Result.failure(Error.invalidType(Unexpected.Option, this))
    }

    /**
     * The input contains a unit `Unit`.
     */
    public fun visitUnit(): Result<Value> =
        Result.failure(Error.invalidType(Unexpected.UnitValue, this))

    /**
     * The input contains a newtype class.
     */
    public fun <D> visitNewtypeStruct(deserializer: D): Result<Value>
        where D : Deserializer {
        deserializer.hashCode()
        return Result.failure(Error.invalidType(Unexpected.NewtypeStruct, this))
    }

    /**
     * The input contains a sequence of elements.
     */
    public fun <A> visitSeq(seq: A): Result<Value>
        where A : SeqAccess {
        seq.hashCode()
        return Result.failure(Error.invalidType(Unexpected.Seq, this))
    }

    /**
     * The input contains a key-value map.
     */
    public fun <A> visitMap(map: A): Result<Value>
        where A : MapAccess {
        map.hashCode()
        return Result.failure(Error.invalidType(Unexpected.Map, this))
    }

    /**
     * The input contains an enum.
     */
    public fun <A> visitEnum(data: A): Result<Value>
        where A : EnumAccess {
        data.hashCode()
        return Result.failure(Error.invalidType(Unexpected.Enum, this))
    }

    /**
     * Used when deserializing a flattened optional field. Not public API.
     */
    public fun <D> privateVisitUntaggedOption(deserializer: D): Result<Value>
        where D : Deserializer {
        deserializer.hashCode()
        return Result.failure(SerdeDeserializationException("untagged option is absent"))
    }
}
