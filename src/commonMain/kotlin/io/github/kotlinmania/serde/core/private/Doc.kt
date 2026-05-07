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
 */
public class Error(
    message: String = "serde documentation test serializer was invoked",
) : RuntimeException(message), SerError, Lib.Debug, Lib.Display {
    public companion object {
        public fun custom(msg: Any?): Error = Error(msg.toString())
    }

    public fun description(): String = message.orEmpty()

    public fun fmt(formatter: Appendable): Result<Unit> =
        runCatching {
            formatter.append(toString())
            Unit
        }
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
 * Kotlin equivalent of the upstream documentation-test serializer macros.
 *
 * Rust uses macros to generate a full set of `Serializer` methods for documentation tests. Kotlin
 * has no macros, so the Kotlin port provides this interface with default implementations.
 */
public interface SerializeDocTestSerializer<Ok, E> : Serializer<Ok, E>
    where E : SerError {
    override fun serializeBool(v: Boolean): Result<Ok> =
        documentationTestError("serializeBool")

    override fun serializeI8(v: Byte): Result<Ok> =
        documentationTestError("serializeI8")

    override fun serializeI16(v: Short): Result<Ok> =
        documentationTestError("serializeI16")

    override fun serializeI32(v: Int): Result<Ok> =
        documentationTestError("serializeI32")

    override fun serializeI64(v: Long): Result<Ok> =
        documentationTestError("serializeI64")

    override fun serializeU8(v: UByte): Result<Ok> =
        documentationTestError("serializeU8")

    override fun serializeU16(v: UShort): Result<Ok> =
        documentationTestError("serializeU16")

    override fun serializeU32(v: UInt): Result<Ok> =
        documentationTestError("serializeU32")

    override fun serializeU64(v: ULong): Result<Ok> =
        documentationTestError("serializeU64")

    override fun serializeF32(v: Float): Result<Ok> =
        documentationTestError("serializeF32")

    override fun serializeF64(v: Double): Result<Ok> =
        documentationTestError("serializeF64")

    override fun serializeChar(v: Char): Result<Ok> =
        documentationTestError("serializeChar")

    override fun serializeStr(v: String): Result<Ok> =
        documentationTestError("serializeStr")

    override fun serializeBytes(v: ByteArray): Result<Ok> =
        documentationTestError("serializeBytes")

    override fun serializeNone(): Result<Ok> =
        documentationTestError("serializeNone")

    override fun <T> serializeSome(value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeSome")

    override fun serializeUnit(): Result<Ok> =
        documentationTestError("serializeUnit")

    override fun serializeUnitStruct(name: String): Result<Ok> =
        documentationTestError("serializeUnitStruct")

    override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): Result<Ok> =
        documentationTestError("serializeUnitVariant")

    override fun <T> serializeNewtypeStruct(name: String, value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeNewtypeStruct")

    override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeNewtypeVariant")

    override fun serializeSeq(len: Int?): Result<SerializeSeq<Ok, E>> =
        documentationTestError("serializeSeq")

    override fun serializeTuple(len: Int): Result<SerializeTuple<Ok, E>> =
        documentationTestError("serializeTuple")

    override fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<Ok, E>> =
        documentationTestError("serializeTupleStruct")

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Ok, E>> =
        documentationTestError("serializeTupleVariant")

    override fun serializeMap(len: Int?): Result<SerializeMap<Ok, E>> =
        documentationTestError("serializeMap")

    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Ok, E>> =
        documentationTestError("serializeStruct")

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Ok, E>> =
        documentationTestError("serializeStructVariant")
}

private fun <T> documentationTestError(method: String): Result<T> =
    Result.failure(Error("serde documentation test serializer method $method was invoked"))
