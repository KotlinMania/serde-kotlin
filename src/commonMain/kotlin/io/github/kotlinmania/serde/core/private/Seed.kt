// port-lint: source serde_core/src/private/seed.rs
package io.github.kotlinmania.serde.core.`private`

import io.github.kotlinmania.serde.core.de.Deserialize
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer

/**
 * A `DeserializeSeed` helper for implementing `deserializeInPlace` Visitors.
 *
 * Wraps a mutable reference and calls `deserializeInPlace` on it.
 */
public class InPlaceSeed<T>(
    public var value: T,
) : DeserializeSeed<Unit> where T : Deserialize<T> {
    override fun <D> deserialize(deserializer: D): Result<Unit>
        where D : Deserializer =
        value.deserializeInPlace(deserializer) { value = it }
}
