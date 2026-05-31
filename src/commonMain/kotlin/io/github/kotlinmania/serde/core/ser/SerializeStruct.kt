// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeStruct`.
 */
interface SerializeStruct<Ok, E>
    where E : Error {
    /**
     * Serialize a class field.
     */
    fun <T> serializeField(
        key: String,
        value: T,
    ): Result<Unit>
        where T : Serialize

    /**
     * Indicate that a class field has been skipped.
     *
     * The default implementation does nothing.
     */
    fun skipField(key: String): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Finish serializing a class.
     */
    fun end(): Result<Ok>
}
