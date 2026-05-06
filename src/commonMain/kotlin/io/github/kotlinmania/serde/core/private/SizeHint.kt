// port-lint: source core/private/size_hint.rs
package io.github.kotlinmania.serde.core.`private`

/*
 * Copyright (c) 2025 Sydney Renee, The Solace Project
 *
 * This source code is dual-licensed under either the MIT license found in the
 * LICENSE-MIT file in the root directory of this source tree or the Apache
 * License, Version 2.0 found in the LICENSE-APACHE file in the root directory
 * of this source tree. You may select, at your option, one of the
 * above-listed licenses.
 */

public interface SizeHintedIterator<out T> : Iterator<T> {
    public fun sizeHint(): Pair<Int, Int?>
}

public fun fromBounds(iter: SizeHintedIterator<*>): Int? = helper(iter.sizeHint())

public inline fun <reified Element> cautious(hint: Int?): Int {
    val maxPreallocBytes = 1024 * 1024

    val elementSizeBytes = elementSizeBytes<Element>()
    if (elementSizeBytes == 0) {
        return 0
    }

    return minOf(
        hint ?: 0,
        maxPreallocBytes / elementSizeBytes,
    )
}

private fun helper(bounds: Pair<Int, Int?>): Int? {
    val (lower, upper) = bounds
    return if (upper != null && lower == upper) upper else null
}

@PublishedApi
internal inline fun <reified Element> elementSizeBytes(): Int =
    when (Element::class) {
        Boolean::class -> 1
        Byte::class -> 1
        UByte::class -> 1
        Short::class -> 2
        UShort::class -> 2
        Char::class -> 2
        Int::class -> 4
        UInt::class -> 4
        Float::class -> 4
        Long::class -> 8
        ULong::class -> 8
        Double::class -> 8
        else -> 8
    }
