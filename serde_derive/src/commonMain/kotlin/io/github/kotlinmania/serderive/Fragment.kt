package io.github.kotlinmania.serderive

/* STUBBED OUT FOR NOW
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.quote

public sealed class Fragment {
    public class Expr(public val expr: TokenStream) : Fragment()
    public class Block(public val block: TokenStream) : Fragment()

    public val asTokenStream: TokenStream
        get() = when (this) {
            is Expr -> expr
            is Block -> block
        }
}

public class FragmentExpr(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (fragment) {
            is Fragment.Expr -> out.extend(fragment.expr)
            is Fragment.Block -> out.extend(quote("{ `#`{fragment.block} }"))
        }
    }
}

public class FragmentStmts(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (fragment) {
            is Fragment.Expr -> out.extend(fragment.expr)
            is Fragment.Block -> out.extend(fragment.block)
        }
    }
}

public class FragmentMatch(public val fragment: Fragment) : ToTokens {
    override fun toTokens(out: TokenStream) {
        when (fragment) {
            is Fragment.Expr -> out.extend(quote("`#`{fragment.expr},"))
            is Fragment.Block -> out.extend(quote("{ `#`{fragment.block} }"))
        }
    }
}

public fun quoteExpr(tokens: TokenStream): Fragment = Fragment.Expr(tokens)
public fun quoteBlock(tokens: TokenStream): Fragment = Fragment.Block(tokens)

*/
