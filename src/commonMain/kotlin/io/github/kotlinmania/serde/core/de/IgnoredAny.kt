// port-lint: source serde_core/src/de/ignored_any.rs
package io.github.kotlinmania.serde.core.de

/**
 * An efficient way of discarding data from a deserializer.
 *
 * Think of this like `serde_json.Value` in that it can be deserialized from any type, except that
 * it does not store any information about the data that gets deserialized.
 *
 * ```kotlin
 * import io.github.kotlinmania.serde.core.de.Deserialize
 * import io.github.kotlinmania.serde.core.de.DeserializeSeed
 * import io.github.kotlinmania.serde.core.de.Deserializer
 * import io.github.kotlinmania.serde.core.de.IgnoredAny
 * import io.github.kotlinmania.serde.core.de.SeqAccess
 * import io.github.kotlinmania.serde.core.de.Visitor
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
 *     override fun <A> visitSeq(seq: A): Result<T>
 *         where A : SeqAccess =
 *         runCatching {
 *             for (index in 0 until n) {
 *                 if (seq.nextElement(IgnoredAny).getOrThrow() == null) {
 *                     throw Error.invalidLength(index, this)
 *                 }
 *             }
 *
 *             val nth = seq.nextElement(object : DeserializeSeed<T> {
 *                 override fun <D> deserialize(deserializer: D): Result<T>
 *                     where D : Deserializer =
 *                     deserializeValue.deserialize(deserializer)
 *             }).getOrThrow() ?: throw Error.invalidLength(n, this)
 *
 *             while (seq.nextElement(IgnoredAny).getOrThrow() != null) {
 *                 // ignore
 *             }
 *
 *             nth
 *         }
 *
 *     override fun <D> deserialize(deserializer: D): Result<T>
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
public data object IgnoredAny : Visitor<IgnoredAny>, Deserialize<IgnoredAny>, DeserializeSeed<IgnoredAny> {
    override fun expecting(): String = "anything at all"

    override fun visitBool(v: Boolean): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun visitI64(v: Long): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun visitI128(v: String): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun visitU64(v: ULong): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun visitU128(v: String): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun visitF64(v: Double): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun visitStr(v: String): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun visitNone(): Result<IgnoredAny> =
        Result.success(IgnoredAny)

    override fun <D> visitSome(deserializer: D): Result<IgnoredAny>
        where D : Deserializer =
        deserialize(deserializer)

    override fun <D> visitNewtypeStruct(deserializer: D): Result<IgnoredAny>
        where D : Deserializer =
        deserialize(deserializer)

    override fun visitUnit(): Result<IgnoredAny> =
        Result.success(IgnoredAny)

    override fun <A> visitSeq(seq: A): Result<IgnoredAny>
        where A : SeqAccess =
        runCatching {
            while (seq.nextElement(IgnoredAny).getOrThrow() != null) {
                // Gobble
            }
            IgnoredAny
        }

    override fun <A> visitMap(map: A): Result<IgnoredAny>
        where A : MapAccess =
        runCatching {
            while (map.nextEntrySeed(IgnoredAny, IgnoredAny).getOrThrow() != null) {
                // Gobble
            }
            IgnoredAny
        }

    override fun visitBytes(v: ByteArray): Result<IgnoredAny> {
        v.hashCode()
        return Result.success(IgnoredAny)
    }

    override fun <A> visitEnum(data: A): Result<IgnoredAny>
        where A : EnumAccess =
        runCatching {
            data.variantSeed(IgnoredAny).getOrThrow().second.newtypeVariant(IgnoredAny).getOrThrow()
        }

    override fun <D> deserialize(deserializer: D): Result<IgnoredAny>
        where D : Deserializer =
        deserializer.deserializeIgnoredAny(IgnoredAny)
}
