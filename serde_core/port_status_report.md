# Code Port - Progress Report

**Generated:** 2026-07-09
**Source:** tmp/serde/serde_core/src
**Target:** serde_core/src/commonMain/kotlin/io/github/kotlinmania/serdecore

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 107/159 matched (target 1099) | 67.3% |
| Class/type parity | 58/104 matched (target 199) | 55.8% |
| Combined symbol parity | 165/263 matched (target 1298) | 62.7% |
| Average function body similarity | 0.32 | inline-code cosine |
| Average documentation similarity | 0.29 | doc text cosine |
| Missing source functions | 0 | 0% parity until ported |
| Missing source classes/types | 0 | 0% parity until ported |
| Missing source symbol files | 0 | 0 symbols |
| Cheat/scoring failures | 7 | forced to 0% |
| Total source files | 19 | 100% |
| Target units (paired) | 51 | - |
| Target files (total) | 51 | - |
| Porting progress | 19 | 100.0% (matched) |
| Missing files | 0 | 0.0% |

## Port Quality Analysis

**Average Function Similarity:** 0.32

Similarity in this report is the required function-by-function body/parameter score. Class/type parity and symbol deficits are reported beside it; whole-file shape is diagnostic only.

**Work Distribution:**
- Critical (<0.60): 16 files (84.2% of matched)
- Needs review (0.60-0.84): 1 files (5.3% of matched)

## Worst Function Scores First

Every matched file is listed from lowest function body/parameter similarity upward. Missing symbol names are not capped.

| Rank | Source | Target | Function similarity | Functions | Missing functions | Types | Missing types | Tests | Symbol deficit | Priority |
|------|--------|--------|---------------------|-----------|-------------------|-------|---------------|-------|----------------|----------|
| 1 | `private.content` | `priv.Content [ZERO]` | 0.00 | 0/0 matched (target 12) | _none_ | 1/1 matched (target 24) | _none_ | - | 0 | 1000110.0 |
| 2 | `de.mod` | `de.OneOf [STUB]` | 0.00 | 32/48 matched (target 45) | `fmt`, `deserialize_in_place`, `deserialize`, `__deserialize_content_v1`, `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__private_visit_untagged_option`, `next_element_seed`, `next_key_seed`, `next_value_seed`, `variant`, `write_str`, `write_char` | 13/17 matched (target 32) | `Expected`, `Value`, `Error`, `LookForDecimalPoint` | - | 20 | 206510.0 |
| 3 | `ser.mod` | `ser.Serialize [STUB]` | 0.00 | 6/9 matched (target 20) | `collect_seq`, `collect_map`, `serialize_entry` | 9/9 matched (target 16) | _none_ | - | 3 | 31810.0 |
| 4 | `std_error` | `serdecore.StdError [ZERO]` | 0.00 | 0/1 matched (target 0) | `source` | 0/1 matched | `Error` | - | 2 | 20210.0 |
| 5 | `crate_root` | `serdecore.CrateRoot [ZERO]` | 0.00 | 0/0 matched (target 3) | _none_ | 0/0 matched (target 2) | _none_ | - | 0 | 10.0 |
| 6 | `macros` | `serdecore.Macros [ZERO]` | 0.00 | 0/0 matched (target 30) | _none_ | 0/0 matched (target 1) | _none_ | - | 0 | 10.0 |
| 7 | `private.mod` | `priv.Mod [STUB]` | 0.00 | 0/0 matched | _none_ | 0/0 matched | _none_ | - | 0 | 10.0 |
| 8 | `private.seed` | `priv.Seed` | 0.00 | 0/1 matched | `deserialize` | 1/2 matched (target 1) | `Value` | - | 2 | 20310.0 |
| 9 | `ser.impossible` | `ser.Impossible` | 0.05 | 1/5 matched (target 3) | `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value` | 2/4 matched (target 2) | `Ok`, `Error` | - | 6 | 1060909.5 |
| 10 | `de.impls` | `de.Impls` | 0.08 | 12/22 matched (target 156) | `deserialize`, `deserialize_in_place`, `visit_seq`, `visit_some`, `__private_visit_untagged_option`, `nop_reserve`, `new`, `visit_enum`, `check_overflow`, `visit_map` | 6/27 matched (target 36) | `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor` | - | 31 | 314909.2 |
| 11 | `private.doc` | `priv.Doc` | 0.23 | 3/3 matched (target 31) | _none_ | 1/1 matched (target 4) | _none_ | - | 0 | 407.7 |
| 12 | `de.ignored_any` | `de.IgnoredAny` | 0.37 | 11/17 matched (target 57) | `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize` | 2/2 matched (target 10) | _none_ | - | 6 | 1061906.4 |
| 13 | `private.string` | `priv.String` | 0.39 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | - | 0 | 106.1 |
| 14 | `ser.fmt` | `ser.Fmt` | 0.39 | 12/16 matched (target 43) | `custom`, `serialize_newtype_struct`, `serialize_some`, `serialize_newtype_variant` | 0/9 matched (target 3) | `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant` | - | 13 | 132506.1 |
| 15 | `ser.impls` | `ser.Impls` | 0.49 | 3/3 matched (target 67) | _none_ | 0/0 matched (target 15) | _none_ | 1/1 | 0 | 305.1 |
| 16 | `de.value` | `value.Value` | 0.57 | 20/27 matched (target 617) | `custom`, `fmt`, `description`, `clone`, `next_pair`, `next_entry_seed`, `split` | 22/30 matched (target 44) | `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second` | - | 15 | 155704.3 |
| 17 | `format` | `serdecore.Format` | 0.61 | 3/3 matched | _none_ | 1/1 matched (target 2) | _none_ | - | 0 | 403.9 |
| 18 | `private.size_hint` | `priv.SizeHint` | 0.88 | 3/3 matched (target 9) | _none_ | 0/0 matched (target 3) | _none_ | - | 0 | 2000301.2 |
| 19 | `lib` | `serdecore.Lib` | 1.00 | 0/0 matched | _none_ | 0/0 matched (target 3) | _none_ | - | 0 | 0.0 |

