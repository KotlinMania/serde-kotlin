// port-lint: source bound.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.syn.*
import io.github.kotlinmania.syn.token.Colon
import io.github.kotlinmania.syn.token.Lt
import io.github.kotlinmania.syn.token.Gt
import io.github.kotlinmania.syn.token.PathSep

public fun withoutDefaults(generics: Generics): Generics {
    val newParams = GenericParamList()
    for (param in generics.params.toList()) {
        when (param) {
            is GenericParam.TypeParam -> newParams.pushValue(GenericParam.TypeParam(
                param.attrs,
                param.ident,
                param.colonToken,
                param.bounds,
                eqToken = null,
                default = null
            ))
            else -> newParams.pushValue(param)
        }
    }
    return generics.copy(params = newParams)
}

public fun withWherePredicates(
    generics: Generics,
    predicates: List<WherePredicate>
): Generics {
    val mutGenerics = generics.deepCopy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (predicate in predicates) {
        dstPredicates.pushValue(predicate)
    }
    return mutGenerics
}

public fun withWherePredicatesFromFields(
    cont: Container,
    generics: Generics,
    fromField: (AttrField) -> List<WherePredicate>?
): Generics {
    val mutGenerics = generics.deepCopy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (field in cont.data.allFields()) {
        val predicateSlice = fromField(field.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.pushValue(innerPredicate)
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
        is Data.Struct -> return generics.deepCopy()
    }
    val mutGenerics = generics.deepCopy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (variant in variants) {
        val predicateSlice = fromVariant(variant.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.pushValue(innerPredicate)
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
                val firstSegment = ty.path.segments.firstOrNull()
                if (firstSegment != null) {
                    if (allTypeParams.contains(firstSegment.ident)) {
                        associatedTypeUsage.add(ty)
                    }
                }
            }
            visitType(field.ty)
        }

        fun visitPath(path: Path) {
            val lastSeg = path.segments.lastOrNull()
            if (lastSeg != null && lastSeg.ident.toString() == "PhantomData") {
                return
            }
            if (path.leadingColon == null && path.segments.size == 1) {
                val id = path.segments[0].ident
                if (allTypeParams.contains(id)) {
                    relevantTypeParams.add(id)
                }
            }
            for (segment in path.segments) {
                visitPathSegment(segment)
            }
        }

        fun visitType(ty: SynType) {
            when (ty) {
                is SynType.Array -> visitType(ty.elem)
                is SynType.BareFn -> {
                    for (arg in ty.inputs) {
                        visitType(arg.ty)
                    }
                    visitReturnType(ty.output)
                }
                is SynType.Group -> visitType(ty.elem)
                is SynType.ImplTrait -> {
                    for (bound in ty.bounds) {
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
                    for (bound in ty.bounds) {
                        visitTypeParamBound(bound)
                    }
                }
                is SynType.Tuple -> {
                    for (elem in ty.elems) {
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
                    for (arg in arguments.args) {
                        when (arg) {
                            is GenericArgument.TypeArg -> visitType(arg.type)
                            is GenericArgument.AssocTypeArg -> visitType(arg.ty)
                            else -> {}
                        }
                    }
                }
                is PathArguments.Parenthesized -> {
                    for (argument in arguments.inputs) {
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
        boundsList.pushValue(TypeParamBound.Trait(bound.deepCopy()))
        return WherePredicate.TypePredicate(
            boundedTy = boundedTy,
            colonToken = Colon.default(),
            bounds = boundsList
        )
    }

    val dstGenerics = generics.deepCopy()
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
        dstPredicates.add(makeWhereBoundedType(boundedTy, bound))
    }
    for (boundedTy in associatedTypeUsage) {
        dstPredicates.add(makeWhereBoundedType(boundedTy.deepCopy() as SynType.Path, bound))
    }
    return dstGenerics
}

public fun withSelfBound(
    cont: Container,
    generics: Generics,
    bound: Path
): Generics {
    val mutGenerics = generics.deepCopy()
    val boundsList = TypeParamBoundList()
    boundsList.pushValue(TypeParamBound.Trait(bound.deepCopy()))
    mutGenerics.makeWhereClause().predicates.add(
        WherePredicate.TypePredicate(
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
        attrs = emptyList(),
        lifetime = bound.deepCopy(),
        colonToken = null,
        bounds = LifetimeList()
    )

    val params = mutableListOf<GenericParam>(GenericParam.Lifetime(def))
    for (param in generics.params.toList()) {
        when (param) {
            is GenericParam.LifetimeParam -> {
                param.bounds.pushValue(bound.deepCopy())
            }
            is GenericParam.TypeParam -> {
                param.bounds.pushValue(TypeParamBound.LifetimeBound(bound.deepCopy()))
            }
            is GenericParam.ConstParam -> {}
        }
        params.add(param)
    }

    val newParamList = GenericParamList()
    for (p in params) {
        newParamList.pushValue(p)
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
                            argList.pushValue(GenericArgument.TypeArg(SynType.Path(
                                qself = null,
                                path = Path.from(param.ident)
                            )))
                        }
                        is GenericParam.LifetimeParam -> {
                            argList.pushValue(GenericArgument.LifetimeArg(param.lifetime.deepCopy()))
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