# Fresh list decoding exception and consumption boundaries

Baseline: `89aa3ca`; the owning Serde checkout was clean. The dependency graph
records 56 direct Serde consumers versus 30 each for Futures/Rand and 21 for JSON.
This bounded repair changes no other checkout or agent-owned source. Earlier map
entry, enum key, unit-payload and in-place repairs remain separate checkpoints.

Upstream `tmp/serde/serde_core/src/de/impls.rs:1157` implements VecVisitor by
querying the size hint, creating the vector, repeatedly deserializing the next
item, and stopping on typed error or end. It adds no catch around hint, sequence
access or custom element decoding. Kotlin's mutableListDeserialize visitor used
serdeCatching, turning programming exceptions into typed deserialization failures.

The repair removes that one catch and composes the existing canonical SerdeResult
and SeqElement cases explicitly. It preserves hint-before-access ordering,
key-independent element order, typed Failure identity, the seed adapter and EOF
success. No public signature, dependency, configuration, existing assertion or
in-place list implementation changed. The original capacity expression remains
unchanged: this does not implement Rust's size_hint::cautious<T> byte policy,
because generic Rust element layout is unavailable. Allocation/capacity limits
are not silently treated as parity.

The previously successful full-file rust-kotlin translation of the exact same
impls.rs was hash-verified and reused. Its VecVisitor lines 3713-3732 show a direct
result loop without a catch. That output is comparison evidence, not a drop-in
implementation: the Kotlin repair uses canonical tagged SeqElement instead of
adopting generated nullable-result scaffolding. Exact unchanged Rust is decisive.
The recorded de/mod.rs parse failure was not retried. This is a manual consumer
repair, not a generator fix, new translation run or generated-source adoption.

Eight actual Rust tests invoke Vec<Item>::deserialize with a custom codec and
sequence driver. They cover normal/empty input; hint/access panics; first/second
codec panics; and first/second typed codec errors. Exact hint/access counts and
codec traces prove order and early stopping without consuming the suffix. The
matching Kotlin suite initially had four panic failures and four passing controls.
Kotlin additionally checks typed-error object identity, a binding-specific
observation; Rust checks error content. The regression source hash is retained
from before production changes, and no assertion was weakened afterward.

Reproduce locally:

```
cargo test --offline --manifest-path verification/list-decode-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-list-decode-boundary/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt includes final-source runtime totals, static attribution and local
log/XML hashes. Existing global static failures are not called green. Cargo.lock
remains ignored by existing policy and its local hash is recorded. No remote CI,
push, PR, merge, release, publication or configuration action occurred. Historical
failed receipts and earlier test-only resolutions remain untouched.

Remaining limits include Rust's cautious generic allocation, allocator/OOM and
Drop behavior, Kotlin references versus consuming Rust ownership, other Serde
exception boundaries, nullable-map-key ambiguity, generic errors/traits, Int/usize
widths, untested platforms and downstream integration. The custom i32-based codec
fixtures do not establish every element type or nullable element representation.
