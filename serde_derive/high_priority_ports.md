# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|
| 1 | `de.struct_` | `de.Struct [STUB]` | 0.00 | 3 | 3/5 matched | `deserialize`, `deserialize_in_place` | 0/0 matched | _none_ | 2 | 5 | 3020510.0 |
| 2 | `de.enum_untagged` | `de.EnumUntagged` | 0.50 | 3 | 2/3 matched | `deserialize` | 0/0 matched | _none_ | 1 | 3 | 3010305.0 |
| 3 | `de.enum_` | `de.Enum` | 0.56 | 3 | 2/3 matched | `deserialize` | 0/0 matched | _none_ | 1 | 3 | 3010304.5 |
| 4 | `de.tuple` | `de.Tuple` | 0.00 | 2 | 0/3 matched | `deserialize`, `deserialize_newtype_struct`, `deserialize_in_place` | 0/0 matched | _none_ | 3 | 3 | 2030310.0 |
| 5 | `de.identifier` | `de.Identifier` | 0.57 | 2 | 3/3 matched | _none_ | 0/0 matched (target 1) | _none_ | 0 | 3 | 2000304.4 |
| 6 | `de.enum_externally` | `de.EnumExternally` | 0.61 | 1 | 3/4 matched | `deserialize` | 0/0 matched | _none_ | 1 | 4 | 1010403.9 |
| 7 | `de.enum_internally` | `de.EnumInternally [ZERO]` | 0.00 | 1 | 1/2 matched | `deserialize` | 0/0 matched | _none_ | 1 | 2 | 1010210.0 |
| 8 | `de.enum_adjacently` | `de.EnumAdjacently` | 0.00 | 1 | 0/1 matched | `deserialize` | 0/0 matched | _none_ | 1 | 1 | 1010110.0 |
| 9 | `internals.receiver` | `internals.Receiver` | 0.04 | 0 | 1/18 matched (target 1) | `self_ty`, `self_to_qself`, `self_to_expr_path`, `visit_type_mut`, `visit_type_path_mut`, `visit_expr_path_mut`, `visit_type_mut_impl`, `visit_type_path_mut_impl`, `visit_expr_path_mut_impl`, `visit_path_mut`, `visit_path_arguments_mut`, `visit_return_type_mut`, `visit_type_param_bound_mut`, `visit_generics_mut`, `visit_data_mut`, `visit_expr_mut`, `visit_macro_mut` | 0/1 matched (target 0) | `ReplaceReceiver` | 18 | 19 | 181909.6 |
| 10 | `de` | `de.De [ZERO]` | 0.00 | 0 | 30/32 matched (target 33) | `new`, `in_place` | 7/9 matched (target 15) | `InPlaceImplGenerics`, `InPlaceTypeGenerics` | 4 | 41 | 44110.0 |
| 11 | `ser` | `ser.Ser [ZERO]` | 0.00 | 0 | 34/36 matched | `new`, `effective_style` | 4/5 matched (target 8) | `Parameters` | 3 | 41 | 34110.0 |
| 12 | `internals.symbol` | `internals.Symbol` | 0.00 | 0 | 0/2 matched (target 4) | `eq`, `fmt` | 1/1 matched | _none_ | 2 | 3 | 20310.0 |
| 13 | `internals.case` | `internals.Case [ZERO]` | 0.00 | 0 | 6/7 matched (target 6) | `fmt` | 2/2 matched (target 5) | _none_ | 1 | 9 | 10910.0 |
| 14 | `de.unit` | `de.Unit` | 0.00 | 0 | 0/1 matched | `deserialize` | 0/0 matched | _none_ | 1 | 1 | 10110.0 |
| 15 | `bound` | `internals.Bound` | 0.73 | 0 | 17/17 matched | _none_ | 1/1 matched | _none_ | 0 | 18 | 1802.7 |
| 16 | `pretend` | `internals.Pretend [ZERO]` | 0.00 | 0 | 6/6 matched | _none_ | 0/0 matched | _none_ | 0 | 6 | 610.0 |
| 17 | `lib` | `serderive.Lib` | 0.66 | 0 | 4/4 matched | _none_ | 1/1 matched | _none_ | 0 | 5 | 503.4 |

## Cheat Detection / Scoring Failures

