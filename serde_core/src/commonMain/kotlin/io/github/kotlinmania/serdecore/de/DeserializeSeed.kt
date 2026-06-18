// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult

/**
 * `DeserializeSeed` is the stateful form of the `Deserialize` interface. If you ever find yourself
 * looking for a way to pass data into a `Deserialize` implementation, this interface is the way to
 * do it.
 */
interface DeserializeSeed<Value> {
    /**
     * Equivalent to the more common `Deserialize.deserialize` method, except with some initial
     * piece of data passed in.
     */
    fun <D> deserialize(deserializer: D): SerdeResult<Value>
        where D : Deserializer
}
