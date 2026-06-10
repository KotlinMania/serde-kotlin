// port-lint: source test_suite/tests/test_annotations.rs
package io.github.kotlinmania.serde.`private`

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.EnumAccess
import io.github.kotlinmania.serde.core.de.Visitor
import io.github.kotlinmania.serde.core.`private`.Content
import io.github.kotlinmania.serde.serdeCatching
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

    override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitBorrowedStr(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun visitString(v: String): SerdeResult<String> = SerdeResult.success(v)
}

private data object BoolVisitor : Visitor<Boolean> {
    override fun expecting(): String = "a boolean"

    override fun visitBool(v: Boolean): SerdeResult<Boolean> = SerdeResult.success(v)
}

private data object FlatBoolEnumVisitor : Visitor<Pair<String, Boolean>> {
    override fun expecting(): String = "an enum"

    override fun <A> visitEnum(access: A): SerdeResult<Pair<String, Boolean>>
        where A : EnumAccess =
        serdeCatching {
            val (variant, variantAccess) =
                access
                    .variantSeed(
                        flatSeed { deserializer ->
                            deserializer.deserializeString(FlatStringVisitor)
                        },
                    ).getOrThrow()
            val value =
                variantAccess
                    .newtypeVariantSeed(
                        flatSeed { deserializer ->
                            deserializer.deserializeBool(BoolVisitor)
                        },
                    ).getOrThrow()
            variant to value
        }
}

private fun <T> flatSeed(block: (Deserializer) -> SerdeResult<T>): DeserializeSeed<T> =
    object : DeserializeSeed<T> {
        override fun <D> deserialize(deserializer: D): SerdeResult<T>
            where D : Deserializer = block(deserializer)
    }
