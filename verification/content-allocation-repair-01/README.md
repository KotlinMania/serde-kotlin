# Content capture allocation regression repair

The integration in f47aa45d8d11f2af10d59b96fb7b733e872d45f7 introduced two root JVM test failures:

- io.github.kotlinmania.serde.UntaggedContentTest.failedAttemptDoesNotConsumeTheNextAttempt
- io.github.kotlinmania.serde.UntaggedContentTest.nullableElementsAreValuesRatherThanEndOfSequence

Previously, cautious<Element>(Int?) silently assumed eight bytes for unknown Kotlin types, so both Content and ContentMapEntry reserved up to 131072 elements. The integration replaced this guess with actual Rust layout evidence: 32-byte Content and a separately measured 64-byte pair for pinned aarch64-apple-darwin. Its strict lookup rejected every unmeasured target before reading even a null hint. This made existing JVM untagged sequence deserialization fail.

The five capture allocation sites now call newContentBuffer/newContentMapBuffer in ContentLayout.kt. Where the measured Rust profile exists, these factories use the unchanged generated cautious function with the exact unsigned size. Else they create an ordinary Kotlin ArrayList with no explicit capacity and grow as elements arrive. This is an explicit Kotlin allocation adapter: it does not estimate Rust layout or claim identical reservation, allocation failure, or memory use on unmeasured profiles. It preserves captured values, iteration and validation order. Negative hints reject before iteration on either path. Huge positive hints are safe without speculative reservation.

Strict cautiousContentCapacity/cautiousContentMapCapacity, sizeOf<Element>(), and the generic exact-size contract continue to reject missing evidence. Their restrictions are not silently relaxed. The two generated String and SizeHint bodies remain byte-identical. No dependency, target, FFI, kernel, workflow or security configuration changed. The two new buffer factories expand the public core API so the root module can use them. Compiler-produced ABI additions are staged separately from existing foreign changes.

Validation: the existing Gradle JVM and macOS ARM64 suites pass: core JVM 74, root JVM 12, core Native 77, root Native 15. Both named regressions pass. Three added common tests cover unknown/zero/large hints, storage growth, actual capture calls, and negative-hint rejection. Native tests retain all five measured allocation caller checks. This is a repair to an existing adoption, not a new translated whole file. Other targets were not runtime-tested; the common no-evidence branch is exercised on JVM. Swift export compatibility is not asserted.

Commands, XML-derived counts, raw hashes, compiler-generated ABI outcome and preservation checks are in validation.json and tests.txt/abi.txt. Original failed-baseline evidence remains in rust-kotlin/verification/foundation-batch-04/target-regression-01.
