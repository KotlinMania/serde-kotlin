package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult

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
     * This returns `SerdeResult.success(key)` for the next key in the map, or `SerdeResult.success(null)` if
     * there are no more remaining entries.
     */
    fun <K> nextKey(deserialize: Deserialize<K>): SerdeResult<K?> =
        nextKeySeed(seedFromDeserialize(deserialize))

    /**
     * This returns a `SerdeResult.success(value)` for the next value in the map.
     */
    fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V>

    /**
     * This returns a `SerdeResult.success(value)` for the next value in the map.
     */
    fun <V> nextValue(deserialize: Deserialize<V>): SerdeResult<V> =
        nextValueSeed(seedFromDeserialize(deserialize))

    /**
     * This returns `SerdeResult.success(Pair(key, value))` for the next key-value pair in the map, or
     * `SerdeResult.success(null)` if there are no more remaining items.
     */
    fun <K, V> nextEntrySeed(
        keySeed: DeserializeSeed<K>,
        valueSeed: DeserializeSeed<V>,
    ): SerdeResult<Pair<K, V>?> =
        // Rust's Option<K> distinguishes a present nullable K from exhaustion.
        // Keep that tag through the legacy nullable nextKeySeed boundary by
        // asking the original key seed for a non-null wrapper around its result.
        nextKeySeed(PresentMapKeySeed(keySeed)).flatMap { key ->
            if (key == null) {
                SerdeResult.success(null)
            } else {
                nextValueSeed(valueSeed).map { value -> key.value to value }
            }
        }

    /**
     * This returns `SerdeResult.success(Pair(key, value))` for the next key-value pair in the map, or
     * `SerdeResult.success(null)` if there are no more remaining items.
     */
    fun <K, V> nextEntry(
        keyDeserialize: Deserialize<K>,
        valueDeserialize: Deserialize<V>,
    ): SerdeResult<Pair<K, V>?> =
        nextEntrySeed(
            seedFromDeserialize(keyDeserialize),
            seedFromDeserialize(valueDeserialize),
        )

    /**
     * Returns the number of entries remaining in the map, if known.
     */
    fun sizeHint(): Int? = null
}

private class PresentMapKey<K>(
    val value: K,
)

private class PresentMapKeySeed<K>(
    private val seed: DeserializeSeed<K>,
) : DeserializeSeed<PresentMapKey<K>> {
    override fun <D> deserialize(deserializer: D): SerdeResult<PresentMapKey<K>>
        where D : Deserializer = seed.deserialize(deserializer).map { PresentMapKey(it) }
}
