// port-lint: source lib.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.append
import io.github.kotlinmania.syn.DeriveInputParse
import io.github.kotlinmania.syn.ParseMacroSynResult
import io.github.kotlinmania.syn.parseMacroInput

// The private sentinel type used in generated code to access serde's
// private API surface. Maps to the private sentinel in upstream.
internal object Private : ToTokens {
    fun ident(): Ident = Ident.new("__private228", Span.callSite())

    override fun toTokens(tokens: TokenStream) {
        tokens.append(ident())
    }
}

internal fun rustUsizeLiteral(value: Int): TokenStream =
    TokenStream.fromString("${value}usize").getOrThrow()

internal fun rustU64Literal(value: Int): TokenStream =
    TokenStream.fromString("${value}u64").getOrThrow()

internal fun rustUnsuffixedLiteral(value: UInt): TokenStream =
    TokenStream.fromString(value.toLong().toString()).getOrThrow()

// Entry point for the Serialize derive. Parses the token stream into a
// DeriveInput, delegates to expandDeriveSerialize, and converts errors to
// compile errors.
public fun deriveSerialize(input: TokenStream): TokenStream {
    val parsed = parseMacroInput(input, DeriveInputParse::parse)
    return when (parsed) {
        is ParseMacroSynResult.Success -> expandDeriveSerialize(parsed.value)
        is ParseMacroSynResult.CompileError -> parsed.tokens
    }
}

// Entry point for the Deserialize derive. Parses the token stream into a
// DeriveInput, delegates to expandDeriveDeserialize, and converts errors to
// compile errors.
public fun deriveDeserialize(input: TokenStream): TokenStream {
    val parsed = parseMacroInput(input, DeriveInputParse::parse)
    return when (parsed) {
        is ParseMacroSynResult.Success -> expandDeriveDeserialize(parsed.value)
        is ParseMacroSynResult.CompileError -> parsed.tokens
    }
}
