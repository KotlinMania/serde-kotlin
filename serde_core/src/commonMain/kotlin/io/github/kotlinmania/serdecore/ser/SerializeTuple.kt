// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeTuple`.
 */
interface SerializeTuple<Ok>
    {
    /**
     * Serialize a tuple element.
     */
    fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple.
     */
    fun end(): SerdeResult<Ok>
}
