# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 17/28 (60.7%)
- **Function parity:** 112/286 matched (target 132) — 39.2%
- **Class/type parity:** 16/44 matched (target 32) — 36.4%
- **Combined symbol parity:** 128/330 matched (target 164) — 38.8%
- **Average inline-code cosine:** 0.23 (function body across 16 matched files)
- **Average documentation cosine:** 0.00 (doc text across 16 matched files)
- **Cheat-zeroed Files:** 6
- **Critical Issues:** 14 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. de.struct_

- **Target:** `de.Struct [STUB]`
- **Similarity:** 0.00
- **Dependents:** 3
- **Priority Score:** 3020510.0
- **Functions:** 3/5 matched
- **Missing functions:** `deserialize`, `deserialize_in_place`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 2. de.enum_untagged

- **Target:** `de.EnumUntagged`
- **Similarity:** 0.50
- **Dependents:** 3
- **Priority Score:** 3010305.0
- **Functions:** 2/3 matched
- **Missing functions:** `deserialize`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 3. de.enum_

- **Target:** `de.Enum`
- **Similarity:** 0.56
- **Dependents:** 3
- **Priority Score:** 3010304.5
- **Functions:** 2/3 matched
- **Missing functions:** `deserialize`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 4. de.tuple

- **Target:** `de.Tuple`
- **Similarity:** 0.00
- **Dependents:** 2
- **Priority Score:** 2030310.0
- **Functions:** 0/3 matched
- **Missing functions:** `deserialize`, `deserialize_newtype_struct`, `deserialize_in_place`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 5. de.identifier

- **Target:** `de.Identifier`
- **Similarity:** 0.57
- **Dependents:** 2
- **Priority Score:** 2000304.4
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 6. de.enum_externally

- **Target:** `de.EnumExternally`
- **Similarity:** 0.61
- **Dependents:** 1
- **Priority Score:** 1010403.9
- **Functions:** 3/4 matched
- **Missing functions:** `deserialize`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 7. de.enum_internally

- **Target:** `de.EnumInternally [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1010210.0
- **Functions:** 1/2 matched
- **Missing functions:** `deserialize`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 8. de.enum_adjacently

- **Target:** `de.EnumAdjacently`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1010110.0
- **Functions:** 0/1 matched
- **Missing functions:** `deserialize`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 9. internals.receiver

- **Target:** `internals.Receiver`
- **Similarity:** 0.04
- **Dependents:** 0
- **Priority Score:** 181909.6
- **Functions:** 1/18 matched (target 1)
- **Missing functions:** `self_ty`, `self_to_qself`, `self_to_expr_path`, `visit_type_mut`, `visit_type_path_mut`, `visit_expr_path_mut`, `visit_type_mut_impl`, `visit_type_path_mut_impl`, `visit_expr_path_mut_impl`, `visit_path_mut`, `visit_path_arguments_mut`, `visit_return_type_mut`, `visit_type_param_bound_mut`, `visit_generics_mut`, `visit_data_mut`, `visit_expr_mut`, `visit_macro_mut`
- **Types:** 0/1 matched (target 0)
- **Missing types:** `ReplaceReceiver`

### 10. de

- **Target:** `de.De [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 44110.0
- **Functions:** 30/32 matched (target 33)
- **Missing functions:** `new`, `in_place`
- **Types:** 7/9 matched (target 15)
- **Missing types:** `InPlaceImplGenerics`, `InPlaceTypeGenerics`
- **Lint issues:** 1

### 11. ser

- **Target:** `ser.Ser [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 34110.0
- **Functions:** 34/36 matched
- **Missing functions:** `new`, `effective_style`
- **Types:** 4/5 matched (target 8)
- **Missing types:** `Parameters`

### 12. internals.symbol

- **Target:** `internals.Symbol`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20310.0
- **Functions:** 0/2 matched (target 4)
- **Missing functions:** `eq`, `fmt`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 13. internals.case

- **Target:** `internals.Case [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10910.0
- **Functions:** 6/7 matched (target 6)
- **Missing functions:** `fmt`
- **Types:** 2/2 matched (target 5)
- **Missing types:** _none_
- **Tests:** 2/2 matched

### 14. de.unit

- **Target:** `de.Unit`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10110.0
- **Functions:** 0/1 matched
- **Missing functions:** `deserialize`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 15. bound

- **Target:** `internals.Bound`
- **Similarity:** 0.73
- **Dependents:** 0
- **Priority Score:** 1802.7
- **Functions:** 17/17 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Lint issues:** 1

### 16. pretend

- **Target:** `internals.Pretend [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 610.0
- **Functions:** 6/6 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 17. lib

- **Target:** `serderive.Lib`
- **Similarity:** 0.66
- **Dependents:** 0
- **Priority Score:** 503.4
- **Functions:** 4/4 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `internals.mod` | `internals.Mod` | 0 | `internals/mod.rs` | `internals/Mod.kt` |

