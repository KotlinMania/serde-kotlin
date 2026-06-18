# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 0/208 (0.0%)
- **Function parity:** 0/1665 matched — 0.0%
- **Class/type parity:** 0/667 matched — 0.0%
- **Combined symbol parity:** 0/2332 matched — 0.0%
- **Average inline-code cosine:** 0.00 (function body across 0 matched files)
- **Average documentation cosine:** 0.00 (doc text across 0 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 0 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `serde.lib` | `serde.serde.src.Lib` | 0 | `serde/serde/src/lib.rs` | `serde/serde/src/Lib.kt` |
| `private.mod` | `serde.serde.src.private.Mod` | 0 | `serde/serde/src/private/mod.rs` | `serde/serde/src/private/Mod.kt` |
| `de.mod` | `serde.serdecore.src.de.Mod` | 0 | `serde/serde_core/src/de/mod.rs` | `serde/serdecore/src/de/Mod.kt` |
| `serde_core.lib` | `serde.serdecore.src.Lib` | 0 | `serde/serde_core/src/lib.rs` | `serde/serdecore/src/Lib.kt` |
| `serde.serde_core.private.mod` | `serde.serdecore.src.private.Mod` | 0 | `serde/serde_core/src/private/mod.rs` | `serde/serdecore/src/private/Mod.kt` |
| `ser.mod` | `serde.serdecore.src.ser.Mod` | 0 | `serde/serde_core/src/ser/mod.rs` | `serde/serdecore/src/ser/Mod.kt` |
| `internals.mod` | `serde.serdederive.src.internals.Mod` | 0 | `serde/serde_derive/src/internals/mod.rs` | `serde/serdederive/src/internals/Mod.kt` |
| `serde_derive.lib` | `serde.serdederive.src.Lib` | 0 | `serde/serde_derive/src/lib.rs` | `serde/serdederive/src/Lib.kt` |
| `serde_derive_internals.lib` | `serde.serdederiveinternals.Lib` | 0 | `serde/serde_derive_internals/lib.rs` | `serde/serdederiveinternals/Lib.kt` |
| `bytes.mod` | `serde.testsuite.tests.bytes.Mod` | 0 | `serde/test_suite/tests/bytes/mod.rs` | `serde/testsuite/tests/bytes/Mod.kt` |
| `macros.mod` | `serde.testsuite.tests.macros.Mod` | 0 | `serde/test_suite/tests/macros/mod.rs` | `serde/testsuite/tests/macros/Mod.kt` |
| `unstable.mod` | `serde.testsuite.tests.unstable.Mod` | 0 | `serde/test_suite/tests/unstable/mod.rs` | `serde/testsuite/tests/unstable/Mod.kt` |

