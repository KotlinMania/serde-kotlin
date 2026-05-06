// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * A **data structure** that can be deserialized from any data format supported by Serde.
 */
public interface Deserialize<T> {
    /**
     * Deserialize this value from the given Serde deserializer.
     */
    public fun <D> deserialize(deserializer: D): Result<T>
        where D : Deserializer

    /**
     * Deserializes a value into `self` from the given Deserializer.
     *
     * The purpose of this method is to allow the deserializer to reuse resources and avoid copies.
     * As such, if this method returns an error, `self` will be in an indeterminate state where some
     * parts of the struct have been overwritten. Although whatever state that is will be
     * memory-safe.
     */
    public fun <D> deserializeInPlace(deserializer: D, place: (T) -> Unit): Result<Unit>
        where D : Deserializer =
        runCatching {
            place(deserialize(deserializer).getOrThrow())
        }
}
