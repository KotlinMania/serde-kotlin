// port-lint: source test_suite/tests/test_deprecated.rs
package io.github.kotlinmania.serde.`private`

@Deprecated("deprecated upstream enum")
private enum class DeprecatedEnum {
    A,
    B,
}

@Deprecated("deprecated upstream struct")
private data class DeprecatedStruct(
    val a: Boolean,
)

private enum class DeprecatedVariant {
    A,

    @Deprecated("deprecated upstream variant")
    B,
}

private data class DeprecatedField(
    @Deprecated("deprecated upstream field")
    val a: Boolean,
)
