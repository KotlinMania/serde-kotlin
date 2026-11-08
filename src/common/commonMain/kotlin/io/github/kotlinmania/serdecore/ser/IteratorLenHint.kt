// port-lint: source ser/mod.rs:iterator_len_hint
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serdecore.priv.IteratorWithSizeHint
import io.github.kotlinmania.serdecore.priv.fromBounds

/** Uses the actual iterator's exact Rust bounds, when that evidence is available. */
internal fun iteratorLenHint(iterator: Iterator<*>): Int? {
    if (iterator !is IteratorWithSizeHint<*>) return null
    val length = fromBounds(iterator) ?: return null
    // The existing Serializer length API is Int-based. A larger Rust usize is
    // unrepresentable here; never wrap it into a negative or incorrect length.
    return if (length <= Int.MAX_VALUE.toULong()) length.toInt() else null
}

internal fun iteratorLenHint(iterator: Iterator<*>, source: Iterable<*>): Int? =
    if (iterator is IteratorWithSizeHint<*>) {
        iteratorLenHint(iterator)
    } else {
        // Kotlin's standard collection iterators have no size_hint method.
        // Retain the collection-size adapter only when iterator evidence is absent.
        if (source is Collection<*>) source.size else null
    }
