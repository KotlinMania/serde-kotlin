// port-lint: source serde_core/src/lib.rs
package io.github.kotlinmania.serde.core

/**
 * Serde is a framework for ***ser***ializing and ***de***serializing Rust data structures
 * efficiently and generically.
 *
 * The Serde core crate contains Serde's trait definitions with **no support for `derive()`**.
 *
 * In crates that derive an implementation of `Serialize` or `Deserialize`, you must depend on the
 * `serde` crate, not the Serde core crate.
 *
 * In crates that handwrite implementations of Serde traits, or only use them as trait bounds,
 * depending on the Serde core crate is permitted. But `serde` re-exports all of these traits and can be
 * used for this use case too. If in doubt, disregard `serde_core` and always use `serde`.
 *
 * Crates that depend on the Serde core crate instead of `serde` are able to compile in parallel
 * with the derive crate even when `serde`'s "derive" feature is turned on, as shown in the following
 * build timings.
 */
internal object Lib {
    interface Debug

    interface Display
}
