// port-lint: source integer128.rs
package io.github.kotlinmania.serde

/*
 * Copyright (c) 2025 Sydney Renee, The Solace Project
 *
 * This source code is dual-licensed under either the MIT license found in the
 * LICENSE-MIT file in the root directory of this source tree or the Apache
 * License, Version 2.0 found in the LICENSE-APACHE file in the root directory
 * of this source tree. You may select, at your option, one of the
 * above-listed licenses.
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
