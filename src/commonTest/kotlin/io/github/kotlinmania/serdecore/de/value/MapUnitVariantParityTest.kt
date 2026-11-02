package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IntoDeserializer
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MapUnitVariantParityTest {
    private object KeySeed : DeserializeSeed<Int> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            I32Deserialize.deserialize(deserializer)
    }

    private class Spy(
        private val mode: Int,
    ) : Deserializer by I32Deserializer.new(0),
        IntoDeserializer {
        var calls = 0
        val typedError = SerdeError("unit error")

        override fun intoDeserializer(): Deserializer = this

        override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> {
            calls++
            return when (mode) {
                0 -> visitor.visitUnit()
                1 -> SerdeResult.failure(typedError)
                else -> error("unit panic")
            }
        }
    }

    private fun variant(value: IntoDeserializer): VariantAccess {
        val map = mapDeserializer(listOf(MapEntry(I32Deserializer.new(1), value)).iterator())
        val (key, access) = MapAccessDeserializer.new(map).variantSeed(KeySeed).getOrThrow()
        assertEquals(1, key)
        return access
    }

    @Test
    fun acceptsUnit() {
        assertEquals(Unit, variant(Unit.intoDeserializer()).unitVariant().getOrThrow())
    }

    @Test
    fun rejectsInteger() {
        assertEquals("invalid type: integer `7`, expected unit", variant(7.intoDeserializer()).unitVariant().exceptionOrNull()?.message)
    }

    @Test
    fun rejectsBoolean() {
        assertEquals("invalid type: boolean `true`, expected unit", variant(true.intoDeserializer()).unitVariant().exceptionOrNull()?.message)
    }

    @Test
    fun rejectsString() {
        assertEquals("invalid type: string \"payload\", expected unit", variant("payload".intoDeserializer()).unitVariant().exceptionOrNull()?.message)
    }

    @Test
    fun dispatchesToUnitOnce() {
        val value = Spy(0)
        assertEquals(Unit, variant(value).unitVariant().getOrThrow())
        assertEquals(1, value.calls)
    }

    @Test
    fun retainsTypedError() {
        val value = Spy(1)
        assertSame(value.typedError, variant(value).unitVariant().exceptionOrNull())
        assertEquals(1, value.calls)
    }

    @Test
    fun propagatesDecoderPanic() {
        val value = Spy(2)
        val failure = assertFailsWith<IllegalStateException> { variant(value).unitVariant() }
        assertEquals("unit panic", failure.message)
        assertEquals(1, value.calls)
    }
}
