package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serdecore.priv.generated.string.from_utf8_lossy
import org.rustlang.alloc.borrow.Utf8Cow
import org.rustlang.core.slice.SharedByteSlice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeneratedPrivateAdoptionTest {
    @Test
    fun unsignedHintsAndZeroSizedElementsRetainRustSemantics() {
        assertEquals(0uL, cautious(ULong.MAX_VALUE, 0uL))
        assertEquals(0uL, cautious(null, 32uL))
        assertEquals(32768uL, cautious(ULong.MAX_VALUE, 32uL))
        assertEquals(16384uL, cautious(ULong.MAX_VALUE, 64uL))
        assertEquals(0uL, cautious(ULong.MAX_VALUE, 1048577uL))
    }

    @Test
    fun openTypesAndUnmeasuredTargetsRequireEvidence() {
        assertFailsWith<IllegalArgumentException> { cautious<Content>(10) }
        assertFailsWith<IllegalArgumentException> { cautious<Unit>(10) }
        assertFailsWith<IllegalArgumentException> { cautious<Content>(-1) }
        for (target in listOf("aarch64-apple-ios", "x86_64-apple-darwin", "aarch64-unknown-linux-gnu", "jvm")) {
            assertFailsWith<IllegalStateException> { rustContentSizes(target) }
        }
        assertEquals(Pair(32uL, 64uL), rustContentSizes("aarch64-apple-darwin"))
    }

    @Test
    fun validUtf8RetainsTheSelectedBorrowAndTextCompatibility() {
        val bytes = byteArrayOf(-1, 97, -30, -126, -84, -1)
        val selected = SharedByteSlice.borrow(bytes, 1, 5)
        val result = assertIs<Utf8Cow.Borrowed>(from_utf8_lossy(selected))
        assertTrue(result.bytes.isSameRegion(selected))
        assertEquals("a€", result.toText())
        assertEquals("a€", fromUtf8Lossy(selected.copyBytes()))
    }

    @Test
    fun invalidUtf8OwnsRepairedBytesAndPreservesNoAllocFallback() {
        val invalid = byteArrayOf(-30, -126, 65)
        val result = assertIs<Utf8Cow.Owned>(from_utf8_lossy(SharedByteSlice.borrow(invalid)))
        assertEquals("�A", result.toText())
        assertEquals(result.toText(), fromUtf8Lossy(invalid))
        assertEquals("���", fromUtf8LossyNoAlloc(invalid))
    }
}
