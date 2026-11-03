# IgnoredAny odd-length drain validation

Test-only follow-up to 0576286. No production, dependency, generator, configuration or remote changes. Existing nine Rust and Kotlin test bodies remain byte-for-byte present, with all assertions unchanged. Only the sequence fixtures gained a configurable length (default still two) and independent access/delivery counters, plus four new groups.

New groups cover zero values (one access, zero delivered), one value (two accesses, one delivered), and three values (four accesses, three delivered). A fourth group covers typed errors and panics before the last value or at exhaustion for lengths one and three (eight cases). Each case asserts outcome, independent producer call/delivery state, and exact scalar-discard/access trace. Thus the eleven new scenarios expose a second read after the first absent element even when the loop body discards all values. The actual Rust IgnoredAny visitor is invoked, not a replacement algorithm.

All original 17 scenarios remain, giving 13 groups / 28 scenarios total. Earlier even-length, empty-body consumer coverage did not distinguish the generated double-evaluation case; this addition closes that consumer coverage gap. It is not execution or validation of generated Kotlin.

The generator finding remains scoped to the recorded CLI SHA-256 027d237f2844e0291cf2b9e06ab4bd73e828abccce02db65db992fb3478396c4 and emitted output hash e65765cbbafe49b80a04e8f37e353734189de345eda63c7c62dda5411a6a06f6. The current generator has a single-evaluation lowering branch and a related direct-call test. A current-build macro-backed regression is unverified and may be stale-binary behavior. No generator rebuild, edit, execution, further investigation or Iris contact occurred in this step.

Before edits, all 37 original receipt hashes were verified and their files copied under `build/rust-kotlin-ignored-any-odd-drain/prior-0576286/`, along with the original receipt. `prior-evidence.json` records the immutable snapshot paths and hashes. The original committed receipt is unchanged; subsequent source and standard test-output changes are validated against the snapshots rather than pretending the historical paths still contain the earlier revision.

Commands:

- `cargo test --offline --manifest-path verification/ignored-any-boundary-oracle/Cargo.toml --target-dir build/rust-kotlin-ignored-any-odd-drain/rust-target`
- `./gradlew jvmTest --tests '*IgnoredAnyBoundaryParityTest*' macosArm64Test --tests '*IgnoredAnyBoundaryParityTest*' detekt ktlintCheck --continue --console=plain`

Kotlin checks are explicitly scoped to this suite; they are not a new full-suite result. Existing static failures and platform/ownership/downstream limits remain. Final evidence is recorded separately in receipt.json.
