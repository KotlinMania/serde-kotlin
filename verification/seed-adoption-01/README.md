# Whole-file generated Seed production adoption

The complete verified `core/private/seed.rs` translation now replaces the old
production `serde_core/src/common/commonMain/kotlin/io/github/kotlinmania/serdecore/priv/Seed.kt`.
The mutable-list in-place consumer in `de/Impls.kt` passes
`InPlaceSeed(slot, elementDeserialize)` directly to `SeqAccess.nextElementSeed`.
The temporary anonymous-codec bridge from the prerequisite commit is removed.
This is one production whole-file replacement, following the separate Content
adoption; prerequisites and test fixtures are not counted as translated files.

## Provenance and exact-body boundary

- Frozen Rust: `tmp/serde/serde/src/core/private/seed.rs`.
- Frozen Rust SHA-256: `3f6e098c5bd314788370dcaf3ab0152fcd7feb6bcf36a9c51808938cd58071eb`.
- Raw artifact: `../rust-kotlin/verification/foundation-batch-04/seed-01/generated/generated.kt`.
- Raw SHA-256: `53ca4518e8e60cd801b1250ac6bbc40282fb353b552fbbf2f9c6682c6aa84d34`.
- Generator: `b5d855c33f874d88257123d83a61c5c332856643`; proof/handoff: `01369fd`.
- Packaging adds only the existing source header and production package before
  the complete unchanged generated body. `provenance.json` records these values.

The generated constructor is `InPlaceSeed<T>(MutablePlace<T>, Deserialize<T>)`.
It retains the original caller slot and codec, forwards exactly once, preserves
the returned result and escaping exception, and guards against repeated Kotlin
invocation. The list consumer constructs a fresh seed for each existing element.
The old unused internal self-deserializing constructor/factory is removed. The
unrelated unused private helper within root `De.kt` is unchanged and is not a
consumer of this file.

## Production proof

`check.sh` verifies the installed body against the raw hash, then links both
oracles to the complete newly built production serde-core Native KLIB. Its
compiler inputs contain no separate generated Seed body. The seed oracle is the
generator's unchanged 28-observation harness with only the canonical production
import prepended. The collection oracle reuses the nine source-backed Vec
observations from `seed-place-prep-01` through the now-wired production consumer.
Both compare byte-for-byte with retained canonical Rust output; the frozen Rust
source/provider hashes are checked before reuse.

The additional common regressions check original input/slot access, preserved
failure and exception identities, mutation before failure/panic, consumed state
after every outcome, and consumption enforcement through the real list consumer.
Existing mutable-place tests cover truncation, growth, resource reuse, ordinary
replacement, dispatch, and provider-specific exception conversion.

The initial Gradle attempt compiled production successfully but failed a new
test's generic identity assertion. `assertSame<Deserializer>` resolved the test
type inference; the generated body was unchanged. The first log is retained.

## Scope and compatibility

The old seed constructor/factory were internal and had no callers. The new
constructor uses the separate codec witness and actual writable slot. All
existing public deserialization entry points remain available. Global
`serdeCatching`, foreign edits, target/FFI settings, and repository configuration
are unchanged.

The error contract specializes Kotlin `SerdeError`; it is not a universal Rust
associated-error model. Existing providers may still catch exceptions. Exclusive
caller borrowing is required during a seed's lifetime; the consumption guard does
not make Rust-invalid aliasing valid. Custom Drop, exact allocation/address
observations, and untested platforms remain outside the proof. Kotlin list growth
retains the prerequisite's explicit non-layout-based policy.

After production Native compilation, reproduce the focused comparison with:
`./verification/seed-adoption-01/check.sh --out build/seed-adoption-01/production-new`.

Final production validation passed: **93 core tests + 21 root tests**, zero
failures/errors/skips and no exclusions in these two Native Gradle tasks.
The installed seed matched **28/28** canonical Rust observations and its wired
collection consumer matched **9/9**. See `validation.json` and
`gradle-validation.json`. This completes one new adoption; Content plus Seed
make **2/5** production replacements in the current five-file group. No other
target, Swift, ABI, serde-derive test, or serde-json success is claimed.

Gradle log snapshots normalize trailing whitespace. Their original hashes are
in the receipt and raw copies remain under `tmp/seed-adoption-01/`.
