// port-lint: source ser/mod.rs
package io.github.kotlinmania.serdecore.ser

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import kotlin.test.Test
import kotlin.test.assertEquals

public class SerializerTest {
    @Test
    public fun collectSeqSerializesLazySequenceItems() {
        val result =
            SequenceRecordingSerializer()
                .collectSeq(sequenceOf(SerializerLiteralSerialize("a"), SerializerLiteralSerialize("b")))
                .getOrThrow()

        assertEquals("Seq(null:a,b)", result)
    }

    @Test
    public fun collectMapSerializesLazySequencePairs() {
        val result =
            SequenceRecordingSerializer()
                .collectMap(
                    sequenceOf(
                        SerializerLiteralSerialize("left") to SerializerLiteralSerialize("1"),
                        SerializerLiteralSerialize("right") to SerializerLiteralSerialize("2"),
                    ),
                ).getOrThrow()

        assertEquals("Map(null:left=1,right=2)", result)
    }

    @Test
    public fun serializeEntryDefaultsToKeyThenValue() {
        val map = RecordingMap(null)

        map.serializeEntry(SerializerLiteralSerialize("key"), SerializerLiteralSerialize("value")).getOrThrow()

        assertEquals("Map(null:key=value)", map.end().getOrThrow())
    }
}

private class SequenceRecordingSerializer : FailingSerializer() {
    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<String>> = SerdeResult.success(RecordingSeq(len))

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<String>> = SerdeResult.success(RecordingMap(len))
}

private class RecordingSeq(
    private val len: Int?,
) : SerializeSeq<String> {
    private val elements = mutableListOf<String>()

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            elements += value.serialize(FieldRecordingSerializer()).getOrThrow()
        }

    override fun end(): SerdeResult<String> =
        SerdeResult.success(elements.joinToString(separator = ",", prefix = "Seq($len:", postfix = ")"))
}

private class RecordingMap(
    private val len: Int?,
) : SerializeMap<String> {
    private val entries = mutableListOf<Pair<String, String>>()
    private var pendingKey: String? = null

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            pendingKey = key.serialize(FieldRecordingSerializer()).getOrThrow()
        }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            val key = pendingKey ?: throw AssertionError("missing key")
            entries += key to value.serialize(FieldRecordingSerializer()).getOrThrow()
            pendingKey = null
        }

    override fun end(): SerdeResult<String> =
        SerdeResult.success(
            entries.joinToString(separator = ",", prefix = "Map($len:", postfix = ")") { (key, value) ->
                "$key=$value"
            },
        )
}

private data class SerializerLiteralSerialize(
    private val text: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(text)
}
