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
    override fun toTokens(out: TokenStream) {
        when (val frag = fragment) {
            is Fragment.Expr -> frag.expr.toTokens(out)
            is Fragment.Block -> {
                Brace.default().surround(out) { inner ->
                    frag.block.toTokens(inner)
                }
            }
        }
    }
}

public class Stmts(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (val frag = fragment) {
            is Fragment.Expr -> frag.expr.toTokens(out)
            is Fragment.Block -> frag.block.toTokens(out)
        }
    }
}

public class Match(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (val frag = fragment) {
            is Fragment.Expr -> {
                frag.expr.toTokens(out)
                Comma.default().toTokens(out)
            }
            is Fragment.Block -> {
                Brace.default().surround(out) { inner ->
                    frag.block.toTokens(inner)
                }
            }
        }
    }
}