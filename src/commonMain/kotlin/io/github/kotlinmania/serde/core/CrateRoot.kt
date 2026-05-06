// port-lint: source serde_core/src/crate_root.rs
package io.github.kotlinmania.serde.core

/*
 * Copyright (c) 2026 Sydney Renee <sydney@solace.ofharmony.ai>
 * and The Solace Project.
 *
 * Licensed under either Apache-2.0 or MIT at your option; see LICENSE-APACHE
 * and LICENSE-MIT.
 */

/**
 * Kotlin translation of the upstream `crateRoot` macro expansion.
 *
 * The Rust macro emits the `serde_core` crate root: a facade around the types Serde needs from
 * `std`, `core`, and `alloc`, the `tri` helper macro, the `de` and `ser` modules, the generated
 * private modules, and the optional standard error shim.
 *
 * Kotlin has no textual crate-root macro system. The module declarations from this source file are
 * represented directly by the package layout under `io.github.kotlinmania.serde.core`; the helper
 * below preserves the one behavioral macro in this file.
 */
internal object CrateRoot {
    /**
     * A facade around all the types we need from the `std`, `core`, and `alloc` crates.
     *
     * This avoids elaborate import wrangling having to happen in every module. In the Kotlin port,
     * this facade is provided by `Lib`.
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

/**
 * None of this crate's error handling needs the `From.from` error conversion performed implicitly
 * by Rust's `?` operator or the standard library's `try` macro.
 *
 * The upstream macro gives a 5.5% improvement in compile time compared to standard `try`, and 9%
 * improvement compared to `?`. Kotlin does not have macro-based early return from the caller, so
 * callers supply the enclosing error-return path explicitly.
 */
internal inline fun <T> tri(
    result: Result<T>,
    onError: (Throwable) -> Nothing,
): T = result.getOrElse(onError)
