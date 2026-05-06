// port-lint: source serde_core/src/ser/mod.rs
package io.github.kotlinmania.serde.core.ser

/**
 * Generic data structure serialization framework.
 *
 * The two most important interfaces in this package are `Serialize` and `Serializer`.
 *
 * - **A type that implements `Serialize` is a data structure** that can be serialized to any data
 *   format supported by Serde, and conversely
 * - **A type that implements `Serializer` is a data format** that can serialize any data structure
 *   supported by Serde.
 *
 * # The Serialize interface
 *
 * Serde provides `Serialize` implementations for many Kotlin primitive and standard library types.
 * All of these can be serialized using Serde out of the box.
 *
 * Additionally, Serde provides code generation to automatically generate `Serialize`
 * implementations for classes and sealed types in your program. See the derive section of the
 * manual for how to use this.
 *
 * In rare cases it may be necessary to implement `Serialize` manually for some type in your
 * program. See the Implementing `Serialize` section of the manual for more about this.
 *
 * Third-party crates may provide `Serialize` implementations for types that they expose. For
 * example the `linked-hash-map` crate provides a `LinkedHashMap<K, V>` type that is serializable by
 * Serde because the crate provides an implementation of `Serialize` for it.
 *
 * # The Serializer interface
 *
 * `Serializer` implementations are provided by third-party crates, for example `serde_json`,
 * `serde_yaml` and `postcard`.
 *
 * A partial list of well-maintained formats is given on the Serde website.
 */

/**
 * Either a re-export of `std::error::Error` or a new identical interface, depending on whether
 * Serde's "std" feature is enabled.
 */
public interface StdError {
    /**
     * The underlying cause of this error, if any.
     */
    public fun source(): StdError? = null
}

public class SerdeSerializationException(message: String) : IllegalStateException(message)

/**
 * Interface used by `Serialize` implementations to generically construct errors belonging to the
 * `Serializer` against which they are currently running.
 *
 * # Example implementation
 *
 * The example data format presented on the website shows an error type appropriate for a basic JSON
 * data format.
 */
public interface Error : StdError {
    public companion object {
        /**
         * Used when a `Serialize` implementation encounters any error while serializing a type.
         *
         * The message should not be capitalized and should not end with a period.
         *
         * For example, a filesystem `Path` may refuse to serialize itself if it contains invalid
         * UTF-8 data.
         */
        public fun custom(msg: Any?): Throwable = SerdeSerializationException(msg.toString())
    }
}

/**
 * A **data structure** that can be serialized into any data format supported by Serde.
 *
 * Serde provides `Serialize` implementations for many Kotlin primitive and standard library types.
 * All of these can be serialized using Serde out of the box.
 *
 * Additionally, Serde provides code generation to automatically generate `Serialize`
 * implementations for classes and sealed types in your program. See the derive section of the
 * manual for how to use this.
 *
 * In rare cases it may be necessary to implement `Serialize` manually for some type in your
 * program. See the Implementing `Serialize` section of the manual for more about this.
 *
 * Third-party crates may provide `Serialize` implementations for types that they expose. For
 * example the `linked-hash-map` crate provides a `LinkedHashMap<K, V>` type that is serializable by
 * Serde because the crate provides an implementation of `Serialize` for it.
 */
