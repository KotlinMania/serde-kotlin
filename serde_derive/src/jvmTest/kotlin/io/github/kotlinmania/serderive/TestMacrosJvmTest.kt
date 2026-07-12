// port-lint: tests test_suite/tests/test_macros.rs
package io.github.kotlinmania.serderive

import java.nio.file.Files
import kotlin.test.Test

class TestMacrosJvmTest {
    @Test
    fun testNamedUnit() = runMacrosRustTest("test_named_unit")

    @Test
    fun testSerNamedTuple() = runMacrosRustTest("test_ser_named_tuple")

    @Test
    fun testDeNamedTuple() = runMacrosRustTest("test_de_named_tuple")

    @Test
    fun testSerNamedMap() = runMacrosRustTest("test_ser_named_map")

    @Test
    fun testDeNamedMap() = runMacrosRustTest("test_de_named_map")

    @Test
    fun testSerEnumUnit() = runMacrosRustTest("test_ser_enum_unit")

    @Test
    fun testSerEnumSeq() = runMacrosRustTest("test_ser_enum_seq")

    @Test
    fun testSerEnumMap() = runMacrosRustTest("test_ser_enum_map")

    @Test
    fun testDeEnumUnit() = runMacrosRustTest("test_de_enum_unit")

    @Test
    fun testDeEnumSeq() = runMacrosRustTest("test_de_enum_seq")

    @Test
    fun testDeEnumMap() = runMacrosRustTest("test_de_enum_map")

    @Test
    fun testLifetimes() = runMacrosRustTest("test_lifetimes")

    @Test
    fun testGenericStruct() = runMacrosRustTest("test_generic_struct")

    @Test
    fun testGenericNewtypeStruct() = runMacrosRustTest("test_generic_newtype_struct")

    @Test
    fun testGenericTupleStruct() = runMacrosRustTest("test_generic_tuple_struct")

    @Test
    fun testGenericEnumUnit() = runMacrosRustTest("test_generic_enum_unit")

    @Test
    fun testGenericEnumNewtype() = runMacrosRustTest("test_generic_enum_newtype")

    @Test
    fun testGenericEnumSeq() = runMacrosRustTest("test_generic_enum_seq")

    @Test
    fun testGenericEnumMap() = runMacrosRustTest("test_generic_enum_map")

    @Test
    fun testDefaultTyParam() = runMacrosRustTest("test_default_ty_param")

    @Test
    fun testEnumStateField() = runMacrosRustTest("test_enum_state_field")

    @Test
    fun testInternallyTaggedStruct() = runMacrosRustTest("test_internally_tagged_struct")

    @Test
    fun testInternallyTaggedBracedStructWithZeroFields() =
        runMacrosRustTest("test_internally_tagged_braced_struct_with_zero_fields")

    @Test
    fun testInternallyTaggedStructWithFlattenedField() =
        runMacrosRustTest("test_internally_tagged_struct_with_flattened_field")

    @Test
    fun testRenameAll() = runMacrosRustTest("test_rename_all")

    @Test
    fun testRenameAllFields() = runMacrosRustTest("test_rename_all_fields")

    @Test
    fun testPackedStructCanDeriveSerialize() = runMacrosRustTest("test_packed_struct_can_derive_serialize")
}

private val macrosFixtureSource by lazy(::buildMacrosFixtureSource)

private fun runMacrosRustTest(testName: String) {
    runExactSerdeRustTest(
        fixtureName = "test_macros",
        source = macrosFixtureSource,
        testName = testName,
    )
}

private fun buildMacrosFixtureSource(): String {
    val source =
        Files.readString(
            findRepositoryRoot().resolve("tmp/serde/test_suite/tests/test_macros.rs"),
        )
    val generated = generateRustFixtureFromSerdeDerives(source)
    require(generated.serdeDerivedItems == 27) {
        "Expected 27 serde-derived declarations, transformed ${generated.serdeDerivedItems}"
    }
    return generated.source
}
