// port-lint: source crate_root.rs
package io.github.kotlinmania.serdecore

import io.github.kotlinmania.serde.SerdeResult

/**
 * Shared crate-root helpers used by the Serde core port.
 *
 * Upstream expands a crate-root macro into imports, module declarations, hidden private exports,
 * and a fast result-propagation helper. Kotlin has explicit imports and Gradle source sets in place
 * of the macro-generated module tree, so this file carries the portable helper behavior.
 */
internal object CrateRoot {
    /**
     * Applies [onSuccess] to a successful result, or propagates a failure unchanged.
     *
     * This is the Kotlin equivalent of Serde's internal fast result-propagation helper: callers can
     * keep the concrete [SerdeResult] error without converting through exceptions.
     */
    inline fun <T, R> tri(
        result: SerdeResult<T>,
        onSuccess: (T) -> SerdeResult<R>,
    ): SerdeResult<R> =
        when (result) {
            is SerdeResult.Success -> onSuccess(result.value)
            is SerdeResult.Failure -> result
        }
}
