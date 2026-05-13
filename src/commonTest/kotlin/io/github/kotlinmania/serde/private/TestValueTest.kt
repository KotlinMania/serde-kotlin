// port-lint: source test_suite/tests/test_value.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.core.de.Deserialize
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.EnumAccess
import io.github.kotlinmania.serde.core.de.I128Deserialize
import io.github.kotlinmania.serde.core.de.MapAccess
import io.github.kotlinmania.serde.core.de.U128Deserialize
import io.github.kotlinmania.serde.core.de.VariantAccess
import io.github.kotlinmania.serde.core.de.Visitor
import io.github.kotlinmania.serde.core.de.value.F64Deserializer
import io.github.kotlinmania.serde.core.de.value.I128Deserializer
import io.github.kotlinmania.serde.core.de.value.MapAccessDeserializer
import io.github.kotlinmania.serde.core.de.value.MapDeserializer
import io.github.kotlinmania.serde.core.de.value.StrDeserializer
import io.github.kotlinmania.serde.core.de.value.U128Deserializer
import io.github.kotlinmania.serde.core.de.value.intoDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals

private enum class E {
    A,
    B,
    ;

    public companion object : Deserialize<E> {
        override fun <D> deserialize(deserializer: D): Result<E>
            where D : Deserializer =
            deserializer.deserializeEnum("E", listOf("A", "B"), EVisitor)
    }
}

private data object EVisitor : Visitor<E> {
    override fun expecting(): String = "enum E"

    override fun <A> visitEnum(data: A): Result<E>
        where A : EnumAccess =
        runCatching {
            val (variant, access) = data.variantSeed(EVariantSeed).getOrThrow()
            access.unitVariant().getOrThrow()
            variant
        }
}

private data object EVariantSeed : DeserializeSeed<E> {
    override fun <D> deserialize(deserializer: D): Result<E>
        where D : Deserializer =
        deserializer.deserializeIdentifier(EVariantVisitor)
}

private data object EVariantVisitor : Visitor<E> {
    override fun expecting(): String = "variant identifier"

    override fun visitU32(v: UInt): Result<E> =
        when (v) {
            0u -> Result.success(E.A)
            1u -> Result.success(E.B)
            else -> Result.failure(IllegalStateException("variant index $v out of range for E"))
        }

    override fun visitU64(v: ULong): Result<E> = visitU32(v.toUInt())

    override fun visitStr(v: String): Result<E> =
        when (v) {
            "A" -> Result.success(E.A)
            "B" -> Result.success(E.B)
            else -> Result.failure(IllegalStateException("unknown variant name $v"))
        }
}

private data class Airebo(val ljSigma: Double) {
    public companion object : Deserialize<Airebo> {
        override fun <D> deserialize(deserializer: D): Result<Airebo>
            where D : Deserializer =
            deserializer.deserializeStruct("Airebo", listOf("lj_sigma"), AireboVisitor)
    }
}

private data object AireboVisitor : Visitor<Airebo> {
    override fun expecting(): String = "struct Airebo"

    override fun <A> visitMap(map: A): Result<Airebo>
        where A : MapAccess =
        runCatching {
            var ljSigma: Double? = null
            while (true) {
                val key = map.nextKeySeed(AireboFieldSeed).getOrThrow() ?: break
                when (key) {
                    AireboField.LjSigma -> ljSigma = map.nextValueSeed(F64Seed).getOrThrow()
                }
            }
            Airebo(ljSigma = ljSigma ?: throw AssertionError("missing field lj_sigma"))
        }
}

private enum class AireboField { LjSigma }

private data object AireboFieldSeed : DeserializeSeed<AireboField> {
    override fun <D> deserialize(deserializer: D): Result<AireboField>
        where D : Deserializer =
        deserializer.deserializeIdentifier(AireboFieldVisitor)
}

private data object AireboFieldVisitor : Visitor<AireboField> {
    override fun expecting(): String = "field identifier"

    override fun visitStr(v: String): Result<AireboField> =
        when (v) {
            "lj_sigma" -> Result.success(AireboField.LjSigma)
            else -> Result.failure(IllegalStateException("unknown field $v"))
        }
}

