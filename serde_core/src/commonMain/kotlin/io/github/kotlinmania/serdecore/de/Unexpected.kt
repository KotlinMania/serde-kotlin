// port-lint: source de/mod.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError

/**
 * `Unexpected` represents an unexpected invocation of any one of the `Visitor` interface methods.
 *
 * This is used as an argument to the `invalidType`, `invalidValue`, and `invalidLength` methods of
 * the `SerdeError` interface to build error messages.
 */
sealed class Unexpected {
    /**
     * The input contained a boolean value that was not expected.
     */
    data class Bool(
        val value: Boolean,
    ) : Unexpected() {
        override fun hashCode(): Int = if (value) 1231 else 1237
    }

    /**
     * The input contained an unsigned integer `UByte`, `UShort`, `UInt` or `ULong` that was not
     * expected.
     */
    data class Unsigned(
        val value: ULong,
    ) : Unexpected()

    /**
     * The input contained a signed integer `Byte`, `Short`, `Int` or `Long` that was not expected.
     */
    data class Signed(
        val value: Long,
    ) : Unexpected() {
        override fun hashCode(): Int = (value xor (value ushr 32)).toInt()
    }

    /**
     * The input contained a floating point `Float` or `Double` that was not expected.
     */
    data class FloatValue(
        val value: Double,
    ) : Unexpected() {
        override fun hashCode(): Int {
            val bits = value.toBits()
            return (bits xor (bits ushr 32)).toInt()
        }
    }

    /**
     * The input contained a `Char` that was not expected.
     */
    data class CharValue(
        val value: Char,
    ) : Unexpected() {
        override fun hashCode(): Int = value.code
    }

    /**
     * The input contained a `String` that was not expected.
     */
    data class Str(
        val value: String,
    ) : Unexpected()

    /**
     * The input contained a `ByteArray` that was not expected.
     */
    data class Bytes(
        val value: ByteArray,
    ) : Unexpected() {
        override fun equals(other: Any?): Boolean = this === other || other is Bytes && value.contentEquals(other.value)

        override fun hashCode(): Int = value.contentHashCode()
    }

    /**
     * The input contained a unit `Unit` that was not expected.
     */
    data object UnitValue : Unexpected() {
        override fun toString(): String = "unit value"
    }

    /**
     * The input contained an optional value that was not expected.
     */
    data object Option : Unexpected() {
        override fun toString(): String = "Option value"
    }

    /**
     * The input contained a newtype class that was not expected.
     */
    data object NewtypeStruct : Unexpected() {
        override fun toString(): String = "newtype struct"
    }

    /**
     * The input contained a sequence that was not expected.
     */
    data object Seq : Unexpected() {
        override fun toString(): String = "sequence"
    }

    /**
     * The input contained a map that was not expected.
     */
    data object Map : Unexpected() {
        override fun toString(): String = "map"
    }

    /**
     * The input contained an enum that was not expected.
     */
    data object Enum : Unexpected() {
        override fun toString(): String = "enum"
    }

    /**
     * The input contained a unit variant that was not expected.
     */
    data object UnitVariant : Unexpected() {
        override fun toString(): String = "unit variant"
    }

    /**
     * The input contained a newtype variant that was not expected.
     */
    data object NewtypeVariant : Unexpected() {
        override fun toString(): String = "newtype variant"
    }

    /**
     * The input contained a tuple variant that was not expected.
     */
    data object TupleVariant : Unexpected() {
        override fun toString(): String = "tuple variant"
    }

    /**
     * The input contained a class variant that was not expected.
     */
    data object StructVariant : Unexpected() {
        override fun toString(): String = "struct variant"
    }

    /**
     * A message stating what uncategorized thing the input contained that was not expected.
     *
     * The message should be a noun or noun phrase, not capitalized and without a period. An example
     * message is "unoriginal superhero".
     */
    data class Other(
        val value: String,
    ) : Unexpected()

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
