# Code Port - Progress Report

**Generated:** 2026-06-18
**Source:** tmp
**Target:** serde-kotlin

## Executive Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| Function parity | 0/1665 matched | 0.0% |
| Class/type parity | 0/667 matched | 0.0% |
| Combined symbol parity | 0/2332 matched | 0.0% |
| Average function body similarity | N/A (no matched function-bearing files) | inline-code cosine |
| Average documentation similarity | 0.00 | doc text cosine |
| Missing source functions | 1665 | 0% parity until ported |
| Missing source classes/types | 667 | 0% parity until ported |
| Missing source symbol files | 190 | 2332 symbols |
| Cheat/scoring failures | 0 | forced to 0% |
| Total source files | 208 | 100% |
| Target units (paired) | 0 | - |
| Target files (total) | 0 | - |
| Porting progress | 0 | 0.0% (matched) |
| Missing files | 196 | 94.2% |
| Reexport/wiring files | 12 | consult-only |

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
| 1 | `private.de` | `serde.serde.src.private.De` | 0 | 257 | 42 | 299 | `serde/serde/src/private/de.rs` | `serde/serde/src/private/De.kt` |
| 2 | `de.impls` | `serde.serdecore.src.de.Impls` | 0 | 141 | 27 | 168 | `serde/serde_core/src/de/impls.rs` | `serde/serdecore/src/de/Impls.kt` |
| 3 | `tests.test_annotations` | `serde.testsuite.tests.TestAnnotations` | 0 | 84 | 76 | 160 | `serde/test_suite/tests/test_annotations.rs` | `serde/testsuite/tests/TestAnnotations.kt` |
| 4 | `private.ser` | `serde.serde.src.private.Ser` | 0 | 130 | 22 | 152 | `serde/serde/src/private/ser.rs` | `serde/serde/src/private/Ser.kt` |
| 5 | `de.value` | `serde.serdecore.src.de.Value` | 0 | 119 | 30 | 149 | `serde/serde_core/src/de/value.rs` | `serde/serdecore/src/de/Value.kt` |
| 6 | `tests.test_de` | `serde.testsuite.tests.TestDe` | 0 | 106 | 16 | 122 | `serde/test_suite/tests/test_de.rs` | `serde/testsuite/tests/TestDe.kt` |
| 7 | `tests.test_gen` | `serde.testsuite.tests.TestGen` | 0 | 16 | 106 | 122 | `serde/test_suite/tests/test_gen.rs` | `serde/testsuite/tests/TestGen.kt` |
| 8 | `internals.attr` | `serde.serdederive.src.internals.Attr` | 9 | 91 | 12 | 103 | `serde/serde_derive/src/internals/attr.rs` | `serde/serdederive/src/internals/Attr.kt` |
| 9 | `tests.test_de_error` | `serde.testsuite.tests.TestDeError` | 0 | 66 | 7 | 73 | `serde/test_suite/tests/test_de_error.rs` | `serde/testsuite/tests/TestDeError.kt` |
| 10 | `tests.test_ser` | `serde.testsuite.tests.TestSer` | 0 | 63 | 5 | 68 | `serde/test_suite/tests/test_ser.rs` | `serde/testsuite/tests/TestSer.kt` |
| 11 | `tests.test_macros` | `serde.testsuite.tests.TestMacros` | 0 | 27 | 26 | 53 | `serde/test_suite/tests/test_macros.rs` | `serde/testsuite/tests/TestMacros.kt` |
| 12 | `serde_derive.de` | `serde.serdederive.src.de.De` | 0 | 37 | 9 | 46 | `serde/serde_derive/src/de.rs` | `serde/serdederive/src/de/De.kt` |
| 13 | `tests.test_remote` | `serde.testsuite.tests.TestRemote` | 0 | 16 | 26 | 42 | `serde/test_suite/tests/test_remote.rs` | `serde/testsuite/tests/TestRemote.kt` |
| 14 | `serde_derive.ser` | `serde.serdederive.src.Ser` | 2 | 36 | 5 | 41 | `serde/serde_derive/src/ser.rs` | `serde/serdederive/src/Ser.kt` |
| 15 | `ser.impls` | `serde.serdecore.src.ser.Impls` | 0 | 41 | 0 | 41 | `serde/serde_core/src/ser/impls.rs` | `serde/serdecore/src/ser/Impls.kt` |
| 16 | `tests.test_enum_internally_tagged` | `serde.testsuite.tests.TestEnumInternallyTagged` | 0 | 24 | 8 | 32 | `serde/test_suite/tests/test_enum_internally_tagged.rs` | `serde/testsuite/tests/TestEnumInternallyTagged.kt` |
| 17 | `tests.test_enum_untagged` | `serde.testsuite.tests.TestEnumUntagged` | 0 | 19 | 10 | 29 | `serde/test_suite/tests/test_enum_untagged.rs` | `serde/testsuite/tests/TestEnumUntagged.kt` |
| 18 | `ser.fmt` | `serde.serdecore.src.ser.Fmt` | 3 | 16 | 9 | 25 | `serde/serde_core/src/ser/fmt.rs` | `serde/serdecore/src/ser/Fmt.kt` |
| 19 | `tests.test_enum_adjacently_tagged` | `serde.testsuite.tests.TestEnumAdjacentlyTagged` | 0 | 19 | 5 | 24 | `serde/test_suite/tests/test_enum_adjacently_tagged.rs` | `serde/testsuite/tests/TestEnumAdjacentlyTagged.kt` |
| 20 | `tests.test_borrow` | `serde.testsuite.tests.TestBorrow` | 0 | 15 | 7 | 22 | `serde/test_suite/tests/test_borrow.rs` | `serde/testsuite/tests/TestBorrow.kt` |
| 21 | `de.ignored_any` | `serde.serdecore.src.de.IgnoredAny` | 1 | 17 | 2 | 19 | `serde/serde_core/src/de/ignored_any.rs` | `serde/serdecore/src/de/IgnoredAny.kt` |
| 22 | `ser.impossible` | `serde.serdecore.src.ser.Impossible` | 1 | 15 | 4 | 19 | `serde/serde_core/src/ser/impossible.rs` | `serde/serdecore/src/ser/Impossible.kt` |
| 23 | `internals.receiver` | `serde.serdederive.src.internals.Receiver` | 0 | 18 | 1 | 19 | `serde/serde_derive/src/internals/receiver.rs` | `serde/serdederive/src/internals/Receiver.kt` |
| 24 | `serde_derive.bound` | `serde.serdederive.src.Bound` | 2 | 17 | 1 | 18 | `serde/serde_derive/src/bound.rs` | `serde/serdederive/src/Bound.kt` |
| 25 | `default-attribute.tuple_struct_path` | `serde.testsuite.tests.ui.defaultattribute.TupleStructPath` | 0 | 2 | 12 | 14 | `serde/test_suite/tests/ui/default-attribute/tuple_struct_path.rs` | `serde/testsuite/tests/ui/defaultattribute/TupleStructPath.kt` |
| 26 | `internals.check` | `serde.serdederive.src.internals.Check` | 0 | 14 | 0 | 14 | `serde/serde_derive/src/internals/check.rs` | `serde/serdederive/src/internals/Check.kt` |
| 27 | `internals.name` | `serde.serdederive.src.internals.Name` | 2 | 11 | 2 | 13 | `serde/serde_derive/src/internals/name.rs` | `serde/serdederive/src/internals/Name.kt` |
| 28 | `tests.test_value` | `serde.testsuite.tests.TestValue` | 0 | 6 | 6 | 12 | `serde/test_suite/tests/test_value.rs` | `serde/testsuite/tests/TestValue.kt` |
| 29 | `internals.ast` | `serde.serdederive.src.internals.Ast` | 0 | 6 | 5 | 11 | `serde/serde_derive/src/internals/ast.rs` | `serde/serdederive/src/internals/Ast.kt` |
| 30 | `tests.test_identifier` | `serde.testsuite.tests.TestIdentifier` | 0 | 9 | 2 | 11 | `serde/test_suite/tests/test_identifier.rs` | `serde/testsuite/tests/TestIdentifier.kt` |
| 31 | `tests.test_ignored_any` | `serde.testsuite.tests.TestIgnoredAny` | 0 | 7 | 4 | 11 | `serde/test_suite/tests/test_ignored_any.rs` | `serde/testsuite/tests/TestIgnoredAny.kt` |
| 32 | `tests.test_self` | `serde.testsuite.tests.TestSelf` | 0 | 4 | 6 | 10 | `serde/test_suite/tests/test_self.rs` | `serde/testsuite/tests/TestSelf.kt` |
| 33 | `default-attribute.tuple_struct` | `serde.testsuite.tests.ui.defaultattribute.TupleStruct` | 0 | 1 | 8 | 9 | `serde/test_suite/tests/ui/default-attribute/tuple_struct.rs` | `serde/testsuite/tests/ui/defaultattribute/TupleStruct.kt` |
| 34 | `internals.case` | `serde.serdederive.src.internals.Case` | 0 | 7 | 2 | 9 | `serde/serde_derive/src/internals/case.rs` | `serde/serdederive/src/internals/Case.kt` |
| 35 | `serde_derive.fragment` | `serde.serdederive.src.Fragment` | 1 | 4 | 4 | 8 | `serde/serde_derive/src/fragment.rs` | `serde/serdederive/src/Fragment.kt` |
| 36 | `no_std.main` | `serde.testsuite.nostd.src.Main` | 0 | 2 | 5 | 7 | `serde/test_suite/no_std/src/main.rs` | `serde/testsuite/nostd/src/Main.kt` |
| 37 | `regression.issue1904` | `serde.testsuite.tests.regression.Issue1904` | 0 | 0 | 7 | 7 | `serde/test_suite/tests/regression/issue1904.rs` | `serde/testsuite/tests/regression/Issue1904.kt` |
| 38 | `tests.test_serde_path` | `serde.testsuite.tests.TestSerdePath` | 0 | 2 | 5 | 7 | `serde/test_suite/tests/test_serde_path.rs` | `serde/testsuite/tests/TestSerdePath.kt` |
| 39 | `internals.ctxt` | `serde.serdederive.src.internals.Ctxt` | 1 | 5 | 1 | 6 | `serde/serde_derive/src/internals/ctxt.rs` | `serde/serdederive/src/internals/Ctxt.kt` |
| 40 | `internals.symbol` | `serde.serdederive.src.internals.Symbol` | 0 | 5 | 1 | 6 | `serde/serde_derive/src/internals/symbol.rs` | `serde/serdederive/src/internals/Symbol.kt` |
| 41 | `serde_derive.pretend` | `serde.serdederive.src.Pretend` | 0 | 6 | 0 | 6 | `serde/serde_derive/src/pretend.rs` | `serde/serdederive/src/Pretend.kt` |
| 42 | `with.incorrect_type` | `serde.testsuite.tests.ui.with.IncorrectType` | 0 | 3 | 3 | 6 | `serde/test_suite/tests/ui/with/incorrect_type.rs` | `serde/testsuite/tests/ui/with/IncorrectType.kt` |
| 43 | `de.struct_` | `serde.serdederive.src.de.Struct` | 3 | 5 | 0 | 5 | `serde/serde_derive/src/de/struct_.rs` | `serde/serdederive/src/de/Struct.kt` |
| 44 | `conflict.alias-enum` | `serde.testsuite.tests.ui.conflict.Alias-enum` | 0 | 1 | 4 | 5 | `serde/test_suite/tests/ui/conflict/alias-enum.rs` | `serde/testsuite/tests/ui/conflict/Alias-enum.kt` |
| 45 | `regression.issue2371` | `serde.testsuite.tests.regression.Issue2371` | 0 | 0 | 5 | 5 | `serde/test_suite/tests/regression/issue2371.rs` | `serde/testsuite/tests/regression/Issue2371.kt` |
| 46 | `de.enum_externally` | `serde.serdederive.src.de.EnumExternally` | 1 | 4 | 0 | 4 | `serde/serde_derive/src/de/enum_externally.rs` | `serde/serdederive/src/de/EnumExternally.kt` |
| 47 | `conflict.alias` | `serde.testsuite.tests.ui.conflict.Alias` | 0 | 1 | 3 | 4 | `serde/test_suite/tests/ui/conflict/alias.rs` | `serde/testsuite/tests/ui/conflict/Alias.kt` |
| 48 | `private.doc` | `serde.serdecore.src.private.Doc` | 0 | 3 | 1 | 4 | `serde/serde_core/src/private/doc.rs` | `serde/serdecore/src/private/Doc.kt` |
| 49 | `serde_core.format` | `serde.serdecore.src.Format` | 0 | 3 | 1 | 4 | `serde/serde_core/src/format.rs` | `serde/serdecore/src/Format.kt` |
| 50 | `tests.test_deprecated` | `serde.testsuite.tests.TestDeprecated` | 0 | 0 | 4 | 4 | `serde/test_suite/tests/test_deprecated.rs` | `serde/testsuite/tests/TestDeprecated.kt` |
| 51 | `unimplemented.required_locally` | `serde.testsuite.tests.ui.unimplemented.RequiredLocally` | 0 | 3 | 1 | 4 | `serde/test_suite/tests/ui/unimplemented/required_locally.rs` | `serde/testsuite/tests/ui/unimplemented/RequiredLocally.kt` |
| 52 | `de.enum_untagged` | `serde.serdederive.src.de.EnumUntagged` | 3 | 3 | 0 | 3 | `serde/serde_derive/src/de/enum_untagged.rs` | `serde/serdederive/src/de/EnumUntagged.kt` |
| 53 | `private.size_hint` | `serde.serdecore.src.private.SizeHint` | 3 | 3 | 0 | 3 | `serde/serde_core/src/private/size_hint.rs` | `serde/serdecore/src/private/SizeHint.kt` |
| 54 | `de.identifier` | `serde.serdederive.src.de.Identifier` | 2 | 3 | 0 | 3 | `serde/serde_derive/src/de/identifier.rs` | `serde/serdederive/src/de/Identifier.kt` |
| 55 | `de.tuple` | `serde.serdederive.src.de.Tuple` | 2 | 3 | 0 | 3 | `serde/serde_derive/src/de/tuple.rs` | `serde/serdederive/src/de/Tuple.kt` |
| 56 | `borrow.duplicate_variant` | `serde.testsuite.tests.ui.borrow.DuplicateVariant` | 0 | 1 | 2 | 3 | `serde/test_suite/tests/ui/borrow/duplicate_variant.rs` | `serde/testsuite/tests/ui/borrow/DuplicateVariant.kt` |
| 57 | `borrow.struct_variant` | `serde.testsuite.tests.ui.borrow.StructVariant` | 0 | 1 | 2 | 3 | `serde/test_suite/tests/ui/borrow/struct_variant.rs` | `serde/testsuite/tests/ui/borrow/StructVariant.kt` |
| 58 | `de.enum_` | `serde.serdederive.src.de.Enum` | 0 | 3 | 0 | 3 | `serde/serde_derive/src/de/enum_.rs` | `serde/serdederive/src/de/Enum.kt` |
| 59 | `deprecated.deprecated_de_with` | `serde.testsuite.tests.ui.deprecated.DeprecatedDeWith` | 0 | 2 | 1 | 3 | `serde/test_suite/tests/ui/deprecated/deprecated_de_with.rs` | `serde/testsuite/tests/ui/deprecated/DeprecatedDeWith.kt` |
| 60 | `deprecated.deprecated_ser_with` | `serde.testsuite.tests.ui.deprecated.DeprecatedSerWith` | 0 | 2 | 1 | 3 | `serde/test_suite/tests/ui/deprecated/deprecated_ser_with.rs` | `serde/testsuite/tests/ui/deprecated/DeprecatedSerWith.kt` |
| 61 | `private.seed` | `serde.serdecore.src.private.Seed` | 0 | 1 | 2 | 3 | `serde/serde_core/src/private/seed.rs` | `serde/serdecore/src/private/Seed.kt` |
| 62 | `regression.issue2565` | `serde.testsuite.tests.regression.Issue2565` | 0 | 2 | 1 | 3 | `serde/test_suite/tests/regression/issue2565.rs` | `serde/testsuite/tests/regression/Issue2565.kt` |
| 63 | `remote.double_generic` | `serde.testsuite.tests.ui.remote.DoubleGeneric` | 0 | 1 | 2 | 3 | `serde/test_suite/tests/ui/remote/double_generic.rs` | `serde/testsuite/tests/ui/remote/DoubleGeneric.kt` |
| 64 | `remote.nonremote_getter` | `serde.testsuite.tests.ui.remote.NonremoteGetter` | 0 | 2 | 1 | 3 | `serde/test_suite/tests/ui/remote/nonremote_getter.rs` | `serde/testsuite/tests/ui/remote/NonremoteGetter.kt` |
| 65 | `remote.wrong_getter` | `serde.testsuite.tests.ui.remote.WrongGetter` | 0 | 2 | 1 | 3 | `serde/test_suite/tests/ui/remote/wrong_getter.rs` | `serde/testsuite/tests/ui/remote/WrongGetter.kt` |
| 66 | `serde_derive.deprecated` | `serde.serdederive.src.Deprecated` | 0 | 3 | 0 | 3 | `serde/serde_derive/src/deprecated.rs` | `serde/serdederive/src/Deprecated.kt` |
| 67 | `transparent.enum` | `serde.testsuite.tests.ui.transparent.Enum` | 3 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/enum.rs` | `serde/testsuite/tests/ui/transparent/Enum.kt` |
| 68 | `unknown-attribute.field` | `serde.testsuite.tests.ui.unknownattribute.Field` | 2 | 1 | 1 | 2 | `serde/test_suite/tests/ui/unknown-attribute/field.rs` | `serde/testsuite/tests/ui/unknownattribute/Field.kt` |
| 69 | `de.enum_internally` | `serde.serdederive.src.de.EnumInternally` | 1 | 2 | 0 | 2 | `serde/serde_derive/src/de/enum_internally.rs` | `serde/serdederive/src/de/EnumInternally.kt` |
| 70 | `internals.respan` | `serde.serdederive.src.internals.Respan` | 1 | 2 | 0 | 2 | `serde/serde_derive/src/internals/respan.rs` | `serde/serdederive/src/internals/Respan.kt` |
| 71 | `type-attribute.try_from` | `serde.testsuite.tests.ui.typeattribute.TryFrom` | 1 | 1 | 1 | 2 | `serde/test_suite/tests/ui/type-attribute/try_from.rs` | `serde/testsuite/tests/ui/typeattribute/TryFrom.kt` |
| 72 | `unknown-attribute.container` | `serde.testsuite.tests.ui.unknownattribute.Container` | 1 | 1 | 1 | 2 | `serde/test_suite/tests/ui/unknown-attribute/container.rs` | `serde/testsuite/tests/ui/unknownattribute/Container.kt` |
| 73 | `unknown-attribute.variant` | `serde.testsuite.tests.ui.unknownattribute.Variant` | 1 | 1 | 1 | 2 | `serde/test_suite/tests/ui/unknown-attribute/variant.rs` | `serde/testsuite/tests/ui/unknownattribute/Variant.kt` |
| 74 | `borrow.bad_lifetimes` | `serde.testsuite.tests.ui.borrow.BadLifetimes` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/borrow/bad_lifetimes.rs` | `serde/testsuite/tests/ui/borrow/BadLifetimes.kt` |
| 75 | `borrow.duplicate_lifetime` | `serde.testsuite.tests.ui.borrow.DuplicateLifetime` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/borrow/duplicate_lifetime.rs` | `serde/testsuite/tests/ui/borrow/DuplicateLifetime.kt` |
| 76 | `borrow.empty_lifetimes` | `serde.testsuite.tests.ui.borrow.EmptyLifetimes` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/borrow/empty_lifetimes.rs` | `serde/testsuite/tests/ui/borrow/EmptyLifetimes.kt` |
| 77 | `borrow.no_lifetimes` | `serde.testsuite.tests.ui.borrow.NoLifetimes` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/borrow/no_lifetimes.rs` | `serde/testsuite/tests/ui/borrow/NoLifetimes.kt` |
| 78 | `borrow.wrong_lifetime` | `serde.testsuite.tests.ui.borrow.WrongLifetime` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/borrow/wrong_lifetime.rs` | `serde/testsuite/tests/ui/borrow/WrongLifetime.kt` |
| 79 | `conflict.adjacent-tag` | `serde.testsuite.tests.ui.conflict.Adjacent-tag` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/conflict/adjacent-tag.rs` | `serde/testsuite/tests/ui/conflict/Adjacent-tag.kt` |
| 80 | `conflict.flatten-newtype-struct` | `serde.testsuite.tests.ui.conflict.Flatten-newtype-struct` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/conflict/flatten-newtype-struct.rs` | `serde/testsuite/tests/ui/conflict/Flatten-newtype-struct.kt` |
| 81 | `conflict.flatten-tuple-struct` | `serde.testsuite.tests.ui.conflict.Flatten-tuple-struct` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/conflict/flatten-tuple-struct.rs` | `serde/testsuite/tests/ui/conflict/Flatten-tuple-struct.kt` |
| 82 | `conflict.from-try-from` | `serde.testsuite.tests.ui.conflict.From-try-from` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/conflict/from-try-from.rs` | `serde/testsuite/tests/ui/conflict/From-try-from.kt` |
| 83 | `conflict.internal-tag` | `serde.testsuite.tests.ui.conflict.Internal-tag` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/conflict/internal-tag.rs` | `serde/testsuite/tests/ui/conflict/Internal-tag.kt` |
| 84 | `conflict.internal-tag-alias` | `serde.testsuite.tests.ui.conflict.Internal-tag-alias` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/conflict/internal-tag-alias.rs` | `serde/testsuite/tests/ui/conflict/Internal-tag-alias.kt` |
| 85 | `default-attribute.enum` | `serde.testsuite.tests.ui.defaultattribute.Enum` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/enum.rs` | `serde/testsuite/tests/ui/defaultattribute/Enum.kt` |
| 86 | `default-attribute.enum_path` | `serde.testsuite.tests.ui.defaultattribute.EnumPath` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/enum_path.rs` | `serde/testsuite/tests/ui/defaultattribute/EnumPath.kt` |
| 87 | `default-attribute.incorrect_type_enum_adjacently_tagged` | `serde.testsuite.tests.ui.defaultattribute.IncorrectTypeEnumAdjacentlyTagged` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/incorrect_type_enum_adjacently_tagged.rs` | `serde/testsuite/tests/ui/defaultattribute/IncorrectTypeEnumAdjacentlyTagged.kt` |
| 88 | `default-attribute.incorrect_type_enum_externally_tagged` | `serde.testsuite.tests.ui.defaultattribute.IncorrectTypeEnumExternallyTagged` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/incorrect_type_enum_externally_tagged.rs` | `serde/testsuite/tests/ui/defaultattribute/IncorrectTypeEnumExternallyTagged.kt` |
| 89 | `default-attribute.incorrect_type_enum_internally_tagged` | `serde.testsuite.tests.ui.defaultattribute.IncorrectTypeEnumInternallyTagged` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/incorrect_type_enum_internally_tagged.rs` | `serde/testsuite/tests/ui/defaultattribute/IncorrectTypeEnumInternallyTagged.kt` |
| 90 | `default-attribute.incorrect_type_enum_untagged` | `serde.testsuite.tests.ui.defaultattribute.IncorrectTypeEnumUntagged` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/incorrect_type_enum_untagged.rs` | `serde/testsuite/tests/ui/defaultattribute/IncorrectTypeEnumUntagged.kt` |
| 91 | `default-attribute.incorrect_type_newtype` | `serde.testsuite.tests.ui.defaultattribute.IncorrectTypeNewtype` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/incorrect_type_newtype.rs` | `serde/testsuite/tests/ui/defaultattribute/IncorrectTypeNewtype.kt` |
| 92 | `default-attribute.incorrect_type_struct` | `serde.testsuite.tests.ui.defaultattribute.IncorrectTypeStruct` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/incorrect_type_struct.rs` | `serde/testsuite/tests/ui/defaultattribute/IncorrectTypeStruct.kt` |
| 93 | `default-attribute.incorrect_type_tuple` | `serde.testsuite.tests.ui.defaultattribute.IncorrectTypeTuple` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/incorrect_type_tuple.rs` | `serde/testsuite/tests/ui/defaultattribute/IncorrectTypeTuple.kt` |
| 94 | `default-attribute.unit` | `serde.testsuite.tests.ui.defaultattribute.Unit` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/unit.rs` | `serde/testsuite/tests/ui/defaultattribute/Unit.kt` |
| 95 | `default-attribute.unit_path` | `serde.testsuite.tests.ui.defaultattribute.UnitPath` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/default-attribute/unit_path.rs` | `serde/testsuite/tests/ui/defaultattribute/UnitPath.kt` |
| 96 | `duplicate-attribute.rename-and-ser` | `serde.testsuite.tests.ui.duplicateattribute.Rename-and-ser` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/duplicate-attribute/rename-and-ser.rs` | `serde/testsuite/tests/ui/duplicateattribute/Rename-and-ser.kt` |
| 97 | `duplicate-attribute.rename-ser-rename` | `serde.testsuite.tests.ui.duplicateattribute.Rename-ser-rename` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/duplicate-attribute/rename-ser-rename.rs` | `serde/testsuite/tests/ui/duplicateattribute/Rename-ser-rename.kt` |
| 98 | `duplicate-attribute.rename-ser-rename-ser` | `serde.testsuite.tests.ui.duplicateattribute.Rename-ser-rename-ser` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/duplicate-attribute/rename-ser-rename-ser.rs` | `serde/testsuite/tests/ui/duplicateattribute/Rename-ser-rename-ser.kt` |
| 99 | `duplicate-attribute.rename-ser-ser` | `serde.testsuite.tests.ui.duplicateattribute.Rename-ser-ser` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/duplicate-attribute/rename-ser-ser.rs` | `serde/testsuite/tests/ui/duplicateattribute/Rename-ser-ser.kt` |
| 100 | `duplicate-attribute.two-rename-ser` | `serde.testsuite.tests.ui.duplicateattribute.Two-rename-ser` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/duplicate-attribute/two-rename-ser.rs` | `serde/testsuite/tests/ui/duplicateattribute/Two-rename-ser.kt` |
| 101 | `duplicate-attribute.with-and-serialize-with` | `serde.testsuite.tests.ui.duplicateattribute.With-and-serialize-with` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/duplicate-attribute/with-and-serialize-with.rs` | `serde/testsuite/tests/ui/duplicateattribute/With-and-serialize-with.kt` |
| 102 | `enum-representation.content-no-tag` | `serde.testsuite.tests.ui.enumrepresentation.Content-no-tag` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/enum-representation/content-no-tag.rs` | `serde/testsuite/tests/ui/enumrepresentation/Content-no-tag.kt` |
| 103 | `enum-representation.internal-tuple-variant` | `serde.testsuite.tests.ui.enumrepresentation.Internal-tuple-variant` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/enum-representation/internal-tuple-variant.rs` | `serde/testsuite/tests/ui/enumrepresentation/Internal-tuple-variant.kt` |
| 104 | `enum-representation.partially_tagged_wrong_order` | `serde.testsuite.tests.ui.enumrepresentation.PartiallyTaggedWrongOrder` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/enum-representation/partially_tagged_wrong_order.rs` | `serde/testsuite/tests/ui/enumrepresentation/PartiallyTaggedWrongOrder.kt` |
| 105 | `enum-representation.untagged-and-adjacent` | `serde.testsuite.tests.ui.enumrepresentation.Untagged-and-adjacent` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/enum-representation/untagged-and-adjacent.rs` | `serde/testsuite/tests/ui/enumrepresentation/Untagged-and-adjacent.kt` |
| 106 | `enum-representation.untagged-and-content` | `serde.testsuite.tests.ui.enumrepresentation.Untagged-and-content` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/enum-representation/untagged-and-content.rs` | `serde/testsuite/tests/ui/enumrepresentation/Untagged-and-content.kt` |
| 107 | `enum-representation.untagged-and-internal` | `serde.testsuite.tests.ui.enumrepresentation.Untagged-and-internal` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/enum-representation/untagged-and-internal.rs` | `serde/testsuite/tests/ui/enumrepresentation/Untagged-and-internal.kt` |
| 108 | `enum-representation.untagged-struct` | `serde.testsuite.tests.ui.enumrepresentation.Untagged-struct` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/enum-representation/untagged-struct.rs` | `serde/testsuite/tests/ui/enumrepresentation/Untagged-struct.kt` |
| 109 | `expected-string.boolean` | `serde.testsuite.tests.ui.expectedstring.Boolean` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/expected-string/boolean.rs` | `serde/testsuite/tests/ui/expectedstring/Boolean.kt` |
| 110 | `expected-string.byte_character` | `serde.testsuite.tests.ui.expectedstring.ByteCharacter` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/expected-string/byte_character.rs` | `serde/testsuite/tests/ui/expectedstring/ByteCharacter.kt` |
| 111 | `expected-string.byte_string` | `serde.testsuite.tests.ui.expectedstring.ByteString` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/expected-string/byte_string.rs` | `serde/testsuite/tests/ui/expectedstring/ByteString.kt` |
| 112 | `expected-string.character` | `serde.testsuite.tests.ui.expectedstring.Character` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/expected-string/character.rs` | `serde/testsuite/tests/ui/expectedstring/Character.kt` |
| 113 | `expected-string.float` | `serde.testsuite.tests.ui.expectedstring.Float` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/expected-string/float.rs` | `serde/testsuite/tests/ui/expectedstring/Float.kt` |
| 114 | `expected-string.integer` | `serde.testsuite.tests.ui.expectedstring.Integer` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/expected-string/integer.rs` | `serde/testsuite/tests/ui/expectedstring/Integer.kt` |
| 115 | `identifier.both` | `serde.testsuite.tests.ui.identifier.Both` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/both.rs` | `serde/testsuite/tests/ui/identifier/Both.kt` |
| 116 | `identifier.field_struct` | `serde.testsuite.tests.ui.identifier.FieldStruct` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/field_struct.rs` | `serde/testsuite/tests/ui/identifier/FieldStruct.kt` |
| 117 | `identifier.field_tuple` | `serde.testsuite.tests.ui.identifier.FieldTuple` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/field_tuple.rs` | `serde/testsuite/tests/ui/identifier/FieldTuple.kt` |
| 118 | `identifier.newtype_not_last` | `serde.testsuite.tests.ui.identifier.NewtypeNotLast` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/newtype_not_last.rs` | `serde/testsuite/tests/ui/identifier/NewtypeNotLast.kt` |
| 119 | `identifier.not_unit` | `serde.testsuite.tests.ui.identifier.NotUnit` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/not_unit.rs` | `serde/testsuite/tests/ui/identifier/NotUnit.kt` |
| 120 | `identifier.other_not_last` | `serde.testsuite.tests.ui.identifier.OtherNotLast` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/other_not_last.rs` | `serde/testsuite/tests/ui/identifier/OtherNotLast.kt` |
| 121 | `identifier.other_untagged` | `serde.testsuite.tests.ui.identifier.OtherUntagged` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/other_untagged.rs` | `serde/testsuite/tests/ui/identifier/OtherUntagged.kt` |
| 122 | `identifier.other_variant` | `serde.testsuite.tests.ui.identifier.OtherVariant` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/other_variant.rs` | `serde/testsuite/tests/ui/identifier/OtherVariant.kt` |
| 123 | `identifier.variant_struct` | `serde.testsuite.tests.ui.identifier.VariantStruct` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/variant_struct.rs` | `serde/testsuite/tests/ui/identifier/VariantStruct.kt` |
| 124 | `identifier.variant_tuple` | `serde.testsuite.tests.ui.identifier.VariantTuple` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/identifier/variant_tuple.rs` | `serde/testsuite/tests/ui/identifier/VariantTuple.kt` |
| 125 | `malformed.bound` | `serde.testsuite.tests.ui.malformed.Bound` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/malformed/bound.rs` | `serde/testsuite/tests/ui/malformed/Bound.kt` |
| 126 | `malformed.cut_off` | `serde.testsuite.tests.ui.malformed.CutOff` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/malformed/cut_off.rs` | `serde/testsuite/tests/ui/malformed/CutOff.kt` |
| 127 | `malformed.not_list` | `serde.testsuite.tests.ui.malformed.NotList` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/malformed/not_list.rs` | `serde/testsuite/tests/ui/malformed/NotList.kt` |
| 128 | `malformed.rename` | `serde.testsuite.tests.ui.malformed.Rename` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/malformed/rename.rs` | `serde/testsuite/tests/ui/malformed/Rename.kt` |
| 129 | `malformed.str_suffix` | `serde.testsuite.tests.ui.malformed.StrSuffix` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/malformed/str_suffix.rs` | `serde/testsuite/tests/ui/malformed/StrSuffix.kt` |
| 130 | `malformed.trailing_expr` | `serde.testsuite.tests.ui.malformed.TrailingExpr` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/malformed/trailing_expr.rs` | `serde/testsuite/tests/ui/malformed/TrailingExpr.kt` |
| 131 | `precondition.deserialize_de_lifetime` | `serde.testsuite.tests.ui.precondition.DeserializeDeLifetime` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/precondition/deserialize_de_lifetime.rs` | `serde/testsuite/tests/ui/precondition/DeserializeDeLifetime.kt` |
| 132 | `precondition.deserialize_dst` | `serde.testsuite.tests.ui.precondition.DeserializeDst` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/precondition/deserialize_dst.rs` | `serde/testsuite/tests/ui/precondition/DeserializeDst.kt` |
| 133 | `precondition.serialize_field_identifier` | `serde.testsuite.tests.ui.precondition.SerializeFieldIdentifier` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/precondition/serialize_field_identifier.rs` | `serde/testsuite/tests/ui/precondition/SerializeFieldIdentifier.kt` |
| 134 | `precondition.serialize_variant_identifier` | `serde.testsuite.tests.ui.precondition.SerializeVariantIdentifier` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/precondition/serialize_variant_identifier.rs` | `serde/testsuite/tests/ui/precondition/SerializeVariantIdentifier.kt` |
| 135 | `private.string` | `serde.serdecore.src.private.String` | 0 | 2 | 0 | 2 | `serde/serde_core/src/private/string.rs` | `serde/serdecore/src/private/String.kt` |
| 136 | `regression.issue2792` | `serde.testsuite.tests.regression.Issue2792` | 0 | 0 | 2 | 2 | `serde/test_suite/tests/regression/issue2792.rs` | `serde/testsuite/tests/regression/Issue2792.kt` |
| 137 | `regression.issue2844` | `serde.testsuite.tests.regression.Issue2844` | 0 | 2 | 0 | 2 | `serde/test_suite/tests/regression/issue2844.rs` | `serde/testsuite/tests/regression/Issue2844.kt` |
| 138 | `remote.bad_getter` | `serde.testsuite.tests.ui.remote.BadGetter` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/remote/bad_getter.rs` | `serde/testsuite/tests/ui/remote/BadGetter.kt` |
| 139 | `remote.bad_remote` | `serde.testsuite.tests.ui.remote.BadRemote` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/remote/bad_remote.rs` | `serde/testsuite/tests/ui/remote/BadRemote.kt` |
| 140 | `remote.enum_getter` | `serde.testsuite.tests.ui.remote.EnumGetter` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/remote/enum_getter.rs` | `serde/testsuite/tests/ui/remote/EnumGetter.kt` |
| 141 | `remote.missing_field` | `serde.testsuite.tests.ui.remote.MissingField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/remote/missing_field.rs` | `serde/testsuite/tests/ui/remote/MissingField.kt` |
| 142 | `remote.unknown_field` | `serde.testsuite.tests.ui.remote.UnknownField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/remote/unknown_field.rs` | `serde/testsuite/tests/ui/remote/UnknownField.kt` |
| 143 | `remote.wrong_de` | `serde.testsuite.tests.ui.remote.WrongDe` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/remote/wrong_de.rs` | `serde/testsuite/tests/ui/remote/WrongDe.kt` |
| 144 | `remote.wrong_ser` | `serde.testsuite.tests.ui.remote.WrongSer` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/remote/wrong_ser.rs` | `serde/testsuite/tests/ui/remote/WrongSer.kt` |
| 145 | `rename.container_unknown_rename_rule` | `serde.testsuite.tests.ui.rename.ContainerUnknownRenameRule` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/rename/container_unknown_rename_rule.rs` | `serde/testsuite/tests/ui/rename/ContainerUnknownRenameRule.kt` |
| 146 | `rename.variant_unknown_rename_rule` | `serde.testsuite.tests.ui.rename.VariantUnknownRenameRule` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/rename/variant_unknown_rename_rule.rs` | `serde/testsuite/tests/ui/rename/VariantUnknownRenameRule.kt` |
| 147 | `serde.build` | `serde.serde.Build` | 0 | 2 | 0 | 2 | `serde/serde/build.rs` | `serde/serde/Build.kt` |
| 148 | `serde_core.build` | `serde.serdecore.Build` | 0 | 2 | 0 | 2 | `serde/serde_core/build.rs` | `serde/serdecore/Build.kt` |
| 149 | `serde_core.std_error` | `serde.serdecore.src.StdError` | 0 | 1 | 1 | 2 | `serde/serde_core/src/std_error.rs` | `serde/serdecore/src/StdError.kt` |
| 150 | `serde_derive.this` | `serde.serdederive.src.This` | 0 | 2 | 0 | 2 | `serde/serde_derive/src/this.rs` | `serde/serdederive/src/This.kt` |
| 151 | `struct-representation.internally-tagged-tuple` | `serde.testsuite.tests.ui.structrepresentation.Internally-tagged-tuple` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/struct-representation/internally-tagged-tuple.rs` | `serde/testsuite/tests/ui/structrepresentation/Internally-tagged-tuple.kt` |
| 152 | `struct-representation.internally-tagged-unit` | `serde.testsuite.tests.ui.structrepresentation.Internally-tagged-unit` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/struct-representation/internally-tagged-unit.rs` | `serde/testsuite/tests/ui/structrepresentation/Internally-tagged-unit.kt` |
| 153 | `tests.test_roundtrip` | `serde.testsuite.tests.TestRoundtrip` | 0 | 2 | 0 | 2 | `serde/test_suite/tests/test_roundtrip.rs` | `serde/testsuite/tests/TestRoundtrip.kt` |
| 154 | `transparent.at_most_one` | `serde.testsuite.tests.ui.transparent.AtMostOne` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/at_most_one.rs` | `serde/testsuite/tests/ui/transparent/AtMostOne.kt` |
| 155 | `transparent.de_at_least_one` | `serde.testsuite.tests.ui.transparent.DeAtLeastOne` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/de_at_least_one.rs` | `serde/testsuite/tests/ui/transparent/DeAtLeastOne.kt` |
| 156 | `transparent.ser_at_least_one` | `serde.testsuite.tests.ui.transparent.SerAtLeastOne` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/ser_at_least_one.rs` | `serde/testsuite/tests/ui/transparent/SerAtLeastOne.kt` |
| 157 | `transparent.unit_struct` | `serde.testsuite.tests.ui.transparent.UnitStruct` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/unit_struct.rs` | `serde/testsuite/tests/ui/transparent/UnitStruct.kt` |
| 158 | `transparent.with_from` | `serde.testsuite.tests.ui.transparent.WithFrom` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/with_from.rs` | `serde/testsuite/tests/ui/transparent/WithFrom.kt` |
| 159 | `transparent.with_into` | `serde.testsuite.tests.ui.transparent.WithInto` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/with_into.rs` | `serde/testsuite/tests/ui/transparent/WithInto.kt` |
| 160 | `transparent.with_try_from` | `serde.testsuite.tests.ui.transparent.WithTryFrom` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/transparent/with_try_from.rs` | `serde/testsuite/tests/ui/transparent/WithTryFrom.kt` |
| 161 | `type-attribute.from` | `serde.testsuite.tests.ui.typeattribute.From` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/type-attribute/from.rs` | `serde/testsuite/tests/ui/typeattribute/From.kt` |
| 162 | `type-attribute.into` | `serde.testsuite.tests.ui.typeattribute.Into` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/type-attribute/into.rs` | `serde/testsuite/tests/ui/typeattribute/Into.kt` |
| 163 | `unexpected-literal.container` | `serde.testsuite.tests.ui.unexpectedliteral.Container` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/unexpected-literal/container.rs` | `serde/testsuite/tests/ui/unexpectedliteral/Container.kt` |
| 164 | `unexpected-literal.field` | `serde.testsuite.tests.ui.unexpectedliteral.Field` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/unexpected-literal/field.rs` | `serde/testsuite/tests/ui/unexpectedliteral/Field.kt` |
| 165 | `unexpected-literal.variant` | `serde.testsuite.tests.ui.unexpectedliteral.Variant` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/unexpected-literal/variant.rs` | `serde/testsuite/tests/ui/unexpectedliteral/Variant.kt` |
| 166 | `unimplemented.required_by_dependency` | `serde.testsuite.tests.ui.unimplemented.RequiredByDependency` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/unimplemented/required_by_dependency.rs` | `serde/testsuite/tests/ui/unimplemented/RequiredByDependency.kt` |
| 167 | `with-variant.skip_de_newtype_field` | `serde.testsuite.tests.ui.withvariant.SkipDeNewtypeField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_de_newtype_field.rs` | `serde/testsuite/tests/ui/withvariant/SkipDeNewtypeField.kt` |
| 168 | `with-variant.skip_de_struct_field` | `serde.testsuite.tests.ui.withvariant.SkipDeStructField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_de_struct_field.rs` | `serde/testsuite/tests/ui/withvariant/SkipDeStructField.kt` |
| 169 | `with-variant.skip_de_tuple_field` | `serde.testsuite.tests.ui.withvariant.SkipDeTupleField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_de_tuple_field.rs` | `serde/testsuite/tests/ui/withvariant/SkipDeTupleField.kt` |
| 170 | `with-variant.skip_de_whole_variant` | `serde.testsuite.tests.ui.withvariant.SkipDeWholeVariant` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_de_whole_variant.rs` | `serde/testsuite/tests/ui/withvariant/SkipDeWholeVariant.kt` |
| 171 | `with-variant.skip_ser_newtype_field` | `serde.testsuite.tests.ui.withvariant.SkipSerNewtypeField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_ser_newtype_field.rs` | `serde/testsuite/tests/ui/withvariant/SkipSerNewtypeField.kt` |
| 172 | `with-variant.skip_ser_newtype_field_if` | `serde.testsuite.tests.ui.withvariant.SkipSerNewtypeFieldIf` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_ser_newtype_field_if.rs` | `serde/testsuite/tests/ui/withvariant/SkipSerNewtypeFieldIf.kt` |
| 173 | `with-variant.skip_ser_struct_field` | `serde.testsuite.tests.ui.withvariant.SkipSerStructField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_ser_struct_field.rs` | `serde/testsuite/tests/ui/withvariant/SkipSerStructField.kt` |
| 174 | `with-variant.skip_ser_struct_field_if` | `serde.testsuite.tests.ui.withvariant.SkipSerStructFieldIf` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_ser_struct_field_if.rs` | `serde/testsuite/tests/ui/withvariant/SkipSerStructFieldIf.kt` |
| 175 | `with-variant.skip_ser_tuple_field` | `serde.testsuite.tests.ui.withvariant.SkipSerTupleField` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_ser_tuple_field.rs` | `serde/testsuite/tests/ui/withvariant/SkipSerTupleField.kt` |
| 176 | `with-variant.skip_ser_tuple_field_if` | `serde.testsuite.tests.ui.withvariant.SkipSerTupleFieldIf` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_ser_tuple_field_if.rs` | `serde/testsuite/tests/ui/withvariant/SkipSerTupleFieldIf.kt` |
| 177 | `with-variant.skip_ser_whole_variant` | `serde.testsuite.tests.ui.withvariant.SkipSerWholeVariant` | 0 | 1 | 1 | 2 | `serde/test_suite/tests/ui/with-variant/skip_ser_whole_variant.rs` | `serde/testsuite/tests/ui/withvariant/SkipSerWholeVariant.kt` |
| 178 | `private.content` | `serde.serdecore.src.private.Content` | 2 | 0 | 1 | 1 | `serde/serde_core/src/private/content.rs` | `serde/serdecore/src/private/Content.kt` |
| 179 | `de.enum_adjacently` | `serde.serdederive.src.de.EnumAdjacently` | 1 | 1 | 0 | 1 | `serde/serde_derive/src/de/enum_adjacently.rs` | `serde/serdederive/src/de/EnumAdjacently.kt` |
| 180 | `de.unit` | `serde.serdederive.src.de.Unit` | 0 | 1 | 0 | 1 | `serde/serde_derive/src/de/unit.rs` | `serde/serdederive/src/de/Unit.kt` |
| 181 | `default-attribute.union` | `serde.testsuite.tests.ui.defaultattribute.Union` | 0 | 1 | 0 | 1 | `serde/test_suite/tests/ui/default-attribute/union.rs` | `serde/testsuite/tests/ui/defaultattribute/Union.kt` |
| 182 | `default-attribute.union_path` | `serde.testsuite.tests.ui.defaultattribute.UnionPath` | 0 | 1 | 0 | 1 | `serde/test_suite/tests/ui/default-attribute/union_path.rs` | `serde/testsuite/tests/ui/defaultattribute/UnionPath.kt` |
| 183 | `regression.issue2415` | `serde.testsuite.tests.regression.Issue2415` | 0 | 0 | 1 | 1 | `serde/test_suite/tests/regression/issue2415.rs` | `serde/testsuite/tests/regression/Issue2415.kt` |
| 184 | `regression.issue2846` | `serde.testsuite.tests.regression.Issue2846` | 0 | 1 | 0 | 1 | `serde/test_suite/tests/regression/issue2846.rs` | `serde/testsuite/tests/regression/Issue2846.kt` |
| 185 | `serde_derive.build` | `serde.serdederive.Build` | 0 | 1 | 0 | 1 | `serde/serde_derive/build.rs` | `serde/serdederive/Build.kt` |
| 186 | `serde_derive.dummy` | `serde.serdederive.src.Dummy` | 0 | 1 | 0 | 1 | `serde/serde_derive/src/dummy.rs` | `serde/serdederive/src/Dummy.kt` |
| 187 | `serde_derive_internals.build` | `serde.serdederiveinternals.Build` | 0 | 1 | 0 | 1 | `serde/serde_derive_internals/build.rs` | `serde/serdederiveinternals/Build.kt` |
| 188 | `tests.compiletest` | `serde.testsuite.tests.Compiletest` | 0 | 1 | 0 | 1 | `serde/test_suite/tests/compiletest.rs` | `serde/testsuite/tests/Compiletest.kt` |
| 189 | `unsupported.union_de` | `serde.testsuite.tests.ui.unsupported.UnionDe` | 0 | 1 | 0 | 1 | `serde/test_suite/tests/ui/unsupported/union_de.rs` | `serde/testsuite/tests/ui/unsupported/UnionDe.kt` |
| 190 | `unsupported.union_ser` | `serde.testsuite.tests.ui.unsupported.UnionSer` | 0 | 1 | 0 | 1 | `serde/test_suite/tests/ui/unsupported/union_ser.rs` | `serde/testsuite/tests/ui/unsupported/UnionSer.kt` |
| 191 | `regression.issue2409` | `serde.testsuite.tests.regression.Issue2409` | 0 | 0 | 0 | 0 | `serde/test_suite/tests/regression/issue2409.rs` | `serde/testsuite/tests/regression/Issue2409.kt` |
| 192 | `serde.integer128` | `serde.serde.src.Integer128` | 0 | 0 | 0 | 0 | `serde/serde/src/integer128.rs` | `serde/serde/src/Integer128.kt` |
| 193 | `serde_core.crate_root` | `serde.serdecore.src.CrateRoot` | 0 | 0 | 0 | 0 | `serde/serde_core/src/crate_root.rs` | `serde/serdecore/src/CrateRoot.kt` |
| 194 | `serde_core.macros` | `serde.serdecore.src.Macros` | 0 | 0 | 0 | 0 | `serde/serde_core/src/macros.rs` | `serde/serdecore/src/Macros.kt` |
| 195 | `tests.regression` | `serde.testsuite.tests.regression.Regression` | 0 | 0 | 0 | 0 | `serde/test_suite/tests/regression.rs` | `serde/testsuite/tests/regression/Regression.kt` |
| 196 | `tests.test_unstable` | `serde.testsuite.tests.TestUnstable` | 0 | 0 | 0 | 0 | `serde/test_suite/tests/test_unstable.rs` | `serde/testsuite/tests/TestUnstable.kt` |

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

