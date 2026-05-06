// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * Provides a `Visitor` access to each entry of a map in the input.
 *
 * This is an interface that a `Deserializer` passes to a `Visitor` implementation.
 */
public interface MapAccess {
    /**
     * This returns `Result.success(key)` for the next key in the map, or `Result.success(null)` if
     * there are no more remaining entries.
     */
    public fun <K> nextKeySeed(seed: DeserializeSeed<K>): Result<K?>

    /**
     * This returns a `Result.success(value)` for the next value in the map.
     */
    public fun <V> nextValueSeed(seed: DeserializeSeed<V>): Result<V>

    /**
     * This returns `Result.success(Pair(key, value))` for the next key-value pair in the map, or
     * `Result.success(null)` if there are no more remaining items.
     */
    public fun <K, V> nextEntrySeed(keySeed: DeserializeSeed<K>, valueSeed: DeserializeSeed<V>): Result<Pair<K, V>?> =
        runCatching {
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
    public fun sizeHint(): Int? = null
}
