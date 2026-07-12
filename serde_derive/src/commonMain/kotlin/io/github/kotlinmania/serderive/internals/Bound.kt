// port-lint: source bound.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.GenericParam
import io.github.kotlinmania.syn.GenericParamList
import io.github.kotlinmania.syn.GenericArgument
import io.github.kotlinmania.syn.GenericArgumentList
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.LifetimeList
import io.github.kotlinmania.syn.Macro
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathArguments
import io.github.kotlinmania.syn.PathSegment
import io.github.kotlinmania.syn.PathSegmentList
import io.github.kotlinmania.syn.QSelf
import io.github.kotlinmania.syn.ReturnType
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.TypeParamBoundList
import io.github.kotlinmania.syn.TraitBoundModifier
import io.github.kotlinmania.syn.WherePredicate
import io.github.kotlinmania.syn.BareFnArg
import io.github.kotlinmania.syn.WherePredicateList
import io.github.kotlinmania.syn.token.Colon
import io.github.kotlinmania.syn.token.Comma
import io.github.kotlinmania.syn.token.Lt
import io.github.kotlinmania.syn.token.Gt
import io.github.kotlinmania.syn.token.PathSep
import io.github.kotlinmania.syn.token.Plus

private fun WherePredicateList.pushPredicate(predicate: WherePredicate) {
    push(predicate) { Comma.default() }
}

public fun withoutDefaults(generics: Generics): Generics {
    val newParams = GenericParamList()
    for (param in generics.params.toList()) {
        when (param) {
            is GenericParam.TypeParam -> newParams.push(GenericParam.TypeParam(
                param.attrs,
                param.ident,
                param.colonToken,
                param.bounds,
                eqToken = null,
                default = null
            )) { Comma.default() }
            else -> newParams.push(param) { Comma.default() }
        }
    }
    return generics.copy(params = newParams)
}

public fun withWherePredicates(
    generics: Generics,
    predicates: List<WherePredicate>
): Generics {
    val mutGenerics = generics.copy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (predicate in predicates) {
        dstPredicates.pushPredicate(predicate)
    }
    return mutGenerics
}

public fun withWherePredicatesFromFields(
    cont: Container,
    generics: Generics,
    fromField: (AttrField) -> List<WherePredicate>?
): Generics {
    val mutGenerics = generics.copy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (field in cont.data.allFields()) {
        val predicateSlice = fromField(field.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.pushPredicate(innerPredicate)
        }
    }
    return mutGenerics
}

public fun withWherePredicatesFromVariants(
    cont: Container,
    generics: Generics,
    fromVariant: (AttrVariant) -> List<WherePredicate>?
): Generics {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return generics.copy()
    }
    val mutGenerics = generics.copy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (variant in variants) {
        val predicateSlice = fromVariant(variant.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.pushPredicate(innerPredicate)
        }
    }
    return mutGenerics
}

