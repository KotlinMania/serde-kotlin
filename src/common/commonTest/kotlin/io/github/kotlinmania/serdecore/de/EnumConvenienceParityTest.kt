package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.I32Deserializer
import io.github.kotlinmania.serdecore.de.value.StrDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class EnumConvenienceParityTest {
    private class Variant : VariantAccess {
        var calls = 0
        override fun unitVariant(): SerdeResult<Unit> = error("unexpected unit variant")
        override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> {
            calls++
            return seed.deserialize(I32Deserializer.new(42))
        }
        override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> = error("unexpected tuple variant")
        override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = error("unexpected struct variant")
    }

    private class Enum(private val variant: Variant) : EnumAccess {
        var calls = 0
        override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> {
            calls++
            return seed.deserialize(StrDeserializer.new("Chosen")).map { it to variant }
        }
    }

    @Test
    fun variantDelegatesOnceAndPreservesPayloadAccessIdentity() {
        val variant = Variant()
        val access = Enum(variant)
        val result = access.variant(StringDeserialize).getOrThrow()
        assertEquals("Chosen", result.first)
        assertSame<VariantAccess>(variant, result.second)
        assertEquals(1, access.calls)
        assertEquals(0, variant.calls)
        assertEquals(42, result.second.newtypeVariant(I32Deserialize).getOrThrow())
        assertEquals(1, variant.calls)
    }

    @Test
    fun convenienceMethodsPreserveNullableValues() {
        val codec = object : Deserialize<Int?> {
            override fun <D> deserialize(deserializer: D): SerdeResult<Int?> where D : Deserializer = SerdeResult.success(null)
        }
        val variant = Variant()
        val access = Enum(variant)
        assertEquals(null, access.variant(codec).getOrThrow().first)
        assertEquals(null, variant.newtypeVariant(codec).getOrThrow())
        assertEquals(1, access.calls)
        assertEquals(1, variant.calls)
    }

    @Test
    fun failuresAndThrownExceptionsRetainIdentity() {
        val failure = SerdeResult.failure(SerdeError.custom("codec failure"))
        val errorCodec = object : Deserialize<Int> {
            override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer = failure
        }
        val variant = Variant()
        assertSame(failure, Enum(variant).variant(errorCodec))
        assertSame(failure, variant.newtypeVariant(errorCodec))
        val panic = IllegalStateException("codec panic")
        val panicCodec = object : Deserialize<Int> {
            override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer = throw panic
        }
        assertSame(panic, assertFailsWith<IllegalStateException> { Enum(variant).variant(panicCodec) })
        assertSame(panic, assertFailsWith<IllegalStateException> { variant.newtypeVariant(panicCodec) })
    }
}
