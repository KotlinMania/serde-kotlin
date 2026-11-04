// port-lint: source private/content.rs
package io.github.kotlinmania.serdecore.priv.generated.content

// Generated from Syn AST; exact original retained in input.rs.
// Native borrowed bytes/Cow retain storage. The caller must keep the selected input range immutable while a borrowed result is live.
// Rust use resolved through explicit library bindings.

// Used from generated code to buffer the contents of the Deserializer when
// deserializing untagged enums and internally tagged enums.
//
// Not public API. Use serde-value instead.
//
// Obsoleted by format-specific buffer types (https://github.com/serde-rs/serde/pull/2912).
// Rust #[doc(hidden)]: omitted from source API documentation.
sealed class `Content` {
    class `Bool` internal constructor(
        internal val __rtkField0: kotlin.Boolean,
    ) : `Content`()
    class `U8` internal constructor(
        internal val __rtkField0: kotlin.UByte,
    ) : `Content`()
    class `U16` internal constructor(
        internal val __rtkField0: kotlin.UShort,
    ) : `Content`()
    class `U32` internal constructor(
        internal val __rtkField0: kotlin.UInt,
    ) : `Content`()
    class `U64` internal constructor(
        internal val __rtkField0: kotlin.ULong,
    ) : `Content`()
    class `I8` internal constructor(
        internal val __rtkField0: kotlin.Byte,
    ) : `Content`()
    class `I16` internal constructor(
        internal val __rtkField0: kotlin.Short,
    ) : `Content`()
    class `I32` internal constructor(
        internal val __rtkField0: kotlin.Int,
    ) : `Content`()
    class `I64` internal constructor(
        internal val __rtkField0: kotlin.Long,
    ) : `Content`()
    class `F32` internal constructor(
        internal val __rtkField0: kotlin.Float,
    ) : `Content`()
    class `F64` internal constructor(
        internal val __rtkField0: kotlin.Double,
    ) : `Content`()
    class `Char` internal constructor(
        internal val __rtkField0: RtkChar,
    ) : `Content`()
    class `String` internal constructor(
        internal val __rtkField0: kotlin.text.StringBuilder,
    ) : `Content`()
    class `Str` internal constructor(
        internal val __rtkField0: kotlin.String,
    ) : `Content`()
    class `ByteBuf` internal constructor(
        internal val __rtkField0: kotlin.collections.MutableList<kotlin.UByte>,
    ) : `Content`()
    class `Bytes` internal constructor(
        internal val __rtkField0: io.github.kotlinmania.serdecore.priv.SharedByteSlice,
    ) : `Content`()
    data object `None` : `Content`()
    class `Some` internal constructor(
        internal val __rtkField0: `Content`,
    ) : `Content`()
    data object `Unit` : `Content`()
    class `Newtype` internal constructor(
        internal val __rtkField0: `Content`,
    ) : `Content`()
    class `Seq` internal constructor(
        internal val __rtkField0: kotlin.collections.MutableList<`Content`>,
    ) : `Content`()
    class `Map` internal constructor(
        internal val __rtkField0: kotlin.collections.MutableList<kotlin.Pair<`Content`, `Content`>>,
    ) : `Content`()
    companion object
}

// Data declaration contract: Kotlin construction transfers owned payload references; relinquish mutable aliases. Borrowed payload owners must remain immutable while retained. Box addresses, allocation capacity/failure and custom Drop behavior are outside this data-only lowering; operations needing them reject.

data class RtkChar(
    val codePoint: Int,
) {
    init {
        require(codePoint in 0..0x10ffff && codePoint !in 0xd800..0xdfff)
    }
    fun lenUtf8(): ULong = scalarString().encodeToByteArray(throwOnInvalidSequence = true).size.toULong()
    fun scalarString(): String =
        if (codePoint < 0x10000) {
            codePoint.toChar().toString()
        } else {
            (0xd800 + ((codePoint - 0x10000) shr 10)).toChar().toString() +
                (0xdc00 + ((codePoint - 0x10000) and 1023)).toChar()
        }
}
