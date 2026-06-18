package io.github.kotlinmania.serderive

/* STUBBED OUT FOR NOW
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.syn.GenericArgument
import io.github.kotlinmania.syn.GenericParam

public fun withoutDefaults(generics: syn.Generics): syn.Generics {
    val newParams = generics.params.map { param ->
        when (param) {
            is GenericParam.Type -> GenericParam.Type(
                syn.TypeParam(
                    attrs = param.attrs,
                    ident = param.ident,
                    colonToken = param.colonToken,
                    bounds = param.bounds,
                    eqToken = null,
                    default = null
                )
            )
            else -> param.clone()
        }
    }
    return syn.Generics(
        ltToken = generics.ltToken,
        params = syn.punctuated.Punctuated.fromList(newParams),
        gtToken = generics.gtToken,
        whereClause = generics.whereClause?.clone()
    )
}

public fun withWherePredicates(
    generics: syn.Generics,
    predicates: List<syn.WherePredicate>
): syn.Generics {
    val newGenerics = generics.clone()
    val dstPredicates = newGenerics.makeWhereClause().predicates

    for (predicate in predicates) {
        dstPredicates.push(predicate.clone())
    }
    return newGenerics
}

public fun withWherePredicatesFromFields(
    cont: Container,
    generics: syn.Generics,
    fromField: (attr.Field) -> List<syn.WherePredicate>?
): syn.Generics {
    val newGenerics = generics.clone()
    val dstPredicates = newGenerics.makeWhereClause().predicates

    for (field in cont.data.allFields()) {
        val predicateSlice = fromField(field.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.push(innerPredicate.clone())
        }
    }
    return newGenerics
}

public fun withWherePredicatesFromVariants(
    cont: Container,
    generics: syn.Generics,
    fromVariant: (attr.Variant) -> List<syn.WherePredicate>?
): syn.Generics {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return generics.clone()
    }
    val newGenerics = generics.clone()
    val dstPredicates = newGenerics.makeWhereClause().predicates

    for (variant in variants) {
        val predicateSlice = fromVariant(variant.attrs) ?: continue
        for (innerPredicate in predicateSlice) {
            dstPredicates.push(innerPredicate.clone())
        }
    }
    return newGenerics
}

public fun withBound(
    cont: Container,
    generics: syn.Generics,
    filter: (attr.Field, attr.Variant?) -> Boolean,
    bound: syn.Path
): syn.Generics {
    class FindTyParams(
        val allTypeParams: Set<syn.Ident>,
        val relevantTypeParams: MutableSet<syn.Ident> = mutableSetOf(),
        val associatedTypeUsage: MutableList<syn.TypePath> = mutableListOf()
    ) {
        fun visitField(field: syn.Field) {
            if (field.ty is syn.Type.Path) {
                val ty = ungroup(field.ty) as? syn.Type.Path
                if (ty != null) {
                    val firstSegment = ty.path.segments.firstOrNull()
                    if (firstSegment != null && allTypeParams.contains(firstSegment.ident)) {
                        associatedTypeUsage.add(ty)
                    }
                }
            }
            visitType(field.ty)
        }

        fun visitPath(path: syn.Path) {
            val lastSeg = path.segments.lastOrNull()
            if (lastSeg?.ident?.toString() == "PhantomData") {
                return
            }
            if (path.leadingColon == null && path.segments.size == 1) {
                val id = path.segments[0].ident
                if (allTypeParams.contains(id)) {
                    relevantTypeParams.add(id.clone())
                }
            }
            for (segment in path.segments) {
                visitPathSegment(segment)
            }
        }

        fun visitType(ty: syn.Type) {
            when (ty) {
                is syn.Type.Array -> visitType(ty.elem)
                is syn.Type.BareFn -> {
                    for (arg in ty.inputs) {
                        visitType(arg.ty)
                    }
                    visitReturnType(ty.output)
                }
                is syn.Type.Group -> visitType(ty.elem)
                is syn.Type.ImplTrait -> {
                    for (paramBound in ty.bounds) {
                        visitTypeParamBound(paramBound)
                    }
                }
                is syn.Type.Macro -> visitMacro(ty.mac)
                is syn.Type.Paren -> visitType(ty.elem)
                is syn.Type.Path -> {
                    if (ty.qself != null) {
                        visitType(ty.qself!!.ty)
                    }
                    visitPath(ty.path)
                }
                is syn.Type.Ptr -> visitType(ty.elem)
                is syn.Type.Reference -> visitType(ty.elem)
                is syn.Type.Slice -> visitType(ty.elem)
                is syn.Type.TraitObject -> {
                    for (paramBound in ty.bounds) {
                        visitTypeParamBound(paramBound)
                    }
                }
                is syn.Type.Tuple -> {
                    for (elem in ty.elems) {
                        visitType(elem)
                    }
                }
                is syn.Type.Infer, is syn.Type.Never, is syn.Type.Verbatim -> {}
                else -> {}
            }
        }

        fun visitPathSegment(segment: syn.PathSegment) {
            visitPathArguments(segment.arguments)
        }

        fun visitPathArguments(arguments: syn.PathArguments) {
            when (arguments) {
                is syn.PathArguments.None -> {}
                is syn.PathArguments.AngleBracketed -> {
                    for (arg in arguments.args) {
                        when (arg) {
                            is GenericArgument.Type -> visitType(arg.ty)
                            is GenericArgument.AssocType -> visitType(arg.ty)
                            is GenericArgument.Lifetime,
                            is GenericArgument.Const,
                            is GenericArgument.AssocConst,
                            is GenericArgument.Constraint -> {}
                            else -> {}
                        }
                    }
                }
                is syn.PathArguments.Parenthesized -> {
                    for (argument in arguments.inputs) {
                        visitType(argument)
                    }
                    visitReturnType(arguments.output)
                }
            }
        }

        fun visitReturnType(returnType: syn.ReturnType) {
            when (returnType) {
                is syn.ReturnType.Default -> {}
                is syn.ReturnType.Type -> visitType(returnType.ty)
            }
        }

        fun visitTypeParamBound(paramBound: syn.TypeParamBound) {
            when (paramBound) {
                is syn.TypeParamBound.Trait -> visitPath(paramBound.path)
                is syn.TypeParamBound.Lifetime,
                is syn.TypeParamBound.PreciseCapture,
                is syn.TypeParamBound.Verbatim -> {}
                else -> {}
            }
        }

        fun visitMacro(mac: syn.Macro) {}
    }

    val allTypeParams = generics.typeParams().map { it.ident.clone() }.toSet()

    val visitor = FindTyParams(allTypeParams)
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

    fun makeWhereBoundedType(boundedTy: syn.TypePath, boundPath: syn.Path): syn.WherePredicate {
        return syn.WherePredicate.Type(
            syn.PredicateType(
                lifetimes = null,
                boundedTy = syn.Type.Path(boundedTy),
                colonToken = null, // Set to something appropriate if missing in Kotlin syn port
                bounds = syn.punctuated.Punctuated.fromList(listOf(
                    syn.TypeParamBound.Trait(syn.TraitBound(
                        parenToken = null,
                        modifier = syn.TraitBoundModifier.None,
                        lifetimes = null,
                        path = boundPath.clone()
                    ))
                ))
            )
        )
    }

    val dstGenerics = generics.clone()
    val dstPredicates = dstGenerics.makeWhereClause().predicates
    for (param in generics.typeParams()) {
        val id = param.ident
        if (!relevantTypeParams.contains(id)) {
            continue
        }
        val boundedTy = syn.TypePath(
            qself = null,
            path = syn.Path(
                leadingColon = null,
                segments = syn.punctuated.Punctuated.fromList(listOf(syn.PathSegment(id.clone(), syn.PathArguments.None)))
            )
        )
        dstPredicates.push(makeWhereBoundedType(boundedTy, bound))
    }
    for (boundedTy in associatedTypeUsage) {
        dstPredicates.push(makeWhereBoundedType(boundedTy.clone(), bound))
    }
    return dstGenerics
}

public fun withSelfBound(
    cont: Container,
    generics: syn.Generics,
    bound: syn.Path
): syn.Generics {
    val dstGenerics = generics.clone()
    dstGenerics.makeWhereClause().predicates.push(
        syn.WherePredicate.Type(
            syn.PredicateType(
                lifetimes = null,
                boundedTy = typeOfItem(cont),
                colonToken = null,
                bounds = syn.punctuated.Punctuated.fromList(listOf(
                    syn.TypeParamBound.Trait(syn.TraitBound(
                        parenToken = null,
                        modifier = syn.TraitBoundModifier.None,
                        lifetimes = null,
                        path = bound.clone()
                    ))
                ))
            )
        )
    )
    return dstGenerics
}

public fun withLifetimeBound(generics: syn.Generics, lifetime: String): syn.Generics {
    val bound = syn.Lifetime(lifetime, Span.callSite())
    val def = syn.LifetimeParam(
        attrs = emptyList(),
        lifetime = bound.clone(),
        colonToken = null,
        bounds = syn.punctuated.Punctuated.new()
    )

    val params = mutableListOf<GenericParam>()
    params.add(GenericParam.Lifetime(def))
    for (param in generics.params) {
        val p = param.clone()
        when (p) {
            is GenericParam.Lifetime -> {
                p.bounds.push(bound.clone())
            }
            is GenericParam.Type -> {
                p.bounds.push(syn.TypeParamBound.Lifetime(bound.clone()))
            }
            is GenericParam.Const -> {}
        }
        params.add(p)
    }

    return syn.Generics(
        ltToken = generics.ltToken,
        params = syn.punctuated.Punctuated.fromList(params),
        gtToken = generics.gtToken,
        whereClause = generics.whereClause?.clone()
    )
}

private fun typeOfItem(cont: Container): syn.Type {
    val args = cont.generics.params.map { param ->
        when (param) {
            is GenericParam.Type -> GenericArgument.Type(
                syn.Type.Path(syn.TypePath(
                    qself = null,
                    path = syn.Path(
                        leadingColon = null,
                        segments = syn.punctuated.Punctuated.fromList(listOf(syn.PathSegment(param.ident.clone(), syn.PathArguments.None)))
                    )
                ))
            )
            is GenericParam.Lifetime -> GenericArgument.Lifetime(param.lifetime.clone())
            is GenericParam.Const -> error("Serde does not support const generics yet")
        }
    }

    return syn.Type.Path(
        syn.TypePath(
            qself = null,
            path = syn.Path(
                leadingColon = null,
                segments = syn.punctuated.Punctuated.fromList(listOf(
                    syn.PathSegment(
                        ident = cont.ident.clone(),
                        arguments = syn.PathArguments.AngleBracketed(
                            syn.AngleBracketedGenericArguments(
                                colon2Token = null,
                                ltToken = null,
                                args = syn.punctuated.Punctuated.fromList(args),
                                gtToken = null
                            )
                        )
                    )
                ))
            )
        )
    )
}

*/
