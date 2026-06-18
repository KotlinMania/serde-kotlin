// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

internal data class WithDecimalPoint(
    private val value: Double,
) {
    override fun toString(): String {
        val rendered = value.toString()
        return if (value.isFinite() && "." !in rendered) "$rendered.0" else rendered
    }
}
