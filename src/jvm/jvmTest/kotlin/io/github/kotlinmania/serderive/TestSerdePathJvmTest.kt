// port-lint: tests test_suite/tests/test_serde_path.rs
package io.github.kotlinmania.serderive

import kotlin.test.Test
import kotlin.test.assertEquals

class TestSerdePathJvmTest {
    @Test
    fun testGenCustomSerde() {
        val output =
            compileDerives(
                fixtureName = "test_gen_custom_serde",
                deriveInput =
                    """
                    #[serde(crate = "fake_serde")]
                    struct Foo;
                    """.trimIndent(),
                declaration = "struct Foo;",
                support =
                    """
                    pub trait AssertNotSerdeSerialize {}

                    impl<T: serde::Serialize> AssertNotSerdeSerialize for T {}

                    pub trait AssertNotSerdeDeserialize<'a> {}

                    impl<'a, T: serde::Deserialize<'a>> AssertNotSerdeDeserialize<'a> for T {}

                    mod fake_serde {
                        pub use serde::*;

                        pub fn assert<T>()
                        where
                            T: Serialize,
                            T: for<'a> Deserialize<'a>,
                        {
                        }

                        pub trait Serialize {
                            fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error>;
                        }

                        pub trait Deserialize<'a>: Sized {
                            fn deserialize<D: Deserializer<'a>>(deserializer: D) -> Result<Self, D::Error>;
                        }
                    }
                    """.trimIndent(),
                verify =
                    """
                    impl AssertNotSerdeSerialize for Foo {}
                    impl<'a> AssertNotSerdeDeserialize<'a> for Foo {}

                    fn verify() {
                        fake_serde::assert::<Foo>();
                    }
                    """.trimIndent(),
            )

        assertEquals(0, output.exitCode, output.diagnostics)
    }
}
