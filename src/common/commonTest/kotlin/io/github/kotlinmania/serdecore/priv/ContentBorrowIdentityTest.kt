package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serdecore.priv.generated.content.Content
import io.github.kotlinmania.serdecore.priv.contentaccess.*
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame

class ContentBorrowIdentityTest {
    @Test fun borrowedCaptureAndRecursiveCloneRetainStorage() {
        for (size in listOf(0,1,4,257,512)) {
            val storage = ByteArray(size) { it.toByte() }
            val borrowed = assertIs<Content.Bytes>(ContentVisitor.new().visitBorrowedBytes(storage).getOrThrow())
            assertTrue(borrowed.value.isSameRegion(SharedByteSlice.borrow(storage)))
            val clone = assertIs<Content.Bytes>(contentClone(borrowed))
            assertTrue(clone.value.isSameRegion(SharedByteSlice.borrow(storage)))
            val nested = newSeqContent(listOf(newSomeContent(borrowed),newMapContent(listOf(io.github.kotlinmania.serdecore.priv.contentaccess.ContentMapEntry(borrowed,newNewtypeContent(borrowed))))))
            val copied = assertIs<Content.Seq>(contentClone(nested))
            assertTrue(assertIs<Content.Bytes>(assertIs<Content.Some>(copied.value[0]).value).value.isSameRegion(SharedByteSlice.borrow(storage)))
            val entry=assertIs<Content.Map>(copied.value[1]).value.single()
            assertTrue(assertIs<Content.Bytes>(entry.key).value.isSameRegion(SharedByteSlice.borrow(storage)))
            assertTrue(assertIs<Content.Bytes>(assertIs<Content.Newtype>(entry.value).value).value.isSameRegion(SharedByteSlice.borrow(storage)))
        }
    }
    @Test fun temporaryBytesAndOwnedCloneStillCopyStorage() {
        val storage=byteArrayOf(0,1,-1,42)
        val captured=assertIs<Content.ByteBuf>(ContentVisitor.new().visitBytes(storage).getOrThrow())
        assertNotSame(storage,captured.value)
        assertContentEquals(storage,captured.value)
        val clone=assertIs<Content.ByteBuf>(contentClone(captured))
        assertNotSame(captured.value,clone.value)
        assertContentEquals(captured.value,clone.value)
    }
}
