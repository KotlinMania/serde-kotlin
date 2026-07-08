// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serdecore.de

internal data class WithDecimalPoint(
    private val value: Double,
) {
    override fun hashCode(): Int {
        val bits = value.toBits()
        return (bits xor (bits ushr 32)).toInt()
    }

    override fun toString(): String {
        val rendered = value.toString()
        return if (value.isFinite() && "." !in rendered) "$rendered.0" else rendered
    }
}
