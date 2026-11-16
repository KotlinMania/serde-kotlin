package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.ResultValue
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.de.resultDeserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ResultVisitorBoundaryParityTest {
    private class Codec(
        private val label: String,
        private val trace: MutableList<String>,
    ) : Deserialize<Int> {
        val typedError = SerdeError("$label error")

        override fun <D> deserialize(deserializer: D): SerdeResult<Int> where D : Deserializer =
            I32Deserialize.deserialize(deserializer).flatMap { value ->
                trace.add("$label:$value")
                when (value) {
                    -2 -> error("$label panic")
                    -1 -> SerdeResult.failure(typedError)
                    else -> SerdeResult.success(value)
                }
            }
    }

    private class Access(
        private val panic: Boolean,
    ) : EnumAccess {
        val typedError = SerdeError("access error")

        override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> {
            if (panic) error("access panic")
            return SerdeResult.failure(typedError)
        }
    }

    private class Driver(
        private val access: Access,
    ) : Deserializer by I32Deserializer.new(0) {
        override fun <V> deserializeEnum(
            name: String,
            variants: List<String>,
            visitor: Visitor<V>,
        ): SerdeResult<V> = visitor.visitEnum(access)
    }

    private class Fixture {
        private val trace = mutableListOf<String>()
        val okCodec = Codec("ok", trace)
        val errCodec = Codec("err", trace)

        fun decode(
            key: String,
            value: Int,
        ): SerdeResult<ResultValue<Int, Int>> {
            val map = mapDeserializer(listOf(MapEntry(key.intoDeserializer(), value.intoDeserializer())).iterator())
            return resultDeserialize(okCodec, errCodec).deserialize(MapAccessDeserializer.new(map))
        }

        fun decodeAccess(access: Access): SerdeResult<ResultValue<Int, Int>> =
            resultDeserialize(okCodec, errCodec).deserialize(Driver(access))

        fun assertTrace(expected: List<String>) {
            assertEquals(expected, trace)
        }
    }

    @Test
    fun okSuccessSelectsOkCodec() {
        val fixture = Fixture()
        assertEquals(ResultValue.Ok(7), fixture.decode("Ok", 7).getOrThrow())
        fixture.assertTrace(listOf("ok:7"))
    }

    @Test
    fun errSuccessIsData() {
        val fixture = Fixture()
        assertEquals(ResultValue.Err(8), fixture.decode("Err", 8).getOrThrow())
        fixture.assertTrace(listOf("err:8"))
    }

    @Test
    fun okCodecPanicPropagates() {
        val fixture = Fixture()
        val failure = assertFailsWith<IllegalStateException> { fixture.decode("Ok", -2) }
        assertEquals("ok panic", failure.message)
        fixture.assertTrace(listOf("ok:-2"))
    }

    @Test
    fun errCodecPanicPropagates() {
        val fixture = Fixture()
        val failure = assertFailsWith<IllegalStateException> { fixture.decode("Err", -2) }
        assertEquals("err panic", failure.message)
        fixture.assertTrace(listOf("err:-2"))
    }

    @Test
    fun okTypedErrorRemainsFailure() {
        val fixture = Fixture()
        assertSame(fixture.okCodec.typedError, fixture.decode("Ok", -1).exceptionOrNull())
        fixture.assertTrace(listOf("ok:-1"))
    }

    @Test
    fun errTypedErrorRemainsFailure() {
        val fixture = Fixture()
        assertSame(fixture.errCodec.typedError, fixture.decode("Err", -1).exceptionOrNull())
        fixture.assertTrace(listOf("err:-1"))
    }

    @Test
    fun accessPanicPropagates() {
        val fixture = Fixture()
        val failure = assertFailsWith<IllegalStateException> { fixture.decodeAccess(Access(true)) }
        assertEquals("access panic", failure.message)
        fixture.assertTrace(emptyList())
    }

    @Test
    fun typedAccessErrorRemainsFailure() {
        val fixture = Fixture()
        val access = Access(false)
        assertSame(access.typedError, fixture.decodeAccess(access).exceptionOrNull())
        fixture.assertTrace(emptyList())
    }

    @Test
    fun unknownVariantSkipsPayload() {
        val fixture = Fixture()
        val failure = fixture.decode("Other", 7).exceptionOrNull()
        assertEquals("unknown variant `Other`, expected `Ok` or `Err`", failure?.message)
        fixture.assertTrace(emptyList())
    }

    @Test
    fun nullableOkIsPresentData() {
        val map = mapDeserializer(listOf(MapEntry("Ok".intoDeserializer(), Unit.intoDeserializer())).iterator())
        val codec = resultDeserialize(nullableDeserialize(I32Deserialize), I32Deserialize)
        assertEquals(ResultValue.Ok(null), codec.deserialize(MapAccessDeserializer.new(map)).getOrThrow())
    }
}
