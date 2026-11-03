// port-lint: source ser/mod.rs
package io.github.kotlinmania.serdecore.ser

internal fun iteratorLenHint(iter: Iterable<*>): Int? = if (iter is Collection<*>) iter.size else null

internal fun iteratorLenHint(iter: Sequence<*>): Int? = null
