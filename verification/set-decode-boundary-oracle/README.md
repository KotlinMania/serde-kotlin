# Fresh set decoding exception and deduplication boundaries

Baseline: `a34f78d`; the owning checkout was clean. The dependency graph records
56 direct Serde consumers versus 30 each for Futures/Rand and 21 for JSON. No
other checkout or agent-owned source changed. Prior map, list, Result, enum and
in-place repairs remain separate checkpoints.

Upstream `tmp/serde/serde_core/src/de/impls.rs:1014` defines seq_impl's visitor:
create the collection, deserialize the next element, insert it, and stop on end
or typed error. The HashSet instantiation at line 1109 chooses its hasher/capacity.
Neither adds a catch around sequence access or element decoding. Kotlin's fresh
mutableSetDeserialize visitor used serdeCatching, converting programming
exceptions from hints/access/codecs into typed failures.

The repair uses the existing canonical SerdeResult and SeqElement cases. It
retains successful insertion, duplicate-element semantics, typed Failure identity
and EOF success. LinkedHashSet and its capacity expression remain unchanged;
Rust HashSet hashing/iteration/BuildHasher and generic cautious capacity are not
claimed equivalent. No public signature, dependency, configuration, existing
assertion, in-place code or other Impls.kt visitor changed.

The exact impls.rs and existing successful full-file rust-kotlin output were
hash-verified and reused. Generated HashSet SeqVisitor lines 3619-3638 show a
direct result/insert loop. seq_impl and HashSet's instantiation were independently
reviewed against unchanged Rust; generated nullable scaffolding is not substituted
for canonical SeqElement. This is a manual consumer repair, not a generator fix,
new translation process or generated-source adoption. The recorded de/mod.rs
failure was not retried.

Nine actual Rust tests call HashSet<Item>::deserialize. Established list-fixture
instrumentation is reused only to provide inputs and observe calls, with the
actual Rust collection changed to HashSet. Set checks use membership and size,
not iteration order. The cases cover ordinary/empty input, duplicate values,
hint/access panics, first/second codec panics, and first/second typed errors.
Duplicates are all decoded before deduplication; suffixes are untouched after
failure. Kotlin initially failed four panic cases and passed five controls.
Typed-error object identity is a Kotlin-specific observation; Rust checks content.

After the red run, only blank-line spacing around the new final test was tidied
before the production change. The red-run Kotlin source and XML are retained,
and whitespace-normalized comparison verifies unchanged tokens/assertions. The
pre-production final test hash is also recorded. No existing test or assertion
was weakened or edited. Prior map-oracle compiler failure/correction evidence
remains untouched and is linked by hashes; this Rust set oracle passed all nine
cases on its first execution.

Reproduce locally:

```
cargo test --offline --manifest-path verification/set-decode-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-set-decode-boundary/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt records final-source runtime/static results and local log/XML hashes.
Cargo.lock stays ignored under existing policy, with its local hash recorded.
Historical failed receipts remain unchanged. No remote CI, push, PR, merge, tag,
release, publication or configuration action occurred.

Remaining limits include generic equality/hash/BuildHasher, hash iteration,
cautious allocation/OOM, Rust Drop/borrow/ownership versus Kotlin references/GC,
nullable-map-key ambiguity, other exception boundaries, generic error/trait/usize
binding, other element types/platforms and downstream integration. Custom hash
failure behavior is not covered by these i32-based codec fixtures.
