package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals

public class ValueTest {
    @Test
    public fun errorCustomCarriesDisplayAndDescriptionText() {
        val error = Error.custom("expected unit")

        assertEquals("expected unit", error.fmt())
        assertEquals("expected unit", error.description())
        assertEquals("expected unit", error.toString())
        assertEquals(Error.custom("expected unit"), error)
        assertEquals(Error.custom("expected unit").hashCode(), error.hashCode())
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
