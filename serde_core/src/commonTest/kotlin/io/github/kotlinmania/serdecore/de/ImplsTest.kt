// port-lint: tests test_suite/tests/test_de.rs
package io.github.kotlinmania.serdecore.de

import io.github.kotlinmania.serde.SerdeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

public class ImplsTest {
    @Test
    public fun optionDeserializesUnitNoneAndSome() {
        val deserialize = nullableDeserialize(I32Deserialize)

        assertNull(deserialize.deserialize(OptionDeserializer(OptionMode.UnitValue)).getOrThrow())
        assertNull(deserialize.deserialize(OptionDeserializer(OptionMode.NoneValue)).getOrThrow())
        assertEquals(1, deserialize.deserialize(OptionDeserializer(OptionMode.SomeValue(1))).getOrThrow())
    }

    @Test
    public fun untaggedOptionMapsFailedValueToNone() {
        val deserialize = nullableDeserialize(I32Deserialize)

        assertEquals(
            7,
            deserialize.deserialize(OptionDeserializer(OptionMode.UntaggedValue(IntDeserializer(7)))).getOrThrow(),
        )
        assertNull(
            deserialize
                .deserialize(OptionDeserializer(OptionMode.UntaggedValue(StringDeserializer("not an int"))))
                .getOrThrow(),
        )
    }
}

private sealed class OptionMode {
    data object UnitValue : OptionMode()
    data object NoneValue : OptionMode()

    data class SomeValue(
        val value: Int,
    ) : OptionMode()

    data class UntaggedValue(
        val deserializer: Deserializer,
    ) : OptionMode()
}

private class OptionDeserializer(
    private val mode: OptionMode,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeOption(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        when (val current = mode) {
            OptionMode.UnitValue -> visitor.visitUnit()
            OptionMode.NoneValue -> visitor.visitNone()
            is OptionMode.SomeValue -> visitor.visitSome(IntDeserializer(current.value))
            is OptionMode.UntaggedValue -> visitor.privateVisitUntaggedOption(current.deserializer)
        }
}

private class IntDeserializer(
    private val value: Int,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeI32(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = visitor.visitI32(value)
}

private class StringDeserializer(
    private val value: String,
) : ForwardingDeserializer() {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeString(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = visitor.visitString(value)
}

private abstract class ForwardingDeserializer : Deserializer {
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
