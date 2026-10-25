package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.priv.generated.content.Content
import io.github.kotlinmania.serdecore.priv.contentaccess.*
import org.rustlang.core.slice.SharedByteSlice
import kotlin.test.*

class GeneratedContentCaptureTest {
    @Test fun capturesEveryScalarWithoutNarrowingAndRejectsInvalidScalars() {
        for (cp in listOf(0, 65, 0xd7ff, 0xe000, 0xffff, 0x10000, 0x1f680, 0x10ffff)) {
            val content = assertIs<Content.Char>(ContentVisitor.new().visitCharCodePoint(cp).getOrThrow())
            assertEquals(cp, content.value)
            assertEquals(cp, assertIs<Content.Char>(contentClone(content)).value)
        }
        for (cp in listOf(-1, 0xd800, 0xdfff, 0x110000)) {
            assertIs<SerdeResult.Failure>(ContentVisitor.new().visitCharCodePoint(cp))
        }
        assertIs<SerdeResult.Failure>(ContentVisitor.new().visitChar('\ud800'))
        assertIs<SerdeResult.Failure>(ContentVisitor.new().visitStr("\ud800"))
        assertIs<SerdeResult.Failure>(ContentVisitor.new().visitBorrowedStr("\udc00"))
    }

    @Test fun borrowedRangesSurviveCaptureAndRecursiveClone() {
        val bytes = byteArrayOf(99, 1, 2, 3, 100)
        for ((start, end) in listOf(0 to 0, 0 to 5, 1 to 4, 5 to 5)) {
            val region = SharedByteSlice.borrow(bytes, start, end)
            val captured = ContentVisitor.new().visitBorrowedByteSlice(region).getOrThrow()
            val nested = newSeqContent(listOf(newSomeContent(captured), newMapContent(listOf(
                io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry(captured, newNewtypeContent(captured)),
            ))))
            val clone = assertIs<Content.Seq>(contentClone(nested))
            val some = assertIs<Content.Bytes>(assertIs<Content.Some>(clone.value[0]).value)
            val entry = assertIs<Content.Map>(clone.value[1]).value.single()
            assertTrue(some.value.isSameRegion(region))
            assertTrue(assertIs<Content.Bytes>(entry.key).value.isSameRegion(region))
            assertTrue(assertIs<Content.Bytes>(assertIs<Content.Newtype>(entry.value).value).value.isSameRegion(region))
            assertContentEquals(bytes.copyOfRange(start, end), some.value.copyBytes())
        }
    }

    @Test fun ownedContainersAreIndependentAndBorrowedTextStaysBorrowed() {
        val bytes = byteArrayOf(1, 2, 3)
        val captured = assertIs<Content.ByteBuf>(ContentVisitor.new().visitBytes(bytes).getOrThrow())
        val cloned = assertIs<Content.ByteBuf>(contentClone(captured))
        bytes[0] = 9
        captured.__rtkField0[1] = 8u
        assertContentEquals(byteArrayOf(1, 2, 3), cloned.value)
        assertContentEquals(byteArrayOf(1, 8, 3), captured.value)
        val text = assertIs<Content.String>(ContentVisitor.new().visitString("water🚀").getOrThrow())
        val textClone = assertIs<Content.String>(contentClone(text))
        text.__rtkField0.append('!')
        assertEquals("water🚀", textClone.value)
        val borrowed = "borrowed🚀"
        val str = assertIs<Content.Str>(ContentVisitor.new().visitBorrowedStr(borrowed).getOrThrow())
        assertSame(borrowed, assertIs<Content.Str>(contentClone(str)).value)
    }

    @Test fun byteTextConversionUsesOnlyTheRegionAndStrictUtf8() {
        val bytes = byteArrayOf(-1, 0x61, -61, -87, -1)
        assertEquals("aé", contentAsStr(newBytesContent(SharedByteSlice.borrow(bytes, 1, 4))))
        assertNull(contentAsStr(newBytesContent(SharedByteSlice.borrow(bytes, 1, 3))))
        assertNull(contentAsStr(newByteBufContent(byteArrayOf(-1))))
        assertEquals("", contentAsStr(newBytesContent(SharedByteSlice.borrow(bytes, 2, 2))))
    }
}
