// port-lint: tests test_suite/tests/unstable/mod.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test
import kotlin.test.assertEquals

class TestUnstableJvmTest {
    @Test
    fun testRawIdentifiers() {
        val output =
            compileDerives(
                fixtureName = "test_unstable_raw_identifiers",
                deriveInput =
                    """
                    #[allow(non_camel_case_types)]
                    enum r#type {
                        r#type { r#type: () },
                    }
                    """.trimIndent(),
                declaration =
                    """
                    #[allow(non_camel_case_types)]
                    #[derive(Debug, PartialEq)]
                    enum r#type {
                        r#type { r#type: () },
                    }
                    """.trimIndent(),
                support = "use serde_test::{assert_tokens, Token};",
                verify =
                    """
                    #[test]
                    fn test_raw_identifiers() {
                        assert_tokens(
                            &r#type::r#type { r#type: () },
                            &[
                                Token::StructVariant {
                                    name: "type",
                                    variant: "type",
                                    len: 1,
                                },
                                Token::Str("type"),
                                Token::Unit,
                                Token::StructVariantEnd,
                            ],
                        );
                    }
                    """.trimIndent(),
                extraDependencies = "serde_test = \"1.0.176\"",
                cargoSubcommand = "test",
            )

        assertEquals(0, output.exitCode, output.diagnostics)
    }
}
