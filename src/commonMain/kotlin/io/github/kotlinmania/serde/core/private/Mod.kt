// port-lint: source serde_core/src/private/mod.rs
package io.github.kotlinmania.serde.core.`private`

// The upstream module declares `content` only when Serde's derive support is available and either
// the `std` or `alloc` feature is enabled.

// FIXME: Upstream has `#[cfg(doctest)]` once https://github.com/rust-lang/rust/issues/67295 is fixed.

// `pub use crate::lib::result::Result;`
public typealias Result<T> = kotlin.Result<T>