- `de.struct_` -> `de.Struct [STUB]`: function-by-function score forced to 0. target contains TODO/stub/placeholder markers in function bodies; Struct.kt: snake_case identifier `in_place` in Kotlin comments
- `de.enum_internally` -> `de.EnumInternally [ZERO]`: function-by-function score forced to 0. EnumInternally.kt: snake_case identifier `visit_seq` in Kotlin comments
- `de` -> `de.De [ZERO]`: function-by-function score forced to 0. De.kt: snake_case identifier `parse_quote` in Kotlin comments; De.kt: Rust lifetime explanation in Kotlin comments
- `ser` -> `ser.Ser [ZERO]`: function-by-function score forced to 0. Ser.kt: snake_case identifier `parse_quote` in Kotlin comments
- `internals.case` -> `internals.Case [ZERO]`: function-by-function score forced to 0. Case.kt: snake_case identifier `snake_case` in Kotlin comments
- `pretend` -> `internals.Pretend [ZERO]`: function-by-function score forced to 0. Pretend.kt: snake_case identifier `dead_code` in Kotlin comments

## Critical Issues (Function Similarity < 0.60 with Dependencies)

These files need immediate attention:

- **de.struct_** → `de.Struct [STUB]`
  - Function similarity: 0.00
  - Dependencies: 3
  - Functions: 3/5 matched
  - Missing functions: `deserialize`, `deserialize_in_place`
  - Types: 0/0 matched
  - Missing types: _none_
  - Scoring failure: target contains TODO/stub/placeholder markers in function bodies; Struct.kt: snake_case identifier `in_place` in Kotlin comments

- **de.enum_untagged** → `de.EnumUntagged`
  - Function similarity: 0.50
  - Dependencies: 3
  - Functions: 2/3 matched
  - Missing functions: `deserialize`
  - Types: 0/0 matched
  - Missing types: _none_

- **de.enum_** → `de.Enum`
  - Function similarity: 0.56
  - Dependencies: 3
  - Functions: 2/3 matched
  - Missing functions: `deserialize`
  - Types: 0/0 matched
  - Missing types: _none_

- **de.tuple** → `de.Tuple`
  - Function similarity: 0.00
  - Dependencies: 2
  - Functions: 0/3 matched
  - Missing functions: `deserialize`, `deserialize_newtype_struct`, `deserialize_in_place`
  - Types: 0/0 matched
  - Missing types: _none_

- **de.identifier** → `de.Identifier`
  - Function similarity: 0.57
  - Dependencies: 2
  - Functions: 3/3 matched
  - Missing functions: _none_
  - Types: 0/0 matched (target 1)
  - Missing types: _none_

- **de.enum_internally** → `de.EnumInternally [ZERO]`
  - Function similarity: 0.00
  - Dependencies: 1
  - Functions: 1/2 matched
  - Missing functions: `deserialize`
  - Types: 0/0 matched
  - Missing types: _none_
  - Scoring failure: EnumInternally.kt: snake_case identifier `visit_seq` in Kotlin comments

- **de.enum_adjacently** → `de.EnumAdjacently`
  - Function similarity: 0.00
  - Dependencies: 1
  - Functions: 0/1 matched
  - Missing functions: `deserialize`
  - Types: 0/0 matched
  - Missing types: _none_

## Missing Files (by Dependents)

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `internals.attr` | `internals.Attr` | 9 | 91 | 12 | 103 | `internals/attr.rs` | `internals/Attr.kt` |
| 2 | `internals.name` | `internals.Name` | 2 | 11 | 2 | 13 | `internals/name.rs` | `internals/Name.kt` |
| 3 | `fragment` | `Fragment` | 1 | 4 | 4 | 8 | `fragment.rs` | `Fragment.kt` |
| 4 | `internals.ctxt` | `internals.Ctxt` | 1 | 5 | 1 | 6 | `internals/ctxt.rs` | `internals/Ctxt.kt` |
| 5 | `internals.respan` | `internals.Respan` | 1 | 2 | 0 | 2 | `internals/respan.rs` | `internals/Respan.kt` |
| 6 | `deprecated` | `Deprecated` | 0 | 3 | 0 | 3 | `deprecated.rs` | `Deprecated.kt` |
| 7 | `dummy` | `Dummy` | 0 | 1 | 0 | 1 | `dummy.rs` | `Dummy.kt` |
| 8 | `internals.ast` | `internals.Ast` | 0 | 6 | 5 | 11 | `internals/ast.rs` | `internals/Ast.kt` |
| 9 | `internals.check` | `internals.Check` | 0 | 14 | 0 | 14 | `internals/check.rs` | `internals/Check.kt` |
| 10 | `this` | `This` | 0 | 2 | 0 | 2 | `this.rs` | `This.kt` |

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `internals.mod` | `internals.Mod` | 0 | `internals/mod.rs` | `internals/Mod.kt` |

