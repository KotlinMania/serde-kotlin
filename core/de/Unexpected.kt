// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

/**
 * `Unexpected` represents an unexpected invocation of any one of the `Visitor` interface methods.
 *
 * This is used as an argument to the `invalidType`, `invalidValue`, and `invalidLength` methods of
 * the `Error` interface to build error messages.
 */
public sealed class Unexpected {
    /**
     * The input contained a boolean value that was not expected.
     */
    public data class Bool(public val value: Boolean) : Unexpected()

    /**
     * The input contained an unsigned integer `UByte`, `UShort`, `UInt` or `ULong` that was not
     * expected.
     */
    public data class Unsigned(public val value: ULong) : Unexpected()

    /**
     * The input contained a signed integer `Byte`, `Short`, `Int` or `Long` that was not expected.
     */
    public data class Signed(public val value: Long) : Unexpected()

    /**
     * The input contained a floating point `Float` or `Double` that was not expected.
     */
    public data class FloatValue(public val value: Double) : Unexpected()

    /**
     * The input contained a `Char` that was not expected.
     */
    public data class CharValue(public val value: Char) : Unexpected()

    /**
     * The input contained a `String` that was not expected.
     */
    public data class Str(public val value: String) : Unexpected()

    /**
     * The input contained a `ByteArray` that was not expected.
     */
    public data class Bytes(public val value: ByteArray) : Unexpected() {
        override fun equals(other: Any?): Boolean =
            this === other || other is Bytes && value.contentEquals(other.value)

        override fun hashCode(): Int = value.contentHashCode()
    }

    /**
     * The input contained a unit `Unit` that was not expected.
     */
    public data object UnitValue : Unexpected()

    /**
     * The input contained an optional value that was not expected.
     */
    public data object Option : Unexpected()

    /**
     * The input contained a newtype class that was not expected.
     */
    public data object NewtypeStruct : Unexpected()

    /**
     * The input contained a sequence that was not expected.
     */
    public data object Seq : Unexpected()

    /**
     * The input contained a map that was not expected.
     */
    public data object Map : Unexpected()

    /**
     * The input contained an enum that was not expected.
     */
    public data object Enum : Unexpected()

    /**
     * The input contained a unit variant that was not expected.
     */
    public data object UnitVariant : Unexpected()

    /**
     * The input contained a newtype variant that was not expected.
     */
    public data object NewtypeVariant : Unexpected()

    /**
     * The input contained a tuple variant that was not expected.
     */
    public data object TupleVariant : Unexpected()

    /**
     * The input contained a class variant that was not expected.
     */
    public data object StructVariant : Unexpected()

    /**
     * A message stating what uncategorized thing the input contained that was not expected.
     *
     * The message should be a noun or noun phrase, not capitalized and without a period. An example
     * message is "unoriginal superhero".
     */
    public data class Other(public val value: String) : Unexpected()

    override fun toString(): String =
        when (this) {
            is Bool -> "boolean `$value`"
            is Unsigned -> "integer `$value`"
            is Signed -> "integer `$value`"
            is FloatValue -> "floating point `${WithDecimalPoint(value)}`"
            is CharValue -> "character `$value`"
            is Str -> "string \"$value\""
            is Bytes -> "byte array"
            UnitValue -> "unit value"
            Option -> "Option value"
            NewtypeStruct -> "newtype struct"
            Seq -> "sequence"
            Map -> "map"
            Enum -> "enum"
            UnitVariant -> "unit variant"
            NewtypeVariant -> "newtype variant"
            TupleVariant -> "tuple variant"
            StructVariant -> "struct variant"
            is Other -> value
        }
}
