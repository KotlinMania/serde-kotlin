// port-lint: source serde_core/src/format.rs
package io.github.kotlinmania.serde.core

/*
 * Copyright (c) 2026 Sydney Renee <sydney@solace.ofharmony.ai>
 * and The Solace Project.
 *
 * Licensed under either Apache-2.0 or MIT at your option; see LICENSE-APACHE
 * and LICENSE-MIT.
 */

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
