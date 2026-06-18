package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.formatIdent
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.Path

public fun pretendUsed(cont: Container, isPacked: Boolean): TokenStream {
    val pretendFields = pretendFieldsUsed(cont, isPacked)
    val pretendVariants = pretendVariantsUsed(cont)

    return quote("""
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
                Style.Unit -> quote("""""")
            }
        }
    }
}

private fun pretendFieldsUsedStruct(cont: Container, fields: List<Field>): TokenStream {
    val typeIdent = cont.ident
    val (_, tyGenerics, _) = cont.generics.splitForImpl()

    val members = fields.map { it.member }
    val placeholders = fields.indices.map { i -> formatIdent("__v`#`i") }

    return quote("""
        when (_serde.`#`private.null::<&`#`typeIdent `#`tyGenerics> ) {
            _serde.`#`private.`#`typeIdent { `#`(`#`members: `#`placeholders,* }) -> {}
            _ -> {}
        }
    """)
}

private fun pretendFieldsUsedStructPacked(cont: Container, fields: List<Field>): TokenStream {
    val typeIdent = cont.ident
    val (_, tyGenerics, _) = cont.generics.splitForImpl()

    val members = fields.map { it.member }
    val private2 = private

    return quote("""
        when (_serde.`#`private.null::<&`#`typeIdent `#`tyGenerics> ) {
            _serde.`#`private.__v @ `#`typeIdent { `#`(`#`members: _,* }) -> {
                `#`(
                    let _ = _serde.`#`private2.ptr.addr_of!(__v.`#`members);
                )*
            }
            _ -> {}
        }
    """)
}

private fun pretendFieldsUsedEnum(cont: Container, variants: List<Variant>): TokenStream {
    val typeIdent = cont.ident
    val (_, tyGenerics, _) = cont.generics.splitForImpl()

    val patterns = mutableListOf<TokenStream>()
    for (variant in variants) {
        when (variant.style) {
            Style.Struct, Style.Tuple, Style.Newtype -> {
                val variantIdent = variant.ident
                val members = variant.fields.map { it.member }
                val placeholders = variant.fields.indices.map { i -> formatIdent("__v`#`i") }
                patterns.add(quote! { `#`typeIdent::`#`variantIdent { `#`(`#`members: `#`placeholders),* } })
            }
            Style.Unit -> {}
        }
    }

    val private2 = private
    return quote("""
        when (_serde.`#`private.null::<&`#`typeIdent `#`tyGenerics> ) {
            `#`(
                _serde.`#`private2.`#`patterns -> {}
            )*
            _ -> {}
        }
    """)
}

private fun pretendVariantsUsed(cont: Container): TokenStream {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return quote("""""")
    }

    val typeIdent = cont.ident
    val (_, tyGenerics, _) = cont.generics.splitForImpl()
    val turbofish = tyGenerics.asTurbofish()

    val cases = variants.map { variant ->
        val variantIdent = variant.ident
        val placeholders = variant.fields.indices.map { i -> formatIdent("__v`#`i") }

        val pat = when (variant.style) {
            Style.Struct -> {
                val members = variant.fields.map { it.member }
                quote! { { `#`(`#`members: `#`placeholders),* } }
            }
            Style.Tuple, Style.Newtype -> quote! { ( `#`(`#`placeholders),* ) }
            Style.Unit -> quote! {}
        }

        quote! {
            when (_serde.`#`private.null ) {
                _serde.`#`private.(`#`(`#`placeholders,*)) -> {
                    let _ = `#`typeIdent::`#`variantIdent `#`turbofish `#`pat;
                }
                _ -> {}
            }
        }
    }

    return quote! { `#`(`#`cases)* }
}
