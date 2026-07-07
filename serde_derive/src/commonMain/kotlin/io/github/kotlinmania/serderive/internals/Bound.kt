package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.*
import io.github.kotlinmania.syn.punctuated.Punctuated

public fun withoutDefaults(generics: Generics): Generics {
    val newParams = GenericParamList()
    for (param in generics.params.toList()) {
        when (param) {
            is GenericParam.Type -> newParams.pushValue(GenericParam.TypeParam(
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
    var mutGenerics = generics.deepCopy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (predicate in predicates) {
        dstPredicates.add(predicate)
    }
    return mutGenerics
}

public fun withWherePredicatesFromFields(
    cont: Container,
    generics: Generics,
    fromField: (Field) -> List<WherePredicate>?
): Generics {
    var mutGenerics = generics.deepCopy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (field in cont.data.allFields()) {
        val predicateSlice = fromField(field.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.add(innerPredicate)
        }
    }
    return mutGenerics
}

public fun withWherePredicatesFromVariants(
    cont: Container,
    generics: Generics,
    fromVariant: (Variant) -> List<WherePredicate>?
): Generics {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return generics.deepCopy()
    }
    var mutGenerics = generics.deepCopy()
    val dstPredicates = mutGenerics.makeWhereClause().predicates

    for (variant in variants) {
        val predicateSlice = fromVariant(variant.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.add(innerPredicate)
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
        val associatedTypeUsage: MutableList<io.github.kotlinmania.syn.TypePath>
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
                is PathArguments.null -> {}
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
                is ReturnType.Type -> visitType(returnType.ty)
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
                    if (filter(field.attrs, variant.attrs)) {
                        visitor.visitField(field.original)
                    }
                }
            }
        }
        is Data.Struct -> {
            for (field in data.fields) {
                if (filter(field.attrs, null)) {
                    visitor.visitField(field.original)
                }
            }
        }
    }

    val relevantTypeParams = visitor.relevantTypeParams
    val associatedTypeUsage = visitor.associatedTypeUsage

    fun makeWhereBoundedType(boundedTy: io.github.kotlinmania.syn.TypePath, bound: Path): WherePredicate {
        return WherePredicate.Type(PredicateType(
            lifetimes = null,
            boundedTy = SynType.Path(boundedTy),
            colonToken = token.Colon(),
            bounds = io.github.kotlinmania.syn.Punctuated.Companion.fromList(listOf(
                TypeParamBound.Trait(TraitBound(
                    parenToken = null,
                    modifier = TraitBoundModifier.null,
                    lifetimes = null,
                    path = bound.deepCopy()
                ))
            ))
        ))
    }

    var dstGenerics = generics.deepCopy()
    val dstPredicates = dstGenerics.makeWhereClause().predicates
    for (param in generics.typeParams()) {
        val id = param.ident
        if (!relevantTypeParams.contains(id)) {
            continue
        }
        val boundedTy = io.github.kotlinmania.syn.TypePath(
            qself = null,
            path = Path.from(id.deepCopy())
        )
        dstPredicates.add(makeWhereBoundedType(boundedTy, bound))
    }
    for (boundedTy in associatedTypeUsage) {
        dstPredicates.add(makeWhereBoundedType(boundedTy.deepCopy(), bound))
    }
    return dstGenerics
}

public fun withSelfBound(
    cont: Container,
    generics: Generics,
    bound: Path
): Generics {
    var mutGenerics = generics.deepCopy()
    mutGenerics.makeWhereClause().predicates.add(
        WherePredicate.Type(PredicateType(
            lifetimes = null,
            boundedTy = typeOfItem(cont),
            colonToken = token.Colon(),
            bounds = io.github.kotlinmania.syn.Punctuated.Companion.fromList(listOf(
                TypeParamBound.Trait(TraitBound(
                    parenToken = null,
                    modifier = TraitBoundModifier.null,
                    lifetimes = null,
                    path = bound.deepCopy()
                ))
            ))
        ))
    )
    return mutGenerics
}

public fun withLifetimeBound(generics: Generics, lifetime: String): Generics {
    val bound = Lifetime.new(lifetime, Span.callSite())
    val def = LifetimeParam(
        attrs = emptyList(),
        lifetime = bound.deepCopy(),
        colonToken = null,
        bounds = punctuated.Punctuated.new()
    )

    val params = mutableListOf<GenericParam>(GenericParam.Lifetime(def))
    for (param in generics.params) {
        when (param) {
            is GenericParam.Lifetime -> {
                param.bounds.add(bound.deepCopy())
            }
            is GenericParam.Type -> {
                param.bounds.add(TypeParamBound.Lifetime(bound.deepCopy()))
            }
            is GenericParam.Const -> {}
        }
        params.add(param)
    }

    return generics.copy(params = (run { val l = io.github.kotlinmania.syn.GenericParamList(); for (p in params)) l.pushValue(p); l })
}

private fun typeOfItem(cont: Container): SynType {
    return SynType.Path(io.github.kotlinmania.syn.TypePath(
        qself = null,
        path = Path(
            leadingColon = null,
            segments = io.github.kotlinmania.syn.Punctuated.Companion.fromList(listOf(
                PathSegment(
                    ident = cont.ident.deepCopy(),
                    arguments = PathArguments.AngleBracketed(
                        AngleBracketedGenericArguments(
                            colon2Token = null,
                            ltToken = token.Lt(),
                            args = io.github.kotlinmania.syn.Punctuated.Companion.fromList(
                                cont.generics.params.map { param ->
                                    when (param) {
                                        is GenericParam.Type -> {
                                            GenericArgument.TypeArg(SynType.Path(io.github.kotlinmania.syn.TypePath(
                                                qself = null,
                                                path = Path.from(param.ident.deepCopy())
                                            )))
                                        }
                                        is GenericParam.Lifetime -> {
                                            GenericArgument.LifetimeArg(param.lifetime.deepCopy())
                                        }
                                        is GenericParam.Const -> {
                                            throw Exception("Serde does not support const generics yet")
                                        }
                                    }
                                }.toList()
                            ),
                            gtToken = token.Gt()
                        )
                    )
                )
            ))
        )
    ))
}
