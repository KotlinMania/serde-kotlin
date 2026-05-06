// port-lint: source serde_core/src/private/content.rs
package io.github.kotlinmania.serde.core.`private`

// Used from generated code to buffer the contents of the Deserializer when
// deserializing untagged enums and internally tagged enums.
//
// Not public API. Use serde-value instead.
//
// Obsoleted by format-specific buffer types (https://github.com/serde-rs/serde/pull/2912).
public sealed class Content {
    public data class Bool(public val value: Boolean) : Content()

    public data class U8(public val value: UByte) : Content()
    public data class U16(public val value: UShort) : Content()
    public data class U32(public val value: UInt) : Content()
    public data class U64(public val value: ULong) : Content()

    public data class I8(public val value: Byte) : Content()
    public data class I16(public val value: Short) : Content()
    public data class I32(public val value: Int) : Content()
    public data class I64(public val value: Long) : Content()

    public data class F32(public val value: Float) : Content()
    public data class F64(public val value: Double) : Content()

    public data class Char(public val value: kotlin.Char) : Content()
    public data class String(public val value: kotlin.String) : Content()
    public data class Str(public val value: kotlin.String) : Content()
    public data class ByteBuf(public val value: ByteArray) : Content()
    public data class Bytes(public val value: ByteArray) : Content()

    public data object None : Content()
    public data class Some(public val value: Content) : Content()

    public data object Unit : Content()
    public data class Newtype(public val value: Content) : Content()
    public data class Seq(public val value: List<Content>) : Content()
    public data class Map(public val value: List<Pair<Content, Content>>) : Content()
}
