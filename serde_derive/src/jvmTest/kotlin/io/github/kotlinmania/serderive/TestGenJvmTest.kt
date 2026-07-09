// port-lint: tests test_suite/tests/test_gen.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}

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

    val serialize = deriveSerialize(TokenStream.fromString(deriveInput).getOrThrow()).toString()
    val deserialize = deriveDeserialize(TokenStream.fromString(deriveInput).getOrThrow()).toString()

    fixture.resolve("Cargo.toml").writeText(
        """
        [package]
        name = "$fixtureName"
        version = "0.0.0"
        edition = "2021"

        [dependencies]
        serde = "1"
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
        ProcessBuilder("cargo", "check", "--quiet", "--manifest-path", fixture.resolve("Cargo.toml").toString())
            .redirectErrorStream(true)
            .start()
    val finished = process.waitFor(2, TimeUnit.MINUTES)
    if (!finished) {
        process.destroyForcibly()
    }
    assertTrue(finished, "cargo check timed out for $fixtureName")
    return CargoOutput(process.exitValue(), process.inputStream.bufferedReader().readText())
}

private fun findRepositoryRoot(): Path {
    var current = Path.of(System.getProperty("user.dir")).toAbsolutePath()
    while (!Files.exists(current.resolve("tmp/serde/serde/Cargo.toml"))) {
        current = current.parent ?: error("cannot locate serde-kotlin repository root")
    }
    return current
}
