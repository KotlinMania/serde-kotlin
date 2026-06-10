// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeTupleVariant`.
 */
interface SerializeTupleVariant<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple variant field.
     */
    fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple variant.
     */
    fun end(): SerdeResult<Ok>
}
