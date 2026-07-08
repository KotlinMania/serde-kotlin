// port-lint: source de/mod.rs
package io.github.kotlinmania.serdecore.de

/**
 * Converts an existing value into a `Deserializer` from which other values can be deserialized.
 */
interface IntoDeserializer {
    /**
     * Convert this value into a deserializer.
     */
    fun intoDeserializer(): Deserializer
}
