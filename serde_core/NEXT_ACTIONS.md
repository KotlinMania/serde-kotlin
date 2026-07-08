# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 17/20 (85.0%)
- **Function parity:** 99/161 matched (target 1005) — 61.5%
- **Class/type parity:** 56/104 matched (target 178) — 53.8%
- **Combined symbol parity:** 155/265 matched (target 1183) — 58.5%
- **Average inline-code cosine:** 0.29 (function body across 13 matched files)
- **Average documentation cosine:** 0.25 (doc text across 13 matched files)
- **Cheat-zeroed Files:** 7
- **Critical Issues:** 15 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. private.size_hint

- **Target:** `priv.SizeHint [PROVENANCE-FALLBACK]`
- **Similarity:** 0.88
- **Dependents:** 2
- **Priority Score:** 2000301.2
- **Functions:** 3/3 matched (target 9)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/private/size_hint.rs` vs expected `private/size_hint.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/private/size_hint.rs` vs expected `private/size_hint.rs`
- **Proposed provenance header:** `// port-lint: source private/size_hint.rs` (current: `// port-lint: source serde_core/src/private/size_hint.rs`)
- **Proposed provenance header:** `// port-lint: source private/size_hint.rs` (current: `// port-lint: source serde_core/src/private/size_hint.rs`)
- **Lint issues:** 2

### 2. de.ignored_any

- **Target:** `de.IgnoredAny [PROVENANCE-FALLBACK]`
- **Similarity:** 0.37
- **Dependents:** 1
- **Priority Score:** 1071906.4
- **Functions:** 11/17 matched (target 11)
- **Missing functions:** `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Value`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/ignored_any.rs` vs expected `de/ignored_any.rs`
- **Proposed provenance header:** `// port-lint: source de/ignored_any.rs` (current: `// port-lint: source serde_core/src/de/ignored_any.rs`)
- **Lint issues:** 1

### 3. ser.impossible

- **Target:** `ser.Impossible [PROVENANCE-FALLBACK]`
- **Similarity:** 0.05
- **Dependents:** 1
- **Priority Score:** 1060909.5
- **Functions:** 1/5 matched (target 3)
- **Missing functions:** `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Ok`, `Error`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/impossible.rs` vs expected `ser/impossible.rs`
- **Proposed provenance header:** `// port-lint: source ser/impossible.rs` (current: `// port-lint: source serde_core/src/ser/impossible.rs`)
- **Lint issues:** 1

### 4. private.content

- **Target:** `priv.Content [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1000110.0
- **Functions:** 0/0 matched (target 4)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 24)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/private/content.rs` vs expected `private/content.rs`
- **Proposed provenance header:** `// port-lint: source private/content.rs` (current: `// port-lint: source serde_core/src/private/content.rs`)
- **Lint issues:** 1

### 5. de.impls

- **Target:** `de.Impls [PROVENANCE-FALLBACK]`
- **Similarity:** 0.08
- **Dependents:** 0
- **Priority Score:** 314909.2
- **Functions:** 12/22 matched (target 153)
- **Missing functions:** `deserialize`, `deserialize_in_place`, `visit_seq`, `visit_some`, `__private_visit_untagged_option`, `nop_reserve`, `new`, `visit_enum`, `check_overflow`, `visit_map`
- **Types:** 6/27 matched (target 36)
- **Missing types:** `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/impls.rs` vs expected `de/impls.rs`
- **Proposed provenance header:** `// port-lint: source de/impls.rs` (current: `// port-lint: source serde_core/src/de/impls.rs`)
- **Lint issues:** 1

### 6. de.mod

- **Target:** `de.OneOf [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 236510.0
- **Functions:** 29/48 matched (target 35)
- **Missing functions:** `fmt`, `deserialize_in_place`, `deserialize`, `__deserialize_content_v1`, `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__private_visit_untagged_option`, `next_element_seed`, `next_key`, `next_value`, `next_entry`, `next_key_seed`, `next_value_seed`, `variant`, `write_str`, `write_char`
- **Types:** 13/17 matched (target 32)
- **Missing types:** `Expected`, `Value`, `Error`, `LookForDecimalPoint`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/mod.rs` vs expected `de/mod.rs`
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Proposed provenance header:** `// port-lint: source de/mod.rs` (current: `// port-lint: source serde_core/src/de/mod.rs`)
- **Lint issues:** 15

### 7. de.value

- **Target:** `value.Value [PROVENANCE-FALLBACK]`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 175704.4
- **Functions:** 18/27 matched (target 613)
- **Missing functions:** `custom`, `fmt`, `description`, `clone`, `next_pair`, `next_entry_seed`, `unit_only`, `map_as_enum`, `split`
- **Types:** 22/30 matched (target 43)
- **Missing types:** `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/value.rs` vs expected `de/value.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/de/value.rs` vs expected `de/value.rs`
- **Proposed provenance header:** `// port-lint: source de/value.rs` (current: `// port-lint: source serde_core/src/de/value.rs`)
- **Proposed provenance header:** `// port-lint: source de/value.rs` (current: `// port-lint: source serde_core/src/de/value.rs`)
- **Lint issues:** 2

