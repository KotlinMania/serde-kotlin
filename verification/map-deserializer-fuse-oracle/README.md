# Serde MapDeserializer permanent-exhaustion parity

## Graph priority and ownership

This repository started clean at c276793. The current graph's earlier direct
inspection found Serde 56 inbound caller repos, Tokio 33, Futures/Rand 30 each.
The selected bounded material gap affects map key, entry and sequence views
and automatic end checking. Adjacent owners are untouched; earlier dependency
26fb96e and content/flattening 7c6c87a checkpoints remain separate from authored
repairs. The prior SeqDeserializer repair c276793 is preserved, not claimed
as newly authored here. No pre-existing dirty work is included.

## Prior receipt/attribution audit

Before any new production edit, every prior final tested hash was checked:
Value.kt, the corrected SeqDeserializer Iterator fixture, original Rust and
oracle source all matched c276793's final receipt. The fixture includes its
NoSuchElementException guard. Its actual final 417 JVM / 185 macOS ARM results
and exact new-suite receipts are captured in
verification/map-deserializer-fuse-oracle/prior-seq-receipt-audit.json
(with an identical working copy under build/rust-kotlin-map-fuse). That audit also retains
the static attribution correction: Check.kt and Internals.kt are byte-identical
to the earlier baseline 7c6c87a, while their ktlint EOF diagnostics point beyond the
last physical line. The corrected checker treats those as unchanged EOF,
not authored lines, and excludes historical Format reports. No source or rule
was changed to suppress these findings.

## Translator and independent upstream evidence

This step reuses the successful full-original-file native CLI output from
build/rust-kotlin-seq-fuse/ValueReview.kt. The original
`tmp/serde/serde_core/src/de/value.rs` SHA-256 still matches the prior successful
translation input; both input and output hashes and explicit reuse are recorded
in build/rust-kotlin-map-fuse/translator-reuse.json. Its MapDeserializer has
iter: Fuse<I> and constructs it with iter.fuse(). No duplicate translator job,
renamed source or extracted fake input was used. Associated types/imports and
projection lowering remain unresolved in scratch output; it is not claimed
independently runnable. This is manual exact-source adaptation informed by the
existing translator diff and direct MapDeserializer source, not a generator fix
or an assumed copy of sequence behavior.

Upstream stores a fused iterator shared by next_pair, MapAccess, SeqAccess and
end. Once that iterator returns None, no access path can expose a later pair;
end() cannot resume it and create an erroneous excess-length diagnostic.
Fused size bounds become exact zero. Kotlin's raw iterator previously resumed.
The repair adds private exhausted state at the existing Kotlin Iterator boundary,
guards nextPair/end and reports zero after EOF. Canonical MapEntry, SeqElement,
SerdeResult and IntoDeserializer are reused; no new generic Fuse type, Option
owner or public API is introduced. Pending-value behavior is unchanged, and
source count/error-order/comment behavior is preserved.

Seven new Kotlin cases and seven actual serde_core Rust oracle cases cover
key/entry/sequence-view access after EOF, end after EOF, a visitor that reads
EOF before automatic end checking, exact zero hint, and unchanged leftover
entry counts/error text. The old Kotlin executes all seven and fails six at
runtime; the ordinary length-error control passes. Logs/XML-derived failures
are retained in build/rust-kotlin-map-fuse. The fixture's next() enforces the
Kotlin Iterator contract from its first revision. The sequence-view None case
never invokes its payload decoder, as confirmed by actual Rust SeqAccess.
No existing assertion or warning rule is weakened. Rust executes actual
MapDeserializer and std Fuse rather than a reimplementation.

```sh
CARGO_TARGET_DIR=build/rust-kotlin-map-fuse/cargo-target cargo test --manifest-path verification/map-deserializer-fuse-oracle/Cargo.toml --offline --locked
```

## Remaining limits

Non-exhausted iterator bounds still use existing Kotlin len metadata and do
not newly support arbitrary IteratorWithSizeHint. Counts/hints remain Int
rather than usize. Public nullable MapAccess key results still cannot fully
distinguish a nullable decoded key from absence; no broad canonical Option API
migration is included. Rust end(self), clone/borrow/move/Drop and error-type
relationships are not fully represented; Kotlin permits receiver reuse that
Rust prevents. Existing serdeCatching conversion is unchanged, not claimed Rust
panic equivalence. Other value-deserializer APIs and Debug remain outside scope.
No dependency pins, repositories, runtime/build/publication settings, UI session
or other owner changed. Downstream source/binary, Swift/other targets and fresh
publication metadata remain unverified. Prior Futures fairness, scheduling,
failed-sink, destruction and width limitations and original/corrected oracle
receipts remain intact.

## Final-source local checks

All 424 JVM tests and 192 macOS ARM tests pass, including all seven new map
cases. All five preserved SeqDeserializer cases also pass again on both
targets, using the unchanged corrected Iterator fixture. All seven actual
upstream Rust map cases pass. Final validation explicitly records both suites
and current source hashes, so the preserved-fixture claim is tied to this
actual final-source execution as well as the prior captured audit.

The combined Gradle command still fails at global static gates: 50 Detekt
findings in untouched code and current ktlint Check groups common-main 2591,
common-test 242, JVM-test 3726, macOS ARM-test 6 and Kotlin-script 5 (6570 total
reported findings); other checked groups have zero. All locations map to
unchanged baseline c276793 lines. There are no authored Detekt or ktlint
findings and no external Check-report finding. Unchanged EOF handling and
historical Format-report exclusion remain part of the attribution checker.
No static rule, existing assertion or warning setting was changed.

Source hashes/validation and reused-translator/runtime-red/oracle/Gradle logs
remain under build/rust-kotlin-map-fuse. The corrected prior-fixture and EOF
attribution audit is also committed beside this README as a durable artifact.
Public APIs, existing callers, pins and earlier Futures 09c4126 remain unchanged;
no previous dirty work is included or counted as new repair coverage. All
nullable-key, non-exhausted-hint, ownership, panic and downstream/platform limits
above remain explicit.
