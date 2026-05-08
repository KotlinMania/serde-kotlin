// port-lint: source test_suite/tests/test_ignored_any.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.core.de.Deserialize
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.EnumAccess
import io.github.kotlinmania.serde.core.de.IgnoredAny
import io.github.kotlinmania.serde.core.de.MapAccess
import io.github.kotlinmania.serde.core.de.SeqAccess
import io.github.kotlinmania.serde.core.de.VariantAccess
import io.github.kotlinmania.serde.core.de.Visitor
import io.github.kotlinmania.serde.core.de.value.MapDeserializer
import io.github.kotlinmania.serde.core.de.value.SeqDeserializer
import io.github.kotlinmania.serde.core.de.value.intoDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals

private sealed class Target {
    public data object UnitVariant : Target()
    public data class Newtype(val value: Int) : Target()
    public data class Tuple(val first: Int, val second: Int) : Target()
    public data class Struct(val a: Int) : Target()

    public companion object : Deserialize<Target> {
        override fun <D> deserialize(deserializer: D): Result<Target>
            where D : Deserializer =
            deserializer.deserializeEnum("Target", listOf("Unit", "Newtype", "Tuple", "Struct"), TargetVisitor)
    }
}

private class EnumDeserializer(
    private val variant: String,
) : Deserializer, EnumAccess, VariantAccess {
    override fun <V> deserializeAny(visitor: Visitor<V>): Result<V> =
        visitor.visitEnum(this)

    override fun <V> deserializeBool(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeI64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU8(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU16(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeU64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF32(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeF64(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeChar(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStr(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeString(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeBytes(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeByteBuf(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeOption(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnit(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeSeq(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeMap(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): Result<V> =
        visitor.visitEnum(this)
    override fun <V> deserializeIdentifier(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)
    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): Result<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): Result<Pair<V, VariantAccess>> =
        runCatching {
            seed.deserialize(variant.intoDeserializer()).getOrThrow() to this
        }

    override fun unitVariant(): Result<Unit> =
        Result.success(Unit)

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): Result<T> =
        seed.deserialize(10.intoDeserializer())

    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): Result<V> {
        len.hashCode()
        val seq = SeqDeserializer(listOf(1.intoDeserializer(), 2.intoDeserializer()).iterator())
        return visitor.visitSeq(seq)
    }

    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): Result<V> {
        fields.hashCode()
        val map = MapDeserializer(listOf("a".intoDeserializer() to 10.intoDeserializer()).iterator())
        return visitor.visitMap(map)
    }
}

private data object TargetVisitor : Visitor<Target> {
    override fun expecting(): String = "a Target enum"

    override fun <A> visitEnum(data: A): Result<Target>
        where A : EnumAccess =
        runCatching {
            val (variant, access) = data.variantSeed(IgnoredAnyStringSeed).getOrThrow()
            when (variant) {
                "Unit" -> {
                    access.unitVariant().getOrThrow()
                    Target.UnitVariant
                }
                "Newtype" -> Target.Newtype(access.newtypeVariantSeed(IgnoredAnyIntSeed).getOrThrow())
                "Tuple" -> access.tupleVariant(2, IgnoredAnyTupleVisitor).getOrThrow()
                "Struct" -> access.structVariant(listOf("a"), IgnoredAnyStructVisitor).getOrThrow()
                else -> throw AssertionError("unexpected variant $variant")
            }
        }
}

private data object IgnoredAnyStringSeed : DeserializeSeed<String> {
    override fun <D> deserialize(deserializer: D): Result<String>
        where D : Deserializer =
        deserializer.deserializeString(IgnoredAnyStringVisitor)
}

private data object IgnoredAnyStringVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): Result<String> = Result.success(v)
    override fun visitString(v: String): Result<String> = Result.success(v)
}

private data object IgnoredAnyIntSeed : DeserializeSeed<Int> {
    override fun <D> deserialize(deserializer: D): Result<Int>
        where D : Deserializer =
        deserializer.deserializeI32(IgnoredAnyIntVisitor)
}

private data object IgnoredAnyIntVisitor : Visitor<Int> {
    override fun expecting(): String = "an Int"

    override fun visitI32(v: Int): Result<Int> = Result.success(v)
}

private data object IgnoredAnyTupleVisitor : Visitor<Target> {
    override fun expecting(): String = "a tuple variant"

    override fun <A> visitSeq(seq: A): Result<Target>
        where A : SeqAccess =
        runCatching {
            val first = seq.nextElementSeed(IgnoredAnyIntSeed).getOrThrow() ?: throw AssertionError("missing first tuple field")
            val second = seq.nextElementSeed(IgnoredAnyIntSeed).getOrThrow() ?: throw AssertionError("missing second tuple field")
            Target.Tuple(first, second)
        }
}

private data object IgnoredAnyStructVisitor : Visitor<Target> {
    override fun expecting(): String = "a struct variant"

    override fun <A> visitMap(map: A): Result<Target>
        where A : MapAccess =
        runCatching {
            var a: Int? = null
            while (true) {
                val key = map.nextKeySeed(IgnoredAnyStringSeed).getOrThrow() ?: break
                if (key == "a") {
                    a = map.nextValueSeed(IgnoredAnyIntSeed).getOrThrow()
                } else {
                    map.nextValueSeed(IgnoredAny).getOrThrow()
                }
            }
            Target.Struct(a ?: throw AssertionError("missing field a"))
        }
}

public class TestIgnoredAnyTest {
    @Test
    public fun testDeserializeEnum() {
        // First just make sure the Deserializer implementation works.
        assertEquals(Target.UnitVariant, Target.deserialize(EnumDeserializer("Unit")).getOrThrow())
        assertEquals(Target.Newtype(10), Target.deserialize(EnumDeserializer("Newtype")).getOrThrow())
        assertEquals(Target.Tuple(1, 2), Target.deserialize(EnumDeserializer("Tuple")).getOrThrow())
        assertEquals(Target.Struct(a = 10), Target.deserialize(EnumDeserializer("Struct")).getOrThrow())

        // Now try IgnoredAny.
        IgnoredAny.deserialize(EnumDeserializer("Unit")).getOrThrow()
        IgnoredAny.deserialize(EnumDeserializer("Newtype")).getOrThrow()
        IgnoredAny.deserialize(EnumDeserializer("Tuple")).getOrThrow()
        IgnoredAny.deserialize(EnumDeserializer("Struct")).getOrThrow()
    }
}
