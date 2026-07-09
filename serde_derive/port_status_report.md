# Code Port - Progress Report

**Generated:** 2026-07-09
**Source:** tmp/serde/serde_derive/src
**Target:** serde_derive/src/commonMain/kotlin/io/github/kotlinmania/serderive

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 112/286 matched (target 132) | 39.2% |
| Class/type parity | 16/44 matched (target 32) | 36.4% |
| Combined symbol parity | 128/330 matched (target 164) | 38.8% |
| Average function body similarity | 0.23 | inline-code cosine |
| Average documentation similarity | 0.00 | doc text cosine |
| Missing source functions | 139 | 0% parity until ported |
| Missing source classes/types | 24 | 0% parity until ported |
| Missing source symbol files | 10 | 163 symbols |
| Cheat/scoring failures | 6 | forced to 0% |
| Total source files | 28 | 100% |
| Target units (paired) | 29 | - |
| Target files (total) | 29 | - |
| Porting progress | 17 | 60.7% (matched) |
| Missing files | 10 | 35.7% |
| Reexport/wiring files | 1 | consult-only |

## Port Quality Analysis

**Average Function Similarity:** 0.23

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 14 files (82.4% of matched)
- Needs review (0.60-0.84): 3 files (17.6% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `de.struct_` | `de.Struct [STUB]` | 0.00 | 3/5 matched | `deserialize`, `deserialize_in_place` | 0/0 matched | _none_ | - | 2 | 3020510.0 |
| 2 | `de.enum_internally` | `de.EnumInternally [ZERO]` | 0.00 | 1/2 matched | `deserialize` | 0/0 matched | _none_ | - | 1 | 1010210.0 |
| 3 | `de` | `de.De [ZERO]` | 0.00 | 30/32 matched (target 33) | `new`, `in_place` | 7/9 matched (target 15) | `InPlaceImplGenerics`, `InPlaceTypeGenerics` | - | 4 | 44110.0 |
| 4 | `ser` | `ser.Ser [ZERO]` | 0.00 | 34/36 matched | `new`, `effective_style` | 4/5 matched (target 8) | `Parameters` | - | 3 | 34110.0 |
| 5 | `internals.case` | `internals.Case [ZERO]` | 0.00 | 6/7 matched (target 6) | `fmt` | 2/2 matched (target 5) | _none_ | 2/2 | 1 | 10910.0 |
| 6 | `pretend` | `internals.Pretend [ZERO]` | 0.00 | 6/6 matched | _none_ | 0/0 matched | _none_ | - | 0 | 610.0 |
| 7 | `de.tuple` | `de.Tuple` | 0.00 | 0/3 matched | `deserialize`, `deserialize_newtype_struct`, `deserialize_in_place` | 0/0 matched | _none_ | - | 3 | 2030310.0 |
| 8 | `de.enum_adjacently` | `de.EnumAdjacently` | 0.00 | 0/1 matched | `deserialize` | 0/0 matched | _none_ | - | 1 | 1010110.0 |
| 9 | `internals.symbol` | `internals.Symbol` | 0.00 | 0/2 matched (target 4) | `eq`, `fmt` | 1/1 matched | _none_ | - | 2 | 20310.0 |
| 10 | `de.unit` | `de.Unit` | 0.00 | 0/1 matched | `deserialize` | 0/0 matched | _none_ | - | 1 | 10110.0 |
| 11 | `internals.receiver` | `internals.Receiver` | 0.04 | 1/18 matched (target 1) | `self_ty`, `self_to_qself`, `self_to_expr_path`, `visit_type_mut`, `visit_type_path_mut`, `visit_expr_path_mut`, `visit_type_mut_impl`, `visit_type_path_mut_impl`, `visit_expr_path_mut_impl`, `visit_path_mut`, `visit_path_arguments_mut`, `visit_return_type_mut`, `visit_type_param_bound_mut`, `visit_generics_mut`, `visit_data_mut`, `visit_expr_mut`, `visit_macro_mut` | 0/1 matched (target 0) | `ReplaceReceiver` | - | 18 | 181909.6 |
| 12 | `de.enum_untagged` | `de.EnumUntagged` | 0.50 | 2/3 matched | `deserialize` | 0/0 matched | _none_ | - | 1 | 3010305.0 |
| 13 | `de.enum_` | `de.Enum` | 0.56 | 2/3 matched | `deserialize` | 0/0 matched | _none_ | - | 1 | 3010304.5 |
| 14 | `de.identifier` | `de.Identifier` | 0.57 | 3/3 matched | _none_ | 0/0 matched (target 1) | _none_ | - | 0 | 2000304.4 |
| 15 | `de.enum_externally` | `de.EnumExternally` | 0.61 | 3/4 matched | `deserialize` | 0/0 matched | _none_ | - | 1 | 1010403.9 |
| 16 | `lib` | `serderive.Lib` | 0.66 | 4/4 matched | _none_ | 1/1 matched | _none_ | - | 0 | 503.4 |
| 17 | `bound` | `internals.Bound` | 0.73 | 17/17 matched | _none_ | 1/1 matched | _none_ | - | 0 | 1802.7 |

## Cheat Detection / Scoring Failures

- `de.struct_` -> `de.Struct [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Struct.kt: snake_case identifier `in_place` in Kotlin comments
- `de.enum_internally` -> `de.EnumInternally [ZERO]`: function-by-function score forced to 0. EnumInternally.kt: snake_case identifier `visit_seq` in Kotlin comments
- `de` -> `de.De [ZERO]`: function-by-function score forced to 0. De.kt: snake_case identifier `parse_quote` in Kotlin comments; De.kt: Rust lifetime explanation in Kotlin comments
- `ser` -> `ser.Ser [ZERO]`: function-by-function score forced to 0. Ser.kt: snake_case identifier `parse_quote` in Kotlin comments
- `internals.case` -> `internals.Case [ZERO]`: function-by-function score forced to 0. Case.kt: snake_case identifier `snake_case` in Kotlin comments
- `pretend` -> `internals.Pretend [ZERO]`: function-by-function score forced to 0. Pretend.kt: snake_case identifier `dead_code` in Kotlin comments

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `de.struct_` -> `de.Struct [STUB]` (0.00, 3 deps)
- `de.enum_internally` -> `de.EnumInternally [ZERO]` (0.00, 1 deps)
- `de` -> `de.De [ZERO]` (0.00)
- `ser` -> `ser.Ser [ZERO]` (0.00)
- `internals.case` -> `internals.Case [ZERO]` (0.00)
- `pretend` -> `internals.Pretend [ZERO]` (0.00)
- `de.tuple` -> `de.Tuple` (0.00, 2 deps)
- `de.enum_adjacently` -> `de.EnumAdjacently` (0.00, 1 deps)
- `internals.symbol` -> `internals.Symbol` (0.00)
- `de.unit` -> `de.Unit` (0.00)
- `internals.receiver` -> `internals.Receiver` (0.04)
- `de.enum_untagged` -> `de.EnumUntagged` (0.50, 3 deps)
- `de.enum_` -> `de.Enum` (0.56, 3 deps)
- `de.identifier` -> `de.Identifier` (0.57, 2 deps)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| `internals.receiver` | `internals.Receiver` | 1/1 | `ReplaceReceiver` |
| `de` | `de.De [ZERO]` | 2/9 | `InPlaceImplGenerics`, `InPlaceTypeGenerics` |
| `ser` | `ser.Ser [ZERO]` | 1/5 | `Parameters` |

## High Priority Missing Files

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `internals.attr` | `internals.Attr` | 9 | 91 | 12 | 103 | `internals/attr.rs` | `internals/Attr.kt` |
| 2 | `internals.check` | `internals.Check` | 0 | 14 | 0 | 14 | `internals/check.rs` | `internals/Check.kt` |
| 3 | `internals.name` | `internals.Name` | 2 | 11 | 2 | 13 | `internals/name.rs` | `internals/Name.kt` |
| 4 | `internals.ast` | `internals.Ast` | 0 | 6 | 5 | 11 | `internals/ast.rs` | `internals/Ast.kt` |
| 5 | `fragment` | `Fragment` | 1 | 4 | 4 | 8 | `fragment.rs` | `Fragment.kt` |
| 6 | `internals.ctxt` | `internals.Ctxt` | 1 | 5 | 1 | 6 | `internals/ctxt.rs` | `internals/Ctxt.kt` |
| 7 | `deprecated` | `Deprecated` | 0 | 3 | 0 | 3 | `deprecated.rs` | `Deprecated.kt` |
| 8 | `internals.respan` | `internals.Respan` | 1 | 2 | 0 | 2 | `internals/respan.rs` | `internals/Respan.kt` |
| 9 | `this` | `This` | 0 | 2 | 0 | 2 | `this.rs` | `This.kt` |
| 10 | `dummy` | `Dummy` | 0 | 1 | 0 | 1 | `dummy.rs` | `Dummy.kt` |

## Documentation Gaps

There is missing documentation that is hurting overall scoring.

**Documentation coverage:** 0 / 220 lines (0%)

Documentation gaps (>20%), complete list:

- `de` - 100% gap (58 → 0 lines)
- `internals.case` - 100% gap (36 → 0 lines)
- `lib` - 100% gap (28 → 0 lines)
- `ser` - 100% gap (24 → 0 lines)
- `de.enum_untagged` - 100% gap (14 → 0 lines)
- `de.enum_adjacently` - 100% gap (14 → 0 lines)
- `de.enum_internally` - 100% gap (14 → 0 lines)
- `de.enum_externally` - 100% gap (12 → 0 lines)
- `de.struct_` - 100% gap (8 → 0 lines)

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `internals.mod` | `internals.Mod` | 0 | `internals/mod.rs` | `internals/Mod.kt` |

