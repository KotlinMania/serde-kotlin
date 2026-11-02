# MapDeserializer next-value programming boundary

The repository began clean at 3b93c15. Serde has 56 reverse-dependent projects,
versus Futures/Rand 30 and JSON 21. A bounded source comparison selected this
material direct MapAccess boundary after reviewing representation-blocked
Scan/TakeUntil and other already faithful Futures adapters. No pre-existing
dirty work or adjacent owner edits are included. Prior Serde checkpoints
26fb96e/7c6c87a and authored sequence/map/string/variant/network repairs remain
separately attributed, not counted as new coverage.

## Translator provenance

This step reuses hash-verified successful full-original-file rust-kotlin output:
tmp/serde/serde_core/src/de/value.rs still matches the recorded input hash, and
build/rust-kotlin-seq-fuse/ValueReview.kt matches its recorded output hash.
The generated nextValueSeed clears its slot, expects a value, then returns the
seed result directly. No redundant translator process, source extraction or
root-guard workaround is used. Scratch generic/associated-type/error lowering
remains incomplete and is not claimed runnable. The repair is a manual exact
source adaptation informed by generated output and actual Rust behavior.

## Source contract and repair

Original serde_core de/value.rs MapDeserializer::next_value_seed takes the
pending value before any conversion/seed call. Its explicit comment says a
missing value panics because this is a programming bug rather than expected
failure. The original message is "MapAccess::next_value called before next_key".
It then directly returns seed.deserialize(value.into_deserializer()).
A seed Err remains an ordinary typed Err; panics in conversion or seed code
propagate. In every case the pending value was already taken.

Kotlin wrapped the whole method in serdeCatching, which converts all thrown
exceptions into SerdeResult.Failure. This hid missing-key/double-value program
errors and user conversion/seed exceptions as data failures. The bounded
repair takes/clears the slot, checks it outside that catch, and returns the
seed's existing SerdeResult directly. The source's programming-error comment
and message are retained. No result owner, signature or alternate algorithm
is introduced; successful nullable decoded values remain Success(null).

This certifies the direct MapDeserializer.nextValueSeed boundary only.
Enclosing legacy deserializeAny/visitor helpers and other serdeCatching call
sites can still convert exceptions; no whole-library panic-preservation claim
is made. Key/entry/sequence access and iterator-fuse logic remain unchanged.

## Independent regressions and history

Eight Rust cases execute actual unchanged serde_core MapDeserializer and
MapAccess. Test seeds implement DeserializeSeed, and one value implements a
panicking IntoDeserializer; fixtures never implement MapDeserializer. Cases
cover value before key, double value, seed panic consuming the slot, conversion
panic consuming the slot, typed seed Err, successful nullable decoded value,
next_entry not populating the pending-value slot, and a later entry after a
typed seed error. Exact panic/error text and seed/conversion call counts are
checked.

The first Rust run passed all eight cases with an empty iterator in the
before-key case. That input was then aligned to Kotlin's one-entry iterator;
all original assertions remain unchanged, and all eight cases pass again.
The original fixture/log are preserved under build with hashes. This changes
no expected result or production algorithm; it strengthens input correspondence.

Eight Kotlin cases run on original source with five failures and three passing
typed-error/nullable/continuation controls. The whole new suite is byte-identical
between red and final. The typed-error object-identity assertion is a Kotlin
binding control; Rust asserts its diagnostic text rather than object identity.
No existing test or assertion is changed. Panic/state-reuse probes observe this
implementation, not generic panic safety, borrow/move or recovery guarantees.

## Prior selectNextSome API impact audit

The Futures ceba470 receipt was rechecked read-only: all 24 recorded hashes
match. Its exact-API workspace Kotlin/Java/Swift search includes aliases,
wildcard imports, bare constructor/method names and JVM facade references.
It identifies five existing calls in StreamSelectNextSomeTest, all using
FuturesUnordered<Int>, which implements FusedStream. No external affected
source owner was found within that scope. The caller file is unchanged and
all assertions remain intact. The public constructor/extension JVM parameter
changed from Stream to FusedStream; consumers must rebuild and may need declared
type adaptation. This is source-level impact evidence, not downstream binary
compatibility. Futures is untouched by this Serde repair.

## Remaining limits

Public signatures do not change. Existing sequence/map-fuse, string and variant/
network suites and all earlier original/corrected receipts remain preserved.
Nullable decoded-key/entry presence, iterator size hints, Kotlin reference
ownership/GC versus Rust move/borrow/Drop, error-type generality and surrounding
exception boundaries remain outside this repair. Prior Futures extra
FusedStream/shared takeValue/Into/From/clone/platform gaps remain unchanged.
No downstream build, Swift/other-platform, fresh publication or complete-library
parity is claimed.

Raw red/oracle/Gradle logs, translator-reuse and static-attribution evidence
remain in build/rust-kotlin-map-value-boundary; receipt.json records exact
commands, final hashes, timestamps and initial/final oracle evidence. Test-only
Cargo uses vendored serde_core offline; Cargo.lock remains ignored by existing
policy with its hash recorded. No dependency pins, repositories/configuration,
UI, adjacent owners, remote CI, pushes, PRs, tags, releases or publication changed.

## Final-source local checks

All eight actual Rust cases pass on the final aligned fixture. All 455 JVM
and 223 macOS ARM tests pass, including the eight new cases and all preserved
prior regression suites. The combined Gradle command exits 1 for existing
static failures: 50 Detekt and 6,570 ktlint findings. Every reported location
maps to unchanged 3b93c15 baseline lines; no authored line has a finding.
Static check reports exclude stale Format reports and account for unchanged
EOF diagnostics as established by earlier audits. Final receipt hashes are
verified after the local checkpoint.
