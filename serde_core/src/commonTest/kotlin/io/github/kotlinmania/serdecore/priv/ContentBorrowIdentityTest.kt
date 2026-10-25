package io.github.kotlinmania.serdecore.priv

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ContentBorrowIdentityTest {
    @Test fun borrowedCaptureAndRecursiveCloneRetainStorage() {
        for (size in listOf(0,1,4,257,512)) {
            val storage = ByteArray(size) { it.toByte() }
            val borrowed = assertIs<Content.Bytes>(ContentVisitor.new().visitBorrowedBytes(storage).getOrThrow())
            assertSame(storage,borrowed.value)
            val clone = assertIs<Content.Bytes>(contentClone(borrowed))
            assertSame(storage,clone.value)
            val nested = Content.Seq(listOf(Content.Some(borrowed),Content.Map(listOf(ContentMapEntry(borrowed,Content.Newtype(borrowed))))))
            val copied = assertIs<Content.Seq>(contentClone(nested))
            assertSame(storage,assertIs<Content.Bytes>(assertIs<Content.Some>(copied.value[0]).value).value)
            val entry=assertIs<Content.Map>(copied.value[1]).value.single()
            assertSame(storage,assertIs<Content.Bytes>(entry.key).value)
            assertSame(storage,assertIs<Content.Bytes>(assertIs<Content.Newtype>(entry.value).value).value)
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
