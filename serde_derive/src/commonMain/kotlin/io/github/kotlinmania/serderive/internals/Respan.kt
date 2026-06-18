package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream

internal fun respan(stream: TokenStream, span: Span): TokenStream {
    return stream
}

