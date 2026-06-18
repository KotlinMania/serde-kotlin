// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serdecore.de

/**
 * A data structure that can be deserialized without borrowing any data from the deserializer.
 */
interface DeserializeOwned<T> : Deserialize<T>
