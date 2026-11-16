# Resolve the historical Value sequence exception characterization

Baseline: `adfcae0`. This step changes no production code, dependency, configuration
or public signature. Earlier production parity work remains a separate checkpoint.

The former `MutablePlaceTest.existingValueSequenceStillHasItsOwnExceptionConversion`
expected a `SerdeResult.Failure("sentinel")` and a retained element value of `800`.
The historical `verification/seed-place-prep-01/README.md` explicitly recorded the
then-unchanged Value sequence provider's exception conversion as a limitation.
It did not establish that conversion as a permanent Kotlin-specific contract.

After `adfcae0` removed the provider's catch-all conversion to match Rust, that
unchanged assertion failed: `IllegalStateException("sentinel")` escaped before
its mutation assertion ran. The prior full suites were **487 passed / 1 failed
on JVM**, and **255 passed / 1 failed on macOS ARM**, with zero errors/skips.
Those failures are preserved, not reclassified as passes.

Before modifying the Kotlin expectation, the new Rust integration test
`verification/visitor-end-boundary-oracle/tests/inplace_one_element.rs` ran
against unchanged local serde_core: `Vec<Item>` initially containing `Item(1)`,
input sequence `[-2]`, and an element override that writes `800` before panicking
with `sentinel`. The panic escaped, mutation survived, and vector storage was
retained. This uses actual Rust Vec in-place decoding, InPlaceSeed and
SeqDeserializer, rather than a substitute algorithm. Upstream call sites are
`de/impls.rs:1205`, `private/seed.rs:18` and `de/value.rs:1057`.

The Kotlin characterization is now named
`existingValueSequencePropagatesExceptionAndRetainsMutation`. It requires the
exception type and message, preserves the original `800` assertion, and checks
list and element identity. Its mutable-place setter now records any replacement
rather than immediately throwing `list replaced`, allowing the identity assertion
to detect replacement directly. It does not silently accept replacement. All
other test assertions and production source remain unchanged.

`receipt.json` records final-source runtime/static results and hashes. The original
`verification/visitor-end-boundary-oracle/receipt.json` remains untouched. Before
any edit, every one of its 27 hashed files was verified and copied into
`build/rust-kotlin-inplace-panic-characterization/prior-adfcae0/`; the snapshot
manifest and original receipt hash are in `prior-evidence.json`. This preserves
failed XML/logs and earlier test source even though standard Gradle result paths
are reused by subsequent runs. Do not apply that historical receipt's source
hashes to today's changed characterization.

This case establishes neither full Rust ownership equivalence nor all exception,
nullable-key, platform or downstream behavior. Kotlin object identity and Rust
storage identity are distinct runtime observations. Existing global static gate
failures remain explicitly reported; no rules were suppressed.
