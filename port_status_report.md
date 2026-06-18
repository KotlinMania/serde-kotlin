# Code Port - Progress Report

**Generated:** 2026-06-17
**Source:** tmp/serde/serde_derive
**Target:** src/commonMain/kotlin/io/github/kotlinmania/serde/derive

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 0/291 matched | 0.0% |
| Class/type parity | 0/43 matched | 0.0% |
| Combined symbol parity | 0/334 matched | 0.0% |
| Average function body similarity | N/A (no matched function-bearing files) | inline-code cosine |
| Average documentation similarity | 0.00 | doc text cosine |
| Missing source functions | 291 | 0% parity until ported |
| Missing source classes/types | 43 | 0% parity until ported |
| Missing source symbol files | 27 | 334 symbols |
| Cheat/scoring failures | 0 | forced to 0% |
| Total source files | 29 | 100% |
| Target units (paired) | 0 | - |
| Target files (total) | 0 | - |
| Porting progress | 0 | 0.0% (matched) |
| Missing files | 27 | 93.1% |
| Reexport/wiring files | 2 | consult-only |

**Report warning:** no source/target files were matched. Check the source and target roots or missing `port-lint` provenance before trusting this run.

## Port Quality Analysis

**Average Function Similarity:** N/A (no matched function-bearing files)

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 0 files (0.0% of matched)
- Needs review (0.60-0.84): 0 files (0.0% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|

## Cheat Detection / Scoring Failures

_None detected._

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:


## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| _None detected_ | | | |

## High Priority Missing Files

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `internals.attr` | `internals.Attr` | 9 | 91 | 12 | 103 | `src/internals/attr.rs` | `internals/Attr.kt` |
| 2 | `de` | `de.De` | 0 | 37 | 9 | 46 | `src/de.rs` | `de/De.kt` |
| 3 | `ser` | `Ser` | 0 | 36 | 5 | 41 | `src/ser.rs` | `Ser.kt` |
| 4 | `internals.receiver` | `internals.Receiver` | 0 | 18 | 1 | 19 | `src/internals/receiver.rs` | `internals/Receiver.kt` |
| 5 | `bound` | `Bound` | 0 | 17 | 1 | 18 | `src/bound.rs` | `Bound.kt` |
| 6 | `internals.check` | `internals.Check` | 0 | 14 | 0 | 14 | `src/internals/check.rs` | `internals/Check.kt` |
| 7 | `internals.name` | `internals.Name` | 2 | 11 | 2 | 13 | `src/internals/name.rs` | `internals/Name.kt` |
| 8 | `internals.ast` | `internals.Ast` | 0 | 6 | 5 | 11 | `src/internals/ast.rs` | `internals/Ast.kt` |
| 9 | `internals.case` | `internals.Case` | 0 | 7 | 2 | 9 | `src/internals/case.rs` | `internals/Case.kt` |
| 10 | `fragment` | `Fragment` | 1 | 4 | 4 | 8 | `src/fragment.rs` | `Fragment.kt` |
| 11 | `internals.ctxt` | `internals.Ctxt` | 1 | 5 | 1 | 6 | `src/internals/ctxt.rs` | `internals/Ctxt.kt` |
| 12 | `internals.symbol` | `internals.Symbol` | 0 | 5 | 1 | 6 | `src/internals/symbol.rs` | `internals/Symbol.kt` |
| 13 | `pretend` | `Pretend` | 0 | 6 | 0 | 6 | `src/pretend.rs` | `Pretend.kt` |
| 14 | `de.struct_` | `de.Struct` | 3 | 5 | 0 | 5 | `src/de/struct_.rs` | `de/Struct.kt` |
| 15 | `de.enum_externally` | `de.EnumExternally` | 1 | 4 | 0 | 4 | `src/de/enum_externally.rs` | `de/EnumExternally.kt` |
| 16 | `de.enum_` | `de.Enum` | 3 | 3 | 0 | 3 | `src/de/enum_.rs` | `de/Enum.kt` |
| 17 | `de.enum_untagged` | `de.EnumUntagged` | 3 | 3 | 0 | 3 | `src/de/enum_untagged.rs` | `de/EnumUntagged.kt` |
| 18 | `de.identifier` | `de.Identifier` | 2 | 3 | 0 | 3 | `src/de/identifier.rs` | `de/Identifier.kt` |
| 19 | `de.tuple` | `de.Tuple` | 2 | 3 | 0 | 3 | `src/de/tuple.rs` | `de/Tuple.kt` |
| 20 | `deprecated` | `Deprecated` | 0 | 3 | 0 | 3 | `src/deprecated.rs` | `Deprecated.kt` |
| 21 | `de.enum_internally` | `de.EnumInternally` | 1 | 2 | 0 | 2 | `src/de/enum_internally.rs` | `de/EnumInternally.kt` |
| 22 | `internals.respan` | `internals.Respan` | 1 | 2 | 0 | 2 | `src/internals/respan.rs` | `internals/Respan.kt` |
| 23 | `this` | `This` | 0 | 2 | 0 | 2 | `src/this.rs` | `This.kt` |
| 24 | `de.enum_adjacently` | `de.EnumAdjacently` | 1 | 1 | 0 | 1 | `src/de/enum_adjacently.rs` | `de/EnumAdjacently.kt` |
| 25 | `build` | `Build` | 0 | 1 | 0 | 1 | `build.rs` | `Build.kt` |
| 26 | `de.unit` | `de.Unit` | 0 | 1 | 0 | 1 | `src/de/unit.rs` | `de/Unit.kt` |
| 27 | `dummy` | `Dummy` | 0 | 1 | 0 | 1 | `src/dummy.rs` | `Dummy.kt` |

## Documentation Gaps

**Documentation coverage:** 0 / 0 lines (N/A)

Documentation gaps (>20%), complete list:

No significant documentation gaps found.

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `internals.mod` | `internals.Mod` | 0 | `src/internals/mod.rs` | `internals/Mod.kt` |
| `lib` | `Lib` | 0 | `src/lib.rs` | `Lib.kt` |

