// port-lint: source ser/impls.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BoundValue
import io.github.kotlinmania.serdecore.de.CStringValue
import io.github.kotlinmania.serdecore.de.IpAddress
import io.github.kotlinmania.serdecore.de.Ipv4Address
import io.github.kotlinmania.serdecore.de.Ipv6Address
import io.github.kotlinmania.serdecore.de.OsStringValue
import io.github.kotlinmania.serdecore.de.PathValue
import io.github.kotlinmania.serdecore.de.RangeFromValue
import io.github.kotlinmania.serdecore.de.RangeInclusiveValue
import io.github.kotlinmania.serdecore.de.RangeToValue
import io.github.kotlinmania.serdecore.de.RangeValue
import io.github.kotlinmania.serdecore.de.ResultValue
import io.github.kotlinmania.serdecore.de.SocketAddress
import io.github.kotlinmania.serdecore.de.SocketAddressV4
import io.github.kotlinmania.serdecore.de.SocketAddressV6
import io.github.kotlinmania.serdecore.priv.IteratorWithSizeHint
import kotlin.text.CharacterCodingException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Boolean.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeBool(this)

fun <Ok> Byte.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI8(this)

fun <Ok> Short.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI16(this)

fun <Ok> Int.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI32(this)

fun <Ok> Long.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI64(this)

fun <Ok> UByte.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU8(this)

fun <Ok> UShort.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU16(this)

fun <Ok> UInt.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU32(this)

fun <Ok> ULong.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU64(this)

fun <Ok> Float.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeF32(this)

fun <Ok> Double.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeF64(this)

fun <Ok> Char.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeChar(this)

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> String.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(this)

class FormatArguments(
    private val value: Any,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.collectStr(value)
}

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> ByteArray.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeBytes(this)

// Serialize for CString forwards the bytes without the C string terminator.
internal fun <Ok> CStringValue.serializeCString(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serializer.serializeBytes(bytes)

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Unit.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeUnit()

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T> T?.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    if (this != null) serializer.serializeSome(this) else serializer.serializeNone()

// //////////////////////////////////////////////////////////////////////////////

data object PhantomData : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeUnitStruct("PhantomData")
}

// //////////////////////////////////////////////////////////////////////////////

internal fun <Ok> serializeEmptyArray(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serializer.serializeTuple(0).flatMap { it.end() }

fun <Ok, T> Array<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize)

fun <Ok, T> Array<T>.serialize(serializer: Serializer<Ok>, elementSerializer: Serialize.For<T>): SerdeResult<Ok> =
    serializer.serializeTuple(size).flatMap { tuple ->
        for (value in this) {
            val result = tuple.serializeElement(elementSerializer.bind(value))
            if (result is SerdeResult.Failure) return@flatMap result
        }
        tuple.end()
    }

fun <Ok, T> Iterable<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize)

fun <Ok, T> Iterable<T>.serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
    serializer.collectSeq(mapForSerialization(this, element::bind))

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, K, V> Map<K, V>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize, DynamicSerialize)

fun <Ok, K, V> Map<K, V>.serialize(
    serializer: Serializer<Ok>,
    key: Serialize.For<K>,
    value: Serialize.For<V>,
): SerdeResult<Ok> =
    serializer.collectMap(mapForSerialization(entries) { key.bind(it.key) to value.bind(it.value) })

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T> RangeValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize)

fun <Ok, T> RangeValue<T>.serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
    serializer.serializeStruct("Range", 2).flatMap { state ->
        state
            .serializeField("start", element.bind(start))
            .flatMap {
                state.serializeField("end", element.bind(end))
            }.flatMap { state.end() }
    }

fun <Ok, T> RangeFromValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize)

fun <Ok, T> RangeFromValue<T>.serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
    serializer.serializeStruct("RangeFrom", 1).flatMap { state ->
        state.serializeField("start", element.bind(start)).flatMap { state.end() }
    }

fun <Ok, T> RangeInclusiveValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize)

