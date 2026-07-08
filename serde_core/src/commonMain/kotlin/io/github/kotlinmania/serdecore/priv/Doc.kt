// port-lint: source private/doc.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.StdError
import io.github.kotlinmania.serdecore.ser.*

// Used only by Serde documentation tests. Not public API.

internal class Error private constructor(
    private val message: String,
) : StdError {
    fun description(): String = message

    override fun toString(): String = message

    companion object {
        fun custom(message: Any?): Error = Error(message.toString())
    }
}

internal interface PrivateSerialize {
    fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
}

internal interface SerializeDocTestSerializer<Ok> : Serializer<Ok> {
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
        where T : io.github.kotlinmania.serdecore.ser.Serialize =
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
        where T : io.github.kotlinmania.serdecore.ser.Serialize =
        documentationTestError("serializeNewtypeStruct", name, value)

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Ok>
        where T : io.github.kotlinmania.serdecore.ser.Serialize =
        documentationTestError("serializeNewtypeVariant", name, variantIndex, variant, value)

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Ok>> = documentationTestError("serializeSeq", len)

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Ok>> = documentationTestError("serializeTuple", len)

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Ok>> = documentationTestError("serializeTupleStruct", name, len)

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Ok>> = documentationTestError("serializeTupleVariant", name, variantIndex, variant, len)

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Ok>> = documentationTestError("serializeMap", len)

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Ok>> = documentationTestError("serializeStruct", name, len)

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Ok>> = documentationTestError("serializeStructVariant", name, variantIndex, variant, len)
}

private fun <T> documentationTestError(
    method: String,
    vararg ignored: Any?,
): SerdeResult<T> = SerdeResult.failure(SerdeError("serde documentation test serializer method $method was invoked"))
