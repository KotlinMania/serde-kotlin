package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult

/**
 * Provides a `Visitor` access to the data of an enum in the input.
 *
 * `EnumAccess` is created by the `Deserializer` and passed to the `Visitor` in order to identify
 * which variant of an enum to deserialize.
 */
interface EnumAccess {
    /**
     * `variant` is called to identify which variant to deserialize.
     */
    fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>>

    /**
     * Identifies the variant using a stateless [Deserialize] implementation.
     *
     * This is the convenience form of [variantSeed]. Implementations should not
     * override its default behavior. The explicit codec supplies Rust's
     * `V: Deserialize` evidence without constructing a value of `V`.
     */
    fun <V> variant(deserialize: Deserialize<V>): SerdeResult<Pair<V, VariantAccess>> =
        variantSeed(seedFromDeserialize(deserialize))
}
