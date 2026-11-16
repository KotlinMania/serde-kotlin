# Preserve the default MapAccess entry-composition contract

Baseline: `0ba0d3f`; the checkout was clean. The workspace graph records 56 direct
Serde consumers versus 30 each for Futures/Rand and 21 for JSON. Futures and Rand
were checked read-only and were clean at `ceba470` and `386aad6`. No other checkout
or agent-owned source was changed. Prior Serde repairs remain separate checkpoints.

Upstream `tmp/serde/serde_core/src/de/mod.rs:1873` defines the default
`MapAccess::next_entry_seed`: call the key seed, return absence if the key is absent,
otherwise call the value seed and return the pair. Typed errors short-circuit;
programming panics propagate. The Kotlin default used `serdeCatching` around this
composition, converting key/value accessor and seed exceptions into typed errors.

This repair uses canonical `SerdeResult.flatMap`/`map`, maintaining key-before-value
order and the existing absent-key branch. No public signature, canonical type,
comment, dependency pin or existing assertion changed. The already-repaired
MapDeserializer override is untouched. Its behavior is not used as a substitute
for testing the inherited default: both oracles use a wrapper MapAccess that
implements only key/value access and inherits the actual default entry method.

The existing rust-kotlin release executable was attempted once on the original
`de/mod.rs`, with output confined to owning-repository scratch. It exited 2 with
`rust-kotlin: expected curly braces`, produced no output and did not time out.
There was no retry, extracted/renamed substitute source or guard bypass. The
receipt records exact command and source/CLI hashes. Consequently there is **no
usable generated diff for this input**; this is a manual repair from direct
upstream comparison and actual Rust oracle evidence, not a generator fix or
claimed generated-source adoption.

Nine actual Rust tests cover four panic boundaries, key/value typed errors,
empty input, a normal pair and a present pair with a decoded null value. They
verify key/value accessor counts and pending/consumed value state. Kotlin's
matching suite initially had four panic failures and five passing controls.
Typed-error object identity is additionally checked as a Kotlin-specific binding
observation; Rust checks error content. The new test source hash is retained
from before the production edit. No assertion was weakened to obtain green.

Reproduce locally:

```
cargo test --offline --manifest-path verification/map-entry-default-oracle/Cargo.toml --target-dir build/rust-kotlin-map-entry-default/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The final-source receipt includes runtime totals, baseline static attribution and
local raw log/XML hashes. Cargo.lock stays under existing ignore policy, with its
hash recorded. Historical failed receipts remain unchanged. No remote CI, push,
PR, merge, tag, release, publication or configuration action occurred.

The nullable-key/absence ambiguity of MapAccess's `K?` representation remains;
this repair preserves that branch and does not claim full Rust Option parity.
Rust borrow/consumption/Drop semantics differ from Kotlin references/GC. Broader
Serde exception boundaries, generic error/trait specialization, Int/usize widths,
untested platforms and downstream integration remain outside this bounded step.
