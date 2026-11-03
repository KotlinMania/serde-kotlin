# IPv6 embedded-IPv4 group construction

Baseline: `47c81f6`; the Serde checkout was clean. The graph records 56 direct
Serde consumers versus 30 each for Futures/Rand and 21 for JSON. No other checkout
or agent-owned source changed. Prior IPv4/port grammar, collection, Result,
map-entry, enum and in-place checkpoints remain separate.

Installed Rust 1.99 core/net/parser.rs read_ipv6_addr/read_groups explicitly
supports a trailing IPv4 address consuming two big-endian u16 groups. It requires
two remaining slots, forbids the IPv4 part before `::`, and reserves at least one
zero group for compression. Hex groups have one to four ASCII hex digits and
allow leading zeros. This source was reviewed independently of IPv4 octet and
port rules; scope IDs are a separate socket parsing feature, not an IPv6 literal.

Kotlin's old parseIpv6 handled only colon-separated hex groups, rejecting valid
embedded IPv4 tails and accepting signed/non-ASCII hex through toIntOrNull(16).
The bounded adaptation retains its String/chunk and canonical Ipv6Address byte
representation. Private group/hex routines now enforce the source's slot limits,
tail placement, hex grammar and head/zero/tail construction. The existing strict
parseIpv4 owns dotted-octet validation; it is reused rather than duplicated.
Its typed parse rejection is converted to the source's IPv6 diagnostic, without
catching unrelated programming exceptions. Tail octets become two big-endian
groups. Direct IPv6 errors use `invalid IPv6 address syntax`.

No public signature or new shared address/type owner was introduced. The previous
IPv4 parser, checked port parser, scope limitations, binary network paths,
collection/container/allocation policies, existing tests and assertions remain
unchanged. IPv6 chunk construction uses Kotlin lists, not a claim of Rust stack
array/allocation, borrow or ownership equivalence. No generic parser redesign or
project-wide resolver was introduced.

Exact Serde impls.rs and its existing successful full-file rust-kotlin output
were hash-verified and reused for IPv6/FromStr delegation evidence. Actual
installed standard-library source and actual Rust deserializer execution govern
group behavior; no generated std parser is claimed. This is a manual consumer
repair, not a generator fix or new translation run. The recorded de/mod.rs
failure was not retried or bypassed.

Ten Rust groups and matching Kotlin groups cover eight embedded-tail vectors,
1,024 mapped-octet combinations, hex case/zero-prefix controls, invalid hex/tail
forms, malformed compression/counts, owned strings, IP/socket callers and all
compressed-head lengths that leave room for a tail plus a zero group. Rust passed
all groups first. Kotlin initially passed valid hex controls but failed nine
groups, including rejected valid tails, permissive hex and wrong diagnostics.
No assertion was weakened after production changes. Red Kotlin source/XML and
exact loop-brace/list-layout replacements are retained; inputs and assertions are
unchanged, with a fixed pre-production final test hash.

Source-level callers are the human-readable Ipv6Address codec, parseIpAddress and
parseSocketAddressV6 (also reached by generic socket decoding). Caller checks
verify address bytes, preserved valid IPv4 input, valid embedded-tail sockets
including a zero-prefixed port, and invalid-tail rejection. They do not establish
wrapper diagnostic normalization. Scoped sockets and flow information remain
unrepresented/unsupported by the existing Kotlin model; this step does not drop
or fabricate them or claim parity for them.

Reproduce locally:

```
cargo test --offline --manifest-path verification/ipv6-embedded-tail-oracle/Cargo.toml --target-dir build/rust-kotlin-ipv6-embedded-tail/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt includes final-source runtime/static results, compiler/source and
caller evidence, plus local log/XML hashes. Cargo.lock stays ignored under
existing policy, with its local hash recorded. Prior failed receipts and fixture
compile-history evidence remain untouched. No remote CI, push, PR, merge, tag,
publication, release or configuration action occurred.

This establishes the listed IPv6 literal/group cases, not full networking parity
or a security claim. Wrapper diagnostics, scoped socket model coverage, generic
hashing/allocation, nullable keys, other exception boundaries, Rust ownership/Drop
versus Kotlin references/GC, generic/usize binding, other platforms and broader
downstream integration retain their limitations.
