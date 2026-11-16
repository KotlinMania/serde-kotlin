// port-lint: tests test_suite/tests/test_de.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.priv.InPlaceSeed
import io.github.kotlinmania.serde.priv.missingField
import io.github.kotlinmania.serdecore.de.value.F32Deserializer
import io.github.kotlinmania.serdecore.de.value.F64Deserializer
import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertDeTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class TestDeTest {
    private fun <T> test(value: T, deserialize: Deserialize<T>, tokens: List<Token>) {
        // Test ser/de roundtripping
        assertDeTokens(value, deserialize, tokens)
        // Test that the tokens are ignorable
        assertDeTokensIgnore(tokens)
    }

    private data class IgnoreBase(
        var a: Int,
    )

    private enum class IgnoreField { Field0, Ignore }

    private object IgnoreFieldDeserialize : Deserialize<IgnoreField> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<IgnoreField> =
            deserializer.deserializeIdentifier(
                object : Visitor<IgnoreField> {
                    override fun expecting(): String = "field identifier"
                    override fun visitU64(v: ULong): SerdeResult<IgnoreField> =
                        SerdeResult.success(if (v == 0uL) IgnoreField.Field0 else IgnoreField.Ignore)
                    override fun visitStr(v: String): SerdeResult<IgnoreField> =
                        SerdeResult.success(if (v == "a") IgnoreField.Field0 else IgnoreField.Ignore)
                    override fun visitBytes(v: ByteArray): SerdeResult<IgnoreField> =
                        SerdeResult.success(if (v.contentEquals("a".encodeToByteArray())) IgnoreField.Field0 else IgnoreField.Ignore)
                },
            )
    }

    private object IgnoreBaseDeserialize : Deserialize<IgnoreBase> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<IgnoreBase> =
            deserializer.deserializeStruct(
                "IgnoreBase",
                listOf("a"),
                object : Visitor<IgnoreBase> {
                    override fun expecting(): String = "struct IgnoreBase"
                    override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<IgnoreBase> {
                        val field0 =
                            when (val value = access.nextElement(I32Deserialize).getOrElse { return SerdeResult.failure(it) }) {
                                is SeqElement.Some -> value.value
                                SeqElement.None -> return SerdeResult.failure(SerdeError.invalidLength(0, Expected { "struct IgnoreBase with 1 element" }))
                            }
                        return SerdeResult.success(IgnoreBase(field0))
                    }
                    override fun <A : MapAccess> visitMap(access: A): SerdeResult<IgnoreBase> {
                        var field0: Int? = null
                        while (true) {
                            val key = access.nextKey(IgnoreFieldDeserialize).getOrElse { return SerdeResult.failure(it) } ?: break
                            when (key) {
                                IgnoreField.Field0 -> {
                                    if (field0 != null) return SerdeResult.failure(SerdeError.duplicateField("a"))
                                    field0 = access.nextValue(I32Deserialize).getOrElse { return SerdeResult.failure(it) }
                                }
                                IgnoreField.Ignore -> access.nextValue(IgnoredAny).getOrElse { return SerdeResult.failure(it) }
                            }
                        }
                        val value = field0 ?: missingField("a", I32Deserialize).getOrElse { return SerdeResult.failure(it) }
                        return SerdeResult.success(IgnoreBase(value))
                    }
                },
            )

        override fun <D : Deserializer> deserializeInPlace(deserializer: D, place: MutablePlace<IgnoreBase>): SerdeResult<Unit> =
            deserializer.deserializeStruct(
                "IgnoreBase",
                listOf("a"),
                object : Visitor<Unit> {
                    private val fieldPlace = mutablePlace({ place.value.a }, { place.value.a = it })
                    private val fieldSeed = InPlaceSeed(fieldPlace) { input, slot -> I32Deserialize.deserializeInPlace(input, slot) }
                    override fun expecting(): String = "struct IgnoreBase"
                    override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Unit> {
                        if (access.nextElementSeed(fieldSeed).getOrElse { return SerdeResult.failure(it) } == SeqElement.None) {
                            return SerdeResult.failure(SerdeError.invalidLength(0, Expected { "struct IgnoreBase with 1 element" }))
                        }
                        return SerdeResult.success(Unit)
                    }
                    override fun <A : MapAccess> visitMap(access: A): SerdeResult<Unit> {
                        var field0 = false
                        while (true) {
                            val key = access.nextKey(IgnoreFieldDeserialize).getOrElse { return SerdeResult.failure(it) } ?: break
                            when (key) {
                                IgnoreField.Field0 -> {
                                    if (field0) return SerdeResult.failure(SerdeError.duplicateField("a"))
                                    access.nextValueSeed(fieldSeed).getOrElse { return SerdeResult.failure(it) }
                                    field0 = true
                                }
                                IgnoreField.Ignore -> access.nextValue(IgnoredAny).getOrElse { return SerdeResult.failure(it) }
                            }
                        }
                        if (!field0) place.value.a = missingField("a", I32Deserialize).getOrElse { return SerdeResult.failure(it) }
                        return SerdeResult.success(Unit)
                    }
                },
            )
    }

    private fun assertDeTokensIgnore(ignorableTokens: List<Token>) {
        // Embed the tokens to be ignored in the normal token
        // stream for an IgnoreBase type
        val concatedTokens =
            listOf(Token.Map(len = 2), Token.Str("a"), Token.I32(1), Token.Str("ignored")) +
                ignorableTokens + Token.MapEnd
        val expected = IgnoreBase(a = 1)
        assertDeTokens(expected, IgnoreBaseDeserialize, concatedTokens)
        assertDeTokens(
            expected,
            object : Deserialize<IgnoreBase> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<IgnoreBase> {
                    var actual = IgnoreBase(a = 0)
                    val original = actual
                    return IgnoreBaseDeserialize
                        .deserializeInPlace(
                            deserializer,
                            mutablePlace({ actual }, { actual = it }),
                        ).map {
                            assertEquals(expected, actual)
                            assertSame(original, actual)
                            actual
                        }
                }
            },
            concatedTokens,
        )
    }

    @Test
    public fun testBool() {
        fun test(value: Boolean, tokens: List<Token>) = test(value, BooleanDeserialize, tokens)
        test(true, listOf(Token.Bool(true)))
        test(false, listOf(Token.Bool(false)))
    }

    @Test
    public fun testI8() {
        fun test(value: Byte, tokens: List<Token>) = test(value, I8Deserialize, tokens)
        // from signed
        test(-128, listOf(Token.I8(-128)))
        test(-128, listOf(Token.I16(-128)))
        test(-128, listOf(Token.I32(-128)))
        test(-128, listOf(Token.I64(-128)))
        test(127, listOf(Token.I8(127)))
        test(127, listOf(Token.I16(127)))
        test(127, listOf(Token.I32(127)))
        test(127, listOf(Token.I64(127)))
        // from unsigned
        test(0, listOf(Token.U8(0u)))
        test(0, listOf(Token.U16(0u)))
        test(0, listOf(Token.U32(0u)))
        test(0, listOf(Token.U64(0uL)))
        test(127, listOf(Token.U8(127u)))
        test(127, listOf(Token.U16(127u)))
        test(127, listOf(Token.U32(127u)))
        test(127, listOf(Token.U64(127uL)))
    }

    @Test
    public fun testI16() {
        fun test(value: Short, tokens: List<Token>) = test(value, I16Deserialize, tokens)
        // from signed
        test(-128, listOf(Token.I8(-128)))
        test(-32768, listOf(Token.I16(-32768)))
        test(-32768, listOf(Token.I32(-32768)))
        test(-32768, listOf(Token.I64(-32768)))
        test(127, listOf(Token.I8(127)))
        test(32767, listOf(Token.I16(32767)))
        test(32767, listOf(Token.I32(32767)))
        test(32767, listOf(Token.I64(32767)))
        // from unsigned
        test(0, listOf(Token.U8(0u)))
        test(0, listOf(Token.U16(0u)))
        test(0, listOf(Token.U32(0u)))
        test(0, listOf(Token.U64(0uL)))
        test(255, listOf(Token.U8(255u)))
        test(32767, listOf(Token.U16(32767u)))
        test(32767, listOf(Token.U32(32767u)))
        test(32767, listOf(Token.U64(32767uL)))
    }

    @Test
    public fun testI32() {
        fun test(value: Int, tokens: List<Token>) = test(value, I32Deserialize, tokens)
        // from signed
        test(-128, listOf(Token.I8(-128)))
        test(-32768, listOf(Token.I16(-32768)))
        test(-2147483648, listOf(Token.I32(-2147483648)))
        test(-2147483648, listOf(Token.I64(-2147483648)))
        test(127, listOf(Token.I8(127)))
        test(32767, listOf(Token.I16(32767)))
        test(2147483647, listOf(Token.I32(2147483647)))
        test(2147483647, listOf(Token.I64(2147483647)))
        // from unsigned
        test(0, listOf(Token.U8(0u)))
        test(0, listOf(Token.U16(0u)))
        test(0, listOf(Token.U32(0u)))
        test(0, listOf(Token.U64(0uL)))
        test(255, listOf(Token.U8(255u)))
        test(65535, listOf(Token.U16(65535u)))
        test(2147483647, listOf(Token.U32(2147483647u)))
        test(2147483647, listOf(Token.U64(2147483647uL)))
    }

    @Test
    public fun testI64() {
        fun test(value: Long, tokens: List<Token>) = test(value, I64Deserialize, tokens)
        // from signed
        test(-128, listOf(Token.I8(-128)))
        test(-32768, listOf(Token.I16(-32768)))
        test(-2147483648, listOf(Token.I32(-2147483648)))
        test(Long.MIN_VALUE, listOf(Token.I64(Long.MIN_VALUE)))
        test(127, listOf(Token.I8(127)))
        test(32767, listOf(Token.I16(32767)))
        test(2147483647, listOf(Token.I32(2147483647)))
        test(9223372036854775807, listOf(Token.I64(9223372036854775807)))
        // from unsigned
        test(0, listOf(Token.U8(0u)))
        test(0, listOf(Token.U16(0u)))
        test(0, listOf(Token.U32(0u)))
        test(0, listOf(Token.U64(0uL)))
        test(255, listOf(Token.U8(255u)))
        test(65535, listOf(Token.U16(65535u)))
        test(4294967295, listOf(Token.U32(4294967295u)))
        test(9223372036854775807, listOf(Token.U64(9223372036854775807uL)))
    }

    @Test
    public fun testU8() {
        fun test(value: UByte, tokens: List<Token>) = test(value, U8Deserialize, tokens)
        // from signed
        test(0u, listOf(Token.I8(0)))
        test(0u, listOf(Token.I16(0)))
        test(0u, listOf(Token.I32(0)))
        test(0u, listOf(Token.I64(0)))
        test(127u, listOf(Token.I8(127)))
        test(255u, listOf(Token.I16(255)))
        test(255u, listOf(Token.I32(255)))
        test(255u, listOf(Token.I64(255)))
        // from unsigned
        test(0u, listOf(Token.U8(0u)))
        test(0u, listOf(Token.U16(0u)))
        test(0u, listOf(Token.U32(0u)))
        test(0u, listOf(Token.U64(0uL)))
        test(255u, listOf(Token.U8(255u)))
        test(255u, listOf(Token.U16(255u)))
        test(255u, listOf(Token.U32(255u)))
        test(255u, listOf(Token.U64(255uL)))
    }

    @Test
    public fun testU16() {
        fun test(value: UShort, tokens: List<Token>) = test(value, U16Deserialize, tokens)
        // from signed
        test(0u, listOf(Token.I8(0)))
        test(0u, listOf(Token.I16(0)))
        test(0u, listOf(Token.I32(0)))
        test(0u, listOf(Token.I64(0)))
        test(127u, listOf(Token.I8(127)))
        test(32767u, listOf(Token.I16(32767)))
        test(65535u, listOf(Token.I32(65535)))
        test(65535u, listOf(Token.I64(65535)))
        // from unsigned
        test(0u, listOf(Token.U8(0u)))
        test(0u, listOf(Token.U16(0u)))
        test(0u, listOf(Token.U32(0u)))
        test(0u, listOf(Token.U64(0uL)))
        test(255u, listOf(Token.U8(255u)))
        test(65535u, listOf(Token.U16(65535u)))
        test(65535u, listOf(Token.U32(65535u)))
        test(65535u, listOf(Token.U64(65535uL)))
    }

    @Test
    public fun testU32() {
        fun test(value: UInt, tokens: List<Token>) = test(value, U32Deserialize, tokens)
        // from signed
        test(0u, listOf(Token.I8(0)))
        test(0u, listOf(Token.I16(0)))
        test(0u, listOf(Token.I32(0)))
        test(0u, listOf(Token.I64(0)))
        test(127u, listOf(Token.I8(127)))
        test(32767u, listOf(Token.I16(32767)))
        test(2147483647u, listOf(Token.I32(2147483647)))
        test(4294967295u, listOf(Token.I64(4294967295)))
        // from unsigned
        test(0u, listOf(Token.U8(0u)))
        test(0u, listOf(Token.U16(0u)))
        test(0u, listOf(Token.U32(0u)))
        test(0u, listOf(Token.U64(0uL)))
        test(255u, listOf(Token.U8(255u)))
        test(65535u, listOf(Token.U16(65535u)))
        test(4294967295u, listOf(Token.U32(4294967295u)))
        test(4294967295u, listOf(Token.U64(4294967295uL)))
    }

    @Test
    public fun testU64() {
        fun test(value: ULong, tokens: List<Token>) = test(value, U64Deserialize, tokens)
        // from signed
        test(0uL, listOf(Token.I8(0)))
        test(0uL, listOf(Token.I16(0)))
        test(0uL, listOf(Token.I32(0)))
        test(0uL, listOf(Token.I64(0)))
        test(127uL, listOf(Token.I8(127)))
        test(32767uL, listOf(Token.I16(32767)))
        test(2147483647uL, listOf(Token.I32(2147483647)))
        test(9223372036854775807uL, listOf(Token.I64(9223372036854775807)))
        // from unsigned
        test(0uL, listOf(Token.U8(0u)))
        test(0uL, listOf(Token.U16(0u)))
        test(0uL, listOf(Token.U32(0u)))
        test(0uL, listOf(Token.U64(0uL)))
        test(255uL, listOf(Token.U8(255u)))
        test(65535uL, listOf(Token.U16(65535u)))
        test(4294967295uL, listOf(Token.U32(4294967295u)))
        test(18446744073709551615uL, listOf(Token.U64(18446744073709551615uL)))
    }

    @Test
    public fun testF32() {
        fun test(value: Float, tokens: List<Token>) = test(value, F32Deserialize, tokens)
        test(1.11f, listOf(Token.F32(1.11f)))
        test(1.11f, listOf(Token.F64(1.11)))
    }

    @Test
    public fun testF64() {
        fun test(value: Double, tokens: List<Token>) = test(value, F64Deserialize, tokens)
        test(1.11f.toDouble(), listOf(Token.F32(1.11f)))
        test(1.11, listOf(Token.F64(1.11)))
    }

    @Test
    public fun testNan() {
        val f32Deserializer = F32Deserializer.Companion::new
        val f64Deserializer = F64Deserializer.Companion::new
        val posF32Nan = f32Deserializer(Float.fromBits(0x7fc00000))
        val posF64Nan = f64Deserializer(Double.fromBits(0x7ff8000000000000L))
        check(F32Deserialize.deserialize(posF32Nan).getOrThrow().toRawBits() and Int.MIN_VALUE == 0)
        check(F32Deserialize.deserialize(posF64Nan).getOrThrow().toRawBits() and Int.MIN_VALUE == 0)
        check(F64Deserialize.deserialize(posF32Nan).getOrThrow().toRawBits() and Long.MIN_VALUE == 0L)
        check(F64Deserialize.deserialize(posF64Nan).getOrThrow().toRawBits() and Long.MIN_VALUE == 0L)
        val negF32Nan = f32Deserializer(Float.fromBits(0xffc00000u.toInt()))
        val negF64Nan = f64Deserializer(Double.fromBits(0xfff8000000000000uL.toLong()))
        check(F32Deserialize.deserialize(negF32Nan).getOrThrow().toRawBits() and Int.MIN_VALUE != 0)
        check(F32Deserialize.deserialize(negF64Nan).getOrThrow().toRawBits() and Int.MIN_VALUE != 0)
        check(F64Deserialize.deserialize(negF32Nan).getOrThrow().toRawBits() and Long.MIN_VALUE != 0L)
        check(F64Deserialize.deserialize(negF64Nan).getOrThrow().toRawBits() and Long.MIN_VALUE != 0L)
    }

    @Test
    public fun testChar() {
        fun test(value: Char, tokens: List<Token>) = test(value, CharDeserialize, tokens)
        test('a', listOf(Token.CharValue('a')))
        test('a', listOf(Token.Str("a")))
        test('a', listOf(Token.StringValue("a")))
    }

    @Test
    public fun testString() {
        fun test(value: String, tokens: List<Token>) = test(value, StringDeserialize, tokens)
        test("abc", listOf(Token.Str("abc")))
        test("abc", listOf(Token.StringValue("abc")))
        test("a", listOf(Token.CharValue('a')))
    }

    @Test
    public fun testOption() {
        fun test(value: Int?, tokens: List<Token>) = test(value, nullableDeserialize(I32Deserialize), tokens)
        test(null, listOf(Token.UnitValue))
        test(null, listOf(Token.None))
        test(1, listOf(Token.Some, Token.I32(1)))
    }

    @Test
    public fun testResult() {
        fun test(value: ResultValue<Int, Int>, tokens: List<Token>) = test(value, resultDeserialize(I32Deserialize, I32Deserialize), tokens)
        test(ResultValue.Ok(0), listOf(Token.Enum(name = "Result"), Token.Str("Ok"), Token.I32(0)))
        test(ResultValue.Err(1), listOf(Token.Enum(name = "Result"), Token.Str("Err"), Token.I32(1)))
    }

    @Test
    public fun testUnit() {
        fun test(value: Unit, tokens: List<Token>) = test(value, UnitDeserialize, tokens)
        test(Unit, listOf(Token.UnitValue))
    }

    @Test
    public fun testDuration() {
        fun test(value: kotlin.time.Duration, tokens: List<Token>) = test(value, DurationDeserialize, tokens)
        test(1.seconds + 2.nanoseconds, listOf(Token.Struct(name = "Duration", len = 2), Token.Str("secs"), Token.U64(1uL), Token.Str("nanos"), Token.U32(2u), Token.StructEnd))
        test(1.seconds + 2.nanoseconds, listOf(Token.Seq(len = 2), Token.I64(1), Token.I64(2), Token.SeqEnd))
    }

    @Test
    public fun testSystemTime() {
        fun test(value: kotlin.time.Instant, tokens: List<Token>) = test(value, SystemTimeDeserialize, tokens)
        test(kotlin.time.Instant.fromEpochSeconds(1, 2), listOf(Token.Struct(name = "SystemTime", len = 2), Token.Str("secs_since_epoch"), Token.U64(1uL), Token.Str("nanos_since_epoch"), Token.U32(2u), Token.StructEnd))
        test(kotlin.time.Instant.fromEpochSeconds(1, 2), listOf(Token.Seq(len = 2), Token.I64(1), Token.I64(2), Token.SeqEnd))
    }

    @Test
    public fun testRange() {
        fun test(value: RangeValue<UInt>, tokens: List<Token>) = test(value, rangeDeserialize(U32Deserialize), tokens)
        test(RangeValue(1u, 2u), listOf(Token.Struct(name = "Range", len = 2), Token.Str("start"), Token.U32(1u), Token.Str("end"), Token.U32(2u), Token.StructEnd))
        test(RangeValue(1u, 2u), listOf(Token.Seq(len = 2), Token.U64(1uL), Token.U64(2uL), Token.SeqEnd))
    }

    @Test
    public fun testRangeInclusive() {
        fun test(value: RangeInclusiveValue<UInt>, tokens: List<Token>) = test(value, rangeInclusiveDeserialize(U32Deserialize), tokens)
        test(RangeInclusiveValue(1u, 2u), listOf(Token.Struct(name = "RangeInclusive", len = 2), Token.Str("start"), Token.U32(1u), Token.Str("end"), Token.U32(2u), Token.StructEnd))
        test(RangeInclusiveValue(1u, 2u), listOf(Token.Seq(len = 2), Token.U64(1uL), Token.U64(2uL), Token.SeqEnd))
    }

    @Test
    public fun testRangeFrom() {
        fun test(value: RangeFromValue<UInt>, tokens: List<Token>) = test(value, rangeFromDeserialize(U32Deserialize), tokens)
        test(RangeFromValue(1u), listOf(Token.Struct(name = "RangeFrom", len = 1), Token.Str("start"), Token.U32(1u), Token.StructEnd))
        test(RangeFromValue(1u), listOf(Token.Seq(len = 1), Token.U32(1u), Token.SeqEnd))
    }

    @Test
    public fun testRangeTo() {
        fun test(value: RangeToValue<UInt>, tokens: List<Token>) = test(value, rangeToDeserialize(U32Deserialize), tokens)
        test(RangeToValue(2u), listOf(Token.Struct(name = "RangeTo", len = 1), Token.Str("end"), Token.U32(2u), Token.StructEnd))
        test(RangeToValue(2u), listOf(Token.Seq(len = 1), Token.U32(2u), Token.SeqEnd))
    }

    @Test
    public fun testBound() {
        test(BoundValue.Unbounded, boundDeserialize(UnitDeserialize), listOf(Token.Enum(name = "Bound"), Token.Str("Unbounded"), Token.UnitValue))
        test(BoundValue.Included(0), boundDeserialize(I32Deserialize), listOf(Token.Enum(name = "Bound"), Token.Str("Included"), Token.U8(0u)))
        test(BoundValue.Excluded(0), boundDeserialize(I32Deserialize), listOf(Token.Enum(name = "Bound"), Token.Str("Excluded"), Token.U8(0u)))
    }

    @Test
    public fun testPath() {
        fun test(value: PathValue, tokens: List<Token>) = test(value, PathDeserialize, tokens)
        test(PathValue("/usr/local/lib"), listOf(Token.BorrowedStr("/usr/local/lib")))
        test(PathValue("/usr/local/lib"), listOf(Token.BorrowedBytes("/usr/local/lib".encodeToByteArray())))
    }

    @Test
    public fun testPathBuf() {
        fun test(value: PathValue, tokens: List<Token>) = test(value, PathBufDeserialize, tokens)
        test(PathValue("/usr/local/lib"), listOf(Token.Str("/usr/local/lib")))
        test(PathValue("/usr/local/lib"), listOf(Token.StringValue("/usr/local/lib")))
        test(PathValue("/usr/local/lib"), listOf(Token.Bytes("/usr/local/lib".encodeToByteArray())))
        test(PathValue("/usr/local/lib"), listOf(Token.ByteBuf("/usr/local/lib".encodeToByteArray())))
    }

    @Test
    public fun testBoxedPath() {
        fun test(value: BoxedPathValue, tokens: List<Token>) = test(value, BoxedPathDeserialize, tokens)
        test(BoxedPathValue(PathValue("/usr/local/lib")), listOf(Token.Str("/usr/local/lib")))
        test(BoxedPathValue(PathValue("/usr/local/lib")), listOf(Token.StringValue("/usr/local/lib")))
        test(BoxedPathValue(PathValue("/usr/local/lib")), listOf(Token.Bytes("/usr/local/lib".encodeToByteArray())))
        test(BoxedPathValue(PathValue("/usr/local/lib")), listOf(Token.ByteBuf("/usr/local/lib".encodeToByteArray())))
    }

    @Test
    public fun testCstring() {
        fun test(value: CStringValue, tokens: List<Token>) = test(value, CStringDeserialize, tokens)
        test(CStringValue("abc".encodeToByteArray()), listOf(Token.Bytes("abc".encodeToByteArray())))
    }
}
