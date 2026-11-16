package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult

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
interface Serialize {
    /**
     * Serialize this value into the given Serde serializer.
     *
     * See the Implementing `Serialize` section of the manual for more information about how to
     * implement this method.
     */
    fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>

    /**
     * The implementation of Serialize for a statically known value type.
     * Kotlin cannot attach this interface to built-in types. Passing this
     * dictionary preserves the upstream trait bound through generic callers,
     * including JavaScript where signed numbers share a runtime representation.
     */
    interface For<in T> {
        fun <Ok> serialize(value: T, serializer: Serializer<Ok>): SerdeResult<Ok>
    }

    object Bool : For<Boolean> {
        override fun <Ok> serialize(value: Boolean, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeBool(value)
    }

    object I8 : For<Byte> {
        override fun <Ok> serialize(value: Byte, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeI8(value)
    }

    object I16 : For<Short> {
        override fun <Ok> serialize(value: Short, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeI16(value)
    }

    object I32 : For<Int> {
        override fun <Ok> serialize(value: Int, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeI32(value)
    }

    object I64 : For<Long> {
        override fun <Ok> serialize(value: Long, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeI64(value)
    }

    object U8 : For<UByte> {
        override fun <Ok> serialize(value: UByte, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeU8(value)
    }

    object U16 : For<UShort> {
        override fun <Ok> serialize(value: UShort, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeU16(value)
    }

    object U32 : For<UInt> {
        override fun <Ok> serialize(value: UInt, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeU32(value)
    }

    object U64 : For<ULong> {
        override fun <Ok> serialize(value: ULong, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeU64(value)
    }

    object F32 : For<Float> {
        override fun <Ok> serialize(value: Float, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeF32(value)
    }

    object F64 : For<Double> {
        override fun <Ok> serialize(value: Double, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeF64(value)
    }

    object Char : For<kotlin.Char> {
        override fun <Ok> serialize(value: kotlin.Char, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeChar(value)
    }

    object Str : For<String> {
        override fun <Ok> serialize(value: String, serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeStr(value)
    }
}

/** Bind the borrowed value without invoking its serializer early. */
fun <T> Serialize.For<T>.bind(value: T): Serialize {
    val implementation = this
    return object : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            implementation.serialize(value, serializer)
    }
}
