package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.BytesDeserializer
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import io.github.kotlinmania.serdecore.de.value.MapEntry
import io.github.kotlinmania.serdecore.de.value.StrDeserializer
import io.github.kotlinmania.serdecore.de.value.U64Deserializer
import io.github.kotlinmania.serdecore.de.value.UnitDeserializer
import io.github.kotlinmania.serdecore.de.value.mapDeserializer
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertFailsWith

class VariantIdentifierParityTest {
    private class Payload(
        private val trace: MutableList<String>,
    ) : VariantAccess {
        override fun unitVariant(): SerdeResult<Unit> {
            trace.add("unit")
            return SerdeResult.success(Unit)
        }

        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> {
            trace.add("newtype")
            return seed.deserialize(I32Deserializer.new(7))
        }

        override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> =
            SerdeResult.failure(SerdeError.custom("unexpected tuple"))

        override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
            SerdeResult.failure(SerdeError.custom("unexpected struct"))
    }

    private class Input(
        private val identifier: Deserializer,
    ) : Deserializer by UnitDeserializer.new() {
        val trace = mutableListOf<String>()

        override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
            visitor.visitEnum(
                object : EnumAccess {
                    override fun <R> variantSeed(seed: DeserializeSeed<R>): SerdeResult<Pair<R, VariantAccess>> =
                        seed.deserialize(identifier).map { it to Payload(trace) }
                },
            )
    }

    @Test
    fun boundAcceptsAllNumericVariantIndices() {
        val expected = listOf<BoundValue<Int>>(BoundValue.Unbounded, BoundValue.Included(7), BoundValue.Excluded(7))
        for ((index, value) in expected.withIndex()) {
            val input = Input(U64Deserializer.new(index.toULong()))
            assertEquals(value, boundDeserialize(I32Deserialize).deserialize(input).getOrThrow())
            assertEquals(listOf(if (index == 0) "unit" else "newtype"), input.trace)
        }
    }

    @Test
    fun resultAcceptsBothNumericVariantIndices() {
        val expected = listOf<ResultValue<Int, Int>>(ResultValue.Ok(7), ResultValue.Err(7))
        for ((index, value) in expected.withIndex()) {
            val input = Input(U64Deserializer.new(index.toULong()))
            assertEquals(value, resultDeserialize(I32Deserialize, I32Deserialize).deserialize(input).getOrThrow())
            assertEquals(listOf("newtype"), input.trace)
        }
    }

