// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeSeq`.
 */
interface SerializeSeq<Ok, E>
    where E : Error {
    /**
     * Serialize a sequence element.
     */
    fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize

    /**
     * Finish serializing a sequence.
     */
    fun end(): SerdeResult<Ok>
}
