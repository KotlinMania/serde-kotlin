# Result visitor programming-exception boundary

Baseline: `9551a45`; the Serde checkout was clean. Serde remains graph-prioritized
with 56 direct consumers versus 30 each for Futures/Rand and 21 for JSON. No
other checkout or agent-owned work changed. Prior collection, map, enum and
in-place repairs remain separate checkpoints.

Upstream `tmp/serde/serde_core/src/de/impls.rs:3071` calls `data.variant()` and
maps the selected newtype payload into Rust Result::Ok or Result::Err. It does
not catch accessor or payload-codec panics. Kotlin's ResultVisitor.visitEnum
used serdeCatching around the whole operation, turning programming exceptions
into typed deserialization errors.

The repair uses canonical SerdeResult.flatMap/map and existing ResultValue<T,E>.
Decoded Err data remains a successful deserialization of ResultValue.Err; it is
not converted to SerdeResult.Failure. Unknown-variant diagnostics and typed
errors remain unchanged. No new result type, public signature, dependency,
configuration, existing test or assertion changed. Exact source replacement
verification ensures prior fresh-list, in-place and allocation code is untouched.

The same exact impls.rs and previously successful full-file rust-kotlin output
were hash-verified and reused. Generated ResultVisitor lines 7063-7087 illustrate
direct variant/payload result composition, but its built-in Result scaffolding
is not adopted as a replacement for the canonical ResultValue owner. This is a
manual consumer repair from generated/direct-source comparison, not a generator
fix, new translator run or claimed generated-source adoption. The recorded
mod.rs parser failure was not retried.

Ten actual Rust tests use the actual Result<T,E>::deserialize implementation:
Ok and Err data, selected codec traces, both payload panics/errors, accessor
panic/error, unknown-variant rejection before payload decoding, and Ok(None)
with Result<Option<i32>,i32>. Kotlin's matching suite initially failed only the
three panic cases; seven controls passed. Kotlin error-object identity checks
are binding-specific; Rust checks error content. Ok(null) remains present data,
not absence of a deserialized result. No assertions were weakened after the
production edit; the regression source hash is retained from before repair.

Reproduce locally:

```
cargo test --offline --manifest-path verification/result-visitor-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-result-visitor-boundary/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt records final-source runtime/static results and local log/XML hashes.
Existing global static failures are not called green. Cargo.lock stays ignored
under existing policy with its local hash recorded. Historical failed receipts
remain unchanged. No remote CI, push, PR, merge, tag, publication, release or
configuration change occurred.

Remaining limits include other Serde exception boundaries, nullable-map-key versus
absence representation, generic error/trait specialization, Int/usize widths,
Rust borrow/ownership/Drop versus Kotlin references/GC, cautious allocation,
untested platforms and downstream integration. This bounded set of codecs does
not establish full Result or Serde parity.
