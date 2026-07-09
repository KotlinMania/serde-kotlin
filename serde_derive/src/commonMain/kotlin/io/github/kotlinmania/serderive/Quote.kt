package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.quote as quoteKotlin
import io.github.kotlinmania.quote.quoteSpanned as quoteSpannedKotlin

private val interpolation = Regex("`#`([A-Za-z_][A-Za-z0-9_]*)")

private object EmptyTokens : ToTokens {
    override fun toTokens(tokens: TokenStream) = Unit
}

public fun quote(
    template: String,
    interpolations: Map<String, *> = emptyMap<String, Any?>(),
): TokenStream =
    quoteKotlin(template, checkedInterpolations(template, interpolations))

public fun quote(template: String, vararg pairs: Pair<String, *>): TokenStream =
    quote(template, mapOf(*pairs))

public fun quoteSpanned(
    span: Span,
    template: String,
    interpolations: Map<String, *> = emptyMap<String, Any?>(),
): TokenStream =
    quoteSpannedKotlin(span, template, checkedInterpolations(template, interpolations))

public fun quoteSpanned(
    span: Span,
    template: String,
    vararg pairs: Pair<String, *>,
): TokenStream =
    quoteSpanned(span, template, mapOf(*pairs))

private fun checkedInterpolations(template: String, values: Map<String, *>): Map<String, *> {
    val required = interpolation.findAll(template).map { it.groupValues[1] }.toSet()
    val missing = required - values.keys
    require(missing.isEmpty()) {
        "missing quote interpolation${if (missing.size == 1) "" else "s"}: ${missing.sorted().joinToString()}"
    }
    return values.mapValues { (_, value) -> cloneInterpolation(value) }
}

private fun cloneInterpolation(value: Any?): Any =
    when (value) {
        null -> EmptyTokens
        is TokenStream -> value.clone()
        is Iterable<*> -> value.map(::cloneInterpolation)
        else -> value
    }
