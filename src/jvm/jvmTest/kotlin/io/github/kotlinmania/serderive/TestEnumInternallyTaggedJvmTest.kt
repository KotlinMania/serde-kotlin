// port-lint: tests test_suite/tests/test_enum_internally_tagged.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test

class TestEnumInternallyTaggedJvmTest {
    @Test
    fun unit() = runInternallyTaggedRustTest("unit")

    @Test
    fun newtypeUnit() = runInternallyTaggedRustTest("newtype_unit")

    @Test
    fun newtypeUnitStruct() = runInternallyTaggedRustTest("newtype_unit_struct")

    @Test
    fun newtypeNewtype() = runInternallyTaggedRustTest("newtype_newtype")

    @Test
    fun newtypeMap() = runInternallyTaggedRustTest("newtype_map")

    @Test
    fun newtypeStruct() = runInternallyTaggedRustTest("newtype_struct")

    @Test
    fun newtypeEnumUnit() = runInternallyTaggedRustTest("newtype_enum::unit")

    @Test
    fun newtypeEnumNewtype() = runInternallyTaggedRustTest("newtype_enum::newtype")

    @Test
    fun newtypeEnumTuple() = runInternallyTaggedRustTest("newtype_enum::tuple")

    @Test
    fun newtypeEnumStruct() = runInternallyTaggedRustTest("newtype_enum::struct_")

    @Test
    fun structValue() = runInternallyTaggedRustTest("struct_")

    @Test
    fun structEnumUnit() = runInternallyTaggedRustTest("struct_enum::unit")

    @Test
    fun wrongTag() = runInternallyTaggedRustTest("wrong_tag")

    @Test
    fun untaggedVariant() = runInternallyTaggedRustTest("untagged_variant")

    @Test
    fun stringFromString() = runInternallyTaggedRustTest("string_and_bytes::string_from_string")

    @Test
    fun stringFromBytes() = runInternallyTaggedRustTest("string_and_bytes::string_from_bytes")

    @Test
    fun bytesFromString() = runInternallyTaggedRustTest("string_and_bytes::bytes_from_string")

    @Test
    fun bytesFromBytes() = runInternallyTaggedRustTest("string_and_bytes::bytes_from_bytes")

    @Test
    fun bytesFromSeq() = runInternallyTaggedRustTest("string_and_bytes::bytes_from_seq")

    @Test
    fun borrow() = runInternallyTaggedRustTest("borrow")

    @Test
    fun withSkippedConflict() = runInternallyTaggedRustTest("with_skipped_conflict")

    @Test
    fun containingFlatten() = runInternallyTaggedRustTest("containing_flatten")

    @Test
    fun unitVariantWithUnknownFields() = runInternallyTaggedRustTest("unit_variant_with_unknown_fields")

    @Test
    fun expectingMessage() = runInternallyTaggedRustTest("expecting_message")
}

private val internallyTaggedFixtureSource by lazy(::buildInternallyTaggedFixtureSource)

private fun runInternallyTaggedRustTest(testName: String) {
    runExactSerdeRustTest(
        fixtureName = "test_enum_internally_tagged",
        source = internallyTaggedFixtureSource,
        testName = testName,
    )
}

