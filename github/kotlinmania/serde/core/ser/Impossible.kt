// port-lint: source serde_core/src/ser/impossible.rs
package io.github.kotlinmania.serde.core.ser

/**
 * This module contains `Impossible` serializer and its implementations.
 */

/**
 * Helper type for implementing a `Serializer` that does not support serializing one of the
 * compound types.
 *
 * This type cannot be instantiated, but implements every one of the interfaces corresponding to
 * the `Serializer` compound types: `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`,
 * `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, and `SerializeStructVariant`.
 *
 * ```kotlin
 * class MySerializer : Serializer<Unit, Error> {
 *     override fun serializeSeq(len: Int?): Result<SerializeSeq<Unit, Error>> {
 *         // Given Impossible cannot be instantiated, the only thing we can do here is to
 *         // return an error.
 *         return Result.failure(...)
 *     }
 *
 *     // other Serializer methods
 * }
 * ```
 */
public class Impossible<Ok, E> private constructor(
    private val void: Void,
) :
    SerializeSeq<Ok, E>,
    SerializeTuple<Ok, E>,
    SerializeTupleStruct<Ok, E>,
    SerializeTupleVariant<Ok, E>,
    SerializeMap<Ok, E>,
    SerializeStruct<Ok, E>,
    SerializeStructVariant<Ok, E>
    where E : Error {
    override fun <T> serializeElement(value: T): Result<Unit>
        where T : Serialize {
        return absurd(void)
    }

    override fun end(): Result<Ok> =
        absurd(void)

    override fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize {
        return absurd(void)
    }

    override fun <T> serializeKey(key: T): Result<Unit>
        where T : Serialize {
        return absurd(void)
    }

    override fun <T> serializeValue(value: T): Result<Unit>
        where T : Serialize {
        return absurd(void)
    }

    override fun <T> serializeField(key: String, value: T): Result<Unit>
        where T : Serialize {
        return absurd(void)
    }

    override fun skipField(key: String): Result<Unit> {
        return Result.success(Unit)
    }
}

private enum class Void

private fun absurd(void: Void): Nothing {
    throw AssertionError("uninhabited Void value reached: $void")
}
