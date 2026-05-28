// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * Converts an existing value into a `Deserializer` from which other values can be deserialized.
 */
interface IntoDeserializer {
    /**
     * Convert this value into a deserializer.
     */
    fun intoDeserializer(): Deserializer
}
