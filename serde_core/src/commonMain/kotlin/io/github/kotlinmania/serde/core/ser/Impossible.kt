// port-lint: source serde_core/src/ser/impossible.rs
package io.github.kotlinmania.serde.core.ser

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeResult

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
 * class MySerializer : Serializer<Unit, SerdeError> {
 *     override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit, SerdeError>> {
 *         // Given Impossible cannot be instantiated, the only thing we can do here is to
 *         // return an error.
 *         return SerdeResult.failure(...)
 *     }
 *
 *     // other Serializer methods
 * }
 * ```
 */
class Impossible<Ok> private constructor(
    private val void: Void,
) : SerializeSeq<Ok>,
    SerializeTuple<Ok>,
    SerializeTupleStruct<Ok>,
    SerializeTupleVariant<Ok>,
    SerializeMap<Ok>,
    SerializeStruct<Ok>,
    SerializeStructVariant<Ok>
    {
    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize = absurd(void)

    override fun end(): SerdeResult<Ok> = absurd(void)

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize = absurd(void)

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize = absurd(void)

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize = absurd(void)

    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = absurd(void)

    override fun skipField(key: String): SerdeResult<Unit> = SerdeResult.success(Unit)
}

private enum class Void

private fun absurd(void: Void): Nothing = throw AssertionError("uninhabited Void value reached: $void")
