# port-lint Proposed Changes

**Generated:** 2026-05-14
**Source:** tmp/serde
**Target:** src/commonMain/kotlin/io/github/kotlinmania/serde

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/serde/core/de/OneOf.kt` | `// port-lint: source serde_core/src/de/mod.rs` | `// port-lint: source test_suite/tests/bytes/mod.rs` | `test_suite/tests/bytes/mod.rs` | `port-lint provenance header matched only by basename: 'serde_core/src/de/mod.rs' vs expected 'test_suite/tests/bytes/mod.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/serde/core/de/Visitor.kt` | `// port-lint: source serde_core/src/de/mod.rs` | `// port-lint: source test_suite/tests/macros/mod.rs` | `test_suite/tests/macros/mod.rs` | `port-lint provenance header matched only by basename: 'serde_core/src/de/mod.rs' vs expected 'test_suite/tests/macros/mod.rs'` |
