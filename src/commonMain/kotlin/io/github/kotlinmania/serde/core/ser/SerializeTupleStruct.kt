// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeTupleStruct`.
 */
interface SerializeTupleStruct<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple class field.
     */
    fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple class.
     */
    fun end(): Result<Ok>
}