fun <Ok, T> RangeInclusiveValue<T>.serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
    serializer.serializeStruct("RangeInclusive", 2).flatMap { state ->
        state
            .serializeField("start", element.bind(start))
            .flatMap {
                state.serializeField("end", element.bind(end))
            }.flatMap { state.end() }
    }

fun <Ok, T> RangeToValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize)

fun <Ok, T> RangeToValue<T>.serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
    serializer.serializeStruct("RangeTo", 1).flatMap { state ->
        state.serializeField("end", element.bind(end)).flatMap { state.end() }
    }

fun <Ok, T> BoundValue<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize)

fun <Ok, T> BoundValue<T>.serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
    when (this) {
        BoundValue.Unbounded -> serializer.serializeUnitVariant("Bound", 0u, "Unbounded")
        is BoundValue.Included -> serializer.serializeNewtypeVariant("Bound", 1u, "Included", element.bind(value))
        is BoundValue.Excluded -> serializer.serializeNewtypeVariant("Bound", 2u, "Excluded", element.bind(value))
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T0, T1> Pair<T0, T1>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize, DynamicSerialize)

fun <Ok, T0, T1> Pair<T0, T1>.serialize(
    serializer: Serializer<Ok>,
    firstSerializer: Serialize.For<T0>,
    secondSerializer: Serialize.For<T1>,
): SerdeResult<Ok> =
    serializer.serializeTuple(2).flatMap { tuple ->
        tuple
            .serializeElement(firstSerializer.bind(first))
            .flatMap {
                tuple.serializeElement(secondSerializer.bind(second))
            }.flatMap { tuple.end() }
    }

fun <Ok, T0, T1, T2> Triple<T0, T1, T2>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize, DynamicSerialize, DynamicSerialize)

