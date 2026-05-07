// port-lint: source serde_core/src/ser/fmt.rs
package io.github.kotlinmania.serde.core.ser

public data object FmtError : Error {
    public fun custom(msg: Any?): FmtError = this
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
        display(rustDisplayFloat(v))

    override fun serializeF64(v: Double): Result<Unit> =
        display(rustDisplayFloat(v))

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
        display(variant)

    override fun <T> serializeNewtypeStruct(name: String, value: T): Result<Unit>
        where T : Serialize {
        return value.serialize(this)
    }

    override fun serializeBytes(v: ByteArray): Result<Unit> =
        fmtError()

    override fun serializeNone(): Result<Unit> =
        fmtError()

    override fun <T> serializeSome(value: T): Result<Unit>
        where T : Serialize {
        return fmtError()
    }

    override fun serializeUnit(): Result<Unit> =
        fmtError()

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<Unit>
        where T : Serialize {
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
    ): Result<SerializeTupleVariant<Unit, FmtError>> =
        fmtError()

    override fun serializeMap(len: Int?): Result<SerializeMap<Unit, FmtError>> =
        fmtError()

    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Unit, FmtError>> =
        fmtError()

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Unit, FmtError>> =
        fmtError()

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

private fun rustDisplayFloat(value: Float): String =
    when {
        value.isNaN() -> "NaN"
        value == Float.POSITIVE_INFINITY -> "inf"
        value == Float.NEGATIVE_INFINITY -> "-inf"
        value == 0.0f && value.toBits() == (-0.0f).toBits() -> "-0"
        else -> rustDisplayDecimal(value.toString())
    }

private fun rustDisplayFloat(value: Double): String =
    when {
        value.isNaN() -> "NaN"
        value == Double.POSITIVE_INFINITY -> "inf"
        value == Double.NEGATIVE_INFINITY -> "-inf"
        value == 0.0 && value.toBits() == (-0.0).toBits() -> "-0"
        else -> rustDisplayDecimal(value.toString())
    }

private fun rustDisplayDecimal(text: String): String {
    val upperExponentIndex = text.indexOf('E')
    val exponentIndex = if (upperExponentIndex == -1) text.indexOf('e') else upperExponentIndex
    if (exponentIndex == -1) {
        return stripTrailingZeroFraction(text)
    }

    val exponent = text.substring(exponentIndex + 1).toIntOrNull() ?: return text
    val mantissa = text.substring(0, exponentIndex)
    return expandScientificDecimal(mantissa, exponent)
}

private fun expandScientificDecimal(mantissa: String, exponent: Int): String {
    val negative = mantissa.startsWith("-")
    val unsignedMantissa = if (negative) mantissa.drop(1) else mantissa
    val pointIndex = unsignedMantissa.indexOf('.')
    val digits = unsignedMantissa.replace(".", "")
    val fractionalDigits = if (pointIndex == -1) 0 else unsignedMantissa.length - pointIndex - 1
    val decimalIndex = digits.length - fractionalDigits + exponent
    val expanded =
        when {
            decimalIndex <= 0 -> "0." + "0".repeat(-decimalIndex) + digits
            decimalIndex >= digits.length -> digits + "0".repeat(decimalIndex - digits.length)
            else -> digits.substring(0, decimalIndex) + "." + digits.substring(decimalIndex)
        }
    val stripped = stripTrailingZeroFraction(expanded)
    return if (negative) "-$stripped" else stripped
}

private fun stripTrailingZeroFraction(text: String): String =
    if ('.' in text) {
        text.trimEnd('0').trimEnd('.')
    } else {
        text
    }
