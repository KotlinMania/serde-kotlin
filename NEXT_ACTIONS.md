# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 42/208 (20.2%)
- **Function parity:** 236/1197 matched (target 1793) — 19.7%
- **Class/type parity:** 133/698 matched (target 355) — 19.1%
- **Combined symbol parity:** 369/1895 matched (target 2148) — 19.5%
- **Average inline-code cosine:** 0.42 (function body across 32 matched files)
- **Average documentation cosine:** 0.36 (doc text across 32 matched files)
- **Cheat-zeroed Files:** 16
- **Critical Issues:** 31 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. ser.fmt

- **Target:** `ser.Fmt`
- **Similarity:** 0.44
- **Dependents:** 3
- **Priority Score:** 3122505.5
- **Functions:** 13/16 matched (target 41)
- **Missing functions:** `serialize_newtype_struct`, `serialize_some`, `serialize_newtype_variant`
- **Types:** 0/9 matched (target 4)
- **Missing types:** `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant`
- **Lint issues:** 4

### 2. private.size_hint

- **Target:** `private.SizeHint`
- **Similarity:** 0.88
- **Dependents:** 3
- **Priority Score:** 3000301.2
- **Functions:** 3/3 matched (target 9)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 3)
- **Missing types:** _none_

### 3. internals.name

- **Target:** `internals.Name`
- **Similarity:** 0.80
- **Dependents:** 2
- **Priority Score:** 2001202.0
- **Functions:** 10/10 matched (target 16)
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_

### 4. private.content

- **Target:** `private.Content`
- **Similarity:** 1.00
- **Dependents:** 2
- **Priority Score:** 2000100.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 23)
- **Missing types:** _none_

### 5. tests.test_annotations

- **Target:** `private.TestAnnotationsDeTest`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 1494910.0
- **Functions:** 1/73 matched (target 9)
- **Missing functions:** `my_default`, `should_skip`, `serialize_with`, `deserialize_with`, `test_default_struct`, `test_default_tuple`, `test_default_struct_variant`, `test_default_tuple_variant`, `test_no_std_default`, `default`, `test_elt_not_deserialize`, `test_ignore_unknown`, `test_rename_struct`, `test_unknown_field_rename_struct`, `test_rename_enum`, `test_unknown_field_rename_enum`, `test_skip_serializing_struct`, `test_skip_serializing_tuple_struct`, `test_skip_struct`, `test_skip_serializing_enum`, `test_elt_not_serialize`, `test_serialize_with_struct`, `test_serialize_with_enum`, `serialize_unit_variant_as_i8`, `deserialize_i8_as_unit_variant`, `serialize_variant_as_string`, `deserialize_string_as_variant`, `test_serialize_with_variant`, `test_deserialize_with_variant`, `test_deserialize_with_struct`, `test_deserialize_with_enum`, `test_missing_renamed_field_struct`, `test_missing_renamed_field_enum`, `test_invalid_length_enum`, `into`, `from`, `try_from`, `test_from_into_traits`, `test_collect_other`, `test_partially_untagged_enum`, `test_partially_untagged_enum_generic`, `test_partially_untagged_enum_desugared`, `assert_tokens_desugared`, `test_partially_untagged_internally_tagged_enum`, `test_transparent_struct`, `test_transparent_tuple_struct`, `test_expecting_message`, `test_expecting_message_externally_tagged_enum`, `test_expecting_message_identifier_enum`, `complex`, `map_twice`, `unsupported_type`, `unknown_field`, `non_string_keys`, `lifetime_propagation`, `enum_tuple_and_struct`, `option`, `ignored_any`, `flatten_any_after_flatten_struct`, `deserialize`, `visit_map`, `alias`, `unit`, `unit_struct`, `straightforward`, `newtype`, `tuple`, `struct_from_seq`, `struct_from_map`, `struct_`, `structs`, `unit_enum_with_unknown_fields`
- **Types:** 0/76 matched (target 4)
- **Missing types:** `MyDefault`, `ShouldSkip`, `SerializeWith`, `DeserializeWith`, `DefaultStruct`, `DefaultTupleStruct`, `CollectOther`, `DefaultStructVariant`, `DefaultTupleVariant`, `NoStdDefault`, `ContainsNoStdDefault`, `NotDeserializeStruct`, `NotDeserializeEnum`, `ContainsNotDeserialize`, `DenyUnknown`, `RenameStruct`, `RenameStructSerializeDeserialize`, `AliasStruct`, `RenameEnum`, `RenameEnumSerializeDeserialize`, `AliasEnum`, `SkipSerializingStruct`, `SkipSerializingTupleStruct`, `SkipStruct`, `SkipSerializingEnum`, `NotSerializeStruct`, `NotSerializeEnum`, `ContainsNotSerialize`, `SerializeWithStruct`, `SerializeWithEnum`, `WithVariant`, `DeserializeWithStruct`, `DeserializeWithEnum`, `InvalidLengthEnum`, `StructFromEnum`, `EnumToU32`, `TryFromU32`, `Error`, `Exp`, `Trait`, `E`, `Assoc`, `Assoc2`, `MyE`, `Test`, `TestUntagged`, `Data`, `Transparent`, `Unit`, `Newtype`, `Tuple`, `Struct`, `Enum`, `FieldEnum`, `VariantEnum`, `Outer`, `First`, `Second`, `Inner`, `TestStruct`, `A`, `B`, `C`, `Nested`, `Inner1`, `Inner2`, `Any`, `AnyVisitor`, `Value`, `Response`, `Flat`, `Flatten`, `NewtypeWrapper`, `NewtypeVariant`, `X`, `Y`
- **Tests:** 0/57 matched

