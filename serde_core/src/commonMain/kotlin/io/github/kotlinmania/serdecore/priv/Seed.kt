// port-lint: source serde_core/src/private/seed.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer

/**
 * A `DeserializeSeed` helper for implementing `deserializeInPlace` Visitors.
 *
 * Wraps a mutable reference and calls `deserializeInPlace` on it.
 */
class InPlaceSeed<T>(
    var value: T,
) : DeserializeSeed<Unit> where T : Deserialize<T> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Unit>
        where D : Deserializer =
        value.deserializeInPlace(deserializer) { value = it }
}
