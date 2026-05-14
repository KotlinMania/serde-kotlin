// port-lint: source serde_core/src/macros.rs
package io.github.kotlinmania.serde.core

import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.Visitor

// Super explicit first paragraph because this shows up at the top level and
// trips up people who are just looking for basic Serialize / Deserialize
// documentation.
/**
 * Helper when implementing the `Deserializer` part of a new data format for Serde.
 *
 * Some `Deserializer` implementations for self-describing formats do not care what hint the
 * `Visitor` gives them, they just want to blindly call the `Visitor` method corresponding to the
 * data they can tell is in the input. This requires repetitive implementations of all the
 * `Deserializer` interface methods.
 *
 * In Rust, Serde provides a `forward_to_deserialize_any!` macro to generate these forwarding
 * methods so that they forward directly to `Deserializer.deserializeAny`. Kotlin does not have
 * macros, so the Kotlin port provides an interface that supplies default implementations.
 *
 * Implement `deserializeAny` and inherit the rest of the `Deserializer` methods from this
 * interface.
 */
public interface ForwardToDeserializeAnyDeserializer : Deserializer {
    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> {
        return deserializeAny(visitor)
    }

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> {
        return deserializeAny(visitor)
    }

    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> {
        return deserializeAny(visitor)
    }

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> {
        return deserializeAny(visitor)
    }

    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> {
        return deserializeAny(visitor)
    }

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> {
        return deserializeAny(visitor)
    }

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
}
