// port-lint: source serde_core/src/ser/impls.rs
package io.github.kotlinmania.serde.core.ser

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> Boolean.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeBool(this)

public fun <Ok, E> Byte.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeI8(this)

public fun <Ok, E> Short.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeI16(this)

public fun <Ok, E> Int.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeI32(this)

public fun <Ok, E> Long.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeI64(this)

public fun <Ok, E> UByte.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeU8(this)

public fun <Ok, E> UShort.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeU16(this)

public fun <Ok, E> UInt.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeU32(this)

public fun <Ok, E> ULong.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeU64(this)

public fun <Ok, E> Float.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeF32(this)

public fun <Ok, E> Double.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeF64(this)

public fun <Ok, E> Char.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeChar(this)

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> String.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeStr(this)

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> ByteArray.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeBytes(this)

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> Unit.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeUnit()

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E, T> T?.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error,
          T : Serialize =
    if (this != null) serializer.serializeSome(this) else serializer.serializeNone()

////////////////////////////////////////////////////////////////////////////////

public data object PhantomData : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        serializer.serializeUnitStruct("PhantomData")
}

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E, T> Array<T>.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error,
          T : Serialize =
    runCatching {
        val tuple = serializer.serializeTuple(size).getOrThrow()
        for (element in this) {
            tuple.serializeElement(element).getOrThrow()
        }
        tuple.end().getOrThrow()
    }

public fun <Ok, E, T> Iterable<T>.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error,
          T : Serialize =
    serializer.collectSeq(this)

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E, K, V> Map<K, V>.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error,
          K : Serialize,
          V : Serialize =
    serializer.collectMap(this.entries.asIterable().map { it.key to it.value })

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E, T0, T1> Pair<T0, T1>.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error,
          T0 : Serialize,
          T1 : Serialize =
    runCatching {
        val tuple = serializer.serializeTuple(2).getOrThrow()
        tuple.serializeElement(first).getOrThrow()
        tuple.serializeElement(second).getOrThrow()
        tuple.end().getOrThrow()
    }

public fun <Ok, E, T0, T1, T2> Triple<T0, T1, T2>.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error,
          T0 : Serialize,
          T1 : Serialize,
          T2 : Serialize =
    runCatching {
        val tuple = serializer.serializeTuple(3).getOrThrow()
        tuple.serializeElement(first).getOrThrow()
        tuple.serializeElement(second).getOrThrow()
        tuple.serializeElement(third).getOrThrow()
        tuple.end().getOrThrow()
    }

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> Duration.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    runCatching {
        if (this < Duration.ZERO || this == Duration.INFINITE) {
            throw Error.custom("duration must be finite and non-negative")
        }
        val secs = inWholeSeconds
        val nanos = (this - secs.seconds).inWholeNanoseconds
        val state = serializer.serializeStruct("Duration", 2).getOrThrow()
        state.serializeField("secs", ULongSerialize(secs.toULong())).getOrThrow()
        state.serializeField("nanos", UIntSerialize(nanos.toUInt())).getOrThrow()
        state.end().getOrThrow()
    }

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> Instant.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    runCatching {
        if (epochSeconds < 0) {
            throw Error.custom("SystemTime must be later than UNIX_EPOCH")
        }
        val state = serializer.serializeStruct("SystemTime", 2).getOrThrow()
        state.serializeField("secs_since_epoch", ULongSerialize(epochSeconds.toULong())).getOrThrow()
        state.serializeField("nanos_since_epoch", UIntSerialize(nanosecondsOfSecond.toUInt())).getOrThrow()
        state.end().getOrThrow()
    }

////////////////////////////////////////////////////////////////////////////////

private val DEC_DIGITS_LUT: ByteArray =
    (
        "0001020304050607080910111213141516171819" +
            "2021222324252627282930313233343536373839" +
            "4041424344454647484950515253545556575859" +
            "6061626364656667686970717273747576777879" +
            "8081828384858687888990919293949596979899"
    ).encodeToByteArray()

internal fun formatU8(n: UByte, out: ByteArray): Int {
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

////////////////////////////////////////////////////////////////////////////////

public data class Wrapping<T : Serialize>(
    public val value: T,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        value.serialize(serializer)
}

public data class Saturating<T : Serialize>(
    public val value: T,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        value.serialize(serializer)
}

public data class Reverse<T : Serialize>(
    public val value: T,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        value.serialize(serializer)
}

private data class ULongSerialize(
    private val value: ULong,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        serializer.serializeU64(value)
}

private data class UIntSerialize(
    private val value: UInt,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        serializer.serializeU32(value)
}
