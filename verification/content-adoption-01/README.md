# Generated Content production adoption

The complete verified translation of `serde_core/src/private/content.rs` now
stores captured production values. This replaces the storage used by both
Content visitors, root cloning/replay, tagged capture, and public
`UntaggedContent`. The legacy declaration remains an input compatibility model;
it is no longer the representation used by those capture/replay paths.

## Source and production boundaries

- Raw output: `../rust-kotlin/verification/foundation-batch-04/content-01/generated/generated.kt`.
- Raw SHA-256: `b2ba1c9cd56f95e112763f26fffccf6848fed96c0a88a41e61d5a81726560b75`.
- Rust source SHA-256: `5fdfb2bb95ecc80375507acb813a4c640496385e56fc99ab448f6b19e01fcc01`.
- Installed at `serde_core/src/commonMain/kotlin/io/github/kotlinmania/serdecore/priv/generated/content/Content.kt`.
  Only the source header and package prefix are added; the entire generated body
  is unchanged. `provenance.json` records this boundary.
- Same-module access is in
  `serde_core/src/commonMain/kotlin/io/github/kotlinmania/serdecore/priv/contentaccess/GeneratedContentAccess.kt`.
  It exposes the generated internal fields to the separate root module without
  changing generated declarations.
- Production consumers are core `priv/ContentVisitor.kt`, root `priv/De.kt`, and
  `UntaggedContent.kt`. Sequences/maps transfer their capture buffers directly;
  the measured Apple allocation bounds and other-target growing fallback in
  `priv/ContentLayout.kt` remain intact.

## Public entry points and compatibility

`Visitor.visitCharCodePoint(Int)` represents every Rust scalar without narrowing
to one UTF-16 code unit. BMP inputs retain the old `visitChar(Char)` dispatch;
supplementary inputs fall back to a complete scalar string unless the visitor
overrides the new method. `CharCodePointDeserialize` and
`CharCodePointDeserializer` provide complete-scalar input/output entry points.
The old `CharDeserialize` still returns a Kotlin `Char` and rejects supplementary
scalars. Invalid scalar values and malformed UTF-16 capture input are rejected.

`Visitor.visitBorrowedByteSlice(SharedByteSlice)`,
`BorrowedByteSliceDeserialize`, and `BorrowedByteSliceDeserializer` retain the
same backing storage and exact start/end region through capture, clone, and
both replay modes. Borrowed input must remain immutable, as with the prior
borrowed-array contract. The old `visitBorrowedBytes(ByteArray)` fallback
materializes only the selected bytes: a ByteArray cannot encode an offset/length
view, and the existing slice API does not expose its backing array. Existing
array consumers retain byte values but cannot retain backing-array identity
through this fallback. Region-aware consumers must override the new method.

The old Content constructors remain available. Both private replay factories
accept them and convert once at the boundary, copying owned payloads and
retaining borrowed storage. Owned generated strings/byte buffers are independent
across clones. UTF-8 text inspection rejects malformed sequences instead of
silently inserting replacement characters.

`UnexpectedScalarChar` adds a sealed Unexpected subtype for supplementary
character diagnostics; downstream exhaustive `when` expressions need its new
case. Existing public methods and the legacy Content model were not removed.
These are source-level compatibility provisions, not a claim of binary ABI or
all-target validation. Pre-existing ABI dump edits were preserved untouched.

## Validation

`check_native.py --out tmp/content-adoption-01/native-05 --content-regressions`
compiles the complete current core and root production source sets with Kotlin
Native 2.4.21 for `macos_arm64`, warnings as errors and offline compiler mode.
It uses already-resolved Gradle-cache KLIB files recorded in the previous direct
adoption receipt, without changing repositories or resolving through Maven.
Its receipt hashes every input source and dependency, runs the existing core
and relevant root suites, and compares the production generated declaration
against all 894 frozen canonical Rust observations.

The added regressions cover complete scalar boundaries and invalid inputs,
empty/full/interior borrowed regions, repeated untagged attempts, nested clones,
owned-copy independence, legacy input conversion, owned/reference visitor
dispatch, tagged keys, and strict UTF-8 selected-region decoding. Existing
allocation, borrowing, nullable-sequence, untagged, and diagnostic tests run too.

Only the two root network-address tests in `TestRoundtripTest.kt` are excluded
from this direct Content regression run: they require the external `serde-test`
transitive dependency graph. This does not establish all-target, Swift, ABI,
serde-json, or full Gradle task success.

The initial offline Gradle attempt stopped before source compilation because
`io.github.kotlinmania:num-traits-kotlin:0.2.15` was not resolvable from the
configured offline repository metadata (`build-02.log`). After authorization,
the existing command-line local Maven opt-in resolved this blocker. No persistent
repository/configuration changes or publication were performed.

`foreign-before.json` records pre-existing dirty/untracked file hashes. Only the
new scalar diagnostic branch was added to the already-edited `Unexpected.kt`;
the existing diagnostic changes and other foreign files are preserved.

This receipt establishes one whole-file Content adoption. It does not count
seed, Syn ident, lifetime, print, or generator fixture work as adopted output.

Final run `native-05`: both complete production modules compiled; 83 core tests,
19 root regressions, and all 894 canonical Rust observations passed. See
`validation.json`, `core-tests.stdout`, `root-tests.stdout`, and `oracle.stdout`.

Read-only NumTraits inspection found both root/target module metadata and the
matching macosArm64 KLIB already in `/Users/sydney/.m2/repository`. The local
KLIB exactly matches the cached binary used by the direct Native proof. The
subsequent authorized validation used that existing metadata through the
repository's explicit local-validation opt-in; NumTraits was not modified or
published. See `num-traits-resolution.json`.

## Authorized Gradle follow-up

At 2026-10-25 23:44 UTC, the following invocation completed successfully:

```
./gradlew --offline --no-daemon -Pproject.repositories.mavenLocal=true -Dmaven.repo.local=/Users/sydney/.m2/repository :serde-core:macosArm64Test :macosArm64Test
```

Gradle compiled the current production modules and ran **83 core tests and 21
root tests**, with zero failures, errors, or skips. This invocation excluded no
tests: the two `TestRoundtripTest` network-address cases excluded from the earlier
direct compiler run both passed here. Scope is these two macosArm64 Gradle test
tasks; this is not a claim of all-target, Swift, derive, ABI, or serde-json success.

`gradle-local-validation.json` records the exact invocation, source commit,
per-suite XML counts/hashes, and before/after artifact and foreign-work checks.
`gradle-local-01.log` preserves the output with trailing whitespace normalized;
the raw log hash is in the receipt and the raw local copy remains at
`tmp/content-adoption-01/gradle-local-01-raw.log`. All sources still match the
earlier direct Native receipt, preserving its separate 894-row Rust comparison.