    @Test
    fun outOfRangeBoundIndexIsAnInvalidValueBeforePayloadAccess() {
        val input = Input(U64Deserializer.new(3uL))
        val failure = assertIs<SerdeResult.Failure>(boundDeserialize(I32Deserialize).deserialize(input))
        assertEquals("invalid value: integer `3`, expected `Unbounded`, `Included` or `Excluded`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun outOfRangeResultIndexIsAnInvalidValueBeforePayloadAccess() {
        val input = Input(U64Deserializer.new(2uL))
        val failure = assertIs<SerdeResult.Failure>(resultDeserialize(I32Deserialize, I32Deserialize).deserialize(input))
        assertEquals("invalid value: integer `2`, expected `Ok` or `Err`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun unknownBoundStringReportsUnknownVariantRatherThanField() {
        val input = Input(StrDeserializer.new("Other"))
        val failure = assertIs<SerdeResult.Failure>(boundDeserialize(I32Deserialize).deserialize(input))
        assertEquals("unknown variant `Other`, expected one of `Unbounded`, `Included`, `Excluded`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun malformedResultVariantBytesReportInvalidBytes() {
        val input = Input(BytesDeserializer.new(byteArrayOf(0xff.toByte())))
        val failure = assertIs<SerdeResult.Failure>(resultDeserialize(I32Deserialize, I32Deserialize).deserialize(input))
        assertEquals("invalid value: byte array, expected `Ok` or `Err`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun numericStructFieldsRemainRejected() {
        val input =
            mapDeserializer(
                listOf(
                    MapEntry(U64Deserializer.new(0uL), I32Deserializer.new(1)),
                    MapEntry(U64Deserializer.new(1uL), I32Deserializer.new(2)),
                ).iterator(),
            )
        val failure = assertIs<SerdeResult.Failure>(rangeDeserialize(I32Deserialize).deserialize(input))
        assertEquals("invalid type: integer `0`, expected `start` or `end`", failure.error.message)
    }

    @Test
    fun knownByteNamesSelectAllBoundAndResultVariants() {
        val bounds =
            listOf<Pair<String, BoundValue<Int>>>(
                "Unbounded" to BoundValue.Unbounded,
                "Included" to BoundValue.Included(7),
                "Excluded" to BoundValue.Excluded(7),
            )
        for ((name, expected) in bounds) {
            val input = Input(BytesDeserializer.new(name.encodeToByteArray()))
            assertEquals(expected, boundDeserialize(I32Deserialize).deserialize(input).getOrThrow())
            assertEquals(listOf(if (name == "Unbounded") "unit" else "newtype"), input.trace)
        }
        val results = listOf<Pair<String, ResultValue<Int, Int>>>("Ok" to ResultValue.Ok(7), "Err" to ResultValue.Err(7))
        for ((name, expected) in results) {
            val input = Input(BytesDeserializer.new(name.encodeToByteArray()))
            assertEquals(expected, resultDeserialize(I32Deserialize, I32Deserialize).deserialize(input).getOrThrow())
            assertEquals(listOf("newtype"), input.trace)
        }
    }

    @Test
    fun osStringAcceptsNumericVariantIndices() {
        assertDeTokens(
            OsStringValue.Unix(listOf(1u, 2u, 3u)), OsStringDeserialize,
            listOf(Token.Enum("OsString"), Token.U64(0uL), Token.Seq(3), Token.U8(1u), Token.U8(2u), Token.U8(3u), Token.SeqEnd),
        )
        assertDeTokens(
            OsStringValue.Windows(listOf(1u, 2u, 3u)), OsStringDeserialize,
            listOf(Token.Enum("OsString"), Token.U64(1uL), Token.Seq(3), Token.U16(1u), Token.U16(2u), Token.U16(3u), Token.SeqEnd),
        )
    }

    @Test
    fun osStringRejectsUnknownVariantBeforePayload() {
        for (identifier in listOf(StrDeserializer.new("Other"), BytesDeserializer.new("Other".encodeToByteArray()))) {
            val input = Input(identifier)
            val failure = assertIs<SerdeResult.Failure>(OsStringDeserialize.deserialize(input))
            assertEquals("unknown variant `Other`, expected `Unix` or `Windows`", failure.error.message)
            assertEquals(emptyList(), input.trace)
        }
    }

    @Test
    fun osStringRejectsUnknownIndexBeforePayload() {
        val input = Input(U64Deserializer.new(2uL))
        val failure = assertIs<SerdeResult.Failure>(OsStringDeserialize.deserialize(input))
        assertEquals("invalid value: integer `2`, expected `Unix` or `Windows`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun osStringRejectsInvalidUtf8VariantBeforePayload() {
        val input = Input(BytesDeserializer.new(byteArrayOf(0xff.toByte())))
        val failure = assertIs<SerdeResult.Failure>(OsStringDeserialize.deserialize(input))
        assertEquals("invalid value: byte array, expected `Unix` or `Windows`", failure.error.message)
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun osStringIdentifierPanicPropagatesUnchanged() {
        val panic = IllegalStateException("identifier panic")
        val input = Input(object : Deserializer by UnitDeserializer.new() {
            override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = throw panic
        })
        assertSame(panic, assertFailsWith<IllegalStateException> { OsStringDeserialize.deserialize(input) })
        assertEquals(emptyList(), input.trace)
    }

    @Test
    fun osStringIdentifierErrorPreservesIdentity() {
        val error = SerdeError("identifier error")
        val input = Input(object : Deserializer by UnitDeserializer.new() {
            override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(error)
        })
        val failure = assertIs<SerdeResult.Failure>(OsStringDeserialize.deserialize(input))
        assertSame(error, failure.error)
        assertEquals(emptyList(), input.trace)
    }
}
