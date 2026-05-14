// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Either a re-export of `std::error::Error` or a new identical interface, depending on whether
 * Serde's "std" feature is enabled.
 */
public interface StdError {
    /**
     * The underlying cause of this error, if any.
     */
    public fun source(): StdError? = null
}
