// port-lint: tests test_suite/tests/test_enum_adjacently_tagged.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test

class TestEnumAdjacentlyTaggedJvmTest {
    @Test
    fun unitMapStrTagOnly() = runAdjacentlyTaggedRustTest("unit::map_str_tag_only")

    @Test
    fun unitMapIntTagOnly() = runAdjacentlyTaggedRustTest("unit::map_int_tag_only")

    @Test
    fun unitMapBytesTagOnly() = runAdjacentlyTaggedRustTest("unit::map_bytes_tag_only")

    @Test
    fun unitMapStrTagContent() = runAdjacentlyTaggedRustTest("unit::map_str_tag_content")

    @Test
    fun unitMapIntTagContent() = runAdjacentlyTaggedRustTest("unit::map_int_tag_content")

    @Test
    fun unitMapBytesTagContent() = runAdjacentlyTaggedRustTest("unit::map_bytes_tag_content")

    @Test
    fun unitSeqTagContent() = runAdjacentlyTaggedRustTest("unit::seq_tag_content")

    @Test
    fun newtypeMapTagOnly() = runAdjacentlyTaggedRustTest("newtype::map_tag_only")

    @Test
    fun newtypeMapTagContent() = runAdjacentlyTaggedRustTest("newtype::map_tag_content")

    @Test
    fun newtypeSeq() = runAdjacentlyTaggedRustTest("newtype::seq")

    @Test
    fun newtypeWithNewtype() = runAdjacentlyTaggedRustTest("newtype_with_newtype")

    @Test
    fun tupleMap() = runAdjacentlyTaggedRustTest("tuple::map")

    @Test
    fun tupleSeq() = runAdjacentlyTaggedRustTest("tuple::seq")

    @Test
    fun structMap() = runAdjacentlyTaggedRustTest("struct_::map")

    @Test
    fun structSeq() = runAdjacentlyTaggedRustTest("struct_::seq")

    @Test
    fun structWithFlatten() = runAdjacentlyTaggedRustTest("struct_with_flatten")

    @Test
    fun expectingMessage() = runAdjacentlyTaggedRustTest("expecting_message")

    @Test
    fun partiallyUntagged() = runAdjacentlyTaggedRustTest("partially_untagged")

    @Test
    fun denyUnknownFields() = runAdjacentlyTaggedRustTest("deny_unknown_fields")
}
private val adjacentlyTaggedFixtureSource by lazy(::buildAdjacentlyTaggedFixtureSource)

private fun runAdjacentlyTaggedRustTest(testName: String) {
    runExactSerdeRustTest(
        fixtureName = "test_enum_adjacently_tagged",
        source = adjacentlyTaggedFixtureSource,
        testName = testName,
    )
}

