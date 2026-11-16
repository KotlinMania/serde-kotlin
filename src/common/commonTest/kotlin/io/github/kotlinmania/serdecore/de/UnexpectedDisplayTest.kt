package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import kotlin.test.Test
import kotlin.test.assertEquals

class UnexpectedDisplayTest {
    @Test
    fun dataVariantsUseRustDisplayInsteadOfGeneratedDataClassText() {
        val cases =
            listOf(
                Unexpected.Bool(true) to "boolean `true`",
                Unexpected.Unsigned(ULong.MAX_VALUE) to "integer `18446744073709551615`",
                Unexpected.Signed(Long.MIN_VALUE) to "integer `-9223372036854775808`",
                Unexpected.FloatValue(1.5) to "floating point `1.5`",
                Unexpected.CharValue('x') to "character `x`",
                Unexpected.Str("word") to "string \"word\"",
                Unexpected.Bytes(byteArrayOf(0, -1)) to "byte array",
                Unexpected.Other("a custom value") to "a custom value",
            )

        for ((value, expected) in cases) {
            assertEquals(expected, value.toString())
        }
    }

    @Test
    fun unitVariantsRetainTheirRustDisplayLabels() {
        val cases =
            listOf(
                Unexpected.UnitValue to "unit value",
                Unexpected.Option to "Option value",
                Unexpected.NewtypeStruct to "newtype struct",
                Unexpected.Seq to "sequence",
                Unexpected.Map to "map",
                Unexpected.Enum to "enum",
                Unexpected.UnitVariant to "unit variant",
                Unexpected.NewtypeVariant to "newtype variant",
                Unexpected.TupleVariant to "tuple variant",
                Unexpected.StructVariant to "struct variant",
            )

        for ((value, expected) in cases) {
            assertEquals(expected, value.toString())
        }
    }

    @Test
    fun errorConstructorsUseTheSameDisplay() {
        val expected = Expected { "a sign of -1, 0, or 1" }
        assertEquals(
            "invalid value: integer `-128`, expected a sign of -1, 0, or 1",
            SerdeError.invalidValue(Unexpected.Signed(-128), expected).message,
        )
        assertEquals(
            "invalid type: boolean `true`, expected a sign of -1, 0, or 1",
            SerdeError.invalidType(Unexpected.Bool(true), expected).message,
        )
    }
}
