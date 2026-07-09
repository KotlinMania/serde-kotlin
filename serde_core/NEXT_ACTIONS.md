# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 19/19 (100.0%)
- **Function parity:** 107/159 matched (target 1099) — 67.3%
- **Class/type parity:** 58/104 matched (target 199) — 55.8%
- **Combined symbol parity:** 165/263 matched (target 1298) — 62.7%
- **Average inline-code cosine:** 0.32 (function body across 16 matched files)
- **Average documentation cosine:** 0.29 (doc text across 16 matched files)
- **Cheat-zeroed Files:** 7
- **Critical Issues:** 16 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. private.size_hint

- **Target:** `priv.SizeHint`
- **Similarity:** 0.88
- **Dependents:** 2
- **Priority Score:** 2000301.2
- **Functions:** 3/3 matched (target 9)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 3)
- **Missing types:** _none_

### 2. de.ignored_any

- **Target:** `de.IgnoredAny`
- **Similarity:** 0.37
- **Dependents:** 1
- **Priority Score:** 1061906.4
- **Functions:** 11/17 matched (target 57)
- **Missing functions:** `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `deserialize`
- **Types:** 2/2 matched (target 10)
- **Missing types:** _none_

### 3. ser.impossible

- **Target:** `ser.Impossible`
- **Similarity:** 0.05
- **Dependents:** 1
- **Priority Score:** 1060909.5
- **Functions:** 1/5 matched (target 3)
- **Missing functions:** `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Ok`, `Error`

### 4. private.content

- **Target:** `priv.Content [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1000110.0
- **Functions:** 0/0 matched (target 12)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 24)
- **Missing types:** _none_

### 5. de.impls

- **Target:** `de.Impls`
- **Similarity:** 0.08
- **Dependents:** 0
- **Priority Score:** 314909.2
- **Functions:** 12/22 matched (target 156)
- **Missing functions:** `deserialize`, `deserialize_in_place`, `visit_seq`, `visit_some`, `__private_visit_untagged_option`, `nop_reserve`, `new`, `visit_enum`, `check_overflow`, `visit_map`
- **Types:** 6/27 matched (target 36)
- **Missing types:** `Value`, `StringInPlaceVisitor`, `CStringVisitor`, `OptionVisitor`, `PhantomDataVisitor`, `VecVisitor`, `VecInPlaceVisitor`, `ArrayVisitor`, `ArrayInPlaceVisitor`, `PathVisitor`, `PathBufVisitor`, `OsStringVisitor`, `Field`, `FieldVisitor`, `DurationVisitor`, `RangeVisitor`, `RangeFromVisitor`, `RangeToVisitor`, `BoundVisitor`, `ResultVisitor`, `FromStrVisitor`

### 6. de.mod

- **Target:** `de.OneOf [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 206510.0
- **Functions:** 32/48 matched (target 45)
- **Missing functions:** `fmt`, `deserialize_in_place`, `deserialize`, `__deserialize_content_v1`, `visit_some`, `visit_newtype_struct`, `visit_seq`, `visit_map`, `visit_enum`, `__private_visit_untagged_option`, `next_element_seed`, `next_key_seed`, `next_value_seed`, `variant`, `write_str`, `write_char`
- **Types:** 13/17 matched (target 32)
- **Missing types:** `Expected`, `Value`, `Error`, `LookForDecimalPoint`

### 7. de.value

- **Target:** `value.Value`
- **Similarity:** 0.57
- **Dependents:** 0
- **Priority Score:** 155704.3
- **Functions:** 20/27 matched (target 617)
- **Missing functions:** `custom`, `fmt`, `description`, `clone`, `next_pair`, `next_entry_seed`, `split`
- **Types:** 22/30 matched (target 44)
- **Missing types:** `Error`, `ErrorImpl`, `Deserializer`, `Variant`, `Value`, `Pair`, `First`, `Second`

### 8. ser.fmt

- **Target:** `ser.Fmt`
- **Similarity:** 0.39
- **Dependents:** 0
- **Priority Score:** 132506.1
- **Functions:** 12/16 matched (target 43)
- **Missing functions:** `custom`, `serialize_newtype_struct`, `serialize_some`, `serialize_newtype_variant`
- **Types:** 0/9 matched (target 3)
- **Missing types:** `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant`

### 9. ser.mod

- **Target:** `ser.Serialize [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 31810.0
- **Functions:** 6/9 matched (target 20)
- **Missing functions:** `collect_seq`, `collect_map`, `serialize_entry`
- **Types:** 9/9 matched (target 16)
- **Missing types:** _none_

### 10. private.seed

- **Target:** `priv.Seed`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20310.0
- **Functions:** 0/1 matched
- **Missing functions:** `deserialize`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Value`

### 11. std_error

- **Target:** `serdecore.StdError [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20210.0
- **Functions:** 0/1 matched (target 0)
- **Missing functions:** `source`
- **Types:** 0/1 matched
- **Missing types:** `Error`

### 12. private.doc

- **Target:** `priv.Doc`
- **Similarity:** 0.23
- **Dependents:** 0
- **Priority Score:** 407.7
- **Functions:** 3/3 matched (target 31)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 4)
- **Missing types:** _none_

### 13. format

- **Target:** `serdecore.Format`
- **Similarity:** 0.61
- **Dependents:** 0
- **Priority Score:** 403.9
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 14. ser.impls

- **Target:** `ser.Impls`
- **Similarity:** 0.49
- **Dependents:** 0
- **Priority Score:** 305.1
- **Functions:** 3/3 matched (target 67)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 15)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 15. private.string

- **Target:** `priv.String`
- **Similarity:** 0.39
- **Dependents:** 0
- **Priority Score:** 106.1
- **Functions:** 1/1 matched (target 2)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 16. macros

- **Target:** `serdecore.Macros [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 30)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 17. private.mod

- **Target:** `priv.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 18. crate_root

- **Target:** `serdecore.CrateRoot [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 3)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 2)
- **Missing types:** _none_

### 19. lib

- **Target:** `serdecore.Lib`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 3)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

