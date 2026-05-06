// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * A **data format** that can deserialize any data structure supported by Serde.
 *
 * The role of this interface is to define the deserialization half of the Serde data model, which
 * is a way to categorize every Rust data type into one of 29 possible types. Each method of the
 * `Deserializer` interface corresponds to one of the types of the data model.
 */
public interface Deserializer {
    /**
     * Require the `Deserializer` to figure out how to drive the visitor based on what data type is
     * in the input.
     */
    public fun <V> deserializeAny(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Boolean` value.
     */
    public fun <V> deserializeBool(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Byte` value.
     */
    public fun <V> deserializeI8(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Short` value.
     */
    public fun <V> deserializeI16(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting an `Int` value.
     */
    public fun <V> deserializeI32(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Long` value.
     */
    public fun <V> deserializeI64(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting an `i128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    public fun <V> deserializeI128(visitor: Visitor<V>): Result<V> {
        visitor.hashCode()
        return Result.failure(Error.custom("i128 is not supported"))
    }

    /**
     * Hint that the `Deserialize` type is expecting a `UByte` value.
     */
    public fun <V> deserializeU8(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `UShort` value.
     */
    public fun <V> deserializeU16(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `UInt` value.
     */
    public fun <V> deserializeU32(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `ULong` value.
     */
    public fun <V> deserializeU64(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `u128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    public fun <V> deserializeU128(visitor: Visitor<V>): Result<V> {
        visitor.hashCode()
        return Result.failure(Error.custom("u128 is not supported"))
    }

    /**
     * Hint that the `Deserialize` type is expecting a `Float` value.
     */
    public fun <V> deserializeF32(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Double` value.
     */
    public fun <V> deserializeF64(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a `Char` value.
     */
    public fun <V> deserializeChar(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a string value and does not benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    public fun <V> deserializeStr(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a string value and would benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    public fun <V> deserializeString(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a byte array and does not benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    public fun <V> deserializeBytes(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a byte array and would benefit from taking
     * ownership of buffered data owned by the `Deserializer`.
     */
    public fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting an optional value.
     */
    public fun <V> deserializeOption(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a unit value.
     */
    public fun <V> deserializeUnit(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a unit class with a particular name.
     */
    public fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a newtype class with a particular name.
     */
    public fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a sequence of values.
     */
    public fun <V> deserializeSeq(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a sequence of values and knows how many values
     * there are without looking at the serialized data.
     */
    public fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a tuple class with a particular name and number
     * of fields.
     */
    public fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a map of key-value pairs.
     */
    public fun <V> deserializeMap(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting a class with a particular name and fields.
     */
    public fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting an enum value with a particular name and
     * possible variants.
     */
    public fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type is expecting the name of a class field or the discriminant
     * of an enum variant.
     */
    public fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V>

    /**
     * Hint that the `Deserialize` type needs to deserialize a value whose type doesn't matter
     * because it is ignored.
     */
    public fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V>

    /**
     * Determine whether `Deserialize` implementations should expect to deserialize their
     * human-readable form.
     */
    public fun isHumanReadable(): Boolean = true
}
