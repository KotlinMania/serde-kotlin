// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * A **data format** that can serialize any data structure supported by Serde.
 *
 * The role of this interface is to define the serialization half of the Serde data model, which is
 * a way to categorize every Rust data structure into one of 29 possible types. Each method of the
 * `Serializer` interface corresponds to one of the types of the data model.
 *
 * Implementations of `Serialize` map themselves into this data model by invoking exactly one of the
 * `Serializer` methods.
 */
public interface Serializer<Ok, E>
    where E : Error {
    /**
     * Serialize a `Boolean` value.
     */
    public fun serializeBool(v: Boolean): Result<Ok>

    /**
     * Serialize a `Byte` value.
     *
     * If the format does not differentiate between `Byte` and `Long`, a reasonable implementation
     * would be to cast the value to `Long` and forward to `serializeI64`.
     */
    public fun serializeI8(v: Byte): Result<Ok>

    /**
     * Serialize a `Short` value.
     *
     * If the format does not differentiate between `Short` and `Long`, a reasonable implementation
     * would be to cast the value to `Long` and forward to `serializeI64`.
     */
    public fun serializeI16(v: Short): Result<Ok>

    /**
     * Serialize an `Int` value.
     *
     * If the format does not differentiate between `Int` and `Long`, a reasonable implementation
     * would be to cast the value to `Long` and forward to `serializeI64`.
     */
    public fun serializeI32(v: Int): Result<Ok>

    /**
     * Serialize a `Long` value.
     */
    public fun serializeI64(v: Long): Result<Ok>

    /**
     * Serialize an `i128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    public fun serializeI128(v: String): Result<Ok> {
        v.hashCode()
        return Result.failure(Error.custom("i128 is not supported"))
    }

    /**
     * Serialize a `UByte` value.
     *
     * If the format does not differentiate between `UByte` and `ULong`, a reasonable implementation
     * would be to cast the value to `ULong` and forward to `serializeU64`.
     */
    public fun serializeU8(v: UByte): Result<Ok>

    /**
     * Serialize a `UShort` value.
     *
     * If the format does not differentiate between `UShort` and `ULong`, a reasonable implementation
     * would be to cast the value to `ULong` and forward to `serializeU64`.
     */
    public fun serializeU16(v: UShort): Result<Ok>

    /**
     * Serialize a `UInt` value.
     *
     * If the format does not differentiate between `UInt` and `ULong`, a reasonable implementation
     * would be to cast the value to `ULong` and forward to `serializeU64`.
     */
    public fun serializeU32(v: UInt): Result<Ok>

    /**
     * Serialize a `ULong` value.
     */
    public fun serializeU64(v: ULong): Result<Ok>

    /**
     * Serialize a `u128` value.
     *
     * The default behavior unconditionally returns an error.
     */
    public fun serializeU128(v: String): Result<Ok> {
        v.hashCode()
        return Result.failure(Error.custom("u128 is not supported"))
    }

    /**
     * Serialize a `Float` value.
     *
     * If the format does not differentiate between `Float` and `Double`, a reasonable
     * implementation would be to cast the value to `Double` and forward to `serializeF64`.
     */
    public fun serializeF32(v: Float): Result<Ok>

    /**
     * Serialize a `Double` value.
     */
    public fun serializeF64(v: Double): Result<Ok>

    /**
     * Serialize a character.
     *
     * If the format does not support characters, it is reasonable to serialize it as a single
     * element `String` or a `UInt`.
     */
    public fun serializeChar(v: Char): Result<Ok>

    /**
     * Serialize a `String`.
     */
    public fun serializeStr(v: String): Result<Ok>

    /**
     * Serialize a chunk of raw byte data.
     */
    public fun serializeBytes(v: ByteArray): Result<Ok>

    /**
     * Serialize a `null` value.
     */
    public fun serializeNone(): Result<Ok>

    /**
     * Serialize a non-null optional value.
     */
    public fun <T> serializeSome(value: T): Result<Ok>
        where T : Serialize

    /**
     * Serialize a `Unit` value.
     */
    public fun serializeUnit(): Result<Ok>

    /**
     * Serialize a unit class like `object Unit` or `PhantomData<T>`.
     */
    public fun serializeUnitStruct(name: String): Result<Ok>

    /**
     * Serialize a unit variant like `E.A` in `sealed class E { data object A : E(); data object B :
     * E() }`.
     */
    public fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): Result<Ok>

    /**
     * Serialize a newtype class like `value class Millimeters(val value: UByte)`.
     */
    public fun <T> serializeNewtypeStruct(name: String, value: T): Result<Ok>
        where T : Serialize

    /**
     * Serialize a newtype variant like `E.N` in `sealed class E { data class N(val value: UByte) :
     * E() }`.
     */
    public fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<Ok>
        where T : Serialize

    /**
     * Begin to serialize a variably sized sequence. This call must be followed by zero or more calls
     * to `serializeElement`, then a call to `end`.
     */
    public fun serializeSeq(len: Int?): Result<SerializeSeq<Ok, E>>

    /**
     * Begin to serialize a statically sized sequence whose length will be known at deserialization
     * time without looking at the serialized data.
     */
    public fun serializeTuple(len: Int): Result<SerializeTuple<Ok, E>>

    /**
     * Begin to serialize a tuple class like `data class Rgb(val red: UByte, val green: UByte, val
     * blue: UByte)`.
     */
    public fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<Ok, E>>

    /**
     * Begin to serialize a tuple variant like `E.T` in `sealed class E { data class T(val first:
     * UByte, val second: UByte) : E() }`.
     */
    public fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Ok, E>>

    /**
     * Begin to serialize a map.
     */
    public fun serializeMap(len: Int?): Result<SerializeMap<Ok, E>>

    /**
     * Begin to serialize a class like `data class Rgb(val red: UByte, val green: UByte, val blue:
     * UByte)`.
     */
    public fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Ok, E>>

    /**
     * Begin to serialize a class variant like `E.S` in `sealed class E { data class S(val red:
     * UByte, val green: UByte, val blue: UByte) : E() }`.
     */
    public fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Ok, E>>

    /**
     * Collect an iterator as a sequence.
     */
    public fun <T> collectSeq(iter: Iterable<T>): Result<Ok>
        where T : Serialize =
        runCatching {
            val serializer = serializeSeq(iteratorLenHint(iter)).getOrThrow()
            for (item in iter) {
                serializer.serializeElement(item).getOrThrow()
            }
            serializer.end().getOrThrow()
        }

    /**
     * Collect an iterator as a map.
     */
    public fun <K, V> collectMap(iter: Iterable<Pair<K, V>>): Result<Ok>
        where K : Serialize,
              V : Serialize =
        runCatching {
            val serializer = serializeMap(iteratorLenHint(iter)).getOrThrow()
            for ((key, value) in iter) {
                serializer.serializeEntry(key, value).getOrThrow()
            }
            serializer.end().getOrThrow()
        }

    /**
     * Serialize a string produced by an implementation of `toString`.
     */
    public fun collectStr(value: Any?): Result<Ok> = serializeStr(value.toString())

    /**
     * Determine whether `Serialize` implementations should serialize in human-readable form.
     */
    public fun isHumanReadable(): Boolean = true
}
