// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * A data structure that can be deserialized without borrowing any data from the deserializer.
 */
public interface DeserializeOwned<T> : Deserialize<T>
