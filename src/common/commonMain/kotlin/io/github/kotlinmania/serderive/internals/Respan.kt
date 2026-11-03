// port-lint: source internals/respan.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Group
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree

// Re-spans every token in the stream to the given span, recursing into
// groups so that nested content is also re-spanned.
internal fun respan(stream: TokenStream, span: Span): TokenStream {
    val result = TokenStream.new()
    for (token in stream) {
        result.extendTokenTrees(listOf(respanToken(token, span)))
    }
    return result
}

private fun respanToken(token: TokenTree, span: Span): TokenTree {
    if (token is TokenTree.Group) {
        val group = token.value
        val newStream = respan(group.stream(), span)
        val newGroup = Group(group.delimiter(), newStream)
        newGroup.setSpan(span)
        return TokenTree.Group(newGroup)
    }
    return token.setSpan(span)
}