public interface Serialize {
    /**
     * Serialize this value into the given Serde serializer.
     *
     * See the Implementing `Serialize` section of the manual for more information about how to
     * implement this method.
     */
    public fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error
}

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
    public fun serializeI128(v: String): Result<Ok> =
        Result.failure(Error.custom("i128 is not supported"))

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
    public fun serializeU128(v: String): Result<Ok> =
        Result.failure(Error.custom("u128 is not supported"))

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
     *
     * Enables serializers to serialize byte arrays more compactly or more efficiently than other
     * types of arrays. If no efficient implementation is available, a reasonable implementation
     * would be to forward to `serializeSeq`.
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
     *
     * A reasonable implementation would be to forward to `serializeUnit`.
     */
    public fun serializeUnitStruct(name: String): Result<Ok>

    /**
     * Serialize a unit variant like `E.A` in `sealed class E { data object A : E(); data object B :
     * E() }`.
     *
     * The `name` is the name of the sealed type, the `variantIndex` is the index of this variant
     * within the sealed type, and the `variant` is the name of the variant.
     */
    public fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): Result<Ok>

    /**
     * Serialize a newtype class like `value class Millimeters(val value: UByte)`.
     *
     * Serializers are encouraged to treat newtype classes as insignificant wrappers around the data
     * they contain. A reasonable implementation would be to forward to `value.serialize(this)`.
     */
    public fun <T> serializeNewtypeStruct(name: String, value: T): Result<Ok>
        where T : Serialize

    /**
     * Serialize a newtype variant like `E.N` in `sealed class E { data class N(val value: UByte) :
     * E() }`.
     *
     * The `name` is the name of the sealed type, the `variantIndex` is the index of this variant
     * within the sealed type, and the `variant` is the name of the variant. The `value` is the data
     * contained within this newtype variant.
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
     *
     * The argument is the number of elements in the sequence, which may or may not be computable
     * before the sequence is iterated. Some serializers only support sequences whose length is known
     * up front.
     */
    public fun serializeSeq(len: Int?): Result<SerializeSeq<Ok, E>>

    /**
     * Begin to serialize a statically sized sequence whose length will be known at deserialization
     * time without looking at the serialized data. This call must be followed by zero or more calls
     * to `serializeElement`, then a call to `end`.
     */
    public fun serializeTuple(len: Int): Result<SerializeTuple<Ok, E>>

    /**
     * Begin to serialize a tuple class like `data class Rgb(val red: UByte, val green: UByte, val
     * blue: UByte)`. This call must be followed by zero or more calls to `serializeField`, then a
     * call to `end`.
     *
     * The `name` is the name of the tuple class and the `len` is the number of data fields that will
     * be serialized.
     */
    public fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<Ok, E>>

    /**
     * Begin to serialize a tuple variant like `E.T` in `sealed class E { data class T(val first:
     * UByte, val second: UByte) : E() }`. This call must be followed by zero or more calls to
     * `serializeField`, then a call to `end`.
     *
     * The `name` is the name of the sealed type, the `variantIndex` is the index of this variant
     * within the sealed type, the `variant` is the name of the variant, and the `len` is the number
     * of data fields that will be serialized.
     */
    public fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Ok, E>>

    /**
     * Begin to serialize a map. This call must be followed by zero or more calls to `serializeKey`
     * and `serializeValue`, then a call to `end`.
     *
     * The argument is the number of elements in the map, which may or may not be computable before
     * the map is iterated. Some serializers only support maps whose length is known up front.
     */
    public fun serializeMap(len: Int?): Result<SerializeMap<Ok, E>>

    /**
     * Begin to serialize a class like `data class Rgb(val red: UByte, val green: UByte, val blue:
     * UByte)`. This call must be followed by zero or more calls to `serializeField`, then a call to
     * `end`.
     *
     * The `name` is the name of the class and the `len` is the number of data fields that will be
     * serialized. `len` does not include fields which are skipped with `SerializeStruct.skipField`.
     */
    public fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Ok, E>>

    /**
     * Begin to serialize a class variant like `E.S` in `sealed class E { data class S(val red:
     * UByte, val green: UByte, val blue: UByte) : E() }`. This call must be followed by zero or
     * more calls to `serializeField`, then a call to `end`.
     *
     * The `name` is the name of the sealed type, the `variantIndex` is the index of this variant
     * within the sealed type, the `variant` is the name of the variant, and the `len` is the number
     * of data fields that will be serialized. `len` does not include fields which are skipped with
     * `SerializeStructVariant.skipField`.
     */
    public fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Ok, E>>

    /**
     * Collect an iterator as a sequence.
     *
     * The default implementation serializes each item yielded by the iterator using `serializeSeq`.
     * Implementors should not need to override this method.
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
     *
     * The default implementation serializes each pair yielded by the iterator using `serializeMap`.
     * Implementors should not need to override this method.
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
     *
     * The default implementation builds a heap-allocated `String` and delegates to `serializeStr`.
     * Serializers are encouraged to provide a more efficient implementation if possible.
     */
    public fun collectStr(value: Any?): Result<Ok> = serializeStr(value.toString())

    /**
     * Determine whether `Serialize` implementations should serialize in human-readable form.
     *
     * Some types have a human-readable form that may be somewhat expensive to construct, as well as
     * a binary form that is compact and efficient. Generally text-based formats like JSON and YAML
     * will prefer to use the human-readable one and binary formats like Postcard will prefer the
     * compact one.
     *
     * The default implementation of this method returns `true`. Data formats may override this to
     * `false` to request a compact form for types that support one. Note that modifying this method
     * to change a format from human-readable to compact or vice versa should be regarded as a
     * breaking change, as a value serialized in human-readable mode is not required to deserialize
     * from the same data in compact mode.
     */
    public fun isHumanReadable(): Boolean = true
}