public fun withBound(
    cont: Container,
    generics: Generics,
    filter: (Field, Variant?) -> Boolean,
    bound: Path
): Generics {
    class FindTyParams(
        val allTypeParams: Set<Ident>,
        val relevantTypeParams: MutableSet<Ident>,
        val associatedTypeUsage: MutableList<SynType.Path>
    ) {
        fun visitField(field: io.github.kotlinmania.syn.Field) {
            val ty = ungroup(field.ty)
            if (ty is SynType.Path) {
                val firstSegment = ty.path.segments.toList().firstOrNull()
                if (firstSegment != null) {
                    if (allTypeParams.contains(firstSegment.ident)) {
                        associatedTypeUsage.add(ty)
                    }
                }
            }
            visitType(field.ty)
        }

        fun visitPath(path: Path) {
            val lastSeg = path.segments.toList().lastOrNull()
            if (lastSeg != null && lastSeg.ident.toString() == "PhantomData") {
                return
            }
            if (path.leadingColon == null && path.segments.size == 1) {
                val id = path.segments.toList()[0].ident
                if (allTypeParams.contains(id)) {
                    relevantTypeParams.add(id)
                }
            }
            for (segment in path.segments.toList()) {
                visitPathSegment(segment)
            }
        }

        fun visitType(ty: SynType) {
            when (ty) {
                is SynType.Array -> visitType(ty.elem)
                is SynType.BareFn -> {
                    for (arg in ty.inputs.toList()) {
                        visitType(arg.ty)
                    }
                    visitReturnType(ty.output)
                }
                is SynType.Group -> visitType(ty.elem)
                is SynType.ImplTrait -> {
                    for (bound in ty.bounds.toList()) {
                        visitTypeParamBound(bound)
                    }
                }
                is SynType.Macro -> visitMacro(ty.mac)
                is SynType.Paren -> visitType(ty.elem)
                is SynType.Path -> {
                    val qself = ty.qself
                    if (qself != null) {
                        visitType(qself.ty)
                    }
                    visitPath(ty.path)
                }
                is SynType.Ptr -> visitType(ty.elem)
                is SynType.Reference -> visitType(ty.elem)
                is SynType.Slice -> visitType(ty.elem)
                is SynType.TraitObject -> {
                    for (bound in ty.bounds.toList()) {
                        visitTypeParamBound(bound)
                    }
                }
                is SynType.Tuple -> {
                    for (elem in ty.elems.toList()) {
                        visitType(elem)
                    }
                }
                else -> {}
            }
        }

        fun visitPathSegment(segment: PathSegment) {
            visitPathArguments(segment.arguments)
        }

        fun visitPathArguments(arguments: PathArguments) {
            when (arguments) {
                is PathArguments.None -> {}
                is PathArguments.AngleBracketed -> {
                    for (arg in arguments.args.toList()) {
                        when (arg) {
                            is GenericArgument.TypeArg -> visitType(arg.type)
                            is GenericArgument.AssocTypeArg -> visitType(arg.assoc.ty)
                            else -> {}
                        }
                    }
                }
                is PathArguments.Parenthesized -> {
                    for (argument in arguments.inputs.toList()) {
                        visitType(argument)
                    }
                    visitReturnType(arguments.output)
                }
            }
        }

        fun visitReturnType(returnType: ReturnType) {
            when (returnType) {
                is ReturnType.Default -> {}
                is ReturnType.TypeReturn -> visitType(returnType.ty)
            }
        }

        fun visitTypeParamBound(bound: TypeParamBound) {
            when (bound) {
                is TypeParamBound.Trait -> visitPath(bound.path)
                else -> {}
            }
        }

        fun visitMacro(mac: Macro) {}
    }

    val allTypeParams = generics.typeParams().map { it.ident }.toSet()

    val visitor = FindTyParams(
        allTypeParams,
        mutableSetOf(),
        mutableListOf()
    )

    when (val data = cont.data) {
        is Data.Enum -> {
            for (variant in data.variants) {
                for (field in variant.fields) {
                    if (filter(field, variant)) {
                        visitor.visitField(field.original)
                    }
                }
            }
        }
        is Data.Struct -> {
            for (field in data.fields) {
                if (filter(field, null)) {
                    visitor.visitField(field.original)
                }
            }
        }
    }

    val relevantTypeParams = visitor.relevantTypeParams
    val associatedTypeUsage = visitor.associatedTypeUsage

    fun makeWhereBoundedType(boundedTy: SynType.Path, bound: Path): WherePredicate {
        val boundsList = TypeParamBoundList()
        boundsList.pushValue(
            TypeParamBound.Trait(
                parenToken = null,
                modifier = TraitBoundModifier.None,
                lifetimes = null,
                path = bound.deepCopy(),
            )
        )
        return WherePredicate.TypePredicate(
            lifetimes = null,
            boundedTy = boundedTy,
            colonToken = Colon.default(),
            bounds = boundsList
        )
    }

    val dstGenerics = generics.copy()
    val dstPredicates = dstGenerics.makeWhereClause().predicates
    for (param in generics.typeParams()) {
        val id = param.ident
        if (!relevantTypeParams.contains(id)) {
            continue
        }
        val boundedTy = SynType.Path(
            qself = null,
            path = Path.from(id)
        )
        dstPredicates.pushPredicate(makeWhereBoundedType(boundedTy, bound))
    }
    for (boundedTy in associatedTypeUsage) {
        dstPredicates.pushPredicate(makeWhereBoundedType(boundedTy.deepCopy(), bound))
    }
    return dstGenerics
}

public fun withSelfBound(
    cont: Container,
    generics: Generics,
    bound: Path
): Generics {
    val mutGenerics = generics.copy()
    val boundsList = TypeParamBoundList()
    boundsList.pushValue(
        TypeParamBound.Trait(
            parenToken = null,
            modifier = TraitBoundModifier.None,
            lifetimes = null,
            path = bound.deepCopy(),
        )
    )
    mutGenerics.makeWhereClause().predicates.pushPredicate(
        WherePredicate.TypePredicate(
            lifetimes = null,
            boundedTy = typeOfItem(cont),
            colonToken = Colon.default(),
            bounds = boundsList
        )
    )
    return mutGenerics
}

public fun withLifetimeBound(generics: Generics, lifetime: String): Generics {
    val bound = Lifetime.new(lifetime, Span.callSite())
    val def = GenericParam.LifetimeParam(
        attrs = mutableListOf(),
        lifetime = bound.deepCopy(),
        colonToken = null,
        bounds = LifetimeList()
    )

    val params = mutableListOf<GenericParam>(def)
    for (param in generics.params.toList()) {
        when (param) {
            is GenericParam.LifetimeParam -> {
                param.bounds.push(bound.deepCopy()) { Plus.default() }
            }
            is GenericParam.TypeParam -> {
                param.bounds.push(TypeParamBound.LifetimeBound(bound.deepCopy())) { Plus.default() }
            }
            is GenericParam.ConstParam -> {}
        }
        params.add(param)
    }

    val newParamList = GenericParamList()
    for (p in params) {
        newParamList.push(p) { Comma.default() }
    }
    return generics.copy(params = newParamList)
}

private fun typeOfItem(cont: Container): SynType {
    val segment = PathSegment(
        ident = cont.ident,
        arguments = PathArguments.AngleBracketed(
            colon2Token = null,
            ltToken = Lt.default(),
            args = run {
                val argList = GenericArgumentList()
                for (param in cont.generics.params.toList()) {
                    when (param) {
                        is GenericParam.TypeParam -> {
                            argList.push(GenericArgument.TypeArg(SynType.Path(
                                qself = null,
                                path = Path.from(param.ident)
                            ))) { Comma.default() }
                        }
                        is GenericParam.LifetimeParam -> {
                            argList.push(GenericArgument.LifetimeArg(param.lifetime.deepCopy())) { Comma.default() }
                        }
                        is GenericParam.ConstParam -> {
                            throw Exception("Serde does not support const generics yet")
                        }
                    }
                }
                argList
            },
            gtToken = Gt.default()
        )
    )
    val segList = PathSegmentList()
    segList.pushValue(segment)
    return SynType.Path(
        qself = null,
        path = Path(
            leadingColon = null,
            segments = segList
        )
    )
}
