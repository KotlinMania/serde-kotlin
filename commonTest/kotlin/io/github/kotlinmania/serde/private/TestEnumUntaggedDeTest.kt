// port-lint: source test_suite/tests/test_enum_untagged.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.core.`private`.Content
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.EnumAccess
import io.github.kotlinmania.serde.core.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals

public class TestEnumUntaggedDeTest {
    @Test
    public fun newtypeStructFallsThroughToUnderlyingValue() {
        val value =
            ContentRefDeserializer
                .new(Content.U32(5u))
                .deserializeNewtypeStruct("NewtypeStruct", NewtypeUIntVisitor)
                .getOrThrow()

        assertEquals(5u, value)
    }

    @Test
    public fun optionalFieldAcceptsSomeWithoutMarker() {
        val value =
            ContentRefDeserializer
                .new(Content.U32(42u))
                .deserializeOption(NullableUIntVisitor)
                .getOrThrow()

        assertEquals(42u, value)
    }

    @Test
    public fun optionalFieldAcceptsUnitAsNone() {
        val value =
            ContentRefDeserializer
                .new(Content.Unit)
                .deserializeOption(NullableUIntVisitor)
                .getOrThrow()

        assertEquals(null, value)
    }

    @Test
    public fun emptyMapDeserializesAsUnit() {
        val value =
            ContentDeserializer
                .new(Content.Map(emptyList()))
                .deserializeUnit(UnitVisitor)
                .getOrThrow()

        assertEquals(Unit, value)
    }

    @Test
    public fun emptyMapDeserializesAsUnitStruct() {
        val value =
            ContentDeserializer
                .new(Content.Map(emptyList()))
                .deserializeUnitStruct("Info", UnitVisitor)
                .getOrThrow()

        assertEquals(Unit, value)
    }

    @Test
    public fun enumMapDeserializesSingleKeyVariant() {
        val value =
            ContentDeserializer
                .new(Content.Map(listOf(Content.String("Newtype") to Content.U32(5u))))
                .deserializeEnum("E", listOf("Newtype", "Null"), EnumNewtypeVisitor)
                .getOrThrow()

        assertEquals("Newtype" to 5u, value)
    }
}

private data object UIntVisitor : Visitor<UInt> {
    override fun expecting(): String = "a `UInt` value"

    override fun visitU32(v: UInt): Result<UInt> = Result.success(v)
}

private data object StringVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): Result<String> = Result.success(v)
    override fun visitBorrowedStr(v: String): Result<String> = Result.success(v)
    override fun visitString(v: String): Result<String> = Result.success(v)
}

private data object UnitVisitor : Visitor<Unit> {
    override fun expecting(): String = "unit"

    override fun visitUnit(): Result<Unit> = Result.success(Unit)
}

private data object NullableUIntVisitor : Visitor<UInt?> {
    override fun expecting(): String = "an optional `UInt` value"

    override fun visitNone(): Result<UInt?> = Result.success(null)
    override fun visitUnit(): Result<UInt?> = Result.success(null)

    override fun <D> visitSome(deserializer: D): Result<UInt?>
        where D : Deserializer =
        runCatching<UInt?> { deserializer.deserializeU32(UIntVisitor).getOrThrow() }
}

private data object NewtypeUIntVisitor : Visitor<UInt> {
    override fun expecting(): String = "a newtype struct"

    override fun <D> visitNewtypeStruct(deserializer: D): Result<UInt>
        where D : Deserializer =
        deserializer.deserializeU32(UIntVisitor)
}

private data object EnumNewtypeVisitor : Visitor<Pair<String, UInt>> {
    override fun expecting(): String = "an enum"

    override fun <A> visitEnum(data: A): Result<Pair<String, UInt>>
        where A : EnumAccess =
        runCatching {
            val (variant, variantAccess) =
                data.variantSeed(seed { deserializer ->
                    deserializer.deserializeString(StringVisitor)
                }).getOrThrow()
            val value =
                variantAccess.newtypeVariantSeed(
                    seed { deserializer ->
                        deserializer.deserializeU32(UIntVisitor)
                    },
                ).getOrThrow()
            variant to value
        }
}

private fun <T> seed(block: (Deserializer) -> Result<T>): DeserializeSeed<T> =
    object : DeserializeSeed<T> {
        override fun <D> deserialize(deserializer: D): Result<T>
            where D : Deserializer =
            block(deserializer)
    }

