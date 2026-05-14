// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * Provides a `Visitor` access to each element of a sequence in the input.
 *
 * This is an interface that a `Deserializer` passes to a `Visitor` implementation, which
 * deserializes each item in a sequence.
 */
public interface SeqAccess {
    /**
     * This returns `Result.success(value)` for the next value in the sequence, or
     * `Result.success(null)` if there are no more remaining items.
     */
    public fun <T> nextElementSeed(seed: DeserializeSeed<T>): Result<T?>

    /**
     * This returns `Result.success(value)` for the next value in the sequence, or
     * `Result.success(null)` if there are no more remaining items.
     */
    public fun <T> nextElement(seed: DeserializeSeed<T>): Result<T?> =
        nextElementSeed(seed)

    /**
     * Returns the number of elements remaining in the sequence, if known.
     */
    public fun sizeHint(): Int? = null
}
