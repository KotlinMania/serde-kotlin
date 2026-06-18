package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.*
import io.github.kotlinmania.syn.punctuated.Punctuated

public fun withoutDefaults(generics: Generics): Generics {
    return generics.copy(
        params = punctuated.Punctuated.fromList(
            generics.params.map { param ->
                when (param) {
                    is GenericParam.Type -> GenericParam.Type(
                        param.type.copy(
                            eqToken = null,
                            default = null
                        )
                    )
                    else -> param
                }
            }.toList()
        )
    )
}

public fun withWherePredicates(
    generics: Generics,
    predicates: List<WherePredicate>
): Generics {
    var mutGenerics = generics.clone()
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
    var mutGenerics = generics.clone()
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
        is Data.Struct -> return generics.clone()
    }
    var mutGenerics = generics.clone()
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
        val associatedTypeUsage: MutableList<TypePath>
    ) {
        fun visitField(field: syn.Field) {
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

    fun makeWhereBoundedType(boundedTy: TypePath, bound: Path): WherePredicate {
        return WherePredicate.Type(PredicateType(
            lifetimes = null,
            boundedTy = SynType.Path(boundedTy),
            colonToken = token.Colon(),
            bounds = punctuated.Punctuated.fromList(listOf(
                TypeParamBound.Trait(TraitBound(
                    parenToken = null,
                    modifier = TraitBoundModifier.None,
                    lifetimes = null,
                    path = bound.clone()
                ))
            ))
        ))
    }

    var dstGenerics = generics.clone()
    val dstPredicates = dstGenerics.makeWhereClause().predicates
    for (param in generics.typeParams()) {
        val id = param.ident
        if (!relevantTypeParams.contains(id)) {
            continue
        }
        val boundedTy = TypePath(
            qself = null,
            path = Path.from(id.clone())
        )
        dstPredicates.add(makeWhereBoundedType(boundedTy, bound))
    }
    for (boundedTy in associatedTypeUsage) {
        dstPredicates.add(makeWhereBoundedType(boundedTy.clone(), bound))
    }
    return dstGenerics
}

public fun withSelfBound(
    cont: Container,
    generics: Generics,
    bound: Path
): Generics {
    var mutGenerics = generics.clone()
    mutGenerics.makeWhereClause().predicates.add(
        WherePredicate.Type(PredicateType(
            lifetimes = null,
            boundedTy = typeOfItem(cont),
            colonToken = token.Colon(),
            bounds = punctuated.Punctuated.fromList(listOf(
                TypeParamBound.Trait(TraitBound(
                    parenToken = null,
                    modifier = TraitBoundModifier.None,
                    lifetimes = null,
                    path = bound.clone()
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
        lifetime = bound.clone(),
        colonToken = null,
        bounds = punctuated.Punctuated.new()
    )

    val params = mutableListOf<GenericParam>(GenericParam.Lifetime(def))
    for (param in generics.params) {
        when (param) {
            is GenericParam.Lifetime -> {
                param.bounds.add(bound.clone())
            }
            is GenericParam.Type -> {
                param.bounds.add(TypeParamBound.Lifetime(bound.clone()))
            }
            is GenericParam.Const -> {}
        }
        params.add(param)
    }

    return generics.copy(params = punctuated.Punctuated.fromList(params))
}

private fun typeOfItem(cont: Container): SynType {
    return SynType.Path(TypePath(
        qself = null,
        path = Path(
            leadingColon = null,
            segments = punctuated.Punctuated.fromList(listOf(
                PathSegment(
                    ident = cont.ident.clone(),
                    arguments = PathArguments.AngleBracketed(
                        AngleBracketedGenericArguments(
                            colon2Token = null,
                            ltToken = token.Lt(),
                            args = punctuated.Punctuated.fromList(
                                cont.generics.params.map { param ->
                                    when (param) {
                                        is GenericParam.Type -> {
                                            GenericArgument.TypeArg(SynType.Path(TypePath(
                                                qself = null,
                                                path = Path.from(param.ident.clone())
                                            )))
                                        }
                                        is GenericParam.Lifetime -> {
                                            GenericArgument.LifetimeArg(param.lifetime.clone())
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
