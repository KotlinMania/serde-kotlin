# Bound and Result variant-identifier parity

## Graph priority and ownership

The repository started clean at 090453b. Serde has 56 inbound caller repos in
the inspected dependency graph (Tokio 33, Futures/Rand 30). This material omitted
capability rejects valid numeric enum indices, affecting non-text identifier
formats without a public API change. Adjacent owners remain untouched and
no earlier dirty work is included. Prior dependency/content, sequence/map and
string-boundary checkpoints remain separate, with their corrected fixture and
static-attribution evidence retained.

## Verified translator reuse and exact source

The step reuses the successful full-original-file output
build/rust-kotlin-string-in-place/ImplsReview.kt. The unchanged original
`tmp/serde/serde_core/src/de/impls.rs` SHA-256 matches that previous successful
translation input; input/output hashes and explicit reuse are recorded under
build/rust-kotlin-variant-identifiers/translator-reuse.json. Its Bound/Result
Field visitors include visitU64 and strict byte identifier error branches.
No duplicate CLI job or fake extracted/renamed input is used. Scratch associated
types/imports and ownership lowering remain unresolved, so output is not
claimed runnable. This repair is manual exact-source adaptation informed by
that output and independent Bound and Result source review, not a generator fix.

Both upstream enums deserialize an identifier through a private FieldVisitor.
Bound accepts unsigned 0/1/2 as Unbounded/Included/Excluded; Result accepts
0/1 as Ok/Err. Other unsigned indices return invalid_value with the exact
variant-name expectation. Recognized strings/bytes identify the variant.
Unknown valid strings report unknown_variant; invalid UTF-8 bytes report
invalid_value(Unexpected::Bytes), not a lossy unknown field. Identifier failure
happens before any unit/newtype payload access. These FieldVisitor rules differ
from source struct field identifiers, which are not broadened to accept numbers.

Kotlin had routed these two enum visitors through fieldIdentifierDeserialize,
which omitted unsigned variant indices and reported field errors. A private
variant decoder now follows the confirmed source rules for those two callers
only, reusing canonical Deserialize/Visitor/SerdeResult/Unexpected and the
existing BoundValue/ResultValue representations. It checks unsigned bounds
before converting the small valid index. Byte names are compared against known
variants first, then only unmatched bytes are strictly decoded for the source
unknown-variant/invalid-byte distinction. Existing struct-field handling and other enum callers
are unchanged. Public signatures/callers/assertions, generic exception helpers
and canonical type ownership are unchanged; no global resolver/Option migration
or public compatibility helper is introduced.

## Runtime evidence

Eight final Kotlin tests and eight actual serde_core Rust oracle cases cover all
three valid Bound indices, both valid Result indices, ordinary out-of-range
indices for both, unknown Bound text, malformed Result identifier bytes and
continued rejection of numeric Range struct fields, and recognized byte names
for all five Bound/Result variants. Valid cases assert exact
ADT results and unit/newtype payload traces. Errors assert exact source text
and zero payload access. The initial old-Kotlin red run executes seven methods and fails
six; the struct-field control passes. The known-byte-name control was added
after that run to validate exact byte-match ordering before unknown-name UTF-8
decoding, without changing any earlier assertion. Initial seven-case local
and Rust logs are retained separately from the final eight-case receipts. The numeric-loop failures occur at their
first rejected index; final runs cover every listed valid index. No existing
assertion or warning rule was weakened.

The Rust format fixtures only deliver numeric/text/byte identifiers and payload
channels; actual upstream Bound/Result/Range Deserialize implementations perform
all mapping/validation. No fixture reimplements their FieldVisitor. The local
serde_core dependency's default result feature is used. Kotlin red log and
XML-derived failures are retained under build/rust-kotlin-variant-identifiers.

```sh
CARGO_TARGET_DIR=build/rust-kotlin-variant-identifiers/cargo-target cargo test --manifest-path verification/variant-identifier-oracle/Cargo.toml --offline --locked
```

## Remaining limits

Byte-pattern metadata is encoded using Kotlin arrays; allocation/capacity and
resource-exhaustion equivalence to Rust static byte literals is not claimed.
Existing Kotlin ADT/private string-identifier representations do not fully
reproduce Rust local Field enum/type relationships or ownership/borrow/Drop.
The cases validate ordinary unsigned indices; all u64 diagnostic formatting
and every primitive identifier channel are not claimed exhaustively covered.
Other enum visitors, including network variants, are not repaired by inference.
Prior nullable-key, non-exhausted-hint/Int-usize, allocation/resource-panic,
failed-sink, fairness/threading and downstream/platform limits remain. The
previous four actual Rust string controls remain distinct from Kotlin-only
setter tests, not retroactively recast as Rust callback evidence. No pins,
repositories, runtime/build/publication settings, foreign owner or UI session
changed. Downstream source/binary, Swift/other-target and fresh publication
metadata remain unverified.

## Final-source local checks

All 440 JVM tests and 208 macOS ARM tests pass, including all eight new
variant cases and preserved sequence (five), map (seven) and string-boundary
(eight) cases on each target. All eight actual upstream Rust variant cases
pass. These final receipts follow the known-byte-order control addition and
new fixture constructor-format correction; earlier seven-case and initial
eight-case logs remain separately retained. Existing assertions are unchanged.

Combined Gradle validation still fails only at global static gates:
50 Detekt findings and current ktlint Check groups common-main 2591,
common-test 242, JVM-test 3726, macOS ARM-test 6 and Kotlin-script 5 (6570 total
reported findings). Other checked groups have zero. All locations map to
unchanged baseline 090453b lines; there are no authored findings. The four new
fixture formatting findings were corrected, not suppressed, before all final
runtime/static checks reran. EOF attribution correction and historical Format
report exclusion remain intact.

Final hashes/validation and reused-translator/initial-red/intermediate/final
Rust and Kotlin logs are retained under build/rust-kotlin-variant-identifiers.
Hashes include preserved iterator/string fixtures and prior receipt audit.
Public signatures, existing callers, pins and prior Futures 09c4126 remain
unchanged. No earlier dirty work is included; all resource/allocation, type,
numeric-channel and downstream/platform limits above remain explicit.
