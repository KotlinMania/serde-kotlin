// port-lint: source serde_core/src/private/mod.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeResult

// Tracking file for upstream `serde_core/src/private/mod.rs`. The upstream module is composed
// entirely of submodule declarations and re-exports; per the workspace rule on
// `mod.rs` re-exports (CLAUDE.md `## Re-exports from upstream `mod.rs` files`), no Kotlin
// `typealias` is introduced. Callers reach the upstream symbol directly via Kotlin
// auto-import (for stdlib targets) or explicit `import <path> as <name>` aliasing (for
// non-stdlib targets).

// The upstream module declares `content` only when Serde's derive support is available and
// either the `std` or `alloc` feature is enabled.

// FIXME: Upstream gates doctest support on a resolved compiler issue.

// Callers migrated:
//   (none — workspace audit confirmed zero Kotlin callers held a direct or wildcard import of
//    `io.github.kotlinmania.serdecore.private.Result` at the time the typealias was retired.
//    The only same-package consumer was Seed.kt; with the typealias gone, its bare `SerdeResult<Unit>`

//    resolves to the auto-imported `kotlin.SerdeResult<Unit>` — same target, no source change.)

// Projected callers:
//   workspace_dep_graph.json shows zero kotlinmania repos importing
//   `serde_core::private::Result`, `serde_core::private::Content`, or
//   `serde_core::private::InPlaceSeed` from upstream. Future Kotlin ports of any
//   downstream serde consumer should target the upstream symbol directly:
//     - `kotlin.Result` (or its public re-export from `serde::de`) for `Result`
//     - `io.github.kotlinmania.serdecore.private.Content` for `Content` (when derive support is
//       enabled — gated on the same `cfg` as upstream)
//     - `io.github.kotlinmania.serdecore.private.InPlaceSeed` for `InPlaceSeed`
