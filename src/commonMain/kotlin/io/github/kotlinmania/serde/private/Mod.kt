// port-lint: source serde/src/private/mod.rs
package io.github.kotlinmania.serde.`private`

// Tracking file for upstream serde/src/private/mod.rs.
//
// Upstream module declarations (translated from Rust cfg attributes):
//   Module `de` is included when serde derive is enabled.
//   Module `ser` is included when serde derive is enabled.
//
// Upstream re-exports (translated from Rust use paths):
//   Re-exports Clone, From, Into, TryFrom, Default, Formatter, PhantomData
//   Re-exports Option, None, Some, Result, Err, Ok, ptr
//   Re-exports fromUtf8Lossy from the serdeCore private module
//   When alloc or std features are enabled: re-exports ToString and Vec
//
// Callers migrated:
// (none)
