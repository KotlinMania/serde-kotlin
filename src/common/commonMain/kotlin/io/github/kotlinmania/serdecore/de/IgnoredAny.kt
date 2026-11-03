// port-lint: source de/ignored_any.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeResult

/**
 * An efficient way of discarding data from a deserializer.
 *
 * Think of this like a JSON value in that it can be deserialized from any type, except that it does
 * not store any information about the data that gets deserialized.
 *
 * ```kotlin
 * import io.github.kotlinmania.serde.serdeCatching
 * import io.github.kotlinmania.serdecore.de.Deserialize
 * import io.github.kotlinmania.serdecore.de.DeserializeSeed
 * import io.github.kotlinmania.serdecore.de.Deserializer
 * import io.github.kotlinmania.serdecore.de.IgnoredAny
 * import io.github.kotlinmania.serdecore.de.SeqAccess
 * import io.github.kotlinmania.serdecore.de.SeqElement
 * import io.github.kotlinmania.serdecore.de.unwrapOrElse
 * import io.github.kotlinmania.serdecore.de.Visitor
 *
 * /**
 *  * A seed that can be used to deserialize only the `n`th element of a sequence
 *  * while efficiently discarding elements of any type before or after index `n`.
 *  *
 *  * For example to deserialize only the element at index 3:
 *  *
 *  * ```
 *  * NthElement(3).deserialize(deserializer)
 *  * ```
 *  */
 * class NthElement<T>(
 *     private val n: Int,
 *     private val deserializeValue: Deserialize<T>,
 * ) : Visitor<T>, DeserializeSeed<T> {
 *     override fun expecting(): String =
 *         "a sequence in which we care about element $n"
 *
 *     override fun <A> visitSeq(access: A): SerdeResult<T>
 *         where A : SeqAccess =
 *         serdeCatching {
 *             for (index in 0 until n) {
 *                 if (access.nextElement(IgnoredAny).getOrThrow() == SeqElement.None) {
 *                     throw SerdeException(SerdeError.invalidLength(index, this))
 *                 }
 *             }
 *
 *             val nth = access.nextElement(object : DeserializeSeed<T> {
 *                 override fun <D> deserialize(deserializer: D): SerdeResult<T>
 *                     where D : Deserializer =
 *                     deserializeValue.deserialize(deserializer)
 *             }).getOrThrow().unwrapOrElse {
 *                 throw SerdeException(SerdeError.invalidLength(n, this))
 *             }
 *
 *             while (access.nextElement(IgnoredAny).getOrThrow() is SeqElement.Some) {
 *                 // ignore
 *             }
 *
 *             nth
 *         }
 *
 *     override fun <D> deserialize(deserializer: D): SerdeResult<T>
 *         where D : Deserializer =
 *         deserializer.deserializeSeq(this)
 * }
 *
 * // Deserialize only the sequence element at index 3 from this deserializer.
 * // The element at index 3 is required to be a string. Elements before and
 * // after index 3 are allowed to be of any type.
 * val s: String = NthElement(3, stringDeserialize).deserialize(deserializer).getOrThrow()
 * ```
 */
data object IgnoredAny : Visitor<IgnoredAny>, Deserialize<IgnoredAny>, DeserializeSeed<IgnoredAny> {
    override fun expecting(): String = "anything at all"

    override fun visitBool(v: Boolean): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun visitI64(v: Long): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun visitI128(v: String): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun visitU64(v: ULong): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun visitU128(v: String): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun visitF64(v: Double): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun visitStr(v: String): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun visitNone(): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun <D> visitSome(deserializer: D): SerdeResult<IgnoredAny>
        where D : Deserializer = deserialize(deserializer)

    override fun <D> visitNewtypeStruct(deserializer: D): SerdeResult<IgnoredAny>
        where D : Deserializer = deserialize(deserializer)

    override fun visitUnit(): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<IgnoredAny> {
        while (true) {
            when (val result = access.nextElementSeed(IgnoredAny)) {
                is SerdeResult.Failure -> {
                    return result
                }

                is SerdeResult.Success -> {
                    when (result.value) {
                        is SeqElement.Some -> Unit
                        SeqElement.None -> return SerdeResult.success(IgnoredAny)
                    }
                }
            }
        }
    }

    override fun <A : MapAccess> visitMap(access: A): SerdeResult<IgnoredAny> {
        while (true) {
            when (val result = access.nextEntrySeed(IgnoredAny, IgnoredAny)) {
                is SerdeResult.Failure -> {
                    return result
                }

                is SerdeResult.Success -> {
                    if (result.value == null) return SerdeResult.success(IgnoredAny)
                }
            }
        }
    }

    override fun visitBytes(v: ByteArray): SerdeResult<IgnoredAny> = SerdeResult.success(IgnoredAny)

    override fun <A> visitEnum(access: A): SerdeResult<IgnoredAny>
        where A : EnumAccess =
        access.variantSeed(IgnoredAny).flatMap { (_, variant) ->
            variant.newtypeVariant(IgnoredAny)
        }

    override fun <D> deserialize(deserializer: D): SerdeResult<IgnoredAny>
        where D : Deserializer =
        deserializer.deserializeIgnoredAny(IgnoredAny)
}
