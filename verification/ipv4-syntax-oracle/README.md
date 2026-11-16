# Strict human-readable IPv4 syntax

Baseline: `9ccc543`; the Serde checkout was clean. The workspace graph prioritizes
Serde with 56 direct consumers versus 30 each for Futures/Rand and 21 for JSON.
No other repository or agent-owned source changed. Prior collection, Result,
map-entry, enum, in-place and allocation-policy checkpoints remain separate.

Serde's parse_ip_impl and FromStrVisitor delegate human-readable Ipv4Addr decoding
to Rust FromStr. The installed Rust 1.99.0 standard-library source at
`library/core/src/net/parser.rs` reads exactly four decimal u8 octets. Each has
one to three ASCII decimal digits, no multi-digit leading zero, and fits u8.
The installed source/commit and executable oracle evidence are recorded.

Kotlin parseIpv4 used toIntOrNull, admitting signed and non-ASCII digits and
zero-prefixed octets. Its existing error text also omitted Rust's `syntax`
suffix. The repair retains the current four-part String adaptation and canonical
Ipv4Address owner, applies the source digit/width/zero-prefix rules, accumulates
decimal digits after ASCII validation, and checks the u8 range. Maximum three
digits bounds intermediate arithmetic to 999; no overflow approximation is used.
The source's octal-prefix comment is retained. Direct IPv4 failures now use the
actual Rust diagnostic `invalid IPv4 address syntax`.

The exact Serde impls.rs and existing successful full-file translator output were
hash-verified and reused. Generated IPv4 and FromStrVisitor paths show delegation;
there is no generated standard-library parser output being claimed. The bounded
manual parser adaptation follows the installed Rust source and actual Rust
Ipv4Addr::deserialize oracle. No generator edit or new translation run occurred,
and the previously recorded de/mod.rs failure was not retried.

Nine Rust tests and matching Kotlin tests cover 1,024 valid octet-position
combinations, signs, leading zeros, non-ASCII digits, overflow/width, malformed
shapes, borrowed/owned strings and direct IP/socket callers. Rust tests all passed
before production edits. Kotlin initially passed the full valid matrix and
failed eight other groups: some inputs were incorrectly accepted, while other
already-rejected inputs had the wrong direct diagnostic. The red XML/log retains
the distinction. No existing assertion or test was changed, and the new test
source hash is retained from before the production edit.

Source-level helper callers are Ipv4AddressDeserialize's human-readable path,
parseIpAddress and parseSocketAddressV4; the latter also serves generic socket
parsing. The direct IP/socket tests verify rejection of invalid IPv4 hosts and
continued valid localhost parsing. They deliberately do not assert diagnostic
normalization for those wrappers: their pre-existing generic-IP/socket diagnostic
differences remain unresolved. IPv6 parsing (including dotted-tail support), port
parsing and all non-human-readable network decoding remain unchanged. No public
signature changed and no downstream integration build was run.

Reproduce locally:

```
cargo test --offline --manifest-path verification/ipv4-syntax-oracle/Cargo.toml --target-dir build/rust-kotlin-ipv4-syntax/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt records final-source runtime/static results, source/caller evidence
and local log/XML hashes. Cargo.lock remains ignored under existing policy with
its local hash recorded. Historical failed receipts and fixture compile-history
evidence remain untouched. No remote CI, push, PR, merge, tag, publication,
release or configuration action occurred.

This is bounded IPv4 input/value/error parity, not full networking parity or a
security claim. Other network syntax/diagnostics, generic hashing/allocation,
nullable-map-key ambiguity, other exception boundaries, Rust ownership/Drop versus
Kotlin references/GC, usize/generic binding, platforms and downstream integration
retain their previously documented limitations.
