// port-lint: tests test_suite/tests/test_ignored_any.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.value.MapDeserializer
import io.github.kotlinmania.serdecore.de.value.SeqDeserializer
import io.github.kotlinmania.serdecore.de.value.intoDeserializer
import io.github.kotlinmania.serdecore.de.value.mapDeserializer
import io.github.kotlinmania.serdecore.de.value.MapEntry
import io.github.kotlinmania.serdecore.de.value.seqDeserializer
import io.github.kotlinmania.serde.serdeCatching
import kotlin.test.Test
import kotlin.test.assertEquals

private sealed class Target {
    public data object UnitVariant : Target()

    public data class Newtype(
        val value: Int,
    ) : Target()

    public data class Tuple(
        val first: Int,
        val second: Int,
    ) : Target()

    public data class Struct(
        val a: Int,
    ) : Target()

    public companion object : Deserialize<Target> {
        override fun <D> deserialize(deserializer: D): SerdeResult<Target>
            where D : Deserializer =
            deserializer.deserializeEnum("Target", listOf("Unit", "Newtype", "Tuple", "Struct"), TargetVisitor)
    }
}

private class TestEnumDeserializer(
    private val variant: String,
) : Deserializer,
    EnumAccess,
    VariantAccess {
    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitEnum(this)

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

    override fun <V> deserializeUnitStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(
        name: String,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> = deserializeAny(visitor)

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
    ): SerdeResult<V> = visitor.visitEnum(this)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        serdeCatching {
            seed.deserialize(variant.intoDeserializer()).getOrThrow() to this
        }

    override fun unitVariant(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> = seed.deserialize(10.intoDeserializer())

    override fun <V> tupleVariant(
        len: Int,
        visitor: Visitor<V>,
    ): SerdeResult<V> {
        val seq = seqDeserializer(listOf(1.intoDeserializer(), 2.intoDeserializer()).iterator())
        return visitor.visitSeq(seq)
    }

    override fun <V> structVariant(
        fields: List<String>,
        visitor: Visitor<V>,
    ): SerdeResult<V> {
        val map = mapDeserializer(listOf(MapEntry("a".intoDeserializer(), 10.intoDeserializer())).iterator())
        return visitor.visitMap(map)
    }
}

private data object TargetVisitor : Visitor<Target> {
    override fun expecting(): String = "a Target enum"

    override fun <A> visitEnum(access: A): SerdeResult<Target>
        where A : EnumAccess =
        serdeCatching {
            val (variant, access) = access.variantSeed(IgnoredAnyStringSeed).getOrThrow()
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
    override fun <D> deserialize(deserializer: D): SerdeResult<String>
        where D : Deserializer =
        deserializer.deserializeString(IgnoredAnyStringVisitor)
}

private data object IgnoredAnyStringVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitString(v: String): SerdeResult<String> = SerdeResult.success(v)
}

private data object IgnoredAnyIntSeed : DeserializeSeed<Int> {
    override fun <D> deserialize(deserializer: D): SerdeResult<Int>
        where D : Deserializer =
        deserializer.deserializeI32(IgnoredAnyIntVisitor)
}

private data object IgnoredAnyIntVisitor : Visitor<Int> {
    override fun expecting(): String = "an Int"

    override fun visitI32(v: Int): SerdeResult<Int> = SerdeResult.success(v)
}

private data object IgnoredAnyTupleVisitor : Visitor<Target> {
    override fun expecting(): String = "a tuple variant"

    override fun <A> visitSeq(access: A): SerdeResult<Target>
        where A : SeqAccess =
        serdeCatching {
            val first = access.nextElementSeed(IgnoredAnyIntSeed).getOrThrow() ?: throw AssertionError("missing first tuple field")
            val second = access.nextElementSeed(IgnoredAnyIntSeed).getOrThrow() ?: throw AssertionError("missing second tuple field")
            Target.Tuple(first, second)
        }
}

private data object IgnoredAnyStructVisitor : Visitor<Target> {
    override fun expecting(): String = "a struct variant"

    override fun <A> visitMap(access: A): SerdeResult<Target>
        where A : MapAccess =
        serdeCatching {
            var a: Int? = null
            while (true) {
                val key = access.nextKeySeed(IgnoredAnyStringSeed).getOrThrow() ?: break
                if (key == "a") {
                    a = access.nextValueSeed(IgnoredAnyIntSeed).getOrThrow()
                } else {
                    val ignored = access.nextValueSeed(IgnoredAny).getOrThrow()
                    check(ignored == IgnoredAny)
                }
            }
            Target.Struct(a ?: throw AssertionError("missing field a"))
        }
}

public class TestIgnoredAnyTest {
    @Test
    public fun testDeserializeEnum() {
        // First just make sure the Deserializer implementation works.
        assertEquals(Target.UnitVariant, Target.deserialize(TestEnumDeserializer("Unit")).getOrThrow())
        assertEquals(Target.Newtype(10), Target.deserialize(TestEnumDeserializer("Newtype")).getOrThrow())
        assertEquals(Target.Tuple(1, 2), Target.deserialize(TestEnumDeserializer("Tuple")).getOrThrow())
        assertEquals(Target.Struct(a = 10), Target.deserialize(TestEnumDeserializer("Struct")).getOrThrow())

        // Now try IgnoredAny.
        assertEquals(IgnoredAny, IgnoredAny.deserialize(TestEnumDeserializer("Unit")).getOrThrow())
        assertEquals(IgnoredAny, IgnoredAny.deserialize(TestEnumDeserializer("Newtype")).getOrThrow())
        assertEquals(IgnoredAny, IgnoredAny.deserialize(TestEnumDeserializer("Tuple")).getOrThrow())
        assertEquals(IgnoredAny, IgnoredAny.deserialize(TestEnumDeserializer("Struct")).getOrThrow())
    }
}
