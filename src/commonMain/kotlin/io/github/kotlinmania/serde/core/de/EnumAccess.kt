// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * Provides a `Visitor` access to the data of an enum in the input.
 *
 * `EnumAccess` is created by the `Deserializer` and passed to the `Visitor` in order to identify
 * which variant of an enum to deserialize.
 */
public interface EnumAccess {
    /**
     * `variant` is called to identify which variant to deserialize.
     */
    public fun <V> variantSeed(seed: DeserializeSeed<V>): Result<Pair<V, VariantAccess>>
}
