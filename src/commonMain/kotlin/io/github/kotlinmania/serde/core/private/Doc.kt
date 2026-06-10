// port-lint: source serde_core/src/private/doc.rs
package io.github.kotlinmania.serde.core.`private`

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.core.Lib
import io.github.kotlinmania.serde.core.ser.*
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serde.core.ser.Error as SerError

// Used only by Serde documentation tests. Not public API.

/**
 * Error type used by Serde documentation tests.
 */
class Error(
    message: String = "serde documentation test serializer was invoked",
) : RuntimeException(message),
    SerError,
    Lib.Debug,
    Lib.Display {
    companion object {
        fun custom(msg: String): Error = Error()
    }

    fun description(): String = message.orEmpty()

    fun fmt(formatter: Appendable): SerdeResult<Unit> =
        serdeCatching {
            formatter.append(toString())
            Unit
        }
}

/**
 * Private serialization interface used only inside documentation tests.
 */
interface PrivateSerialize {
    fun <Ok, E> serialize(serializer: Serializer<Ok, E>): SerdeResult<Ok>
        where E : SerError
}

/**
 * Serializer method set used only inside documentation tests.
 */
interface SerializeDocTestSerializer<Ok, E> : Serializer<Ok, E>
    where E : SerError {
    override fun serializeBool(v: Boolean): SerdeResult<Ok> = documentationTestError("serializeBool", v)

    override fun serializeI8(v: Byte): SerdeResult<Ok> = documentationTestError("serializeI8", v)

    override fun serializeI16(v: Short): SerdeResult<Ok> = documentationTestError("serializeI16", v)

    override fun serializeI32(v: Int): SerdeResult<Ok> = documentationTestError("serializeI32", v)

    override fun serializeI64(v: Long): SerdeResult<Ok> = documentationTestError("serializeI64", v)

    override fun serializeU8(v: UByte): SerdeResult<Ok> = documentationTestError("serializeU8", v)

    override fun serializeU16(v: UShort): SerdeResult<Ok> = documentationTestError("serializeU16", v)

    override fun serializeU32(v: UInt): SerdeResult<Ok> = documentationTestError("serializeU32", v)

    override fun serializeU64(v: ULong): SerdeResult<Ok> = documentationTestError("serializeU64", v)

    override fun serializeF32(v: Float): SerdeResult<Ok> = documentationTestError("serializeF32", v)

    override fun serializeF64(v: Double): SerdeResult<Ok> = documentationTestError("serializeF64", v)

    override fun serializeChar(v: Char): SerdeResult<Ok> = documentationTestError("serializeChar", v)

    override fun serializeStr(v: String): SerdeResult<Ok> = documentationTestError("serializeStr", v)

    override fun serializeBytes(v: ByteArray): SerdeResult<Ok> = documentationTestError("serializeBytes", v)

    override fun serializeNone(): SerdeResult<Ok> = documentationTestError("serializeNone")

    override fun <T> serializeSome(value: T): SerdeResult<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeSome", value)

    override fun serializeUnit(): SerdeResult<Ok> = documentationTestError("serializeUnit")

    override fun serializeUnitStruct(name: String): SerdeResult<Ok> = documentationTestError("serializeUnitStruct", name)

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Ok> = documentationTestError("serializeUnitVariant", name, variantIndex, variant)

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeNewtypeStruct", name, value)

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Ok>
        where T : io.github.kotlinmania.serde.core.ser.Serialize =
        documentationTestError("serializeNewtypeVariant", name, variantIndex, variant, value)

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Ok, E>> = documentationTestError("serializeSeq", len)

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Ok, E>> = documentationTestError("serializeTuple", len)

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Ok, E>> = documentationTestError("serializeTupleStruct", name, len)

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Ok, E>> = documentationTestError("serializeTupleVariant", name, variantIndex, variant, len)

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Ok, E>> = documentationTestError("serializeMap", len)

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Ok, E>> = documentationTestError("serializeStruct", name, len)

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Ok, E>> = documentationTestError("serializeStructVariant", name, variantIndex, variant, len)
}

private fun <T> documentationTestError(
    method: String,
    vararg ignored: Any?,
): SerdeResult<T> = SerdeResult.failure(SerdeError("serde documentation test serializer method $method was invoked"))
