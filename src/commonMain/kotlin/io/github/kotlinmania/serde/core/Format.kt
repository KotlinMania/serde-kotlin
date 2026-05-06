// port-lint: source serde_core/src/format.rs
package io.github.kotlinmania.serde.core

internal class Buf private constructor(
    private val bytes: ByteArray,
) {
    private var offset: Int = 0

    companion object {
        fun new(bytes: ByteArray): Buf = Buf(bytes)
    }

    fun asStr(): String {
        val slice = bytes.copyOfRange(0, offset)
        return slice.decodeToString(throwOnInvalidSequence = true)
    }

    fun writeStr(s: String): Result<Unit> {
        val stringBytes = s.encodeToByteArray()
        return if (offset + stringBytes.size > bytes.size) {
            Result.failure(IllegalStateException("format buffer is full"))
        } else {
            stringBytes.copyInto(bytes, destinationOffset = offset)
            offset += stringBytes.size
            Result.success(Unit)
        }
    }
}
