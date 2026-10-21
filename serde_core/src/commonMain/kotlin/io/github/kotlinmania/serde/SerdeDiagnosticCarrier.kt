package io.github.kotlinmania.serde

/** Provides the original diagnostic at a Kotlin exception-handling boundary. */
sealed interface SerdeDiagnosticCarrier {
    val error: SerdeError
}
