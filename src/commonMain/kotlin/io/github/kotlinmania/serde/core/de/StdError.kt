// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * Either a re-export of `std::error::Error` or a new identical interface, depending on whether
 * Serde's "std" feature is enabled.
 */
interface StdError {
    /**
     * The underlying cause of this error, if any.
     */
    fun source(): StdError? = null
}
