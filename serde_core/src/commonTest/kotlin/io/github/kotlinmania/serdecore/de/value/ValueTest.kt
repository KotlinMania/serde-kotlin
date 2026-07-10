// port-lint: tests test_suite/tests/test_value.rs
package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.I128Deserialize
import io.github.kotlinmania.serdecore.de.U128Deserialize
import io.github.kotlinmania.serdecore.de.U32Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals

public class ValueTest {
    @Test
    public fun testU32ToEnum() {
        val e = U32Deserializer.new(1u).deserializeEnum("E", listOf("A", "B"), EnumVisitor).getOrThrow()

        assertEquals(EnumValue.B, e)
    }

    @Test
    public fun testInteger128() {
        val deU128 = "1".intoU128Deserializer()
        val deI128 = "1".intoI128Deserializer()

        assertEquals("1", U128Deserialize.deserialize(deU128).getOrThrow())
        assertEquals("1", I128Deserialize.deserialize(deU128).getOrThrow())
        assertEquals("1", U128Deserialize.deserialize(deI128).getOrThrow())
        assertEquals("1", I128Deserialize.deserialize(deI128).getOrThrow())
    }

    @Test
    public fun errorCustomCarriesDisplayAndDescriptionText() {
        val error = ValueError.custom("expected unit")

        assertEquals("expected unit", error.fmt())
        assertEquals("expected unit", error.description())
        assertEquals("expected unit", error.toString())
        assertEquals(ValueError.custom("expected unit"), error)
        assertEquals(ValueError.custom("expected unit").hashCode(), error.hashCode())
    }

    @Test
    public fun strDeserializerVisitsBorrowedStringPath() {
        assertEquals("str:value", StrDeserializer.new("value").deserializeAny(StringKindVisitor).getOrThrow())
        assertEquals("str:value", StrDeserializer.new("value").deserializeString(StringKindVisitor).getOrThrow())
    }

    @Test
    public fun stringDeserializerVisitsOwnedStringPath() {
        assertEquals("string:value", StringDeserializer.new("value").deserializeAny(StringKindVisitor).getOrThrow())
    }

    @Test
    public fun strDeserializerActsAsUnitEnumAccess() {
        val (variant, access) = StrDeserializer.new("Variant").variantSeed(StringSeed).getOrThrow()

        assertEquals("Variant", variant)
        assertEquals(Unit, access.unitVariant().getOrThrow())
    }

    @Test
    public fun mapDeserializerNextEntrySeedConsumesKeyAndValueTogether() {
        val deserializer =
            mapDeserializer(
                listOf(
                    MapEntry("left".intoDeserializer(), "1".intoDeserializer()),
                    MapEntry("right".intoDeserializer(), "2".intoDeserializer()),
                ).iterator(),
            )

        assertEquals("left" to "1", deserializer.nextEntrySeed(StringSeed, StringSeed).getOrThrow())
        assertEquals("right" to "2", deserializer.nextEntrySeed(StringSeed, StringSeed).getOrThrow())
        assertEquals(null, deserializer.nextEntrySeed(StringSeed, StringSeed).getOrThrow())
        assertEquals(Unit, deserializer.end().getOrThrow())
    }

    @Test
    public fun mapDeserializerNextKeyStoresValueForNextValue() {
        val deserializer =
            mapDeserializer(
                listOf(
                    MapEntry("key".intoDeserializer(), "value".intoDeserializer()),
                ).iterator(),
            )

        assertEquals("key", deserializer.nextKeySeed(StringSeed).getOrThrow())
        assertEquals("value", deserializer.nextValueSeed(StringSeed).getOrThrow())
        assertEquals(null, deserializer.nextKeySeed(StringSeed).getOrThrow())
    }

}

private data object StringKindVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success("str:$v")

    override fun visitString(v: String): SerdeResult<String> = SerdeResult.success("string:$v")
}

private data object StringSeed : DeserializeSeed<String> {
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeStr(StringOnlyVisitor)
}

private data object StringOnlyVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success(v)
}

private enum class EnumValue {
    A,
    B,
}

private data object EnumVisitor : Visitor<EnumValue> {
    override fun expecting(): String = "enum E"

    override fun <A> visitEnum(access: A): SerdeResult<EnumValue>
        where A : EnumAccess =
        access.variantSeed(EnumSeed).map { (variant, variantAccess) ->
            variantAccess.unitVariant().getOrThrow()
            variant
        }
}

private data object EnumSeed : DeserializeSeed<EnumValue> {
    override fun <D> deserialize(deserializer: D): SerdeResult<EnumValue>
        where D : Deserializer =
        U32Deserialize.deserialize(deserializer).map { index ->
            when (index) {
                0u -> EnumValue.A
                1u -> EnumValue.B
                else -> error("unknown enum index $index")
            }
        }
}
