// port-lint: source test_suite/tests/test_value.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.I128Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.U128Deserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.value.F64Deserializer
import io.github.kotlinmania.serdecore.de.value.I128Deserializer
import io.github.kotlinmania.serdecore.de.value.MapAccessDeserializer
import io.github.kotlinmania.serdecore.de.value.MapDeserializer
import io.github.kotlinmania.serdecore.de.value.StrDeserializer
import io.github.kotlinmania.serdecore.de.value.U128Deserializer
import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import io.github.kotlinmania.serde.serdeCatching
import kotlin.test.Test
import kotlin.test.assertEquals

private enum class E {
    A,
    B,
    ;

    companion object : Deserialize<E> {
        override fun <D> deserialize(deserializer: D): SerdeResult<E>
            where D : Deserializer =
            deserializer.deserializeEnum("E", listOf("A", "B"), EVisitor)
    }
}

private data object EVisitor : Visitor<E> {
    override fun expecting(): String = "enum E"

    override fun <A> visitEnum(access: A): SerdeResult<E>
        where A : EnumAccess =
        serdeCatching {
            val (variant, access) = access.variantSeed(EVariantSeed).getOrThrow()
            access.unitVariant().getOrThrow()
            variant
        }
}

private data object EVariantSeed : DeserializeSeed<E> {
    override fun <D> deserialize(deserializer: D): SerdeResult<E>
        where D : Deserializer =
        deserializer.deserializeIdentifier(EVariantVisitor)
}

private data object EVariantVisitor : Visitor<E> {
    override fun expecting(): String = "variant identifier"

    override fun visitU32(v: UInt): SerdeResult<E> =
        when (v) {
            0u -> SerdeResult.success(E.A)
            1u -> SerdeResult.success(E.B)
            else -> SerdeResult.failure(SerdeError("variant index $v out of range for E"))
        }

    override fun visitU64(v: ULong): SerdeResult<E> = visitU32(v.toUInt())

    override fun visitStr(v: String): SerdeResult<E> =
        when (v) {
            "A" -> SerdeResult.success(E.A)
            "B" -> SerdeResult.success(E.B)
            else -> SerdeResult.failure(SerdeError("unknown variant name $v"))
        }
}

private data class Airebo(
    val ljSigma: Double,
) {
    companion object : Deserialize<Airebo> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Airebo>
            where D : Deserializer =
            deserializer.deserializeStruct("Airebo", listOf("lj_sigma"), AireboVisitor)
    }
}

private data object AireboVisitor : Visitor<Airebo> {
    override fun expecting(): String = "struct Airebo"

    override fun <A> visitMap(access: A): SerdeResult<Airebo>
        where A : MapAccess =
        serdeCatching {
            var ljSigma: Double? = null
            while (true) {
                val key = access.nextKeySeed(AireboFieldSeed).getOrThrow() ?: break
                when (key) {
                    AireboField.LjSigma -> ljSigma = access.nextValueSeed(F64Seed).getOrThrow()
                }
            }
            Airebo(ljSigma = ljSigma ?: throw AssertionError("missing field lj_sigma"))
        }
}

private enum class AireboField { LjSigma }

private data object AireboFieldSeed : DeserializeSeed<AireboField> {
    override fun <D> deserialize(deserializer: D): SerdeResult<AireboField>
        where D : Deserializer =
        deserializer.deserializeIdentifier(AireboFieldVisitor)
}

private data object AireboFieldVisitor : Visitor<AireboField> {
    override fun expecting(): String = "field identifier"

    override fun visitStr(v: String): SerdeResult<AireboField> =
        when (v) {
            "lj_sigma" -> SerdeResult.success(AireboField.LjSigma)
            else -> SerdeResult.failure(SerdeError("unknown field $v"))
        }
}

private data object F64Seed : DeserializeSeed<Double> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Double>
        where D : Deserializer =
        deserializer.deserializeF64(F64Capture)
}

private data object F64Capture : Visitor<Double> {
    override fun expecting(): String = "f64"

    override fun visitF64(v: Double): SerdeResult<Double> = SerdeResult.success(v)
}

private sealed class PotentialKind {
    data class Airebo(
        val value: io.github.kotlinmania.serde.`private`.Airebo,
    ) : PotentialKind()

    companion object : Deserialize<PotentialKind> {
        override fun <D> deserialize(deserializer: D): SerdeResult<PotentialKind>
            where D : Deserializer =
            deserializer.deserializeEnum("PotentialKind", listOf("Airebo"), PotentialKindVisitor)
    }
}

private data object PotentialKindVisitor : Visitor<PotentialKind> {
    override fun expecting(): String = "enum PotentialKind"

    override fun <A> visitEnum(access: A): SerdeResult<PotentialKind>
        where A : EnumAccess =
        serdeCatching {
            val (variant, access) = access.variantSeed(PotentialKindVariantSeed).getOrThrow()
            when (variant) {
                PotentialKindVariant.Airebo ->
                    PotentialKind.Airebo(access.newtypeVariantSeed(AireboDeserializeSeed).getOrThrow())
            }
        }
}

private enum class PotentialKindVariant { Airebo }

private data object PotentialKindVariantSeed : DeserializeSeed<PotentialKindVariant> {
    override fun <D> deserialize(deserializer: D): SerdeResult<PotentialKindVariant>
        where D : Deserializer =
        deserializer.deserializeIdentifier(PotentialKindVariantVisitor)
}

private data object PotentialKindVariantVisitor : Visitor<PotentialKindVariant> {
    override fun expecting(): String = "variant identifier"

    override fun visitStr(v: String): SerdeResult<PotentialKindVariant> =
        when (v) {
            "Airebo" -> SerdeResult.success(PotentialKindVariant.Airebo)
            else -> SerdeResult.failure(SerdeError("unknown variant $v"))
        }
}

private data object AireboDeserializeSeed : DeserializeSeed<Airebo> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Airebo>
        where D : Deserializer = Airebo.deserialize(deserializer)
}

private data class Potential(
    val kind: PotentialKind,
) {
    companion object : Deserialize<Potential> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Potential>
            where D : Deserializer =
            deserializer.deserializeAny(PotentialVisitor)
    }
}

private data object PotentialVisitor : Visitor<Potential> {
    override fun expecting(): String = "a map"

    override fun <A> visitMap(access: A): SerdeResult<Potential>
        where A : MapAccess =
        PotentialKind.deserialize(MapAccessDeserializer.new(access)).map { Potential(it) }
}

class TestValueTest {
    @Test
    fun testU32ToEnum() {
        val deserializer = 1u.intoDeserializer()
        val e = E.deserialize(deserializer).getOrThrow()
        assertEquals(E.B, e)
    }

    @Test
    fun testInteger128() {
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
    fun testMapAccessToEnum() {
        val inner =
            MapDeserializer(
                listOf(StrDeserializer.new("lj_sigma") to F64Deserializer.new(14.0)).iterator(),
            )
        val outer =
            MapDeserializer(
                listOf(StrDeserializer.new("Airebo") to inner).iterator(),
            )

        val expected = Potential(PotentialKind.Airebo(Airebo(ljSigma = 14.0)))
        assertEquals(expected, Potential.deserialize(outer).getOrThrow())
    }
}
