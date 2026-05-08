// port-lint: source serde_core/src/std_error.rs
package io.github.kotlinmania.serde.core

/**
 * Either a re-export of the standard error trait or a new identical trait, depending on whether
 * Serde's "std" feature is enabled.
 *
 * Serde's serialization and deserialization error traits require the standard error trait as a
 * supertrait, but only when Serde is built with "std" enabled. Data formats that don't care about
 * standard-library-free support should generally provide their error types with a standard error
 * implementation directly:
 *
 * ```kotlin
 * // This format requires the standard library.
 * class MySerError : io.github.kotlinmania.serde.core.ser.Error
 * ```
 *
 * Data formats that *do* support standard-library-free use may either have a "std" feature of
 * their own:
 *
 * ```toml
 * [features]
 * std = ["serde/std"]
 * ```
 *
 * ... or else provide the standard error implementation unconditionally via Serde's re-export:
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
