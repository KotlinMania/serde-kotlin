// port-lint: source de/mod.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult

/**
 * Provides a `Visitor` access to each element of a sequence in the input.
 *
 * This is an interface that a `Deserializer` passes to a `Visitor` implementation, which
 * deserializes each item in a sequence.
 */
interface SeqAccess {
    /**
     * This returns `SerdeResult.success(value)` for the next value in the sequence, or
     * `SerdeResult.success(null)` if there are no more remaining items.
     */
    fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?>

    /**
     * This returns `SerdeResult.success(value)` for the next value in the sequence, or
     * `SerdeResult.success(null)` if there are no more remaining items.
     */
    fun <T> nextElement(seed: DeserializeSeed<T>): SerdeResult<T?> = nextElementSeed(seed)

    /**
     * Returns the number of elements remaining in the sequence, if known.
     */
    fun sizeHint(): Int? = null
}
