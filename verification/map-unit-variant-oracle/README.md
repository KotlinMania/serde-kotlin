# Validate payloads for map-backed unit enum variants

Baseline: `b94fab6`, separate from this authored repair. The clean Serde checkout
was selected from the workspace graph (56 direct dependent repositories versus
30 each for Futures/Rand and 21 for JSON). Iris-owned generator/proc-macro2/syn/
quote and concurrently owned adjacent work were not touched. JSON was checked
read-only and was clean at `f85acc0`; no claim is made about unseen agent activity.

Upstream `tmp/serde/serde_core/src/de/value.rs:1810` implements
`MapAsEnum::unit_variant` with `self.map.next_value()`. The return type infers
`Deserialize<()>`, so the payload must actually deserialize as unit. The Kotlin
implementation instead supplied an anonymous seed that unconditionally returned
success, bypassing the payload decoder. This accepted integers, booleans and
strings and suppressed decoder errors/panics.

The repair uses the existing `MapAccess.nextValue(UnitDeserialize)` adapter and
canonical unit codec. No new type or alternate algorithm was introduced, and no
public signature changed. The successful full-file rust-kotlin output for the
same exact upstream source was hash-verified and reused; provenance is in the
receipt. This is a manual consumer repair based on generated/source comparison,
not a generator fix or newly generated source adoption.

Seven actual Rust tests cover a valid unit, invalid integer/boolean/string payloads,
unit-method dispatch exactly once, typed decoder error and decoder panic. The
matching Kotlin suite initially failed six cases; only the unit control passed.
Kotlin additionally checks typed error object identity, a binding-specific
observation rather than a claim about Rust error-object identity. No existing test
or assertion was edited. The regression file hash is retained from before repair.

Run from the owning repository:

```
cargo test --offline --manifest-path verification/map-unit-variant-oracle/Cargo.toml --target-dir build/rust-kotlin-map-unit-variant/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt includes final-source runtime totals, static attribution and local log
hashes. Existing static failures are not reported as green gates. Cargo.lock is
locally generated under existing ignore policy, with its hash recorded. No pins,
configuration, remote CI, push, PR, release or publication changed.

Remaining limits include the separate MapAccessDeserializer variant-seed catch-all,
nullable key versus absence ambiguity, Kotlin references versus consuming Rust
ownership/Drop, generic/error specializations, Int/usize widths, untested platforms
and downstream integration. This only establishes the listed enum-unit cases.
Historical failed receipts from `adfcae0` remain separate and untouched; the
Rust-backed test-only resolution `b94fab6` is preserved.
