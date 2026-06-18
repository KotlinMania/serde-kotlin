// port-lint: source serde_core/src/ser/fmt.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class FmtTest {
    @Test
    public fun primitiveValuesUseRustDisplayText() {
        assertEquals("true", format { serializeBool(true) })
        assertEquals("-8", format { serializeI8((-8).toByte()) })
        assertEquals("8", format { serializeU8(8u) })
        assertEquals("a", format { serializeChar('a') })
        assertEquals("text", format { serializeStr("text") })
        assertEquals("UnitName", format { serializeUnitStruct("UnitName") })
        assertEquals("Variant", format { serializeUnitVariant("EnumName", 0u, "Variant") })
    }

    @Test
    public fun floatValuesUseRustDisplayText() {
        assertEquals("1", format { serializeF32(1.0f) })
        assertEquals("-0", format { serializeF32(-0.0f) })
        assertEquals("10000000", format { serializeF64(1.0e7) })
        assertEquals("0.0000001", format { serializeF64(1.0e-7) })
        assertEquals("inf", format { serializeF64(Double.POSITIVE_INFINITY) })
        assertEquals("-inf", format { serializeF64(Double.NEGATIVE_INFINITY) })
        assertEquals("NaN", format { serializeF64(Double.NaN) })
    }

    @Test
    public fun newtypeStructSerializesItsValue() {
        assertEquals("value", format { serializeNewtypeStruct("Wrapper", LiteralSerialize("value")) })
    }

    @Test
    public fun unsupportedSerializerEntriesReturnFormattingError() {
        val builder = StringBuilder()
        val serializer = FormatterSerializer(builder)

        assertTrue(serializer.serializeBytes(byteArrayOf(1, 2, 3)).isFailure)
        assertTrue(serializer.serializeNone().isFailure)
        assertTrue(serializer.serializeSome(LiteralSerialize("value")).isFailure)
        assertTrue(serializer.serializeUnit().isFailure)
        assertTrue(serializer.serializeNewtypeVariant("EnumName", 0u, "Variant", LiteralSerialize("value")).isFailure)
        assertTrue(serializer.serializeSeq(null).isFailure)
        assertTrue(serializer.serializeTuple(2).isFailure)
        assertTrue(serializer.serializeTupleStruct("TupleStruct", 2).isFailure)
        assertTrue(serializer.serializeTupleVariant("EnumName", 0u, "Tuple", 2).isFailure)
        assertTrue(serializer.serializeMap(null).isFailure)
        assertTrue(serializer.serializeStruct("Struct", 1).isFailure)
        assertTrue(serializer.serializeStructVariant("EnumName", 0u, "Struct", 1).isFailure)
        assertEquals("", builder.toString())
    }
}

private fun format(block: FormatterSerializer.() -> SerdeResult<Unit>): String {
    val builder = StringBuilder()
    FormatterSerializer(builder).block().getOrThrow()
    return builder.toString()
}

private data class LiteralSerialize(
    private val text: String,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): SerdeResult<Ok>
        where E : Error = serializer.serializeStr(text)
}
