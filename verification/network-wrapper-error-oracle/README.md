# Normalize network wrapper diagnostics to Rust AddrKind

Baseline: `17a7ca3`; the Serde checkout was clean. The dependency graph records
56 direct Serde consumers versus 30 each for Futures/Rand and 21 for JSON. No
other checkout or agent-owned source changed. Prior IPv4/IPv6/port, collection,
Result, enum and in-place checkpoints remain separate.

Installed Rust 1.99 core/net/parser.rs parse_with associates failure with the
requested wrapper's AddrKind. AddrParseError Display therefore emits `invalid IP
address syntax`, `invalid socket address syntax`, or the typed IPv4/IPv6 socket
message. It does not expose the last attempted subtype or a host-only diagnostic.
The source parses typed socket hosts before ports.

Kotlin's generic IP/socket dispatch exposed the last subtype error, and its typed
socket helpers could expose raw host errors. The bounded repair maps expected
SerdeException parse rejections to the enclosing wrapper's canonical message and
puts typed host parsing before port parsing. Protocol dispatch catches only
expected typed parser rejections locally. The outer FromStringVisitor still uses
its existing serdeCatching boundary: **this is not a claim of overall programming-
exception propagation parity**. No unexpected failure is injected to fabricate
such a proof.

No grammar, address/type owner, public signature, byte encoding, binary network
path, allocation/container policy, existing test or assertion changed. Exact
source replacement verifies that only four private wrapper helpers changed;
strict IPv4, IPv6 group/tail, checked port and earlier visitor code stays intact.
No caller adaptation or broad downstream build was needed for a signature change.
Source-level helper users are the four corresponding human-readable codecs.

The same Serde impls.rs and successful full-file rust-kotlin output were hash-
verified and reused. Its FromStrVisitor maps the actual FromStr error through
Error::custom. Installed standard-library source/commit and actual Rust serde
execution provide wrapper-kind evidence. This is a manual consumer repair, not
a generator fix/new translation run. The recorded de/mod.rs failure was not retried.

Nine actual Rust groups and matching Kotlin groups cover generic IP/socket errors,
typed socket invalid hosts, both-invalid host/port inputs, owned-string errors,
and valid IPv4/IPv6/embedded-tail/zero-prefixed-port values. Rust passed all groups
before production edits. Kotlin initially failed five diagnostic groups while
four controls passed. The combined-error controls check the public error kind;
they do not instrument hidden parser call order, which is established by source
comparison. Original red source/XML are retained. Only list/chain layout was
formatted before production; tokens other than optional call commas, assertion
values and input vectors remain unchanged, with a fixed pre-production hash.

Rust-valid scoped socket inputs are not mislabeled as rejected oracle controls.
Scoped socket IDs/flow information remain unsupported/unrepresented by the existing
Kotlin model; acceptance parity for them is not claimed. Generic IP's `%` literal
negative is an actual Rust-rejected IpAddr input, not a scoped SocketAddr test.
Wrapper messages are verified for the listed rejected inputs, not a claim of
complete networking behavior or downstream compatibility.

Reproduce locally:

```
cargo test --offline --manifest-path verification/network-wrapper-error-oracle/Cargo.toml --target-dir build/rust-kotlin-network-wrapper-errors/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt includes final-source runtime/static results, compiler/source/caller
and local log/XML hashes. Cargo.lock stays ignored under existing policy, with
its local hash recorded. Prior failed receipts and fixture compile-history
remain untouched. No remote CI, push, PR, merge, tag, publication, release or
configuration change occurred.

Scoped sockets, broader exception boundaries, generic allocation/hashing, nullable
keys, Rust borrow/ownership/Drop versus Kotlin references/GC, generic/usize
binding, platforms and downstream integration retain their limitations. No
security or full-networking-parity claim is made.
