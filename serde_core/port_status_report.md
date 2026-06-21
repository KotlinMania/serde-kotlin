# Code Port - Progress Report

**Generated:** 2026-06-20
**Source:** tmp/serde/serde_core
**Target:** serde_core

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 99/161 matched (target 1005) | 61.5% |
| Class/type parity | 56/104 matched (target 178) | 53.8% |
| Combined symbol parity | 155/265 matched (target 1183) | 58.5% |
| Average function body similarity | 0.29 | inline-code cosine |
| Average documentation similarity | 0.25 | doc text cosine |
| Missing source functions | 3 | 0% parity until ported |
| Missing source classes/types | 1 | 0% parity until ported |
| Missing source symbol files | 2 | 4 symbols |
| Cheat/scoring failures | 7 | forced to 0% |
| Total source files | 20 | 100% |
| Target units (paired) | 49 | - |
| Target files (total) | 49 | - |
| Porting progress | 17 | 85.0% (matched) |
| Missing files | 3 | 15.0% |

## Port Quality Analysis

**Average Function Similarity:** 0.29

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 15 files (88.2% of matched)
- Needs review (0.60-0.84): 1 files (5.9% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `private.content` | `priv.Content [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 0/0 matched (target 4) | _none_ | 1/1 matched (target 24) | _none_ | - | 0 | 1000110.0 |
| 2 | `de.mod` | `de.OneOf [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 29/48 matched (target 35) | `fmt`, `deserialize_in_place`, `deserialize`, `__deserialize_content_v1`, `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__private_visit_untagged_option`, `next_element_seed`, `next_key`, `next_value`, `next_entry`, `next_key_seed`, `next_value_seed`, `variant`, `write_str`, `write_char` | 13/17 matched (target 32) | `Expected`, `Value`, `Error`, `LookForDecimalPoint` | - | 23 | 236510.0 |
| 3 | `ser.mod` | `ser.Serialize [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 6/9 matched (target 7) | `collect_seq`, `collect_map`, `serialize_entry` | 9/9 matched (target 10) | _none_ | - | 3 | 31810.0 |
| 4 | `private.seed` | `priv.Seed [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 0/1 matched (target 0) | `deserialize` | 1/2 matched (target 1) | `Value` | - | 2 | 20310.0 |
| 5 | `lib` | `serde.Lib [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 0/0 matched (target 1) | _none_ | 0/0 matched (target 3) | _none_ | - | 0 | 10.0 |
| 6 | `macros` | `serdecore.Macros [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 0/0 matched (target 30) | _none_ | 0/0 matched (target 1) | _none_ | - | 0 | 10.0 |
| 7 | `private.mod` | `priv.Mod [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 0/0 matched | _none_ | 0/0 matched | _none_ | - | 0 | 10.0 |
| 8 | `private.doc` | `priv.Doc [PROVENANCE-FALLBACK]` | 0.00 | 0/3 matched (target 26) | `custom`, `description`, `fmt` | 0/1 matched (target 2) | `Error` | - | 4 | 40410.0 |
| 9 | `ser.impossible` | `ser.Impossible [PROVENANCE-FALLBACK]` | 0.05 | 1/5 matched (target 3) | `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value` | 2/4 matched (target 2) | `Ok`, `Error` | - | 6 | 1060909.5 |
| 10 | `de.impls` | `de.Impls [PROVENANCE-FALLBACK]` | 0.08 | 12/22 matched (target 153) | `deserialize`, `deserialize_in_place`, `visit_seq`, `visit_some`, `__private_visit_untagged_option`, `nop_reserve`, `new`, `visit_enum`, `check_overflow`, `visit_map` | 6/27 matched (target 36) | `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor` | - | 31 | 314909.2 |
| 11 | `de.ignored_any` | `de.IgnoredAny [PROVENANCE-FALLBACK]` | 0.37 | 11/17 matched (target 11) | `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize` | 1/2 matched (target 1) | `Value` | - | 7 | 1071906.4 |
| 12 | `private.string` | `priv.String [PROVENANCE-FALLBACK]` | 0.39 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | - | 0 | 106.1 |
| 13 | `ser.fmt` | `ser.Fmt [PROVENANCE-FALLBACK]` | 0.40 | 12/16 matched (target 41) | `custom`, `serialize_newtype_struct`, `serialize_some`, `serialize_newtype_variant` | 0/9 matched (target 3) | `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant` | - | 13 | 132506.0 |
| 14 | `ser.impls` | `ser.Impls [PROVENANCE-FALLBACK]` | 0.49 | 3/3 matched (target 67) | _none_ | 0/0 matched (target 15) | _none_ | 1/1 | 0 | 305.1 |
| 15 | `de.value` | `value.Value [PROVENANCE-FALLBACK]` | 0.56 | 18/27 matched (target 613) | `custom`, `fmt`, `description`, `clone`, `next_pair`, `next_entry_seed`, `unit_only`, `map_as_enum`, `split` | 22/30 matched (target 43) | `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second` | - | 17 | 175704.4 |
| 16 | `format` | `serdecore.Format [PROVENANCE-FALLBACK]` | 0.61 | 3/3 matched | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 403.9 |
| 17 | `private.size_hint` | `priv.SizeHint [PROVENANCE-FALLBACK]` | 0.88 | 3/3 matched (target 9) | _none_ | 0/0 matched (target 3) | _none_ | - | 0 | 2000301.2 |

## Cheat Detection / Scoring Failures

- `private.content` -> `priv.Content [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `de.mod` -> `de.OneOf [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `ser.mod` -> `ser.Serialize [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `private.seed` -> `priv.Seed [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. no target functions found; report scoring is function-by-function only
- `lib` -> `serde.Lib [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Lib.kt: snake_case identifier `no_std` in Kotlin comments; Lib.kt: snake_case identifier `serde_core` in Kotlin comments; no source functions found; target defines functions; report scoring is function-by-function only
- `macros` -> `serdecore.Macros [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `private.mod` -> `priv.Mod [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Mod.kt: snake_case identifier `serde_core` in Kotlin comments

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `private.content` -> `priv.Content [ZERO] [PROVENANCE-FALLBACK]` (0.00, 1 deps)
- `de.mod` -> `de.OneOf [STUB] [PROVENANCE-FALLBACK]` (0.00)
- `ser.mod` -> `ser.Serialize [STUB] [PROVENANCE-FALLBACK]` (0.00)
- `private.seed` -> `priv.Seed [ZERO] [PROVENANCE-FALLBACK]` (0.00)
- `lib` -> `serde.Lib [STUB] [PROVENANCE-FALLBACK]` (0.00)
- `macros` -> `serdecore.Macros [ZERO] [PROVENANCE-FALLBACK]` (0.00)
- `private.mod` -> `priv.Mod [STUB] [PROVENANCE-FALLBACK]` (0.00)
- `private.doc` -> `priv.Doc [PROVENANCE-FALLBACK]` (0.00)
- `ser.impossible` -> `ser.Impossible [PROVENANCE-FALLBACK]` (0.05, 1 deps)
- `de.impls` -> `de.Impls [PROVENANCE-FALLBACK]` (0.08)
- `de.ignored_any` -> `de.IgnoredAny [PROVENANCE-FALLBACK]` (0.37, 1 deps)
- `private.string` -> `priv.String [PROVENANCE-FALLBACK]` (0.39)
- `ser.fmt` -> `ser.Fmt [PROVENANCE-FALLBACK]` (0.40)
- `ser.impls` -> `ser.Impls [PROVENANCE-FALLBACK]` (0.49)
- `de.value` -> `value.Value [PROVENANCE-FALLBACK]` (0.56)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| `de.ignored_any` | `de.IgnoredAny [PROVENANCE-FALLBACK]` | 1/2 | `Value` |
| `ser.impossible` | `ser.Impossible [PROVENANCE-FALLBACK]` | 2/4 | `Ok`, `Error` |
| `de.impls` | `de.Impls [PROVENANCE-FALLBACK]` | 21/27 | `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor` |
| `de.mod` | `de.OneOf [STUB] [PROVENANCE-FALLBACK]` | 4/17 | `Expected`, `Value`, `Error`, `LookForDecimalPoint` |
| `de.value` | `value.Value [PROVENANCE-FALLBACK]` | 8/30 | `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second` |
| `ser.fmt` | `ser.Fmt [PROVENANCE-FALLBACK]` | 9/9 | `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant` |
| `private.doc` | `priv.Doc [PROVENANCE-FALLBACK]` | 1/1 | `Error` |
| `private.seed` | `priv.Seed [ZERO] [PROVENANCE-FALLBACK]` | 1/2 | `Value` |

## High Priority Missing Files

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `build` | `Build` | 0 | 2 | 0 | 2 | `build.rs` | `Build.kt` |
| 2 | `std_error` | `StdError` | 0 | 1 | 1 | 2 | `src/std_error.rs` | `StdError.kt` |
| 3 | `crate_root` | `CrateRoot` | 0 | 0 | 0 | 0 | `src/crate_root.rs` | `CrateRoot.kt` |

## Documentation Gaps

There is missing documentation that is hurting overall scoring.

**Documentation coverage:** 328 / 6916 lines (5%)

Documentation gaps (>20%), complete list:

- `ser.mod` - 99% gap (3229 → 23 lines)
- `de.mod` - 100% gap (2782 → 9 lines)
- `macros` - 93% gap (210 → 15 lines)
- `de.ignored_any` - 69% gap (206 → 64 lines)
- `ser.impossible` - 77% gap (102 → 23 lines)
- `ser.impls` - 100% gap (77 → 0 lines)
- `de.impls` - 100% gap (74 → 0 lines)
- `de.value` - 36% gap (126 → 81 lines)
- `ser.fmt` - 31% gap (36 → 25 lines)

