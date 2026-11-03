// port-lint: source private/string.rs
package io.github.kotlinmania.serdecore.priv

import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.priv.generated.string.from_utf8_lossy as generatedFromUtf8Lossy

/**
 * A read-only view of an existing byte-array range. The owner must not mutate
 * any byte in this range while a shared view or a borrowed UTF-8 value is live.
 */
class SharedByteSlice private constructor(
    private val storage: ByteArray,
    val start: Int,
    val size: Int,
) {
    operator fun get(index: Int): Byte {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("Byte-slice index: $index")
        return storage[start + index]
    }

    fun copyBytes(): ByteArray = storage.copyOfRange(start, start + size)

    fun isSameRegion(other: SharedByteSlice): Boolean =
        storage === other.storage && start == other.start && size == other.size

    internal fun decodeValidUtf8(): String =
        storage.decodeToString(start, start + size, throwOnInvalidSequence = true)

    companion object {
        /** Borrows the selected half-open range without copying it. */
        fun borrow(storage: ByteArray, fromIndex: Int = 0, toIndex: Int = storage.size): SharedByteSlice {
            require(fromIndex >= 0 && fromIndex <= toIndex && toIndex <= storage.size)
            return SharedByteSlice(storage, fromIndex, toIndex - fromIndex)
        }
    }
}

/** Primitive UTF-8 byte operations recovered from Rust's validation helpers. */
object Utf8ByteOperations {
    /** Appends the low six byte bits; the Int carries the original u32 bit pattern. */
    fun appendContinuationBits(accumulator: Int, byte: Byte): Int =
        (accumulator shl 6) or (byte.toInt() and 0x3f)

    /** Leading-byte width, or zero for an invalid lead; does not validate a sequence. */
    fun sequenceByteWidth(byte: Byte): Long =
        when (byte.toInt() and 0xff) {
            in 0x00..0x7f -> 1L
            in 0xc2..0xdf -> 2L
            in 0xe0..0xef -> 3L
            in 0xf0..0xf4 -> 4L
            else -> 0L
        }

    /** True when any of the eight bytes in the 64-bit word has its high bit set. */
    fun hasNonAsciiByte64(word: Long): Boolean =
        (word and -9187201950435737472L) != 0L

    /** True for byte patterns 0x80..0xbf, retaining Rust's signed-i8 test. */
    fun isContinuationByte(byte: Byte): Boolean =
        byte.toInt() < -0x40

    /**
     * True when this byte is not a continuation byte. This is a byte-level test,
     * not full UTF-8 validation; invalid lead bytes also satisfy the predicate.
     */
    fun isCharBoundaryByte(byte: Byte): Boolean =
        byte.toInt() >= -0x40
}

/** A borrowed or owned UTF-8 string; accessing its bytes does not copy them. */
sealed class Utf8Cow private constructor(val bytes: SharedByteSlice) {
    class Borrowed internal constructor(bytes: SharedByteSlice) : Utf8Cow(bytes)
    class Owned internal constructor(bytes: SharedByteSlice) : Utf8Cow(bytes)

    val isBorrowed: Boolean get() = this is Borrowed

    /** Decodes validated UTF-8; malformed input is never silently replaced here. */
    fun toText(): String = bytes.decodeValidUtf8()

    companion object {
        private val empty = SharedByteSlice.borrow(ByteArray(0))

        /**
         * Retains valid input as a borrow. Invalid sequences are replaced with
         * the replacement character in freshly owned UTF-8 storage.
         */
        fun fromUtf8Lossy(input: SharedByteSlice): Utf8Cow {
            if (input.size == 0) return Borrowed(empty)
            var chunk = nextChunk(input, 0)
            if (chunk.validEnd == chunk.end) return Borrowed(input)

            val output = ArrayList<Byte>(input.size)
            var start = 0
            while (true) {
                for (index in start until chunk.validEnd) output.add(input[index])
                if (chunk.validEnd != chunk.end) {
                    output.add(0xef.toByte())
                    output.add(0xbf.toByte())
                    output.add(0xbd.toByte())
                }
                start = chunk.end
                if (start == input.size) break
                chunk = nextChunk(input, start)
            }
            val owned = ByteArray(output.size) { output[it] }
            return Owned(SharedByteSlice.borrow(owned))
        }

        private class Chunk(val validEnd: Int, val end: Int)

        // Chunk boundaries follow the UTF-8 chunk iterator's consumed prefix.
        private fun nextChunk(input: SharedByteSlice, start: Int): Chunk {
            var index = start
            var validEnd = start
            while (index < input.size) {
                val byte = input[index].toInt() and 0xff
                index += 1
                if (byte >= 0x80) {
                    when (Utf8ByteOperations.sequenceByteWidth(byte.toByte())) {
                        2L -> {
                            if (!Utf8ByteOperations.isContinuationByte(safeGet(input, index))) break
                            index += 1
                        }
                        3L -> {
                            val second = safeGet(input, index).toInt() and 0xff
                            val validSecond = when (byte) {
                                0xe0 -> second in 0xa0..0xbf
                                in 0xe1..0xec -> second in 0x80..0xbf
                                0xed -> second in 0x80..0x9f
                                in 0xee..0xef -> second in 0x80..0xbf
                                else -> false
                            }
                            if (!validSecond) break
                            index += 1
                            if (!Utf8ByteOperations.isContinuationByte(safeGet(input, index))) break
                            index += 1
                        }
                        4L -> {
                            val second = safeGet(input, index).toInt() and 0xff
                            val validSecond = when (byte) {
                                0xf0 -> second in 0x90..0xbf
                                in 0xf1..0xf3 -> second in 0x80..0xbf
                                0xf4 -> second in 0x80..0x8f
                                else -> false
                            }
                            if (!validSecond) break
                            index += 1
                            if (!Utf8ByteOperations.isContinuationByte(safeGet(input, index))) break
                            index += 1
                            if (!Utf8ByteOperations.isContinuationByte(safeGet(input, index))) break
                            index += 1
                        }
                        else -> break
                    }
                }
                validEnd = index
            }
            return Chunk(validEnd, index)
        }

        private fun safeGet(input: SharedByteSlice, index: Int): Byte =
            if (index < input.size) input[index] else 0
    }
}

// Compatibility entry point materializes text; the generated entry point retains Cow ownership.
fun fromUtf8Lossy(bytes: ByteArray): String =
    generatedFromUtf8Lossy(SharedByteSlice.borrow(bytes)).toText()

// The generated code calls this like:
//
//     val value = io.github.kotlinmania.serdecore.priv.fromUtf8Lossy(bytes)
//     Err(serde.de.Error.unknownVariant(value, VARIANTS))
//
// so it is okay for the return type to be different from the standard-library
// case as long as the above works.
fun fromUtf8LossyNoAlloc(bytes: ByteArray): String =
    serdeCatching { bytes.decodeToString(throwOnInvalidSequence = true) }
        .getOrElse {
            // Three unicode replacement characters if it fails. They look like a
            // white-on-black question mark. The user will recognize it as invalid
            // UTF-8.
            "\ufffd\ufffd\ufffd"
        }
