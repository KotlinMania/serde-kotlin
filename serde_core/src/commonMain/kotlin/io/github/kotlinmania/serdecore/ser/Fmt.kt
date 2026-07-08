// port-lint: source ser/fmt.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching

private fun unsignedDecimal(value: UInt): String {
    val bits = value.toInt()
    return if (bits >= 0) bits.toString() else (bits.toLong() and 0xffff_ffffL).toString()
}

private fun unsignedDecimal(value: ULong): String {
    val bits = value.toLong()
    if (bits >= 0) return bits.toString()

    val quotient = (bits ushr 1) / 5
    val remainder = bits - quotient * 10
    return quotient.toString() + remainder.toString()
}

/**
 * ```kotlin
 * import io.github.kotlinmania.serdecore.ser.Serialize
 * import io.github.kotlinmania.serdecore.ser.Serializer
 *
 * enum class MessageType : Serialize {
 *     StartRequest,
 *     EndRequest;
 *
 *     override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
 *         =
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
 * fun MessageType.format(formatter: Appendable): SerdeResult<Unit> =
 *     serialize(FormatterSerializer(formatter))
 * ```
 */
class FormatterSerializer(
    private val formatter: Appendable,
) : Serializer<Unit> {
    override fun serializeBool(v: Boolean): SerdeResult<Unit> = display(v.toString())

    override fun serializeI8(v: Byte): SerdeResult<Unit> = display(v.toString())

    override fun serializeI16(v: Short): SerdeResult<Unit> = display(v.toString())

    override fun serializeI32(v: Int): SerdeResult<Unit> = display(v.toString())

    override fun serializeI64(v: Long): SerdeResult<Unit> = display(v.toString())

    override fun serializeI128(value: String): SerdeResult<Unit> = display(value)

    override fun serializeU8(v: UByte): SerdeResult<Unit> = display(v.toString())

    override fun serializeU16(v: UShort): SerdeResult<Unit> = display(v.toString())

    override fun serializeU32(v: UInt): SerdeResult<Unit> = display(unsignedDecimal(v))

    override fun serializeU64(v: ULong): SerdeResult<Unit> = display(unsignedDecimal(v))

    override fun serializeU128(value: String): SerdeResult<Unit> = display(value)

    override fun serializeF32(v: Float): SerdeResult<Unit> = display(rustDisplayFloat(v))

    override fun serializeF64(v: Double): SerdeResult<Unit> = display(rustDisplayFloat(v))

    override fun serializeChar(v: Char): SerdeResult<Unit> = display(v.toString())

    override fun serializeStr(v: String): SerdeResult<Unit> = display(v)

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = display(name)

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> = display(variant)

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = fmtError()

    override fun serializeNone(): SerdeResult<Unit> = fmtError()

    override fun <T> serializeSome(value: T): SerdeResult<Unit>
        where T : Serialize = fmtError()

    override fun serializeUnit(): SerdeResult<Unit> = fmtError()

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = fmtError()

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> = fmtError()

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> = fmtError()

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Unit>> = fmtError()

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> = fmtError()

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> = fmtError()

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Unit>> = fmtError()

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> = fmtError()

    override fun collectStr(value: String): SerdeResult<Unit> = display(value)

    private fun display(value: String): SerdeResult<Unit> =
        serdeCatching {
            formatter.append(value)
        }
}

private fun <T> fmtError(): SerdeResult<T> = SerdeResult.failure(SerdeError("formatting error"))

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

private fun expandScientificDecimal(
    mantissa: String,
    exponent: Int,
): String {
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
