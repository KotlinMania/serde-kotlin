package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IntoDeserializer
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.priv.SharedByteSlice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PrimitiveValueParityTest {
    private data class Payload(
        val input: Deserializer,
        val expected: String,
    )

    private fun payloads(): List<Payload> =
        listOf(
            Payload(BoolDeserializer.new(true), "bool:true"),
            Payload(I8Deserializer.new(Byte.MIN_VALUE), "i8:-128"),
            Payload(I16Deserializer.new(Short.MIN_VALUE), "i16:-32768"),
            Payload(I32Deserializer.new(Int.MIN_VALUE), "i32:-2147483648"),
            Payload(I64Deserializer.new(Long.MIN_VALUE), "i64:-9223372036854775808"),
            Payload(I128Deserializer.new("-170141183460469231731687303715884105728"), "i128:-170141183460469231731687303715884105728"),
            Payload(IsizeDeserializer.new(Long.MAX_VALUE), "i64:9223372036854775807"),
            Payload(U8Deserializer.new(UByte.MAX_VALUE), "u8:255"),
            Payload(U16Deserializer.new(UShort.MAX_VALUE), "u16:65535"),
            Payload(U64Deserializer.new(ULong.MAX_VALUE), "u64:18446744073709551615"),
            Payload(U128Deserializer.new("340282366920938463463374607431768211455"), "u128:340282366920938463463374607431768211455"),
            Payload(UsizeDeserializer.new(ULong.MAX_VALUE), "u64:18446744073709551615"),
            Payload(F32Deserializer.new(-0.0f), "f32:-2147483648"),
            Payload(F64Deserializer.new(Double.POSITIVE_INFINITY), "f64:9218868437227405312"),
            Payload(CharDeserializer.new('Ω'), "char:Ω"),
        )

    private class ProbeVisitor(
        private val observe: (String) -> SerdeResult<String>,
    ) : Visitor<String> {
        override fun expecting(): String = "a primitive value"
        override fun visitBool(v: Boolean): SerdeResult<String> = observe("bool:$v")
        override fun visitI8(v: Byte): SerdeResult<String> = observe("i8:$v")
        override fun visitI16(v: Short): SerdeResult<String> = observe("i16:$v")
        override fun visitI32(v: Int): SerdeResult<String> = observe("i32:$v")
        override fun visitI64(v: Long): SerdeResult<String> = observe("i64:$v")
        override fun visitI128(v: String): SerdeResult<String> = observe("i128:$v")
        override fun visitU8(v: UByte): SerdeResult<String> = observe("u8:$v")
        override fun visitU16(v: UShort): SerdeResult<String> = observe("u16:$v")
        override fun visitU64(v: ULong): SerdeResult<String> = observe("u64:$v")
        override fun visitU128(v: String): SerdeResult<String> = observe("u128:$v")
        override fun visitF32(v: Float): SerdeResult<String> = observe("f32:${v.toRawBits()}")
        override fun visitF64(v: Double): SerdeResult<String> = observe("f64:${v.toRawBits()}")
        override fun visitChar(v: Char): SerdeResult<String> = observe("char:$v")
    }

    private fun forwarded(input: Deserializer, visitor: Visitor<String>): List<SerdeResult<String>> =
        listOf(
            input.deserializeAny(visitor),
            input.deserializeBool(visitor),
            input.deserializeI8(visitor),
            input.deserializeI16(visitor),
            input.deserializeI32(visitor),
            input.deserializeI64(visitor),
            input.deserializeI128(visitor),
            input.deserializeU8(visitor),
            input.deserializeU16(visitor),
            input.deserializeU32(visitor),
            input.deserializeU64(visitor),
            input.deserializeU128(visitor),
            input.deserializeF32(visitor),
            input.deserializeF64(visitor),
            input.deserializeChar(visitor),
            input.deserializeStr(visitor),
            input.deserializeString(visitor),
            input.deserializeBytes(visitor),
            input.deserializeByteBuf(visitor),
            input.deserializeOption(visitor),
            input.deserializeUnit(visitor),
            input.deserializeUnitStruct("UnitName", visitor),
            input.deserializeNewtypeStruct("NewtypeName", visitor),
            input.deserializeSeq(visitor),
            input.deserializeTuple(2, visitor),
            input.deserializeTupleStruct("TupleName", 2, visitor),
            input.deserializeMap(visitor),
            input.deserializeStruct("StructName", listOf("field"), visitor),
            input.deserializeEnum("EnumName", listOf("variant"), visitor),
            input.deserializeIdentifier(visitor),
            input.deserializeIgnoredAny(visitor),
        )

    @Test
    fun concretePayloadsForwardEveryRustMacroOperationExactlyOnce() {
        for ((input, expected) in payloads()) {
            val observations = mutableListOf<String>()
            val visitor =
                ProbeVisitor {
                    observations.add(it)
                    SerdeResult.success(it)
                }
            val results = forwarded(input, visitor)
            assertEquals(31, results.size)
            assertEquals(List(31) { expected }, observations)
            assertEquals(List(31) { expected }, results.map { it.getOrThrow() })
            assertSame(input, (input as IntoDeserializer).intoDeserializer())
        }
    }

    @Test
    fun visitorFailureAndExceptionIdentitiesArePreserved() {
        val failure = SerdeResult.failure(SerdeError.custom("visitor failure"))
        val panic = IllegalStateException("visitor panic")
        for ((input, _) in payloads()) {
            for (result in forwarded(input, ProbeVisitor { failure })) assertSame(failure, result)
            assertSame(
                panic,
                assertFailsWith<IllegalStateException> {
                    input.deserializeOption(ProbeVisitor { throw panic })
                },
            )
        }
    }

    @Test
    fun scalarAndBorrowedRegionAdaptersRetainTheirSpecializedVisits() {
        val codePoint = 0x1f680
        val scalarVisitor =
            object : Visitor<Int> {
                override fun expecting(): String = "a Unicode scalar"
                override fun visitCharCodePoint(codePoint: Int): SerdeResult<Int> = SerdeResult.success(codePoint)
            }
        assertEquals(codePoint, CharCodePointDeserializer.new(codePoint).deserializeString(scalarVisitor).getOrThrow())
        assertFailsWith<IllegalArgumentException> { CharCodePointDeserializer.new(0xd800) }
        val region = SharedByteSlice.borrow(byteArrayOf(9, 1, 2, 8), 1, 3)
        val bytesVisitor =
            object : Visitor<SharedByteSlice> {
                override fun expecting(): String = "the same borrowed byte region"
                override fun visitBorrowedByteSlice(v: SharedByteSlice): SerdeResult<SharedByteSlice> = SerdeResult.success(v)
            }
        val input = BorrowedByteSliceDeserializer.new(region)
        assertSame(region, input.deserializeByteBuf(bytesVisitor).getOrThrow())
        assertSame(input, input.intoDeserializer())
    }

    @Test
    fun sequenceAccessConversionPreservesIdentityAndIteratorProgress() {
        val source = seqDeserializer(listOf(I32Deserializer.new(7), I32Deserializer.new(8)).iterator())
        val adapter = SeqAccessDeserializer.new(source)
        val conversion: IntoDeserializer = adapter
        val converted = conversion.intoDeserializer()
        assertSame(adapter, converted)
        val visitor =
            object : Visitor<SeqElement<Int>> {
                override fun expecting(): String = "the original sequence access"
                override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<SeqElement<Int>> {
                    assertSame<SeqAccess>(source, access)
                    return access.nextElement(I32Deserialize)
                }
            }
        assertEquals(SeqElement.Some(7), converted.deserializeAny(visitor).getOrThrow())
        assertEquals(SeqElement.Some(8), source.nextElement(I32Deserialize).getOrThrow())
        assertEquals(SeqElement.None, adapter.intoDeserializer().deserializeSeq(visitor).getOrThrow())
    }

    @Test
    fun sequenceAccessConversionPreservesErrorsWithoutConsumingAnotherItem() {
        val source = seqDeserializer(listOf(I32Deserializer.new(7), I32Deserializer.new(8)).iterator())
        val failure = SerdeResult.failure(SerdeError.custom("seed failure"))
        val seed =
            object : DeserializeSeed<Int> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Int> = failure
            }
        val visitor =
            object : Visitor<SeqElement<Int>> {
                override fun expecting(): String = "a sequence"
                override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<SeqElement<Int>> = access.nextElementSeed(seed)
            }
        val converted: IntoDeserializer = SeqAccessDeserializer.new(source)
        assertSame(failure, converted.intoDeserializer().deserializeAny(visitor))
        assertEquals(SeqElement.Some(8), source.nextElement(I32Deserialize).getOrThrow())
    }
}
