# rust-kotlin: while-let scrutinee duplicated in generated loop

Status: reproduced structurally in fresh CLI output; not fixed. Generator files belong to Iris and were left untouched. This is a local evidence record, not a claim that a Hermes card was created.

Original input: `tmp/serde/serde_core/src/de/ignored_any.rs`, visit_seq. Exact CLI invocation, source/CLI/output hashes, exit 0 and five-item output message are retained in `build/rust-kotlin-ignored-any-boundary/translator-attempt.json`.

Rust:

```rust
while let Some(IgnoredAny) = tri!(seq.next_element()) {
    // Gobble
}
```

Generated fragment, `IgnoredAnyReview.kt:233-236`:

```kotlin
while (tri(seq, nextElement) != null) {
    val IgnoredAny = tri(seq, nextElement)
}
```

The scrutinee appears twice. Its iterator/access side effects must happen once per iteration, including the terminal absent element. The generated text has unresolved generic/helper scaffolding, so this record does not claim compiled execution or an observed runtime skip in the emitted file. The duplicate evaluation is directly visible in the source-to-output comparison. The separate Kotlin consumer regression asserts exact single-read traces and never adopts this fragment.

Proposed bounded generator fix: lower a while-let using a fresh temporary for the scrutinee once at the start of each iteration; test the pattern and bind its variables from that same temporary; terminate on a nonmatch. Keep error propagation at the original evaluation point and preserve the body's ordering. Do not regenerate the scrutinee expression for bindings or add project-wide resolution/helpers.

Regression cases should cover side-effecting call counts and bound values for Some/None; an empty sequence; a final None after multiple values; an Err before/after a value; and a panic at the access point. Assert both one evaluation per iteration and unchanged return/error/panic order. Extend the existing single-file lowering tests with a minimal supported source; no cross-file semantic merger is needed.
