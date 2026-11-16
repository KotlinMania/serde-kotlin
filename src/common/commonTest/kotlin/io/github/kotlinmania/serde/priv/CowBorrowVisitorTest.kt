// port-lint: tests test_suite/tests/test_borrow.rs
package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.value.BytesDeserializer
import io.github.kotlinmania.serdecore.priv.contentaccess.newByteBufContent
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

private interface VisitorDriver {
    fun <V> drive(visitor: Visitor<V>): SerdeResult<V>
}

private class RecordingDeserializer(
    private val driver: VisitorDriver,
) : Deserializer {
    var lastHint: String? = null
        private set

    private fun <V> record(
        hint: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> {
        lastHint = hint
        return driver.drive(visitor)
    }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = record("deserializeAny", visitor)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = record("deserializeBool", visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = record("deserializeI8", visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = record("deserializeI16", visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = record("deserializeI32", visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = record("deserializeI64", visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = record("deserializeI128", visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = record("deserializeU8", visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = record("deserializeU16", visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = record("deserializeU32", visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = record("deserializeU64", visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = record("deserializeU128", visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = record("deserializeF32", visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = record("deserializeF64", visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = record("deserializeChar", visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = record("deserializeStr", visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = record("deserializeString", visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = record("deserializeBytes", visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = record("deserializeByteBuf", visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = record("deserializeOption", visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = record("deserializeUnit", visitor)

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = record("deserializeUnitStruct", visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = record("deserializeNewtypeStruct", visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = record("deserializeSeq", visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = record("deserializeTuple", visitor)

    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = record("deserializeTupleStruct", visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = record("deserializeMap", visitor)

    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = record("deserializeStruct", visitor)

    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = record("deserializeEnum", visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = record("deserializeIdentifier", visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = record("deserializeIgnoredAny", visitor)
}

private class TransientBytesDriver(
    private val scratch: ByteArray,
    private val wipeAfterVisit: Boolean = false,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> {
        val result = visitor.visitBytes(scratch)
        if (wipeAfterVisit) {
            scratch.fill(0)
        }
        return result
    }
}

private class BorrowedBytesDriver(
    private val borrowed: ByteArray,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBorrowedBytes(borrowed)
}

private class OwnedByteBufDriver(
    private val owned: ByteArray,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitByteBuf(owned)
}

private class TransientStrDriver(
    private val value: String,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitStr(value)
}

private class BorrowedStrDriver(
    private val borrowed: String,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBorrowedStr(borrowed)
}

private class OwnedStringDriver(
    private val owned: String,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitString(owned)
}

private class CharDriver(
    private val value: Char,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitChar(value)
}

private class CodePointDriver(
    private val codePoint: Int,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitCharCodePoint(codePoint)
}

private class I32Driver(
    private val value: Int,
) : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitI32(value)
}

private object EmptySeqAccess : SeqAccess {
    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
        SerdeResult.success(SeqElement.None)
}

private object EmptySeqDriver : VisitorDriver {
    override fun <V> drive(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSeq(EmptySeqAccess)
}

class CowBorrowVisitorTest {
    @Test
    fun borrowCowBytesCopiesTransientBytesAndIsolatesResultFromSourceMutation() {
        val scratchDuringDeserialize = byteArrayOf(0x10, 0x20, 0x30, 0x40)
        val wipeDeserializer = RecordingDeserializer(TransientBytesDriver(scratchDuringDeserialize, wipeAfterVisit = true))
        val fromWipedScratch = borrowCowBytes(wipeDeserializer).getOrThrow()

        assertEquals("deserializeBytes", wipeDeserializer.lastHint)
        assertNotSame(scratchDuringDeserialize, fromWipedScratch)
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00), scratchDuringDeserialize)
        assertContentEquals(byteArrayOf(0x10, 0x20, 0x30, 0x40), fromWipedScratch)

        val callerMutableSource = byteArrayOf(1, 2, 3, 4, 5)
        val transientDeserializer = RecordingDeserializer(TransientBytesDriver(callerMutableSource))
        val ownedCopy = borrowCowBytes(transientDeserializer).getOrThrow()

        assertEquals("deserializeBytes", transientDeserializer.lastHint)
        assertNotSame(callerMutableSource, ownedCopy)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), ownedCopy)

        callerMutableSource[0] = 99
        callerMutableSource[4] = -1
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), ownedCopy)

        val valueBytesSource = byteArrayOf(7, 8, 9)
        val fromValueDeserializer = borrowCowBytes(BytesDeserializer.new(valueBytesSource)).getOrThrow()
        assertNotSame(valueBytesSource, fromValueDeserializer)
        assertContentEquals(byteArrayOf(7, 8, 9), fromValueDeserializer)
        valueBytesSource[1] = 42
        assertContentEquals(byteArrayOf(7, 8, 9), fromValueDeserializer)

        val emptyTransient = byteArrayOf()
        val emptyOwnedCopy = borrowCowBytes(RecordingDeserializer(TransientBytesDriver(emptyTransient))).getOrThrow()
        assertNotSame(emptyTransient, emptyOwnedCopy)
        assertContentEquals(byteArrayOf(), emptyOwnedCopy)
    }

    @Test
    fun borrowCowBytesPreservesBorrowedAndOwnedByteBufCallbackIdentity() {
        val borrowed = byteArrayOf(0x41, 0x42, 0x43)
        val borrowedDeserializer = RecordingDeserializer(BorrowedBytesDriver(borrowed))
        val borrowedResult = borrowCowBytes(borrowedDeserializer).getOrThrow()

        assertEquals("deserializeBytes", borrowedDeserializer.lastHint)
        assertSame(borrowed, borrowedResult)
        assertContentEquals(byteArrayOf(0x41, 0x42, 0x43), borrowedResult)

        val privBorrowed = byteArrayOf(0x51, 0x52)
        val privBorrowedResult = borrowCowBytes(BorrowedBytesDeserializer.new(privBorrowed)).getOrThrow()
        assertSame(privBorrowed, privBorrowedResult)

        val ownedBuf = byteArrayOf(0x61, 0x62, 0x63, 0x64)
        val ownedBufDeserializer = RecordingDeserializer(OwnedByteBufDriver(ownedBuf))
        val ownedBufResult = borrowCowBytes(ownedBufDeserializer).getOrThrow()

        assertEquals("deserializeBytes", ownedBufDeserializer.lastHint)
        assertSame(ownedBuf, ownedBufResult)
        assertContentEquals(byteArrayOf(0x61, 0x62, 0x63, 0x64), ownedBufResult)

        val contentByteBufResult =
            borrowCowBytes(ContentDeserializer.new(newByteBufContent(byteArrayOf(0x71, 0x72, 0x73)))).getOrThrow()
        assertContentEquals(byteArrayOf(0x71, 0x72, 0x73), contentByteBufResult)
    }

    @Test
    fun borrowCowBytesConvertsStringsAndCharsToUtf8BytesAndRejectsNonByteInputs() {
        val sample = "héllo 🚀"
        val expectedUtf8 = sample.encodeToByteArray()

        val strDeserializer = RecordingDeserializer(TransientStrDriver(sample))
        assertContentEquals(expectedUtf8, borrowCowBytes(strDeserializer).getOrThrow())
        assertEquals("deserializeBytes", strDeserializer.lastHint)

        val borrowedStrDeserializer = RecordingDeserializer(BorrowedStrDriver(sample))
        assertContentEquals(expectedUtf8, borrowCowBytes(borrowedStrDeserializer).getOrThrow())
        assertEquals("deserializeBytes", borrowedStrDeserializer.lastHint)

        val ownedStringDeserializer = RecordingDeserializer(OwnedStringDriver(sample))
        assertContentEquals(expectedUtf8, borrowCowBytes(ownedStringDeserializer).getOrThrow())
        assertEquals("deserializeBytes", ownedStringDeserializer.lastHint)

        assertContentEquals(
            "λ".encodeToByteArray(),
            borrowCowBytes(RecordingDeserializer(CharDriver('λ'))).getOrThrow(),
        )
        assertContentEquals(
            "🚀".encodeToByteArray(),
            borrowCowBytes(RecordingDeserializer(CodePointDriver(0x1F680))).getOrThrow(),
        )

        val seqError = borrowCowBytes(RecordingDeserializer(EmptySeqDriver)).exceptionOrNull()!!
        assertEquals("invalid type: sequence, expected a byte array", seqError.message)

        val intError = borrowCowBytes(RecordingDeserializer(I32Driver(9))).exceptionOrNull()!!
        assertEquals("invalid type: integer `9`, expected a byte array", intError.message)
    }

    @Test
    fun borrowCowStrPreservesStringsDecodesValidUtf8BytesAndStrictlyRejectsInvalidUtf8() {
        val transientStr = "transient-str"
        val strDeserializer = RecordingDeserializer(TransientStrDriver(transientStr))
        assertEquals(transientStr, borrowCowStr(strDeserializer).getOrThrow())
        assertEquals("deserializeStr", strDeserializer.lastHint)

        val borrowedStr = StringBuilder("borrowed").append("-str").toString()
        val borrowedStrDeserializer = RecordingDeserializer(BorrowedStrDriver(borrowedStr))
        val borrowedStrResult = borrowCowStr(borrowedStrDeserializer).getOrThrow()
        assertEquals("deserializeStr", borrowedStrDeserializer.lastHint)
        assertSame(borrowedStr, borrowedStrResult)

        val ownedStr = StringBuilder("owned").append("-string").toString()
        val ownedStrDeserializer = RecordingDeserializer(OwnedStringDriver(ownedStr))
        val ownedStrResult = borrowCowStr(ownedStrDeserializer).getOrThrow()
        assertEquals("deserializeStr", ownedStrDeserializer.lastHint)
        assertSame(ownedStr, ownedStrResult)

        assertEquals("Ω", borrowCowStr(RecordingDeserializer(CharDriver('Ω'))).getOrThrow())
        assertEquals("🚀", borrowCowStr(RecordingDeserializer(CodePointDriver(0x1F680))).getOrThrow())

        val validText = "λ-serde-🚀"
        val transientUtf8 = validText.encodeToByteArray()
        val fromTransientBytes =
            borrowCowStr(RecordingDeserializer(TransientBytesDriver(transientUtf8, wipeAfterVisit = true))).getOrThrow()
        assertEquals(validText, fromTransientBytes)
        assertContentEquals(ByteArray(transientUtf8.size), transientUtf8)

        val borrowedUtf8 = validText.encodeToByteArray()
        val fromBorrowedBytes = borrowCowStr(RecordingDeserializer(BorrowedBytesDriver(borrowedUtf8))).getOrThrow()
        borrowedUtf8.fill(0x3F)
        assertEquals(validText, fromBorrowedBytes)

        val ownedUtf8 = validText.encodeToByteArray()
        val fromOwnedByteBuf = borrowCowStr(RecordingDeserializer(OwnedByteBufDriver(ownedUtf8))).getOrThrow()
        ownedUtf8.fill(0x3F)
        assertEquals(validText, fromOwnedByteBuf)

        val invalidSequences =
            listOf(
                byteArrayOf(0xFF.toByte()),
                byteArrayOf(0xC3.toByte()),
                byteArrayOf(0xC0.toByte(), 0xAF.toByte()),
                byteArrayOf(0xED.toByte(), 0xA0.toByte(), 0x80.toByte()),
            )
        for (invalid in invalidSequences) {
            val transientErr =
                borrowCowStr(RecordingDeserializer(TransientBytesDriver(invalid.copyOf()))).exceptionOrNull()!!
            assertEquals("invalid value: byte array, expected a string", transientErr.message)

            val borrowedErr =
                borrowCowStr(RecordingDeserializer(BorrowedBytesDriver(invalid.copyOf()))).exceptionOrNull()!!
            assertEquals("invalid value: byte array, expected a string", borrowedErr.message)

            val byteBufErr =
                borrowCowStr(RecordingDeserializer(OwnedByteBufDriver(invalid.copyOf()))).exceptionOrNull()!!
            assertEquals("invalid value: byte array, expected a string", byteBufErr.message)
        }

        val intError = borrowCowStr(RecordingDeserializer(I32Driver(42))).exceptionOrNull()!!
        assertEquals("invalid type: integer `42`, expected a string", intError.message)
    }
}
