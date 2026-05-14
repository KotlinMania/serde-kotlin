// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeTuple`.
 */
public interface SerializeTuple<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple element.
     */
    public fun <T> serializeElement(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple.
     */
    public fun end(): Result<Ok>
}
