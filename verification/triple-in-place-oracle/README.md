# Three-field tuple in-place decoding

The actual upstream `tuple_impl_body!` in `tmp/serde/serde_core/src/de/impls.rs:1433-1465`, instantiated for three fields at line 1483, decodes each field through an in-place seed in order. This fixture invokes that real tuple implementation, not a substitute visitor. The custom Item codec mutates to 700 before a typed failure and to 800 before a panic, making the retained state and exact visited-field order observable.

Thirteen Rust/Kotlin groups cover mutable element reuse; empty input; absent second and third fields; typed error and panic at each of the three positions; successful primitive replacement; missing-third primitive partial replacement; and three successfully decoded nullable fields. Kotlin additionally checks each mutable element's identity. The primitive partial case asserts the original copied snapshot and new caller-slot state separately. Nullable success remains distinct from missing elements.

The Kotlin baseline compiled and ran 13 tests: ten failed, three controls passed. Original red XML and exact test source are retained under `build/rust-kotlin-triple-in-place/`. Before the production repair, an assertion's existing error extraction was named and the same `kotlin.test.assertIs` imported explicitly; its before/after source is recorded. No expectations, fixture behavior or existing tests were relaxed.

Hash-verified existing full `de/impls.rs` translation output already has the triple in-place visitor at `ImplsReview.kt:4274-4298`. It passes field values to seeds, requiring manual adaptation to existing canonical `MutablePlace`, `InPlaceSeed`, `SerdeResult` and `SeqElement`. This is a manual consumer repair informed by generated output and exact source, not a generator fix or a fresh CLI success. No known failing translator input was retried or guard bypassed.

The production insertion specializes only the existing MutablePlace overload of `tripleDeserialize`. Existing ordinary decoding, the setter overload, all pair code, other tuple arities, dependencies, allocation policy and public signatures remain unchanged. No duplicate support types are introduced.

Kotlin's outer Triple, like Pair, is immutable: a field replacement reconstructs the outer value through the caller slot. Mutable element identities are retained, but Rust tuple address, outer identity, exclusive borrowing enforcement, Drop/allocator observations and Send/Sync are not represented. Callers must keep the slot exclusively accessible during the visit; reentrant getter/setter callbacks are not verified. Other platforms and downstream source/binary compatibility have not been tested. Existing static failures remain explicitly reported in the receipt.

Graph impact: Serde has 56 direct reverse-dependent repositories. Tokio has 33 but has concurrent dirty work, which was inspected read-only and left untouched. This step completes the missing three-field in-place path among the existing pair/triple factory APIs without expanding tuple architecture.

Commands:

- `cargo test --offline --manifest-path verification/triple-in-place-oracle/Cargo.toml --target-dir build/rust-kotlin-triple-in-place/rust-target`
- `./gradlew jvmTest --tests '*TripleInPlaceParityTest*' --console=plain` (before repair)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)

The worktree was clean at baseline `9f2bd35`. Earlier checkpoint work, including `1030be3`, `26fb96e` and `7c6c87a`, is separate from this repair. No remote, publication or configuration actions were taken.
