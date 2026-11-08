// port-lint: source serde_core/src/ser/impls.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.priv.ContentDeserializer
import io.github.kotlinmania.serdecore.de.BoxedPathValue
import io.github.kotlinmania.serdecore.de.CStringDeserialize
import io.github.kotlinmania.serdecore.de.CStringValue
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.OsStringDeserialize
import io.github.kotlinmania.serdecore.de.OsStringValue
import io.github.kotlinmania.serdecore.de.PathValue
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.priv.contentaccess.newByteBufContent
import io.github.kotlinmania.serdecore.priv.contentaccess.newU8Content
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokens
import io.github.kotlinmania.serdetest.assertSerTokens
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

private class StandardValueProbe(
    private val outcome: SerdeResult<Unit>,
    private val panic: Throwable? = null,
) : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
    var calls = 0
    private fun result(): SerdeResult<Unit> {
        calls += 1
        panic?.let { throw it }
        return outcome
    }
    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = result()
    override fun serializeStr(v: String): SerdeResult<Unit> = result()
    override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): SerdeResult<Unit>
        where T : Serialize = result()
}

private class CStringSequenceAccess(
    private val bytes: List<UByte>,
    private val hint: Int? = bytes.size,
    private val stop: (() -> SerdeResult<Nothing>)? = null,
) : SeqAccess {
    var reads = 0
    var hints = 0
    private var index = 0
    override fun sizeHint(): Int? { hints += 1; return hint }
    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> {
        reads += 1
        if (index == 1 && stop != null) return stop.invoke()
        if (index == bytes.size) return SerdeResult.success(SeqElement.None)
        return seed.deserialize(ContentDeserializer.new(newU8Content(bytes[index++]))).map { SeqElement.Some(it) }
    }
}

private fun cStringSequenceDeserializer(access: SeqAccess): Deserializer =
    object : Deserializer by ContentDeserializer.new(Content.Unit) {
        override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSeq(access)
    }

class StandardValueBoundaryParityTest {
    @Test
    fun cStringUsesBytesWithoutTextDecodingOrATerminator() {
        val bytes = byteArrayOf(0x61, 0x80.toByte(), 0xff.toByte())
        val value = CStringDeserialize.deserialize(ContentDeserializer.new(newByteBufContent(bytes))).getOrThrow()
        assertSerTokens(value, listOf(Token.Bytes(bytes)))
        assertSerTokens(CStringValue(byteArrayOf()), listOf(Token.Bytes(byteArrayOf())))
    }

    @Test
    fun validPathsAndBoxedPathsDelegateToStringSerialization() {
        val path = PathValue("/usr/local/lib")
        assertSerTokens(path, listOf(Token.Str("/usr/local/lib")))
        assertSerTokens(BoxedPathValue(path), listOf(Token.Str("/usr/local/lib")))
    }

    @Test
    fun osStringPreservesVariantAndUnsignedSequenceWidths() {
        val unix = OsStringValue.Unix(listOf(0u, 128u, 255u).map(UInt::toUByte))
        val unixTokens = listOf(Token.NewtypeVariant(name = "OsString", variant = "Unix"),
            Token.Seq(len = 3), Token.U8(0u), Token.U8(128u), Token.U8(255u), Token.SeqEnd)
        val windows = OsStringValue.Windows(listOf(0xd800u, 0u, 0xffffu).map(UInt::toUShort))
        val windowsTokens = listOf(Token.NewtypeVariant(name = "OsString", variant = "Windows"),
            Token.Seq(len = 3), Token.U16(0xd800u), Token.U16(0u), Token.U16(0xffffu), Token.SeqEnd)
        assertSerTokens(unix, unixTokens)
        assertDeTokens(unix, OsStringDeserialize, unixTokens)
        assertSerTokens(windows, windowsTokens)
        assertDeTokens(windows, OsStringDeserialize, windowsTokens)
    }

