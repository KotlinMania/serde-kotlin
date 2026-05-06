// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * Used in error messages.
 *
 * - expected `a`
 * - expected `a` or `b`
 * - expected one of `a`, `b`, `c`
 *
 * The list of names must not be empty.
 */
internal data class OneOf(
    private val names: List<String>,
) {
    override fun toString(): String =
        when (names.size) {
            0 -> throw IllegalStateException("expected names must not be empty")
            1 -> "`${names[0]}`"
            2 -> "`${names[0]}` or `${names[1]}`"
            else -> names.joinToString(prefix = "one of ") { "`$it`" }
        }
}