### 8. ser.fmt

- **Target:** `ser.Fmt [PROVENANCE-FALLBACK]`
- **Similarity:** 0.40
- **Dependents:** 0
- **Priority Score:** 132506.0
- **Functions:** 12/16 matched (target 41)
- **Missing functions:** `custom`, `serialize_newtype_struct`, `serialize_some`, `serialize_newtype_variant`
- **Types:** 0/9 matched (target 3)
- **Missing types:** `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/fmt.rs` vs expected `ser/fmt.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/fmt.rs` vs expected `ser/fmt.rs`
- **Proposed provenance header:** `// port-lint: source ser/fmt.rs` (current: `// port-lint: source serde_core/src/ser/fmt.rs`)
- **Proposed provenance header:** `// port-lint: source ser/fmt.rs` (current: `// port-lint: source serde_core/src/ser/fmt.rs`)
- **Lint issues:** 2

### 9. private.doc

- **Target:** `priv.Doc [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 40410.0
- **Functions:** 0/3 matched (target 26)
- **Missing functions:** `custom`, `description`, `fmt`
- **Types:** 0/1 matched (target 2)
- **Missing types:** `Error`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/private/doc.rs` vs expected `private/doc.rs`
- **Proposed provenance header:** `// port-lint: source private/doc.rs` (current: `// port-lint: source serde_core/src/private/doc.rs`)
- **Lint issues:** 1

### 10. ser.mod

- **Target:** `ser.Serialize [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 31810.0
- **Functions:** 6/9 matched (target 7)
- **Missing functions:** `collect_seq`, `collect_map`, `serialize_entry`
- **Types:** 9/9 matched (target 10)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/mod.rs` vs expected `ser/mod.rs`
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Proposed provenance header:** `// port-lint: source ser/mod.rs` (current: `// port-lint: source serde_core/src/ser/mod.rs`)
- **Lint issues:** 11

### 11. private.seed

- **Target:** `priv.Seed [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20310.0
- **Functions:** 0/1 matched (target 0)
- **Missing functions:** `deserialize`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Value`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/private/seed.rs` vs expected `private/seed.rs`
- **Proposed provenance header:** `// port-lint: source private/seed.rs` (current: `// port-lint: source serde_core/src/private/seed.rs`)
- **Lint issues:** 1

### 12. format

- **Target:** `serdecore.Format [PROVENANCE-FALLBACK]`
- **Similarity:** 0.61
- **Dependents:** 0
- **Priority Score:** 403.9
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/format.rs` vs expected `format.rs`
- **Proposed provenance header:** `// port-lint: source format.rs` (current: `// port-lint: source serde_core/src/format.rs`)
- **Lint issues:** 1

### 13. ser.impls

- **Target:** `ser.Impls [PROVENANCE-FALLBACK]`
- **Similarity:** 0.49
- **Dependents:** 0
- **Priority Score:** 305.1
- **Functions:** 3/3 matched (target 67)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 15)
- **Missing types:** _none_
- **Tests:** 1/1 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/impls.rs` vs expected `ser/impls.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/ser/impls.rs` vs expected `ser/impls.rs`
- **Proposed provenance header:** `// port-lint: source ser/impls.rs` (current: `// port-lint: source serde_core/src/ser/impls.rs`)
- **Proposed provenance header:** `// port-lint: source ser/impls.rs` (current: `// port-lint: source serde_core/src/ser/impls.rs`)
- **Lint issues:** 2

### 14. private.string

- **Target:** `priv.String [PROVENANCE-FALLBACK]`
- **Similarity:** 0.39
- **Dependents:** 0
- **Priority Score:** 106.1
- **Functions:** 1/1 matched (target 2)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/private/string.rs` vs expected `private/string.rs`
- **Proposed provenance header:** `// port-lint: source private/string.rs` (current: `// port-lint: source serde_core/src/private/string.rs`)
- **Lint issues:** 1

### 15. lib

- **Target:** `serde.Lib [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde/src/lib.rs` vs expected `lib.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source serde/src/lib.rs`)
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source serde_core/src/lib.rs`)
- **Lint issues:** 2

### 16. macros

- **Target:** `serdecore.Macros [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 30)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/macros.rs` vs expected `macros.rs`
- **Proposed provenance header:** `// port-lint: source macros.rs` (current: `// port-lint: source serde_core/src/macros.rs`)
- **Lint issues:** 1

### 17. private.mod

- **Target:** `priv.Mod [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `serde_core/src/private/mod.rs` vs expected `private/mod.rs`
- **Proposed provenance header:** `// port-lint: source private/mod.rs` (current: `// port-lint: source serde_core/src/private/mod.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

