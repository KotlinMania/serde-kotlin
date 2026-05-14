// port-lint: source serde_core/src/format.rs
package io.github.kotlinmania.serde.core

internal class FormatError : RuntimeException()

internal class Buf private constructor(
    private val bytes: ByteArray,
) {
    private var offset: Int = 0

    companion object {
        fun new(bytes: ByteArray): Buf = Buf(bytes)
    }

    fun asStr(): String {
        return bytes.decodeToString(
            startIndex = 0,
            endIndex = offset,
            throwOnInvalidSequence = false,
        )
    }

    fun writeStr(s: String): Result<Unit> {
        val stringBytes = s.encodeToByteArray()
        return if (offset + stringBytes.size > bytes.size) {
            Result.failure(FormatError())
        } else {
            stringBytes.copyInto(bytes, destinationOffset = offset)
            offset += stringBytes.size
            Result.success(Unit)
        }
    }
}
