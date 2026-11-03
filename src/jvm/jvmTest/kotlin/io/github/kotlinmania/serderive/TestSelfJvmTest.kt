// port-lint: tests test_suite/tests/test_self.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test
import kotlin.test.assertEquals

class TestSelfJvmTest {
    @Test
    fun testSelf() {
        val support =
            """
            use serde::de::DeserializeOwned;
            use serde::ser::Serialize;

            pub trait Trait {
                type Assoc;
            }

            fn assert_traits<T: Serialize + DeserializeOwned>() {}
            """.trimIndent()

        val cases =
            listOf(
                SelfCompileCase(
                    fixtureName = "test_self_generics",
                    deriveInput =
                        """
                        pub struct Generics<T: Trait<Assoc = Self>>
                        where
                            Self: Trait<Assoc = Self>,
                            <Self as Trait>::Assoc: Sized,
                        {
                            _f: T,
                        }
                        """.trimIndent(),
                    declaration =
                        """
                        pub struct Generics<T: Trait<Assoc = Self>>
                        where
                            Self: Trait<Assoc = Self>,
                            <Self as Trait>::Assoc: Sized,
                        {
                            _f: T,
                        }

                        impl<T: Trait<Assoc = Self>> Trait for Generics<T> {
                            type Assoc = Self;
                        }
                        """.trimIndent(),
                ),
                SelfCompileCase(
                    fixtureName = "test_self_struct",
                    deriveInput =
                        """
                        pub struct Struct {
                            _f1: Box<Self>,
                            _f2: Box<<Self as Trait>::Assoc>,
                            _f4: [(); Self::ASSOC],
                            _f5: [(); Self::assoc()],
                        }
                        """.trimIndent(),
                    declaration =
                        """
                        pub struct Struct {
                            _f1: Box<Self>,
                            _f2: Box<<Self as Trait>::Assoc>,
                            _f4: [(); Self::ASSOC],
                            _f5: [(); Self::assoc()],
                        }

                        impl Struct {
                            const ASSOC: usize = 1;
                            const fn assoc() -> usize { 0 }
                        }

                        impl Trait for Struct {
                            type Assoc = Self;
                        }
                        """.trimIndent(),
                ),
                SelfCompileCase(
                    fixtureName = "test_self_tuple",
                    deriveInput =
                        """
                        pub struct Tuple(
                            Box<Self>,
                            Box<<Self as Trait>::Assoc>,
                            [(); Self::ASSOC],
                            [(); Self::assoc()],
                        );
                        """.trimIndent(),
                    declaration =
                        """
                        pub struct Tuple(
                            Box<Self>,
                            Box<<Self as Trait>::Assoc>,
                            [(); Self::ASSOC],
                            [(); Self::assoc()],
                        );

                        impl Tuple {
                            const ASSOC: usize = 1;
                            const fn assoc() -> usize { 0 }
                        }

                        impl Trait for Tuple {
                            type Assoc = Self;
                        }
                        """.trimIndent(),
                ),
                SelfCompileCase(
                    fixtureName = "test_self_enum",
                    deriveInput =
                        """
                        pub enum Enum {
                            Struct {
                                _f1: Box<Self>,
                                _f2: Box<<Self as Trait>::Assoc>,
                                _f4: [(); Self::ASSOC],
                                _f5: [(); Self::assoc()],
                            },
                            Tuple(
                                Box<Self>,
                                Box<<Self as Trait>::Assoc>,
                                [(); Self::ASSOC],
                                [(); Self::assoc()],
                            ),
                        }
                        """.trimIndent(),
                    declaration =
                        """
                        pub enum Enum {
                            Struct {
                                _f1: Box<Self>,
                                _f2: Box<<Self as Trait>::Assoc>,
                                _f4: [(); Self::ASSOC],
                                _f5: [(); Self::assoc()],
                            },
                            Tuple(
                                Box<Self>,
                                Box<<Self as Trait>::Assoc>,
                                [(); Self::ASSOC],
                                [(); Self::assoc()],
                            ),
                        }

                        impl Enum {
                            const ASSOC: usize = 1;
                            const fn assoc() -> usize { 0 }
                        }

                        impl Trait for Enum {
                            type Assoc = Self;
                        }
                        """.trimIndent(),
                ),
            )

        for (case in cases) {
            val output =
                compileDerives(
                    fixtureName = case.fixtureName,
                    deriveInput = case.deriveInput,
                    declaration = case.declaration,
                    support = support,
                    verify = "",
                )

            assertEquals(0, output.exitCode, output.diagnostics)
        }
    }
}

private data class SelfCompileCase(
    val fixtureName: String,
    val deriveInput: String,
    val declaration: String,
)
