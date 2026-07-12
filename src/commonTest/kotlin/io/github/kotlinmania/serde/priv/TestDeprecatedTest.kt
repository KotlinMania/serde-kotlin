// port-lint: tests test_suite/tests/test_deprecated.rs
package io.github.kotlinmania.serde.priv

@Deprecated("deprecated upstream enum")
internal enum class DeprecatedEnum {
    A,
    B,
}

@Deprecated("deprecated upstream struct")
internal data class DeprecatedStruct(
    val a: Boolean,
)

internal enum class DeprecatedVariant {
    A,

    @Deprecated("deprecated upstream variant")
    B,
}

internal data class DeprecatedField(
    @Deprecated("deprecated upstream field")
    val a: Boolean,
)
