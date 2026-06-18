// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

import io.github.kotlinmania.serde.SerdeError

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching

/**
 * A **data format** that can serialize any data structure supported by Serde.
 *
 * The role of this interface is to define the serialization half of the Serde data model, which is
 * a way to categorize every data structure into one of 29 possible types. Each method of the
 * `Serializer` interface corresponds to one of the types of the data model.
 *
 * Implementations of `Serialize` map themselves into this data model by invoking exactly one of the
 * `Serializer` methods.
 */
interface Serializer<Ok>
    {
    /**
     * Serialize a `Boolean` value.
     */
    fun serializeBool(v: Boolean): SerdeResult<Ok>

    /**
     * Serialize a `Byte` value.
     *
     * If the format does not differentiate between `Byte` and `Long`, a reasonable implementation
     * would be to cast the value to `Long` and forward to `serializeI64`.
     */
    fun serializeI8(v: Byte): SerdeResult<Ok>

    /**
     * Serialize a `Short` value.
     *
     * If the format does not differentiate between `Short` and `Long`, a reasonable implementation
     * would be to cast the value to `Long` and forward to `serializeI64`.
     */
    fun serializeI16(v: Short): SerdeResult<Ok>

    /**
     * Serialize an `Int` value.
     *
     * If the format does not differentiate between `Int` and `Long`, a reasonable implementation
     * would be to cast the value to `Long` and forward to `serializeI64`.
     */
    fun serializeI32(v: Int): SerdeResult<Ok>

    /**
     * Serialize a `Long` value.
     */
    fun serializeI64(v: Long): SerdeResult<Ok>

    /**
     * Serialize an `i128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    fun serializeI128(value: String): SerdeResult<Ok> = SerdeResult.failure(SerdeError.custom("i128 is not supported"))

    /**
     * Serialize a `UByte` value.
     *
     * If the format does not differentiate between `UByte` and `ULong`, a reasonable implementation
     * would be to cast the value to `ULong` and forward to `serializeU64`.
     */
    fun serializeU8(v: UByte): SerdeResult<Ok>

    /**
     * Serialize a `UShort` value.
     *
     * If the format does not differentiate between `UShort` and `ULong`, a reasonable implementation
     * would be to cast the value to `ULong` and forward to `serializeU64`.
     */
    fun serializeU16(v: UShort): SerdeResult<Ok>

    /**
     * Serialize a `UInt` value.
     *
     * If the format does not differentiate between `UInt` and `ULong`, a reasonable implementation
     * would be to cast the value to `ULong` and forward to `serializeU64`.
     */
    fun serializeU32(v: UInt): SerdeResult<Ok>

    /**
     * Serialize a `ULong` value.
     */
    fun serializeU64(v: ULong): SerdeResult<Ok>

    /**
     * Serialize a `u128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    fun serializeU128(value: String): SerdeResult<Ok> = SerdeResult.failure(SerdeError.custom("u128 is not supported"))

    /**
     * Serialize a `Float` value.
     *
     * If the format does not differentiate between `Float` and `Double`, a reasonable
     * implementation would be to cast the value to `Double` and forward to `serializeF64`.
     */
    fun serializeF32(v: Float): SerdeResult<Ok>

    /**
     * Serialize a `Double` value.
     */
    fun serializeF64(v: Double): SerdeResult<Ok>

    /**
     * Serialize a character.
     *
     * If the format does not support characters, it is reasonable to serialize it as a single
     * element `String` or a `UInt`.
     */
    fun serializeChar(v: Char): SerdeResult<Ok>

    /**
     * Serialize a `String`.
     */
    fun serializeStr(v: String): SerdeResult<Ok>

    /**
     * Serialize a chunk of raw byte data.
     */
    fun serializeBytes(v: ByteArray): SerdeResult<Ok>

    /**
     * Serialize a `null` value.
     */
    fun serializeNone(): SerdeResult<Ok>

    /**
     * Serialize a non-null optional value.
     */
    fun <T> serializeSome(value: T): SerdeResult<Ok>
        where T : Serialize

    /**
     * Serialize a `Unit` value.
     */
    fun serializeUnit(): SerdeResult<Ok>

    /**
     * Serialize a unit class like `object Unit` or `PhantomData<T>`.
     */
    fun serializeUnitStruct(name: String): SerdeResult<Ok>

    /**
     * Serialize a unit variant like `E.A` in `sealed class E { data object A : E(); data object B :
     * E() }`.
     */
    fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Ok>

    /**
     * Serialize a newtype class like `value class Millimeters(val value: UByte)`.
     */
    fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Ok>
        where T : Serialize

    /**
     * Serialize a newtype variant like `E.N` in `sealed class E { data class N(val value: UByte) :
     * E() }`.
     */
    fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Ok>
        where T : Serialize

    /**
     * Begin to serialize a variably sized sequence. This call must be followed by zero or more calls
     * to `serializeElement`, then a call to `end`.
     */
    fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Ok>>

    /**
     * Begin to serialize a statically sized sequence whose length will be known at deserialization
     * time without looking at the serialized data.
     */
    fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Ok>>

    /**
     * Begin to serialize a tuple class like `data class Rgb(val red: UByte, val green: UByte, val
     * blue: UByte)`.
     */
    fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Ok>>

    /**
     * Begin to serialize a tuple variant like `E.T` in `sealed class E { data class T(val first:
     * UByte, val second: UByte) : E() }`.
     */
    fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Ok>>

    /**
     * Begin to serialize a map.
     */
    fun serializeMap(len: Int?): SerdeResult<SerializeMap<Ok>>

    /**
     * Begin to serialize a class like `data class Rgb(val red: UByte, val green: UByte, val blue:
     * UByte)`.
     */
    fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Ok>>

    /**
     * Begin to serialize a class variant like `E.S` in `sealed class E { data class S(val red:
     * UByte, val green: UByte, val blue: UByte) : E() }`.
     */
    fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Ok>>

    /**
     * Collect an iterator as a sequence.
     */
    fun <T> collectSeq(iter: Iterable<T>): SerdeResult<Ok>
        where T : Serialize =
        serdeCatching {
            val serializer = serializeSeq(iteratorLenHint(iter)).getOrThrow()
            for (item in iter) {
                serializer.serializeElement(item).getOrThrow()
            }
            serializer.end().getOrThrow()
        }

    /**
     * Collect an iterator as a map.
     */
    fun <K, V> collectMap(iter: Iterable<Pair<K, V>>): SerdeResult<Ok>
        where K : Serialize,
              V : Serialize =
        serdeCatching {
            val serializer = serializeMap(iteratorLenHint(iter)).getOrThrow()
            for ((key, value) in iter) {
                serializer.serializeEntry(key, value).getOrThrow()
            }
            serializer.end().getOrThrow()
        }

    /**
     * Serialize a string produced by an implementation of `toString`.
     */
    fun collectStr(value: String): SerdeResult<Ok> = serializeStr(value)

    /**
     * Determine whether `Serialize` implementations should serialize in human-readable form.
     */
    fun isHumanReadable(): Boolean = true
}