    @Test
    fun standardCarriersParticipateInGenericCollectionSerialization() {
        assertSerTokens(object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                listOf(CStringValue("abc".encodeToByteArray()), CStringValue(byteArrayOf())).serialize(serializer)
        }, listOf(Token.Seq(len = 2), Token.Bytes("abc".encodeToByteArray()), Token.Bytes(byteArrayOf()), Token.SeqEnd))
    }

    @Test
    fun serializerResultsAndCallbackPanicsRetainIdentity() {
        val values: List<Serialize> = listOf(CStringValue("abc".encodeToByteArray()), PathValue("path"),
            BoxedPathValue(PathValue("path")), OsStringValue.Unix(listOf(255u)), OsStringValue.Windows(listOf(65535u)))
        for (value in values) {
            val success = SerdeResult.success(Unit)
            val successProbe = StandardValueProbe(success)
            assertSame(success, value.serialize(successProbe))
            assertEquals(1, successProbe.calls)
            val failure = SerdeResult.failure(SerdeError.custom("serializer stopped"))
            val failureProbe = StandardValueProbe(failure)
            assertSame(failure, value.serialize(failureProbe))
            assertEquals(1, failureProbe.calls)
            val panic = SerdeException(SerdeError.custom("callback panic"))
            val panicProbe = StandardValueProbe(success, panic)
            assertSame(panic, assertFailsWith<SerdeException> { value.serialize(panicProbe) })
            assertEquals(1, panicProbe.calls)
        }
    }

    @Test
    fun cStringSequenceFailureAndPanicStopWithoutConversion() {
        for (throwPanic in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("sequence stopped"))
            val panic = SerdeException(SerdeError.custom("sequence panic"))
            val access = CStringSequenceAccess(listOf(97u, 98u)) { if (throwPanic) throw panic else failure }
            val deserializer = cStringSequenceDeserializer(access)
            if (throwPanic) assertSame(panic, assertFailsWith<SerdeException> { CStringDeserialize.deserialize(deserializer) })
            else assertSame(failure, CStringDeserialize.deserialize(deserializer))
            assertEquals(2, access.reads)
            assertEquals(1, access.hints)
        }
    }

    @Test
    fun cStringChecksForNulAfterConsumingTheSequence() {
        val access = CStringSequenceAccess(listOf(97u, 0u, 99u))
        val result = CStringDeserialize.deserialize(cStringSequenceDeserializer(access))
        assertEquals("nul byte found in provided data at position: 1", result.exceptionOrNull()?.message)
        assertEquals(4, access.reads)
        assertEquals(1, access.hints)
    }

    @Test
    fun cStringAcceptsUnknownAndBoundedHintsAndRejectsNegativeBeforeReading() {
        for (hint in listOf(null, 0, 2, Int.MAX_VALUE)) {
            val access = CStringSequenceAccess(listOf(97u, 98u, 99u), hint)
            val value = CStringDeserialize.deserialize(cStringSequenceDeserializer(access)).getOrThrow()
            assertContentEquals("abc".encodeToByteArray(), value.bytes)
            assertEquals(4, access.reads)
            assertEquals(1, access.hints)
        }
        val access = CStringSequenceAccess(listOf(97u), -1)
        val result = CStringDeserialize.deserialize(cStringSequenceDeserializer(access))
        assertEquals("A Rust size hint cannot be negative", result.exceptionOrNull()?.message)
        assertEquals(0, access.reads)
    }

    @Test
    fun cStringSizeHintPanicEscapesWithoutReading() {
        val panic = SerdeException(SerdeError.custom("hint panic"))
        val access = object : SeqAccess {
            override fun sizeHint(): Int = throw panic
            override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> = error("hint failed")
        }
        assertSame(panic, assertFailsWith<SerdeException> { CStringDeserialize.deserialize(cStringSequenceDeserializer(access)) })
    }

    @Test
    fun cStringCollectsPastNulAndPreservesALaterCallbackFailureOrPanic() {
        for (throwPanic in listOf(false, true)) {
            val failure = SerdeResult.failure(SerdeError.custom("later callback stopped"))
            val panic = SerdeException(SerdeError.custom("later callback panic"))
            val access = CStringSequenceAccess(listOf(0u, 99u)) { if (throwPanic) throw panic else failure }
            val deserializer = cStringSequenceDeserializer(access)
            if (throwPanic) assertSame(panic, assertFailsWith<SerdeException> { CStringDeserialize.deserialize(deserializer) })
            else assertSame(failure, CStringDeserialize.deserialize(deserializer))
            assertEquals(2, access.reads)
        }
    }

    @Test
    fun osStringVariantIndicesMatchTheSourcePlatformBranches() {
        val variants = mutableListOf<Triple<String, UInt, String>>()
        val probe = object : Serializer<Unit> by FormatterSerializer(StringBuilder()) {
            override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): SerdeResult<Unit>
                where T : Serialize {
                variants.add(Triple(name, variantIndex, variant))
                return SerdeResult.success(Unit)
            }
        }
        OsStringValue.Unix(emptyList()).serialize(probe).getOrThrow()
        OsStringValue.Windows(emptyList()).serialize(probe).getOrThrow()
        assertEquals(listOf(Triple("OsString", 0u, "Unix"), Triple("OsString", 1u, "Windows")), variants)
    }

    @Test
    fun pathRejectsUnpairedSurrogatesBeforeCallingTheSerializer() {
        // Verify the malformed input code units before exercising the serializer.
        fun utf16(vararg units: Int): String = CharArray(units.size) { units[it].toChar() }.concatToString()
        val high = utf16(0xd800)
        val low = utf16(0xdc00)
        assertEquals(0xd800, high[0].code)
        assertEquals(0xdc00, low[0].code)
        for (text in listOf(high, low, "prefix" + high + "suffix")) {
            val probe = StandardValueProbe(SerdeResult.success(Unit))
            val result = PathValue(text).serialize(probe)
            assertEquals("path contains invalid UTF-8 characters", result.exceptionOrNull()?.message)
            assertEquals(0, probe.calls)
        }
        val valid = "prefix\uD83D\uDE00suffix"
        assertSerTokens(PathValue(valid), listOf(Token.Str(valid)))
    }
}
