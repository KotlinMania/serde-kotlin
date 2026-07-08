# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `private.size_hint` | `priv.SizeHint [PROVENANCE-FALLBACK]` | 0.88 | 2 | 3/3 matched (target 9) | _none_ | 0/0 matched (target 3) | _none_ | 0 | 3 | 2000301.2 |
| 2 | `de.ignored_any` | `de.IgnoredAny [PROVENANCE-FALLBACK]` | 0.37 | 1 | 11/17 matched (target 11) | `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize` | 1/2 matched (target 1) | `Value` | 7 | 19 | 1071906.4 |
| 3 | `ser.impossible` | `ser.Impossible [PROVENANCE-FALLBACK]` | 0.05 | 1 | 1/5 matched (target 3) | `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value` | 2/4 matched (target 2) | `Ok`, `Error` | 6 | 9 | 1060909.5 |
| 4 | `private.content` | `priv.Content [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 1 | 0/0 matched (target 4) | _none_ | 1/1 matched (target 24) | _none_ | 0 | 1 | 1000110.0 |
| 5 | `de.impls` | `de.Impls [PROVENANCE-FALLBACK]` | 0.08 | 0 | 12/22 matched (target 153) | `deserialize`, `deserialize_in_place`, `visit_seq`, `visit_some`, `__private_visit_untagged_option`, `nop_reserve`, `new`, `visit_enum`, `check_overflow`, `visit_map` | 6/27 matched (target 36) | `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor` | 31 | 49 | 314909.2 |
| 6 | `de.mod` | `de.OneOf [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 29/48 matched (target 35) | `fmt`, `deserialize_in_place`, `deserialize`, `__deserialize_content_v1`, `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__private_visit_untagged_option`, `next_element_seed`, `next_key`, `next_value`, `next_entry`, `next_key_seed`, `next_value_seed`, `variant`, `write_str`, `write_char` | 13/17 matched (target 32) | `Expected`, `Value`, `Error`, `LookForDecimalPoint` | 23 | 65 | 236510.0 |
| 7 | `de.value` | `value.Value [PROVENANCE-FALLBACK]` | 0.56 | 0 | 18/27 matched (target 613) | `custom`, `fmt`, `description`, `clone`, `next_pair`, `next_entry_seed`, `unit_only`, `map_as_enum`, `split` | 22/30 matched (target 43) | `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second` | 17 | 57 | 175704.4 |
| 8 | `ser.fmt` | `ser.Fmt [PROVENANCE-FALLBACK]` | 0.40 | 0 | 12/16 matched (target 41) | `custom`, `serialize_newtype_struct`, `serialize_some`, `serialize_newtype_variant` | 0/9 matched (target 3) | `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant` | 13 | 25 | 132506.0 |
| 9 | `private.doc` | `priv.Doc [PROVENANCE-FALLBACK]` | 0.00 | 0 | 0/3 matched (target 26) | `custom`, `description`, `fmt` | 0/1 matched (target 2) | `Error` | 4 | 4 | 40410.0 |
| 10 | `ser.mod` | `ser.Serialize [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 6/9 matched (target 7) | `collect_seq`, `collect_map`, `serialize_entry` | 9/9 matched (target 10) | _none_ | 3 | 18 | 31810.0 |
| 11 | `private.seed` | `priv.Seed [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 0/1 matched (target 0) | `deserialize` | 1/2 matched (target 1) | `Value` | 2 | 3 | 20310.0 |
| 12 | `format` | `serdecore.Format [PROVENANCE-FALLBACK]` | 0.61 | 0 | 3/3 matched | _none_ | 1/1 matched (target 2) | _none_ | 0 | 4 | 403.9 |
| 13 | `ser.impls` | `ser.Impls [PROVENANCE-FALLBACK]` | 0.49 | 0 | 3/3 matched (target 67) | _none_ | 0/0 matched (target 15) | _none_ | 0 | 3 | 305.1 |
| 14 | `private.string` | `priv.String [PROVENANCE-FALLBACK]` | 0.39 | 0 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | 0 | 1 | 106.1 |
| 15 | `lib` | `serde.Lib [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 0/0 matched (target 1) | _none_ | 0/0 matched (target 3) | _none_ | 0 | 0 | 10.0 |
| 16 | `macros` | `serdecore.Macros [ZERO] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 0/0 matched (target 30) | _none_ | 0/0 matched (target 1) | _none_ | 0 | 0 | 10.0 |
| 17 | `private.mod` | `priv.Mod [STUB] [PROVENANCE-FALLBACK]` | 0.00 | 0 | 0/0 matched | _none_ | 0/0 matched | _none_ | 0 | 0 | 10.0 |

## Cheat Detection / Scoring Failures

- `private.content` -> `priv.Content [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `de.mod` -> `de.OneOf [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `ser.mod` -> `ser.Serialize [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `private.seed` -> `priv.Seed [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. no target functions found; report scoring is function-by-function only
- `lib` -> `serde.Lib [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Lib.kt: snake_case identifier `no_std` in Kotlin comments; Lib.kt: snake_case identifier `serde_core` in Kotlin comments; no source functions found; target defines functions; report scoring is function-by-function only
- `macros` -> `serdecore.Macros [ZERO] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `private.mod` -> `priv.Mod [STUB] [PROVENANCE-FALLBACK]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Mod.kt: snake_case identifier `serde_core` in Kotlin comments

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **de.ignored_any** → `de.IgnoredAny [PROVENANCE-FALLBACK]`
  - Function similarity: 0.37
  - Dependencies: 1
  - Functions: 11/17 matched (target 11)
  - Missing functions: `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize`
  - Types: 1/2 matched (target 1)
  - Missing types: `Value`
  - Lint issues: 1

- **ser.impossible** → `ser.Impossible [PROVENANCE-FALLBACK]`
  - Function similarity: 0.05
  - Dependencies: 1
  - Functions: 1/5 matched (target 3)
  - Missing functions: `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value`
  - Types: 2/4 matched (target 2)
  - Missing types: `Ok`, `Error`
  - Lint issues: 1

- **private.content** → `priv.Content [ZERO] [PROVENANCE-FALLBACK]`
  - Function similarity: 0.00
  - Dependencies: 1
  - Functions: 0/0 matched (target 4)
  - Missing functions: _none_
  - Types: 1/1 matched (target 24)
  - Missing types: _none_
  - Scoring failure: no source functions found; target defines functions; report scoring is function-by-function only
  - Lint issues: 1

## Missing Files (by Dependents)

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `build` | `Build` | 0 | 2 | 0 | 2 | `build.rs` | `Build.kt` |
| 2 | `crate_root` | `CrateRoot` | 0 | 0 | 0 | 0 | `src/crate_root.rs` | `CrateRoot.kt` |
| 3 | `std_error` | `StdError` | 0 | 1 | 1 | 2 | `src/std_error.rs` | `StdError.kt` |