fun <Ok, T0, T1, T2> Triple<T0, T1, T2>.serialize(
    serializer: Serializer<Ok>,
    firstSerializer: Serialize.For<T0>,
    secondSerializer: Serialize.For<T1>,
    thirdSerializer: Serialize.For<T2>,
): SerdeResult<Ok> =
    serializer.serializeTuple(3).flatMap { tuple ->
        tuple
            .serializeElement(firstSerializer.bind(first))
            .flatMap {
                tuple.serializeElement(secondSerializer.bind(second)).flatMap {
                    tuple.serializeElement(thirdSerializer.bind(third))
                }
            }.flatMap { tuple.end() }
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, T, E> ResultValue<T, E>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    serialize(serializer, DynamicSerialize, DynamicSerialize)

fun <Ok, T, E> ResultValue<T, E>.serialize(
    serializer: Serializer<Ok>,
    ok: Serialize.For<T>,
    error: Serialize.For<E>,
): SerdeResult<Ok> =
    when (this) {
        is ResultValue.Ok -> serializer.serializeNewtypeVariant("Result", 0u, "Ok", ok.bind(value))
        is ResultValue.Err -> serializer.serializeNewtypeVariant("Result", 1u, "Err", error.bind(this.error))
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Duration.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    if (this < Duration.ZERO || this == Duration.INFINITE) {
        SerdeResult.failure(SerdeError.custom("duration must be finite and non-negative"))
    } else {
        val secs = inWholeSeconds
        val nanos = (this - secs.seconds).inWholeNanoseconds
        serializer.serializeStruct("Duration", 2).flatMap { state ->
            state
                .serializeField("secs", ULongSerialize(secs.toULong()))
                .flatMap {
                    state.serializeField("nanos", UIntSerialize(nanos.toUInt()))
                }.flatMap { state.end() }
        }
    }

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Instant.serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
    if (epochSeconds < 0) {
        SerdeResult.failure(SerdeError.custom("SystemTime must be later than UNIX_EPOCH"))
    } else {
        serializer.serializeStruct("SystemTime", 2).flatMap { state ->
            state
                .serializeField("secs_since_epoch", ULongSerialize(epochSeconds.toULong()))
                .flatMap {
                    state.serializeField("nanos_since_epoch", UIntSerialize(nanosecondsOfSecond.toUInt()))
                }.flatMap { state.end() }
        }
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
    serializer.serializeTuple(octets.size).flatMap { tuple ->
        for (octet in octets) {
            val result = tuple.serializeElement(UByteSerialize(octet))
            if (result is SerdeResult.Failure) return@flatMap result
        }
        tuple.end()
    }

private fun <Ok> serializeSocketAddress(
    ip: Serialize,
    port: UShort,
    serializer: Serializer<Ok>,
): SerdeResult<Ok> =
    serializer.serializeTuple(2).flatMap { tuple ->
        tuple
            .serializeElement(ip)
            .flatMap {
                tuple.serializeElement(UShortSerialize(port))
            }.flatMap { tuple.end() }
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

internal fun <Ok> PathValue.serializePath(serializer: Serializer<Ok>): SerdeResult<Ok> {
    val text =
        try {
            value.encodeToByteArray(throwOnInvalidSequence = true)
            value
        } catch (error: CharacterCodingException) {
            return SerdeResult.failure(SerdeError.custom("path contains invalid UTF-8 characters"))
        }
    return serializer.serializeStr(text)
}

internal fun <Ok> OsStringValue.serializeOsString(serializer: Serializer<Ok>): SerdeResult<Ok> =
    when (this) {
        is OsStringValue.Unix ->
            serializer.serializeNewtypeVariant(
                "OsString",
                0u,
                "Unix",
                object : Serialize {
                    override fun <Value> serialize(serializer: Serializer<Value>): SerdeResult<Value> =
                        bytes.serialize(serializer, Serialize.U8)
                },
            )
        is OsStringValue.Windows ->
            serializer.serializeNewtypeVariant(
                "OsString",
                1u,
                "Windows",
                object : Serialize {
                    override fun <Value> serialize(serializer: Serializer<Value>): SerdeResult<Value> =
                        wide.serialize(serializer, Serialize.U16)
                },
            )
    }

// //////////////////////////////////////////////////////////////////////////////

data class Wrapping<T>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = asSerialize(value).serialize(serializer)

    fun <Ok> serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
        element.serialize(value, serializer)
}

data class Saturating<T>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = asSerialize(value).serialize(serializer)

    fun <Ok> serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
        element.serialize(value, serializer)
}

data class Reverse<T>(
    val value: T,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = asSerialize(value).serialize(serializer)

    fun <Ok> serialize(serializer: Serializer<Ok>, element: Serialize.For<T>): SerdeResult<Ok> =
        element.serialize(value, serializer)
}

private data class ULongSerialize(
    private val value: ULong,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU64(value)
}

private data class UByteSerialize(
    private val value: UByte,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU8(value)
}

private data class UShortSerialize(
    private val value: UShort,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU16(value)
}

private data class UIntSerialize(
    private val value: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeU32(value)
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

// Compatibility for callers that already erased their value type. This cannot
// recover signed/float widths on JavaScript; generic ports must pass For<T>.
private object DynamicSerialize : Serialize.For<Any?> {
    override fun <Ok> serialize(value: Any?, serializer: Serializer<Ok>): SerdeResult<Ok> =
        asSerialize(value).serialize(serializer)
}

private fun asSerialize(value: Any?): Serialize =
    if (value is Serialize) value else AnySerialize(value)

// Rust's borrowed collection iterators adapt one item at a time. Creating the
// serialization wrapper must not consume the input before collectSeq/collectMap
// has obtained its iterator and asked the serializer to begin.
private fun <T, R> mapForSerialization(source: Iterable<T>, transform: (T) -> R): Iterable<R> =
    Iterable {
        val iterator = source.iterator()
        var remaining = if (iterator !is IteratorWithSizeHint<*> && source is Collection<*>) source.size.toULong() else null
        object : IteratorWithSizeHint<R> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): R {
                val value = iterator.next()
                remaining = remaining?.minus(1uL)
                return transform(value)
            }
            override fun sizeHint(): Pair<ULong, ULong?> =
                if (iterator is IteratorWithSizeHint<*>) {
                    iterator.sizeHint()
                } else if (remaining != null) {
                    val length = requireNotNull(remaining)
                    length to length
                } else {
                    0uL to null
                }
        }
    }
