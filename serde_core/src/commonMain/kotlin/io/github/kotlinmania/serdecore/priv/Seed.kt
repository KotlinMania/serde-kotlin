// port-lint: source private/seed.rs
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
internal class InPlaceSeed<T>(
    var value: T,
) : DeserializeSeed<Unit> where T : Deserialize<T> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Unit>
        where D : Deserializer =
        value.deserializeInPlace(deserializer) { value = it }
}

/**
 * Creates an [InPlaceSeed] wrapping a mutable reference for in-place
 * deserialization. Use this instead of the constructor.
 */
internal fun <T : Deserialize<T>> inPlaceSeed(value: T): InPlaceSeed<T> = InPlaceSeed(value)
