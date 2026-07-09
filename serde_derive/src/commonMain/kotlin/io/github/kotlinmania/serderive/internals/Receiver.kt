// port-lint: source internals/receiver.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.toTokens
import io.github.kotlinmania.syn.Data
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Expr
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.Macro
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathArguments
import io.github.kotlinmania.syn.PathParse
import io.github.kotlinmania.syn.PathSegmentList
import io.github.kotlinmania.syn.QSelf
import io.github.kotlinmania.syn.ReturnType
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.WherePredicate
import io.github.kotlinmania.syn.gen.VisitMut
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.token.Gt
import io.github.kotlinmania.syn.token.Lt
import io.github.kotlinmania.syn.token.PathSep

public fun replaceReceiver(input: DeriveInput) {
    val ident = input.ident
    val tyGenerics = input.generics.splitForImpl().typeGenerics
    val selfTyTokens = TokenStream.new()
    ident.toTokens(selfTyTokens)
    tyGenerics.toTokens(selfTyTokens)
    val selfTy = parseSelfTy(selfTyTokens)

    val visitor = ReplaceReceiver(selfTy)
    require(visitor.visitGenericsMut(input.generics) === input.generics)
    require(visitor.visitDataMut(input.data) === input.data)
}

private fun parseSelfTy(tokens: TokenStream): SynType.Path {
    val result: io.github.kotlinmania.syn.SynResult<Path> =
        parse2(PathParse, tokens)
    return SynType.Path(null, result.getOrThrow())
}

private class ReplaceReceiver(private val selfTy: SynType.Path) : VisitMut() {

    private fun selfTy(span: Span): SynType.Path {
        val tokens = TokenStream.new()
        selfTy.toTokens(tokens)
        val respanned = respan(tokens, span)
        val result: io.github.kotlinmania.syn.SynResult<Path> =
            parse2(PathParse, respanned)
        return SynType.Path(null, result.getOrThrow())
    }

    private fun selfToQself(qselfRef: QSelf?, path: Path): Pair<QSelf?, Path> {
        if (path.leadingColon != null) return qselfRef to path
        if (path.segments.first()?.ident?.toString() != "Self") return qselfRef to path

        val segments = path.segments.toList()
        if (segments.size == 1) {
            return qselfRef to selfToExprPath(path)
        }

        val span = segments[0].ident.span()
        val newQself = QSelf(
            ltToken = Lt.default(),
            ty = selfTy(span),
            position = 0,
            asToken = null,
            gtToken = Gt.default(),
        )

        val newSegments = PathSegmentList()
        for (i in 1 until segments.size) {
            newSegments.pushValue(segments[i].deepCopy())
        }
        return newQself to Path(PathSep.default(), newSegments)
    }

    private fun selfToExprPath(path: Path): Path {
        val selfTyPath = selfTy(path.segments.first()?.ident?.span() ?: Span.callSite())
        val variantSegments = path.segments.toList()
        val receiverSegments = selfTyPath.path.segments.toList()
        val newSegments = PathSegmentList()
        for (seg in receiverSegments) {
            newSegments.pushValue(seg.deepCopy())
        }
        if (variantSegments.size > 1) {
            newSegments.pushPunct(PathSep.default())
            for (i in 1 until variantSegments.size) {
                newSegments.pushValue(variantSegments[i].deepCopy())
            }
        }
        return Path(selfTyPath.path.leadingColon, newSegments)
    }

    override fun visitType(t: SynType): SynType {
        if (t is SynType.Path) {
            return if (t.qself == null && t.path.isIdent("Self")) {
                val span = t.path.segments.first()?.ident?.span() ?: Span.callSite()
                selfTy(span)
            } else {
                visitTypePathMut(t)
            }
        }
        return visitTypeMutImpl(t)
    }

    override fun visitTypePath(typePath: SynType.Path): SynType {
        val rewritten =
            if (typePath.qself == null) {
                val (qself, path) = selfToQself(typePath.qself, typePath.path)
                SynType.Path(qself, path)
            } else {
                typePath
            }
        return visitTypePathMutImpl(rewritten)
    }