private fun buildAdjacentlyTaggedFixtureSource(): String {
    val adjacentlyTaggedType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "t", content = "c")]
                enum AdjacentlyTagged<T> {
                    Unit,
                    Newtype(T),
                    Tuple(u8, u8),
                    Struct { f: u8 },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum AdjacentlyTagged<T> {
                    Unit,
                    Newtype(T),
                    Tuple(u8, u8),
                    Struct { f: u8 },
                }
                """.trimIndent(),
        )
    val newtypeStructType =
        generatedDerives(
            "struct NewtypeStruct(u32);",
            "#[derive(Debug, PartialEq)] struct NewtypeStruct(u32);",
        )
    val flattenDataType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "t", content = "c")]
                enum Data {
                    A {
                        a: i32,
                        #[serde(flatten)]
                        flat: Flat,
                    },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(PartialEq, Debug)]
                enum Data {
                    A { a: i32, flat: Flat },
                }
                """.trimIndent(),
        )
    val flatType =
        generatedDerives(
            "struct Flat { b: i32 }",
            "#[derive(PartialEq, Debug)] struct Flat { b: i32 }",
        )
    val expectingType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag", content = "content")]
                #[serde(expecting = "something strange...")]
                enum Enum {
                    AdjacentlyTagged,
                }
                """.trimIndent(),
            declaration = "enum Enum { AdjacentlyTagged }",
            serialize = false,
        )
    val partiallyUntaggedType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "t", content = "c")]
                enum Data {
                    A(u32),
                    B,
                    #[serde(untagged)]
                    Var(u32),
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(PartialEq, Debug)]
                enum Data {
                    A(u32),
                    B,
                    Var(u32),
                }
                """.trimIndent(),
        )
    val denyUnknownFieldsType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "t", content = "c", deny_unknown_fields)]
                enum AdjacentlyTagged {
                    Unit,
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum AdjacentlyTagged {
                    Unit,
                }
                """.trimIndent(),
            serialize = false,
        )

    return """
        #![deny(trivial_numeric_casts)]
        #![allow(dead_code)]

        use serde_test::{assert_de_tokens, assert_de_tokens_error, assert_tokens, Token};

        $adjacentlyTaggedType

        mod unit {
            use super::*;

            #[test]
            fn map_str_tag_only() {
                // Map: tag only
                assert_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 1,
                        },
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );

                // Map: tag only and incorrect hint for number of elements
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn map_int_tag_only() {
                // Map: tag (as number) only
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 1,
                        },
                        Token::U16(0),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn map_bytes_tag_only() {
                // Map: tag only
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 1,
                        },
                        Token::Bytes(b"t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );

                // Map: tag only
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 1,
                        },
                        Token::BorrowedBytes(b"t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn map_str_tag_content() {
                // Map: tag + content
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::Str("c"),
                        Token::Unit,
                        Token::StructEnd,
                    ],
                );
                // Map: content + tag
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("c"),
                        Token::Unit,
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );

                // Map: tag + content + excess fields (f, g, h)
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("f"),
                        Token::Unit,
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::Str("g"),
                        Token::Unit,
                        Token::Str("c"),
                        Token::Unit,
                        Token::Str("h"),
                        Token::Unit,
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn map_int_tag_content() {
                // Map: tag (as number) + content (as number)
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::U8(0),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::U8(1),
                        Token::Unit,
                        Token::StructEnd,
                    ],
                );

                // Map: content (as number) + tag (as number)
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::U64(1),
                        Token::Unit,
                        Token::U64(0),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn map_bytes_tag_content() {
                // Map: tag + content
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::BorrowedBytes(b"t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::BorrowedBytes(b"c"),
                        Token::Unit,
                        Token::StructEnd,
                    ],
                );

                // Map: content + tag
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Bytes(b"c"),
                        Token::Unit,
                        Token::Bytes(b"t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn seq_tag_content() {
                // Seq: tag and content
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Seq { len: Some(2) },
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Unit",
                        },
                        Token::Unit,
                        Token::SeqEnd,
                    ],
                );

                // Seq: tag (as string) and content
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Seq { len: None },
                        Token::Str("Unit"), // tag
                        Token::Unit,        // content
                        Token::SeqEnd,
                    ],
                );

                // Seq: tag (as borrowed string) and content
                assert_de_tokens(
                    &AdjacentlyTagged::Unit::<u8>,
                    &[
                        Token::Seq { len: None },
                        Token::BorrowedStr("Unit"), // tag
                        Token::Unit,                // content
                        Token::SeqEnd,
                    ],
                );
            }
        }

        mod newtype {
            use super::*;

            #[test]
            fn map_tag_only() {
                // optional newtype with no content field
                assert_de_tokens(
                    &AdjacentlyTagged::Newtype::<Option<u8>>(None),
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 1,
                        },
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Newtype",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn map_tag_content() {
                let value = AdjacentlyTagged::Newtype::<u8>(1);

                // Map: tag + content
                assert_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Newtype",
                        },
                        Token::Str("c"),
                        Token::U8(1),
                        Token::StructEnd,
                    ],
                );

                // Map: content + tag
                assert_de_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("c"),
                        Token::U8(1),
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Newtype",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn seq() {
                let value = AdjacentlyTagged::Newtype::<u8>(1);

                // Seq: tag and content
                assert_de_tokens(
                    &value,
                    &[
                        Token::Seq { len: Some(2) },
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Newtype",
                        },
                        Token::U8(1),
                        Token::SeqEnd,
                    ],
                );

                // Seq: tag (as string) and content
                assert_de_tokens(
                    &value,
                    &[
                        Token::Seq { len: None },
                        Token::Str("Newtype"), // tag
                        Token::U8(1),          // content
                        Token::SeqEnd,
                    ],
                );

                // Seq: tag (as borrowed string) and content
                assert_de_tokens(
                    &value,
                    &[
                        Token::Seq { len: None },
                        Token::BorrowedStr("Newtype"), // tag
                        Token::U8(1),                  // content
                        Token::SeqEnd,
                    ],
                );
            }
        }

        #[test]
        fn newtype_with_newtype() {
            $newtypeStructType

            assert_de_tokens(
                &AdjacentlyTagged::Newtype(NewtypeStruct(5)),
                &[
                    Token::Struct {
                        name: "AdjacentlyTagged",
                        len: 2,
                    },
                    Token::Str("c"),
                    Token::NewtypeStruct {
                        name: "NewtypeStruct",
                    },
                    Token::U32(5),
                    Token::Str("t"),
                    Token::UnitVariant {
                        name: "AdjacentlyTagged",
                        variant: "Newtype",
                    },
                    Token::StructEnd,
                ],
            );
        }

        mod tuple {
            use super::*;

            #[test]
            fn map() {
                let value = AdjacentlyTagged::Tuple::<u8>(1, 1);

                // Map: tag + content
                assert_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Tuple",
                        },
                        Token::Str("c"),
                        Token::Tuple { len: 2 },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleEnd,
                        Token::StructEnd,
                    ],
                );

                // Map: content + tag
                assert_de_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("c"),
                        Token::Tuple { len: 2 },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleEnd,
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Tuple",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn seq() {
                let value = AdjacentlyTagged::Tuple::<u8>(1, 1);

                // Seq: tag + content
                assert_de_tokens(
                    &value,
                    &[
                        Token::Seq { len: Some(2) },
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Tuple",
                        },
                        Token::Tuple { len: 2 },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleEnd,
                        Token::SeqEnd,
                    ],
                );
            }
        }

        mod struct_ {
            use super::*;

            #[test]
            fn map() {
                let value = AdjacentlyTagged::Struct::<u8> { f: 1 };

                // Map: tag + content
                assert_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Struct",
                        },
                        Token::Str("c"),
                        Token::Struct {
                            name: "Struct",
                            len: 1,
                        },
                        Token::Str("f"),
                        Token::U8(1),
                        Token::StructEnd,
                        Token::StructEnd,
                    ],
                );

                // Map: content + tag
                assert_de_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "AdjacentlyTagged",
                            len: 2,
                        },
                        Token::Str("c"),
                        Token::Struct {
                            name: "Struct",
                            len: 1,
                        },
                        Token::Str("f"),
                        Token::U8(1),
                        Token::StructEnd,
                        Token::Str("t"),
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Struct",
                        },
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn seq() {
                let value = AdjacentlyTagged::Struct::<u8> { f: 1 };

                // Seq: tag + content
                assert_de_tokens(
                    &value,
                    &[
                        Token::Seq { len: Some(2) },
                        Token::UnitVariant {
                            name: "AdjacentlyTagged",
                            variant: "Struct",
                        },
                        Token::Struct {
                            name: "Struct",
                            len: 1,
                        },
                        Token::Str("f"),
                        Token::U8(1),
                        Token::StructEnd,
                        Token::SeqEnd,
                    ],
                );
            }
        }

        #[test]
        fn struct_with_flatten() {
            $flattenDataType

            $flatType

            let data = Data::A {
                a: 0,
                flat: Flat { b: 0 },
            };

            assert_tokens(
                &data,
                &[
                    Token::Struct {
                        name: "Data",
                        len: 2,
                    },
                    Token::Str("t"),
                    Token::UnitVariant {
                        name: "Data",
                        variant: "A",
                    },
                    Token::Str("c"),
                    Token::Map { len: None },
                    Token::Str("a"),
                    Token::I32(0),
                    Token::Str("b"),
                    Token::I32(0),
                    Token::MapEnd,
                    Token::StructEnd,
                ],
            );
        }

        #[test]
        fn expecting_message() {
            $expectingType

            assert_de_tokens_error::<Enum>(
                &[Token::Str("AdjacentlyTagged")],
                r#"invalid type: string "AdjacentlyTagged", expected something strange..."#,
            );

            assert_de_tokens_error::<Enum>(
                &[Token::Map { len: None }, Token::Unit],
                r#"invalid type: unit value, expected "tag", "content", or other ignored fields"#,
            );

            // Check that #[serde(expecting = "...")] doesn't affect variant identifier error message
            assert_de_tokens_error::<Enum>(
                &[Token::Map { len: None }, Token::Str("tag"), Token::Unit],
                "invalid type: unit value, expected variant of enum Enum",
            );
        }

        #[test]
        fn partially_untagged() {
            $partiallyUntaggedType

            let data = Data::A(7);

            assert_de_tokens(
                &data,
                &[
                    Token::Map { len: None },
                    Token::Str("t"),
                    Token::Str("A"),
                    Token::Str("c"),
                    Token::U32(7),
                    Token::MapEnd,
                ],
            );

            let data = Data::Var(42);

            assert_de_tokens(&data, &[Token::U32(42)]);

        }

        #[test]
        fn deny_unknown_fields() {
            $denyUnknownFieldsType

            assert_de_tokens(
                &AdjacentlyTagged::Unit,
                &[
                    Token::Struct {
                        name: "AdjacentlyTagged",
                        len: 2,
                    },
                    Token::Str("t"),
                    Token::UnitVariant {
                        name: "AdjacentlyTagged",
                        variant: "Unit",
                    },
                    Token::Str("c"),
                    Token::Unit,
                    Token::StructEnd,
                ],
            );

            assert_de_tokens_error::<AdjacentlyTagged>(
                &[
                    Token::Struct {
                        name: "AdjacentlyTagged",
                        len: 2,
                    },
                    Token::Str("t"),
                    Token::UnitVariant {
                        name: "AdjacentlyTagged",
                        variant: "Unit",
                    },
                    Token::Str("c"),
                    Token::Unit,
                    Token::Str("h"),
                ],
                r#"invalid value: string "h", expected "t" or "c""#,
            );

            assert_de_tokens_error::<AdjacentlyTagged>(
                &[
                    Token::Struct {
                        name: "AdjacentlyTagged",
                        len: 2,
                    },
                    Token::Str("h"),
                ],
                r#"invalid value: string "h", expected "t" or "c""#,
            );

            assert_de_tokens_error::<AdjacentlyTagged>(
                &[
                    Token::Struct {
                        name: "AdjacentlyTagged",
                        len: 2,
                    },
                    Token::Str("c"),
                    Token::Unit,
                    Token::Str("h"),
                ],
                r#"invalid value: string "h", expected "t" or "c""#,
            );

            assert_de_tokens_error::<AdjacentlyTagged>(
                &[
                    Token::Struct {
                        name: "AdjacentlyTagged",
                        len: 2,
                    },
                    Token::U64(0), // tag field
                    Token::UnitVariant {
                        name: "AdjacentlyTagged",
                        variant: "Unit",
                    },
                    Token::U64(3),
                ],
                r#"invalid value: integer `3`, expected "t" or "c""#,
            );

            assert_de_tokens_error::<AdjacentlyTagged>(
                &[
                    Token::Struct {
                        name: "AdjacentlyTagged",
                        len: 2,
                    },
                    Token::Bytes(b"c"),
                    Token::Unit,
                    Token::Bytes(b"h"),
                ],
                r#"invalid value: byte array, expected "t" or "c""#,
            );
        }
    """.trimIndent() + "\n"
}