### 6. de.ignored_any

- **Target:** `de.IgnoredAny`
- **Similarity:** 0.44
- **Dependents:** 1
- **Priority Score:** 1071905.6
- **Functions:** 11/17 matched (target 11)
- **Missing functions:** `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Value`
- **Lint issues:** 8

### 7. ser.impossible

- **Target:** `ser.Impossible`
- **Similarity:** 0.05
- **Dependents:** 1
- **Priority Score:** 1060909.5
- **Functions:** 1/5 matched (target 3)
- **Missing functions:** `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Ok`, `Error`
- **Lint issues:** 1

### 8. internals.ctxt

- **Target:** `internals.Ctxt`
- **Similarity:** 0.51
- **Dependents:** 1
- **Priority Score:** 1010604.9
- **Functions:** 4/5 matched (target 4)
- **Missing functions:** `drop`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 9. serde_derive.fragment

- **Target:** `serdederive.Fragment`
- **Similarity:** 0.85
- **Dependents:** 1
- **Priority Score:** 1000601.5
- **Functions:** 2/2 matched (target 6)
- **Missing functions:** _none_
- **Types:** 4/4 matched (target 5)
- **Missing types:** _none_

### 10. internals.respan

- **Target:** `internals.Respan`
- **Similarity:** 0.86
- **Dependents:** 1
- **Priority Score:** 1000201.4
- **Functions:** 2/2 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 11. de.mod

- **Target:** `de.Error [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 466510.0
- **Functions:** 7/48 matched (target 21)
- **Missing functions:** `fmt`, `deserialize_in_place`, `deserialize`, `__deserialize_content_v1`, `visit_bool`, `visit_i8`, `visit_i16`, `visit_i32`, `visit_i64`, `visit_i128`, `visit_u8`, `visit_u16`, `visit_u32`, `visit_u64`, `visit_u128`, `visit_f32`, `visit_f64`, `visit_char`, `visit_str`, `visit_borrowed_str`, `visit_string`, `visit_bytes`, `visit_borrowed_bytes`, `visit_byte_buf`, `visit_none`, `visit_some`, `visit_unit`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__private_visit_untagged_option`, `next_element_seed`, `next_key`, `next_value`, `next_entry`, `next_key_seed`, `next_value_seed`, `variant`, `write_str`, `write_char`
- **Types:** 12/17 matched (target 32)
- **Missing types:** `Expected`, `Value`, `Visitor`, `OneOf`, `LookForDecimalPoint`

### 12. de.impls

- **Target:** `de.Impls`
- **Similarity:** 0.08
- **Dependents:** 0
- **Priority Score:** 314909.2
- **Functions:** 12/22 matched (target 149)
- **Missing functions:** `deserialize`, `deserialize_in_place`, `visit_seq`, `visit_some`, `__private_visit_untagged_option`, `nop_reserve`, `new`, `visit_enum`, `check_overflow`, `visit_map`
- **Types:** 6/27 matched (target 36)
- **Missing types:** `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor`

