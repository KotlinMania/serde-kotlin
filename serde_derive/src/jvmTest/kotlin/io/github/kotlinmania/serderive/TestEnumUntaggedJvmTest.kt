// port-lint: tests test_suite/tests/test_enum_untagged.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestEnumUntaggedJvmTest {
    @Test
    fun complex() = runUntaggedRustTest("complex")

    @Test
    fun newtypeUnitAndEmptyMap() = runUntaggedRustTest("newtype_unit_and_empty_map")

    @Test
    fun newtypeStruct() = runUntaggedRustTest("newtype_struct")

    @Test
    fun newtypeEnumUnit() = runUntaggedRustTest("newtype_enum::unit")

    @Test
    fun newtypeEnumNewtype() = runUntaggedRustTest("newtype_enum::newtype")

    @Test
    fun newtypeEnumTuple0() = runUntaggedRustTest("newtype_enum::tuple0")

    @Test
    fun newtypeEnumTuple2() = runUntaggedRustTest("newtype_enum::tuple2")

    @Test
    fun newtypeEnumStructFromMap() = runUntaggedRustTest("newtype_enum::struct_from_map")

    @Test
    fun newtypeEnumStructFromSeq() = runUntaggedRustTest("newtype_enum::struct_from_seq")

    @Test
    fun newtypeEnumEmptyStructFromMap() = runUntaggedRustTest("newtype_enum::empty_struct_from_map")

    @Test
    fun newtypeEnumEmptyStructFromSeq() = runUntaggedRustTest("newtype_enum::empty_struct_from_seq")

    @Test
    fun withOptionalFieldSome() = runUntaggedRustTest("with_optional_field::some")

    @Test
    fun withOptionalFieldSomeWithoutMarker() = runUntaggedRustTest("with_optional_field::some_without_marker")

    @Test
    fun withOptionalFieldNone() = runUntaggedRustTest("with_optional_field::none")

    @Test
    fun withOptionalFieldUnit() = runUntaggedRustTest("with_optional_field::unit")

    @Test
    fun stringAndBytes() = runUntaggedRustTest("string_and_bytes")

    @Test
    fun containsFlatten() = runUntaggedRustTest("contains_flatten")

    @Test
    fun containsFlattenWithIntegerKey() = runUntaggedRustTest("contains_flatten_with_integer_key")

    @Test
    fun expectingMessage() = runUntaggedRustTest("expecting_message")
}

private val untaggedFixtureLock = Any()
private val untaggedFixtureSource by lazy(::buildUntaggedFixtureSource)

private fun runUntaggedRustTest(testName: String) {
    val output =
        synchronized(untaggedFixtureLock) {
            compileRustFixture(
                fixtureName = "test_enum_untagged",
                source = untaggedFixtureSource,
                extraDependencies = "serde_test = \"=1.0.176\"",
                cargoArguments = listOf("test", "--quiet", testName, "--", "--exact"),
                reuseFixture = true,
                offline = false,
            )
    }
    assertEquals(0, output.exitCode, "Rust test $testName failed:\n${output.diagnostics}")
    assertTrue(
        output.diagnostics.contains("1 passed; 0 failed"),
        "Rust test $testName did not execute exactly one passing test:\n${output.diagnostics}",
    )
}

private fun generatedDerives(
    deriveInput: String,
    declaration: String,
    serialize: Boolean = true,
    deserialize: Boolean = true,
): String =
    buildString {
        appendLine(declaration)
        if (serialize) {
            val tokens = TokenStream.fromString(deriveInput).getOrThrow()
            appendLine(renderRust(deriveSerialize(tokens)))
        }
        if (deserialize) {
            val tokens = TokenStream.fromString(deriveInput).getOrThrow()
            appendLine(renderRust(deriveDeserialize(tokens)))
        }
    }

