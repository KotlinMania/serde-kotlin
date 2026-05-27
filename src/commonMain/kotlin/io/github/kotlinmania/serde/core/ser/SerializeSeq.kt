// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeSeq`.
 */
interface SerializeSeq<Ok, E>
    where E : Error {
    /**
     * Serialize a sequence element.
     */
    fun <T> serializeElement(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a sequence.
     */
    fun end(): Result<Ok>
}
