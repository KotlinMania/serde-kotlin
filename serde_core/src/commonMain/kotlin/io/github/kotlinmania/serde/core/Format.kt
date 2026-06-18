// port-lint: source serde_core/src/format.rs
package io.github.kotlinmania.serde.core

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult

internal class FormatError : RuntimeException()

internal class Buf private constructor(
    private val bytes: ByteArray,
) {
    private var offset: Int = 0

    companion object {
        fun new(bytes: ByteArray): Buf = Buf(bytes)
    }

    fun asStr(): String =
        bytes.decodeToString(
            startIndex = 0,
            endIndex = offset,
            throwOnInvalidSequence = false,
        )

    fun writeStr(s: String): SerdeResult<Unit> {
        val stringBytes = s.encodeToByteArray()
        return if (offset + stringBytes.size > bytes.size) {
            SerdeResult.failure(SerdeError("serde format buffer overflow"))
        } else {
            stringBytes.copyInto(bytes, destinationOffset = offset)
            offset += stringBytes.size
            SerdeResult.success(Unit)
        }
    }
}
