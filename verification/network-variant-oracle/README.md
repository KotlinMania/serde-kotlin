# Non-human-readable network variant identifiers

## Selection and ownership

The repository started clean at 7af36b9. Serde has 56 inbound caller repos in
the inspected graph (Tokio 33, Futures/Rand 30). The bounded material omission
rejects valid binary-mode IpAddr/SocketAddr variants, using existing canonical
serdecore ADTs, value deserializers and the private source-validated variant
decoder. Adjacent owners are untouched; no prior dirty work is included or
newly attributed. Earlier dependency/content, sequence/map, string error-boundary
and Bound/Result checkpoints remain separate, with corrected receipt/attribution
evidence preserved.

## Independent source and translator evidence

The original tmp/serde/serde_core/src/de/impls.rs SHA-256 still matches the
previous successful full-file CLI translation. This step reuses
build/rust-kotlin-string-in-place/ImplsReview.kt, with exact input/output hashes
and explicit reuse recorded under build/rust-kotlin-network-variants. No duplicate
CLI job or fake extracted source is used. Macro/projection/type lowering in
scratch output is not claimed runnable. The variant_identifier and
deserialize_enum macro definitions and actual IpAddr/SocketAddr invocations
were independently reviewed in original Rust, rather than assuming that the
Bound/Result decoder applied.

The network macros accept unsigned 0/1 as V4/V6, recognized strings and byte
names, invalid_value for other unsigned indices, unknown_variant for unknown
valid text, and invalid_value(Unexpected::Bytes) for malformed UTF-8. Known byte
names match before unknown-name UTF-8 decoding. These are the exact rules
already implemented by the private variant decoder. Both network enums select
that representation only when is_human_readable is false. Their human-readable
paths use separate FromStr parsing. IPv4/IPv6 payloads deserialize fixed octet
tuples; socket payloads deserialize (address, u16). Rust's SocketAddrV6 source
constructs flowinfo/scope_id as zero in this representation.

Kotlin's two network visitors still used the struct-field identifier helper.
The entire production patch changes those two calls to the existing private
variant decoder. That decoder, all human-readable parsers, payload algorithms,
struct-field behavior and public signatures/callers/assertions remain unchanged.
No new parser, network type owner, compatibility helper, security claim or
settings change is introduced.

## Runtime evidence

Seven Kotlin cases and seven actual serde_core Rust cases cover both numeric
IP families, both numeric socket families with port 8080, recognized byte IP
names, unknown socket text, malformed IP identifier bytes, out-of-range index
errors for both enums before payload access, and known-good human-readable
IPv4/IP-socket controls. Results check complete represented ADTs and exact
payload trace/error text. The old Kotlin runs seven methods and fails five;
known-byte and human-readable controls pass. Numeric-loop failures occur at
the first old rejected index; final runs cover both indices. Red log and
XML-derived failure receipts remain under build/rust-kotlin-network-variants.
No existing assertion or warning rule is weakened.

The fixtures only drive non-human-readable EnumAccess, primitive and tuple
channels. Kotlin reuses canonical value/SeqDeserializer nodes with a mode
wrapper; Rust uses a minimal binary format with actual serde_core Deserialize
for std::net IpAddr/SocketAddr and address/port types. Identifier mapping and actual result construction are left to the library
under test; fixture constants construct only expected assertion values. No
fixture implements address parsing. No socket I/O
or external network is involved. Only the listed valid payload shapes are
validated; arbitrary malformed tuple payloads are not claimed equivalent.

```sh
CARGO_TARGET_DIR=build/rust-kotlin-network-variants/cargo-target cargo test --manifest-path verification/network-variant-oracle/Cargo.toml --offline --locked
```

## Remaining limits

Kotlin's existing network ADTs do not fully reproduce Rust type relationships,
Display/FromStr behavior, borrow/move/Drop, IPv6 scope/flow metadata or resource
semantics. Human-readable parsing is unchanged; positive controls do not prove
all strict validation cases or security properties. Ordinary index tests do
not cover every u64 diagnostic channel. Byte metadata allocation and prior
nullable-key, hint/Int-usize, panic/destruction, failed-sink, fairness/threading
and downstream/platform limitations remain. Previous actual Rust UTF-8 controls
are still distinct from Kotlin-only setter tests. No pins, repositories or
runtime/build/publication settings changed. No downstream source/binary,
Swift/other-target or fresh publication validation is claimed, and no foreign
owner or UI session is touched.

## Final-source local checks

All 447 JVM tests and 215 macOS ARM tests pass, including the seven new
network cases and the preserved Bound/Result (eight), iterator (five sequence,
seven map) and string-boundary (eight) suites on both targets. All seven actual
upstream Rust network cases pass. The complete production diff is two private
identifier calls; no previous dirty work or earlier authored repair is swept
into this checkpoint.

Combined Gradle validation remains unsuccessful at global static gates:
50 Detekt findings and current ktlint Check groups common-main 2591,
common-test 242, JVM-test 3726, macOS ARM-test 6 and Kotlin-script 5 (6570 total
reported findings). Other checked groups have zero. Every location maps to
unchanged baseline 7af36b9 lines; there are no authored Detekt/ktlint findings.
Unchanged EOF attribution and historical Format-report exclusion remain intact;
no rule, assertion or warning setting changed.

Final hashes/validation and verified-reused-translator/runtime-red/oracle/
Gradle logs remain under build/rust-kotlin-network-variants. Hashes include
preserved fixtures and the committed prior receipt/attribution audit. Public
signatures/callers, pins and earlier Futures 09c4126 are unchanged. All parser,
resource, type/ownership, numeric-width and downstream/platform caveats above
remain explicit.