private data object F64Seed : DeserializeSeed<Double> {
    override fun <D> deserialize(deserializer: D): Result<Double>
        where D : Deserializer =
        deserializer.deserializeF64(F64Capture)
}

private data object F64Capture : Visitor<Double> {
    override fun expecting(): String = "f64"

    override fun visitF64(v: Double): Result<Double> = Result.success(v)
}

private sealed class PotentialKind {
    public data class Airebo(public val value: io.github.kotlinmania.serde.`private`.Airebo) : PotentialKind()

    public companion object : Deserialize<PotentialKind> {
        override fun <D> deserialize(deserializer: D): Result<PotentialKind>
            where D : Deserializer =
            deserializer.deserializeEnum("PotentialKind", listOf("Airebo"), PotentialKindVisitor)
    }
}

private data object PotentialKindVisitor : Visitor<PotentialKind> {
    override fun expecting(): String = "enum PotentialKind"

    override fun <A> visitEnum(data: A): Result<PotentialKind>
        where A : EnumAccess =
        runCatching {
            val (variant, access) = data.variantSeed(PotentialKindVariantSeed).getOrThrow()
            when (variant) {
                PotentialKindVariant.Airebo ->
                    PotentialKind.Airebo(access.newtypeVariantSeed(AireboDeserializeSeed).getOrThrow())
            }
        }
}

private enum class PotentialKindVariant { Airebo }

private data object PotentialKindVariantSeed : DeserializeSeed<PotentialKindVariant> {
    override fun <D> deserialize(deserializer: D): Result<PotentialKindVariant>
        where D : Deserializer =
        deserializer.deserializeIdentifier(PotentialKindVariantVisitor)
}

private data object PotentialKindVariantVisitor : Visitor<PotentialKindVariant> {
    override fun expecting(): String = "variant identifier"

    override fun visitStr(v: String): Result<PotentialKindVariant> =
        when (v) {
            "Airebo" -> Result.success(PotentialKindVariant.Airebo)
            else -> Result.failure(IllegalStateException("unknown variant $v"))
        }
}

private data object AireboDeserializeSeed : DeserializeSeed<Airebo> {
    override fun <D> deserialize(deserializer: D): Result<Airebo>
        where D : Deserializer =
        Airebo.deserialize(deserializer)
}

private data class Potential(val kind: PotentialKind) {
    public companion object : Deserialize<Potential> {
        override fun <D> deserialize(deserializer: D): Result<Potential>
            where D : Deserializer =
            deserializer.deserializeAny(PotentialVisitor)
    }
}

private data object PotentialVisitor : Visitor<Potential> {
    override fun expecting(): String = "a map"

    override fun <A> visitMap(map: A): Result<Potential>
        where A : MapAccess =
        PotentialKind.deserialize(MapAccessDeserializer.new(map)).map { Potential(it) }
}

public class TestValueTest {
    @Test
    public fun testU32ToEnum() {
        val deserializer = 1u.intoDeserializer()
        val e = E.deserialize(deserializer).getOrThrow()
        assertEquals(E.B, e)
    }

    @Test
    public fun testInteger128() {
        val deU128 = U128Deserializer.new("1")
        val deI128 = I128Deserializer.new("1")

        // u128 to u128
        assertEquals("1", U128Deserialize.deserialize(deU128).getOrThrow())

        // u128 to i128
        assertEquals("1", I128Deserialize.deserialize(deU128).getOrThrow())

        // i128 to u128
        assertEquals("1", U128Deserialize.deserialize(deI128).getOrThrow())

        // i128 to i128
        assertEquals("1", I128Deserialize.deserialize(deI128).getOrThrow())
    }

    @Test
    public fun testMapAccessToEnum() {
        val inner = MapDeserializer(
            listOf(StrDeserializer.new("lj_sigma") to F64Deserializer.new(14.0)).iterator(),
        )
        val outer = MapDeserializer(
            listOf(StrDeserializer.new("Airebo") to inner).iterator(),
        )

        val expected = Potential(PotentialKind.Airebo(Airebo(ljSigma = 14.0)))
        assertEquals(expected, Potential.deserialize(outer).getOrThrow())
    }
}
