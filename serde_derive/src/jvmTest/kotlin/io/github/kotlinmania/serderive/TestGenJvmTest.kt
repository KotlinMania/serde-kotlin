// port-lint: tests test_suite/tests/test_gen.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Delimiter
import io.github.kotlinmania.procmacro2.Spacing
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.concurrent.thread

class TestGenJvmTest {
    @Test
    fun genericFieldWithCustomFunctionsCompiles() {
        val deriveInput =
            """
            struct With<T> {
                t: T,
                #[serde(serialize_with = "ser_x", deserialize_with = "de_x")]
                x: X,
            }
            """.trimIndent()
        val declaration =
            """
            struct With<T> {
                t: T,
                x: X,
            }
            """.trimIndent()
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned, Deserializer};
            use serde::ser::{Serialize, Serializer};
            use std::result::Result as StdResult;

            pub struct X;

            pub fn ser_x<S: Serializer>(_: &X, _: S) -> StdResult<S::Ok, S::Error> {
                unimplemented!()
            }

            pub fn de_x<'de, D: Deserializer<'de>>(_: D) -> StdResult<X, D::Error> {
                unimplemented!()
            }

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_with",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<With<i32>>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun genericFieldWithModuleFunctionsCompiles() {
        val deriveInput =
            """
            struct WithTogether<T> {
                t: T,
                #[serde(with = "both_x")]
                x: X,
            }
            """.trimIndent()
        val declaration =
            """
            struct WithTogether<T> {
                t: T,
                x: X,
            }
            """.trimIndent()
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned, Deserializer};
            use serde::ser::{Serialize, Serializer};
            use std::result::Result as StdResult;

            pub struct X;

            pub mod both_x {
                use super::{StdResult, X};
                use serde::de::Deserializer;
                use serde::ser::Serializer;

                pub fn serialize<S: Serializer>(_: &X, _: S) -> StdResult<S::Ok, S::Error> {
                    unimplemented!()
                }

                pub fn deserialize<'de, D: Deserializer<'de>>(_: D) -> StdResult<X, D::Error> {
                    unimplemented!()
                }
            }

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_with_together",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<WithTogether<i32>>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun emptyTupleAndSkippedTupleFieldsCompile() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val cases = listOf(
            CompileCase(
                fixtureName = "test_gen_empty_tuple",
                deriveInput = "pub struct EmptyTuple();",
                declaration = "pub struct EmptyTuple();",
                verifyType = "EmptyTuple",
            ),
            CompileCase(
                fixtureName = "test_gen_empty_tuple_deny_unknown",
                deriveInput = """
                    #[serde(deny_unknown_fields)]
                    pub struct EmptyTupleDenyUnknown();
                """.trimIndent(),
                declaration = "pub struct EmptyTupleDenyUnknown();",
                verifyType = "EmptyTupleDenyUnknown",
            ),
            CompileCase(
                fixtureName = "test_gen_tuple_skip_all",
                deriveInput = "pub struct TupleSkipAll(#[serde(skip_deserializing)] u8);",
                declaration = "pub struct TupleSkipAll(u8);",
                verifyType = "TupleSkipAll",
            ),
            CompileCase(
                fixtureName = "test_gen_tuple_skip_all_deny_unknown",
                deriveInput = """
                    #[serde(deny_unknown_fields)]
                    pub struct TupleSkipAllDenyUnknown(#[serde(skip_deserializing)] u8);
                """.trimIndent(),
                declaration = "pub struct TupleSkipAllDenyUnknown(u8);",
                verifyType = "TupleSkipAllDenyUnknown",
            ),
        )

        for (case in cases) {
            val output = compileDerives(
                fixtureName = case.fixtureName,
                deriveInput = case.deriveInput,
                declaration = case.declaration,
                support = support,
                verify = "fn verify() { assert_traits::<${case.verifyType}>(); }",
            )

            assertEquals(0, output.exitCode, output.diagnostics)
        }
    }
}

private data class CompileCase(
    val fixtureName: String,
    val deriveInput: String,
    val declaration: String,
    val verifyType: String,
)

private data class CargoOutput(
    val exitCode: Int,
    val diagnostics: String,
)

private fun compileDerives(
    fixtureName: String,
    deriveInput: String,
    declaration: String,
    support: String,
    verify: String,
): CargoOutput {
    val root = findRepositoryRoot()
    val fixture = root.resolve("build/rust-compile-tests/$fixtureName")
    fixture.toFile().deleteRecursively()
    Files.createDirectories(fixture.resolve("src"))

    val serialize = renderRust(deriveSerialize(TokenStream.fromString(deriveInput).getOrThrow()))
    val deserialize = renderRust(deriveDeserialize(TokenStream.fromString(deriveInput).getOrThrow()))

    val serdePath = root.resolve("tmp/serde/serde").toString().replace('\\', '/')
    fixture.resolve("Cargo.toml").writeText(
        """
        [package]
        name = "$fixtureName"
        version = "0.0.0"
        edition = "2021"

        [dependencies]
        serde = { path = "$serdePath" }
        """.trimIndent() + "\n",
    )
    fixture.resolve("src/lib.rs").writeText(
        buildString {
            appendLine("#![allow(dead_code)]")
            appendLine(support)
            appendLine(declaration)
            appendLine(serialize)
            appendLine(deserialize)
            appendLine(verify)
        },
    )

    val process =
        ProcessBuilder("cargo", "check", "--offline", "--quiet", "--manifest-path", fixture.resolve("Cargo.toml").toString())
            .redirectErrorStream(true)
            .start()
    val diagnostics = StringBuilder()
    val outputReader = thread(name = "cargo-output-$fixtureName") {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { diagnostics.appendLine(it) }
        }
    }
    val finished = process.waitFor(2, TimeUnit.MINUTES)
    if (!finished) {
        process.destroyForcibly()
    }
    outputReader.join()
    assertTrue(finished, "cargo check timed out for $fixtureName")
    return CargoOutput(process.exitValue(), diagnostics.toString())
}

private fun renderRust(tokens: TokenStream): String =
    buildString {
        for (token in tokens) {
            when (token) {
                is TokenTree.Group -> {
                    val (open, close) =
                        when (token.value.delimiter()) {
                            Delimiter.Parenthesis -> "(" to ")"
                            Delimiter.Brace -> "{" to "}"
                            Delimiter.Bracket -> "[" to "]"
                            Delimiter.None -> "" to ""
                        }
                    append(open)
                    append(renderRust(token.value.stream()))
                    append(close)
                    append(' ')
                }
                is TokenTree.Punct -> {
                    append(token.value.asChar())
                    if (token.value.spacing() == Spacing.Alone) append(' ')
                }
                else -> {
                    append(token.toString())
                    append(' ')
                }
            }
        }
    }.trim()

private fun findRepositoryRoot(): Path {
    var current = Path.of(System.getProperty("user.dir")).toAbsolutePath()
    while (!Files.exists(current.resolve("settings.gradle.kts"))) {
        current = current.parent ?: error("cannot locate serde-kotlin repository root")
    }
    return current
}
