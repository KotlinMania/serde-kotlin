// port-lint: source ser/mod.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeSeq`.
 */
interface SerializeSeq<Ok>
    {
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
