// port-lint: source serde_derive/src/bound.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.GenericParam
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.WhereClause

private fun ToTokens.generatedString(): String =
    toTokenStream().toString()

private fun tokensFromRust(text: String): TokenStream =
    TokenStream.fromString(text).getOrThrow()

private fun renderImplParam(param: GenericParam): String =
    when (param) {
        is GenericParam.LifetimeParam -> buildString {
            append(param.lifetime)
            if (!param.bounds.isEmpty()) {
                append(": ")
                append(param.bounds.generatedString())
            }
        }
        is GenericParam.TypeParam -> buildString {
            append(param.ident)
            if (!param.bounds.isEmpty()) {
                append(": ")
                append(param.bounds.generatedString())
            }
        }
        is GenericParam.ConstParam -> buildString {
            append("const ")
            append(param.ident)
            append(": ")
            append(param.ty.generatedString())
        }
    }

private fun renderTypeParam(param: GenericParam): String =
    when (param) {
        is GenericParam.LifetimeParam -> param.lifetime.toString()
        is GenericParam.TypeParam -> param.ident.toString()
        is GenericParam.ConstParam -> param.ident.toString()
    }

private fun renderGenerics(params: List<GenericParam>, render: (GenericParam) -> String): String {
    if (params.isEmpty()) return ""
    return params.joinToString(prefix = "<", postfix = ">", transform = render)
}

internal fun generatedImplGenerics(generics: Generics): TokenStream =
    tokensFromRust(renderGenerics(generics.params.toList(), ::renderImplParam))

internal fun generatedTypeGenerics(generics: Generics): TokenStream =
    tokensFromRust(renderGenerics(generics.params.toList(), ::renderTypeParam))

internal fun generatedWhereClause(whereClause: WhereClause?): TokenStream =
    if (whereClause == null || whereClause.predicates.isEmpty()) {
        TokenStream.new()
    } else {
        tokensFromRust(whereClause.generatedString())
    }
