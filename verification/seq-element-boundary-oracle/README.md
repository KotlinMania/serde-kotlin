# SeqDeserializer direct element boundary

The repository began clean at c2adb67. Serde has 56 reverse-dependent projects,
versus Futures/Rand 30 and JSON 21. This bounded repair extends the confirmed
typed-error versus programming-exception distinction to direct sequence access.
No pre-existing dirty work or adjacent owner edits are included. Prior Serde
checkpoints, iterator-fuse repairs, MapDeserializer value boundary, original/
corrected receipts and all earlier limits remain separately preserved.

## Translator and source provenance

The complete original serde_core de/value.rs input and prior successful
build/rust-kotlin-seq-fuse/ValueReview.kt output are reused after SHA-256
verification. Generated SeqDeserializer increments count and returns
seed.deserialize(value.intoDeserializer()).map(Some) directly. No new
translator process, source extraction or guard workaround is needed. The
generic/associated-type/error lowering remains incomplete; scratch is not
installed or claimed runnable. This is a manual exact-source boundary repair.

## Contract and repair

Original SeqDeserializer stores a fused iterator. Its next_element_seed
obtains the next item, increments count, invokes IntoDeserializer and the
seed, then maps a successful decoded value to Some. Iterator/conversion/seed
panics propagate; a returned seed Err remains a typed Err. Count is incremented
before conversion/seed, including their failure. No item after None may resume.

Kotlin's entire method was wrapped in serdeCatching, turning unexpected iterator,
conversion and seed exceptions into typed failures. It now preserves the
existing exhaustion guard, returns Success(SeqElement.None) on EOF, otherwise
consumes/increments as before and returns the canonical SerdeResult mapped to
SeqElement.Some. That existing tag preserves a decoded null independently from
EOF. No substitute Option/result owner, signature, iterator algorithm or
compatibility facade is introduced.

Only direct SeqDeserializer.nextElementSeed is certified. Legacy surrounding
deserializeAny/visitor/end helpers and other access methods can still catch
exceptions; no whole-library panic-boundary claim is made. The c2adb67 map-value
method is byte-for-byte preserved within the otherwise modified Value.kt.

## Actual regressions

Eight Rust cases execute actual unchanged serde_core SeqDeserializer and
SeqAccess. Fixtures implement only iterator, IntoDeserializer and DeserializeSeed
contracts, not the adapter. Cases cover seed panic after consumption,
conversion panic before the seed, iterator panic without permanent exhaustion,
typed seed error consuming the item, Some(None) as a successful nullable decoded
item, permanent empty exhaustion without invoking a panicking seed, and count
advancement before typed error or seed panic (verified by exact end length
diagnostics). The iterator-panic case uses an explicitly scripted producer;
it is implementation evidence, not a promise all panicked iterators recover.

Eight Kotlin cases run on original source with four failures and four passing
controls. All assertions are unchanged between red and final. The first full static
check identified two invalid test Iterator.next exhaustion implementations.
Only their NoSuchElementException guards were corrected, without changing
production code, expected outcomes or data; original fixtures/logs are retained.
No existing test or assertion changes. Kotlin hasNext/next and Rust next use
their native protocols; tests compare logical values, exception boundaries,
seed/conversion counts and permanent exhaustion, not an invented identical
physical iterator-call model. Typed-error object identity is an additional
Kotlin binding assertion; Rust asserts the error text.

## Limits and receipts

Iterator consumption and decoded null presence are preserved; wider Int/usize
size/count domains, generic error representations, Kotlin reference ownership/
GC versus Rust move/borrow/Drop and unrelated visitor exception boundaries remain
outside scope. Prior Futures FusedStream/shared-helper/Into/From/clone/platform
limits and selectNextSome consumer-rebuild caveat remain unchanged. Public
signatures do not change and no caller migration is introduced. No downstream
build, Swift/other-platform, publication or complete-library parity is claimed.

Raw red/oracle/Gradle logs, translator-reuse and final static attribution remain
in build/rust-kotlin-seq-element-boundary; receipt.json records exact commands,
timestamps, source hashes and original failures. Test-only Cargo uses vendored
serde_core offline. Cargo.lock remains ignored under existing policy, with
its hash recorded. No production pins, repositories/configuration, UI,
adjacent owners, remote CI, push, PR, tag, release or publication changed.

## Final-source local checks

All eight actual Rust cases pass. All 463 JVM and 231 macOS ARM tests pass on
the final corrected fixture source, including new cases and preserved map-value/
fuse/string/variant suites. The combined Gradle invocation exits 1 for existing
static failures: 50 Detekt and 6,570 ktlint findings, all mapped to unchanged
c2adb67 baseline lines, none on authored lines. The first full runtime pass
and 52-finding Detekt receipt are retained; final 50 confirms the two authored
iterator-contract findings were fixed rather than suppressed. Check reports
exclude stale Format artifacts and handle unchanged EOF diagnostics as before.
Final receipt hashes are verified after the local checkpoint.
