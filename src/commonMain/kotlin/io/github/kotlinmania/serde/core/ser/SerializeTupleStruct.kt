// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeTupleStruct`.
 */
interface SerializeTupleStruct<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple class field.
     */
    fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple class.
     */
    fun end(): SerdeResult<Ok>
}
