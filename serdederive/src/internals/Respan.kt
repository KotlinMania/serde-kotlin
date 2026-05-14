// port-lint: source serde_derive/src/internals/respan.rs
package io.github.kotlinmania.serde.serdederive.src.internals

import io.github.kotlinmania.procmacro2.Group
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree

internal fun respan(stream: TokenStream, span: Span): TokenStream =
    TokenStream.fromIterable(stream.map { token: TokenTree -> respanToken(token, span) })

private fun respanToken(token: TokenTree, span: Span): TokenTree {
    val respanned =
        when (token) {
            is TokenTree.Group -> {
                val group = token.group
                TokenTree.Group(
                    Group(
                        group.delimiter(),
                        respan(group.stream(), span),
                    ),
                )
            }

            else -> token
        }
    respanned.setSpan(span)
    return respanned
}
