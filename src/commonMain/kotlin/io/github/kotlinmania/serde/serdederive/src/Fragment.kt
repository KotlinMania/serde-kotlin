// port-lint: source serde_derive/src/fragment.rs
package io.github.kotlinmania.serde.serdederive.src

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.Token
import io.github.kotlinmania.syn.token.Brace

public sealed class Fragment {
    /**
     * Tokens that can be used as an expression.
     */
    public data class Expr(public val expr: TokenStream) : Fragment()

    /**
     * Tokens that can be used inside a block. The surrounding curly braces are not part of these
     * tokens.
     */
    public data class Block(public val block: TokenStream) : Fragment()

    public fun asRef(): TokenStream =
        when (this) {
            is Expr -> expr
            is Block -> block
        }
}

public fun quoteExpr(tokens: TokenStream): Fragment =
    Fragment.Expr(tokens)

public fun quoteBlock(tokens: TokenStream): Fragment =
    Fragment.Block(tokens)

/**
 * Interpolate a fragment in place of an expression. This involves surrounding `Block` fragments in
 * curly braces.
 */
public data class Expr(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (val current = fragment) {
            is Fragment.Expr -> current.expr.toTokens(out)
            is Fragment.Block -> {
                Brace.default().surround(out) { inner: TokenStream ->
                    current.block.toTokens(inner)
                }
            }
        }
    }
}

/**
 * Interpolate a fragment as the statements of a block.
 */
public data class Stmts(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (val current = fragment) {
            is Fragment.Expr -> current.expr.toTokens(out)
            is Fragment.Block -> current.block.toTokens(out)
        }
    }
}

/**
 * Interpolate a fragment as the value part of a `when` expression. This involves putting a comma
 * after expressions and curly braces around blocks.
 */
public data class Match(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (val current = fragment) {
            is Fragment.Expr -> {
                current.expr.toTokens(out)
                Token.Comma.default().toTokens(out)
            }

            is Fragment.Block -> {
                Brace.default().surround(out) { inner: TokenStream ->
                    current.block.toTokens(inner)
                }
            }
        }
    }
}