### 13. tests.test_enum_untagged

- **Target:** `private.TestEnumUntaggedDeTest`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 282810.0
- **Functions:** 0/18 matched (target 20)
- **Missing functions:** `complex`, `newtype_unit_and_empty_map`, `newtype_struct`, `unit`, `newtype`, `tuple0`, `tuple2`, `struct_from_map`, `struct_from_seq`, `empty_struct_from_map`, `empty_struct_from_seq`, `some`, `some_without_marker`, `none`, `string_and_bytes`, `contains_flatten`, `contains_flatten_with_integer_key`, `expecting_message`
- **Types:** 0/10 matched (target 7)
- **Missing types:** `Untagged`, `Unit`, `Message`, `NewtypeStruct`, `E`, `Outer`, `Inner`, `Enum`, `Data`, `Flat`
- **Tests:** 0/18 matched

### 14. private.de

- **Target:** `private.De [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 203110.0
- **Functions:** 78/89 matched (target 537)
- **Missing functions:** `deserialize`, `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__deserialize_content_v1`, `fmt`, `clone`, `into_deserializer`, `from`
- **Types:** 34/42 matched
- **Missing types:** `Error`, `CowStrVisitor`, `Value`, `CowBytesVisitor`, `Variant`, `Deserializer`, `InternallyTaggedUnitVisitor`, `UntaggedUnitVisitor`

### 15. de.value

- **Target:** `value.Value`
- **Similarity:** 0.57
- **Dependents:** 0
- **Priority Score:** 155704.3
- **Functions:** 19/27 matched (target 615)
- **Missing functions:** `fmt`, `description`, `clone`, `next_pair`, `next_entry_seed`, `unit_only`, `map_as_enum`, `split`
- **Types:** 23/30 matched (target 44)
- **Missing types:** `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second`

### 16. private.ser

- **Target:** `private.Ser [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 146310.0
- **Functions:** 29/41 matched (target 94)
- **Missing functions:** `serialize_tagged_newtype`, `fmt`, `serialize_some`, `serialize_newtype_struct`, `serialize_newtype_variant`, `new`, `serialize_field`, `serialize`, `serialize_element`, `serialize_key`, `serialize_value`, `serialize_entry`
- **Types:** 20/22 matched (target 48)
- **Missing types:** `Ok`, `Error`

### 17. bytes.mod

- **Target:** `de.OneOf [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 90910.0
- **Functions:** 0/7 matched (target 1)
- **Missing functions:** `deserialize`, `expecting`, `visit_seq`, `visit_bytes`, `visit_byte_buf`, `visit_str`, `visit_string`
- **Types:** 0/2 matched (target 1)
- **Missing types:** `ByteBufVisitor`, `Value`
- **Provenance warning:** port-lint provenance header matched only by basename: `serde_core/src/de/mod.rs` vs expected `test_suite/tests/bytes/mod.rs`
- **Proposed provenance header:** `// port-lint: source test_suite/tests/bytes/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Lint issues:** 1

### 18. ser.mod

- **Target:** `ser.Error [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 31810.0
- **Functions:** 6/9 matched
- **Missing functions:** `collect_seq`, `collect_map`, `serialize_entry`
- **Types:** 9/9 matched (target 12)
- **Missing types:** _none_

### 19. tests.test_value

- **Target:** `private.TestValueTest`
- **Similarity:** 0.31
- **Dependents:** 0
- **Priority Score:** 31206.9
- **Functions:** 4/6 matched (target 17)
- **Missing functions:** `deserialize`, `visit_map`
- **Types:** 5/6 matched (target 20)
- **Missing types:** `Value`
- **Tests:** 3/3 matched

### 20. tests.test_ignored_any

- **Target:** `private.TestIgnoredAnyTest`
- **Similarity:** 0.70
- **Dependents:** 0
- **Priority Score:** 31103.0
- **Functions:** 7/7 matched (target 43)
- **Missing functions:** _none_
- **Types:** 1/4 matched (target 14)
- **Missing types:** `Enum`, `Error`, `Variant`
- **Tests:** 1/1 matched

### 21. private.seed

- **Target:** `private.Seed [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20310.0
- **Functions:** 0/1 matched (target 0)
- **Missing functions:** `deserialize`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Value`

### 22. macros.mod

- **Target:** `de.Visitor [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20210.0
- **Functions:** 0/1 matched (target 22)
- **Missing functions:** `into_iter`
- **Types:** 0/1 matched
- **Missing types:** `SingleTokenIntoIterator`
- **Provenance warning:** port-lint provenance header matched only by basename: `serde_core/src/de/mod.rs` vs expected `test_suite/tests/macros/mod.rs`
- **Proposed provenance header:** `// port-lint: source test_suite/tests/macros/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Lint issues:** 1

### 23. ser.impls

- **Target:** `ser.Impls`
- **Similarity:** 0.04
- **Dependents:** 0
- **Priority Score:** 10309.6
- **Functions:** 2/3 matched (target 43)
- **Missing functions:** `serialize`
- **Types:** 0/0 matched (target 15)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 24. unstable.mod

- **Target:** `unstable.ModTest [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10210.0
- **Functions:** 1/1 matched (target 28)
- **Missing functions:** _none_
- **Types:** 0/1 matched (target 12)
- **Missing types:** `r#type`
- **Tests:** 1/1 matched

### 25. internals.case

- **Target:** `internals.Case`
- **Similarity:** 0.76
- **Dependents:** 0
- **Priority Score:** 902.4
- **Functions:** 7/7 matched (target 14)
- **Missing functions:** _none_
- **Types:** 2/2 matched (target 5)
- **Missing types:** _none_
- **Tests:** 2/2 matched

### 26. private.doc

- **Target:** `private.Doc`
- **Similarity:** 0.24
- **Dependents:** 0
- **Priority Score:** 407.6
- **Functions:** 3/3 matched (target 29)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Lint issues:** 1

### 27. serde_core.format

- **Target:** `core.Format`
- **Similarity:** 0.62
- **Dependents:** 0
- **Priority Score:** 403.8
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 28. tests.test_deprecated

- **Target:** `private.TestDeprecatedTest`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 400.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 4/4 matched
- **Missing types:** _none_

### 29. internals.symbol

- **Target:** `internals.Symbol`
- **Similarity:** 0.48
- **Dependents:** 0
- **Priority Score:** 305.2
- **Functions:** 2/2 matched (target 4)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 30. serde_derive.deprecated

- **Target:** `serdederive.Deprecated`
- **Similarity:** 0.81
- **Dependents:** 0
- **Priority Score:** 301.9
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 31. internals.mod

- **Target:** `internals.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 210.0
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 32. serde_core.std_error

- **Target:** `core.StdError`
- **Similarity:** 0.27
- **Dependents:** 0
- **Priority Score:** 207.3
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 33. serde_derive.this

- **Target:** `serdederive.This`
- **Similarity:** 0.86
- **Dependents:** 0
- **Priority Score:** 201.4
- **Functions:** 2/2 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 34. private.string

- **Target:** `private.String`
- **Similarity:** 0.39
- **Dependents:** 0
- **Priority Score:** 106.1
- **Functions:** 1/1 matched (target 2)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 35. serde_derive.dummy

- **Target:** `serdederive.Dummy`
- **Similarity:** 0.50
- **Dependents:** 0
- **Priority Score:** 105.0
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 36. serde_core.private.mod

- **Target:** `commonMain.kotlin.io.github.kotlinmania.serde.core.private.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 37. serde_core.crate_root

- **Target:** `core.CrateRoot [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 4)
- **Missing types:** _none_

### 38. serde_core.macros

- **Target:** `core.Macros [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 30)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Lint issues:** 9

### 39. private.mod

- **Target:** `private.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 40. serde.integer128

- **Target:** `serde.Integer128 [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 41. serde_core.lib

- **Target:** `core.Lib [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 3)
- **Missing types:** _none_

### 42. serde.lib

- **Target:** `serde.Lib [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/serde rust ../../src/commonMain/kotlin/io/github/kotlinmania/serde kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `serde_derive.lib` | `serdederive.src.Lib` | 0 | `serde_derive/src/lib.rs` | `serdederive/src/Lib.kt` |
| `serde_derive_internals.lib` | `serdederiveinternals.Lib` | 0 | `serde_derive_internals/lib.rs` | `serdederiveinternals/Lib.kt` |

