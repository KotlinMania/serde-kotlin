// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeResult

/**
 * A **data format** that can deserialize any data structure supported by Serde.
 *
 * The role of this interface is to define the deserialization half of the Serde data model, which
 * is a way to categorize every data type into one of 29 possible types. Each method of the
 * `Deserializer` interface corresponds to one of the types of the data model.
 */
interface Deserializer {
    /**
     * Require the `Deserializer` to figure out how to drive the visitor based on what data type is
     * in the input.
     */
    fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Boolean` value.
     */
    fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Byte` value.
     */
    fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Short` value.
     */
    fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting an `Int` value.
     */
    fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Long` value.
     */
    fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting an `i128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("i128 is not supported"))

    /**
     * Hint that the `Deserialize` type is expecting a `UByte` value.
     */
    fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `UShort` value.
     */
    fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `UInt` value.
     */
    fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `ULong` value.
     */
    fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `u128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("u128 is not supported"))

    /**
     * Hint that the `Deserialize` type is expecting a `Float` value.
     */
    fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Double` value.
     */
    fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Char` value.
     */
    fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a string value and does not benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a string value and would benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a byte array and does not benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a byte array and would benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting an optional value.
     */
    fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a unit value.
     */
    fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a unit class with a particular name.
     */
    fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a newtype class with a particular name.
     */
    fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a sequence of values.
     */
    fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a sequence of values and knows how many values
     * there are without looking at the serialized data.
     */
    fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a tuple class with a particular name and number
     * of fields.
     */
    fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a map of key-value pairs.
     */
    fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting a class with a particular name and fields.
     */
    fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting an enum value with a particular name and
     * possible variants.
     */
    fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type is expecting the name of a class field or the discriminant
     * of an enum variant.
     */
    fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Hint that the `Deserialize` type needs to deserialize a value whose type doesn't matter
     * because it is ignored.
     */
    fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V>

    /**
     * Determine whether `Deserialize` implementations should expect to deserialize their
     * human-readable form.
     */
    fun isHumanReadable(): Boolean = true
}
