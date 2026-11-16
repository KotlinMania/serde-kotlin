# Seed adoption prerequisites: writable place and real collection consumer

This is preparatory integration work: **zero newly translated files and zero
newly adopted files**. No generated seed artifact exists yet. Core `priv/Seed.kt`
and the duplicate root helper are unchanged. The generator owner supplied the
binding contract at `../rust-kotlin/verification/foundation-batch-04/replacement-five-01/seed-contract-01/contract.md`.

## Exact binding API

Package: `io.github.kotlinmania.serdecore.de`.

```kotlin
interface MutablePlace<T> { var value: T }
fun <T> mutablePlace(getValue: () -> T, setValue: (T) -> Unit): MutablePlace<T>
// Virtual member of the existing Deserialize<T> codec interface:
fun <D : Deserializer> deserializeInPlace(deserializer: D, place: MutablePlace<T>): SerdeResult<Unit>
```

`MutablePlace.kt` retains getter/setter closures for the actual caller field or
list element. It does not snapshot T into a seed-owned field. The separate codec
witness is the existing `Deserialize<T>` interface; T need not implement it.
`DeserializeSeed<Unit>` already supplies the consuming seed's result signature.
The error binding is the existing Kotlin `SerdeError` specialization, not every
possible Rust `D::Error`.

The new virtual overload delegates once to the existing setter overload so that
existing Unit/String/custom setter overrides retain dispatch. The default setter
now uses `SerdeResult.map`: a Failure is returned unchanged, writes happen only
after successful decoding, and thrown exceptions escape. This deliberately
changes the old default's exception-to-Failure conversion to match Rust's default
operation. Signatures remain available. Global `serdeCatching` is unchanged.

## Real consumer and generated handoff

`de/Impls.kt` adds the mutable-place overload to `mutableListDeserialize` and
implements the value/mutation path of actual Rust `Vec<T>::deserialize_in_place`
in `tmp/serde/serde_core/src/de/impls.rs`. It retains the caller list, calls the
element codec's in-place override for existing slots, truncates when input ends,
and appends newly deserialized elements. An error or escaping exception preserves
all earlier mutations, including those made by the failing element override.

The existing `SeqAccess.nextElement(Deserialize<T>)` adapter currently bridges
each per-slot codec call. The explicitly marked handoff site will accept the
verified generated InPlaceSeed instead. No new hand-written InPlaceSeed class was
introduced. The bridge is created and consumed once per attempted existing slot;
future generated seeds must retain that ownership discipline.

The list uses ordinary Kotlin growth. There is no verified generic Rust T size,
so no Rust byte reservation or capacity equivalence is claimed. Existing target
settings, generated Content, and prior allocation adapters remain unchanged.

## Error and panic proof boundary

The new default and list visitor add no catch or rollback. Individual format or
codec providers can still catch: existing Value.SeqDeserializer does, and
StringInPlaceVisitor's byte branch also converts exceptions. Those providers are
unchanged. A dedicated test records the existing Value sequence behavior rather
than claiming universal panic propagation through all providers.

The Rust oracle invokes the actual local serde_core Vec implementation with an
element codec that records ordinary versus in-place dispatch and mutates before
returning an error or panicking. The Kotlin oracle links the complete newly built
production core KLIB and uses a minimal non-catching sequence format driver.
Nine exact output rows compare final values, call order, error outcomes, and
panic outcomes for empty, short, equal, growing, partial-failure, and panic cases.
These are source-backed consumer comparisons, not generated seed verification.

## Validation

- Native Gradle: 91 core tests and 21 root tests passed, zero failures/errors/skips,
  no excluded tests. This covers these two macosArm64 test tasks only, not every
  target, Swift, ABI, derive tests, or serde-json.
- Targeted pinned Rust aarch64-apple-darwin versus Kotlin Native macos_arm64:
  9/9 observations matched with warnings treated as errors. See `validation.json`.
- New tests also cover caller-slot replacement, nullable values, preserved error
  identity, default setter exception propagation, legacy override dispatch,
  retained list/element identity, and the unchanged old list replacement API.
- `gradle-validation.json` records the invocation and counts. `gradle-01.log` has
  trailing whitespace normalized; its raw hash and local raw-copy location
  (`tmp/seed-place-prep-01/gradle-01-raw.log`) preserve the original output.
- Foreign edits were verified unchanged. ABI dump files remain user-owned edits.

Reproduce after the Native Gradle build:
`./verification/seed-place-prep-01/check.sh --out build/seed-place-prep-01/compare-new`.
