# Fresh map decoding exception and insertion boundaries

Baseline: `0ab54d7`; the Serde checkout was clean. The dependency graph records
56 direct Serde consumers versus 30 each for Futures/Rand and 21 for JSON. No
other repository or agent-owned source changed. Prior map-entry, Result, list,
unit-payload and in-place repairs remain separate checkpoints.

Upstream `tmp/serde/serde_core/src/de/impls.rs:1501` defines map_impl: obtain the
container, call next_entry until absence or typed error, and insert each pair.
The HashMap instantiation at line 1565 chooses its own cautious capacity/hasher.
Neither adds a catch around map access or key/value decoding. Kotlin's fresh
mutableMapDeserialize visitor used serdeCatching, masking hint/access/codec
programming exceptions as typed deserialization errors.

The repair handles canonical SerdeResult success/failure explicitly. Key-before-
value order, typed Failure identity, duplicate-key last-value replacement and
EOF success remain. No new collection type or substitute algorithm was added.
The current LinkedHashMap and hint expression are unchanged, as are public
signatures, existing assertions, dependencies, configuration, Result, list and
in-place behavior. Rust's HashMap hashing/iteration/BuildHasher and cautious
capacity are not claimed equivalent to Kotlin's existing container policy.

The successful full-file rust-kotlin output and exact impls.rs hashes were
verified and reused. Generated HashMap MapVisitor lines 6012-6031 provides
comparison evidence for its direct entry/insert loop; the map_impl macro and
HashMap instantiation were independently compared against unchanged Rust. This
is a manual consumer repair, not a generator fix, new translation process or
generated-source adoption. The recorded de/mod.rs failure was not retried.

Ten actual Rust tests invoke HashMap<Key,Value>::deserialize with instrumented
map access and codecs. They cover ordinary/empty data, duplicate keys, hint and
key/value accessor panics, key/value codec panics, and typed key/value failures.
They verify lookups/lengths, exact accessor/hint counts, decoder selection and
unconsumed suffixes, without comparing hash iteration order. Kotlin initially
failed precisely five panic cases; five controls passed. Error identity checks
are Kotlin-specific; Rust checks error content. No Kotlin assertion changed after
the repair; its pre-repair source hash is retained.

The first Rust fixture compile failed before execution because the forwarding
macro defaults to an undeclared 'de lifetime in its explicit 'static Driver.
Supplying the documented `<V: Visitor<'static>>` macro binding fixed only that
fixture declaration. Test inputs/assertions and the actual HashMap call remained
unchanged. Original source and compiler log are retained and hashed. All ten
oracle tests then passed; no initial successful execution is claimed.

Reproduce locally:

```
cargo test --offline --manifest-path verification/map-decode-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-map-decode-boundary/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt records final-source results, baseline static attribution, source and
local log/XML hashes. Cargo.lock stays ignored under existing policy, with its
local hash recorded. Historical failed receipts remain untouched. No remote CI,
push, PR, merge, tag, release, publication or configuration action occurred.

Remaining limits include generic hash/equality/ordering and BuildHasher, cautious
allocation/OOM, Rust Drop/borrow/ownership versus Kotlin references/GC, nullable-key
ambiguity, other Serde exception boundaries, generic error/trait/usize binding,
untested key/value types, platforms and downstream integration. This fixture's
i32-based keys and values do not establish general collection parity.
