// port-lint: source src/integer128.rs
package io.github.kotlinmania.serde

/**
 * This is the Kotlin equivalent of Serde's `serde_if_integer128!` macro.
 *
 * In upstream Rust, this macro expands to its input tokens unconditionally and
 * is deprecated because all supported Rust targets have 128-bit integers.
 *
 * Kotlin always has `Long` but not a native `i128`/`u128`; this wrapper is still
 * kept for parity with the upstream API surface.
 */
@Deprecated(
    message = "This macro has no effect on any version of Serde released in the past 2 years. " +
        "It was used long ago in crates that needed to support Rustc older than 1.26.0, " +
        "or Emscripten targets older than 1.40.0, which did not yet have 128-bit integer support. " +
        "These days Serde requires a Rust compiler newer than that so 128-bit integers are always supported."
)
public inline fun serdeIfInteger128(block: () -> Unit) {
    block()
}
