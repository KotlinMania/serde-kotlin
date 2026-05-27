// port-lint: source serde_core/src/de/mod.rs
package io.github.kotlinmania.serde.core.de

import io.github.kotlinmania.serde.SerdeError

/**
 * The `Error` interface allows `Deserialize` implementations to create descriptive error messages
 * belonging to the `Deserializer` against which they are currently running.
 *
 * Every `Deserializer` declares an `Error` type that encompasses both general-purpose
 * deserialization errors as well as errors specific to the particular deserialization format. For
 * example the `Error` type of `serde_json` can represent errors like an invalid JSON escape
 * sequence or an unterminated string literal, in addition to the error cases that are part of this
 * interface.
 *
 * Most deserializers should only need to provide the `custom` method and inherit the default
 * behavior for the other methods.
 */
interface Error : StdError {
    companion object {
        /**
         * Raised when there is general error when deserializing a type.
         *
         * The message should not be capitalized and should not end with a period.
         */
        fun custom(msg: String): SerdeError = SerdeError(msg)

        /**
         * Raised when a `Deserialize` receives a type different from what it was expecting.
         */
        fun invalidType(
            unexp: Unexpected,
            exp: Expected,
        ): SerdeError = custom("invalid type: $unexp, expected ${exp.expecting()}")

        /**
         * Raised when a `Deserialize` receives a value of the right type but that is wrong for some
         * other reason.
         */
        fun invalidValue(
            unexp: Unexpected,
            exp: Expected,
        ): SerdeError = custom("invalid value: $unexp, expected ${exp.expecting()}")

        /**
         * Raised when deserializing a sequence or map and the input data contains too many or too
         * few elements.
         */
        fun invalidLength(
            len: Int,
            exp: Expected,
        ): SerdeError = custom("invalid length $len, expected ${exp.expecting()}")

        /**
         * Raised when a `Deserialize` enum type received a variant with an unrecognized name.
         */
        fun unknownVariant(
            variant: String,
            expected: List<String>,
        ): SerdeError =
            if (expected.isEmpty()) {
                custom("unknown variant `$variant`, there are no variants")
            } else {
                custom("unknown variant `$variant`, expected ${OneOf(expected)}")
            }

        /**
         * Raised when a `Deserialize` class type received a field with an unrecognized name.
         */
        fun unknownField(
            field: String,
            expected: List<String>,
        ): SerdeError =
            if (expected.isEmpty()) {
                custom("unknown field `$field`, there are no fields")
            } else {
                custom("unknown field `$field`, expected ${OneOf(expected)}")
            }

        /**
         * Raised when a `Deserialize` class type expected to receive a required field with a
         * particular name but that field was not present in the input.
         */
        fun missingField(field: String): SerdeError = custom("missing field `$field`")

        /**
         * Raised when a `Deserialize` class type received more than one of the same field.
         */
        fun duplicateField(field: String): SerdeError = custom("duplicate field `$field`")
    }
}