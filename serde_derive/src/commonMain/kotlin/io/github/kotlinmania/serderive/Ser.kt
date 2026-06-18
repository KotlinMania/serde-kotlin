package io.github.kotlinmania.serderive

/* STUBBED OUT FOR NOW
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Ident

public fun expandDeriveSerialize(input: DeriveInput): Result<TokenStream> {
    replaceReceiver(input)

    val ctxt = Ctxt()
    val cont = Container.fromAst(ctxt, input, Derive.Serialize, Private.ident())
        ?: return Result.failure(ctxt.check().exceptionOrNull()!!)
    precondition(ctxt, cont)
    ctxt.check().getOrThrow()

    val ident = cont.ident
    val params = Parameters(cont)
    val (implGenerics, tyGenerics, whereClause) = params.generics.splitForImpl()
    val body = Stmts(serializeBody(cont, params))
    val allowDeprecated = allowDeprecated(input)

    val implBlock = if (cont.attrs.remote() != null) {
        val remote = cont.attrs.remote()!!
        val vis = input.vis
        val used = pretendUsed(cont, params.isPacked)
        quote("""
            `#`[automatically_derived]
            `#`allowDeprecated
            impl `#`implGenerics `#`ident `#`tyGenerics `#`whereClause {
                `#`vis fun serialize<__S>(__self: &`#`remote `#`tyGenerics, __serializer: __S): _serde.Private.Result<__S.Ok, __S.Error>
                where
                    __S: _serde.Serializer
                {
                    `#`used
                    `#`body
                }
            }
        """)
    } else {
        quote("""
            `#`[automatically_derived]
            `#`allowDeprecated
            impl `#`implGenerics _serde.Serialize for `#`ident `#`tyGenerics `#`whereClause {
                fun serialize<__S>(&self, __serializer: __S): _serde.Private.Result<__S.Ok, __S.Error>
                where
                    __S: _serde.Serializer
                {
                    `#`body
                }
            }
        """)
    }

    return Result.success(wrapInConst(cont.attrs.customSerdePath(), implBlock))
}

internal fun precondition(cx: Ctxt, cont: Container) {
    when (cont.attrs.identifier()) {
        attr.Identifier.No -> {}
        attr.Identifier.Field -> {
            cx.errorSpannedBy(cont.original, "field identifiers cannot be serialized")
        }
        attr.Identifier.Variant -> {
            cx.errorSpannedBy(cont.original, "variant identifiers cannot be serialized")
        }
    }
}

internal class Parameters(
    val selfVar: Ident,
    val thisType: syn.Path,
    val thisValue: syn.Path,
    val generics: syn.Generics,
    val isRemote: Boolean,
    val isPacked: Boolean
) {
    companion object {
        operator fun invoke(cont: Container): Parameters {
            val isRemote = cont.attrs.remote() != null
            val selfVar = if (isRemote) {
                Ident("__self", Span.callSite())
            } else {
                Ident("self", Span.callSite())
            }

            val thisType = thisType(cont)
            val thisValue = thisValue(cont)
            val isPacked = cont.attrs.isPacked()
            val generics = buildGenerics(cont)

            return Parameters(
                selfVar,
                thisType,
                thisValue,
                generics,
                isRemote,
                isPacked
            )
        }
    }

    fun typeName(): String {
        return thisType.segments.last()!!.ident.toString()
    }
}

internal fun buildGenerics(cont: Container): syn.Generics {
    var generics = withoutDefaults(cont.generics)

    generics = withWherePredicatesFromFields(cont, generics, attr.Field::serBound)
    generics = withWherePredicatesFromVariants(cont, generics, attr.Variant::serBound)

    val serBound = cont.attrs.serBound()
    if (serBound != null) {
        return withWherePredicates(generics, serBound)
    } else {
        return withBound(
            cont,
            generics,
            ::needsSerializeBound,
            parseQuote("_serde.Serialize")
        )
    }
}

internal fun needsSerializeBound(field: attr.Field, variant: attr.Variant?): Boolean {
    return !field.skipSerializing()
        && field.serializeWith() == null
        && field.serBound() == null
        && (variant == null || (
            !variant.skipSerializing()
            && variant.serializeWith() == null
            && variant.serBound() == null
        ))
}

internal fun serializeBody(cont: Container, params: Parameters): Fragment {
    if (cont.attrs.transparent()) {
        return serializeTransparent(cont, params)
    } else if (cont.attrs.typeInto() != null) {
        return serializeInto(params, cont.attrs.typeInto()!!)
    } else {
        return when (val data = cont.data) {
            is Data.Enum -> serializeEnum(params, data.variants, cont.attrs)
            is Data.Struct -> {
                when (data.style) {
                    Style.Struct -> serializeStruct(params, data.fields, cont.attrs)
                    Style.Tuple -> serializeTupleStruct(params, data.fields, cont.attrs)
                    Style.Newtype -> serializeNewtypeStruct(params, data.fields[0], cont.attrs)
                    Style.Unit -> serializeUnitStruct(cont.attrs)
                }
            }
        }
    }
}

internal fun serializeTransparent(cont: Container, params: Parameters): Fragment {
    val fields = (cont.data as Data.Struct).fields
    val selfVar = params.selfVar
    val transparentField = fields.first { it.attrs.transparent() }
    val member = transparentField.member

    val path = transparentField.attrs.serializeWith()?.let { path ->
        quote(path)
    } ?: run {
        val span = transparentField.original.span()
        quoteSpanned(span, "_serde.Serialize.serialize")
    }

    return Fragment.Block(quoteBlock {
        `#`path(&`#`selfVar.`#`member, __serializer)
    })
}

internal fun serializeInto(params: Parameters, typeInto: syn.Type): Fragment {
    val selfVar = params.selfVar
    return Fragment.Block(quoteBlock {
        _serde.Serialize.serialize(
            &_serde.Private.Into::<`#`typeInto>.into(_serde.Private.Clone.clone(`#`selfVar)),
            __serializer
        )
    })
}

internal fun serializeUnitStruct(cattrs: attr.Container): Fragment {
    val typeName = cattrs.name().serializeName()
    return Fragment.Expr(quoteExpr {
        _serde.Serializer.serialize_unit_struct(__serializer, `#`typeName)
    })
}

internal fun serializeNewtypeStruct(params: Parameters, field: Field, cattrs: attr.Container): Fragment {
    val typeName = cattrs.name().serializeName()

    var fieldExpr = getMember(
        params,
        field,
        syn.Member.Unnamed(syn.Index(0u, Span.callSite()))
    )
    if (field.attrs.serializeWith() != null) {
        val path = field.attrs.serializeWith()!!
        fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
    }

    val span = field.original.span()
    val func = quoteSpanned(span, "_serde.Serializer.serialize_newtype_struct")
    return Fragment.Expr(quoteExpr {
        `#`func(__serializer, `#`typeName, `#`fieldExpr)
    })
}

internal fun serializeTupleStruct(params: Parameters, fields: List<Field>, cattrs: attr.Container): Fragment {
    val serializeStmts = serializeTupleStructVisitor(fields, params, false, TupleTrait.SerializeTupleStruct)
    val typeName = cattrs.name().serializeName()

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }
    val letMut = mutIf(serializedFields.isNotEmpty())

    var len = quote("0")
    for ((i, field) in serializedFields.withIndex()) {
        val skipIf = field.attrs.skipSerializingIf()
        val expr = if (skipIf == null) {
            quote("1")
        } else {
            val index = syn.Index(i.toUInt(), Span.callSite())
            val fieldExpr = getMember(params, field, syn.Member.Unnamed(index))
            quote("if (`#`skipIf(`#`fieldExpr)) 0 else 1")
        }
        len = quote("`#`len + `#`expr")
    }

    return Fragment.Block(quoteBlock {
        val `#`letMut __serde_state = _serde.Serializer.serialize_tuple_struct(__serializer, `#`typeName, `#`len)?
        `#`serializeStmts
        _serde.ser.SerializeTupleStruct.end(__serde_state)
    })
}

internal fun serializeStruct(params: Parameters, fields: List<Field>, cattrs: attr.Container): Fragment {
    require(fields.size.toUInt() <= UInt.MAX_VALUE) {
        "too many fields in ${cattrs.name().serializeName()}: ${fields.size}, maximum supported count is ${UInt.MAX_VALUE}"
    }

    val hasNonSkippedFlatten = fields.any { it.attrs.flatten() && !it.attrs.skipSerializing() }
    return if (hasNonSkippedFlatten) {
        serializeStructAsMap(params, fields, cattrs)
    } else {
        serializeStructAsStruct(params, fields, cattrs)
    }
}

internal fun serializeStructTagField(cattrs: attr.Container, structTrait: StructTrait): TokenStream {
    return when (val tag = cattrs.tag()) {
        is attr.TagType.Internal -> {
            val typeName = cattrs.name().serializeName()
            val func = structTrait.serializeField(Span.callSite())
            quote("""
                `#`func(&mut __serde_state, `#`{tag.tag}, `#`typeName)?
            """)
        }
        else -> quote("")
    }
}

internal fun serializeStructAsStruct(params: Parameters, fields: List<Field>, cattrs: attr.Container): Fragment {
    val serializeFields = serializeStructVisitor(fields, params, false, StructTrait.SerializeStruct)
    val typeName = cattrs.name().serializeName()

    val tagField = serializeStructTagField(cattrs, StructTrait.SerializeStruct)
    val tagFieldExists = tagField.toString().isNotEmpty()

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }
    val letMut = mutIf(serializedFields.isNotEmpty() || tagFieldExists)

    var len = quote(if (tagFieldExists) "1 as usize" else "0 as usize")
    for (field in serializedFields) {
        val skipIf = field.attrs.skipSerializingIf()
        val expr = if (skipIf == null) {
            quote("1")
        } else {
            val fieldExpr = getMember(params, field, field.member)
            quote("if (`#`skipIf(`#`fieldExpr)) 0 else 1")
        }
        len = quote("`#`len + `#`expr")
    }

    return Fragment.Block(quoteBlock {
        val `#`letMut __serde_state = _serde.Serializer.serialize_struct(__serializer, `#`typeName, `#`len)?
        `#`tagField
        `#`serializeFields
        _serde.ser.SerializeStruct.end(__serde_state)
    })
}

internal fun serializeStructAsMap(params: Parameters, fields: List<Field>, cattrs: attr.Container): Fragment {
    val serializeFields = serializeStructVisitor(fields, params, false, StructTrait.SerializeMap)

    val tagField = serializeStructTagField(cattrs, StructTrait.SerializeMap)
    val tagFieldExists = tagField.toString().isNotEmpty()

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }
    val letMut = mutIf(serializedFields.isNotEmpty() || tagFieldExists)

    return Fragment.Block(quoteBlock {
        val `#`letMut __serde_state = _serde.Serializer.serialize_map(__serializer, _serde.Private.null)?
        `#`tagField
        `#`serializeFields
        _serde.ser.SerializeMap.end(__serde_state)
    })
}

internal fun serializeEnum(params: Parameters, variants: List<Variant>, cattrs: attr.Container): Fragment {
    require(variants.size.toUInt() <= UInt.MAX_VALUE)

    val selfVar = params.selfVar
    val arms = variants.mapIndexed { index, variant ->
        serializeVariant(params, variant, index.toUInt(), cattrs)
    }.toMutableList()

    if (cattrs.remote() != null && cattrs.nonExhaustive()) {
        arms.add(quote("""
            ref unrecognized -> _serde.Private.Err(_serde.ser.Error.custom(_serde.Private.ser.CannotSerializeVariant(unrecognized)))
        """))
    }

    return Fragment.Expr(quoteExpr {
        when ((*`#`selfVar) ) {
            `#`arms
        }
    })
}

internal fun serializeVariant(
    params: Parameters,
    variant: Variant,
    variantIndex: UInt,
    cattrs: attr.Container
): TokenStream {
    val thisValue = params.thisValue
    val variantIdent = variant.ident

    if (variant.attrs.skipSerializing()) {
        val skippedMsg = "the enum variant ${params.typeName()}::$variantIdent cannot be serialized"
        val skippedErr = quote("""
            _serde.Private.Err(_serde.ser.Error.custom(`#`skippedMsg))
        """)
        val fieldsPat = when (variant.style) {
            Style.Unit -> quote("")
            Style.Newtype, Style.Tuple -> quote("(..)")
            Style.Struct -> quote("{ .. }")
        }
        return quote("""
            `#`thisValue::`#`variantIdent `#`fieldsPat -> `#`skippedErr
        """)
    } else {
        val case = when (variant.style) {
            Style.Unit -> quote("""
                `#`thisValue::`#`variantIdent
            """)
            Style.Newtype -> quote("""
                `#`thisValue::`#`variantIdent(ref __field0)
            """)
            Style.Tuple -> {
                val fieldNames = variant.fields.indices.map { fieldI(it) }
                quote("""
                    `#`thisValue::`#`variantIdent(#(ref `#`fieldNames),*)
                """)
            }
            Style.Struct -> {
                val members = variant.fields.map { it.member }
                quote("""
                    `#`thisValue::`#`variantIdent { #(ref `#`members),* }
                """)
            }
        }

        val tag = cattrs.tag()
        val untagged = variant.attrs.untagged()

        val body = Fragment.Match(
            when {
                tag is attr.TagType.External && !untagged -> serializeExternallyTaggedVariant(params, variant, variantIndex, cattrs)
                tag is attr.TagType.Internal && !untagged -> serializeInternallyTaggedVariant(params, variant, cattrs, tag.tag)
                tag is attr.TagType.Adjacent && !untagged -> io.github.kotlinmania.serderive.ser.enum_adjacently.serializeAdjacentlyTaggedVariant(params, variant, cattrs, variantIndex, tag.tag, tag.content)
                tag is attr.TagType.null || untagged -> io.github.kotlinmania.serderive.ser.enum_untagged.serializeUntaggedVariant(params, variant, cattrs)
                else -> error("unreachable")
            }
        )

        return quote("""
            `#`case -> `#`body
        """)
    }
}

internal fun serializeExternallyTaggedVariant(
    params: Parameters,
    variant: Variant,
    variantIndex: UInt,
    cattrs: attr.Container
): Fragment {
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()

    if (variant.attrs.serializeWith() != null) {
        val path = variant.attrs.serializeWith()!!
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(quoteExpr {
            _serde.Serializer.serialize_newtype_variant(
                __serializer,
                `#`typeName,
                `#`variantIndex,
                `#`variantName,
                `#`ser
            )
        })
    }

    return when (effectiveStyle(variant)) {
        Style.Unit -> Fragment.Expr(quoteExpr {
            _serde.Serializer.serialize_unit_variant(
                __serializer,
                `#`typeName,
                `#`variantIndex,
                `#`variantName
            )
        })
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = quote("__field0")
            if (field.attrs.serializeWith() != null) {
                val path = field.attrs.serializeWith()!!
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = quoteSpanned(span, "_serde.Serializer.serialize_newtype_variant")
            Fragment.Expr(quoteExpr {
                `#`func(
                    __serializer,
                    `#`typeName,
                    `#`variantIndex,
                    `#`variantName,
                    `#`fieldExpr
                )
            })
        }
        Style.Tuple -> io.github.kotlinmania.serderive.ser.tuple.serializeTupleVariant(
            TupleVariant.ExternallyTagged(typeName, variantIndex, variantName),
            params,
            variant.fields
        )
        Style.Struct -> io.github.kotlinmania.serderive.ser.struct_.serializeStructVariant(
            StructVariant.ExternallyTagged(variantIndex, variantName),
            params,
            variant.fields,
            typeName
        )
    }
}

internal fun serializeInternallyTaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container,
    tag: String
): Fragment {
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()

    val enumIdentStr = params.typeName()
    val variantIdentStr = variant.ident.toString()

    if (variant.attrs.serializeWith() != null) {
        val path = variant.attrs.serializeWith()!!
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(quoteExpr {
            _serde.Private.ser.serialize_tagged_newtype(
                __serializer,
                `#`enumIdentStr,
                `#`variantIdentStr,
                `#`tag,
                `#`variantName,
                `#`ser
            )
        })
    }

    return when (effectiveStyle(variant)) {
        Style.Unit -> Fragment.Block(quoteBlock {
            var __struct = _serde.Serializer.serialize_struct(__serializer, `#`typeName, 1)?
            _serde.ser.SerializeStruct.serialize_field(&mut __struct, `#`tag, `#`variantName)?
            _serde.ser.SerializeStruct.end(__struct)
        })
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = quote("__field0")
            if (field.attrs.serializeWith() != null) {
                val path = field.attrs.serializeWith()!!
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = quoteSpanned(span, "_serde.Private.ser.serialize_tagged_newtype")
            Fragment.Expr(quoteExpr {
                `#`func(
                    __serializer,
                    `#`enumIdentStr,
                    `#`variantIdentStr,
                    `#`tag,
                    `#`variantName,
                    `#`fieldExpr
                )
            })
        }
        Style.Struct -> io.github.kotlinmania.serderive.ser.struct_.serializeStructVariant(
            StructVariant.InternallyTagged(tag, variantName),
            params,
            variant.fields,
            typeName
        )
        Style.Tuple -> error("checked in serde_derive_internals")
    }
}

internal fun serializeAdjacentlyTaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container,
    variantIndex: UInt,
    tag: String,
    content: String
): Fragment {
    val thisType = params.thisType
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()
    val serializeVariant = quote("""
        &_serde.Private.ser.AdjacentlyTaggedEnumVariant {
            enum_name: `#`typeName,
            variant_index: `#`variantIndex,
            variant_name: `#`variantName,
        }
    """)

    val inner = Stmts(
        if (variant.attrs.serializeWith() != null) {
            val path = variant.attrs.serializeWith()!!
            val ser = wrapSerializeVariantWith(params, path, variant)
            Fragment.Expr(quoteExpr {
                _serde.Serialize.serialize(`#`ser, __serializer)
            })
        } else {
            when (effectiveStyle(variant)) {
                Style.Unit -> {
                    return Fragment.Block(quoteBlock {
                        var __struct = _serde.Serializer.serialize_struct(__serializer, `#`typeName, 1)?
                        _serde.ser.SerializeStruct.serialize_field(&mut __struct, `#`tag, `#`serializeVariant)?
                        _serde.ser.SerializeStruct.end(__struct)
                    })
                }
                Style.Newtype -> {
                    val field = variant.fields[0]
                    var fieldExpr = quote("__field0")
                    if (field.attrs.serializeWith() != null) {
                        val path = field.attrs.serializeWith()!!
                        fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
                    }

                    val span = field.original.span()
                    val func = quoteSpanned(span, "_serde.ser.SerializeStruct.serialize_field")
                    return Fragment.Block(quoteBlock {
                        var __struct = _serde.Serializer.serialize_struct(__serializer, `#`typeName, 2)?
                        _serde.ser.SerializeStruct.serialize_field(&mut __struct, `#`tag, `#`serializeVariant)?
                        `#`func(&mut __struct, `#`content, `#`fieldExpr)?
                        _serde.ser.SerializeStruct.end(__struct)
                    })
                }
                Style.Tuple -> io.github.kotlinmania.serderive.ser.tuple.serializeTupleVariant(
                    TupleVariant.Untagged, params, variant.fields
                )
                Style.Struct -> io.github.kotlinmania.serderive.ser.struct_.serializeStructVariant(
                    StructVariant.Untagged, params, variant.fields, variantName
                )
            }
        }
    )

    val fieldsTy = variant.fields.map { it.ty }
    val fieldsIdent = when (variant.style) {
        Style.Unit -> {
            if (variant.attrs.serializeWith() != null) {
                emptyList()
            } else {
                error("unreachable")
            }
        }
        Style.Newtype -> listOf(syn.Member.Named(fieldI(0)))
        Style.Tuple -> variant.fields.indices.map { syn.Member.Named(fieldI(it)) }
        Style.Struct -> variant.fields.map { it.member }
    }

    val (_, tyGenerics, whereClause) = params.generics.splitForImpl()

    val wrapperGenerics = if (fieldsIdent.isEmpty()) {
        params.generics.clone()
    } else {
        withLifetimeBound(params.generics, "'__a")
    }
    val (wrapperImplGenerics, wrapperTyGenerics, _) = wrapperGenerics.splitForImpl()

    return Fragment.Block(quoteBlock {
        `#`[doc(hidden)]
        struct __AdjacentlyTagged `#`wrapperGenerics `#`whereClause {
            data: (#(&'__a `#`fieldsTy),*),
            phantom: _serde.Private.PhantomData<`#`thisType `#`tyGenerics>,
        }

        `#`[automatically_derived]
        impl `#`wrapperImplGenerics _serde.Serialize for __AdjacentlyTagged `#`wrapperTyGenerics `#`whereClause {
            fun serialize<__S>(&self, __serializer: __S): _serde.Private.Result<__S.Ok, __S.Error>
            where
                __S: _serde.Serializer
            {
                `#`[allow(unused_variables)]
                val (#(`#`fieldsIdent),*) = self.data
                `#`inner
            }
        }

        var __struct = _serde.Serializer.serialize_struct(__serializer, `#`typeName, 2)?
        _serde.ser.SerializeStruct.serialize_field(&mut __struct, `#`tag, `#`serializeVariant)?
        _serde.ser.SerializeStruct.serialize_field(
            &mut __struct, `#`content, &__AdjacentlyTagged {
                data: (#(`#`fieldsIdent),*),
                phantom: _serde.Private.PhantomData::<`#`thisType `#`tyGenerics>,
            }
        )?
        _serde.ser.SerializeStruct.end(__struct)
    })
}

internal fun serializeUntaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container
): Fragment {
    if (variant.attrs.serializeWith() != null) {
        val path = variant.attrs.serializeWith()!!
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(quoteExpr {
            _serde.Serialize.serialize(`#`ser, __serializer)
        })
    }

    return when (effectiveStyle(variant)) {
        Style.Unit -> Fragment.Expr(quoteExpr {
            _serde.Serializer.serialize_unit(__serializer)
        })
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = quote("__field0")
            if (field.attrs.serializeWith() != null) {
                val path = field.attrs.serializeWith()!!
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = quoteSpanned(span, "_serde.Serialize.serialize")
            Fragment.Expr(quoteExpr {
                `#`func(`#`fieldExpr, __serializer)
            })
        }
        Style.Tuple -> io.github.kotlinmania.serderive.ser.tuple.serializeTupleVariant(
            TupleVariant.Untagged, params, variant.fields
        )
        Style.Struct -> {
            val typeName = cattrs.name().serializeName()
            io.github.kotlinmania.serderive.ser.struct_.serializeStructVariant(
                StructVariant.Untagged, params, variant.fields, typeName
            )
        }
    }
}

}

internal fun serializeTupleVariant(
    context: TupleVariant,
    params: Parameters,
    fields: List<Field>
): Fragment {
    val tupleTrait = when (context) {
        is TupleVariant.ExternallyTagged -> TupleTrait.SerializeTupleVariant
        is TupleVariant.Untagged -> TupleTrait.SerializeTuple
    }

    val serializeStmts = serializeTupleStructVisitor(fields, params, true, tupleTrait)

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }
    val letMut = mutIf(serializedFields.isNotEmpty())

    var len = quote("0")
    for ((i, field) in serializedFields.withIndex()) {
        val skipIf = field.attrs.skipSerializingIf()
        val expr = if (skipIf == null) {
            quote("1")
        } else {
            val fieldExpr = fieldI(i)
            quote("if (`#`skipIf(`#`fieldExpr)) 0 else 1")
        }
        len = quote("`#`len + `#`expr")
    }

    return when (context) {
        is TupleVariant.ExternallyTagged -> Fragment.Block(quoteBlock {
            val `#`letMut __serde_state = _serde.Serializer.serialize_tuple_variant(
                __serializer,
                `#`{context.typeName},
                `#`{context.variantIndex},
                `#`{context.variantName},
                `#`len
            )?
            `#`serializeStmts
            _serde.ser.SerializeTupleVariant.end(__serde_state)
        })
        is TupleVariant.Untagged -> Fragment.Block(quoteBlock {
            val `#`letMut __serde_state = _serde.Serializer.serialize_tuple(
                __serializer,
                `#`len
            )?
            `#`serializeStmts
            _serde.ser.SerializeTuple.end(__serde_state)
        })
    }
}

internal sealed class StructVariant {
    class ExternallyTagged(val variantIndex: UInt, val variantName: Name) : StructVariant()
    class InternallyTagged(val tag: String, val variantName: Name) : StructVariant()
    object Untagged : StructVariant()
}

internal fun serializeStructVariant(
    context: StructVariant,
    params: Parameters,
    fields: List<Field>,
    name: Name
): Fragment {
    if (fields.any { it.attrs.flatten() }) {
        return serializeStructVariantWithFlatten(context, params, fields, name)
    }

    val structTrait = when (context) {
        is StructVariant.ExternallyTagged -> StructTrait.SerializeStructVariant
        is StructVariant.InternallyTagged, is StructVariant.Untagged -> StructTrait.SerializeStruct
    }

    val serializeFields = serializeStructVisitor(fields, params, true, structTrait)

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }
    val letMut = mutIf(serializedFields.isNotEmpty())

    var len = quote("0")
    for (field in serializedFields) {
        val member = field.member
        val skipIf = field.attrs.skipSerializingIf()
        val expr = if (skipIf == null) {
            quote("1")
        } else {
            quote("if (`#`skipIf(`#`member)) 0 else 1")
        }
        len = quote("`#`len + `#`expr")
    }

    return when (context) {
        is StructVariant.ExternallyTagged -> Fragment.Block(quoteBlock {
            val `#`letMut __serde_state = _serde.Serializer.serialize_struct_variant(
                __serializer,
                `#`name,
                `#`{context.variantIndex},
                `#`{context.variantName},
                `#`len
            )?
            `#`serializeFields
            _serde.ser.SerializeStructVariant.end(__serde_state)
        })
        is StructVariant.InternallyTagged -> Fragment.Block(quoteBlock {
            var __serde_state = _serde.Serializer.serialize_struct(
                __serializer,
                `#`name,
                `#`len + 1
            )?
            _serde.ser.SerializeStruct.serialize_field(
                &mut __serde_state,
                `#`{context.tag},
                `#`{context.variantName}
            )?
            `#`serializeFields
            _serde.ser.SerializeStruct.end(__serde_state)
        })
        is StructVariant.Untagged -> Fragment.Block(quoteBlock {
            val `#`letMut __serde_state = _serde.Serializer.serialize_struct(
                __serializer,
                `#`name,
                `#`len
            )?
            `#`serializeFields
            _serde.ser.SerializeStruct.end(__serde_state)
        })
    }
}

internal fun serializeStructVariantWithFlatten(
    context: StructVariant,
    params: Parameters,
    fields: List<Field>,
    name: Name
): Fragment {
    val structTrait = StructTrait.SerializeMap
    val serializeFields = serializeStructVisitor(fields, params, true, structTrait)

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }
    val letMut = mutIf(serializedFields.isNotEmpty())

    return when (context) {
        is StructVariant.ExternallyTagged -> {
            val thisType = params.thisType
            val fieldsTy = fields.map { it.ty }
            val members = fields.map { it.member }

            val (_, tyGenerics, whereClause) = params.generics.splitForImpl()
            val wrapperGenerics = withLifetimeBound(params.generics, "'__a")
            val (wrapperImplGenerics, wrapperTyGenerics, _) = wrapperGenerics.splitForImpl()

            Fragment.Block(quoteBlock {
                `#`[doc(hidden)]
                struct __EnumFlatten `#`wrapperGenerics `#`whereClause {
                    data: (#(&'__a `#`fieldsTy),*),
                    phantom: _serde.Private.PhantomData<`#`thisType `#`tyGenerics>,
                }

                `#`[automatically_derived]
                impl `#`wrapperImplGenerics _serde.Serialize for __EnumFlatten `#`wrapperTyGenerics `#`whereClause {
                    fun serialize<__S>(&self, __serializer: __S): _serde.Private.Result<__S.Ok, __S.Error>
                    where
                        __S: _serde.Serializer
                    {
                        val (#(`#`members),*) = self.data
                        val `#`letMut __serde_state = _serde.Serializer.serialize_map(
                            __serializer,
                            _serde.Private.null
                        )?
                        `#`serializeFields
                        _serde.ser.SerializeMap.end(__serde_state)
                    }
                }

                _serde.Serializer.serialize_newtype_variant(
                    __serializer,
                    `#`name,
                    `#`{context.variantIndex},
                    `#`{context.variantName},
                    &__EnumFlatten {
                        data: (#(`#`members),*),
                        phantom: _serde.Private.PhantomData::<`#`thisType `#`tyGenerics>,
                    }
                )
            })
        }
        is StructVariant.InternallyTagged -> Fragment.Block(quoteBlock {
            val `#`letMut __serde_state = _serde.Serializer.serialize_map(
                __serializer,
                _serde.Private.null
            )?
            _serde.ser.SerializeMap.serialize_entry(
                &mut __serde_state,
                `#`{context.tag},
                `#`{context.variantName}
            )?
            `#`serializeFields
            _serde.ser.SerializeMap.end(__serde_state)
        })
        is StructVariant.Untagged -> Fragment.Block(quoteBlock {
            val `#`letMut __serde_state = _serde.Serializer.serialize_map(
                __serializer,
                _serde.Private.null
            )?
            `#`serializeFields
            _serde.ser.SerializeMap.end(__serde_state)
        })
    }
}

internal fun serializeTupleStructVisitor(
    fields: List<Field>,
    params: Parameters,
    isEnum: Boolean,
    tupleTrait: TupleTrait
): List<TokenStream> {
    val dstFields = mutableListOf<TokenStream>()

    for ((i, field) in fields.withIndex()) {
        if (field.attrs.skipSerializing()) {
            continue
        }

        var fieldExpr = if (isEnum) {
            val id = fieldI(i)
            quote("`#`id")
        } else {
            getMember(params, field, syn.Member.Unnamed(syn.Index(i.toUInt(), Span.callSite())))
        }

        val skip = field.attrs.skipSerializingIf()?.let { path -> quote("`#`path(`#`fieldExpr)") }

        if (field.attrs.serializeWith() != null) {
            val path = field.attrs.serializeWith()!!
            fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
        }

        val span = field.original.span()
        val func = tupleTrait.serializeElement(span)
        val ser = quote("""
            `#`func(&mut __serde_state, `#`fieldExpr)?
        """)

        dstFields.add(if (skip == null) ser else quote("if (!`#`skip) { `#`ser }"))
    }
    return dstFields
}

internal fun serializeStructVisitor(
    fields: List<Field>,
    params: Parameters,
    isEnum: Boolean,
    structTrait: StructTrait
): List<TokenStream> {
    val dstFields = mutableListOf<TokenStream>()

    for (field in fields) {
        if (field.attrs.skipSerializing()) {
            continue
        }
        val member = field.member

        var fieldExpr = if (isEnum) {
            quote("`#`member")
        } else {
            getMember(params, field, member)
        }

        val keyExpr = field.attrs.name().serializeName()
        val skip = field.attrs.skipSerializingIf()?.let { path -> quote("`#`path(`#`fieldExpr)") }

        if (field.attrs.serializeWith() != null) {
            val path = field.attrs.serializeWith()!!
            fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
        }

        val span = field.original.span()
        val ser = if (field.attrs.flatten()) {
            val func = quoteSpanned(span, "_serde.Serialize.serialize")
            quote("""
                `#`func(&`#`fieldExpr, _serde.Private.ser.FlatMapSerializer(&mut __serde_state))?
            """)
        } else {
            val func = structTrait.serializeField(span)
            quote("""
                `#`func(&mut __serde_state, `#`keyExpr, `#`fieldExpr)?
            """)
        }

        if (skip == null) {
            dstFields.add(ser)
        } else {
            val skipFunc = structTrait.skipField(span)
            if (skipFunc != null) {
                dstFields.add(quote("""
                    if (!`#`skip) {
                        `#`ser
                    } else {
                        `#`skipFunc(&mut __serde_state, `#`keyExpr)?
                    }
                """))
            } else {
                dstFields.add(quote("""
                    if (!`#`skip) {
                        `#`ser
                    }
                """))
            }
        }
    }
    return dstFields
}

internal fun wrapSerializeWith(
    params: Parameters,
    serializeWith: syn.ExprPath,
    fieldTys: List<syn.Type>,
    fieldExprs: List<TokenStream>
): TokenStream {
    val thisType = params.thisType
    val (_, tyGenerics, whereClause) = params.generics.splitForImpl()

    val wrapperGenerics = if (fieldExprs.isEmpty()) {
        params.generics.clone()
    } else {
        withLifetimeBound(params.generics, "'__a")
    }
    val (wrapperImplGenerics, wrapperTyGenerics, _) = wrapperGenerics.splitForImpl()

    val fieldAccess = fieldExprs.indices.map { syn.Member.Unnamed(syn.Index(it.toUInt(), Span.callSite())) }

    val selfVar = quote("self")
    val serializerVar = quote("__s")

    val wrapperSerialize = quoteSpanned(serializeWith.span(), "`#`serializeWith(#(`#`selfVar.values.`#`fieldAccess),* `#`serializerVar)")

    return quote("""
        &{
            `#`[doc(hidden)]
            struct __SerializeWith `#`wrapperImplGenerics `#`whereClause {
                values: (#(&'__a `#`fieldTys),*),
                phantom: _serde.Private.PhantomData<`#`thisType `#`tyGenerics>,
            }

            `#`[automatically_derived]
            impl `#`wrapperImplGenerics _serde.Serialize for __SerializeWith `#`wrapperTyGenerics `#`whereClause {
                fun serialize<__S>(&`#`selfVar, `#`serializerVar: __S): _serde.Private.Result<__S.Ok, __S.Error>
                where
                    __S: _serde.Serializer
                {
                    `#`wrapperSerialize
                }
            }

            __SerializeWith {
                values: (#(`#`fieldExprs),*),
                phantom: _serde.Private.PhantomData::<`#`thisType `#`tyGenerics>,
            }
        }
    """)
}

internal fun mutIf(isMut: Boolean): TokenStream? {
    return if (isMut) quote("mut") else null
}

internal fun getMember(params: Parameters, field: Field, member: syn.Member): TokenStream {
    val selfVar = params.selfVar
    val getter = field.attrs.getter()

    return if (!params.isRemote && getter == null) {
        if (params.isPacked) {
            quote("&{`#`selfVar.`#`member}")
        } else {
            quote("&`#`selfVar.`#`member")
        }
    } else if (params.isRemote && getter == null) {
        val inner = if (params.isPacked) {
            quote("&{`#`selfVar.`#`member}")
        } else {
            quote("&`#`selfVar.`#`member")
        }
        val ty = field.ty
        quote("_serde.Private.ser.constrain::<`#`ty>(`#`inner)")
    } else if (params.isRemote && getter != null) {
        val ty = field.ty
        quote("_serde.Private.ser.constrain::<`#`ty>(&`#`getter(`#`selfVar))")
    } else {
        error("getter is only allowed for remote impls")
    }
}

internal enum class StructTrait {
    SerializeMap,
    SerializeStruct,
    SerializeStructVariant;

    fun serializeField(span: Span): TokenStream {
        return when (this) {
            SerializeMap -> quoteSpanned(span, "_serde.ser.SerializeMap.serialize_entry")
            SerializeStruct -> quoteSpanned(span, "_serde.ser.SerializeStruct.serialize_field")
            SerializeStructVariant -> quoteSpanned(span, "_serde.ser.SerializeStructVariant.serialize_field")
        }
    }

    fun skipField(span: Span): TokenStream? {
        return when (this) {
            SerializeMap -> null
            SerializeStruct -> quoteSpanned(span, "_serde.ser.SerializeStruct.skip_field")
            SerializeStructVariant -> quoteSpanned(span, "_serde.ser.SerializeStructVariant.skip_field")
        }
    }
}

internal enum class TupleTrait {
    SerializeTuple,
    SerializeTupleStruct,
    SerializeTupleVariant;

    fun serializeElement(span: Span): TokenStream {
        return when (this) {
            SerializeTuple -> quoteSpanned(span, "_serde.ser.SerializeTuple.serialize_element")
            SerializeTupleStruct -> quoteSpanned(span, "_serde.ser.SerializeTupleStruct.serialize_field")
            SerializeTupleVariant -> quoteSpanned(span, "_serde.ser.SerializeTupleVariant.serialize_field")
        }
    }
}

*/
