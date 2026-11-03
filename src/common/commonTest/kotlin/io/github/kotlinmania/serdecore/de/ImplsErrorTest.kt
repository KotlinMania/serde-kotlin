// port-lint: tests test_suite/tests/test_de_error.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals

public class ImplsErrorTest {
    @Test
    public fun cStringInternalNullReportsUpstreamError() {
        val error =
            CStringDeserialize
                .deserialize(byteArrayOf('a'.code.toByte(), 0, 'b'.code.toByte()).intoDeserializer())
                .exceptionOrNull()

        assertEquals("nul byte found in provided data at position: 1", error?.message)
    }

    @Test
    public fun cStringInternalNullEndReportsUpstreamError() {
        val error =
            CStringDeserialize
                .deserialize(byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 0).intoDeserializer())
                .exceptionOrNull()

        assertEquals("nul byte found in provided data at position: 2", error?.message)
    }

    @Test
    public fun zeroArrayFromUnitReportsUpstreamError() {
        val error = emptyArrayDeserialize<Int>().deserialize(Unit.intoDeserializer()).exceptionOrNull()

        assertEquals("invalid type: unit value, expected an empty array", error?.message)
    }

    @Test
    public fun zeroArrayFromUnitStructReportsUpstreamError() {
        val error = emptyArrayDeserialize<Int>().deserialize(UnitStructDeserializer).exceptionOrNull()

        assertEquals("invalid type: unit value, expected an empty array", error?.message)
    }

    @Test
    public fun durationOverflowSeqReportsUpstreamError() {
        val error =
            DurationDeserialize
                .deserialize(
                    listOf(
                        ULong.MAX_VALUE.intoDeserializer(),
                        1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing Duration", error?.message)
    }

    @Test
    public fun durationOverflowStructReportsUpstreamError() {
        val error =
            DurationDeserialize
                .deserialize(
                    mapOf(
                        "secs".intoDeserializer() to ULong.MAX_VALUE.intoDeserializer(),
                        "nanos".intoDeserializer() to 1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing Duration", error?.message)
    }

    @Test
    public fun systemTimeEpochOffsetOverflowSeqReportsUpstreamError() {
        val error =
            SystemTimeDeserialize
                .deserialize(
                    listOf(
                        ULong.MAX_VALUE.intoDeserializer(),
                        1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing SystemTime epoch offset", error?.message)
    }

    @Test
    public fun systemTimeEpochOffsetOverflowStructReportsUpstreamError() {
        val error =
            SystemTimeDeserialize
                .deserialize(
                    mapOf(
                        "secs_since_epoch".intoDeserializer() to ULong.MAX_VALUE.intoDeserializer(),
                        "nanos_since_epoch".intoDeserializer() to 1_000_000_000u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing SystemTime epoch offset", error?.message)
    }

    @Test
    public fun systemTimeOverflowReportsUpstreamError() {
        val error =
            SystemTimeDeserialize
                .deserialize(
                    listOf(
                        ULong.MAX_VALUE.intoDeserializer(),
                        0u.intoDeserializer(),
                    ).intoDeserializer(),
                ).exceptionOrNull()

        assertEquals("overflow deserializing SystemTime", error?.message)
    }
}

private data object UnitStructDeserializer : Deserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()
    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(
        name: String,
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(
        name: String,
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(
        name: String,
        variants: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
}
