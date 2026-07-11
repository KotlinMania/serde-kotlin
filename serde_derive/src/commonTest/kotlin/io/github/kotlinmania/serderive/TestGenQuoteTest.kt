// port-lint: tests test_suite/tests/test_gen.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

public class TestGenQuoteTest {
    @Test
    public fun serializeWithPathInterpolatesIntoGeneratedCall() {
        val path = tokens("ser_x")
        val selfVar = tokens("self")
        val member = tokens("x")

        val quoted = checkedQuote(
            "`#`path(&`#`selfVar.`#`member, __serializer)",
            "path" to path,
            "selfVar" to selfVar,
            "member" to member,
        ).compact()

        assertContains(quoted, "ser_x")
        assertContains(quoted, "__serializer")
    }

    @Test
    public fun generatedVariantArmKeepsBodyInterpolation() {
        val case = tokens("Self::Unit")
        val body = tokens("_serde::Serializer::serialize_unit(__serializer)")

        val quoted = checkedQuote(
            "`#`case => `#`body",
            "case" to case,
            "body" to body,
        ).compact()

        assertContains(quoted, "Self::Unit")
        assertContains(quoted, "serialize_unit")
    }

    @Test
    public fun missingInterpolationFailsBeforeGeneratingCode() {
        val error = assertFailsWith<IllegalArgumentException> {
            checkedQuote("`#`path(&self.x, __serializer)")
        }

        assertContains(error.message ?: "", "path")
    }
}

private fun tokens(source: String): TokenStream =
    TokenStream.fromString(source).getOrThrow()

private fun TokenStream.compact(): String =
    toString().replace(" ", "")
