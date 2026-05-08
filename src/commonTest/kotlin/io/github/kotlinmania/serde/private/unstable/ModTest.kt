// port-lint: source test_suite/tests/unstable/mod.rs
package io.github.kotlinmania.serde.`private`.unstable

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
import kotlin.test.Test
import kotlin.test.assertEquals

private sealed class Type : Serialize {
    public data class TypeVariant(
        public val typeField: UnitValue,
    ) : Type() {
        override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
            where E : Error =
            runCatching {
                val state = serializer.serializeStructVariant("type", 0u, "type", 1).getOrThrow()
                state.serializeField("type", typeField).getOrThrow()
                state.end().getOrThrow()
            }
    }
}

private data object UnitValue : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        serializer.serializeUnit()
}

private sealed class Token {
    public data class StructVariant(
        public val name: String,
        public val variant: String,
        public val len: Int,
    ) : Token()

    public data class Str(public val value: String) : Token()
    public data object Unit : Token()
    public data object StructVariantEnd : Token()
}

private data object TokenError : Error

private class TokenSerializer(
    private val tokens: MutableList<Token> = mutableListOf(),
) : Serializer<List<Token>, TokenError> {
    private fun <T> unexpected(): Result<T> =
        Result.failure(AssertionError("unexpected serializer method"))

    override fun serializeBool(v: Boolean): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeI8(v: Byte): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeI16(v: Short): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeI32(v: Int): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeI64(v: Long): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeU8(v: UByte): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeU16(v: UShort): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeU32(v: UInt): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeU64(v: ULong): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeF32(v: Float): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeF64(v: Double): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeChar(v: Char): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeStr(v: String): Result<List<Token>> =
        runCatching {
            tokens += Token.Str(v)
            tokens.toList()
        }

    override fun serializeBytes(v: ByteArray): Result<List<Token>> {
        v.hashCode()
        return unexpected()
    }

    override fun serializeNone(): Result<List<Token>> =
        unexpected()

    override fun <T> serializeSome(value: T): Result<List<Token>>
        where T : Serialize {
        value.hashCode()
        return unexpected()
    }

    override fun serializeUnit(): Result<List<Token>> =
        runCatching {
            tokens += Token.Unit
            tokens.toList()
        }

    override fun serializeUnitStruct(name: String): Result<List<Token>> {
        name.hashCode()
        return unexpected()
    }

    override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): Result<List<Token>> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        return unexpected()
    }

    override fun <T> serializeNewtypeStruct(name: String, value: T): Result<List<Token>>
        where T : Serialize {
        name.hashCode()
        value.hashCode()
        return unexpected()
    }

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<List<Token>>
        where T : Serialize {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        value.hashCode()
        return unexpected()
    }

    override fun serializeSeq(len: Int?): Result<SerializeSeq<List<Token>, TokenError>> {
        len.hashCode()
        return unexpected()
    }

    override fun serializeTuple(len: Int): Result<SerializeTuple<List<Token>, TokenError>> {
        len.hashCode()
        return unexpected()
    }

    override fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<List<Token>, TokenError>> {
        name.hashCode()
        len.hashCode()
        return unexpected()
    }

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<List<Token>, TokenError>> {
        name.hashCode()
        variantIndex.hashCode()
        variant.hashCode()
        len.hashCode()
        return unexpected()
    }

    override fun serializeMap(len: Int?): Result<SerializeMap<List<Token>, TokenError>> {
        len.hashCode()
        return unexpected()
    }

    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<List<Token>, TokenError>> {
        name.hashCode()
        len.hashCode()
        return unexpected()
    }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<List<Token>, TokenError>> =
        runCatching {
            variantIndex.hashCode()
            tokens += Token.StructVariant(name, variant, len)
            TokenStructVariant(tokens)
        }
}

private class TokenStructVariant(
    private val tokens: MutableList<Token>,
) : SerializeStructVariant<List<Token>, TokenError> {
    override fun <T> serializeField(key: String, value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            tokens += Token.Str(key)
            value.serialize(TokenSerializer(tokens)).getOrThrow()
            Unit
        }

    override fun end(): Result<List<Token>> =
        runCatching {
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
