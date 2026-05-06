// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeTupleVariant`.
 */
public interface SerializeTupleVariant<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple variant field.
     */
    public fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple variant.
     */
    public fun end(): Result<Ok>
}
