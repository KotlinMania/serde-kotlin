package io.github.kotlinmania.serde.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.UntaggedContent
import io.github.kotlinmania.serdecore.de.*
import io.github.kotlinmania.serdecore.de.value.BorrowedByteSliceDeserializer
import io.github.kotlinmania.serdecore.de.value.CharCodePointDeserializer
import io.github.kotlinmania.serdecore.priv.contentaccess.*
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import org.rustlang.core.slice.SharedByteSlice
import kotlin.test.*

class GeneratedContentReplayTest {
    @Test fun publicCaptureReplaysCompleteScalarsAcrossIndependentAttempts() {
        for (cp in listOf(0, 65, 0xffff, 0x10000, 0x1f680, 0x10ffff)) {
            val captured = UntaggedContent.read(CharCodePointDeserializer.new(cp)).getOrThrow()
            assertIs<SerdeResult.Failure>(captured.decode(I32Deserialize))
            repeat(2) { assertEquals(cp, captured.decode(CharCodePointDeserialize).getOrThrow()) }
            if (cp > 0xffff) assertIs<SerdeResult.Failure>(captured.decode(CharDeserialize))
            else assertEquals(cp.toChar(), captured.decode(CharDeserialize).getOrThrow())
        }
    }

    @Test fun publicCaptureAndBothReplayModesRetainBorrowedRegions() {
        val storage = byteArrayOf(99, 1, 2, 3, 100)
        for ((start, end) in listOf(0 to 0, 0 to 5, 1 to 4, 5 to 5)) {
            val region = SharedByteSlice.borrow(storage, start, end)
            val captured = UntaggedContent.read(BorrowedByteSliceDeserializer.new(region)).getOrThrow()
            repeat(2) { assertTrue(captured.decode(BorrowedByteSliceDeserialize).getOrThrow().isSameRegion(region)) }
            assertContentEquals(region.copyBytes(), captured.decode(BorrowedBytesDeserialize).getOrThrow())
            val content = ContentVisitor.new().visitBorrowedByteSlice(region).getOrThrow()
            for (d in listOf(ContentDeserializer.new(content), ContentRefDeserializer.new(content.clone()))) {
                assertTrue(BorrowedByteSliceDeserialize.deserialize(d).getOrThrow().isSameRegion(region))
            }
        }
    }

    @Test fun legacyInputsRemainUsableAndAreCopiedAtTheOwnedBoundary() {
        val bytes = byteArrayOf(1, 2, 3)
        val source = io.github.kotlinmania.serdecore.priv.Content.ByteBuf(bytes)
        val d = ContentRefDeserializer.new(source)
        bytes[0] = 9
        val observer = object : Visitor<ByteArray> {
            override fun expecting(): String = "bytes"
            override fun visitBytes(v: ByteArray): SerdeResult<ByteArray> = SerdeResult.success(v)
        }
        assertContentEquals(byteArrayOf(1, 2, 3), d.deserializeAny(observer).getOrThrow())
    }

    @Test fun replayPreservesOwnedAndBorrowedVisitorDispatch() {
        val observer = object : Visitor<String> {
            override fun expecting(): String = "a captured value"
            override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success("str:$v")
            override fun visitString(v: String): SerdeResult<String> = SerdeResult.success("string:$v")
            override fun visitBytes(v: ByteArray): SerdeResult<String> = SerdeResult.success("bytes:${v.size}")
            override fun visitByteBuf(v: ByteArray): SerdeResult<String> = SerdeResult.success("buffer:${v.size}")
        }
        val text = newStringContent("hello")
        val bytes = newByteBufContent(byteArrayOf(1, 2))
        assertEquals("string:hello", ContentDeserializer.new(text).deserializeAny(observer).getOrThrow())
        assertEquals("str:hello", ContentRefDeserializer.new(text).deserializeAny(observer).getOrThrow())
        assertEquals("buffer:2", ContentDeserializer.new(bytes).deserializeAny(observer).getOrThrow())
        assertEquals("bytes:2", ContentRefDeserializer.new(bytes).deserializeAny(observer).getOrThrow())
        assertEquals("str:🚀", ContentDeserializer.new(newCharContent(0x1f680)).deserializeAny(observer).getOrThrow())
    }

    @Test fun taggedCaptureUsesExactRangeAndPreservesScalarVariant() {
        val bytes = "--tag++".encodeToByteArray()
        val tag = SharedByteSlice.borrow(bytes, 2, 5)
        val nonTag = SharedByteSlice.borrow(bytes, 2, 4)
        val visitor = TagOrContentVisitor.new("tag")
        assertSame(TagOrContent.Tag, visitor.visitBorrowedByteSlice(tag).getOrThrow())
        val value = assertIs<TagOrContent.ContentValue>(visitor.visitBorrowedByteSlice(nonTag).getOrThrow()).value
        assertTrue(assertIs<Content.Bytes>(value).value.isSameRegion(nonTag))
        val char = assertIs<TagOrContent.ContentValue>(visitor.visitCharCodePoint(0x1f680).getOrThrow()).value
        assertEquals(0x1f680, assertIs<Content.Char>(char).value)
    }

    @Test fun invalidUtf8AndScalarDiagnosticsMatchTheActualValue() {
        val bytes = byteArrayOf(-1, 0x61, -61, -87, -1)
        assertEquals("aé", contentAsStr(newBytesContent(SharedByteSlice.borrow(bytes, 1, 4))))
        assertNull(contentAsStr(newBytesContent(SharedByteSlice.borrow(bytes, 1, 3))))
        val failure = assertIs<SerdeResult.Failure>(I32Deserialize.deserialize(ContentDeserializer.new(newCharContent(0x1f680))))
        assertTrue(failure.error.message.contains("character `🚀`"))
    }
}
