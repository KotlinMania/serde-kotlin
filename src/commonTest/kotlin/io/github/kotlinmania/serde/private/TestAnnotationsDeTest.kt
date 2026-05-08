// port-lint: source test_suite/tests/test_annotations.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.core.`private`.Content
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.EnumAccess
import io.github.kotlinmania.serde.core.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals

public class TestAnnotationsDeTest {
    @Test
    public fun flatMapDeserializerFindsVariantInFlattenedData() {
        val value =
            FlatMapDeserializer(
                mutableListOf(Content.String("B") to Content.Bool(true)),
            ).deserializeEnum("E", listOf("B"), FlatBoolEnumVisitor)
                .getOrThrow()

        assertEquals("B" to true, value)
    }
}

private data object FlatStringVisitor : Visitor<String> {
    override fun expecting(): String = "a string"

    override fun visitStr(v: String): Result<String> = Result.success(v)
    override fun visitBorrowedStr(v: String): Result<String> = Result.success(v)
    override fun visitString(v: String): Result<String> = Result.success(v)
}

private data object BoolVisitor : Visitor<Boolean> {
    override fun expecting(): String = "a boolean"

    override fun visitBool(v: Boolean): Result<Boolean> = Result.success(v)
}

private data object FlatBoolEnumVisitor : Visitor<Pair<String, Boolean>> {
    override fun expecting(): String = "an enum"

    override fun <A> visitEnum(data: A): Result<Pair<String, Boolean>>
        where A : EnumAccess =
        runCatching {
            val (variant, variantAccess) =
                data.variantSeed(flatSeed { deserializer ->
                    deserializer.deserializeString(FlatStringVisitor)
                }).getOrThrow()
            val value =
                variantAccess.newtypeVariantSeed(
                    flatSeed { deserializer ->
                        deserializer.deserializeBool(BoolVisitor)
                    },
                ).getOrThrow()
            variant to value
        }
}

private fun <T> flatSeed(block: (Deserializer) -> Result<T>): DeserializeSeed<T> =
    object : DeserializeSeed<T> {
        override fun <D> deserialize(deserializer: D): Result<T>
            where D : Deserializer =
            block(deserializer)
    }

