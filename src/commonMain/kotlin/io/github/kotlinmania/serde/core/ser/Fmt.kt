// port-lint: source serde_core/src/ser/fmt.rs
package io.github.kotlinmania.serde.core.ser

public data object FmtError : Error {
    public fun custom(_msg: Any?): FmtError = this
}

/**
 * ```kotlin
 * import io.github.kotlinmania.serde.core.ser.Serialize
 * import io.github.kotlinmania.serde.core.ser.Serializer
 *
 * enum class MessageType : Serialize {
 *     StartRequest,
 *     EndRequest;
 *
 *     override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
 *         where E : Error =
 *         serializer.serializeUnitVariant(
 *             name = "MessageType",
 *             variantIndex = ordinal.toUInt(),
 *             variant = when (this) {
 *                 StartRequest -> "start-request"
 *                 EndRequest -> "end-request"
 *             },
 *         )
 * }
 *
 * fun MessageType.format(formatter: Appendable): Result<Unit> =
 *     serialize(FormatterSerializer(formatter))
 * ```
 */
public class FormatterSerializer(
    private val formatter: Appendable,
) : Serializer<Unit, FmtError> {
    override fun serializeBool(v: Boolean): Result<Unit> =
        display(v)

    override fun serializeI8(v: Byte): Result<Unit> =
        display(v)

    override fun serializeI16(v: Short): Result<Unit> =
        display(v)

    override fun serializeI32(v: Int): Result<Unit> =
        display(v)

    override fun serializeI64(v: Long): Result<Unit> =
        display(v)

    override fun serializeI128(v: String): Result<Unit> =
        display(v)

    override fun serializeU8(v: UByte): Result<Unit> =
        display(v)

    override fun serializeU16(v: UShort): Result<Unit> =
        display(v)

    override fun serializeU32(v: UInt): Result<Unit> =
        display(v)

    override fun serializeU64(v: ULong): Result<Unit> =
        display(v)

    override fun serializeU128(v: String): Result<Unit> =
        display(v)

    override fun serializeF32(v: Float): Result<Unit> =
        display(v)

    override fun serializeF64(v: Double): Result<Unit> =
        display(v)

    override fun serializeChar(v: Char): Result<Unit> =
        display(v)

    override fun serializeStr(v: String): Result<Unit> =
        display(v)

    override fun serializeUnitStruct(name: String): Result<Unit> =
        display(name)

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): Result<Unit> {
        name.hashCode()
        variantIndex.hashCode()
        return display(variant)
    }

    override fun <T : Serialize> serializeNewtypeStruct(name: String, value: T): Result<Unit> {
        name.hashCode()
        return value.serialize(this)
    }

    override fun serializeBytes(v: ByteArray): Result<Unit> {
        v.hashCode()
        return fmtError()
    }

    override fun serializeNone(): Result<Unit> =
        fmtError()

    override fun <T : Serialize> serializeSome(value: T): Result<Unit> =
        fmtError()

    override fun serializeUnit(): Result<Unit> =
        fmtError()

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<Unit> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        value.hashCode()
        return fmtError()
    }

    override fun serializeSeq(len: Int?): Result<SerializeSeq<Unit, FmtError>> =
        fmtError()

    override fun serializeTuple(len: Int): Result<SerializeTuple<Unit, FmtError>> =
        fmtError()

    override fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<Unit, FmtError>> =
        fmtError()

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Unit, FmtError>> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        len.hashCode()
        return fmtError()
    }

    override fun serializeMap(len: Int?): Result<SerializeMap<Unit, FmtError>> =
        fmtError()

    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Unit, FmtError>> =
        fmtError()

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Unit, FmtError>> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        len.hashCode()
        return fmtError()
    }

    override fun collectStr(value: Any?): Result<Unit> =
        display(value)

    private fun display(value: Any?): Result<Unit> =
        runCatching {
            formatter.append(value.toString())
            Unit
        }
}

private fun <T> fmtError(): Result<T> =
    Result.failure(SerdeSerializationException("formatting error"))
