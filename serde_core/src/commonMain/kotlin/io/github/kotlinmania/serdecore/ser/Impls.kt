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

internal class FormatArguments(
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

fun <Ok, T> Array<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serdeCatching {
        val tuple = serializer.serializeTuple(size).getOrThrow()
        for (element in this) {
            tuple.serializeElement(element).getOrThrow()
        }
        tuple.end().getOrThrow()
    }

fun <Ok, T> Iterable<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serializer.collectSeq(this)

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, K, V> Map<K, V>.serialize(
    serializer: Serializer<Ok>,
): SerdeResult<Ok>
    where K : Serialize,
          V : Serialize =
    serializer.collectMap(this.entries.asIterable().map { it.key to it.value })

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T> RangeValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serdeCatching {
        val state = serializer.serializeStruct("Range", 2).getOrThrow()
        state.serializeField("start", start).getOrThrow()
        state.serializeField("end", end).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> RangeFromValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serdeCatching {
        val state = serializer.serializeStruct("RangeFrom", 1).getOrThrow()
        state.serializeField("start", start).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> RangeInclusiveValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serdeCatching {
        val state = serializer.serializeStruct("RangeInclusive", 2).getOrThrow()
        state.serializeField("start", start).getOrThrow()
        state.serializeField("end", end).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> RangeToValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serdeCatching {
        val state = serializer.serializeStruct("RangeTo", 1).getOrThrow()
        state.serializeField("end", end).getOrThrow()
        state.end().getOrThrow()
    }

fun <Ok, T> BoundValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    when (this) {
        BoundValue.Unbounded -> serializer.serializeUnitVariant("Bound", 0u, "Unbounded")
        is BoundValue.Included -> serializer.serializeNewtypeVariant("Bound", 1u, "Included", value)
        is BoundValue.Excluded -> serializer.serializeNewtypeVariant("Bound", 2u, "Excluded", value)
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T0, T1> Pair<T0, T1>.serialize(
    serializer: Serializer<Ok>,
): SerdeResult<Ok>
    where T0 : Serialize,
          T1 : Serialize =
    serdeCatching {
        val tuple = serializer.serializeTuple(2).getOrThrow()
        tuple.serializeElement(first).getOrThrow()
        tuple.serializeElement(second).getOrThrow()
        tuple.end().getOrThrow()
    }

fun <Ok, T0, T1, T2> Triple<T0, T1, T2>.serialize(
    serializer: Serializer<Ok>,
): SerdeResult<Ok>
    where T0 : Serialize,
          T1 : Serialize,
          T2 : Serialize =
    serdeCatching {
        val tuple = serializer.serializeTuple(3).getOrThrow()
        tuple.serializeElement(first).getOrThrow()
        tuple.serializeElement(second).getOrThrow()
        tuple.serializeElement(third).getOrThrow()
        tuple.end().getOrThrow()
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

data class Wrapping<T : Serialize>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = value.serialize(serializer)
}

data class Saturating<T : Serialize>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = value.serialize(serializer)
}

data class Reverse<T : Serialize>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = value.serialize(serializer)
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
