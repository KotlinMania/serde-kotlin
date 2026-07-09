package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.serderive.quote
import io.github.kotlinmania.syn.Attribute
import io.github.kotlinmania.syn.Data
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Meta
import io.github.kotlinmania.syn.parseNestedMeta

public fun allowDeprecated(input: DeriveInput): TokenStream? {
    return if (shouldAllowDeprecated(input)) {
        quote(""" `#`[allow(deprecated)] """)
    } else {
        null
    }
}

private fun shouldAllowDeprecated(input: DeriveInput): Boolean {
    if (containsDeprecated(input.attrs)) {
        return true
    }
    when (val data = input.data) {
        is Data.Enum -> {
            for (variant in data.variants.toList()) {
                if (containsDeprecated(variant.attrs)) {
                    return true
                }
            }
        }
        else -> {}
    }
    return false
}

private fun containsDeprecated(attrs: List<Attribute>): Boolean {
    for (attr in attrs) {
        if (attr.path().isIdent("deprecated")) {
            return true
        }
        val meta = attr.meta
        if (meta is Meta.List) {
            if (meta.path.isIdent("allow")) {
                var allowDeprecated = false
                meta.parseNestedMeta { nestedMeta ->
                    if (nestedMeta.path.isIdent("deprecated")) {
                        allowDeprecated = true
                    }
                    io.github.kotlinmania.syn.SynResult.success(Unit)
                }
                if (allowDeprecated) {
                    return true
                }
            }
        }
    }
    return false
}
