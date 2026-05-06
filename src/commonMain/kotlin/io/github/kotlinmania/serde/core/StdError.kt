// port-lint: source src/core/std_error.rs
package io.github.kotlinmania.serde.core

/*
 * Copyright (c) 2026 Sydney Renee <sydney@solace.ofharmony.ai>
 * and The Solace Project.
 *
 * Licensed under the Apache License, Version 2.0. See LICENSE and NOTICE.
 *
 * This Kotlin source is a port of upstream Serde code, which is licensed
 * under either Apache-2.0 or MIT at your option; see LICENSE-APACHE and
 * LICENSE-MIT.
 */

/**
 * Either a re-export of `std::error::Error` or a new identical trait, depending on whether Serde's
 * "std" feature is enabled.
 *
 * Serde's error traits `serde::ser::Error` and `serde::de::Error` require `std::error::Error` as a
 * supertrait, but only when Serde is built with "std" enabled. Data formats that don't care about
 * no_std support should generally provide their error types with a `std::error::Error` implementation
 * directly:
 *
 * ```kotlin
 * // We don't support no_std!
 * class MySerError : io.github.kotlinmania.serde.core.ser.Error
 * ```
 *
 * Data formats that *do* support no_std may either have a "std" feature of their own:
 *
 * ```toml
 * [features]
 * std = ["serde/std"]
 * ```
 *
 * ... or else provide the std Error implementation unconditionally via Serde's re-export:
 *
 * ```kotlin
 * class MySerError : io.github.kotlinmania.serde.core.ser.StdError
 * ```
 */
internal interface Error : Lib.Debug, Lib.Display {
    /**
     * The underlying cause of this error, if any.
     */
    fun source(): Error? = null
}

