// port-lint: source serde_core/src/ser/fmt.rs
package io.github.kotlinmania.serde.core.ser

public data object FmtError : Error

/**
 * ```kotlin
 * import io.github.kotlinmania.serde.core.ser.Serialize
 *
 * enum class MessageType : Serialize {
 *     StartRequest,
 *     EndRequest;
 *
 *     override fun toString(): String =
 *         when (this) {
 *             StartRequest -> "start-request"
 *             EndRequest -> "end-request"
 *         }
 *
 *     override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
 *         where E : Error =
 *         serializer.serializeUnitVariant("MessageType", ordinal.toUInt(), toString())
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
    ): Result<Unit> =
        display(variant, name, variantIndex)

    override fun <T> serializeNewtypeStruct(name: String, value: T): Result<Unit>
        where T : Serialize {
        name.hashCode()
        return value.serialize(this)
    }

    override fun serializeBytes(v: ByteArray): Result<Unit> =
        fmtError(v)

    override fun serializeNone(): Result<Unit> =
        fmtError()

    override fun <T> serializeSome(value: T): Result<Unit>
        where T : Serialize =
        fmtError(value)

    override fun serializeUnit(): Result<Unit> =
        fmtError()

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<Unit>
        where T : Serialize =
        fmtError(name, variantIndex, variant, value)

    override fun serializeSeq(len: Int?): Result<SerializeSeq<Unit, FmtError>> =
        fmtError(len)

    override fun serializeTuple(len: Int): Result<SerializeTuple<Unit, FmtError>> =
        fmtError(len)

    override fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<Unit, FmtError>> =
        fmtError(name, len)

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Unit, FmtError>> =
        fmtError(name, variantIndex, variant, len)

    override fun serializeMap(len: Int?): Result<SerializeMap<Unit, FmtError>> =
        fmtError(len)

    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Unit, FmtError>> =
        fmtError(name, len)

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Unit, FmtError>> =
        fmtError(name, variantIndex, variant, len)

    override fun collectStr(value: Any?): Result<Unit> =
        display(value)

    private fun display(value: Any?, vararg touched: Any?): Result<Unit> =
        runCatching {
            touched.forEach { it?.hashCode() }
            formatter.append(value.toString())
            Unit
        }
}

private fun <T> fmtError(vararg touched: Any?): Result<T> {
    touched.forEach { it?.hashCode() }
    return Result.failure(SerdeSerializationException("formatting error"))
}
