// port-lint: tests test_suite/tests/test_identifier.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test
import kotlin.test.assertEquals

class TestIdentifierJvmTest {
    @Test
    fun variant1() {
        runVariantIdentifierTest(
            "variant1",
            """
            assert_de_tokens(&V::Aaa, &[Token::U8(0)]);
            assert_de_tokens(&V::Aaa, &[Token::U16(0)]);
            assert_de_tokens(&V::Aaa, &[Token::U32(0)]);
            assert_de_tokens(&V::Aaa, &[Token::U64(0)]);
            assert_de_tokens(&V::Aaa, &[Token::Str("Aaa")]);
            assert_de_tokens(&V::Aaa, &[Token::Bytes(b"Aaa")]);
            """.trimIndent(),
        )
    }

    @Test
    fun variantAliases() {
        runVariantIdentifierTest(
            "aliases",
            """
            assert_de_tokens(&V::Bbb, &[Token::U8(1)]);
            assert_de_tokens(&V::Bbb, &[Token::U16(1)]);
            assert_de_tokens(&V::Bbb, &[Token::U32(1)]);
            assert_de_tokens(&V::Bbb, &[Token::U64(1)]);
            assert_de_tokens(&V::Bbb, &[Token::Str("Bbb")]);
            assert_de_tokens(&V::Bbb, &[Token::Bytes(b"Bbb")]);
            assert_de_tokens(&V::Bbb, &[Token::Str("Ccc")]);
            assert_de_tokens(&V::Bbb, &[Token::Bytes(b"Ccc")]);
            assert_de_tokens(&V::Bbb, &[Token::Str("Ddd")]);
            assert_de_tokens(&V::Bbb, &[Token::Bytes(b"Ddd")]);
            """.trimIndent(),
        )
    }

    @Test
    fun variantUnknown() {
        runVariantIdentifierTest(
            "unknown",
            """
            assert_de_tokens_error::<V>(
                &[Token::U8(42)],
                "invalid value: integer `42`, expected variant index 0 <= i < 2",
            );
            assert_de_tokens_error::<V>(
                &[Token::U16(42)],
                "invalid value: integer `42`, expected variant index 0 <= i < 2",
            );
            assert_de_tokens_error::<V>(
                &[Token::U32(42)],
                "invalid value: integer `42`, expected variant index 0 <= i < 2",
            );
            assert_de_tokens_error::<V>(
                &[Token::U64(42)],
                "invalid value: integer `42`, expected variant index 0 <= i < 2",
            );
            assert_de_tokens_error::<V>(
                &[Token::Str("Unknown")],
                "unknown variant `Unknown`, expected one of `Aaa`, `Bbb`, `Ccc`, `Ddd`",
            );
            assert_de_tokens_error::<V>(
                &[Token::Bytes(b"Unknown")],
                "unknown variant `Unknown`, expected one of `Aaa`, `Bbb`, `Ccc`, `Ddd`",
            );
            """.trimIndent(),
        )
    }

    @Test
    fun field1() {
        runFieldIdentifierTest(
            "field1",
            """
            assert_de_tokens(&F::Aaa, &[Token::U8(0)]);
            assert_de_tokens(&F::Aaa, &[Token::U16(0)]);
            assert_de_tokens(&F::Aaa, &[Token::U32(0)]);
            assert_de_tokens(&F::Aaa, &[Token::U64(0)]);
            assert_de_tokens(&F::Aaa, &[Token::Str("aaa")]);
            assert_de_tokens(&F::Aaa, &[Token::Bytes(b"aaa")]);
            """.trimIndent(),
        )
    }

    @Test
    fun fieldAliases() {
        runFieldIdentifierTest(
            "aliases",
            """
            assert_de_tokens(&F::Bbb, &[Token::U8(1)]);
            assert_de_tokens(&F::Bbb, &[Token::U16(1)]);
            assert_de_tokens(&F::Bbb, &[Token::U32(1)]);
            assert_de_tokens(&F::Bbb, &[Token::U64(1)]);
            assert_de_tokens(&F::Bbb, &[Token::Str("bbb")]);
            assert_de_tokens(&F::Bbb, &[Token::Bytes(b"bbb")]);
            assert_de_tokens(&F::Bbb, &[Token::Str("ccc")]);
            assert_de_tokens(&F::Bbb, &[Token::Bytes(b"ccc")]);
            assert_de_tokens(&F::Bbb, &[Token::Str("ddd")]);
            assert_de_tokens(&F::Bbb, &[Token::Bytes(b"ddd")]);
            """.trimIndent(),
        )
    }

