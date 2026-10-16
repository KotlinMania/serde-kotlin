// port-lint: source ser/impls.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.BoundValue
import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv6Address
import io.github.kotlinmania.serdecore.de.RangeFromValue
import io.github.kotlinmania.serdecore.de.RangeInclusiveValue
import io.github.kotlinmania.serdecore.de.RangeToValue
import io.github.kotlinmania.serdecore.de.RangeValue
import io.github.kotlinmania.serdecore.de.ResultValue
import io.github.kotlinmania.serdecore.de.SocketAddress
import io.github.kotlinmania.serdecore.de.SocketAddressV4
import io.github.kotlinmania.serdecore.de.SocketAddressV6
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Boolean.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeBool(this)

fun <Ok> Byte.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeI8(this)

fun <Ok> Short.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeI16(this)

fun <Ok> Int.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeI32(this)

fun <Ok> Long.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeI64(this)

fun <Ok> UByte.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeU8(this)

fun <Ok> UShort.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeU16(this)

fun <Ok> UInt.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeU32(this)

fun <Ok> ULong.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeU64(this)

fun <Ok> Float.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeF32(this)

fun <Ok> Double.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeF64(this)

fun <Ok> Char.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeChar(this)

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> String.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeStr(this)

class FormatArguments(
    private val value: Any,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = serializer.collectStr(value)
}

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> ByteArray.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeBytes(this)

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Unit.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeUnit()

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T> T?.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    if (this != null) serializer.serializeSome(this) else serializer.serializeNone()

// //////////////////////////////////////////////////////////////////////////////

data object PhantomData : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        =
        serializer.serializeUnitStruct("PhantomData")
}

// //////////////////////////////////////////////////////////////////////////////

