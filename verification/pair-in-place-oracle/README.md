# Pair field-by-field in-place decoding

Rust source: `tmp/serde/serde_core/src/de/impls.rs:1433-1465`, the actual `tuple_impl_body!` implementation for `(T0, T1)`. The tests invoke its `Deserialize::deserialize_in_place`; they do not implement a substitute tuple visitor. A custom element codec intentionally mutates before returning an error or panicking, making partial updates observable.

The nine matching Rust/Kotlin groups cover mutable element reuse, empty input, missing second field, first/second typed failures, first/second panics, primitive replacement, and two successfully decoded nullable fields. The missing-field diagnostics and per-element traces are asserted exactly. Kotlin additionally asserts retained mutable element identities. Rust panic unwinding is compared to Kotlin exception propagation; runtime destruction/address equivalence is not claimed.

Before production changes the Kotlin suite compiled and ran all nine groups: six failed, three controls passed. The original red XML and exact test source remain in `build/rust-kotlin-pair-in-place/`. No existing tests or assertions changed. The final receipt records full runtime and static results, including existing static failures.

The prior complete `de/impls.rs` translation is reused after verifying source/output hashes. Generated `ImplsReview.kt:4217-4237` contains the pair in-place visitor, but passes field values to a seed rather than a writable borrowed location. This repair manually adapts that path to the existing canonical `MutablePlace`, `InPlaceSeed`, `SerdeResult`, and `SeqElement` types. It is a consumer repair, not a generator fix or a fresh CLI success; no recorded parser failure was retried or bypassed.

Only the existing mutable-place pair overload is specialized. Ordinary pair decoding, the setter overload, triples, other tuple arities, capacity policy, dependencies and public signatures are unchanged. No compatibility adapter or duplicate type owner is introduced.

Kotlin `Pair` is immutable. Replacing a field reconstructs the outer Pair through the caller slot; it cannot preserve the Rust tuple address or outer object identity. Mutating a mutable element preserves that element's identity. The caller must keep this slot exclusively accessible throughout decoding; reentrant getter/setter callbacks, Rust borrowing/Drop/allocator observations, Send/Sync and platform-wide behavior are not verified. Downstream builds and binary compatibility are untested.

Commands:

- `cargo test --offline --manifest-path verification/pair-in-place-oracle/Cargo.toml --target-dir build/rust-kotlin-pair-in-place/rust-target`
- `./gradlew jvmTest --tests '*PairInPlaceParityTest*' --console=plain` (before repair)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)

Prior work remains separately checkpointed at `1030be3` and earlier commits, including `26fb96e` and `7c6c87a`. The worktree was clean at the start of this step. No remote, publication or configuration actions were taken.
