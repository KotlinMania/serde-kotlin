# Visitor and end-check exception boundaries

This bounded manual repair follows the unchanged Serde 1.0.228 source at
`tmp/serde/serde_core/src/de/value.rs`: sequence `end`/`deserialize_any` and map
`end`/`deserialize_any`/`deserialize_seq`. The existing successful full-file
rust-kotlin output was reused after verifying source and output hashes; no new
translator process or generator edit was needed. See `receipt.json` for provenance.

Rust calls the visitor, propagates a typed error immediately, then counts remaining
input and returns an invalid-length error when appropriate. It does not catch
visitor or iterator panics. Kotlin's five methods previously wrapped this in
`serdeCatching`, turning programming exceptions into deserialization errors.
The repair uses canonical `SerdeResult.flatMap`/`map` and explicit typed length
results; the existing counters, drain order, exhaustion state and comments remain.
No public signatures changed. No existing assertions or tests were edited.

The actual Rust oracle has eight tests (fourteen scenarios). It verifies visitor
panics skip end checks in sequence, map and map-as-sequence entrypoints; iterator
panics during automatic end checks propagate; typed visitor errors skip input;
partial visitors retain exact length errors; complete visitors retain results and
input-read counts. The matching Kotlin suite initially had five failures and three
passing controls. Pair-sum fixtures only cover valid two-element pairs, not arbitrary
malformed pair inputs. Existing pair-boundary regressions remain in the full suite.

Run locally from the owning repository:

```
cargo test --offline --manifest-path verification/visitor-end-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-visitor-end-boundary/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

Both full runtime suites retain one failure in the unchanged
`MutablePlaceTest.existingValueSequenceStillHasItsOwnExceptionConversion`, which
explicitly expects the removed catch-all conversion. The propagated `sentinel`
exception is consistent with the boundary repair, but this legacy assertion has
not been updated and the full suite is not green. No test was weakened to fit it.
The new eight-test suite passes on both targets. First final checks also found
five formatting diagnostics in the new test's when branches; only braces and
spacing were corrected, with first-run source/log/report retained. No assertions
or production behavior changed in that formatting correction.

Raw local logs and XML attribution are under
`build/rust-kotlin-visitor-end-boundary/`; their hashes are recorded in the receipt.
Cargo.lock is locally generated and ignored by existing repository policy; its hash
is recorded without changing that policy. No remote action or publication occurred.

This does not establish full Serde parity: other catch-all boundaries, nullable map
keys versus absence, consuming Rust ownership versus Kotlin references/GC, generic
trait behavior, Int/usize widths, untested platforms and downstream integration
remain outside this repair. Rust panic propagation corresponds here to Kotlin
programming exceptions; their payload and runtime models are not identical.
