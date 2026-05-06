// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeMap`.
 */
public interface SerializeMap<Ok, E>
    where E : Error {
    /**
     * Serialize a map key.
     */
    public fun <T> serializeKey(key: T): Result<Unit>
        where T : Serialize

    /**
     * Serialize a map value.
     */
    public fun <T> serializeValue(value: T): Result<Unit>
        where T : Serialize

    /**
     * Serialize a map entry consisting of a key and a value.
     */
    public fun <K, V> serializeEntry(key: K, value: V): Result<Unit>
        where K : Serialize,
              V : Serialize =
        runCatching {
            serializeKey(key).getOrThrow()
            serializeValue(value).getOrThrow()
        }

    /**
     * Finish serializing a map.
     */
    public fun end(): Result<Ok>
}
