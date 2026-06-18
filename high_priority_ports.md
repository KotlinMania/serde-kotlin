# High Priority Ports - Action Plan

## Files by Impact

Priority = deps * 1,000,000 + SymDeficit * 10,000 + SrcSymbols * 100 + (1 - function similarity) * 10

Dependency fanout is ranked first so the ladder favors ports that clear downstream compilation failures fastest.

This list is complete and includes function/type detail for every matched file. Function similarity is the required body/parameter comparison; file-level shape does not rescue a port.

| Rank | Source | Target | Function similarity | Deps | Functions | Missing functions | Types | Missing types | SymDeficit | SrcSymbols | Priority |
|------|--------|--------|------------|------|-----------|-------------------|-------|---------------|-----------|------------|----------|

## Cheat Detection / Scoring Failures

_None detected._

## Critical Issues (Function Similarity < 0.60 with Dependencies)

No critical issues with dependencies.

## Missing Files (by Dependents)

| Rank | Source file | Expected target | Deps | Functions | Classes/types | Symbols | Source path | Expected path |
|------|-------------|-----------------|------|-----------|---------------|---------|-------------|---------------|
| 1 | `internals.attr` | `internals.Attr` | 9 | 91 | 12 | 103 | `src/internals/attr.rs` | `internals/Attr.kt` |
| 2 | `de.enum_` | `de.Enum` | 3 | 3 | 0 | 3 | `src/de/enum_.rs` | `de/Enum.kt` |
| 3 | `de.enum_untagged` | `de.EnumUntagged` | 3 | 3 | 0 | 3 | `src/de/enum_untagged.rs` | `de/EnumUntagged.kt` |
| 4 | `de.struct_` | `de.Struct` | 3 | 5 | 0 | 5 | `src/de/struct_.rs` | `de/Struct.kt` |
| 5 | `internals.name` | `internals.Name` | 2 | 11 | 2 | 13 | `src/internals/name.rs` | `internals/Name.kt` |
| 6 | `de.identifier` | `de.Identifier` | 2 | 3 | 0 | 3 | `src/de/identifier.rs` | `de/Identifier.kt` |
| 7 | `de.tuple` | `de.Tuple` | 2 | 3 | 0 | 3 | `src/de/tuple.rs` | `de/Tuple.kt` |
| 8 | `fragment` | `Fragment` | 1 | 4 | 4 | 8 | `src/fragment.rs` | `Fragment.kt` |
| 9 | `internals.respan` | `internals.Respan` | 1 | 2 | 0 | 2 | `src/internals/respan.rs` | `internals/Respan.kt` |
| 10 | `de.enum_adjacently` | `de.EnumAdjacently` | 1 | 1 | 0 | 1 | `src/de/enum_adjacently.rs` | `de/EnumAdjacently.kt` |
| 11 | `de.enum_externally` | `de.EnumExternally` | 1 | 4 | 0 | 4 | `src/de/enum_externally.rs` | `de/EnumExternally.kt` |
| 12 | `de.enum_internally` | `de.EnumInternally` | 1 | 2 | 0 | 2 | `src/de/enum_internally.rs` | `de/EnumInternally.kt` |
| 13 | `internals.ctxt` | `internals.Ctxt` | 1 | 5 | 1 | 6 | `src/internals/ctxt.rs` | `internals/Ctxt.kt` |
| 14 | `build` | `Build` | 0 | 1 | 0 | 1 | `build.rs` | `Build.kt` |
| 15 | `dummy` | `Dummy` | 0 | 1 | 0 | 1 | `src/dummy.rs` | `Dummy.kt` |
| 16 | `internals.ast` | `internals.Ast` | 0 | 6 | 5 | 11 | `src/internals/ast.rs` | `internals/Ast.kt` |
| 17 | `deprecated` | `Deprecated` | 0 | 3 | 0 | 3 | `src/deprecated.rs` | `Deprecated.kt` |
| 18 | `internals.case` | `internals.Case` | 0 | 7 | 2 | 9 | `src/internals/case.rs` | `internals/Case.kt` |
| 19 | `internals.check` | `internals.Check` | 0 | 14 | 0 | 14 | `src/internals/check.rs` | `internals/Check.kt` |
| 20 | `de.unit` | `de.Unit` | 0 | 1 | 0 | 1 | `src/de/unit.rs` | `de/Unit.kt` |
| 21 | `de` | `de.De` | 0 | 37 | 9 | 46 | `src/de.rs` | `de/De.kt` |
| 22 | `internals.receiver` | `internals.Receiver` | 0 | 18 | 1 | 19 | `src/internals/receiver.rs` | `internals/Receiver.kt` |
| 23 | `bound` | `Bound` | 0 | 17 | 1 | 18 | `src/bound.rs` | `Bound.kt` |
| 24 | `internals.symbol` | `internals.Symbol` | 0 | 5 | 1 | 6 | `src/internals/symbol.rs` | `internals/Symbol.kt` |
| 25 | `pretend` | `Pretend` | 0 | 6 | 0 | 6 | `src/pretend.rs` | `Pretend.kt` |
| 26 | `ser` | `Ser` | 0 | 36 | 5 | 41 | `src/ser.rs` | `Ser.kt` |
| 27 | `this` | `This` | 0 | 2 | 0 | 2 | `src/this.rs` | `This.kt` |

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

