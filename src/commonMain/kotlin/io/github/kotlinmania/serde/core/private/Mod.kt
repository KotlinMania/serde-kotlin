// port-lint: source serde_core/src/private/mod.rs
package io.github.kotlinmania.serde.core.`private`

// Tracking file for upstream `serde_core/src/private/mod.rs`. The upstream module is composed
// entirely of submodule declarations and `pub use` re-exports; per the workspace rule on
// `mod.rs` re-exports (CLAUDE.md `## Re-exports from upstream `mod.rs` files`), no Kotlin
// `typealias` is introduced. Callers reach the upstream symbol directly via Kotlin
// auto-import (for stdlib targets) or explicit `import <path> as <name>` aliasing (for
// non-stdlib targets).

// The upstream module declares `content` only when Serde's derive support is available and
// either the `std` or `alloc` feature is enabled.

// FIXME: Upstream has `#[cfg(doctest)]` once https://github.com/rust-lang/rust/issues/67295 is fixed.

// Upstream `pub use` lines:
//   `pub use self::content::Content;`     — Content lives in Content.kt in this same package.
//                                           Already accessible by name from any caller in
//                                           `io.github.kotlinmania.serde.core.private` and
//                                           by qualified import elsewhere; no synthetic alias.
//   `pub use self::seed::InPlaceSeed;`    — InPlaceSeed lives in Seed.kt in this same package.
//                                           Same reasoning as Content.
//   `pub use crate::lib::result::Result;` — resolves to `kotlin.Result`, auto-imported.

// Callers migrated:
//   (none — workspace audit confirmed zero Kotlin callers held a direct or wildcard import of
//    `io.github.kotlinmania.serde.core.private.Result` at the time the typealias was retired.
//    The only same-package consumer was Seed.kt; with the typealias gone, its bare `Result<Unit>`
//    resolves to the auto-imported `kotlin.Result<Unit>` — same target, no source change.)

// Projected callers (Rust):
//   workspace_dep_graph.json shows zero kotlinmania repos importing
//   `serde_core::private::Result`, `serde_core::private::Content`, or
//   `serde_core::private::InPlaceSeed` from upstream Rust. Future Kotlin ports of any
//   downstream serde consumer should target the upstream symbol directly:
//     - `kotlin.Result` (or its public re-export from `serde::de`) for `Result`
//     - `io.github.kotlinmania.serde.core.private.Content` for `Content` (when derive support is
//       enabled — gated on the same `cfg` as upstream)
//     - `io.github.kotlinmania.serde.core.private.InPlaceSeed` for `InPlaceSeed`
