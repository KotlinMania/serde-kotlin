// port-lint: source serde_derive/src/deprecated.rs
package io.github.kotlinmania.serde.serdederive.src

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.syn.Attribute
import io.github.kotlinmania.syn.Data
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Meta

public fun allowDeprecated(input: DeriveInput): TokenStream? =
    if (shouldAllowDeprecated(input)) {
        TokenStream.fromString("#[allow(deprecated)]").getOrThrow()
    } else {
        null
    }

/**
 * Determine if an allow-deprecated attribute should be added to the derived implementation.
 *
 * This should happen if the derive input or an enum variant it contains has one of:
 * - deprecated
 * - allow(deprecated)
 */
private fun shouldAllowDeprecated(input: DeriveInput): Boolean {
    if (containsDeprecated(input.attrs)) {
        return true
    }
    val data = input.data
    if (data is Data.Enum) {
        for (variant in data.variants) {
            if (containsDeprecated(variant.attrs)) {
                return true
            }
        }
    }
    return false
}

/**
 * Check whether the given attributes contain one of:
 * - deprecated
 * - allow(deprecated)
 */
private fun containsDeprecated(attrs: List<Attribute>): Boolean {
    for (attr in attrs) {
        if (attr.path().isIdent("deprecated")) {
            return true
        }
        val meta = attr.meta
        if (meta is Meta.List && meta.path.isIdent("allow")) {
            var allowDeprecated = false
            meta.parseNestedMeta { nested ->
                if (nested.path.isIdent("deprecated")) {
                    allowDeprecated = true
                }
                Result.success(Unit)
            }
            if (allowDeprecated) {
                return true
            }
        }
    }
    return false
}