internal fun <Ok> serializeEmptyArray(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serdeCatching {
        serializer.serializeTuple(0).getOrThrow().end().getOrThrow()
    }

fun <Ok, T> Array<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serdeCatching {
        val tuple = serializer.serializeTuple(size).getOrThrow()
        for (element in this) {
            tuple.serializeElement(asSerialize(element)).getOrThrow()
        }
        tuple.end().getOrThrow()
    }

fun <Ok, T> Iterable<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serializer.collectSeq(this.map(::asSerialize))

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, K, V> Map<K, V>.serialize(
    serializer: Serializer<Ok>,
): SerdeResult<Ok> =
    serializer.collectMap(this.entries.asIterable().map { asSerialize(it.key) to asSerialize(it.value) })

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T> RangeValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serdeCatching {
        val state = serializer.serializeStruct("Range", 2).getOrThrow()
        state.serializeField("start", asSerialize(start)).getOrThrow()
        state.serializeField("end", asSerialize(end)).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> RangeFromValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serdeCatching {
        val state = serializer.serializeStruct("RangeFrom", 1).getOrThrow()
        state.serializeField("start", asSerialize(start)).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> RangeInclusiveValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serdeCatching {
        val state = serializer.serializeStruct("RangeInclusive", 2).getOrThrow()
        state.serializeField("start", asSerialize(start)).getOrThrow()
        state.serializeField("end", asSerialize(end)).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> RangeToValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serdeCatching {
        val state = serializer.serializeStruct("RangeTo", 1).getOrThrow()
        state.serializeField("end", asSerialize(end)).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> BoundValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    when (this) {
        BoundValue.Unbounded -> serializer.serializeUnitVariant("Bound", 0u, "Unbounded")
        is BoundValue.Included -> serializer.serializeNewtypeVariant("Bound", 1u, "Included", asSerialize(value))
        is BoundValue.Excluded -> serializer.serializeNewtypeVariant("Bound", 2u, "Excluded", asSerialize(value))
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T0, T1> Pair<T0, T1>.serialize(
    serializer: Serializer<Ok>,
): SerdeResult<Ok> =
    serdeCatching {
        val tuple = serializer.serializeTuple(2).getOrThrow()
        tuple.serializeElement(asSerialize(first)).getOrThrow()
        tuple.serializeElement(asSerialize(second)).getOrThrow()
        tuple.end().getOrThrow()
    }

fun <Ok, T0, T1, T2> Triple<T0, T1, T2>.serialize(
    serializer: Serializer<Ok>,
): SerdeResult<Ok> =
    serdeCatching {
        val tuple = serializer.serializeTuple(3).getOrThrow()
        tuple.serializeElement(asSerialize(first)).getOrThrow()
        tuple.serializeElement(asSerialize(second)).getOrThrow()
        tuple.serializeElement(asSerialize(third)).getOrThrow()
        tuple.end().getOrThrow()
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T, E> ResultValue<T, E>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    when (this) {
        is ResultValue.Ok -> serializer.serializeNewtypeVariant("Result", 0u, "Ok", asSerialize(value))
        is ResultValue.Err -> serializer.serializeNewtypeVariant("Result", 1u, "Err", asSerialize(error))
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Duration.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    =
    serdeCatching {
        if (this < Duration.ZERO || this == Duration.INFINITE) {
            throw SerdeException(SerdeError.custom("duration must be finite and non-negative"))
        }
        val secs = inWholeSeconds
        val nanos = (this - secs.seconds).inWholeNanoseconds
        val state = serializer.serializeStruct("Duration", 2).getOrThrow()
        state.serializeField("secs", ULongSerialize(secs.toULong())).getOrThrow()
        state.serializeField("nanos", UIntSerialize(nanos.toUInt())).getOrThrow()
        state.end().getOrThrow()
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Instant.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    =
    serdeCatching {
        if (epochSeconds < 0) {
            throw SerdeException(SerdeError.custom("SystemTime must be later than UNIX_EPOCH"))
        }
        val state = serializer.serializeStruct("SystemTime", 2).getOrThrow()
        state.serializeField("secs_since_epoch", ULongSerialize(epochSeconds.toULong())).getOrThrow()
        state.serializeField("nanos_since_epoch", UIntSerialize(nanosecondsOfSecond.toUInt())).getOrThrow()
        state.end().getOrThrow()
    }

// //////////////////////////////////////////////////////////////////////////////

private val DEC_DIGITS_LUT: ByteArray =
    (
        "0001020304050607080910111213141516171819" +
            "2021222324252627282930313233343536373839" +
            "4041424344454647484950515253545556575859" +
            "6061626364656667686970717273747576777879" +
            "8081828384858687888990919293949596979899"
    ).encodeToByteArray()

internal fun formatU8(
    n: UByte,
    out: ByteArray,
): Int {
    var value = n.toInt()
    return if (value >= 100) {
        val d1 = ((value % 100) shl 1)
        value /= 100
        out[0] = ('0'.code + value).toByte()
        out[1] = DEC_DIGITS_LUT[d1]
        out[2] = DEC_DIGITS_LUT[d1 + 1]
        3
    } else if (value >= 10) {
        val d1 = (value shl 1)
        out[0] = DEC_DIGITS_LUT[d1]
        out[1] = DEC_DIGITS_LUT[d1 + 1]
        2
    } else {
        out[0] = ('0'.code + value).toByte()
        1
    }
}

// //////////////////////////////////////////////////////////////////////////////

internal fun <Ok> IpAddress.serializeNetwork(serializer: Serializer<Ok>): SerdeResult<Ok> =
    when (this) {
        is IpAddress.V4 ->
            if (serializer.isHumanReadable()) {
                address.serialize(serializer)
            } else {
                serializer.serializeNewtypeVariant("IpAddr", 0u, "V4", address)
            }
        is IpAddress.V6 ->
            if (serializer.isHumanReadable()) {
                address.serialize(serializer)
            } else {
                serializer.serializeNewtypeVariant("IpAddr", 1u, "V6", address)
            }
    }

internal fun <Ok> Ipv4Address.serializeNetwork(serializer: Serializer<Ok>): SerdeResult<Ok> =
    if (serializer.isHumanReadable()) {
        serializer.serializeStr(toString())
    } else {
        serializeOctets(octets, serializer)
    }

internal fun <Ok> Ipv6Address.serializeNetwork(serializer: Serializer<Ok>): SerdeResult<Ok> =
    if (serializer.isHumanReadable()) {
        serializer.serializeStr(formatIpv6(octets))
    } else {
        serializeOctets(octets, serializer)
    }

internal fun <Ok> SocketAddress.serializeNetwork(serializer: Serializer<Ok>): SerdeResult<Ok> =
    when (this) {
        is SocketAddress.V4 ->
            if (serializer.isHumanReadable()) {
                address.serialize(serializer)
            } else {
                serializer.serializeNewtypeVariant("SocketAddr", 0u, "V4", address)
            }
        is SocketAddress.V6 ->
            if (serializer.isHumanReadable()) {
                address.serialize(serializer)
            } else {
                serializer.serializeNewtypeVariant("SocketAddr", 1u, "V6", address)
            }
    }

internal fun <Ok> SocketAddressV4.serializeNetwork(serializer: Serializer<Ok>): SerdeResult<Ok> =
    if (serializer.isHumanReadable()) {
        serializer.serializeStr("$ip:$port")
    } else {
        serializeSocketAddress(ip, port, serializer)
    }

internal fun <Ok> SocketAddressV6.serializeNetwork(serializer: Serializer<Ok>): SerdeResult<Ok> =
    if (serializer.isHumanReadable()) {
        serializer.serializeStr("[${formatIpv6(ip.octets)}]:$port")
    } else {
        serializeSocketAddress(ip, port, serializer)
    }

private fun <Ok> serializeOctets(
    octets: List<UByte>,
    serializer: Serializer<Ok>,
): SerdeResult<Ok> =
    serdeCatching {
        val tuple = serializer.serializeTuple(octets.size).getOrThrow()
        for (octet in octets) {
            tuple.serializeElement(UByteSerialize(octet)).getOrThrow()
        }
        tuple.end().getOrThrow()
    }

private fun <Ok> serializeSocketAddress(
    ip: Serialize,
    port: UShort,
    serializer: Serializer<Ok>,
): SerdeResult<Ok> =
    serdeCatching {
        val tuple = serializer.serializeTuple(2).getOrThrow()
        tuple.serializeElement(ip).getOrThrow()
        tuple.serializeElement(UShortSerialize(port)).getOrThrow()
        tuple.end().getOrThrow()
    }

private fun formatIpv6(octets: List<UByte>): String {
    val segments =
        octets.chunked(2).map { pair ->
            (pair[0].toInt() shl 8) or pair[1].toInt()
        }
    var bestStart = -1
    var bestLength = 0
    var index = 0
    while (index < segments.size) {
        if (segments[index] != 0) {
            index += 1
            continue
        }
        val start = index
        while (index < segments.size && segments[index] == 0) index += 1
        val length = index - start
        if (length >= 2 && length > bestLength) {
            bestStart = start
            bestLength = length
        }
    }
    if (bestStart < 0) return segments.joinToString(":") { it.toString(16) }

    val left = segments.take(bestStart).joinToString(":") { it.toString(16) }
    val right = segments.drop(bestStart + bestLength).joinToString(":") { it.toString(16) }
    return when {
        left.isEmpty() && right.isEmpty() -> "::"
        left.isEmpty() -> "::$right"
        right.isEmpty() -> "$left::"
        else -> "$left::$right"
    }
}

// //////////////////////////////////////////////////////////////////////////////

data class Wrapping<T>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = asSerialize(value).serialize(serializer)
}

data class Saturating<T>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = asSerialize(value).serialize(serializer)
}

data class Reverse<T>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = asSerialize(value).serialize(serializer)
}

private data class ULongSerialize(
    private val value: ULong,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = serializer.serializeU64(value)
}

private data class UByteSerialize(
    private val value: UByte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = serializer.serializeU8(value)
}

private data class UShortSerialize(
    private val value: UShort,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = serializer.serializeU16(value)
}

private data class UIntSerialize(
    private val value: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = serializer.serializeU32(value)
}

private class AnySerialize(
    private val value: Any?,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        when (val v = value) {
            null -> serializer.serializeNone()
            is Serialize -> v.serialize(serializer)
            is Boolean -> v.serialize(serializer)
            is Byte -> v.serialize(serializer)
            is Short -> v.serialize(serializer)
            is Int -> v.serialize(serializer)
            is Long -> v.serialize(serializer)
            is UByte -> v.serialize(serializer)
            is UShort -> v.serialize(serializer)
            is UInt -> v.serialize(serializer)
            is ULong -> v.serialize(serializer)
            is Float -> v.serialize(serializer)
            is Double -> v.serialize(serializer)
            is Char -> v.serialize(serializer)
            is String -> v.serialize(serializer)
            is ByteArray -> v.serialize(serializer)
            is Unit -> v.serialize(serializer)
            is Array<*> -> v.serialize(serializer)
            is Iterable<*> -> v.serialize(serializer)
            is Map<*, *> -> v.serialize(serializer)
            is Pair<*, *> -> v.serialize(serializer)
            is Triple<*, *, *> -> v.serialize(serializer)
            is RangeValue<*> -> v.serialize(serializer)
            is RangeFromValue<*> -> v.serialize(serializer)
            is RangeInclusiveValue<*> -> v.serialize(serializer)
            is RangeToValue<*> -> v.serialize(serializer)
            is BoundValue<*> -> v.serialize(serializer)
            is ResultValue<*, *> -> v.serialize(serializer)
            is Duration -> v.serialize(serializer)
            is Instant -> v.serialize(serializer)
            else -> SerdeResult.failure(SerdeError.custom("type does not implement Serialize: ${v::class}"))
        }
}

private fun asSerialize(value: Any?): Serialize =
    if (value is Serialize) value else AnySerialize(value)
