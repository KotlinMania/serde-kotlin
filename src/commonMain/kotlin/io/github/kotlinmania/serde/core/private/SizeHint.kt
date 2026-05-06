// port-lint: source serde_core/src/private/size_hint.rs
package io.github.kotlinmania.serde.core.`private`

public interface IteratorWithSizeHint<out T> : Iterator<T> {
    public fun sizeHint(): Pair<Int, Int?>
}

public fun fromBounds(iter: IteratorWithSizeHint<*>): Int? {
    return helper(iter.sizeHint())
}

public inline fun <reified Element> cautious(hint: Int?): Int {
    val maxPreallocBytes = 1024 * 1024

    return if (sizeOf<Element>() == 0) {
        0
    } else {
        minOf(
            hint ?: 0,
            maxPreallocBytes / sizeOf<Element>(),
        )
    }
}

private fun helper(bounds: Pair<Int, Int?>): Int? {
    val (lower, upper) = bounds
    return when {
        upper != null && lower == upper -> upper
        else -> null
    }
}

@PublishedApi
internal inline fun <reified Element> sizeOf(): Int = 8
