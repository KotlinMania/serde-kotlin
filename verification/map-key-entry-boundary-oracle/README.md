# MapDeserializer key and entry boundaries

The repository began clean at be27dd4. Serde has 56 reverse-dependent projects,
versus Futures/Rand 30 and JSON 21. This bounded direct MapAccess repair extends
the established typed-error versus programming-exception distinction to key
and entry access. No pre-existing dirty work or adjacent owner edits are
included; earlier checkpoints and their coverage remain separately attributed.

## Prior corrected-fixture receipt audit

Before any new production edit or runtime run, all twenty hashes in be27dd4's
sequence-element receipt matched, including the final corrected test source.
Both final eight-case XML hashes matched. JVM timestamp was
2026-11-02T22:09:49.671Z and macOS ARM timestamp was
2026-11-02T22:11:38.422Z, after the two iterator fixes. The original first pass
with 52 Detekt findings and the final pass with 50 remain preserved.

The corrections added NoSuchElementException after exhaustion to the scripted
iterator and changed the empty iterator's next exception to the same required
type. No assertion/data changed and no rule was suppressed. The audit is saved
under build/rust-kotlin-map-key-entry-boundary/prior-corrected-fixture-audit.json
and embedded in receipt.json. It verifies the historical final receipt before
this step; new full runs naturally refresh test XML timestamps. The corrected
source and its earlier snapshot/receipts are untouched by this repair.

## Translator and exact source behavior

The complete original serde_core de/value.rs input and previously successful
build/rust-kotlin-seq-fuse/ValueReview.kt output are reused after hash checks.
The generated key method stores the pending value before key decoding; the
entry method returns immediately on a typed key error before value decoding.
No duplicate translator job, source extraction or root-guard bypass is used.
Scratch generic/associated-type/error lowerings remain incomplete, and it is
not installed or claimed runnable. The repair is manual exact-source adaptation.

Actual next_key_seed consumes the next pair, stores its value, then deserializes
the key. That pending value remains available even if key conversion/seed code
panics or the seed returns Err. Actual next_entry_seed consumes the pair and
decodes key before value, short-circuiting typed key errors. It does not populate
or otherwise change the pending-value slot. Panics propagate; returned typed
errors remain results.

Kotlin wrapped both methods in serdeCatching, swallowing programming/user-code
exceptions. Key access now directly returns its seed result after unchanged
pending-value setup. Entry access now uses canonical SerdeResult.flatMap/map to
preserve source ordering and typed-error short circuit without catching panics.
No new owner, result/Option representation, signature or substitute algorithm
is introduced. The previous map-value and sequence-element repairs remain
byte-for-byte intact within the otherwise modified Value.kt.

## Actual regressions

Nine Rust cases execute unchanged serde_core MapDeserializer and MapAccess.
Fixtures implement only DeserializeSeed and IntoDeserializer, never the adapter.
They cover key seed/conversion panic preserving the pending value, typed key
error preserving it, EOF without invoking a panicking seed, entry key panic
skipping value seed, entry value seed panic consuming that entry, typed entry
key error skipping even a panicking value conversion, a present pair of two
decoded null values, and entry value conversion panic skipping its seed.
Later-entry controls verify consumption and conversion/seed counts.

Nine matching Kotlin cases run on original code with five failures and four
passing controls. The entire new suite is byte-identical between red and final;
no existing assertion/test changes. Rust/Kotlin script counters use their native
seed ownership models; comparisons concern invocation/consumption observations,
not seed object identity. Error object identity is an extra Kotlin binding
control; Rust asserts error text. Post-panic probes observe this implementation,
not universal exception-safety or recovery guarantees.

## Scope and limits

This certifies direct MapDeserializer key/entry access only. Surrounding
deserializeAny/visitor/end and map-as-sequence helpers can still catch
exceptions; no whole-library panic-parity claim is made. The public nullable
nextKey result still cannot distinguish a decoded null key from EOF. The entry
pair itself is non-null when present, so its nullable fields are covered without
claiming the single-key API limitation solved. Wider Int/usize/error-generic,
move/borrow/Drop/GC, Debug/accessor/comment and platform gaps remain explicit.

Public signatures do not change; no caller migration is introduced. Prior
Futures bounds/rebuild, extra FusedStream/shared-helper/Into/From/clone limits
remain preserved. No downstream build, Swift/other-platform, publication or
complete-library parity is claimed. No pins, repositories/configuration, UI,
adjacent owners, remote CI, push, PR, tag, release or publication changed.

Raw red/oracle/Gradle logs, translator reuse, prior audit and final static
attribution remain under build/rust-kotlin-map-key-entry-boundary. receipt.json
records commands, final hashes and original failures. Test-only Cargo uses
vendored serde_core offline. Cargo.lock remains ignored by existing policy
with its hash recorded.

## Final-source local checks

All nine actual Rust cases pass. All 472 JVM and 240 macOS ARM tests pass,
including nine new cases and the unchanged corrected iterator fixtures and
prior boundary/fuse/string/variant suites. The combined Gradle command exits 1
for existing static failures: 50 Detekt and 6,570 ktlint findings, all mapped
to unchanged be27dd4 baseline lines, none on authored lines. Static attribution
excludes stale Format reports and handles unchanged EOF diagnostics as before.
Both the baseline corrected-fixture receipts and refreshed current eight-case
XML receipts are recorded. Final source/log hashes are verified after the
local checkpoint.
