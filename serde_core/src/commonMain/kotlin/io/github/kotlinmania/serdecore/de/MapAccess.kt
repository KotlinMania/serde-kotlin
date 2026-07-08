// port-lint: source de/mod.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching

/**
 * Provides a `Visitor` access to each entry of a map in the input.
 *
 * This is an interface that a `Deserializer` passes to a `Visitor` implementation.
 */
interface MapAccess {
    /**
     * This returns `SerdeResult.success(key)` for the next key in the map, or `SerdeResult.success(null)` if
     * there are no more remaining entries.
     */
    fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?>

    /**
     * This returns a `SerdeResult.success(value)` for the next value in the map.
     */
    fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V>

    /**
     * This returns `SerdeResult.success(Pair(key, value))` for the next key-value pair in the map, or
     * `SerdeResult.success(null)` if there are no more remaining items.
     */
    fun <K, V> nextEntrySeed(
        keySeed: DeserializeSeed<K>,
        valueSeed: DeserializeSeed<V>,
    ): SerdeResult<Pair<K, V>?> =
        serdeCatching {
            val key = nextKeySeed(keySeed).getOrThrow()
            if (key == null) {
                null
            } else {
                key to nextValueSeed(valueSeed).getOrThrow()
            }
        }

    /**
     * Returns the number of entries remaining in the map, if known.
     */
    fun sizeHint(): Int? = null
}
