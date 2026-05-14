// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Returned from `Serializer.serializeStruct`.
 */
public interface SerializeStruct<Ok, E>
    where E : Error {
    /**
     * Serialize a class field.
     */
    public fun <T> serializeField(key: String, value: T): Result<Unit>
        where T : Serialize

    /**
     * Indicate that a class field has been skipped.
     *
     * The default implementation does nothing.
     */
    public fun skipField(key: String): Result<Unit> {
        key.hashCode()
        return Result.success(Unit)
    }

    /**
     * Finish serializing a class.
     */
    public fun end(): Result<Ok>
}
