# Direct String and SizeHint adoption

The approved integration installs two complete generated files without changing their raw bodies:

| Rust input | Kotlin destination under serde_core/src/common/commonMain/kotlin/io/github/kotlinmania/serdecore/priv/ | Raw SHA-256 |
| --- | --- | --- |
| tmp/serde/serde/src/core/private/string.rs | generated/string/String.kt | 6303a9cd9c96dfac8099b3feddab6b3ee76ff36ccbff9eb143dd7404e038876e |
| tmp/serde/serde/src/core/private/size_hint.rs | generated/sizehint/SizeHint.kt | e46f3a76ac6712e5a24e977ba07e247ef63a51e9c6d736707c9ca56d221f7106 |

String.kt retains `fromUtf8Lossy(ByteArray): String`, delegating to the generated Cow function and materializing text. The generated function preserves Borrowed/Owned and the selected storage/range. `fromUtf8LossyNoAlloc` remains byte-for-byte unchanged, including its three-replacement-character fallback. The three existing kernel sources from commit 300f797 are read in place; none are copied or edited. The source filter is retained explicitly at JVM/Android compilation because Android otherwise flattens nested source directories.

SizeHint.kt delegates exact iterator bounds and the unsigned explicit-size overload to the generated implementation. NumTraits 0.2.15 is an implementation dependency. All five concrete allocation sites are migrated: ContentVisitor sequence/map in serde_core/priv/ContentVisitor.kt, and ContentVisitor sequence/map plus TaggedContentVisitor map in the root src/common/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt. Int hints are checked nonnegative before widening; allocation capacities are checked before narrowing back to Int.

## Exact layout and compatibility

Subsequent repair: [Content allocation portability](../content-allocation-repair-01/README.md) restores capture on unmeasured targets through ordinary Kotlin list growth. The exact-size helpers still reject absent layout evidence. The runtime rejection described below records the original integration and its subsequently repaired regression.

ContentLayout.kt carries the measured sizes of canonical serde_core 1.0.228 `__private228::Content<'static>` (32 bytes) and the complete `(Content<'static>, Content<'static>)` tuple (64 bytes). The tuple was measured independently, not computed by addition. The measurement is for Rust revision 7189d39d9ce936881b8132c6addeaf3e295cd455, std + alloc, aarch64-apple-darwin only. The Native implementation admits only macOS ARM64; every other target explicitly rejects missing evidence. Existing targets are retained. This is a runtime limitation on allocation paths for unmeasured targets, not a claim that their layouts equal Apple's.

The legacy public `cautious<Element>(Int?)` signature remains callable but now rejects missing Rust layout evidence, including open/unknown types. It no longer guesses eight bytes. External users must migrate to `cautious(ULong?, rustElementSizeBytes: ULong)` with the exact source Element/profile size. `sizeOf<Element>()` also rejects missing evidence. ULong iterator bounds from commit 79992ae remain unsigned. A scalar supplied by an external caller is an obligation, not a runtime proof of that caller's Rust type. The generated 64-bit profile and zero-sized branch are unchanged.

Cow requires the caller to keep borrowed bytes immutable and uses Int indices. The added generated/kernel types and capacity helpers expand the public API; the ByteArray text helper signatures remain unchanged. No binary or Swift-export compatibility claim is made. No new Java library/runtime dependency was added.

## Validation

- Kotlin/Native 2.4.21 macosArm64Test: serde_core 74 tests and root Serde 15 tests, all passing. Ten new tests cover ownership/UTF-8, unsigned/zero-sized bounds, missing-layout rejection, exact target selection and each of the five migrated allocation callers.
- External consumers link the actual complete production Gradle serde-core KLIB: 608 Rust String rows, 608 text-compatibility checks, 608 unchanged noalloc checks, 138 Rust cautious rows and 30 iterator rows. Missing witness compilation fails as expected.
- Existing updateLegacyAbi task passes for every configured serde_core compilation (Android/JVM, JS/Wasm and Native targets). This verifies compilation/API generation, not runtime behavior on other hosts. Only this task's compiler-generated API declarations are staged; unrelated stale or user-owned API changes remain in the working files.
- Native tests: `./gradlew --offline --no-daemon -Pproject.repositories.mavenLocal=true -Dmaven.repo.local=/Users/sydney/.m2/repository :serde-core:macosArm64Test :macosArm64Test`.
- Oracle reproduction: rust-kotlin/verification/production-adoption-01/run.py, after its recorded Gradle dependency-input tasks. Canonical Rust layout reproduction: `python3 ../rust-kotlin/verification/element-layout-01/run.py --out ../rust-kotlin/tmp/element-layout-01/new-run`.

layout-facts.json and source-evidence.json pin the compiler, canonical upstream inputs and kernel implementations. Full command receipts, including failed attempts, are retained in rust-kotlin/verification/production-adoption-01. No source snapshot, target removal, kernel edit, FFI change, remote action or publication was performed. The frozen batch gains two adoptions here, not two new verified inputs.
