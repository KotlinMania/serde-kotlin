# Default collectSeq construction, error and panic order

Baseline: `2fdf0e5`; the Serde checkout was clean. This graph-prioritized step
changes only the two default Serializer.collectSeq overload bodies. Prior network,
collection and in-place repairs remain separate. No public signature, existing
assertion, dependency, configuration, allocation policy or other source changed.

Actual serde_core/src/ser/mod.rs:1296 constructs the iterator, queries its hint,
opens the serializer, serializes yielded values, then calls end. Typed errors
short-circuit and programming panics escape. Kotlin opened the backend before
calling iterator(), and serdeCatching masked programming exceptions. The repair
constructs one iterator first and uses canonical SerdeResult branches; end is
called only after normal exhaustion. Length-hint adapters stay unchanged.

One bounded CLI attempt on original ser/mod.rs (distinct from de/mod.rs) exited 2
with `expected curly braces`, no output and no timeout. No retry, extraction,
rename or guard bypass followed. There is no usable generated diff for this input;
this is a direct upstream/manual repair with actual Rust oracle evidence.

The Rust fixture does not override collect_seq: it invokes the real Serializer
trait default. Its instrumented serializer handles seq/i32; unsupported mandatory
fixture paths panic and unused compounds use canonical Impossible. Kotlin's probe
delegates unused requirements to existing FormatterSerializer but explicitly calls
super<Serializer>.collectSeq for both overloads, ensuring the production default
runs with the probe receiver. Complete begin/element/value/end traces enforce this;
no collect algorithm is copied into either fixture.

Ten groups cover 21 scenarios: known/unknown lengths; success/empty; factory,
known-hint, begin, next, element and end panics; typed begin/element/end errors;
no suffix reads or end after early failure. Rust passed all groups before edits.
Kotlin's ten red groups fail on opening/construction order or swallowed exceptions.
Groups fail fast on the first form in that red run, so it is not evidence that
all unknown-form baseline scenarios executed. Final passing groups run both forms
and the extra known-hint case. New-test constructor layout only was formatted
before production edits; original red source and exact replacements are retained.
Assertions/inputs remain unchanged, with a fixed pre-production test hash.

Hint and iterator representation limits are explicit, not hidden by the oracle:
Rust calls Iterator::size_hint once after construction for both forms. Kotlin's
Collection adapter calls size once, while its Sequence adapter has no observable
hint callback (zero fixture hint calls; the helper still evaluates to null).
Tests assert these respective counts. The trace measures logical construction,
yields, EOF and serializer events, not equivalence of Rust next versus Kotlin
hasNext/next call counts. Arbitrary iterator size-hint, min/max, dynamic metadata,
consuming ownership/Drop and usize equivalence remain unestablished. The existing
adapters were preserved; no replacement SizedIterator or fabricated hint was added.
Kotlin typed-error identity is an additional binding-specific observation.

Run locally:

```
cargo test --offline --manifest-path verification/collect-seq-order-oracle/Cargo.toml --target-dir build/rust-kotlin-collect-seq-order/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt records final-source results, static attribution and raw local source,
log/XML hashes. Existing static gates remain explicitly failing. Cargo.lock stays
ignored under existing policy with its local hash recorded. Prior failed receipts
and compile history remain intact. Scoped sockets, outer FromStringVisitor,
generic hashing/allocation, nullable-key, ownership/platform and downstream limits
remain unchanged. No remote CI, push, PR, merge, tag, release, publication or
configuration action occurred.
