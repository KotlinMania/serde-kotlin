// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeStruct`.
 */
interface SerializeStruct<Ok>
    {
    /**
     * Serialize a class field.
     */
    fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize

    /**
     * Indicate that a class field has been skipped.
     *
     * The default implementation does nothing.
     */
    fun skipField(key: String): SerdeResult<Unit> = SerdeResult.success(Unit)

    /**
     * Finish serializing a class.
     */
    fun end(): SerdeResult<Ok>
}
