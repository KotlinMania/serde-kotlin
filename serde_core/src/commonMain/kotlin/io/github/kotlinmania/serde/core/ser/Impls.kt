// port-lint: source serde_core/src/ser/impls.rs
package io.github.kotlinmania.serde.core.ser

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
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

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> ByteArray.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeBytes(this)

// //////////////////////////////////////////////////////////////////////////////

fun <Ok> Unit.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    = serializer.serializeUnit()

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, E, T> T?.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    if (this != null) serializer.serializeSome(this) else serializer.serializeNone()

// //////////////////////////////////////////////////////////////////////////////

data object PhantomData : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        =
        serializer.serializeUnitStruct("PhantomData")
}

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, E, T> Array<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serdeCatching {
        val tuple = serializer.serializeTuple(size).getOrThrow()
        for (element in this) {
            tuple.serializeElement(element).getOrThrow()
        }
        tuple.end().getOrThrow()
    }

fun <Ok, E, T> Iterable<T>.serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
    where T : Serialize =
    serializer.collectSeq(this)

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, E, K, V> Map<K, V>.serialize(
    serializer: Serializer<Ok>,
): SerdeResult<Ok>
    where K : Serialize,
          V : Serialize =
    serializer.collectMap(this.entries.asIterable().map { it.key to it.value })

// //////////////////////////////////////////////////////////////////////////////

fun <Ok, E, T0, T1> Pair<T0, T1>.serialize(
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

fun <Ok, E, T0, T1, T2> Triple<T0, T1, T2>.serialize(
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

private data class UIntSerialize(
    private val value: UInt,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = serializer.serializeU32(value)
}
