// port-lint: source pretend.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.formatIdent
import io.github.kotlinmania.serderive.checkedQuote

// Suppress dead_code warnings that would otherwise appear when using a remote
// derive. Other than this pretend code, a struct annotated with remote derive
// never has its fields referenced and an enum annotated with remote derive
// never has its variants constructed.
public fun pretendUsed(cont: Container, isPacked: Boolean): TokenStream {
    val pretendFields = pretendFieldsUsed(cont, isPacked)
    val pretendVariants = pretendVariantsUsed(cont)

    return checkedQuote("""
        `#`pretendFields
        `#`pretendVariants
    """)
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
                Style.Unit -> checkedQuote("")
            }
        }
    }
}

private fun pretendFieldsUsedStruct(cont: Container, fields: List<Field>): TokenStream {
    val typeIdent = cont.ident
    val split = cont.generics.splitForImpl()

    val members = fields.map { it.member }
    val placeholders = fields.indices.map { i -> formatIdent("__v$i") }

    return checkedQuote("""
        when (_serde::`#`Private::None::<&`#`typeIdent `#`tyGenerics> ) {
            _serde::`#`Private::Some(`#`typeIdent { `#`(`#`members: `#`placeholders),* }) -> {}
            _ => {}
        }
    """)
}

private fun pretendFieldsUsedStructPacked(cont: Container, fields: List<Field>): TokenStream {
    val typeIdent = cont.ident
    val split = cont.generics.splitForImpl()

    val members = fields.map { it.member }

    return checkedQuote("""
        when (_serde::`#`Private::None::<&`#`typeIdent `#`tyGenerics> ) {
            _serde::`#`Private::Some(__v @ `#`typeIdent { `#`(`#`members: _),* }) => {
                `#`(
                    let _ = _serde::`#`Private::ptr.addr_of!(__v.`#`members);
                )*
            }
            _ => {}
        }
    """)
}

private fun pretendFieldsUsedEnum(cont: Container, variants: List<Variant>): TokenStream {
    val typeIdent = cont.ident
    val split = cont.generics.splitForImpl()

    val patterns = mutableListOf<TokenStream>()
    for (variant in variants) {
        when (variant.style) {
            Style.Struct, Style.Tuple, Style.Newtype -> {
                val variantIdent = variant.ident
                val members = variant.fields.map { it.member }
                val placeholders = variant.fields.indices.map { i -> formatIdent("__v$i") }
                patterns.add(checkedQuote("`#`typeIdent::`#`variantIdent { `#`(`#`members: `#`placeholders),* }"))
            }
            Style.Unit -> {}
        }
    }

    return checkedQuote("""
        when (_serde::`#`Private::None::<&`#`typeIdent `#`tyGenerics> ) {
            `#`(
                _serde::`#`Private::Some(`#`patterns) => {}
            )*
            _ => {}
        }
    """)
}

private fun pretendVariantsUsed(cont: Container): TokenStream {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return checkedQuote("")
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
                checkedQuote("{ `#`(`#`members: `#`placeholders),* }")
            }
            Style.Tuple, Style.Newtype -> checkedQuote("( `#`(`#`placeholders),* )")
            Style.Unit -> checkedQuote("")
        }

        checkedQuote("""
            when (_serde::`#`Private::None ) {
                _serde::`#`Private::Some((`#`(`#`placeholders,)*)) => {
                    let _ = `#`typeIdent::`#`variantIdent `#`turbofish `#`pat;
                }
                _ => {}
            }
        """)
    }

    return checkedQuote("`#`(`#`cases)*")
}