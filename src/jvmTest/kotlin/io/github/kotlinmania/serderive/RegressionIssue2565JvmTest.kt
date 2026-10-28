// port-lint: tests test_suite/tests/regression/issue2565.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test

class RegressionIssue2565JvmTest {
    @Test
    fun simpleVariant() = runIssue2565RustTest("regression::issue2565::simple_variant")

    @Test
    fun flattenVariant() = runIssue2565RustTest("regression::issue2565::flatten_variant")
}

private val issue2565FixtureSource by lazy(::buildIssue2565FixtureSource)

private fun runIssue2565RustTest(testName: String) {
    runExactSerdeRustTest(
        fixtureName = "regression_issue2565",
        source = issue2565FixtureSource,
        testName = testName,
    )
}

private fun buildIssue2565FixtureSource(): String {
    val generated =
        generateRustFixtureFromSerdeDerives(
            """
            mod regression {
                mod issue2565 {
                    use serde_derive::{Deserialize, Serialize};
                    use serde_test::{assert_tokens, Token};

                    #[derive(Serialize, Deserialize, Debug, PartialEq)]
                    enum Enum {
                        Simple {
                            a: i32,
                        },
                        Flatten {
                            #[serde(flatten)]
                            flatten: (),
                            a: i32,
                        },
                    }

                    #[test]
                    fn simple_variant() {
                        assert_tokens(
                            &Enum::Simple { a: 42 },
                            &[
                                Token::StructVariant {
                                    name: "Enum",
                                    variant: "Simple",
                                    len: 1,
                                },
                                Token::Str("a"),
                                Token::I32(42),
                                Token::StructVariantEnd,
                            ],
                        );
                    }

                    #[test]
                    fn flatten_variant() {
                        assert_tokens(
                            &Enum::Flatten { flatten: (), a: 42 },
                            &[
                                Token::NewtypeVariant {
                                    name: "Enum",
                                    variant: "Flatten",
                                },
                                Token::Map { len: None },
                                Token::Str("a"),
                                Token::I32(42),
                                Token::MapEnd,
                            ],
                        );
                    }
                }
            }
            """.trimIndent(),
        )
    require(generated.serdeDerivedItems == 1) {
        "Expected 1 serde-derived declaration, transformed ${generated.serdeDerivedItems}"
    }
    return generated.source
}
