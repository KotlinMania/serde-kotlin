// port-lint: tests test_suite/tests/test_deprecated.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDeprecatedJvmTest {
    @Test
    fun deprecatedDerivesCompileWithoutWarnings() {
        val generated =
            generateRustFixtureFromSerdeDerives(
                """
                #![deny(deprecated)]
                #![allow(dead_code)]

                use serde_derive::{Deserialize, Serialize};

                #[derive(Serialize, Deserialize)]
                #[deprecated]
                enum DeprecatedEnum {
                    A,
                    B,
                }

                #[derive(Serialize, Deserialize)]
                #[deprecated]
                struct DeprecatedStruct {
                    a: bool,
                }

                #[derive(Serialize, Deserialize)]
                enum DeprecatedVariant {
                    A,
                    #[deprecated]
                    B,
                }
                """.trimIndent(),
            )
        // Rust proc-macro field-access spans preserve deprecation hygiene, which rendered token source cannot preserve.
        assertEquals(3, generated.serdeDerivedItems)

        val output =
            compileRustFixture(
                fixtureName = "test_deprecated",
                source = generated.source,
                cargoArguments = listOf("check", "--quiet"),
            )
        assertEquals(0, output.exitCode, output.diagnostics)
    }
}
