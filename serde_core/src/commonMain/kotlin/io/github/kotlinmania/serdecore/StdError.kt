// port-lint: source std_error.rs
package io.github.kotlinmania.serdecore

/**
 * Common superinterface for serde error types.
 *
 * Data formats can implement this directly when they need a serde-shaped error
 * hierarchy without exposing platform exception types through the public API.
 */
interface StdError {
    /**
     * The underlying cause of this error, if any.
     */
    fun source(): StdError? = null
}
