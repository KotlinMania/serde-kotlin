// port-lint: source serde_core/src/crate_root.rs
package io.github.kotlinmania.serde.core

internal object CrateRoot {
    /**
     * A facade around all the types we need from the `std`, `core`, and `alloc` crates. This avoids
     * elaborate import wrangling having to happen in every module.
     */
    internal object LibFacade

    /**
     * Used by generated code. Not public API.
     */
    internal object PrivateFacade

    /**
     * Used by declarative macro generated code. Not public API.
     */
    internal object DoubleUnderscorePrivate
}

// None of this crate's error handling needs the `From.from` error conversion
// performed implicitly by the `?` operator or the standard library's `try`
// macro. This simplified macro gives a 5.5% improvement in compile time
// compared to standard `try`, and 9% improvement compared to `?`.
internal inline fun <T> tri(
    result: Result<T>,
    onError: (Throwable) -> Nothing,
): T = result.getOrElse(onError)
