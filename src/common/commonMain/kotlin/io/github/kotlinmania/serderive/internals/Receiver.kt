// port-lint: source internals/receiver.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.toTokens
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Expr
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathParse
import io.github.kotlinmania.syn.PathSegmentList
import io.github.kotlinmania.syn.QSelf
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.gen.VisitMut
import io.github.kotlinmania.syn.gen.clone
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.token.Gt
import io.github.kotlinmania.syn.token.Lt
import io.github.kotlinmania.syn.token.PathSep

public fun replaceReceiver(input: DeriveInput): DeriveInput {
    val output = input.clone()
    val ident = output.ident
    val tyGenerics = output.generics.splitForImpl().typeGenerics
    val selfTyTokens = TokenStream.new()
    ident.toTokens(selfTyTokens)
    tyGenerics.toTokens(selfTyTokens)
    val selfTy = parseSelfTy(selfTyTokens)

    val visitor = ReplaceReceiver(selfTy)
    visitor.visitGenericsMut(output.generics)
    visitor.visitDataMut(output.data)
    return output
}

private fun parseSelfTy(tokens: TokenStream): SynType.Path {
    val result: io.github.kotlinmania.syn.SynResult<Path> =
        parse2(PathParse::parse, tokens)
    return SynType.Path(null, result.getOrThrow())
}

private class ReplaceReceiver(private val selfTy: SynType.Path) : VisitMut() {

    private fun selfTy(span: Span): SynType.Path {
        val tokens = TokenStream.new()
        selfTy.toTokens(tokens)
        val respanned = respan(tokens, span)
        val result: io.github.kotlinmania.syn.SynResult<Path> =
            parse2(PathParse::parse, respanned)
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

        val newSegments = pathSegments(segments.drop(1))
        return newQself to Path(PathSep.default(), newSegments)
    }

    private fun selfToExprPath(path: Path): Path {
        val selfTyPath = selfTy(path.segments.first()?.ident?.span() ?: Span.callSite())
        val variantSegments = path.segments.toList()
        val receiverSegments = selfTyPath.path.segments.toList()
        val newSegments = pathSegments(receiverSegments + variantSegments.drop(1))
        return Path(selfTyPath.path.leadingColon, newSegments)
    }

    override fun visitType(t: SynType) {
        if (t is SynType.Path && t.qself == null && t.path.isIdent("Self")) {
                val span = t.path.segments.first()?.ident?.span() ?: Span.callSite()
            val replacement = selfTy(span)
            t.qself = replacement.qself
            t.path = replacement.path
            return
        }
        super.visitType(t)
    }

    override fun visitTypePath(typePath: SynType.Path) {
        if (typePath.qself == null) {
            val (qself, path) = selfToQself(typePath.qself, typePath.path)
            typePath.qself = qself
            typePath.path = path
        }
        super.visitTypePath(typePath)
    }

    override fun visitExprPath(exprPath: Expr.Path) {
        if (exprPath.qself == null) {
            val (qself, path) = selfToQself(exprPath.qself, exprPath.path)
            exprPath.qself = qself
            exprPath.path = path
        }
        super.visitExprPath(exprPath)
    }
}

private fun pathSegments(segments: List<io.github.kotlinmania.syn.PathSegment>): PathSegmentList {
    val result = PathSegmentList()
    for (segment in segments) {
        result.push(segment.deepCopy(), PathSep::default)
    }
    return result
}
