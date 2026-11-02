# Map-as-sequence and private pair boundaries

The repository began clean at 8c5b859. Graph priority remains Serde 56 reverse
dependents, Futures/Rand 30 and JSON 21. This bounded repair covers the direct
map sequence view and its private pair adapter, without changing adjacent
owners or including pre-existing dirty work. Earlier map key/entry/value and
sequence repairs, corrected fixtures and original/final receipts are preserved.

## Source and translator provenance

The complete original serde_core de/value.rs and prior successful full-file
build/rust-kotlin-seq-fuse/ValueReview.kt are reused after hash verification.
No new translator process, source extraction or root-guard workaround is used.
Generated generic/associated-type/error lowering remains incomplete; scratch
is not installed or claimed runnable. The repair is manual exact-source
adaptation using existing SerdeResult and SeqElement owners.

Actual MapDeserializer's SeqAccess obtains a pair and calls the supplied seed
through private PairDeserializer, mapping success to Some. PairDeserializer
calls the visitor, short-circuits its typed error, then checks whether the pair
was fully consumed. Incomplete consumption is a typed invalid-length error.
PairVisitor takes its key slot before conversion/seed, then its value slot
before conversion/seed, mapping success to Some. Panics propagate at every
boundary; typed seed/visitor errors remain results.

Kotlin wrapped all three methods in serdeCatching, hiding outer seed, visitor
and private element-seed exceptions. The direct map element method now returns
canonical result mapping; the pair deserializer uses result flatMap so a typed
visitor error bypasses leftover checking; the pair visitor takes/clears slots
before direct seed mapping. The guaranteed private size hint is checked rather
than defaulted to zero, matching Rust's unwrap invariant. No new owner,
signature, state flag or substitute iteration algorithm is introduced.

## Independent regressions

Eight Rust cases execute actual unchanged serde_core MapDeserializer,
PairDeserializer and PairVisitor through public SeqAccess and custom visitors.
Fixtures implement only seed/visitor/conversion interfaces, never the adapters.
Cases cover outer seed panic consuming one entry, outer typed seed error,
visitor panic, key/value seed panic consuming its slot, unchanged partial-pair
length error, typed key error skipping a deliberately panicking value conversion,
and a present pair with two nullable decoded values.

The private-slot tests catch a seed panic inside the visitor, then inspect the
remaining size hint/value/EOF and return normally. This makes the private
boundary observable without changing upstream source. They establish actual
implementation traces, not general panic recovery, borrow safety or ownership
equivalence.

The eight matching Kotlin cases run on original code with four failures and
four passing controls. Two private-slot failures surface through SerdeException
because the old outer catches convert the visitor's failed inner exception
assertion into a typed error; this is not an expected exception substitution.
The assertions themselves remain unchanged, and all cases execute successfully
after the direct-boundary repair. No existing test or assertion changes.

## Scope and limits

Only direct map-as-sequence and these private pair boundaries are certified.
Outer map deserializeAny/deserializeSeq visitor wrappers and other legacy
serdeCatching sites can still convert exceptions. Nullable single-key versus
EOF ambiguity, wider Int/usize counts/hints, error-generic differences and
Kotlin reference/GC versus Rust move/borrow/Drop remain outside scope.
The entry-pair and sequence tags retain decoded null independently of EOF;
this does not claim the single-key API limitation solved.

Public signatures are unchanged and no caller migration is introduced.
Earlier exact-API Futures bound audits and consumer-rebuild caveat, extra
FusedStream/shared-helper/Into/From/clone/platform limits remain preserved.
No downstream build, Swift/other-platform, publication or complete-library
parity is claimed.

Raw red/oracle/Gradle logs, translator-reuse and source-integrity/static evidence
remain under build/rust-kotlin-map-sequence-boundary. receipt.json records
commands, hashes and original failures. Test-only Cargo uses vendored
serde_core offline; Cargo.lock remains ignored under existing policy with its
hash recorded. No production pins, repositories/configuration, UI, adjacent
owners, remote CI, push, PR, tag, release or publication changed.

## Final-source local checks

All eight actual Rust cases pass. All 480 JVM and 248 macOS ARM tests pass,
including the eight new cases, corrected iterator fixtures and all prior
boundary/fuse/string/variant suites. The combined Gradle invocation exits 1
for existing static failures: 50 Detekt and 6,570 ktlint findings, all on
unchanged 8c5b859 baseline lines, none authored. Static attribution excludes
stale Format reports and handles unchanged EOF diagnostics as before.
Final receipt hashes are verified after the local checkpoint.
