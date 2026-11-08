// port-lint: tests test_suite/tests/test_de_error.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serdecore.de.value.I128Deserializer
import io.github.kotlinmania.serdecore.de.value.U128Deserializer
import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokensError
import kotlin.test.Test

class TestDeErrorTest {
    @Test
    public fun testI8() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(I8Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I16(-129)), "invalid value: integer `-129`, expected i8")
        test(listOf(Token.I32(-129)), "invalid value: integer `-129`, expected i8")
        test(listOf(Token.I64(-129)), "invalid value: integer `-129`, expected i8")
        test(listOf(Token.I16(128)), "invalid value: integer `128`, expected i8")
        test(listOf(Token.I32(128)), "invalid value: integer `128`, expected i8")
        test(listOf(Token.I64(128)), "invalid value: integer `128`, expected i8")
        // from unsigned
        test(listOf(Token.U8(128u)), "invalid value: integer `128`, expected i8")
        test(listOf(Token.U16(128u)), "invalid value: integer `128`, expected i8")
        test(listOf(Token.U32(128u)), "invalid value: integer `128`, expected i8")
        test(listOf(Token.U64(128uL)), "invalid value: integer `128`, expected i8")
    }

    @Test
    public fun testI16() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(I16Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I32(-32769)), "invalid value: integer `-32769`, expected i16")
        test(listOf(Token.I64(-32769)), "invalid value: integer `-32769`, expected i16")
        test(listOf(Token.I32(32768)), "invalid value: integer `32768`, expected i16")
        test(listOf(Token.I64(32768)), "invalid value: integer `32768`, expected i16")
        // from unsigned
        test(listOf(Token.U16(32768u)), "invalid value: integer `32768`, expected i16")
        test(listOf(Token.U32(32768u)), "invalid value: integer `32768`, expected i16")
        test(listOf(Token.U64(32768uL)), "invalid value: integer `32768`, expected i16")
    }

    @Test
    public fun testI32() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(I32Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I64(-2147483649)), "invalid value: integer `-2147483649`, expected i32")
        test(listOf(Token.I64(2147483648)), "invalid value: integer `2147483648`, expected i32")
        // from unsigned
        test(listOf(Token.U32(2147483648u)), "invalid value: integer `2147483648`, expected i32")
        test(listOf(Token.U64(2147483648uL)), "invalid value: integer `2147483648`, expected i32")
    }

    @Test
    public fun testI64() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(I64Deserialize, tokens, message)
        // from unsigned
        test(listOf(Token.U64(9223372036854775808uL)), "invalid value: integer `9223372036854775808`, expected i64")
    }

    @Test
    public fun testI128() {
        val deserializer = I128Deserializer.new("1")
        val error = BorrowedStrDeserialize.deserialize(deserializer).exceptionOrNull() ?: error("deserialization must fail")
        check(error.toString() == "invalid type: integer `1` as i128, expected a borrowed string")
    }

    @Test
    public fun testU8() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(U8Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I8(-1)), "invalid value: integer `-1`, expected u8")
        test(listOf(Token.I16(-1)), "invalid value: integer `-1`, expected u8")
        test(listOf(Token.I32(-1)), "invalid value: integer `-1`, expected u8")
        test(listOf(Token.I64(-1)), "invalid value: integer `-1`, expected u8")
        test(listOf(Token.I16(256)), "invalid value: integer `256`, expected u8")
        test(listOf(Token.I32(256)), "invalid value: integer `256`, expected u8")
        test(listOf(Token.I64(256)), "invalid value: integer `256`, expected u8")
        // from unsigned
        test(listOf(Token.U16(256u)), "invalid value: integer `256`, expected u8")
        test(listOf(Token.U32(256u)), "invalid value: integer `256`, expected u8")
        test(listOf(Token.U64(256uL)), "invalid value: integer `256`, expected u8")
    }

    @Test
    public fun testU16() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(U16Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I8(-1)), "invalid value: integer `-1`, expected u16")
        test(listOf(Token.I16(-1)), "invalid value: integer `-1`, expected u16")
        test(listOf(Token.I32(-1)), "invalid value: integer `-1`, expected u16")
        test(listOf(Token.I64(-1)), "invalid value: integer `-1`, expected u16")
        test(listOf(Token.I32(65536)), "invalid value: integer `65536`, expected u16")
        test(listOf(Token.I64(65536)), "invalid value: integer `65536`, expected u16")
        // from unsigned
        test(listOf(Token.U32(65536u)), "invalid value: integer `65536`, expected u16")
        test(listOf(Token.U64(65536uL)), "invalid value: integer `65536`, expected u16")
    }

    @Test
    public fun testU32() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(U32Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I8(-1)), "invalid value: integer `-1`, expected u32")
        test(listOf(Token.I16(-1)), "invalid value: integer `-1`, expected u32")
        test(listOf(Token.I32(-1)), "invalid value: integer `-1`, expected u32")
        test(listOf(Token.I64(-1)), "invalid value: integer `-1`, expected u32")
        test(listOf(Token.I64(4294967296)), "invalid value: integer `4294967296`, expected u32")
        // from unsigned
        test(listOf(Token.U64(4294967296uL)), "invalid value: integer `4294967296`, expected u32")
    }

    @Test
    public fun testU64() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(U64Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I8(-1)), "invalid value: integer `-1`, expected u64")
        test(listOf(Token.I16(-1)), "invalid value: integer `-1`, expected u64")
        test(listOf(Token.I32(-1)), "invalid value: integer `-1`, expected u64")
        test(listOf(Token.I64(-1)), "invalid value: integer `-1`, expected u64")
        val deserializer = 1uL.intoDeserializer()
        val error = BorrowedStrDeserialize.deserialize(deserializer).exceptionOrNull() ?: error("deserialization must fail")
        check(error.toString() == "invalid type: integer `1`, expected a borrowed string")
    }

    @Test
    public fun testU128() {
        fun test(tokens: List<Token>, message: String) = assertDeTokensError(U128Deserialize, tokens, message)
        // from signed
        test(listOf(Token.I8(-1)), "invalid value: integer `-1`, expected u128")
        test(listOf(Token.I16(-1)), "invalid value: integer `-1`, expected u128")
        test(listOf(Token.I32(-1)), "invalid value: integer `-1`, expected u128")
        test(listOf(Token.I64(-1)), "invalid value: integer `-1`, expected u128")
        val deserializer = U128Deserializer.new("1")
        val error = BorrowedStrDeserialize.deserialize(deserializer).exceptionOrNull() ?: error("deserialization must fail")
        check(error.toString() == "invalid type: integer `1` as u128, expected a borrowed string")
    }

    @Test
    public fun testShortTuple() {
        assertDeTokensError(tripleDeserialize(U8Deserialize, U8Deserialize, U8Deserialize), listOf(Token.Tuple(len = 1), Token.U8(1u), Token.TupleEnd), "invalid length 1, expected a tuple of size 3")
    }

    @Test
    public fun testShortArray() {
        assertDeTokensError(arrayDeserialize(3, U8Deserialize), listOf(Token.Seq(len = 1), Token.U8(1u), Token.SeqEnd), "invalid length 1, expected an array of length 3")
    }

    @Test
    public fun testCstringInternalNull() {
        assertDeTokensError(CStringDeserialize, listOf(Token.Bytes("a\u0000c".encodeToByteArray())), "nul byte found in provided data at position: 1")
    }

    @Test
    public fun testCstringInternalNullEnd() {
        assertDeTokensError(CStringDeserialize, listOf(Token.Bytes("ac\u0000".encodeToByteArray())), "nul byte found in provided data at position: 2")
    }

    @Test
    public fun testUnitFromEmptySeq() {
        assertDeTokensError(UnitDeserialize, listOf(Token.Seq(len = 0), Token.SeqEnd), "invalid type: sequence, expected unit")
    }

    @Test
    public fun testUnitFromEmptySeqWithoutLen() {
        assertDeTokensError(UnitDeserialize, listOf(Token.Seq(len = null), Token.SeqEnd), "invalid type: sequence, expected unit")
    }

    @Test
    public fun testUnitFromTupleStruct() {
        assertDeTokensError(UnitDeserialize, listOf(Token.TupleStruct(name = "Anything", len = 0), Token.TupleStructEnd), "invalid type: sequence, expected unit")
    }

    @Test
    public fun testStringFromUnit() {
        assertDeTokensError(StringDeserialize, listOf(Token.UnitValue), "invalid type: unit value, expected a string")
    }

    @Test
    public fun testBoolFromString() {
        assertDeTokensError(BooleanDeserialize, listOf(Token.Str("false")), "invalid type: string \"false\", expected a boolean")
    }

    @Test
    public fun testDurationOverflowSeq() {
        assertDeTokensError(DurationDeserialize, listOf(Token.Seq(len = 2), Token.U64(ULong.MAX_VALUE), Token.U32(1000000000u), Token.SeqEnd), "overflow deserializing Duration")
    }

    @Test
    public fun testDurationOverflowStruct() {
        assertDeTokensError(DurationDeserialize, listOf(Token.Struct(name = "Duration", len = 2), Token.Str("secs"), Token.U64(ULong.MAX_VALUE), Token.Str("nanos"), Token.U32(1000000000u), Token.StructEnd), "overflow deserializing Duration")
    }

    @Test
    public fun testSystemtimeOverflowSeq() {
        assertDeTokensError(SystemTimeDeserialize, listOf(Token.Seq(len = 2), Token.U64(ULong.MAX_VALUE), Token.U32(1000000000u), Token.SeqEnd), "overflow deserializing SystemTime epoch offset")
    }

    @Test
    public fun testSystemtimeOverflowStruct() {
        assertDeTokensError(SystemTimeDeserialize, listOf(Token.Struct(name = "SystemTime", len = 2), Token.Str("secs_since_epoch"), Token.U64(ULong.MAX_VALUE), Token.Str("nanos_since_epoch"), Token.U32(1000000000u), Token.StructEnd), "overflow deserializing SystemTime epoch offset")
    }

    @Test
    public fun testSystemtimeOverflow() {
        assertDeTokensError(SystemTimeDeserialize, listOf(Token.Seq(len = 2), Token.U64(ULong.MAX_VALUE), Token.U32(0u), Token.SeqEnd), "overflow deserializing SystemTime")
    }
}