/**
 * Returned from `Serializer.serializeSeq`.
 */
public interface SerializeSeq<Ok, E>
    where E : Error {
    /**
     * Serialize a sequence element.
     */
    public fun <T> serializeElement(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a sequence.
     */
    public fun end(): Result<Ok>
}

/**
 * Returned from `Serializer.serializeTuple`.
 */
public interface SerializeTuple<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple element.
     */
    public fun <T> serializeElement(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple.
     */
    public fun end(): Result<Ok>
}

/**
 * Returned from `Serializer.serializeTupleStruct`.
 */
public interface SerializeTupleStruct<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple class field.
     */
    public fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple class.
     */
    public fun end(): Result<Ok>
}

/**
 * Returned from `Serializer.serializeTupleVariant`.
 */
public interface SerializeTupleVariant<Ok, E>
    where E : Error {
    /**
     * Serialize a tuple variant field.
     */
    public fun <T> serializeField(value: T): Result<Unit>
        where T : Serialize

    /**
     * Finish serializing a tuple variant.
     */
    public fun end(): Result<Ok>
}

/**
 * Returned from `Serializer.serializeMap`.
 */
public interface SerializeMap<Ok, E>
    where E : Error {
    /**
     * Serialize a map key.
     *
     * If possible, `Serialize` implementations are encouraged to use `serializeEntry` instead as it
     * may be implemented more efficiently in some formats compared to a pair of calls to
     * `serializeKey` and `serializeValue`.
     */
    public fun <T> serializeKey(key: T): Result<Unit>
        where T : Serialize

    /**
     * Serialize a map value.
     *
     * # Panics
     *
     * Calling `serializeValue` before `serializeKey` is incorrect and is allowed to panic or
     * produce bogus results.
     */
    public fun <T> serializeValue(value: T): Result<Unit>
        where T : Serialize

    /**
     * Serialize a map entry consisting of a key and a value.
     *
     * Some `Serialize` types are not able to hold a key and value in memory at the same time so
     * `SerializeMap` implementations are required to support `serializeKey` and `serializeValue`
     * individually. The `serializeEntry` method allows serializers to optimize for the case where
     * key and value are both available. `Serialize` implementations are encouraged to use
     * `serializeEntry` if possible.
     *
     * The default implementation delegates to `serializeKey` and `serializeValue`. This is
     * appropriate for serializers that do not care about performance or are not able to optimize
     * `serializeEntry` any better than this.
     */
    public fun <K, V> serializeEntry(key: K, value: V): Result<Unit>
        where K : Serialize,
              V : Serialize =
        runCatching {
            serializeKey(key).getOrThrow()
            serializeValue(value).getOrThrow()
        }

    /**
     * Finish serializing a map.
     */
    public fun end(): Result<Ok>
}

/**
 * Returned from `Serializer.serializeStruct`.
 */
public interface SerializeStruct<Ok, E>
    where E : Error {
    /**
     * Serialize a class field.
     */
    public fun <T> serializeField(key: String, value: T): Result<Unit>
        where T : Serialize

    /**
     * Indicate that a class field has been skipped.
     *
     * The default implementation does nothing.
     */
    public fun skipField(key: String): Result<Unit> = Result.success(Unit)

    /**
     * Finish serializing a class.
     */
    public fun end(): Result<Ok>
}

/**
 * Returned from `Serializer.serializeStructVariant`.
 */
public interface SerializeStructVariant<Ok, E>
    where E : Error {
    /**
     * Serialize a class variant field.
     */
    public fun <T> serializeField(key: String, value: T): Result<Unit>
        where T : Serialize

    /**
     * Indicate that a class variant field has been skipped.
     *
     * The default implementation does nothing.
     */
    public fun skipField(key: String): Result<Unit> = Result.success(Unit)

    /**
     * Finish serializing a class variant.
     */
    public fun end(): Result<Ok>
}

private fun iteratorLenHint(iter: Iterable<*>): Int? =
    if (iter is Collection<*>) iter.size else null
