// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * `DeserializeSeed` is the stateful form of the `Deserialize` interface. If you ever find yourself
 * looking for a way to pass data into a `Deserialize` implementation, this interface is the way to
 * do it.
 */
public interface DeserializeSeed<Value> {
    /**
     * Equivalent to the more common `Deserialize.deserialize` method, except with some initial
     * piece of data passed in.
     */
    public fun <D> deserialize(deserializer: D): Result<Value>
        where D : Deserializer
}
