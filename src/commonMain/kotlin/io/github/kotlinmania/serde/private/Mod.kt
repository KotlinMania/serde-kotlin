// port-lint: source serde/src/private/mod.rs
package io.github.kotlinmania.serde.`private`

// Tracking file for upstream `serde/src/private/mod.rs`. The upstream module is composed of
// submodule declarations (gated on `cfg(not(no_serde_derive))`) and `pub use` re-exports.
//
// Kotlin has no module-level re-export mechanism, so this file:
// - Keeps the upstream module boundary (`io.github.kotlinmania.serde.private`) for callers that
//   want to mirror `serde::__private::...` paths used by derived code.
// - Avoids introducing Kotlin `typealias` shims for standard-library names (Clone, Default,
//   Result, Option, etc.). Kotlin callers use the Kotlin standard library directly.
//
// Upstream `pub mod` lines:
//   `pub mod de;` and `pub mod ser;` — the Kotlin ports live in `De.kt` and `Ser.kt` in this
//   package. These are ported from `serde/src/private/de.rs` and `serde/src/private/ser.rs`.
//
// Upstream `pub use` lines:
//   - `crate::serde_core_private::string::from_utf8_lossy` is surfaced here as a forwarder to the
//     serde_core implementation in `io.github.kotlinmania.serde.core.private`.

public fun fromUtf8Lossy(bytes: ByteArray): String =
    io.github.kotlinmania.serde.core.`private`.fromUtf8Lossy(bytes)