private fun buildInternallyTaggedFixtureSource(): String {
    val unitType =
        generatedDerives(
            "struct Unit;",
            "#[derive(Debug, PartialEq)] struct Unit;",
        )
    val newtypeType =
        generatedDerives(
            "struct Newtype(BTreeMap<String, String>);",
            "#[derive(Debug, PartialEq)] struct Newtype(BTreeMap<String, String>);",
        )
    val structType =
        generatedDerives(
            "struct Struct { f: u8 }",
            "#[derive(Debug, PartialEq)] struct Struct { f: u8 }",
        )
    val enumType =
        generatedDerives(
            deriveInput =
                """
                enum Enum {
                    Unit,
                    Newtype(u8),
                    Tuple(u8, u8),
                    Struct { f: u8 },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Enum {
                    Unit,
                    Newtype(u8),
                    Tuple(u8, u8),
                    Struct { f: u8 },
                }
                """.trimIndent(),
        )
    val internallyTaggedType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag")]
                enum InternallyTagged {
                    Unit,
                    NewtypeUnit(()),
                    NewtypeUnitStruct(Unit),
                    NewtypeNewtype(Newtype),
                    NewtypeMap(BTreeMap<String, String>),
                    NewtypeStruct(Struct),
                    NewtypeEnum(Enum),
                    Struct { a: u8 },
                    StructEnum { enum_: Enum },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum InternallyTagged {
                    Unit,
                    NewtypeUnit(()),
                    NewtypeUnitStruct(Unit),
                    NewtypeNewtype(Newtype),
                    NewtypeMap(BTreeMap<String, String>),
                    NewtypeStruct(Struct),
                    NewtypeEnum(Enum),
                    Struct { a: u8 },
                    StructEnum { enum_: Enum },
                }
                """.trimIndent(),
        )
    val untaggedVariantType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag")]
                enum InternallyTagged {
                    Tagged { a: u8 },
                    #[serde(untagged)]
                    Untagged { tag: String, b: u8 },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum InternallyTagged {
                    Tagged { a: u8 },
                    Untagged { tag: String, b: u8 },
                }
                """.trimIndent(),
        )
    val stringAndBytesType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag")]
                enum InternallyTagged {
                    String { string: String },
                    Bytes {
                        #[serde(with = "bytes")]
                        bytes: Vec<u8>,
                    },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum InternallyTagged {
                    String { string: String },
                    Bytes { bytes: Vec<u8> },
                }
                """.trimIndent(),
            serialize = false,
        )
    val borrowType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag")]
                enum Input<'a> {
                    Package { name: &'a str },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Input<'a> {
                    Package { name: &'a str },
                }
                """.trimIndent(),
        )
    val skippedConflictType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag")]
                enum Data {
                    A,
                    #[serde(skip)]
                    B { t: String },
                    C {
                        #[serde(default, skip)]
                        t: String,
                    },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Data {
                    A,
                    B { t: String },
                    C { t: String },
                }
                """.trimIndent(),
        )
    val flattenDataType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag")]
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
                #[derive(Debug, PartialEq)]
                enum Data {
                    A { a: i32, flat: Flat },
                }
                """.trimIndent(),
        )
    val flattenFlatType =
        generatedDerives(
            "struct Flat { b: i32 }",
            "#[derive(Debug, PartialEq)] struct Flat { b: i32 }",
        )
    val expectingType =
        generatedDerives(
            deriveInput =
                """
                #[serde(tag = "tag")]
                #[serde(expecting = "something strange...")]
                enum Enum {
                    InternallyTagged,
                }
                """.trimIndent(),
            declaration = "enum Enum { InternallyTagged }",
            serialize = false,
        )

    return """
        #![deny(trivial_numeric_casts)]
        #![allow(dead_code)]

        use serde_test::{assert_de_tokens, assert_de_tokens_error, assert_tokens, Token};
        use std::collections::BTreeMap;
        use std::iter::FromIterator;

        mod bytes {
            use serde::de::{Deserializer, Error, SeqAccess, Visitor};
            use std::fmt;

            pub fn deserialize<'de, D>(deserializer: D) -> Result<Vec<u8>, D::Error>
            where
                D: Deserializer<'de>,
            {
                deserializer.deserialize_byte_buf(ByteBufVisitor)
            }

            struct ByteBufVisitor;

            impl<'de> Visitor<'de> for ByteBufVisitor {
                type Value = Vec<u8>;

                fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
                    formatter.write_str("byte array")
                }

                fn visit_seq<V>(self, mut visitor: V) -> Result<Self::Value, V::Error>
                where
                    V: SeqAccess<'de>,
                {
                    let mut values = Vec::new();
                    while let Some(value) = visitor.next_element()? {
                        values.push(value);
                    }
                    Ok(values)
                }

                fn visit_bytes<E>(self, v: &[u8]) -> Result<Self::Value, E>
                where
                    E: Error,
                {
                    Ok(v.to_vec())
                }

                fn visit_byte_buf<E>(self, v: Vec<u8>) -> Result<Self::Value, E>
                where
                    E: Error,
                {
                    Ok(v)
                }

                fn visit_str<E>(self, v: &str) -> Result<Self::Value, E>
                where
                    E: Error,
                {
                    Ok(v.as_bytes().to_vec())
                }

                fn visit_string<E>(self, v: String) -> Result<Self::Value, E>
                where
                    E: Error,
                {
                    Ok(v.into_bytes())
                }
            }
        }

        $unitType
        $newtypeType
        $structType
        $enumType
        $internallyTaggedType

        #[test]
        fn unit() {
            assert_tokens(
                &InternallyTagged::Unit,
                &[
                    Token::Struct { name: "InternallyTagged", len: 1 },
                    Token::Str("tag"),
                    Token::Str("Unit"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &InternallyTagged::Unit,
                &[
                    Token::Struct { name: "InternallyTagged", len: 1 },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("Unit"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &InternallyTagged::Unit,
                &[
                    Token::Map { len: Some(1) },
                    Token::Str("tag"),
                    Token::Str("Unit"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &InternallyTagged::Unit,
                &[
                    Token::Map { len: Some(1) },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("Unit"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &InternallyTagged::Unit,
                &[Token::Seq { len: Some(1) }, Token::Str("Unit"), Token::SeqEnd],
            );
            assert_de_tokens(
                &InternallyTagged::Unit,
                &[Token::Seq { len: Some(1) }, Token::BorrowedStr("Unit"), Token::SeqEnd],
            );
        }

        #[test]
        fn newtype_unit() {
            let value = InternallyTagged::NewtypeUnit(());
            assert_tokens(
                &value,
                &[
                    Token::Map { len: Some(1) },
                    Token::Str("tag"),
                    Token::Str("NewtypeUnit"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(1) },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeUnit"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct { name: "InternallyTagged", len: 1 },
                    Token::Str("tag"),
                    Token::Str("NewtypeUnit"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct { name: "InternallyTagged", len: 1 },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeUnit"),
                    Token::StructEnd,
                ],
            );
        }

        #[test]
        fn newtype_unit_struct() {
            let value = InternallyTagged::NewtypeUnitStruct(Unit);
            assert_tokens(
                &value,
                &[
                    Token::Map { len: Some(1) },
                    Token::Str("tag"),
                    Token::Str("NewtypeUnitStruct"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(1) },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeUnitStruct"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct { name: "InternallyTagged", len: 1 },
                    Token::Str("tag"),
                    Token::Str("NewtypeUnitStruct"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct { name: "InternallyTagged", len: 1 },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeUnitStruct"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[Token::Seq { len: Some(1) }, Token::Str("NewtypeUnitStruct"), Token::SeqEnd],
            );
            assert_de_tokens(
                &value,
                &[Token::Seq { len: Some(1) }, Token::BorrowedStr("NewtypeUnitStruct"), Token::SeqEnd],
            );
        }

        #[test]
        fn newtype_newtype() {
            assert_tokens(
                &InternallyTagged::NewtypeNewtype(Newtype(BTreeMap::new())),
                &[
                    Token::Map { len: Some(1) },
                    Token::Str("tag"),
                    Token::Str("NewtypeNewtype"),
                    Token::MapEnd,
                ],
            );
        }

        #[test]
        fn newtype_map() {
            let value = InternallyTagged::NewtypeMap(BTreeMap::new());
            assert_tokens(
                &value,
                &[
                    Token::Map { len: Some(1) },
                    Token::Str("tag"),
                    Token::Str("NewtypeMap"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(1) },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeMap"),
                    Token::MapEnd,
                ],
            );

            let value = InternallyTagged::NewtypeMap(BTreeMap::from_iter([(
                "field".to_string(),
                "value".to_string(),
            )]));
            assert_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::Str("tag"),
                    Token::Str("NewtypeMap"),
                    Token::Str("field"),
                    Token::Str("value"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeMap"),
                    Token::BorrowedStr("field"),
                    Token::BorrowedStr("value"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::Str("field"),
                    Token::Str("value"),
                    Token::Str("tag"),
                    Token::Str("NewtypeMap"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::BorrowedStr("field"),
                    Token::BorrowedStr("value"),
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeMap"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens_error::<InternallyTagged>(
                &[
                    Token::Seq { len: Some(2) },
                    Token::Str("NewtypeMap"),
                    Token::Map { len: Some(0) },
                    Token::MapEnd,
                    Token::SeqEnd,
                ],
                "invalid type: sequence, expected a map",
            );
        }

        #[test]
        fn newtype_struct() {
            let value = InternallyTagged::NewtypeStruct(Struct { f: 6 });
            assert_tokens(
                &value,
                &[
                    Token::Struct { name: "Struct", len: 2 },
                    Token::Str("tag"),
                    Token::Str("NewtypeStruct"),
                    Token::Str("f"),
                    Token::U8(6),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct { name: "Struct", len: 2 },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeStruct"),
                    Token::BorrowedStr("f"),
                    Token::U8(6),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct { name: "Struct", len: 2 },
                    Token::Str("f"),
                    Token::U8(6),
                    Token::Str("tag"),
                    Token::Str("NewtypeStruct"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct { name: "Struct", len: 2 },
                    Token::BorrowedStr("f"),
                    Token::U8(6),
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("NewtypeStruct"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Seq { len: Some(2) },
                    Token::Str("NewtypeStruct"),
                    Token::U8(6),
                    Token::SeqEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Seq { len: Some(2) },
                    Token::BorrowedStr("NewtypeStruct"),
                    Token::U8(6),
                    Token::SeqEnd,
                ],
            );
        }

        mod newtype_enum {
            use super::*;

            #[test]
            fn unit() {
                let value = InternallyTagged::NewtypeEnum(Enum::Unit);

                // Special case: tag field ("tag") is the first field
                assert_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::Str("Unit"),
                        Token::Unit,
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                        Token::MapEnd,
                    ],
                );

                // General case: tag field ("tag") is not the first field
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("Unit"),
                        Token::Unit,
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
            }

            #[test]
            fn newtype() {
                let value = InternallyTagged::NewtypeEnum(Enum::Newtype(1));

                // Special case: tag field ("tag") is the first field
                assert_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::Str("Newtype"),
                        Token::U8(1),
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::BorrowedStr("Newtype"),
                        Token::U8(1),
                        Token::MapEnd,
                    ],
                );

                // General case: tag field ("tag") is not the first field
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("Newtype"),
                        Token::U8(1),
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("Newtype"),
                        Token::U8(1),
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
            }

            #[test]
            fn tuple() {
                let value = InternallyTagged::NewtypeEnum(Enum::Tuple(1, 1));

                // Special case: tag field ("tag") is the first field
                assert_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::Str("Tuple"),
                        Token::TupleStruct {
                            name: "Tuple",
                            len: 2,
                        },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleStructEnd,
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::BorrowedStr("Tuple"),
                        Token::TupleStruct {
                            name: "Tuple",
                            len: 2,
                        },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleStructEnd,
                        Token::MapEnd,
                    ],
                );

                // Special case: tag field ("tag") is not the first field
                // Reaches crate::private::de::content::VariantDeserializer::tuple_variant
                // Content::Seq case
                // via ContentDeserializer::deserialize_enum
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("Tuple"),
                        Token::TupleStruct {
                            name: "Tuple",
                            len: 2,
                        },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleStructEnd,
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("Tuple"),
                        Token::TupleStruct {
                            name: "Tuple",
                            len: 2,
                        },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleStructEnd,
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
            }

            #[test]
            fn struct_() {
                let value = InternallyTagged::NewtypeEnum(Enum::Struct { f: 1 });

                // Special case: tag field ("tag") is the first field
                assert_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::Str("Struct"),
                        Token::Struct {
                            name: "Struct",
                            len: 1,
                        },
                        Token::Str("f"),
                        Token::U8(1),
                        Token::StructEnd,
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::BorrowedStr("Struct"),
                        Token::Struct {
                            name: "Struct",
                            len: 1,
                        },
                        Token::BorrowedStr("f"),
                        Token::U8(1),
                        Token::StructEnd,
                        Token::MapEnd,
                    ],
                );

                // General case: tag field ("tag") is not the first field
                // Reaches crate::private::de::content::VariantDeserializer::struct_variant
                // Content::Map case
                // via ContentDeserializer::deserialize_enum
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("Struct"),
                        Token::Struct {
                            name: "Struct",
                            len: 1,
                        },
                        Token::Str("f"),
                        Token::U8(1),
                        Token::StructEnd,
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("Struct"),
                        Token::Struct {
                            name: "Struct",
                            len: 1,
                        },
                        Token::BorrowedStr("f"),
                        Token::U8(1),
                        Token::StructEnd,
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );

                // Special case: tag field ("tag") is the first field
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::Str("Struct"),
                        Token::Seq { len: Some(1) },
                        Token::U8(1), // f
                        Token::SeqEnd,
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::BorrowedStr("Struct"),
                        Token::Seq { len: Some(1) },
                        Token::U8(1), // f
                        Token::SeqEnd,
                        Token::MapEnd,
                    ],
                );

                // General case: tag field ("tag") is not the first field
                // Reaches crate::private::de::content::VariantDeserializer::struct_variant
                // Content::Seq case
                // via ContentDeserializer::deserialize_enum
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("Struct"),
                        Token::Seq { len: Some(1) },
                        Token::U8(1), // f
                        Token::SeqEnd,
                        Token::Str("tag"),
                        Token::Str("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("Struct"),
                        Token::Seq { len: Some(1) },
                        Token::U8(1), // f
                        Token::SeqEnd,
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("NewtypeEnum"),
                        Token::MapEnd,
                    ],
                );
            }
        }

        #[test]
        fn struct_() {
            let value = InternallyTagged::Struct { a: 1 };

            // Special case: tag field ("tag") is the first field
            assert_tokens(
                &value,
                &[
                    Token::Struct {
                        name: "InternallyTagged",
                        len: 2,
                    },
                    Token::Str("tag"),
                    Token::Str("Struct"),
                    Token::Str("a"),
                    Token::U8(1),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct {
                        name: "InternallyTagged",
                        len: 2,
                    },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("Struct"),
                    Token::BorrowedStr("a"),
                    Token::U8(1),
                    Token::StructEnd,
                ],
            );

            // General case: tag field ("tag") is not the first field
            assert_de_tokens(
                &value,
                &[
                    Token::Struct {
                        name: "InternallyTagged",
                        len: 2,
                    },
                    Token::Str("a"),
                    Token::U8(1),
                    Token::Str("tag"),
                    Token::Str("Struct"),
                    Token::StructEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Struct {
                        name: "InternallyTagged",
                        len: 2,
                    },
                    Token::BorrowedStr("a"),
                    Token::U8(1),
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("Struct"),
                    Token::StructEnd,
                ],
            );

            // Special case: tag field ("tag") is the first field
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::Str("tag"),
                    Token::Str("Struct"),
                    Token::Str("a"),
                    Token::U8(1),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("Struct"),
                    Token::BorrowedStr("a"),
                    Token::U8(1),
                    Token::MapEnd,
                ],
            );

            // General case: tag field ("tag") is not the first field
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::Str("a"),
                    Token::U8(1),
                    Token::Str("tag"),
                    Token::Str("Struct"),
                    Token::MapEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: Some(2) },
                    Token::BorrowedStr("a"),
                    Token::U8(1),
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("Struct"),
                    Token::MapEnd,
                ],
            );

            assert_de_tokens(
                &value,
                &[
                    Token::Seq { len: Some(2) },
                    Token::Str("Struct"), // tag
                    Token::U8(1),
                    Token::SeqEnd,
                ],
            );
            assert_de_tokens(
                &value,
                &[
                    Token::Seq { len: Some(2) },
                    Token::BorrowedStr("Struct"), // tag
                    Token::U8(1),
                    Token::SeqEnd,
                ],
            );
        }

        mod struct_enum {
            use super::*;

            #[test]
            fn unit() {
                assert_de_tokens(
                    &Enum::Unit,
                    &[
                        Token::Enum { name: "Enum" },
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                    ],
                );

                let value = InternallyTagged::StructEnum { enum_: Enum::Unit };

                // Special case: tag field ("tag") is the first field
                assert_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "InternallyTagged",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("StructEnum"),
                        Token::Str("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::Str("Unit"),
                        Token::Unit,
                        Token::StructEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "InternallyTagged",
                            len: 2,
                        },
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("StructEnum"),
                        Token::BorrowedStr("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                        Token::StructEnd,
                    ],
                );

                // General case: tag field ("tag") is not the first field
                assert_de_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "InternallyTagged",
                            len: 2,
                        },
                        Token::Str("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::Str("Unit"),
                        Token::Unit,
                        Token::Str("tag"),
                        Token::Str("StructEnum"),
                        Token::StructEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Struct {
                            name: "InternallyTagged",
                            len: 2,
                        },
                        Token::BorrowedStr("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("StructEnum"),
                        Token::StructEnd,
                    ],
                );

                // Special case: tag field ("tag") is the first field
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("tag"),
                        Token::Str("StructEnum"),
                        Token::Str("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::Str("Unit"),
                        Token::Unit,
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("StructEnum"),
                        Token::BorrowedStr("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                        Token::MapEnd,
                    ],
                );

                // General case: tag field ("tag") is not the first field
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::Str("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::Str("Unit"),
                        Token::Unit,
                        Token::Str("tag"),
                        Token::Str("StructEnum"),
                        Token::MapEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Map { len: Some(2) },
                        Token::BorrowedStr("enum_"),
                        Token::Enum { name: "Enum" },
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                        Token::BorrowedStr("tag"),
                        Token::BorrowedStr("StructEnum"),
                        Token::MapEnd,
                    ],
                );

                assert_de_tokens(
                    &value,
                    &[
                        Token::Seq { len: Some(2) },
                        Token::Str("StructEnum"),     // tag
                        Token::Enum { name: "Enum" }, // enum_
                        Token::Str("Unit"),
                        Token::Unit,
                        Token::SeqEnd,
                    ],
                );
                assert_de_tokens(
                    &value,
                    &[
                        Token::Seq { len: Some(2) },
                        Token::BorrowedStr("StructEnum"), // tag
                        Token::Enum { name: "Enum" },     // enum_
                        Token::BorrowedStr("Unit"),
                        Token::Unit,
                        Token::SeqEnd,
                    ],
                );
            }
        }

        #[test]
        fn wrong_tag() {
            assert_de_tokens_error::<InternallyTagged>(
                &[Token::Map { len: Some(0) }, Token::MapEnd],
                "missing field `tag`",
            );

            assert_de_tokens_error::<InternallyTagged>(
                &[
                    Token::Map { len: Some(1) },
                    Token::Str("tag"),
                    Token::Str("Z"),
                    Token::MapEnd,
                ],
                "unknown variant `Z`, expected one of \
                `Unit`, \
                `NewtypeUnit`, \
                `NewtypeUnitStruct`, \
                `NewtypeNewtype`, \
                `NewtypeMap`, \
                `NewtypeStruct`, \
                `NewtypeEnum`, \
                `Struct`, \
                `StructEnum`",
            );
        }

        #[test]
        fn untagged_variant() {
            $untaggedVariantType

            assert_de_tokens(
                &InternallyTagged::Tagged { a: 1 },
                &[
                    Token::Map { len: Some(2) },
                    Token::Str("tag"),
                    Token::Str("Tagged"),
                    Token::Str("a"),
                    Token::U8(1),
                    Token::MapEnd,
                ],
            );

            assert_tokens(
                &InternallyTagged::Tagged { a: 1 },
                &[
                    Token::Struct {
                        name: "InternallyTagged",
                        len: 2,
                    },
                    Token::Str("tag"),
                    Token::Str("Tagged"),
                    Token::Str("a"),
                    Token::U8(1),
                    Token::StructEnd,
                ],
            );

            assert_de_tokens(
                &InternallyTagged::Untagged {
                    tag: "Foo".to_owned(),
                    b: 2,
                },
                &[
                    Token::Map { len: Some(2) },
                    Token::Str("tag"),
                    Token::Str("Foo"),
                    Token::Str("b"),
                    Token::U8(2),
                    Token::MapEnd,
                ],
            );

            assert_tokens(
                &InternallyTagged::Untagged {
                    tag: "Foo".to_owned(),
                    b: 2,
                },
                &[
                    Token::Struct {
                        name: "InternallyTagged",
                        len: 2,
                    },
                    Token::Str("tag"),
                    Token::Str("Foo"),
                    Token::Str("b"),
                    Token::U8(2),
                    Token::StructEnd,
                ],
            );

            assert_tokens(
                &InternallyTagged::Untagged {
                    tag: "Tagged".to_owned(),
                    b: 2,
                },
                &[
                    Token::Struct {
                        name: "InternallyTagged",
                        len: 2,
                    },
                    Token::Str("tag"),
                    Token::Str("Tagged"),
                    Token::Str("b"),
                    Token::U8(2),
                    Token::StructEnd,
                ],
            );
        }

        mod string_and_bytes {
            use super::*;

            $stringAndBytesType

            #[test]
            fn string_from_string() {
                assert_de_tokens(
                    &InternallyTagged::String {
                        string: "\0".to_owned(),
                    },
                    &[
                        Token::Struct {
                            name: "String",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("String"),
                        Token::Str("string"),
                        Token::Str("\0"),
                        Token::StructEnd,
                    ],
                );

                assert_de_tokens(
                    &InternallyTagged::String {
                        string: "\0".to_owned(),
                    },
                    &[
                        Token::Struct {
                            name: "String",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("String"),
                        Token::Str("string"),
                        Token::String("\0"),
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn string_from_bytes() {
                assert_de_tokens(
                    &InternallyTagged::String {
                        string: "\0".to_owned(),
                    },
                    &[
                        Token::Struct {
                            name: "String",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("String"),
                        Token::Str("string"),
                        Token::Bytes(b"\0"),
                        Token::StructEnd,
                    ],
                );

                assert_de_tokens(
                    &InternallyTagged::String {
                        string: "\0".to_owned(),
                    },
                    &[
                        Token::Struct {
                            name: "String",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("String"),
                        Token::Str("string"),
                        Token::ByteBuf(b"\0"),
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn bytes_from_string() {
                assert_de_tokens(
                    &InternallyTagged::Bytes { bytes: vec![0] },
                    &[
                        Token::Struct {
                            name: "Bytes",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("Bytes"),
                        Token::Str("bytes"),
                        Token::Str("\0"),
                        Token::StructEnd,
                    ],
                );

                assert_de_tokens(
                    &InternallyTagged::Bytes { bytes: vec![0] },
                    &[
                        Token::Struct {
                            name: "Bytes",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("Bytes"),
                        Token::Str("bytes"),
                        Token::String("\0"),
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn bytes_from_bytes() {
                assert_de_tokens(
                    &InternallyTagged::Bytes { bytes: vec![0] },
                    &[
                        Token::Struct {
                            name: "Bytes",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("Bytes"),
                        Token::Str("bytes"),
                        Token::Bytes(b"\0"),
                        Token::StructEnd,
                    ],
                );

                assert_de_tokens(
                    &InternallyTagged::Bytes { bytes: vec![0] },
                    &[
                        Token::Struct {
                            name: "Bytes",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("Bytes"),
                        Token::Str("bytes"),
                        Token::ByteBuf(b"\0"),
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn bytes_from_seq() {
                assert_de_tokens(
                    &InternallyTagged::Bytes { bytes: vec![0] },
                    &[
                        Token::Struct {
                            name: "Bytes",
                            len: 2,
                        },
                        Token::Str("tag"),
                        Token::Str("Bytes"),
                        Token::Str("bytes"),
                        Token::Seq { len: Some(1) },
                        Token::U8(0),
                        Token::SeqEnd,
                        Token::StructEnd,
                    ],
                );
            }
        }

        #[test]
        fn borrow() {
            $borrowType

            assert_tokens(
                &Input::Package { name: "borrowed" },
                &[
                    Token::Struct {
                        name: "Input",
                        len: 2,
                    },
                    Token::BorrowedStr("tag"),
                    Token::BorrowedStr("Package"),
                    Token::BorrowedStr("name"),
                    Token::BorrowedStr("borrowed"),
                    Token::StructEnd,
                ],
            );
        }

        #[test]
        fn with_skipped_conflict() {
            $skippedConflictType

            let data = Data::C { t: String::new() };

            assert_tokens(
                &data,
                &[
                    Token::Struct {
                        name: "Data",
                        len: 1,
                    },
                    Token::Str("tag"),
                    Token::Str("C"),
                    Token::StructEnd,
                ],
            );
        }

        #[test]
        fn containing_flatten() {
            $flattenDataType

            $flattenFlatType

            let data = Data::A {
                a: 0,
                flat: Flat { b: 0 },
            };

            assert_tokens(
                &data,
                &[
                    Token::Map { len: None },
                    Token::Str("tag"),
                    Token::Str("A"),
                    Token::Str("a"),
                    Token::I32(0),
                    Token::Str("b"),
                    Token::I32(0),
                    Token::MapEnd,
                ],
            );
        }

        #[test]
        fn unit_variant_with_unknown_fields() {
            let value = InternallyTagged::Unit;

            assert_de_tokens(
                &value,
                &[
                    Token::Map { len: None },
                    Token::Str("tag"),
                    Token::Str("Unit"),
                    Token::Str("b"),
                    Token::I32(0),
                    Token::MapEnd,
                ],
            );

            // Unknown elements are not allowed in sequences
            assert_de_tokens_error::<InternallyTagged>(
                &[
                    Token::Seq { len: None },
                    Token::Str("Unit"), // tag
                    Token::I32(0),
                    Token::SeqEnd,
                ],
                "invalid length 1, expected 0 elements in sequence",
            );
        }

        #[test]
        fn expecting_message() {
            $expectingType

            assert_de_tokens_error::<Enum>(
                &[Token::Str("InternallyTagged")],
                r#"invalid type: string "InternallyTagged", expected something strange..."#,
            );

            // Check that #[serde(expecting = "...")] doesn't affect variant identifier error message
            assert_de_tokens_error::<Enum>(
                &[Token::Map { len: None }, Token::Str("tag"), Token::Unit],
                "invalid type: unit value, expected variant identifier",
            );
        }
    """.trimIndent() + "\n"
}
