package io.github.kotlinmania.serdecore.de

/** The next value in a sequence, or the absence of further values. */
sealed class SeqElement<out T> {
    /** A value in the sequence, which may itself be null. */
    data class Some<out T>(val value: T) : SeqElement<T>()

    /** There are no more remaining items. */
    data object None : SeqElement<Nothing>()
}

/** Returns the contained value or computes a default when there are no more items. */
inline fun <T> SeqElement<T>.unwrapOrElse(defaultValue: () -> T): T =
    when (this) {
        is SeqElement.Some -> value
        SeqElement.None -> defaultValue()
    }