## Cheat Detection / Scoring Failures

- `private.content` -> `priv.Content [ZERO]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `de.mod` -> `de.OneOf [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `ser.mod` -> `ser.Serialize [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `std_error` -> `serdecore.StdError [ZERO]`: function-by-function score forced to 0. no target functions found; report scoring is function-by-function only
- `crate_root` -> `serdecore.CrateRoot [ZERO]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `macros` -> `serdecore.Macros [ZERO]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `private.mod` -> `priv.Mod [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Mod.kt: snake_case identifier `workspace_dep_graph` in Kotlin comments

### Critical Ports (Similarity < 0.60, Worst First)

These files need significant work:

- `private.content` -> `priv.Content [ZERO]` (0.00, 1 deps)
- `de.mod` -> `de.OneOf [STUB]` (0.00)
- `ser.mod` -> `ser.Serialize [STUB]` (0.00)
- `std_error` -> `serdecore.StdError [ZERO]` (0.00)
- `crate_root` -> `serdecore.CrateRoot [ZERO]` (0.00)
- `macros` -> `serdecore.Macros [ZERO]` (0.00)
- `private.mod` -> `priv.Mod [STUB]` (0.00)
- `private.seed` -> `priv.Seed` (0.00)
- `ser.impossible` -> `ser.Impossible` (0.05, 1 deps)
- `de.impls` -> `de.Impls` (0.08)
- `private.doc` -> `priv.Doc` (0.23)
- `de.ignored_any` -> `de.IgnoredAny` (0.37, 1 deps)
- `private.string` -> `priv.String` (0.39)
- `ser.fmt` -> `ser.Fmt` (0.39)
- `ser.impls` -> `ser.Impls` (0.49)
- `de.value` -> `value.Value` (0.57)

## Incorrect Ports (Missing Types)

These files are matched (often via `// port-lint`) but appear to be missing one or more type declarations
present in the Rust source file.

| Source | Target | Missing types | Examples |
|--------|--------|---------------|----------|
| `ser.impossible` | `ser.Impossible` | 2/4 | `Ok`, `Error` |
| `de.impls` | `de.Impls` | 21/27 | `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor` |
| `de.mod` | `de.OneOf [STUB]` | 4/17 | `Expected`, `Value`, `Error`, `LookForDecimalPoint` |
| `de.value` | `value.Value` | 8/30 | `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second` |
| `ser.fmt` | `ser.Fmt` | 9/9 | `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant` |
| `private.seed` | `priv.Seed` | 1/2 | `Value` |
| `std_error` | `serdecore.StdError [ZERO]` | 1/1 | `Error` |

## High Priority Missing Files

No missing files detected.

## Documentation Gaps

There is missing documentation that is hurting overall scoring.

**Documentation coverage:** 302 / 7004 lines (4%)

Documentation gaps (>20%), complete list:

- `ser.mod` - 99% gap (3229 → 23 lines)
- `de.mod` - 100% gap (2782 → 9 lines)
- `macros` - 93% gap (210 → 15 lines)
- `de.ignored_any` - 69% gap (206 → 64 lines)
- `ser.impossible` - 77% gap (102 → 23 lines)
- `ser.impls` - 100% gap (77 → 0 lines)
- `de.impls` - 100% gap (74 → 0 lines)
- `std_error` - 89% gap (82 → 9 lines)
- `lib` - 75% gap (68 → 17 lines)
- `de.value` - 25% gap (126 → 95 lines)
- `ser.fmt` - 31% gap (36 → 25 lines)

