// port-lint: tests ser/impls.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
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
    public fun emptyArraySerializesAsZeroTupleWithoutElementSerializer() {
        assertEquals("Tuple0()", serializeEmptyArray(TupleRecordingSerializer()).getOrThrow())
    }

    @Test
    public fun wrappersSerializeTheirValues() {
        assertEquals("wrapped", formatSerialize(Wrapping(ImplsLiteralSerialize("wrapped"))))
        assertEquals("saturating", formatSerialize(Saturating(ImplsLiteralSerialize("saturating"))))
        assertEquals("reverse", formatSerialize(Reverse(ImplsLiteralSerialize("reverse"))))
    }

    @Test
    public fun testFmtArguments() {
        assertEquals("1a", formatSerialize(FormatArguments("${1}${'a'}")))
    }

    @Test
    public fun arraysSerializeAsTuples() {
        val serialized =
            arrayOf(
                ImplsLiteralSerialize("a"),
                ImplsLiteralSerialize("b"),
            ).serialize(TupleRecordingSerializer()).getOrThrow()

        assertEquals("Tuple2(a,b)", serialized)
    }
}

private data object TestError : Error()

internal open class FailingSerializer : Serializer<String> {
    protected fun <T> unexpected(): SerdeResult<T> = SerdeResult.failure(SerdeError("unexpected serializer method"))

    override fun serializeBool(v: Boolean): SerdeResult<String> = unexpected()

    override fun serializeI8(v: Byte): SerdeResult<String> = unexpected()

    override fun serializeI16(v: Short): SerdeResult<String> = unexpected()

    override fun serializeI32(v: Int): SerdeResult<String> = unexpected()

    override fun serializeI64(v: Long): SerdeResult<String> = unexpected()

    override fun serializeI128(value: String): SerdeResult<String> = unexpected()

    override fun serializeU8(v: UByte): SerdeResult<String> = unexpected()

    override fun serializeU16(v: UShort): SerdeResult<String> = unexpected()

    override fun serializeU32(v: UInt): SerdeResult<String> = unexpected()

    override fun serializeU64(v: ULong): SerdeResult<String> = unexpected()

    override fun serializeF32(v: Float): SerdeResult<String> = unexpected()

    override fun serializeF64(v: Double): SerdeResult<String> = unexpected()

    override fun serializeChar(v: Char): SerdeResult<String> = unexpected()

    override fun serializeStr(v: String): SerdeResult<String> = unexpected()

    override fun serializeBytes(v: ByteArray): SerdeResult<String> = unexpected()

    override fun serializeNone(): SerdeResult<String> = unexpected()

    override fun <T> serializeSome(value: T): SerdeResult<String>
        where T : Serialize = unexpected()

    override fun serializeUnit(): SerdeResult<String> = unexpected()

    override fun serializeUnitStruct(name: String): SerdeResult<String> = unexpected()

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<String> = unexpected()

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<String>
        where T : Serialize = unexpected()

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<String>
        where T : Serialize = unexpected()

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<String>> = unexpected()

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<String>> = unexpected()

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<String>> = unexpected()

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<String>> = unexpected()

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<String>> = unexpected()

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<String>> = unexpected()

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<String>> = unexpected()
}

private class StructRecordingSerializer : FailingSerializer() {
    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<String>> = SerdeResult.success(RecordingStruct(name, len))
}

internal class FieldRecordingSerializer : FailingSerializer() {
    override fun serializeStr(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun serializeU32(v: UInt): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeU64(v: ULong): SerdeResult<String> = SerdeResult.success(v.toString())
}

private class TupleRecordingSerializer : FailingSerializer() {
    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<String>> = SerdeResult.success(RecordingTuple(len))
}

private class RecordingTuple(
    private val len: Int,
) : SerializeTuple<String> {
    private val elements = mutableListOf<String>()

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            if (elements.size == len) {
                throw AssertionError("too many elements")
            }
            elements += value.serialize(FieldRecordingSerializer()).getOrThrow()
        }

    override fun end(): SerdeResult<String> =
        SerdeResult.success(elements.joinToString(separator = ",", prefix = "Tuple$len(", postfix = ")"))
}

private class RecordingStruct(
    private val name: String,
    private val len: Int,
) : SerializeStruct<String> {
    private val fields = mutableListOf<Pair<String, String>>()

    override fun <T> serializeField(
        key: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            if (fields.size == len) {
                throw AssertionError("too many fields")
            }
            fields += key to value.serialize(FieldRecordingSerializer()).getOrThrow()
        }

    override fun end(): SerdeResult<String> =
        SerdeResult.success(fields.joinToString(separator = ",", prefix = "$name(", postfix = ")") { (key, value) -> "$key=$value" })
}

private fun formatSerialize(value: Serialize): String {
    val builder = StringBuilder()
    value.serialize(FormatterSerializer(builder)).getOrThrow()
    return builder.toString()
}

private data class ImplsLiteralSerialize(
    private val text: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok>
        = serializer.serializeStr(text)
}
