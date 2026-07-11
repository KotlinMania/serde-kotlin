// port-lint: tests test_suite/tests/test_value.rs
package io.github.kotlinmania.serdecore.de.value

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.Deserialize
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

    @Test
    public fun seqDeserializerSizeHintTracksRemainingCollectionElements() {
        val deserializer = listOf("first".intoDeserializer(), "second".intoDeserializer()).intoDeserializer()

        assertEquals(2, deserializer.sizeHint())
        assertEquals("first", deserializer.nextElementSeed(StringSeed).getOrThrow())
        assertEquals(1, deserializer.sizeHint())
        assertEquals("second", deserializer.nextElementSeed(StringSeed).getOrThrow())
        assertEquals(0, deserializer.sizeHint())
    }

    @Test
    public fun mapDeserializerSizeHintTracksRemainingMapEntries() {
        val deserializer = mapOf("key".intoDeserializer() to "value".intoDeserializer()).intoDeserializer()

        assertEquals(1, deserializer.sizeHint())
        assertEquals("key", deserializer.nextKeySeed(StringSeed).getOrThrow())
        assertEquals(0, deserializer.sizeHint())
        assertEquals("value", deserializer.nextValueSeed(StringSeed).getOrThrow())
        assertEquals(0, deserializer.sizeHint())
    }

    @Test
    public fun testMapAccessToEnum() {
        val deserializer =
            mapDeserializer(
                listOf(
                    MapEntry(
                        "Airebo".intoDeserializer(),
                        mapDeserializer(
                            listOf(
                                MapEntry("lj_sigma".intoDeserializer(), 14.0.intoDeserializer()),
                            ).iterator(),
                        ),
                    ),
                ).iterator(),
            )

        val actual = PotentialDeserialize.deserialize(deserializer).getOrThrow()

        assertEquals(Potential(PotentialKind.Airebo(Airebo(14.0))), actual)
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

private data class Potential(
    val kind: PotentialKind,
)

private sealed interface PotentialKind {
    data class Airebo(
        val value: io.github.kotlinmania.serdecore.de.value.Airebo,
    ) : PotentialKind
}

private data class Airebo(
    val ljSigma: Double,
)

private data object PotentialDeserialize : Deserialize<Potential> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Potential>
        where D : Deserializer =
        deserializer.deserializeAny(PotentialVisitor)
}

private data object PotentialVisitor : Visitor<Potential> {
    override fun expecting(): String = "a map"

    override fun <A> visitMap(access: A): SerdeResult<Potential>
        where A : MapAccess =
        PotentialKindDeserialize
            .deserialize(MapAccessDeserializer.new(access))
            .map(::Potential)
}

private data object PotentialKindDeserialize : Deserialize<PotentialKind> {
    override fun <D> deserialize(deserializer: D): SerdeResult<PotentialKind>
        where D : Deserializer =
        deserializer.deserializeEnum("PotentialKind", listOf("Airebo"), PotentialKindVisitor)
}

private data object PotentialKindVisitor : Visitor<PotentialKind> {
    override fun expecting(): String = "enum PotentialKind"

    override fun <A> visitEnum(access: A): SerdeResult<PotentialKind>
        where A : EnumAccess =
        access.variantSeed(StringSeed).map { (variant, variantAccess) ->
            when (variant) {
                "Airebo" -> PotentialKind.Airebo(variantAccess.newtypeVariantSeed(AireboDeserialize).getOrThrow())
                else -> throw IllegalArgumentException("unknown variant $variant")
            }
        }
}

private data object AireboDeserialize : Deserialize<Airebo>, DeserializeSeed<Airebo> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Airebo>
        where D : Deserializer =
        deserializer.deserializeMap(AireboVisitor)
}

private data object AireboVisitor : Visitor<Airebo> {
    override fun expecting(): String = "struct Airebo"

    override fun <A> visitMap(access: A): SerdeResult<Airebo>
        where A : MapAccess =
        serdeCatching {
            var ljSigma: Double? = null
            while (true) {
                val key = access.nextKeySeed(StringSeed).getOrThrow() ?: break
                when (key) {
                    "lj_sigma" -> ljSigma = access.nextValueSeed(DoubleSeed).getOrThrow()
                    else -> access.nextValueSeed(IgnoredValueSeed).getOrThrow()
                }
            }
            Airebo(requireNotNull(ljSigma) { "missing lj_sigma" })
        }
}

private data object DoubleSeed : DeserializeSeed<Double> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Double>
        where D : Deserializer =
        deserializer.deserializeF64(DoubleVisitor)
}

private data object DoubleVisitor : Visitor<Double> {
    override fun expecting(): String = "a double"

    override fun visitF64(v: Double): SerdeResult<Double> = SerdeResult.success(v)
}

private data object IgnoredValueSeed : DeserializeSeed<Unit> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Unit>
        where D : Deserializer =
        deserializer.deserializeIgnoredAny(IgnoredValueVisitor)
}

private data object IgnoredValueVisitor : Visitor<Unit> {
    override fun expecting(): String = "ignored value"

    override fun visitUnit(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun visitBool(v: Boolean): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun visitF64(v: Double): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun visitStr(v: String): SerdeResult<Unit> = SerdeResult.success(Unit)
}
