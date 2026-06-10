// port-lint: source test_suite/tests/unstable/mod.rs
package io.github.kotlinmania.serde.`private`.unstable

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.core.ser.Error
import io.github.kotlinmania.serde.core.ser.Serialize
import io.github.kotlinmania.serde.core.ser.SerializeMap
import io.github.kotlinmania.serde.core.ser.SerializeSeq
import io.github.kotlinmania.serde.core.ser.SerializeStruct
import io.github.kotlinmania.serde.core.ser.SerializeStructVariant
import io.github.kotlinmania.serde.core.ser.SerializeTuple
import io.github.kotlinmania.serde.core.ser.SerializeTupleStruct
import io.github.kotlinmania.serde.core.ser.SerializeTupleVariant
import io.github.kotlinmania.serde.core.ser.Serializer
import io.github.kotlinmania.serde.serdeCatching
import kotlin.test.Test
import kotlin.test.assertEquals

private sealed class Type : Serialize {
    public data class TypeVariant(
        public val typeField: UnitValue,
    ) : Type() {
        override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): SerdeResult<Ok>
            where E : Error =
            serdeCatching {
                val state = serializer.serializeStructVariant("type", 0u, "type", 1).getOrThrow()
                state.serializeField("type", typeField).getOrThrow()
                state.end().getOrThrow()
            }
    }
}

private data object UnitValue : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): SerdeResult<Ok>
        where E : Error = serializer.serializeUnit()
}

private sealed class Token {
    public data class StructVariant(
        public val name: String,
        public val variant: String,
        public val len: Int,
    ) : Token()

    public data class Str(
        public val value: String,
    ) : Token()

    public data object Unit : Token()

    public data object StructVariantEnd : Token()
}

private data object TokenError : Error

private class TokenSerializer(
    private val tokens: MutableList<Token> = mutableListOf(),
) : Serializer<List<Token>, TokenError> {
    private fun <T> unexpected(): SerdeResult<T> = SerdeResult.failure(SerdeError("unexpected serializer method"))

    override fun serializeBool(v: Boolean): SerdeResult<List<Token>> = unexpected()

    override fun serializeI8(v: Byte): SerdeResult<List<Token>> = unexpected()

    override fun serializeI16(v: Short): SerdeResult<List<Token>> = unexpected()

    override fun serializeI32(v: Int): SerdeResult<List<Token>> = unexpected()

    override fun serializeI64(v: Long): SerdeResult<List<Token>> = unexpected()

    override fun serializeU8(v: UByte): SerdeResult<List<Token>> = unexpected()

    override fun serializeU16(v: UShort): SerdeResult<List<Token>> = unexpected()

    override fun serializeU32(v: UInt): SerdeResult<List<Token>> = unexpected()

    override fun serializeU64(v: ULong): SerdeResult<List<Token>> = unexpected()

    override fun serializeF32(v: Float): SerdeResult<List<Token>> = unexpected()

    override fun serializeF64(v: Double): SerdeResult<List<Token>> = unexpected()

    override fun serializeChar(v: Char): SerdeResult<List<Token>> = unexpected()

    override fun serializeStr(v: String): SerdeResult<List<Token>> =
        serdeCatching {
            tokens += Token.Str(v)
            tokens.toList()
        }

    override fun serializeBytes(v: ByteArray): SerdeResult<List<Token>> = unexpected()

    override fun serializeNone(): SerdeResult<List<Token>> = unexpected()

    override fun <T> serializeSome(value: T): SerdeResult<List<Token>>
        where T : Serialize = unexpected()

    override fun serializeUnit(): SerdeResult<List<Token>> =
        serdeCatching {
            tokens += Token.Unit
            tokens.toList()
        }

    override fun serializeUnitStruct(name: String): SerdeResult<List<Token>> = unexpected()

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<List<Token>> = unexpected()

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<List<Token>>
        where T : Serialize = unexpected()

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<List<Token>>
        where T : Serialize = unexpected()

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<List<Token>, TokenError>> = unexpected()

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<List<Token>, TokenError>> = unexpected()

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<List<Token>, TokenError>> = unexpected()

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<List<Token>, TokenError>> = unexpected()

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<List<Token>, TokenError>> = unexpected()

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<List<Token>, TokenError>> = unexpected()

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<List<Token>, TokenError>> =
        serdeCatching {
            tokens += Token.StructVariant(name, variant, len)
            TokenStructVariant(tokens)
        }
}

private class TokenStructVariant(
    private val tokens: MutableList<Token>,
) : SerializeStructVariant<List<Token>, TokenError> {
    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            tokens += Token.Str(key)
            value.serialize(TokenSerializer(tokens)).getOrThrow()
            Unit
        }

    override fun end(): SerdeResult<List<Token>> =
        serdeCatching {
            tokens += Token.StructVariantEnd
            tokens.toList()
        }
}

public class ModTest {
    @Test
    public fun testRawIdentifiers() {
        val value = Type.TypeVariant(typeField = UnitValue)

        assertEquals(
            listOf(
                Token.StructVariant(name = "type", variant = "type", len = 1),
                Token.Str("type"),
                Token.Unit,
                Token.StructVariantEnd,
            ),
            value.serialize(TokenSerializer()).getOrThrow(),
        )
    }
}
