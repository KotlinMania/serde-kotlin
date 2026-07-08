// port-lint: source de/mod.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching

/**
 * A **data structure** that can be deserialized from any data format supported by Serde.
 */
interface Deserialize<T> {
    /**
     * Deserialize this value from the given Serde deserializer.
     */
    fun <D> deserialize(deserializer: D): SerdeResult<T>
        where D : Deserializer

    /**
     * Deserializes a value into `self` from the given Deserializer.
     *
     * The purpose of this method is to allow the deserializer to reuse resources and avoid copies.
     * As such, if this method returns an error, `self` will be in an indeterminate state where some
     * parts of the struct have been overwritten. Although whatever state that is will be
     * memory-safe.
     */
    fun <D> deserializeInPlace(
        deserializer: D,
        place: (T) -> Unit,
    ): SerdeResult<Unit>
        where D : Deserializer =
        serdeCatching {
            place(deserialize(deserializer).getOrThrow())
        }
}
