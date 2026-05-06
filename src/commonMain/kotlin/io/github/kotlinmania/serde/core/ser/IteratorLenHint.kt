// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

internal fun iteratorLenHint(iter: Iterable<*>): Int? =
    if (iter is Collection<*>) iter.size else null
