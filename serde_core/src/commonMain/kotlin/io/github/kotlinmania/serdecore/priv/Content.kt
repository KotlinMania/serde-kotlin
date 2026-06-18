// port-lint: source serde_core/src/private/content.rs
package io.github.kotlinmania.serdecore.priv

// Used from generated code to buffer the contents of the Deserializer when
// deserializing untagged enums and internally tagged enums.
//
// Not public API. Use serde-value instead.
//
// Obsoleted by format-specific buffer types (https://github.com/serde-rs/serde/pull/2912).
data class ContentMapEntry(val key: Content, val value: Content)

sealed interface Content {
    data class Bool(
        val value: Boolean,
    ) : Content

    data class U8(
        val value: UByte,
    ) : Content

    data class U16(
        val value: UShort,
    ) : Content

    data class U32(
        val value: UInt,
    ) : Content

    data class U64(
        val value: ULong,
    ) : Content

    data class I8(
        val value: Byte,
    ) : Content

    data class I16(
        val value: Short,
    ) : Content

    data class I32(
        val value: Int,
    ) : Content

    data class I64(
        val value: Long,
    ) : Content

    data class F32(
        val value: Float,
    ) : Content

    data class F64(
        val value: Double,
    ) : Content

    data class Char(
        val value: kotlin.Char,
    ) : Content

    data class StringValue(
        val value: kotlin.String,
    ) : Content

    data class Str(
        val value: kotlin.String,
    ) : Content

    data class ByteBuf(
        val value: ByteArray,
    ) : Content {
        override fun equals(other: Any?): Boolean = this === other || other is ByteBuf && value.contentEquals(other.value)

        override fun hashCode(): Int = value.contentHashCode()
    }

    data class Bytes(
        val value: ByteArray,
    ) : Content {
        override fun equals(other: Any?): Boolean = this === other || other is Bytes && value.contentEquals(other.value)

        override fun hashCode(): Int = value.contentHashCode()
    }

    data object None : Content

    data class Some(
        val value: Content,
    ) : Content

    data object Unit : Content

    data class Newtype(
        val value: Content,
    ) : Content

    data class Seq(
        val value: List<Content>,
    ) : Content

    data class Map(
        val value: List<ContentMapEntry>,
    ) : Content
}
