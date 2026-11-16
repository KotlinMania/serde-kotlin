# Map-to-enum variant-key exception boundary

Baseline: `a8f39bf`; the checkout was clean. Earlier checkpoints, particularly
canonical UnitDeserialize payload validation and `b94fab6`'s Rust-backed in-place
characterization resolution, remain separate. The workspace graph still gives
Serde 56 direct dependent repositories versus 30 each for Futures/Rand and 21
for JSON. This step touches no other repository or agent-owned work.

Upstream `tmp/serde/serde_core/src/de/value.rs:1673` matches directly on
`tri!(self.map.next_key_seed(seed))`. A key creates `map_as_enum`; absence returns
the typed invalid-map/expected-enum error. Neither it nor the underlying map key
path catches programming panics. Kotlin's `MapAccessDeserializer.variantSeed`
instead wrapped that operation in `serdeCatching`, converting programming
exceptions to typed errors.

The bounded manual repair composes the existing `nextKeySeed` result with
`SerdeResult.flatMap`, explicitly returning the same typed empty-map error.
The now-unused SerdeException/serdeCatching imports were removed. Canonical
MapAsEnum, UnitDeserialize, seed and result types remain owned by their existing
implementations. No signatures, dependency pins, settings or existing tests changed.

The exact successful full-file rust-kotlin output was hash-verified and reused
for comparison with the unchanged Rust source. This is a manual consumer repair,
not a generator fix, new translator run or generated-file adoption.

Eight actual Rust oracle tests exercise seed/access/key-conversion panics, typed
seed/access errors, empty-map errors, successful payload routing and deferred
payload conversion. Borrowing the map in Rust allows direct inspection that a
failed key seed/conversion leaves the pending value intact. Kotlin uses the same
reference to inspect that state, without claiming Rust ownership equivalence.
The Kotlin suite initially failed precisely the three panic cases; five controls
passed. Typed error identity assertions are Kotlin binding observations, while
Rust checks the error's content. The test source hash is retained from before the
production repair, and no assertion was weakened or updated afterward. The first
full run passed all 503 JVM and 271 Native tests but found four new chain-layout
findings in the empty-map assertion. Only layout and an optional trailing comma
were corrected; first-run test source/log/validation are retained and hashed,
followed by final-source runtime and static checks. A local integrity assertion
initially rejected the optional comma as a token change; comparison was corrected
to account explicitly for optional trailing call commas, without changing tests,
inputs, assertions or production behavior.

Reproduce locally:

```
cargo test --offline --manifest-path verification/map-enum-seed-oracle/Cargo.toml --target-dir build/rust-kotlin-map-enum-seed/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The receipt records final-source tests, static findings attributed against the
baseline, raw local log/XML hashes and translator provenance. Cargo.lock is
locally generated under existing ignore policy, with its hash recorded. Global
static gate failures remain explicit. No remote action or publication occurred.

The nullable-key versus absent-key ambiguity in MapAccess's `K?` API remains:
this repair preserves the existing null/empty-map branch, not full Rust Option
parity. Other exception boundaries elsewhere in Serde, ownership/Drop and borrow
contracts, generic error/trait specializations, Int/usize widths, untested
platforms and downstream integration remain outside this bounded repair.
