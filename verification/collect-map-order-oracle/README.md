# Default collectMap and serializeEntry order

Baseline: `0ddf2b1`; the Serde checkout was clean. This bounded path changes the
two default collectMap overload bodies and default SerializeMap.serializeEntry,
plus unused imports. Prior collectSeq, network, decode and in-place repairs remain
separate. No signature, existing assertion, dependency or configuration changed.

Actual ser/mod.rs:1333 creates the iterator, queries its hint, opens the map
backend, serializes each entry, then ends on normal exhaustion. The inherited
serialize_entry at line 1855 calls the key before the value, short-circuiting on
key error. Neither catches programming panics. Kotlin opened before iterator
construction and had catches in both collector and inherited entry. The repair
uses canonical SerdeResult branches/flatMap, one iterator, direct end results,
key-before-value order and unchanged length-hint adapters.

The recorded parser failure for this exact original ser/mod.rs input was hash-
verified and reused. No CLI retry, renamed/extracted input, bypass or generated
diff is claimed. This is direct upstream/manual work with actual Rust evidence.

The Rust Probe overrides neither collect_map nor serialize_entry: both real
trait defaults execute. Unused mandatory fixture paths panic and unused compound
types use canonical Impossible. Kotlin explicitly calls default super collectMap
for both overloads and its compound implements only key/value/end, inheriting the
real default entry. Required unused serializer members delegate to the existing
FormatterSerializer. Complete scalar/protocol traces and typed error checks fail
if that delegate accidentally replaces the tested path. Neither fixture copies
the collector or entry algorithm.

Ten groups cover 25 final scenarios: known/unknown input forms; success/empty;
factory, known-hint, begin, next, key/value and end panics; typed begin/key/value/
end errors; and suffix/value/end suppression. Rust passed every group before the
production edit. Kotlin's ten red groups fail on order or exception behavior;
fail-fast groups do not establish complete baseline execution of later forms or
branches. Final passing loops execute both forms and both key/value cases, plus
the extra known-hint scenario. Original red source/XML and exact formatting
replacements are retained. Only trace-list/constraint layout changed before
production; assertions/inputs remain fixed with a pre-production final hash.

Hint representation is intentionally explicit: Rust size_hint callbacks after
construction are one for known and unknown iterators; Kotlin collection-size
callbacks are one and sequence callbacks are zero. The sequence helper still
evaluates to null; it has no external hint callback. Tests assert these respective
counts and do not claim full metadata parity. Traces cover logical yields/EOF,
not total Rust next versus Kotlin hasNext/next polling equivalence. Dynamic/min-
max hints, consuming state/iterator ownership, Drop/GC and usize remain limited.
Kotlin error-object identity is an additional binding-specific check.

Run locally:

```
cargo test --offline --manifest-path verification/collect-map-order-oracle/Cargo.toml --target-dir build/rust-kotlin-collect-map-order/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt includes final-source runtime/static results and local source/log/XML
hashes. Global static gate failures remain explicit. Cargo.lock remains ignored
under existing policy with its local hash recorded. Prior failure/compile history,
scoped-socket/outer-boundary/ownership/platform limits and all container/allocation
policies remain intact. Other serializer formats, generic types and downstream
integration are unverified. No remote CI, push, PR, merge, tag, release, publication
or configuration action occurred.
