// port-lint: source ser/mod.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeMap`.
 */
interface SerializeMap<Ok>
    {
    /**
     * Serialize a map key.
     */
    fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize

    /**
     * Serialize a map value.
     */
    fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize

    /**
     * Serialize a map entry consisting of a key and a value.
     */
    fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize =
        serializeKey(key).flatMap { serializeValue(value) }

    /**
     * Finish serializing a map.
     */
    fun end(): SerdeResult<Ok>
}
