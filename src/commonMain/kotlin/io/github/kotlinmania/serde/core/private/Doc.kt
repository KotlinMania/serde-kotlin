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
        public fun custom(msg: Any?): Error = Error()
    }

    public fun description(): String = message.orEmpty()

    public fun fmt(formatter: Appendable): Result<Unit> =
        runCatching {
            formatter.append(toString())
            Unit
        }
}

/**
 * Private serialization interface used only inside documentation tests.
 */
public interface PrivateSerialize {
    public fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : SerError
}

/**
 * Serializer method set used only inside documentation tests.
 */
public interface SerializeDocTestSerializer<Ok, E> : Serializer<Ok, E>
    where E : SerError {
    override fun serializeBool(v: Boolean): Result<Ok> =
        documentationTestError("serializeBool", v)

    override fun serializeI8(v: Byte): Result<Ok> =
        documentationTestError("serializeI8", v)

    override fun serializeI16(v: Short): Result<Ok> =
        documentationTestError("serializeI16", v)

    override fun serializeI32(v: Int): Result<Ok> =
        documentationTestError("serializeI32", v)

    override fun serializeI64(v: Long): Result<Ok> =
        documentationTestError("serializeI64", v)

    override fun serializeU8(v: UByte): Result<Ok> =
        documentationTestError("serializeU8", v)

    override fun serializeU16(v: UShort): Result<Ok> =
        documentationTestError("serializeU16", v)

    override fun serializeU32(v: UInt): Result<Ok> =
        documentationTestError("serializeU32", v)

    override fun serializeU64(v: ULong): Result<Ok> =
        documentationTestError("serializeU64", v)

    override fun serializeF32(v: Float): Result<Ok> =
        documentationTestError("serializeF32", v)

    override fun serializeF64(v: Double): Result<Ok> =
        documentationTestError("serializeF64", v)

    override fun serializeChar(v: Char): Result<Ok> =
        documentationTestError("serializeChar", v)

    override fun serializeStr(v: String): Result<Ok> =
        documentationTestError("serializeStr", v)

    override fun serializeBytes(v: ByteArray): Result<Ok> =
        documentationTestError("serializeBytes", v)

    override fun serializeNone(): Result<Ok> =
        documentationTestError("serializeNone")

    override fun <T> serializeSome(value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeSome", value)

    override fun serializeUnit(): Result<Ok> =
        documentationTestError("serializeUnit")

    override fun serializeUnitStruct(name: String): Result<Ok> =
        documentationTestError("serializeUnitStruct", name)

    override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): Result<Ok> =
        documentationTestError("serializeUnitVariant", name, variantIndex, variant)

    override fun <T> serializeNewtypeStruct(name: String, value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeNewtypeStruct", name, value)

    override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): Result<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeNewtypeVariant", name, variantIndex, variant, value)

    override fun serializeSeq(len: Int?): Result<SerializeSeq<Ok, E>> =
        documentationTestError("serializeSeq", len)

    override fun serializeTuple(len: Int): Result<SerializeTuple<Ok, E>> =
        documentationTestError("serializeTuple", len)

    override fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<Ok, E>> =
        documentationTestError("serializeTupleStruct", name, len)

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<Ok, E>> =
        documentationTestError("serializeTupleVariant", name, variantIndex, variant, len)

    override fun serializeMap(len: Int?): Result<SerializeMap<Ok, E>> =
        documentationTestError("serializeMap", len)

    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<Ok, E>> =
        documentationTestError("serializeStruct", name, len)

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<Ok, E>> =
        documentationTestError("serializeStructVariant", name, variantIndex, variant, len)
}

private fun <T> documentationTestError(method: String, vararg ignored: Any?): Result<T> {
    return Result.failure(Error("serde documentation test serializer method $method was invoked"))
}
