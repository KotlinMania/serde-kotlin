// port-lint: source serde_core/src/private/doc.rs
package io.github.kotlinmania.serde.core.`private`

import io.github.kotlinmania.serde.core.Lib
import io.github.kotlinmania.serde.core.ser.Error as SerError
import io.github.kotlinmania.serde.core.ser.SerializeMap
import io.github.kotlinmania.serde.core.ser.SerializeSeq
import io.github.kotlinmania.serde.core.ser.SerializeStruct
import io.github.kotlinmania.serde.core.ser.SerializeStructVariant
import io.github.kotlinmania.serde.core.ser.SerializeTuple
import io.github.kotlinmania.serde.core.ser.SerializeTupleStruct
import io.github.kotlinmania.serde.core.ser.SerializeTupleVariant
import io.github.kotlinmania.serde.core.ser.Serializer

// Used only by Serde documentation tests. Not public API.

/**
 * Error type used by Serde documentation tests.
 *
 * In the upstream Rust implementation, methods are `unimplemented!()` because this error is never
 * constructed at runtime; it exists to satisfy trait bounds in doctests.
 */
public class Error : RuntimeException(), SerError, Lib.Debug, Lib.Display {
}

/**
 * Kotlin equivalent of the upstream `__private_serialize!` macro.
 *
 * The macro exists to define a private `Serialize` trait used only inside documentation tests.
 */
public interface __PrivateSerialize {
    public fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : SerError
}

/**
 * Kotlin equivalent of the upstream `__serialize_unimplemented!` macros.
 *
 * Rust uses macros to generate a full set of `Serializer` methods that panic at runtime. Kotlin has
 * no macros, so the Kotlin port provides this interface with default implementations.
 */
public interface SerializeUnimplementedSerializer<Ok, E> : Serializer<Ok, E>
    where E : SerError {
    override fun serializeBool(v: Boolean): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeI8(v: Byte): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeI16(v: Short): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeI32(v: Int): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeI64(v: Long): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeU8(v: UByte): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeU16(v: UShort): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeU32(v: UInt): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeU64(v: ULong): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeF32(v: Float): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeF64(v: Double): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeChar(v: Char): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeStr(v: String): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeBytes(v: ByteArray): Result<Ok> {
        v.hashCode()
        throw NotImplementedError()
    }

    override fun serializeNone(): Result<Ok> {
        throw NotImplementedError()
    }

    override fun <T> serializeSome(value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize {
        value.hashCode()
        throw NotImplementedError()
    }

    override fun serializeUnit(): Result<Ok> {
        throw NotImplementedError()
    }

    override fun serializeUnitStruct(name: String): Result<Ok> {
        name.hashCode()
        throw NotImplementedError()
    }

    override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): Result<Ok> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        throw NotImplementedError()
    }

    override fun <T> serializeNewtypeStruct(name: String, value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize {
        name.hashCode()
        value.hashCode()
        throw NotImplementedError()
    }

    override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        value.hashCode()
        throw NotImplementedError()
    }

    override fun serializeSeq(len: Int?): Result<SerializeSeq<Ok, E>> {
        len?.hashCode()
        throw NotImplementedError()
    }

    override fun serializeTuple(len: Int): Result<SerializeTuple<Ok, E>> {
        len.hashCode()
        throw NotImplementedError()
    }

    override fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<Ok, E>> {
        name.hashCode()
        len.hashCode()
        throw NotImplementedError()
    }

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Ok, E>> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        len.hashCode()
        throw NotImplementedError()
    }

    override fun serializeMap(len: Int?): Result<SerializeMap<Ok, E>> {
        len?.hashCode()
        throw NotImplementedError()
    }

    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Ok, E>> {
        name.hashCode()
        len.hashCode()
        throw NotImplementedError()
    }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Ok, E>> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        len.hashCode()
        throw NotImplementedError()
    }
}