    @Test
    fun fieldUnknown() {
        runFieldIdentifierTest(
            "unknown",
            """
            assert_de_tokens_error::<F>(
                &[Token::U8(42)],
                "invalid value: integer `42`, expected field index 0 <= i < 2",
            );
            assert_de_tokens_error::<F>(
                &[Token::U16(42)],
                "invalid value: integer `42`, expected field index 0 <= i < 2",
            );
            assert_de_tokens_error::<F>(
                &[Token::U32(42)],
                "invalid value: integer `42`, expected field index 0 <= i < 2",
            );
            assert_de_tokens_error::<F>(
                &[Token::U64(42)],
                "invalid value: integer `42`, expected field index 0 <= i < 2",
            );
            assert_de_tokens_error::<F>(
                &[Token::Str("unknown")],
                "unknown field `unknown`, expected one of `aaa`, `bbb`, `ccc`, `ddd`",
            );
            assert_de_tokens_error::<F>(
                &[Token::Bytes(b"unknown")],
                "unknown field `unknown`, expected one of `aaa`, `bbb`, `ccc`, `ddd`",
            );
            """.trimIndent(),
        )
    }

    @Test
    fun unitFallthrough() {
        val output =
            compileIdentifierTest(
                fixtureName = "test_identifier_unit_fallthrough",
                deriveInput =
                    """
                    #[serde(field_identifier, rename_all = "snake_case")]
                    enum F {
                        Aaa,
                        Bbb,
                        #[serde(other)]
                        Other,
                    }
                    """.trimIndent(),
                declaration = "enum F { Aaa, Bbb, Other }",
                testName = "unit_fallthrough",
                body =
                    """
                    assert_de_tokens(&F::Other, &[Token::U8(42)]);
                    assert_de_tokens(&F::Other, &[Token::U16(42)]);
                    assert_de_tokens(&F::Other, &[Token::U32(42)]);
                    assert_de_tokens(&F::Other, &[Token::U64(42)]);
                    assert_de_tokens(&F::Other, &[Token::Str("x")]);
                    """.trimIndent(),
            )
        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun newtypeFallthrough() {
        val output =
            compileIdentifierTest(
                fixtureName = "test_identifier_newtype_fallthrough",
                deriveInput =
                    """
                    #[serde(field_identifier, rename_all = "snake_case")]
                    enum F {
                        Aaa,
                        Bbb,
                        Other(String),
                    }
                    """.trimIndent(),
                declaration = "enum F { Aaa, Bbb, Other(String) }",
                testName = "newtype_fallthrough",
                body = "assert_de_tokens(&F::Other(\"x\".to_owned()), &[Token::Str(\"x\")]);",
            )
        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun newtypeFallthroughGeneric() {
        val output =
            compileIdentifierTest(
                fixtureName = "test_identifier_newtype_fallthrough_generic",
                deriveInput =
                    """
                    #[serde(field_identifier, rename_all = "snake_case")]
                    enum F<T> {
                        Aaa,
                        Bbb,
                        Other(T),
                    }
                    """.trimIndent(),
                declaration = "enum F<T> { Aaa, Bbb, Other(T) }",
                testName = "newtype_fallthrough_generic",
                body =
                    """
                    assert_de_tokens(&F::Other(42u8), &[Token::U8(42)]);
                    assert_de_tokens(&F::Other(42u16), &[Token::U16(42)]);
                    assert_de_tokens(&F::Other(42u32), &[Token::U32(42)]);
                    assert_de_tokens(&F::Other(42u64), &[Token::U64(42)]);
                    assert_de_tokens(&F::Other("x".to_owned()), &[Token::Str("x")]);
                    """.trimIndent(),
            )
        assertEquals(0, output.exitCode, output.diagnostics)
    }
}

private fun runVariantIdentifierTest(
    testName: String,
    body: String,
) {
    val output =
        compileIdentifierTest(
            fixtureName = "test_identifier_variant_$testName",
            deriveInput =
                """
                #[serde(variant_identifier)]
                enum V {
                    Aaa,
                    #[serde(alias = "Ccc", alias = "Ddd")]
                    Bbb,
                }
                """.trimIndent(),
            declaration = "enum V { Aaa, Bbb }",
            testName = testName,
            body = body,
        )
    assertEquals(0, output.exitCode, output.diagnostics)
}

private fun runFieldIdentifierTest(
    testName: String,
    body: String,
) {
    val output =
        compileIdentifierTest(
            fixtureName = "test_identifier_field_$testName",
            deriveInput =
                """
                #[serde(field_identifier, rename_all = "snake_case")]
                enum F {
                    Aaa,
                    #[serde(alias = "ccc", alias = "ddd")]
                    Bbb,
                }
                """.trimIndent(),
            declaration = "enum F { Aaa, Bbb }",
            testName = testName,
            body = body,
        )
    assertEquals(0, output.exitCode, output.diagnostics)
}

private fun compileIdentifierTest(
    fixtureName: String,
    deriveInput: String,
    declaration: String,
    testName: String,
    body: String,
): CargoOutput =
    compileDerives(
        fixtureName = fixtureName,
        deriveInput = deriveInput,
        declaration = "#[derive(Debug, PartialEq)]\n$declaration",
        support = "use serde_test::{assert_de_tokens, assert_de_tokens_error, Token};",
        verify =
            """
            #[test]
            fn $testName() {
                $body
            }
            """.trimIndent(),
        generateSerialize = false,
        extraDependencies = "serde_test = \"1.0.176\"",
        cargoSubcommand = "test",
    )
