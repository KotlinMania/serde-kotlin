// port-lint: source pretend.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.formatIdent
import io.github.kotlinmania.serderive.Private
import io.github.kotlinmania.quote.quote

// Suppress unused-code warnings that would otherwise appear when using a remote
// derive. Other than this pretend code, a struct annotated with remote derive
// never has its fields referenced and an enum annotated with remote derive
// never has its variants constructed.
public fun pretendUsed(cont: Container, isPacked: Boolean): TokenStream {
    val pretendFields = pretendFieldsUsed(cont, isPacked)
    val pretendVariants = pretendVariantsUsed(cont)

    return quote("""
        `#`pretendFields
        `#`pretendVariants
    """, mapOf(
        "pretendFields" to pretendFields,
        "pretendVariants" to pretendVariants,
    ))
}

private fun pretendFieldsUsed(cont: Container, isPacked: Boolean): TokenStream {
    return when (val data = cont.data) {
        is Data.Enum -> pretendFieldsUsedEnum(cont, data.variants)
        is Data.Struct -> {
            when (data.style) {
                Style.Struct, Style.Tuple, Style.Newtype -> {
                    if (isPacked) {
                        pretendFieldsUsedStructPacked(cont, data.fields)
                    } else {
                        pretendFieldsUsedStruct(cont, data.fields)
                    }
                }
                Style.Unit -> quote("")
            }
        }
    }
}

private fun pretendFieldsUsedStruct(cont: Container, fields: List<Field>): TokenStream {
    val typeIdent = cont.ident
    val split = cont.generics.splitForImpl()
    val tyGenerics = split.typeGenerics

    val members = fields.map { it.member }
    val placeholders = fields.indices.map { i -> formatIdent("__v$i") }

    return quote("""
        match _serde::`#`Private::None::<&`#`typeIdent `#`tyGenerics> {
            _serde::`#`Private::Some(`#`typeIdent { `#`(`#`members: `#`placeholders),* }) => {}
            _ => {}
        }
    """, mapOf(
        "Private" to Private,
        "typeIdent" to typeIdent,
        "tyGenerics" to tyGenerics,
        "members" to members,
        "placeholders" to placeholders,
    ))
}

private fun pretendFieldsUsedStructPacked(cont: Container, fields: List<Field>): TokenStream {
    val typeIdent = cont.ident
    val split = cont.generics.splitForImpl()
    val tyGenerics = split.typeGenerics

    val members = fields.map { it.member }

    return quote("""
        match _serde::`#`Private::None::<&`#`typeIdent `#`tyGenerics> {
            _serde::`#`Private::Some(__v @ `#`typeIdent { `#`(`#`members: _),* }) => {
                `#`(
                    let _ = _serde::`#`Private::ptr::addr_of!(__v.`#`members);
                )*
            }
            _ => {}
        }
    """, mapOf(
        "Private" to Private,
        "typeIdent" to typeIdent,
        "tyGenerics" to tyGenerics,
        "members" to members,
    ))
}

private fun pretendFieldsUsedEnum(cont: Container, variants: List<Variant>): TokenStream {
    val typeIdent = cont.ident
    val split = cont.generics.splitForImpl()
    val tyGenerics = split.typeGenerics

    val patterns = mutableListOf<TokenStream>()
    for (variant in variants) {
        when (variant.style) {
            Style.Struct, Style.Tuple, Style.Newtype -> {
                val variantIdent = variant.ident
                val members = variant.fields.map { it.member }
                val placeholders = variant.fields.indices.map { i -> formatIdent("__v$i") }
                patterns.add(
                    quote(
                        "`#`typeIdent::`#`variantIdent { `#`(`#`members: `#`placeholders),* }",
                        "typeIdent" to typeIdent,
                        "variantIdent" to variantIdent,
                        "members" to members,
                        "placeholders" to placeholders,
                    ),
                )
            }
            Style.Unit -> {}
        }
    }

    return quote("""
        match _serde::`#`Private::None::<&`#`typeIdent `#`tyGenerics> {
            `#`(
                _serde::`#`Private::Some(`#`patterns) => {}
            )*
            _ => {}
        }
    """, mapOf(
        "Private" to Private,
        "typeIdent" to typeIdent,
        "tyGenerics" to tyGenerics,
        "patterns" to patterns,
    ))
}

private fun pretendVariantsUsed(cont: Container): TokenStream {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return quote("")
    }

    val typeIdent = cont.ident
    val split = cont.generics.splitForImpl()
    val turbofish = split.turbofish

    val cases = variants.map { variant ->
        val variantIdent = variant.ident
        val placeholders = variant.fields.indices.map { i -> formatIdent("__v$i") }

        val pat = when (variant.style) {
            Style.Struct -> {
                val members = variant.fields.map { it.member }
                quote(
                    "{ `#`(`#`members: `#`placeholders),* }",
                    "members" to members,
                    "placeholders" to placeholders,
                )
            }
            Style.Tuple, Style.Newtype -> quote(
                "( `#`(`#`placeholders),* )",
                "placeholders" to placeholders,
            )
            Style.Unit -> quote("")
        }

        quote("""
            match _serde::`#`Private::None {
                _serde::`#`Private::Some((`#`(`#`placeholders,)*)) => {
                    let _ = `#`typeIdent::`#`variantIdent `#`turbofish `#`pat;
                }
                _ => {}
            }
        """, mapOf(
            "Private" to Private,
            "placeholders" to placeholders,
            "typeIdent" to typeIdent,
            "variantIdent" to variantIdent,
            "turbofish" to turbofish,
            "pat" to pat,
        ))
    }

    return quote("`#`(`#`cases)*", "cases" to cases)
}
