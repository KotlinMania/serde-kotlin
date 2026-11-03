// port-lint: source fragment.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.toTokens
import io.github.kotlinmania.syn.token.Brace
import io.github.kotlinmania.syn.token.Comma

public sealed class Fragment {
    public data class Expr(val expr: TokenStream) : Fragment()
    public data class Block(val block: TokenStream) : Fragment()

    public fun asRef(): TokenStream = when (this) {
        is Expr -> expr
        is Block -> block
    }
}

public class Expr(public val fragment: Fragment) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        when (val frag = fragment) {
            is Fragment.Expr -> frag.expr.toTokens(tokens)
            is Fragment.Block -> {
                Brace.default().surround(tokens) { inner ->
                    frag.block.toTokens(inner)
                }
            }
        }
    }
}

public class Stmts(public val fragment: Fragment) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        when (val frag = fragment) {
            is Fragment.Expr -> frag.expr.toTokens(tokens)
            is Fragment.Block -> frag.block.toTokens(tokens)
        }
    }
}

public class Match(public val fragment: Fragment) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        when (val frag = fragment) {
            is Fragment.Expr -> {
                frag.expr.toTokens(tokens)
                Comma.default().toTokens(tokens)
            }
            is Fragment.Block -> {
                Brace.default().surround(tokens) { inner ->
                    frag.block.toTokens(inner)
                }
            }
        }
    }
}