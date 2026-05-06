// port-lint: source serde_core/src/private/seed.rs
package io.github.kotlinmania.serde.core.`private`

/*
 * Copyright (c) 2026 Sydney Renee <sydney@solace.ofharmony.ai>
 * and The Solace Project.
 *
 * Licensed under either Apache-2.0 or MIT at your option; see LICENSE-APACHE
 * and LICENSE-MIT.
 */

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
