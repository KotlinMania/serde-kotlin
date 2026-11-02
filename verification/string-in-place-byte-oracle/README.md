# String deserialize-in-place UTF-8 error boundary

## Selection and provenance

This repository started clean at edbf297. Serde has 56 inbound caller repos in
the inspected dependency graph, ahead of Tokio 33 and Futures/Rand 30. The
selected bounded material error-classification gap uses existing String,
SerdeResult/Error and MutablePlace APIs; no public signature or broad exception
framework change is needed. Adjacent owners and prior dependency/content,
sequence/map and Futures checkpoints remain separate and untouched. Earlier
corrected Iterator receipt and byte-identical EOF-attribution audit are retained.

The existing native rust-kotlin CLI successfully translated unchanged original
`tmp/serde/serde_core/src/de/impls.rs` with this repo's gradle.properties and
original serde_core source root into build/rust-kotlin-string-in-place/ImplsReview.kt.
No fake extracted/renamed input was used. The scratch StringInPlaceVisitor uses
fromUtf8 result matching before assignment. Unresolved associated types/imports
and ownership lowering mean that output is not claimed independently runnable.
This is manual exact-source adaptation informed by that output and direct
original Rust, not a generator fix or substitute decoding algorithm.

## Exact source boundary and Kotlin adaptation

Rust StringInPlaceVisitor::visit_bytes matches str::from_utf8 and maps only its
Err to invalid_value(Unexpected::Bytes, expected string). The successful branch
then clears/writes the destination. visit_byte_buf similarly matches
String::from_utf8 before assigning. An arbitrary panic during the successful
write is not a UTF-8 decoding error. Kotlin's string/setter visitor already
propagates setter exceptions for visitStr/visitString, as does the documented
Deserialize MutablePlace/default setter adaptation.

The byte visitor instead put both strict decoding and the setter inside
serdeCatching followed by recoverCatching, so a valid input's setter exception
was swallowed and relabeled invalid UTF-8. The repair confines the mapping to
CharacterCodingException from strict decodeToString, then calls the unchanged
setter outside the decoding error handler. Generic SerdeError/Exception helpers,
other visitors, public signatures and existing callers/assertions are unchanged.
No suppression, warning-policy change or broad catch rewrite is introduced.

## Honest split of validation evidence

Four shared Kotlin/Rust cases cover valid borrowed UTF-8 with supplementary
scalar and NUL, empty owned input replacing the old string, invalid overlong
borrowed bytes and invalid truncated owned bytes. Actual Rust String::
deserialize_in_place executes both its byte visitor channels; these four
upstream controls pass and confirm final string state and exact invalid-byte
error text. The custom input deserializer only drives the visitor channel;
it does not reimplement UTF-8 or in-place deserialization.

Four additional Kotlin-specific regression cases cover exceptions from the
existing caller setter/MutablePlace API on valid input: borrowed/owned arbitrary
exceptions and borrowed/owned typed SerdeException identities. They assert one
write attempt, partial caller mutation where applicable and exact exception/
diagnostic identity. Rust String::deserialize_in_place has no arbitrary setter
callback API; these cases are not presented as four Rust oracle tests or as
resource-panic equivalence. They verify Kotlin's established setter contract
and the source's separation of decoding errors from successful writes.

The old Kotlin implementation executes eight tests and fails those four setter
cases at runtime; the four shared UTF-8/state controls pass. Red logs/XML-derived
failures are retained under build/rust-kotlin-string-in-place. Existing tests
and assertions are not weakened.

```sh
CARGO_TARGET_DIR=build/rust-kotlin-string-in-place/cargo-target cargo test --manifest-path verification/string-in-place-byte-oracle/Cargo.toml --offline --locked
```

## Remaining limits

Kotlin immutable String and setter/MutablePlace adaptation do not reproduce
Rust string capacity reuse, owned buffer transfer, borrow/move/Drop/pin rules
or allocation-failure state. Resource exhaustion/panic behavior and global
serdeCatching policies are not fully validated or changed. Four Rust cases
cover only the listed UTF-8/state inputs, not all Unicode or platform failures.
Prior nullable-key, iterator-hint/Int-usize, ownership, failed-sink, fairness,
thread-scheduling and downstream/platform limitations remain unchanged. No
other serializer/deserializer or canonical owner changed. Dependency pins,
repositories and runtime/build/publication settings are untouched. No downstream
source/binary, Swift/other-target or fresh publication validation is claimed;
no UI session or foreign dirty work is included.

## Final-source local checks

All 432 JVM and 200 macOS ARM tests pass. The eight new Kotlin cases pass on
both platforms, including all four setter regressions; the preserved five-case
sequence and seven-case map suites also pass again on both targets. All four
actual upstream Rust UTF-8/state oracle controls pass. The distinction between
those four Rust cases and the Kotlin-only setter tests is retained explicitly;
no Rust callback/panic test is claimed.

Combined Gradle validation remains unsuccessful at global static gates:
50 Detekt findings and current ktlint Check groups common-main 2591,
common-test 242, JVM-test 3726, macOS ARM-test 6 and Kotlin-script 5 (6570 total
reported findings); other checked groups have zero. All locations map to
unchanged baseline edbf297 lines, with no authored Detekt/ktlint finding.
Unchanged EOF attribution and historical Format-report exclusion remain in the
checker. No rule, warning setting or existing assertion changed.

Final hashes/validation and CLI/runtime-red/oracle/Gradle logs remain under
build/rust-kotlin-string-in-place. Hashes include preserved Value.kt, both
iterator fixtures and the committed prior receipt/attribution audit. Public
signatures, existing callers, production pins and earlier Futures 09c4126
remain unchanged. No prior dirty work is included; all source/state, resource,
ownership, width and downstream/platform caveats above remain explicit.
