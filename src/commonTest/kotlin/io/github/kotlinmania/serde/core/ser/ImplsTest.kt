// port-lint: source serde_core/src/ser/impls.rs
package io.github.kotlinmania.serde.core.ser

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

public class ImplsTest {
    @Test
    public fun testFormatU8() {
        var i: UByte = 0u

        while (true) {
            val buf = ByteArray(3)
            val written = formatU8(i, buf)
            assertContentEquals(i.toString().encodeToByteArray(), buf.copyOfRange(0, written))

            if (i == UByte.MAX_VALUE) {
                break
            }
            i = (i + 1u).toUByte()
        }
    }

    @Test
    public fun durationSerializesAsStruct() {
        val serialized = (2.seconds + 345.nanoseconds).serialize(StructRecordingSerializer()).getOrThrow()

        assertEquals("Duration(secs=2,nanos=345)", serialized)
    }

    @Test
    public fun instantSerializesAsSystemTimeStruct() {
        val serialized = Instant.fromEpochSeconds(4, 5).serialize(StructRecordingSerializer()).getOrThrow()

        assertEquals("SystemTime(secs_since_epoch=4,nanos_since_epoch=5)", serialized)
    }

    @Test
    public fun instantBeforeEpochReturnsError() {
        assertTrue(Instant.fromEpochSeconds(-1, 0).serialize(StructRecordingSerializer()).isFailure)
    }

    @Test
    public fun phantomDataSerializesAsUnitStruct() {
        assertEquals("PhantomData", formatSerialize(PhantomData))
    }

    @Test
    public fun wrappersSerializeTheirValues() {
        assertEquals("wrapped", formatSerialize(Wrapping(ImplsLiteralSerialize("wrapped"))))
        assertEquals("saturating", formatSerialize(Saturating(ImplsLiteralSerialize("saturating"))))
        assertEquals("reverse", formatSerialize(Reverse(ImplsLiteralSerialize("reverse"))))
    }
}

private data object TestError : Error

private open class FailingSerializer : Serializer<String, TestError> {
    protected fun <T> unexpected(): Result<T> =
        Result.failure(AssertionError("unexpected serializer method"))

    override fun serializeBool(v: Boolean): Result<String> = unexpected()
    override fun serializeI8(v: Byte): Result<String> = unexpected()
    override fun serializeI16(v: Short): Result<String> = unexpected()
    override fun serializeI32(v: Int): Result<String> = unexpected()
    override fun serializeI64(v: Long): Result<String> = unexpected()
    override fun serializeI128(v: String): Result<String> = unexpected()
    override fun serializeU8(v: UByte): Result<String> = unexpected()
    override fun serializeU16(v: UShort): Result<String> = unexpected()
    override fun serializeU32(v: UInt): Result<String> = unexpected()
    override fun serializeU64(v: ULong): Result<String> = unexpected()
    override fun serializeF32(v: Float): Result<String> = unexpected()
    override fun serializeF64(v: Double): Result<String> = unexpected()
    override fun serializeChar(v: Char): Result<String> = unexpected()
    override fun serializeStr(v: String): Result<String> = unexpected()
    override fun serializeBytes(v: ByteArray): Result<String> = unexpected()
    override fun serializeNone(): Result<String> = unexpected()

    override fun <T> serializeSome(value: T): Result<String>
        where T : Serialize =
        unexpected()

    override fun serializeUnit(): Result<String> = unexpected()
    override fun serializeUnitStruct(name: String): Result<String> = unexpected()
    override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): Result<String> = unexpected()

    override fun <T> serializeNewtypeStruct(name: String, value: T): Result<String>
        where T : Serialize =
        unexpected()

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): Result<String>
        where T : Serialize =
        unexpected()

    override fun serializeSeq(len: Int?): Result<SerializeSeq<String, TestError>> = unexpected()
    override fun serializeTuple(len: Int): Result<SerializeTuple<String, TestError>> = unexpected()
    override fun serializeTupleStruct(name: String, len: Int): Result<SerializeTupleStruct<String, TestError>> = unexpected()

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeTupleVariant<String, TestError>> = unexpected()

    override fun serializeMap(len: Int?): Result<SerializeMap<String, TestError>> = unexpected()
    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<String, TestError>> = unexpected()

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): Result<SerializeStructVariant<String, TestError>> = unexpected()
}

private class StructRecordingSerializer : FailingSerializer() {
    override fun serializeStruct(name: String, len: Int): Result<SerializeStruct<String, TestError>> =
        Result.success(RecordingStruct(name, len))
}

private class FieldRecordingSerializer : FailingSerializer() {
    override fun serializeU32(v: UInt): Result<String> = Result.success(v.toString())
    override fun serializeU64(v: ULong): Result<String> = Result.success(v.toString())
}

private class RecordingStruct(
    private val name: String,
    private val len: Int,
) : SerializeStruct<String, TestError> {
    private val fields = mutableListOf<Pair<String, String>>()

    override fun <T> serializeField(key: String, value: T): Result<Unit>
        where T : Serialize =
        runCatching {
            if (fields.size == len) {
                throw AssertionError("too many fields")
            }
            fields += key to value.serialize(FieldRecordingSerializer()).getOrThrow()
        }

    override fun end(): Result<String> =
        Result.success(fields.joinToString(separator = ",", prefix = "$name(", postfix = ")") { (key, value) -> "$key=$value" })
}

private fun formatSerialize(value: Serialize): String {
    val builder = StringBuilder()
    value.serialize(FormatterSerializer(builder)).getOrThrow()
    return builder.toString()
}

private data class ImplsLiteralSerialize(
    private val text: String,
) : Serialize {
    override fun <Ok, E> serialize(serializer: Serializer<Ok, E>): Result<Ok>
        where E : Error =
        serializer.serializeStr(text)
}
