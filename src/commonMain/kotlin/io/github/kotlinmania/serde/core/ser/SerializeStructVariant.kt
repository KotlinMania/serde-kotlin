// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeStructVariant`.
 */
interface SerializeStructVariant<Ok, E>
    where E : Error {
    /**
     * Serialize a class variant field.
     */
    fun <T> serializeField(
        key: String,
        value: T,
    ): Result<Unit>
        where T : Serialize

    /**
     * Indicate that a class variant field has been skipped.
     *
     * The default implementation does nothing.
     */
    fun skipField(_: String): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Finish serializing a class variant.
     */
    fun end(): Result<Ok>
}
