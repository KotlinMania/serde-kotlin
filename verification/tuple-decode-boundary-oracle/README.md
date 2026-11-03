# Ordinary tuple decode error/exception boundary

The actual Rust `tuple_impl_body!::deserialize` in `tmp/serde/serde_core/src/de/impls.rs:1397-1431` returns declared errors directly and does not intercept panics from its sequence access or element codecs. The fixture invokes the real two- and three-element tuple Deserialize implementations. The Driver only supplies the format protocol, the Access supplies seed inputs or injected failures, and Item supplies an instrumented codec. They do not implement replacement tuple algorithms. Unsupported Driver format methods are fixture traps and are never used in the passing cases.

Nine matching Rust/Kotlin groups contain 33 scenarios: success for each arity; all five missing-field positions across both arities; all five positions for codec errors, codec panics, access errors and access panics; backend typed errors and backend panics for each arity; and successful nullable fields for each arity. Assertions check complete access/codec traces, decoded values and exact outcomes. Nullable presence uses Rust Option and Kotlin's existing nullable codec; no duplicate Option type is introduced.

Before the repair Kotlin compiled and executed nine groups: two failed (codec panic and access panic converted to typed errors), seven controls passed. Each failed group stopped at its first assertion, so the baseline does not prove every later scenario ran. The passing final run executes all 33. The original red suite and exact test source remain in `build/rust-kotlin-tuple-decode-boundary/`. Only braces/layout were adjusted before production changes; exact replacements are recorded, with no changed assertions, expected values or fixture behavior.

The existing complete de/impls.rs translation output was reused after source/output hash verification. Its ordinary pair and triple visitors around `ImplsReview.kt:4189-4215` and `4239-4272` already use direct result/presence handling. The repair manually binds that structure to canonical SerdeResult, SeqElement and SeedFromDeserialize. It is a consumer repair, not a generator fix or a fresh translation claim. No recorded parser failure was retried or guard bypassed.

The production diff changes only the two ordinary visitSeq bodies. All committed Pair/Triple MutablePlace/InPlaceSeed paths, setter overloads, public signatures, dependencies and other codecs remain unchanged. Missing-field diagnostics and typed errors are preserved; programming exceptions now propagate. No existing tests or assertions were changed.

Kotlin's immutable outer Pair/Triple replacement behavior and borrowed-slot/Drop/allocator/Send/Sync limitations from the earlier in-place work remain unresolved and explicit; this ordinary decoding repair makes no ownership-equivalence claim. The format owns trailing-input validation; the custom sequence fixture does not establish universal extra-element rejection or EOF policy. Nullable Rust control uses SeqDeserializer whereas Kotlin uses the test Driver, so only decoded presence/value is compared there. Other platforms, downstream builds and binary compatibility are unverified. Global static gates retain known failures, separately attributed in the final receipt.

Graph impact: Serde has 56 direct reverse-dependent repositories. Worktree baseline `7fb7e2b` was clean; prior pair/triple and checkpoint work remains separate. No remote or configuration actions were performed.

Commands:

- `cargo test --offline --manifest-path verification/tuple-decode-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-tuple-decode-boundary/rust-target`
- `./gradlew jvmTest --tests '*TupleDecodeBoundaryParityTest*' --console=plain` (baseline)
- `./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain` (final source)
