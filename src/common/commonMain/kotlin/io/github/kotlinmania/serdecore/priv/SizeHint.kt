// port-lint: source private/size_hint.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serdecore.priv.generated.sizehint.RtkBound0 as GeneratedSizeHintBound
import io.github.kotlinmania.serdecore.priv.generated.sizehint.RtkOption as GeneratedSizeHintOption
import io.github.kotlinmania.serdecore.priv.generated.sizehint.cautious as generatedCautious
import io.github.kotlinmania.serdecore.priv.generated.sizehint.from_bounds as generatedFromBounds

interface IteratorWithSizeHint<out T> : Iterator<T> {
    fun sizeHint(): Pair<ULong, ULong?>
}

fun fromBounds(iter: IteratorWithSizeHint<*>): ULong? =
    when (val bounds = generatedFromBounds(iter, GeneratedSizeHintBound.Case0<IteratorWithSizeHint<*>>())) {
        GeneratedSizeHintOption.None -> null
        is GeneratedSizeHintOption.Some -> bounds.value
    }

/**
 * Explicit Rust layout entry point for the selected aarch64-apple-darwin/rustc
 * 7189d39d9ce936881b8132c6addeaf3e295cd455 profile. The caller supplies the exact
 * Rust Element size; this function does not infer a Kotlin class or host layout.
 */
fun cautious(hint: ULong?, rustElementSizeBytes: ULong): ULong =
    generatedCautious(
        if (hint == null) GeneratedSizeHintOption.None else GeneratedSizeHintOption.Some(hint),
        rustElementSizeBytes,
    )

/**
 * Kept for source compatibility. An open Kotlin type does not establish Rust layout.
 * Supply the exact Rust size to the unsigned overload instead.
 */
inline fun <reified Element> cautious(hint: Int?): Int {
    require(hint == null || hint >= 0) { "A Rust size hint cannot be negative" }
    return sizeOf<Element>()
}

@PublishedApi
internal inline fun <reified Element> sizeOf(): Int =
    throw IllegalArgumentException("Missing Rust Element layout evidence; use cautious(hint, rustElementSizeBytes)")
