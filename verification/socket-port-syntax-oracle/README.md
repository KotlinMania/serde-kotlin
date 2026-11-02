# Independent socket-port decimal syntax parity

Baseline: `6b6b804`; the owning Serde checkout was clean. The dependency graph
records 56 direct Serde consumers versus 30 each for Futures/Rand and 21 for JSON.
No other repository or agent-owned source changed. Prior IPv4 grammar/error,
collection, Result, enum and in-place checkpoints remain separate.

This step does not infer port or IPv6 rules from IPv4 octets. Installed Rust 1.99
core/net/parser.rs independently defines read_port through read_decimal::<u16>:
ASCII decimal digits, checked multiplication/addition, arbitrary leading zeros,
and no signs. parse_with rejects trailing input. The source's SocketV4/SocketV6
call sites and AddrParseError display strings were reviewed separately. IPv6
host grammar, scopes, flow information and dotted tails are not being ported.

Kotlin's parsePort used signed toIntOrNull, accepting signs and Unicode digits.
The repair validates each ASCII digit while accumulating, preserves arbitrary
zero prefixes, and checks u16 overflow after multiplication and addition in the
same order as Rust. The wider Kotlin Int temporary cannot overflow: each prior
value is at most 65535 and multiplying by 10 is at most 655350. No Int-sized
acceptance window or arbitrary input-length cutoff is introduced.

IPv4 host/port splitting now uses the first colon, matching the source's complete
IPv4 host followed by read_port. This keeps malformed port tails such as 80:1
in the port parser rather than misclassifying them as part of an IPv4 address.
Direct SocketV4/SocketV6 structural/port error constants now match Rust's `syntax`
messages. Invalid host diagnostics and the generic SocketAddr wrapper's diagnostic
normalization remain separate known limitations. The IPv4 parser itself, IPv6
host parser, public models/signatures, binary network paths, existing assertions,
containers, allocations and all prior visitor repairs are unchanged.

The same Serde impls.rs and successful full-file translator output were hash-
verified and reused for socket/FromStr delegation evidence. The installed Rust
standard-library source and actual deserializer oracle determine port behavior;
no generated standard-library parser is claimed. No new translator process,
generator edit, source substitution or retry of de/mod.rs occurred.

Nine actual Rust groups cover all 65,536 IPv4 port values, IPv6 port boundaries,
long leading-zero forms, signs, non-ASCII digits, overflow, malformed tails,
generic socket callers and owned string dispatch. Tests use valid localhost
hosts to isolate port behavior. Rust passed all groups before production edits.
Kotlin initially passed the complete u16 range, IPv6 boundaries and zero-prefix
controls but failed six other groups, reflecting both incorrectly accepted inputs
and pre-existing direct diagnostic differences. Caller tests assert generic
acceptance/value behavior, not unresolved generic error text.

After red, the new Kotlin test received loop braces and chain layout only before
the production edit. The original red source and exact formatting replacements
are retained and hashed; inputs and assertions are unchanged. The final test's
pre-production hash remains fixed. No existing assertion was weakened or changed.

Reproduce locally:

```
cargo test --offline --manifest-path verification/socket-port-syntax-oracle/Cargo.toml --target-dir build/rust-kotlin-socket-port-syntax/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt records final-source runtime/static results, installed compiler/source
and bounded private-caller evidence, plus local log/XML hashes. Cargo.lock remains
ignored under existing policy, with its local hash recorded. Prior failure and
fixture compile-history evidence remains untouched. No remote CI, push, PR,
merge, tag, publication, release or configuration action occurred.

This is port parsing parity for the listed valid-host contexts, not full socket,
IPv6 or networking parity or a security claim. Generic hashing/allocation,
nullable-key ambiguity, other exception boundaries, Rust ownership/Drop versus
Kotlin references/GC, generic/usize binding, platforms and downstream integration
retain their existing limitations.