    override fun visitExprPath(exprPath: Expr.Path): Expr {
        val rewritten =
            if (exprPath.qself == null) {
                val (qself, path) = selfToQself(exprPath.qself, exprPath.path)
                Expr.Path(exprPath.attrs, qself, path)
            } else {
                exprPath
            }
        return visitExprPathMutImpl(rewritten)
    }

    private fun visitTypeMut(ty: SynType): SynType {
        return visitType(ty)
    }

    private fun visitTypePathMut(ty: SynType.Path): SynType {
        return visitTypePath(ty)
    }

    private fun visitExprPathMut(expr: Expr.Path): Expr {
        return visitExprPath(expr)
    }

    private fun visitTypeMutImpl(ty: SynType): SynType {
        return super.visitType(ty)
    }

    private fun visitTypePathMutImpl(ty: SynType.Path): SynType {
        return super.visitTypePath(ty)
    }

    private fun visitExprPathMutImpl(expr: Expr.Path): Expr {
        return super.visitExprPath(expr)
    }

    private fun visitPathMut(path: Path): Path {
        return visitPath(path)
    }

    private fun visitPathArgumentsMut(arguments: PathArguments): PathArguments {
        return visitPathArguments(arguments)
    }

    private fun visitReturnTypeMut(returnType: ReturnType): ReturnType {
        return visitReturnType(returnType)
    }

    private fun visitExprMut(expr: Expr): Expr {
        return visitExpr(expr)
    }

    private fun visitMacroMut(mac: Macro): Macro {
        return visitMacro(mac)
    }

    fun visitGenericsMut(generics: Generics): Generics {
        return super.visitGenerics(generics)
    }

    fun visitDataMut(data: Data): Data {
        return super.visitData(data)
    }

    override fun visitGenerics(g: Generics): Generics {
        return super.visitGenerics(g)
    }

    override fun visitData(d: Data): Data {
        return super.visitData(d)
    }

    override fun visitTypeParamBound(bound: TypeParamBound): TypeParamBound {
        return if (bound is TypeParamBound.Trait) {
            bound.copy(path = visitPathMut(bound.path))
        } else {
            bound
        }
    }

    override fun visitWherePredicate(wherePredicate: WherePredicate): WherePredicate {
        return if (wherePredicate is WherePredicate.TypePredicate) {
            val boundedTy = visitTypeMut(wherePredicate.boundedTy) as SynType.Path
            val bounds = wherePredicate.bounds.copy({ visitTypeParamBoundMut(it) }, { it })
            WherePredicate.TypePredicate(
                boundedTy = boundedTy,
                colonToken = wherePredicate.colonToken,
                bounds = bounds,
            )
        } else {
            wherePredicate
        }
    }

    private fun visitTypeParamBoundMut(bound: TypeParamBound): TypeParamBound {
        return visitTypeParamBound(bound)
    }

    private fun visitWherePredicateMut(wherePredicate: WherePredicate): WherePredicate {
        return visitWherePredicate(wherePredicate)
    }

    override fun visitPath(p: Path): Path {
        val newSegments = PathSegmentList()
        val segments = p.segments.toList()
        for (i in segments.indices) {
            if (i > 0) {
                newSegments.pushPunct(PathSep.default())
            }
            val segment = segments[i].deepCopy()
            segment.arguments = visitPathArgumentsMut(segment.arguments)
            newSegments.pushValue(segment)
        }
        return Path(p.leadingColon, newSegments)
    }

    override fun visitPathArguments(pathArgs: PathArguments): PathArguments {
        return super.visitPathArguments(pathArgs)
    }

    override fun visitReturnType(rt: ReturnType): ReturnType {
        return super.visitReturnType(rt)
    }

    override fun visitExpr(e: Expr): Expr {
        return super.visitExpr(e)
    }

    override fun visitMacro(mac: Macro): Macro {
        return super.visitMacro(mac)
    }
}
