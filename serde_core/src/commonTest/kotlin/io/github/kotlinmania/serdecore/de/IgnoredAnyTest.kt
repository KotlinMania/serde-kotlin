// port-lint: tests de/ignored_any.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import kotlin.test.Test
import kotlin.test.assertEquals

public class IgnoredAnyTest {
    @Test
    public fun visitSeqConsumesEveryElementThroughDeserialize() {
        val access = RecordingSeqAccess(
            mutableListOf(
                CountingDeserializer("first"),
                CountingDeserializer("second"),
                CountingDeserializer("third"),
            ),
        )

        assertEquals(IgnoredAny, IgnoredAny.visitSeq(access).getOrThrow())
        assertEquals(listOf("first", "second", "third"), access.consumedLabels)
    }

    @Test
    public fun seqAccessConvenienceWrapsDeserializeAsSeed() {
        val access = RecordingSeqAccess(mutableListOf(CountingDeserializer("value")))

        assertEquals("value", access.nextElement(LabelDeserialize).getOrThrow())
        assertEquals(null, access.nextElement(LabelDeserialize).getOrThrow())
    }

    @Test
    public fun visitMapConsumesEveryKeyAndValueThroughDeserialize() {
        val access = RecordingMapAccess(
            mutableListOf(
                CountingDeserializer("key-a") to CountingDeserializer("value-a"),
                CountingDeserializer("key-b") to CountingDeserializer("value-b"),
            ),
        )

        assertEquals(IgnoredAny, IgnoredAny.visitMap(access).getOrThrow())
        assertEquals(
            listOf("key-a", "value-a", "key-b", "value-b"),
            access.consumedLabels,
        )
    }

    @Test
    public fun mapAccessConvenienceWrapsDeserializeAsSeeds() {
        val access = RecordingMapAccess(
            mutableListOf(
                CountingDeserializer("key") to CountingDeserializer("value"),
            ),
        )

        assertEquals("key" to "value", access.nextEntry(LabelDeserialize, LabelDeserialize).getOrThrow())
        assertEquals(null, access.nextEntry(LabelDeserialize, LabelDeserialize).getOrThrow())
    }
}

private class RecordingSeqAccess(
    private val elements: MutableList<CountingDeserializer>,
) : SeqAccess {
    val consumedLabels: MutableList<String> = mutableListOf()

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?> {
        val next = elements.removeFirstOrNull() ?: return SerdeResult.success(null)
        consumedLabels += next.label
        return seed.deserialize(next).map { it }
    }
}

private class RecordingMapAccess(
    private val entries: MutableList<Pair<CountingDeserializer, CountingDeserializer>>,
) : MapAccess {
    val consumedLabels: MutableList<String> = mutableListOf()
    private var pendingValue: CountingDeserializer? = null

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> {
        val (key, value) = entries.removeFirstOrNull() ?: return SerdeResult.success(null)
        pendingValue = value
        consumedLabels += key.label
        return seed.deserialize(key).map { it }
    }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
        val value = pendingValue
            ?: return SerdeResult.failure(SerdeError.custom("value requested before key"))
        pendingValue = null
        consumedLabels += value.label
        return seed.deserialize(value)
    }
}

private class CountingDeserializer(
    val label: String,
) : Deserializer {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        visitor.visitStr(label)

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
    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSome(this)
    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = visitor.visitNewtypeStruct(this)
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

private data object LabelDeserialize : Deserialize<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeStr(LabelVisitor)
}

private data object LabelVisitor : Visitor<String> {
    override fun expecting(): String = "a label"

    override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success(v)
}
