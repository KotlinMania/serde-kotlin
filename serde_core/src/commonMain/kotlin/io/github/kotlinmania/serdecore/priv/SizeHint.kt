// port-lint: source private/size_hint.rs
package io.github.kotlinmania.serdecore.priv

interface IteratorWithSizeHint<out T> : Iterator<T> {
    fun sizeHint(): Pair<Int, Int?>
}

fun fromBounds(iter: IteratorWithSizeHint<*>): Int? = helper(iter.sizeHint())

inline fun <reified Element> cautious(hint: Int?): Int {
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
internal inline fun <reified Element> sizeOf(): Int =
    when (Element::class) {
        Unit::class -> 0
        Boolean::class, Byte::class, UByte::class -> 1
        Short::class, UShort::class -> 2
        Int::class, UInt::class, Float::class, Char::class -> 4
        Long::class, ULong::class, Double::class -> 8
        else -> 8
    }