private fun buildUntaggedFixtureSource(): String {
    val complexType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
                enum Untagged {
                    A { a: u8 },
                    B { b: u8 },
                    C,
                    D(u8),
                    E(String),
                    F(u8, u8),
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Untagged {
                    A { a: u8 },
                    B { b: u8 },
                    C,
                    D(u8),
                    E(String),
                    F(u8, u8),
                }
                """.trimIndent(),
        )
    val unitType =
        generatedDerives(
            "struct Unit;",
            "#[derive(Debug, PartialEq)] struct Unit;",
        )
    val messageType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
                enum Message {
                    Unit(Unit),
                    Map(BTreeMap<String, String>),
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Message {
                    Unit(Unit),
                    Map(BTreeMap<String, String>),
                }
                """.trimIndent(),
        )
    val newtypeStructType =
        generatedDerives(
            "struct NewtypeStruct(u32);",
            "#[derive(Debug, PartialEq)] struct NewtypeStruct(u32);",
        )
    val newtypeOuterType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
                enum Outer {
                    Inner(Inner),
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Outer {
                    Inner(Inner),
                }
                """.trimIndent(),
        )
    val newtypeInnerType =
        generatedDerives(
            deriveInput =
                """
                enum Inner {
                    Unit,
                    Newtype(u8),
                    Tuple0(),
                    Tuple2(u8, u8),
                    Struct { f: u8 },
                    EmptyStruct {},
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Inner {
                    Unit,
                    Newtype(u8),
                    Tuple0(),
                    Tuple2(u8, u8),
                    Struct { f: u8 },
                    EmptyStruct {},
                }
                """.trimIndent(),
        )
    val optionalEnumType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
                enum Enum {
                    Struct { optional: Option<u32> },
                    Null,
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                enum Enum {
                    Struct { optional: Option<u32> },
                    Null,
                }
                """.trimIndent(),
        )
    val stringBytesType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
                enum Untagged {
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
                enum Untagged {
                    String { string: String },
                    Bytes { bytes: Vec<u8> },
                }
                """.trimIndent(),
            serialize = false,
        )
    val flattenDataType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
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
    val flattenType =
        generatedDerives(
            "struct Flat { b: i32 }",
            "#[derive(PartialEq, Debug)] struct Flat { b: i32 }",
        )
    val integerKeyType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
                pub enum Untagged {
                    Variant {
                        #[serde(flatten)]
                        map: BTreeMap<u64, String>,
                    },
                }
                """.trimIndent(),
            declaration =
                """
                #[derive(Debug, PartialEq)]
                pub enum Untagged {
                    Variant { map: BTreeMap<u64, String> },
                }
                """.trimIndent(),
        )
    val expectingType =
        generatedDerives(
            deriveInput =
                """
                #[serde(untagged)]
                #[serde(expecting = "something strange...")]
                enum Enum {
                    Untagged,
                }
                """.trimIndent(),
            declaration = "enum Enum { Untagged }",
            serialize = false,
        )

    return """
        #![deny(trivial_numeric_casts)]
        #![allow(dead_code)]

        use serde_test::{assert_de_tokens, assert_de_tokens_error, assert_tokens, Token};
        use std::collections::BTreeMap;

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

        #[test]
        fn complex() {
            $complexType

            assert_tokens(
                &Untagged::A { a: 1 },
                &[
                    Token::Struct { name: "Untagged", len: 1 },
                    Token::Str("a"),
                    Token::U8(1),
                    Token::StructEnd,
                ],
            );
            assert_tokens(
                &Untagged::B { b: 2 },
                &[
                    Token::Struct { name: "Untagged", len: 1 },
                    Token::Str("b"),
                    Token::U8(2),
                    Token::StructEnd,
                ],
            );
            assert_tokens(&Untagged::C, &[Token::Unit]);
            assert_de_tokens(&Untagged::C, &[Token::None]);
            assert_tokens(&Untagged::D(4), &[Token::U8(4)]);
            assert_tokens(&Untagged::E("e".to_owned()), &[Token::Str("e")]);
            assert_tokens(
                &Untagged::F(1, 2),
                &[
                    Token::Tuple { len: 2 },
                    Token::U8(1),
                    Token::U8(2),
                    Token::TupleEnd,
                ],
            );
            assert_de_tokens_error::<Untagged>(
                &[Token::Tuple { len: 1 }, Token::U8(1), Token::TupleEnd],
                "data did not match any variant of untagged enum Untagged",
            );
            assert_de_tokens_error::<Untagged>(
                &[
                    Token::Tuple { len: 3 },
                    Token::U8(1),
                    Token::U8(2),
                    Token::U8(3),
                    Token::TupleEnd,
                ],
                "data did not match any variant of untagged enum Untagged",
            );
        }

        #[test]
        fn newtype_unit_and_empty_map() {
            $unitType
            $messageType
            assert_tokens(
                &Message::Map(BTreeMap::new()),
                &[Token::Map { len: Some(0) }, Token::MapEnd],
            );
        }

        #[test]
        fn newtype_struct() {
            $newtypeStructType
            ${generatedDerives(
                deriveInput = "#[serde(untagged)] enum E { Newtype(NewtypeStruct), Null }",
                declaration = "#[derive(Debug, PartialEq)] enum E { Newtype(NewtypeStruct), Null }",
            )}
            let value = E::Newtype(NewtypeStruct(5));
            assert_tokens(
                &value,
                &[Token::NewtypeStruct { name: "NewtypeStruct" }, Token::U32(5)],
            );
            assert_de_tokens(&value, &[Token::U32(5)]);
        }

        mod newtype_enum {
            use super::*;

            $newtypeOuterType
            $newtypeInnerType

            #[test]
            fn unit() {
                assert_tokens(
                    &Outer::Inner(Inner::Unit),
                    &[Token::UnitVariant { name: "Inner", variant: "Unit" }],
                );
            }

            #[test]
            fn newtype() {
                assert_tokens(
                    &Outer::Inner(Inner::Newtype(1)),
                    &[
                        Token::NewtypeVariant { name: "Inner", variant: "Newtype" },
                        Token::U8(1),
                    ],
                );
            }

            #[test]
            fn tuple0() {
                assert_tokens(
                    &Outer::Inner(Inner::Tuple0()),
                    &[
                        Token::TupleVariant { name: "Inner", variant: "Tuple0", len: 0 },
                        Token::TupleVariantEnd,
                    ],
                );
            }

            #[test]
            fn tuple2() {
                assert_tokens(
                    &Outer::Inner(Inner::Tuple2(1, 1)),
                    &[
                        Token::TupleVariant { name: "Inner", variant: "Tuple2", len: 2 },
                        Token::U8(1),
                        Token::U8(1),
                        Token::TupleVariantEnd,
                    ],
                );
            }

            #[test]
            fn struct_from_map() {
                assert_tokens(
                    &Outer::Inner(Inner::Struct { f: 1 }),
                    &[
                        Token::StructVariant { name: "Inner", variant: "Struct", len: 1 },
                        Token::Str("f"),
                        Token::U8(1),
                        Token::StructVariantEnd,
                    ],
                );
            }

            #[test]
            fn struct_from_seq() {
                assert_de_tokens(
                    &Outer::Inner(Inner::Struct { f: 1 }),
                    &[
                        Token::Map { len: Some(1) },
                        Token::Str("Struct"),
                        Token::Seq { len: Some(1) },
                        Token::U8(1),
                        Token::SeqEnd,
                        Token::MapEnd,
                    ],
                );
            }

            #[test]
            fn empty_struct_from_map() {
                assert_de_tokens(
                    &Outer::Inner(Inner::EmptyStruct {}),
                    &[
                        Token::Map { len: Some(1) },
                        Token::Str("EmptyStruct"),
                        Token::Map { len: Some(0) },
                        Token::MapEnd,
                        Token::MapEnd,
                    ],
                );
            }

            #[test]
            fn empty_struct_from_seq() {
                assert_de_tokens(
                    &Outer::Inner(Inner::EmptyStruct {}),
                    &[
                        Token::Map { len: Some(1) },
                        Token::Str("EmptyStruct"),
                        Token::Seq { len: Some(0) },
                        Token::SeqEnd,
                        Token::MapEnd,
                    ],
                );
            }
        }

        mod with_optional_field {
            use super::*;

            $optionalEnumType

            #[test]
            fn some() {
                assert_tokens(
                    &Enum::Struct { optional: Some(42) },
                    &[
                        Token::Struct { name: "Enum", len: 1 },
                        Token::Str("optional"),
                        Token::Some,
                        Token::U32(42),
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn some_without_marker() {
                assert_de_tokens(
                    &Enum::Struct { optional: Some(42) },
                    &[
                        Token::Struct { name: "Enum", len: 1 },
                        Token::Str("optional"),
                        Token::U32(42),
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn none() {
                assert_tokens(
                    &Enum::Struct { optional: None },
                    &[
                        Token::Struct { name: "Enum", len: 1 },
                        Token::Str("optional"),
                        Token::None,
                        Token::StructEnd,
                    ],
                );
            }

            #[test]
            fn unit() {
                assert_de_tokens(
                    &Enum::Struct { optional: None },
                    &[
                        Token::Map { len: None },
                        Token::Str("optional"),
                        Token::Unit,
                        Token::MapEnd,
                    ],
                );
            }
        }

        #[test]
        fn string_and_bytes() {
            $stringBytesType

            for token in [Token::Str("\0"), Token::String("\0"), Token::Bytes(b"\0"), Token::ByteBuf(b"\0")] {
                assert_de_tokens(
                    &Untagged::String { string: "\0".to_owned() },
                    &[
                        Token::Struct { name: "Untagged", len: 1 },
                        Token::Str("string"),
                        token,
                        Token::StructEnd,
                    ],
                );
            }
            for token in [Token::Str("\0"), Token::String("\0"), Token::Bytes(b"\0"), Token::ByteBuf(b"\0")] {
                assert_de_tokens(
                    &Untagged::Bytes { bytes: vec![0] },
                    &[
                        Token::Struct { name: "Untagged", len: 1 },
                        Token::Str("bytes"),
                        token,
                        Token::StructEnd,
                    ],
                );
            }
            assert_de_tokens(
                &Untagged::Bytes { bytes: vec![0] },
                &[
                    Token::Struct { name: "Untagged", len: 1 },
                    Token::Str("bytes"),
                    Token::Seq { len: Some(1) },
                    Token::U8(0),
                    Token::SeqEnd,
                    Token::StructEnd,
                ],
            );
        }

        #[test]
        fn contains_flatten() {
            $flattenDataType
            $flattenType
            let data = Data::A { a: 0, flat: Flat { b: 0 } };
            assert_tokens(
                &data,
                &[
                    Token::Map { len: None },
                    Token::Str("a"),
                    Token::I32(0),
                    Token::Str("b"),
                    Token::I32(0),
                    Token::MapEnd,
                ],
            );
        }

        #[test]
        fn contains_flatten_with_integer_key() {
            $integerKeyType
            assert_tokens(
                &Untagged::Variant {
                    map: {
                        let mut map = BTreeMap::new();
                        map.insert(100, "BTreeMap".to_owned());
                        map
                    },
                },
                &[
                    Token::Map { len: None },
                    Token::U64(100),
                    Token::Str("BTreeMap"),
                    Token::MapEnd,
                ],
            );
        }

        #[test]
        fn expecting_message() {
            $expectingType
            assert_de_tokens_error::<Enum>(&[Token::Str("Untagged")], "something strange...");
        }
    """.trimIndent() + "\n"
}
