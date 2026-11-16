package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.BytesDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class StringInPlaceByteBoundaryTest {
    private class Input(
        private val bytes: ByteArray,
        private val owned: Boolean,
    ) : Deserializer by BytesDeserializer.new(bytes) {
        override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> =
            if (owned) visitor.visitByteBuf(bytes) else visitor.visitBytes(bytes)
    }

    @Test
    fun validBorrowedBytesReplaceTheStringIncludingSupplementaryAndNul() {
        var value = "before"
        var calls = 0
        val text = "a🙂\u0000"
        val result =
            StringDeserialize.deserializeInPlace(Input(text.encodeToByteArray(), false)) {
                calls += 1
                value = it
            }
        assertIs<SerdeResult.Success<Unit>>(result)
        assertEquals(text, value)
        assertEquals(1, calls)
    }

    @Test
    fun validOwnedEmptyBufferClearsTheString() {
        var value = "before"
        var calls = 0
        val result =
            StringDeserialize.deserializeInPlace(Input(byteArrayOf(), true)) {
                calls += 1
                value = it
            }
        assertIs<SerdeResult.Success<Unit>>(result)
        assertEquals("", value)
        assertEquals(1, calls)
    }

    private fun assertInvalidBytesDoNotWrite(bytes: ByteArray, owned: Boolean) {
        var value = "before"
        var calls = 0
        val result =
            StringDeserialize.deserializeInPlace(Input(bytes, owned)) {
                calls += 1
                value = it
            }
        val failure = assertIs<SerdeResult.Failure>(result)
        assertEquals("invalid value: byte array, expected a string", failure.error.message)
        assertEquals("before", value)
        assertEquals(0, calls)
    }

    @Test
    fun invalidBorrowedOverlongBytesLeaveTheStringUntouched() {
        assertInvalidBytesDoNotWrite(byteArrayOf(0xc0.toByte(), 0x80.toByte()), false)
    }

    @Test
    fun invalidOwnedTruncatedBytesLeaveTheStringUntouched() {
        assertInvalidBytesDoNotWrite(byteArrayOf(0xf0.toByte(), 0x9f.toByte(), 0x99.toByte()), true)
    }

    private fun assertSetterExceptionPropagates(owned: Boolean) {
        val failure = IllegalStateException("setter")
        var calls = 0
        var value = "before"
        val place =
            mutablePlace({ value }) {
                value = it
                calls += 1
                throw failure
            }
        assertSame(
            failure,
            assertFailsWith<IllegalStateException> {
                StringDeserialize.deserializeInPlace(Input("valid".encodeToByteArray(), owned), place)
            },
        )
        assertEquals("valid", value)
        assertEquals(1, calls)
    }

    @Test
    fun borrowedByteSetterExceptionIsNotAnInvalidUtf8Error() {
        assertSetterExceptionPropagates(false)
    }

    @Test
    fun ownedByteSetterExceptionIsNotAnInvalidUtf8Error() {
        assertSetterExceptionPropagates(true)
    }

    private fun assertTypedSetterExceptionPropagates(owned: Boolean) {
        val diagnostic = SerdeError.custom("setter rejected")
        val failure = SerdeException(diagnostic)
        var calls = 0
        val actual =
            assertFailsWith<SerdeException> {
                StringDeserialize.deserializeInPlace(Input("valid".encodeToByteArray(), owned)) {
                    calls += 1
                    throw failure
                }
            }
        assertSame(failure, actual)
        assertSame(diagnostic, actual.error)
        assertEquals(1, calls)
    }

    @Test
    fun borrowedByteTypedSetterExceptionRetainsIdentity() {
        assertTypedSetterExceptionPropagates(false)
    }

    @Test
    fun ownedByteTypedSetterExceptionRetainsIdentity() {
        assertTypedSetterExceptionPropagates(true)
    }
}
