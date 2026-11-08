package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult

/**
 * `VariantAccess` is a visitor that is created by the `Deserializer` and passed to the
 * `Deserialize` to deserialize the content of a particular enum variant.
 */
interface VariantAccess {
    /**
     * Called when deserializing a variant with no values.
     */
    fun unitVariant(): SerdeResult<Unit>

    /**
     * Called when deserializing a variant with a single value.
     */
    fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T>

    /**
     * Called when deserializing a variant with a single value.
     */
    fun <T> newtypeVariant(seed: DeserializeSeed<T>): SerdeResult<T> = newtypeVariantSeed(seed)

    /**
     * Deserializes the single value using a stateless [Deserialize] implementation.
     *
     * This is the convenience form of [newtypeVariantSeed]. Implementations
     * should not override its default behavior.
     */
    fun <T> newtypeVariant(deserialize: Deserialize<T>): SerdeResult<T> =
        newtypeVariantSeed(seedFromDeserialize(deserialize))

    /**
     * Called when deserializing a tuple-like variant.
     */
    fun <V> tupleVariant(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V>

    /**
     * Called when deserializing a struct-like variant.
     */
    fun <V> structVariant(
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V>
}
