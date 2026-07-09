# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `private.size_hint` | `priv.SizeHint` | 0.88 | 2 | 3/3 matched (target 9) | _none_ | 0/0 matched (target 3) | _none_ | 0 | 3 | 2000301.2 |
| 2 | `de.ignored_any` | `de.IgnoredAny` | 0.37 | 1 | 11/17 matched (target 57) | `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize` | 2/2 matched (target 10) | _none_ | 6 | 19 | 1061906.4 |
| 3 | `ser.impossible` | `ser.Impossible` | 0.05 | 1 | 1/5 matched (target 3) | `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value` | 2/4 matched (target 2) | `Ok`, `Error` | 6 | 9 | 1060909.5 |
| 4 | `private.content` | `priv.Content [ZERO]` | 0.00 | 1 | 0/0 matched (target 12) | _none_ | 1/1 matched (target 24) | _none_ | 0 | 1 | 1000110.0 |
| 5 | `de.impls` | `de.Impls` | 0.08 | 0 | 12/22 matched (target 156) | `deserialize`, `deserialize_in_place`, `visit_seq`, `visit_some`, `__private_visit_untagged_option`, `nop_reserve`, `new`, `visit_enum`, `check_overflow`, `visit_map` | 6/27 matched (target 36) | `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor` | 31 | 49 | 314909.2 |
| 6 | `de.mod` | `de.OneOf [STUB]` | 0.00 | 0 | 32/48 matched (target 45) | `fmt`, `deserialize_in_place`, `deserialize`, `__deserialize_content_v1`, `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__private_visit_untagged_option`, `next_element_seed`, `next_key_seed`, `next_value_seed`, `variant`, `write_str`, `write_char` | 13/17 matched (target 32) | `Expected`, `Value`, `Error`, `LookForDecimalPoint` | 20 | 65 | 206510.0 |
| 7 | `de.value` | `value.Value` | 0.57 | 0 | 20/27 matched (target 617) | `custom`, `fmt`, `description`, `clone`, `next_pair`, `next_entry_seed`, `split` | 22/30 matched (target 44) | `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second` | 15 | 57 | 155704.3 |
| 8 | `ser.fmt` | `ser.Fmt` | 0.39 | 0 | 12/16 matched (target 43) | `custom`, `serialize_newtype_struct`, `serialize_some`, `serialize_newtype_variant` | 0/9 matched (target 3) | `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant` | 13 | 25 | 132506.1 |
| 9 | `ser.mod` | `ser.Serialize [STUB]` | 0.00 | 0 | 6/9 matched (target 20) | `collect_seq`, `collect_map`, `serialize_entry` | 9/9 matched (target 16) | _none_ | 3 | 18 | 31810.0 |
| 10 | `private.seed` | `priv.Seed` | 0.00 | 0 | 0/1 matched | `deserialize` | 1/2 matched (target 1) | `Value` | 2 | 3 | 20310.0 |
| 11 | `std_error` | `serdecore.StdError [ZERO]` | 0.00 | 0 | 0/1 matched (target 0) | `source` | 0/1 matched | `Error` | 2 | 2 | 20210.0 |
| 12 | `private.doc` | `priv.Doc` | 0.23 | 0 | 3/3 matched (target 31) | _none_ | 1/1 matched (target 4) | _none_ | 0 | 4 | 407.7 |
| 13 | `format` | `serdecore.Format` | 0.61 | 0 | 3/3 matched | _none_ | 1/1 matched (target 2) | _none_ | 0 | 4 | 403.9 |
| 14 | `ser.impls` | `ser.Impls` | 0.49 | 0 | 3/3 matched (target 67) | _none_ | 0/0 matched (target 15) | _none_ | 0 | 3 | 305.1 |
| 15 | `private.string` | `priv.String` | 0.39 | 0 | 1/1 matched (target 2) | _none_ | 0/0 matched | _none_ | 0 | 1 | 106.1 |
| 16 | `macros` | `serdecore.Macros [ZERO]` | 0.00 | 0 | 0/0 matched (target 30) | _none_ | 0/0 matched (target 1) | _none_ | 0 | 0 | 10.0 |
| 17 | `private.mod` | `priv.Mod [STUB]` | 0.00 | 0 | 0/0 matched | _none_ | 0/0 matched | _none_ | 0 | 0 | 10.0 |
| 18 | `crate_root` | `serdecore.CrateRoot [ZERO]` | 0.00 | 0 | 0/0 matched (target 3) | _none_ | 0/0 matched (target 2) | _none_ | 0 | 0 | 10.0 |
| 19 | `lib` | `serdecore.Lib` | 1.00 | 0 | 0/0 matched | _none_ | 0/0 matched (target 3) | _none_ | 0 | 0 | 0.0 |

## Cheat Detection / Scoring Failures

- `private.content` -> `priv.Content [ZERO]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `de.mod` -> `de.OneOf [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `ser.mod` -> `ser.Serialize [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies
- `std_error` -> `serdecore.StdError [ZERO]`: function-by-function score forced to 0. no target functions found; report scoring is function-by-function only
- `macros` -> `serdecore.Macros [ZERO]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only
- `private.mod` -> `priv.Mod [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Mod.kt: snake_case identifier `workspace_dep_graph` in Kotlin comments
- `crate_root` -> `serdecore.CrateRoot [ZERO]`: function-by-function score forced to 0. no source functions found; target defines functions; report scoring is function-by-function only

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **de.ignored_any** → `de.IgnoredAny`
  - Function similarity: 0.37
  - Dependencies: 1
  - Functions: 11/17 matched (target 57)
  - Missing functions: `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize`
  - Types: 2/2 matched (target 10)
  - Missing types: _none_

- **ser.impossible** → `ser.Impossible`
  - Function similarity: 0.05
  - Dependencies: 1
  - Functions: 1/5 matched (target 3)
  - Missing functions: `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value`
  - Types: 2/4 matched (target 2)
  - Missing types: `Ok`, `Error`

- **private.content** → `priv.Content [ZERO]`
  - Function similarity: 0.00
  - Dependencies: 1
  - Functions: 0/0 matched (target 12)
  - Missing functions: _none_
  - Types: 1/1 matched (target 24)
  - Missing types: _none_
  - Scoring failure: no source functions found; target defines functions; report scoring is function-by-function only

## Missing Files (by Dependents)

No missing files detected.

