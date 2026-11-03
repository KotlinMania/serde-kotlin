# IgnoredAny streaming exception boundary

Actual Rust source: `tmp/serde/serde_core/src/de/ignored_any.rs:191-234`. The sequence and map visitors perform one access per loop iteration and return declared failures directly; the enum visitor selects the variant before deserializing its newtype payload. None catches programming panics.

The fixture calls the actual Rust IgnoredAny visitor methods. Custom sequence/map/enum accessors only supply instrumented format access; scalar deserializers log actual `deserialize_ignored_any` calls. The map fixture implements key/value access and inherits the real default next_entry algorithm. Variant fixture methods outside the expected newtype route are explicit traps. No IgnoredAny algorithm is duplicated in the oracle.

Nine matching groups contain 17 scenarios: full sequence/map drains; sequence typed errors and panics before first/second/EOF access; map typed errors and panics at second key or value; enum success plus typed errors and panics at selection or payload. Complete traces assert discarded scalar values, single sequence reads, key-before-value and variant-before-payload, and no later reads after failure. Rust uses its actual panic/error mechanisms and Kotlin its exception/typed-result bindings.

Baseline Kotlin compiled and ran nine groups: three panic groups failed by returning typed errors, six control groups passed. Failed groups stop at their first assertion; later positions are proven by the complete final passing run, not claimed as individually reproduced red. Original red XML/source and pre-repair source hash are retained. Existing tests and all new test assertions remain unchanged.

A fresh bounded CLI run on the original ignored_any.rs succeeded (five translated items, exact hashes in translator-attempt.json). Success means output was emitted, not that it compiles. Generated `IgnoredAnyReview.kt:233-236` evaluates `tri(seq, nextElement)` in both the loop condition and pattern-binding body. Rust's single while-let scrutinee evaluation does not do that. This fragment was not copied. The production repair follows exact Rust using canonical SerdeResult/SeqElement, one access per iteration, and direct enum result chaining. The generator defect is recorded separately in GENERATOR_ISSUE.md; no generator files were edited or competing prompts sent.

Only IgnoredAny sequence/map/enum runtime bodies changed. The now-unused serdeCatching file import was moved into its existing documentation example, preserving that sample's helper reference. Public signatures, tuple ordinary/in-place paths, other codecs, dependencies and allocation policies remain unchanged.

Limits: Rust borrowing/Drop/Send/Sync and other platforms/downstream behavior are unverified. Shared MapAccess's generic nullable-key ambiguity remains; this concrete IgnoredAny key codec returns a non-null singleton. This fixture does not establish every format's unknown-field policy or arbitrary nested value support. Generator output was inspected, not compiled as a standalone library. Existing Pair/Triple immutable outer identity limitations are untouched. Global static failures remain reported honestly in the receipt.

The baseline edc20ba worktree was clean. Previous tuple and checkpoint commits remain separate. Serde's 56 direct reverse dependents make unknown-data draining a foundational path. No remote or configuration actions were performed.

Commands:

- `cargo test --offline --manifest-path verification/ignored-any-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-ignored-any-boundary/rust-target`
- `./gradlew jvmTest --tests '*IgnoredAnyBoundaryParityTest*' --console=plain` (baseline)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
