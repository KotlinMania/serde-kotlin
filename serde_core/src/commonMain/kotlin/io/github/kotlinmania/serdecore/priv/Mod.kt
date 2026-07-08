// port-lint: source private/mod.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeResult

// Tracking file for the upstream private module. The upstream module is composed entirely of
// submodule declarations and re-exports, so no Kotlin type aliases are introduced here. Callers use
// the direct Kotlin symbol or an explicit import alias when they need the upstream spelling.

// The upstream module declares content support only when Serde's derive support is available and
// collection allocation is enabled.

// Callers migrated:
//   (none; workspace audit confirmed zero Kotlin callers held a direct or wildcard import of the
//    retired private Result alias. Seed.kt now imports SerdeResult directly.)

// Projected callers:
//   workspace_dep_graph.json shows zero kotlinmania repos importing
//   the private Result, Content, or InPlaceSeed re-exports. Kotlin ports of downstream serde
//   consumers should target the upstream symbol directly:
//     - `kotlin.Result` (or its public re-export from `serde::de`) for `Result`
//     - `io.github.kotlinmania.serdecore.private.Content` for `Content` (when derive support is
//       enabled — gated on the same `cfg` as upstream)
//     - `io.github.kotlinmania.serdecore.private.InPlaceSeed` for `InPlaceSeed`
