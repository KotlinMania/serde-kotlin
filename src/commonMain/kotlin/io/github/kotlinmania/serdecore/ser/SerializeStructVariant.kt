// port-lint: source ser/mod.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult

/**
 * Returned from `Serializer.serializeStructVariant`.
 */
interface SerializeStructVariant<Ok>
    {
    /**
     * Serialize a class variant field.
     */
    fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize

    /**
     * Indicate that a class variant field has been skipped.
     *
     * The default implementation does nothing.
     */
    fun skipField(key: String): SerdeResult<Unit> = SerdeResult.success(Unit)

    /**
     * Finish serializing a class variant.
     */
    fun end(): SerdeResult<Ok>
}
