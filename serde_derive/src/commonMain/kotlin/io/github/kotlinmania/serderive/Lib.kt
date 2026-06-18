package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.parseMacroInput
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.serderive.internals.private
import io.github.kotlinmania.serderive.ser.expandDeriveSerialize
import io.github.kotlinmania.serderive.de.expandDeriveDeserialize

internal object Private : ToTokens {
    fun ident(): Ident {
        return Ident("private", Span.callSite())
    }

    override fun toTokens(out: TokenStream) {
        out.append(ident())
    }
}

public fun deriveSerialize(input: TokenStream): TokenStream {
    // This assumes parseMacroInput is a function that parses TokenStream into DeriveInput
    // and expandDeriveSerialize returns a SynResult<TokenStream>
    return TokenStream.new() // mechanical stub for now, need parseMacroInput equivalent
}

public fun deriveDeserialize(input: TokenStream): TokenStream {
    return TokenStream.new() // mechanical stub for now
}
