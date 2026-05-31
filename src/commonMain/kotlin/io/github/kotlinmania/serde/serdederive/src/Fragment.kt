// port-lint: source serde_derive/src/fragment.rs
package io.github.kotlinmania.serde.serdederive.src

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.toTokens
import io.github.kotlinmania.syn.token.Brace
import io.github.kotlinmania.syn.token.Comma

sealed class Fragment {
    /**
     * Tokens that can be used as an expression.
     */
    data class Expr(
        val expr: TokenStream,
    ) : Fragment()

    /**
     * Tokens that can be used inside a block. The surrounding curly braces are not part of these
     * tokens.
     */
    data class Block(
        val block: TokenStream,
    ) : Fragment()

    fun asRef(): TokenStream =
        when (this) {
            is Expr -> expr
            is Block -> block
        }
}

fun quoteExpr(tokens: TokenStream): Fragment = Fragment.Expr(tokens)

fun quoteBlock(tokens: TokenStream): Fragment = Fragment.Block(tokens)

/**
 * Interpolate a fragment in place of an expression. This involves surrounding `Block` fragments in
 * curly braces.
 */
data class Expr(
    val fragment: Fragment,
) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        when (val current = fragment) {
            is Fragment.Expr -> current.expr.toTokens(tokens)
            is Fragment.Block -> {
                Brace.default().surround(tokens) { inner: TokenStream ->
                    current.block.toTokens(inner)
                }
            }
        }
    }
}

/**
 * Interpolate a fragment as the statements of a block.
 */
data class Stmts(
    val fragment: Fragment,
) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        when (val current = fragment) {
            is Fragment.Expr -> current.expr.toTokens(tokens)
            is Fragment.Block -> current.block.toTokens(tokens)
        }
    }
}

/**
 * Interpolate a fragment as the value part of a `when` expression. This involves putting a comma
 * after expressions and curly braces around blocks.
 */
data class Match(
    val fragment: Fragment,
) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        when (val current = fragment) {
            is Fragment.Expr -> {
                current.expr.toTokens(tokens)
                Comma.default().toTokens(tokens)
            }

            is Fragment.Block -> {
                Brace.default().surround(tokens) { inner: TokenStream ->
                    current.block.toTokens(inner)
                }
            }
        }
    }
}
