// port-lint: source lib.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.serderive.de.expandDeriveDeserialize
import io.github.kotlinmania.serderive.ser.expandDeriveSerialize
import io.github.kotlinmania.syn.ParseMacroSynResult
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.parseDeriveInput
import io.github.kotlinmania.syn.parseMacroInput

internal object Private : ToTokens {
    fun ident(): Ident {
        return Ident("__private0", Span.callSite())
    }

    override fun toTokens(out: TokenStream) {
        out.append(ident())
    }
}

public fun deriveSerialize(input: TokenStream): TokenStream {
    val parsed = parseMacroInput(input) { stream ->
        parseDeriveInput(stream)
    }
    return when (parsed) {
        is ParseMacroSynResult.Success -> {
            val result = expandDeriveSerialize(parsed.value)
            result.fold(
                onSuccess = { it },
                onFailure = { error -> error.toCompileError() },
            )
        }
        is ParseMacroSynResult.CompileError -> parsed.tokens
    }
}

public fun deriveDeserialize(input: TokenStream): TokenStream {
    val parsed = parseMacroInput(input) { stream ->
        parseDeriveInput(stream)
    }
    return when (parsed) {
        is ParseMacroSynResult.Success -> {
            val result = expandDeriveDeserialize(parsed.value)
            result.fold(
                onSuccess = { it },
                onFailure = { error -> error.toCompileError() },
            )
        }
        is ParseMacroSynResult.CompileError -> parsed.tokens
    }
}