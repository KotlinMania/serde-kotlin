// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult

/**
 * This interface represents a visitor that walks through a deserializer.
 */
interface Visitor<Value> : Expected {
    /**
     * Format a message stating what data this Visitor expects to receive.
     */
    override fun expecting(): String

    /**
     * The input contains a boolean.
     */
    fun visitBool(v: Boolean): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Bool(v), this))

    /**
     * The input contains a `Byte`.
     */
    fun visitI8(v: Byte): SerdeResult<Value> = visitI64(v.toLong())

    /**
     * The input contains a `Short`.
     */
    fun visitI16(v: Short): SerdeResult<Value> = visitI64(v.toLong())

    /**
     * The input contains an `Int`.
     */
    fun visitI32(v: Int): SerdeResult<Value> = visitI64(v.toLong())

    /**
     * The input contains a `Long`.
     */
    fun visitI64(v: Long): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Signed(v), this))

    /**
     * The input contains an `i128`.
     */
    fun visitI128(v: String): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Other("integer `$v` as i128"), this))

    /**
     * The input contains a `UByte`.
     */
    fun visitU8(v: UByte): SerdeResult<Value> = visitU64(v.toULong())

    /**
     * The input contains a `UShort`.
     */
    fun visitU16(v: UShort): SerdeResult<Value> = visitU64(v.toULong())

    /**
     * The input contains a `UInt`.
     */
    fun visitU32(v: UInt): SerdeResult<Value> = visitU64(v.toULong())

    /**
     * The input contains a `ULong`.
     */
    fun visitU64(v: ULong): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Unsigned(v), this))

    /**
     * The input contains a `u128`.
     */
    fun visitU128(v: String): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Other("integer `$v` as u128"), this))

    /**
     * The input contains a `Float`.
     */
    fun visitF32(v: Float): SerdeResult<Value> = visitF64(v.toDouble())

    /**
     * The input contains a `Double`.
     */
    fun visitF64(v: Double): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.FloatValue(v), this))

    /**
     * The input contains a `Char`.
     */
    fun visitChar(v: Char): SerdeResult<Value> = visitStr(v.toString())

    /**
     * The input contains a string.
     */
    fun visitStr(v: String): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Str(v), this))

    /**
     * The input contains a string that lives at least as long as the `Deserializer`.
     */
    fun visitBorrowedStr(v: String): SerdeResult<Value> = visitStr(v)

    /**
     * The input contains a string and ownership of the string is being given to the `Visitor`.
     */
    fun visitString(v: String): SerdeResult<Value> = visitStr(v)

    /**
     * The input contains a byte array.
     */
    fun visitBytes(v: ByteArray): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Bytes(v), this))

    /**
     * The input contains a byte array that lives at least as long as the `Deserializer`.
     */
    fun visitBorrowedBytes(v: ByteArray): SerdeResult<Value> = visitBytes(v)

    /**
     * The input contains a byte array and ownership of the byte array is being given to the
     * `Visitor`.
     */
    fun visitByteBuf(v: ByteArray): SerdeResult<Value> = visitBytes(v)

    /**
     * The input contains an optional that is absent.
     */
    fun visitNone(): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.Option, this))

    /**
     * The input contains an optional that is present.
     */
    fun <D> visitSome(deserializer: D): SerdeResult<Value>
        where D : Deserializer =
        SerdeResult.failure(Error.invalidType(Unexpected.Option, this))

    /**
     * The input contains a unit `Unit`.
     */
    fun visitUnit(): SerdeResult<Value> = SerdeResult.failure(Error.invalidType(Unexpected.UnitValue, this))

    /**
     * The input contains a newtype class.
     */
    fun <D> visitNewtypeStruct(deserializer: D): SerdeResult<Value>
        where D : Deserializer =
        SerdeResult.failure(Error.invalidType(Unexpected.NewtypeStruct, this))

    /**
     * The input contains a sequence of elements.
     */
    fun <A> visitSeq(access: A): SerdeResult<Value>
        where A : SeqAccess =
        SerdeResult.failure(Error.invalidType(Unexpected.Seq, this))

    /**
     * The input contains a key-value map.
     */
    fun <A> visitMap(access: A): SerdeResult<Value>
        where A : MapAccess =
        SerdeResult.failure(Error.invalidType(Unexpected.Map, this))

    /**
     * The input contains an enum.
     */
    fun <A> visitEnum(access: A): SerdeResult<Value>
        where A : EnumAccess =
        SerdeResult.failure(Error.invalidType(Unexpected.Enum, this))

    /**
     * Used when deserializing a flattened optional field. Not public API.
     */
    fun <D> privateVisitUntaggedOption(deserializer: D): SerdeResult<Value>
        where D : Deserializer =
        SerdeResult.failure(SerdeError("untagged option is absent"))
}
