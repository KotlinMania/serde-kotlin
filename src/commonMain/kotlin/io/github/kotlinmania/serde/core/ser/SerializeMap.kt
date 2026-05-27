// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeMap`.
 */
interface SerializeMap<Ok, E>
    where E : Error {
    /**
     * Serialize a map key.
     */
    fun <T> serializeKey(key: T): Result<Unit>
        where T : Serialize

    /**
     * Serialize a map value.
     */
    fun <T> serializeValue(value: T): Result<Unit>
        where T : Serialize

    /**
     * Serialize a map entry consisting of a key and a value.
     */
    fun <K, V> serializeEntry(
        key: K,
        value: V,
    ): Result<Unit>
        where K : Serialize,
              V : Serialize =
        runCatching {
            serializeKey(key).getOrThrow()
            serializeValue(value).getOrThrow()
        }

    /**
     * Finish serializing a map.
     */
    fun end(): Result<Ok>
}
