# Serde SeqDeserializer permanent-exhaustion parity

## Graph priority and ownership

The current workspace_dep_graph.json was read directly: unique inbound caller
repos are Serde 56, Tokio 33, Futures 30 and Rand 30. Serde and Rand were clean
at their earlier checkpoints; Tokio ownership remains adjacent work. Serde was
selected for higher graph impact and a bounded source omission, starting clean
at 7c6c87a (with earlier 26fb96e dependency checkpoint separate). The local
AGENTS.md and SWIFT.md are byte-identical to the already-read Futures copies;
existing user/parent remote and broad-platform holds supersede their release
runbook. No foreign dirty work is included or attributed to this repair.

## Translator and exact source

The existing native rust-kotlin CLI successfully translated the unchanged
original tmp/serde/serde_core/src/de/value.rs, using this repository's
gradle.properties and original serde_core source root, into
build/rust-kotlin-seq-fuse/ValueReview.kt. No renamed/extracted input was used.
The full scratch output has unresolved associated types/imports and is not
claimed runnable. The implementation is manual exact-source adaptation informed
by that translator output and direct upstream comparison, not a generator fix.

Upstream SeqDeserializer::new stores iter.fuse(). Once next_element_seed
observes None, later accesses and end() cannot resume the underlying iterator.
The fused iterator then has exact zero bounds, so size_hint returns Some(0).
end() still counts actual remaining items before exhaustion and reports total
length plus the visitor's consumed count in the existing invalid-length error.

Kotlin previously retained the raw Iterator without permanent exhaustion state.
Its nextElementSeed could expose a later value after None, and its deserializeAny
end check could fabricate an excess-length error after a visitor had correctly
read EOF. The repair adds only private exhaustion state in SeqDeserializer,
representing the source's owned iter::Fuse behavior at the existing Kotlin
Iterator boundary. No generic Fuse class, duplicate canonical owner, new public
Option representation or public signature is introduced. Existing SeqElement,
SerdeResult and IntoDeserializer owners are reused. end() does not touch an
already exhausted iterator; sizeHint returns zero after EOF. Count/error logic
and the source's explanatory error-count comment are preserved.

Five new Kotlin cases and five actual serde_core Rust oracle cases cover
repeated next after EOF, end after EOF, a visitor reading EOF before automatic
end checking, exact zero hint after EOF, and unchanged leftover-length errors.
The first Kotlin fixture build stopped at a redundant Unit-expression warning;
that expression was removed without changing assertions or warning policy,
and the compile receipt is preserved. The old production then executes five
cases with four runtime failures; the leftover-length control passes. Rust
executes actual SeqDeserializer and std Fuse, not a reimplemented algorithm.
Scripted producers can resume after their first end, allowing the adapter's
own fusion to be observed. No existing assertion was changed.

```sh
CARGO_TARGET_DIR=build/rust-kotlin-seq-fuse/cargo-target cargo test --manifest-path verification/seq-deserializer-fuse-oracle/Cargo.toml --offline --locked
```

## Remaining limits

Non-exhausted Kotlin size hints still depend on the existing len metadata;
arbitrary Rust iterator bounds, including the canonical IteratorWithSizeHint
path, are not newly supported. Public SeqAccess hints/counts remain Int rather
than usize. Rust end(self) ownership, clone/borrow/move/Drop and error type
relationships are not fully represented; Kotlin permits receiver reuse that
Rust ownership prevents. Existing serdeCatching exception conversion is not
changed or claimed Rust panic equivalence. Other deserializers, including the
separate MapDeserializer iterator state, are outside this repair. No Unicode,
mutable borrowing, nullable map-key API, generic resolver or representation
redesign is included. No dependency pins, repositories, runtime/build/publication
settings, UI sessions or another owner's sources changed. Downstream source/
binary, Swift/other platforms and fresh publication metadata remain unverified.
Prior Futures failed-sink/destruction/fairness/thread-scheduling limits and
original/corrected oracle receipts remain intact.

## Final-source local checks

All 417 JVM tests and 185 macOS ARM tests pass, including the five new cases
on each target. All five actual upstream Rust oracle cases pass. These are
actual final-source results, not the older partial Serde receipts. The fixture
Iterator.next was corrected to throw NoSuchElementException when unavailable;
that guard changes no valid iterator observation or existing assertion. Runtime
and static checks were rerun on that actual final test source.

The complete Gradle command remains unsuccessful because global static gates
fail: 50 Detekt findings in untouched code, and current ktlint Check reports
contain common-main 2591, common-test 242, JVM-test 3726, macOS ARM-test 6 and
Kotlin-script 5 findings (6570 total reported findings). Other checked groups
have zero. All in-repository locations map to unchanged baseline 7c6c87a lines;
none is on an authored line. The mapping includes unchanged EOF diagnostics;
byte-identical Check.kt/Internals.kt were not edited. Historical Format reports
are excluded from current-check receipts. No static settings, assertions or
warning policy were weakened to obtain this result.

Final hashes/validation reports and CLI/initial-fixture-compile/runtime-red/
oracle/Gradle logs are retained under build/rust-kotlin-seq-fuse. Public APIs,
existing callers, dependency pins and the earlier clean Futures 09c4126
checkpoint are unchanged. No prior dirty work was included. All source-count,
size-hint, ownership, exception, downstream and platform limits above remain.
