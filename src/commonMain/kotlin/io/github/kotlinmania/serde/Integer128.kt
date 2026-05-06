// port-lint: source serde/src/integer128.rs
package io.github.kotlinmania.serde

/*
 * Copyright (c) 2026 Sydney Renee <sydney@solace.ofharmony.ai>
 * and The Solace Project.
 *
 * Licensed under either Apache-2.0 or MIT at your option; see LICENSE-APACHE
 * and LICENSE-MIT.
 */

@Deprecated(
    message = "This is the Kotlin port of Serde's deprecated `serde_if_integer128!` macro. " +
        "It has no effect on any version of Serde released in the past 2 years. " +
        "It was used long ago in crates that needed to support Rustc older than 1.26.0, " +
        "or Emscripten targets older than 1.40.0, which did not yet have 128-bit integer support. " +
        "These days Serde requires a Rust compiler newer than that so 128-bit integers are always supported, " +
        "so this wrapper always executes its block."
)
public inline fun <T> serdeIfInteger128(block: () -> T): T = block()
