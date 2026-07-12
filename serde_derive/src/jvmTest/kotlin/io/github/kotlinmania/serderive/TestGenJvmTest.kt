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

    @Test
    fun emptyAndSkippedBracedStructsCompile() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val cases = listOf(
            CompileCase(
                fixtureName = "test_gen_empty_struct",
                deriveInput = "pub struct EmptyStruct {}",
                declaration = "pub struct EmptyStruct {}",
                verifyType = "EmptyStruct",
            ),
            CompileCase(
                fixtureName = "test_gen_empty_braced",
                deriveInput = "pub struct EmptyBraced {}",
                declaration = "pub struct EmptyBraced {}",
                verifyType = "EmptyBraced",
            ),
            CompileCase(
                fixtureName = "test_gen_empty_braced_deny_unknown",
                deriveInput = """
                    #[serde(deny_unknown_fields)]
                    pub struct EmptyBracedDenyUnknown {}
                """.trimIndent(),
                declaration = "pub struct EmptyBracedDenyUnknown {}",
                verifyType = "EmptyBracedDenyUnknown",
            ),
            CompileCase(
                fixtureName = "test_gen_braced_skip_all",
                deriveInput = """
                    pub struct BracedSkipAll {
                        #[serde(skip_deserializing)]
                        f: u8,
                    }
                """.trimIndent(),
                declaration = "pub struct BracedSkipAll { f: u8 }",
                verifyType = "BracedSkipAll",
            ),
            CompileCase(
                fixtureName = "test_gen_braced_skip_all_deny_unknown",
                deriveInput = """
                    #[serde(deny_unknown_fields)]
                    pub struct BracedSkipAllDenyUnknown {
                        #[serde(skip_deserializing)]
                        f: u8,
                    }
                """.trimIndent(),
                declaration = "pub struct BracedSkipAllDenyUnknown { f: u8 }",
                verifyType = "BracedSkipAllDenyUnknown",
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

    @Test
    fun emptyAndSkippedEnumVariantsCompile() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val cases = listOf(
            CompileCase(
                fixtureName = "test_gen_empty_enum",
                deriveInput = "pub enum EmptyEnum {}",
                declaration = "pub enum EmptyEnum {}",
                verifyType = "EmptyEnum",
            ),
            CompileCase(
                fixtureName = "test_gen_empty_enum_deny_unknown",
                deriveInput = """
                    #[serde(deny_unknown_fields)]
                    pub enum EmptyEnumDenyUnknown {}
                """.trimIndent(),
                declaration = "pub enum EmptyEnumDenyUnknown {}",
                verifyType = "EmptyEnumDenyUnknown",
            ),
            CompileCase(
                fixtureName = "test_gen_empty_enum_variant",
                deriveInput = """
                    pub enum EmptyEnumVariant {
                        EmptyStruct {},
                    }
                """.trimIndent(),
                declaration = "pub enum EmptyEnumVariant { EmptyStruct {} }",
                verifyType = "EmptyEnumVariant",
            ),
            CompileCase(
                fixtureName = "test_gen_empty_variants",
                deriveInput = """
                    pub enum EmptyVariants {
                        Braced {},
                        Tuple(),
                        BracedSkip {
                            #[serde(skip_deserializing)]
                            f: u8,
                        },
                        TupleSkip(#[serde(skip_deserializing)] u8),
                    }
                """.trimIndent(),
                declaration = "pub enum EmptyVariants { Braced {}, Tuple(), BracedSkip { f: u8 }, TupleSkip(u8) }",
                verifyType = "EmptyVariants",
            ),
            CompileCase(
                fixtureName = "test_gen_empty_variants_deny_unknown",
                deriveInput = """
                    #[serde(deny_unknown_fields)]
                    pub enum EmptyVariantsDenyUnknown {
                        Braced {},
                        Tuple(),
                        BracedSkip {
                            #[serde(skip_deserializing)]
                            f: u8,
                        },
                        TupleSkip(#[serde(skip_deserializing)] u8),
                    }
                """.trimIndent(),
                declaration = "pub enum EmptyVariantsDenyUnknown { Braced {}, Tuple(), BracedSkip { f: u8 }, TupleSkip(u8) }",
                verifyType = "EmptyVariantsDenyUnknown",
            ),
            CompileCase(
                fixtureName = "test_gen_unit_deny_unknown",
                deriveInput = """
                    #[serde(deny_unknown_fields)]
                    pub struct UnitDenyUnknown;
                """.trimIndent(),
                declaration = "pub struct UnitDenyUnknown;",
                verifyType = "UnitDenyUnknown",
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

    @Test
    fun phantomDataAndNoBoundsCompile() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;
            use std::marker::PhantomData;
            use std::option::Option as StdOption;
            use std::boxed::Box;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val extraSupport = """
            $support

            pub struct X;
        """.trimIndent()

        // PhantomX is not generic; PhantomT and NoBounds are generic.
        val phantomXOutput = compileDerives(
            fixtureName = "test_gen_phantom_x",
            deriveInput = "struct PhantomX { x: PhantomData<X> }",
            declaration = "struct PhantomX { x: PhantomData<X> }",
            support = extraSupport,
            verify = "fn verify() { assert_traits::<PhantomX>(); }",
        )
        assertEquals(0, phantomXOutput.exitCode, phantomXOutput.diagnostics)

        val phantomTOutput = compileDerives(
            fixtureName = "test_gen_phantom_t",
            deriveInput = "struct PhantomT<T> { t: PhantomData<T> }",
            declaration = "struct PhantomT<T> { t: PhantomData<T> }",
            support = extraSupport,
            verify = "fn verify() { assert_traits::<PhantomT<X>>(); }",
        )
        assertEquals(0, phantomTOutput.exitCode, phantomTOutput.diagnostics)

        val noBoundsOutput = compileDerives(
            fixtureName = "test_gen_no_bounds",
            deriveInput = """
                struct NoBounds<T> {
                    t: T,
                    option: StdOption<T>,
                    boxed: Box<T>,
                    option_boxed: StdOption<Box<T>>,
                }
            """.trimIndent(),
            declaration = "struct NoBounds<T> { t: T, option: StdOption<T>, boxed: Box<T>, option_boxed: StdOption<Box<T>> }",
            support = extraSupport,
            verify = "fn verify() { assert_traits::<NoBounds<i32>>(); }",
        )
        assertEquals(0, noBoundsOutput.exitCode, noBoundsOutput.diagnostics)
    }

    @Test
    fun recursiveTypesCompile() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;
            use std::boxed::Box;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val deriveInput = """
            enum TreeNode<D> {
                Split {
                    left: Box<TreeNode<D>>,
                    right: Box<TreeNode<D>>,
                },
                Leaf {
                    data: D,
                },
            }
        """.trimIndent()

        val declaration = deriveInput

        val output = compileDerives(
            fixtureName = "test_gen_tree_node",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<TreeNode<i32>>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun newtypeAndTupleWithCustomFunctionsCompile() {
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

        val newtypeOutput = compileDerives(
            fixtureName = "test_gen_newtype",
            deriveInput = "struct Newtype(#[serde(serialize_with = \"ser_x\", deserialize_with = \"de_x\")] X);",
            declaration = "struct Newtype(X);",
            support = support,
            verify = "fn verify() { assert_traits::<Newtype>(); }",
        )
        assertEquals(0, newtypeOutput.exitCode, newtypeOutput.diagnostics)

        val tupleOutput = compileDerives(
            fixtureName = "test_gen_tuple",
            deriveInput = """
                struct Tuple<T>(
                    T,
                    #[serde(serialize_with = "ser_x", deserialize_with = "de_x")] X,
                );
            """.trimIndent(),
            declaration = "struct Tuple<T>(T, X);",
            support = support,
            verify = "fn verify() { assert_traits::<Tuple<i32>>(); }",
        )
        assertEquals(0, tupleOutput.exitCode, tupleOutput.diagnostics)
    }

    @Test
    fun enumWithCustomFunctionsCompile() {
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

        val deriveInput = """
            enum EnumWith<T> {
                Unit,
                Newtype(#[serde(serialize_with = "ser_x", deserialize_with = "de_x")] X),
                Tuple(
                    T,
                    #[serde(serialize_with = "ser_x", deserialize_with = "de_x")] X,
                ),
                Struct {
                    t: T,
                    #[serde(serialize_with = "ser_x", deserialize_with = "de_x")]
                    x: X,
                },
            }
        """.trimIndent()

        val declaration = """
            enum EnumWith<T> {
                Unit,
                Newtype(X),
                Tuple(T, X),
                Struct {
                    t: T,
                    x: X,
                },
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_enum_with",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<EnumWith<i32>>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun adjacentlyTaggedVariantWithCustomFunctionsDeserializeCompiles() {
        val support =
            """
            use serde::de::{DeserializeOwned, Deserializer};
            use std::result::Result as StdResult;

            pub struct X;

            pub fn de_x<'de, D: Deserializer<'de>>(_: D) -> StdResult<X, D::Error> {
                unimplemented!()
            }

            pub fn deserialize_some_unit_variant<'de, D: Deserializer<'de>>(_: D) -> StdResult<(), D::Error> {
                unimplemented!()
            }

            pub fn deserialize_some_other_variant<'de, D: Deserializer<'de>>(_: D) -> StdResult<(String, u8), D::Error> {
                unimplemented!()
            }

            fn assert_de<T: DeserializeOwned>() {}
            """.trimIndent()

        val deriveInput =
            """
            #[serde(tag = "t", content = "c")]
            enum AdjacentlyTaggedVariantWith {
                #[serde(deserialize_with = "de_x")]
                Newtype(X),
                #[serde(deserialize_with = "deserialize_some_other_variant")]
                Tuple(String, u8),
                #[serde(deserialize_with = "de_x")]
                Struct1 { x: X },
                #[serde(deserialize_with = "deserialize_some_other_variant")]
                Struct { f1: String, f2: u8 },
                #[serde(deserialize_with = "deserialize_some_unit_variant")]
                Unit,
            }
            """.trimIndent()

        val declaration =
            """
            enum AdjacentlyTaggedVariantWith {
                Newtype(X),
                Tuple(String, u8),
                Struct1 { x: X },
                Struct { f1: String, f2: u8 },
                Unit,
            }
            """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_adjacently_tagged_variant_with_deserialize",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_de::<AdjacentlyTaggedVariantWith>(); }",
            generateSerialize = false,
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun remoteDefinitionsCompile() {
        val support = "use serde::de::Deserialize; use serde::ser::Serialize;"

        val enumOutput = compileDerives(
            fixtureName = "test_gen_remote_or",
            deriveInput =
                """
                #[serde(untagged, remote = "Or")]
                pub enum OrDef<A, B> {
                    A(A),
                    B(B),
                }
                """.trimIndent(),
            declaration =
                """
                pub enum Or<A, B> { A(A), B(B) }
                pub enum OrDef<A, B> { A(A), B(B) }
                """.trimIndent(),
            support = support,
            verify = "",
        )
        assertEquals(0, enumOutput.exitCode, enumOutput.diagnostics)

        val strOutput = compileDerives(
            fixtureName = "test_gen_remote_str",
            deriveInput =
                """
                #[serde(remote = "Str")]
                struct StrDef<'a>(&'a str);
                """.trimIndent(),
            declaration =
                """
                struct Str<'a>(&'a str);
                struct StrDef<'a>(&'a str);
                """.trimIndent(),
            support = support,
            verify = "",
        )
        assertEquals(0, strOutput.exitCode, strOutput.diagnostics)
    }

    @Test
    fun remotePackedSerializeCompiles() {
        val output = compileDerives(
            fixtureName = "test_gen_remote_packed",
            deriveInput =
                """
                #[repr(C, packed)]
                #[serde(remote = "RemotePacked")]
                pub struct RemotePackedDef {
                    a: u16,
                    b: u32,
                }
                """.trimIndent(),
            declaration =
                """
                #[repr(C, packed)]
                pub struct RemotePacked {
                    pub a: u16,
                    pub b: u32,
                }

                #[repr(C, packed)]
                pub struct RemotePackedDef {
                    pub a: u16,
                    pub b: u32,
                }
                """.trimIndent(),
            support = "use serde::ser::Serialize;",
            verify = "",
            generateDeserialize = false,
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun flattenWithSerializeCompiles() {
        val support =
            """
            use serde::ser::{Serialize, Serializer};
            use std::result::Result as StdResult;

            pub struct X;

            pub fn ser_x<S: Serializer>(_: &X, _: S) -> StdResult<S::Ok, S::Error> {
                unimplemented!()
            }
            """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_flatten_with_serialize",
            deriveInput =
                """
                struct FlattenWith {
                    #[serde(flatten, serialize_with = "ser_x")]
                    x: X,
                }
                """.trimIndent(),
            declaration = "struct FlattenWith { x: X }",
            support = support,
            verify = "fn verify() { fn assert_ser<T: Serialize>() {} assert_ser::<FlattenWith>(); }",
            generateDeserialize = false,
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun skippedStaticStrCompiles() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val deriveInput = """
            struct SkippedStaticStr {
                #[serde(skip_deserializing)]
                skipped: &'static str,
                other: isize,
            }
        """.trimIndent()

        val declaration = """
            struct SkippedStaticStr {
                skipped: &'static str,
                other: isize,
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_skipped_static_str",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<SkippedStaticStr>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun transparentWithCustomFunctionsCompiles() {
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

        val deriveInput = """
            #[serde(transparent)]
            #[allow(dead_code)]
            pub struct TransparentWith {
                #[serde(serialize_with = "ser_x")]
                #[serde(deserialize_with = "de_x")]
                x: X,
            }
        """.trimIndent()

        val declaration = """
            pub struct TransparentWith {
                x: X,
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_transparent_with",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<TransparentWith>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun skippedVariantCompiles() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;

            pub struct X;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val deriveInput = """
            enum SkippedVariant<T> {
                #[serde(skip)]
                #[allow(dead_code)]
                T(T),
                Unit,
            }
        """.trimIndent()

        val declaration = """
            enum SkippedVariant<T> {
                T(T),
                Unit,
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_skipped_variant",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<SkippedVariant<X>>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun emptyArrayStructCompiles() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;

            pub struct X;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val deriveInput = """
            pub struct EmptyArray {
                empty: [X; 0],
            }
        """.trimIndent()

        val declaration = """
            pub struct EmptyArray {
                empty: [X; 0],
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_empty_array",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<EmptyArray>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun enumSkipAllCompiles() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned};
            use serde::ser::Serialize;

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val deriveInput = """
            pub enum EnumSkipAll {
                #[serde(skip_deserializing)]
                #[allow(dead_code)]
                Variant,
            }
        """.trimIndent()

        val declaration = """
            pub enum EnumSkipAll {
                Variant,
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_enum_skip_all",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<EnumSkipAll>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun flattenWithCompiles() {
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

        val deriveInput = """
            struct FlattenWith {
                #[serde(flatten, serialize_with = "ser_x", deserialize_with = "de_x")]
                x: X,
            }
        """.trimIndent()

        val declaration = """
            struct FlattenWith {
                x: X,
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_flatten_with",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<FlattenWith>(); }",
        )

        assertEquals(0, output.exitCode, output.diagnostics)
    }

    @Test
    fun internallyTaggedDeserializeWithGenericCompiles() {
        val support =
            """
            use serde::de::{Deserialize, DeserializeOwned, Deserializer};
            use std::result::Result as StdResult;

            fn deserialize_generic<'de, T, D>(deserializer: D) -> StdResult<T, D::Error>
            where
                T: Deserialize<'de>,
                D: Deserializer<'de>,
            {
                T::deserialize(deserializer)
            }

            fn assert_traits<T: DeserializeOwned>() {}
            """.trimIndent()

        val deriveInput = """
            #[serde(tag = "tag")]
            pub enum InternallyTagged {
                #[serde(deserialize_with = "deserialize_generic")]
                Unit,

                #[serde(deserialize_with = "deserialize_generic")]
                Newtype(i32),

                #[serde(deserialize_with = "deserialize_generic")]
                Struct { f1: String, f2: u8 },
            }
        """.trimIndent()

        val declaration = """
            pub enum InternallyTagged {
                Unit,
                Newtype(i32),
                Struct { f1: String, f2: u8 },
            }
        """.trimIndent()

        val output = compileDerives(
            fixtureName = "test_gen_internally_tagged_deserialize_with_generic",
            deriveInput = deriveInput,
            declaration = declaration,
            support = support,
            verify = "fn verify() { assert_traits::<InternallyTagged>(); }",
            generateSerialize = false,
        )

        assertEquals(0, output.exitCode, output.diagnostics)
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
    generateSerialize: Boolean = true,
    generateDeserialize: Boolean = true,
): CargoOutput {
    val root = findRepositoryRoot()
    val fixture = root.resolve("build/rust-compile-tests/$fixtureName")
    fixture.toFile().deleteRecursively()
    Files.createDirectories(fixture.resolve("src"))

    val serialize = if (generateSerialize) {
        renderRust(deriveSerialize(TokenStream.fromString(deriveInput).getOrThrow()))
    } else {
        ""
    }
    val deserialize = if (generateDeserialize) {
        renderRust(deriveDeserialize(TokenStream.fromString(deriveInput).getOrThrow()))
    } else {
        ""
    }

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
