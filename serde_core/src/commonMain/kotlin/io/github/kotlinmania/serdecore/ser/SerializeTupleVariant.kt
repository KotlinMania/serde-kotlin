// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeTupleVariant`.
 */
interface SerializeTupleVariant<Ok>
    {
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
