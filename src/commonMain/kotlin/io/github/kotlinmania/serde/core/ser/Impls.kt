// port-lint: source serde_core/src/ser/impls.rs
package io.github.kotlinmania.serde.core.ser

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

public fun <Ok, E, T> Array<T>.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error,
          T : Serialize =
    serializer.collectSeq(this.asList())

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

