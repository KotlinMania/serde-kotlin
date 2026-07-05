// port-lint: source ser.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.appendAll
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.quote.quoteSpanned
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.AttrField
import io.github.kotlinmania.serderive.internals.AttrVariant
import io.github.kotlinmania.serderive.internals.Container
import io.github.kotlinmania.serderive.internals.Ctxt
import io.github.kotlinmania.serderive.internals.Data
import io.github.kotlinmania.serderive.internals.Derive
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Identifier
import io.github.kotlinmania.serderive.internals.Match
import io.github.kotlinmania.serderive.internals.Name
import io.github.kotlinmania.serderive.internals.Pretend
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.TagType
import io.github.kotlinmania.serderive.internals.Variant
import io.github.kotlinmania.serderive.internals.allowDeprecated
import io.github.kotlinmania.serderive.internals.pretendUsed
import io.github.kotlinmania.serderive.internals.replaceReceiver
import io.github.kotlinmania.serderive.internals.thisType
import io.github.kotlinmania.serderive.internals.thisValue
import io.github.kotlinmania.serderive.internals.withBound
import io.github.kotlinmania.serderive.internals.withLifetimeBound
import io.github.kotlinmania.serderive.internals.withWherePredicates
import io.github.kotlinmania.serderive.internals.withWherePredicatesFromFields
import io.github.kotlinmania.serderive.internals.withWherePredicatesFromVariants
import io.github.kotlinmania.serderive.internals.withoutDefaults
import io.github.kotlinmania.serderive.internals.wrapInConst
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Expr
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Index
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.parseQuote
import io.github.kotlinmania.syn.spanned.Spanned

public fun expandDeriveSerialize(input: DeriveInput): TokenStream {
    replaceReceiver(input)

    val ctxt = Ctxt()
    val cont = Container.fromAst(ctxt, input, Derive.Serialize, Private.ident())
    if (cont == null) {
        ctxt.check()
        return TokenStream.new()
    }
    precondition(ctxt, cont)
    ctxt.check()

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
                `#`vis fun serialize<__S>(__self: &`#`remote `#`tyGenerics, __serializer: __S) -> _serde.`#`Private.Result<__S.Ok, __S.Error>
                where
                    __S: _serde.Serializer,
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
                fun serialize<__S>(&self, __serializer: __S) -> _serde.`#`Private.Result<__S.Ok, __S.Error>
                where
                    __S: _serde.Serializer,
                {
                    `#`body
                }
            }
        """)
    }

    return wrapInConst(cont.attrs.serdePath(), implBlock)
}

private fun precondition(cx: Ctxt, cont: Container) {
    when (cont.attrs.identifier()) {
        Identifier.No -> {}
        Identifier.Field -> {
            cx.errorSpannedBy(cont.original, "field identifiers cannot be serialized")
        }
        Identifier.Variant -> {
            cx.errorSpannedBy(cont.original, "variant identifiers cannot be serialized")
        }
    }
}

private class Parameters(cont: Container) {
    // Variable holding the value being serialized. Either `self` for local
    // types or `__self` for remote types.
    val selfVar: Ident

    // Path to the type the impl is for. Either a single Ident for local
    // types (does not include generic parameters) or some::remote::Path for
    // remote types.
    val thisType: io.github.kotlinmania.syn.Path

    // Same as thisType but using ::<T> for generic parameters for use in
    // expression position.
    val thisValue: io.github.kotlinmania.syn.Path

    // Generics including any explicit and inferred bounds for the impl.
    val generics: io.github.kotlinmania.syn.Generics

    // Type has a serde(remote = "...") attribute.
    val isRemote: Boolean

    // Type has a repr(packed) attribute.
    val isPacked: Boolean

    init {
        isRemote = cont.attrs.remote() != null
        selfVar = if (isRemote) {
            Ident.new("__self", Span.callSite())
        } else {
            Ident.new("self", Span.callSite())
        }
        thisType = thisType(cont)
        thisValue = thisValue(cont)
        isPacked = cont.attrs.isPacked()
        generics = buildGenerics(cont)
    }

    // Type name to use in error messages and &'static str arguments to
    // various Serializer methods.
    fun typeName(): String {
        return thisType.segments.last().ident.toString()
    }
}

// All the generics in the input, plus a bound T: Serialize for each generic
// field type that will be serialized by us.
private fun buildGenerics(cont: Container): io.github.kotlinmania.syn.Generics {
    val generics = withoutDefaults(cont.generics)

    val generics = withWherePredicatesFromFields(cont, generics) { field -> field.serBound() }

    val generics = withWherePredicatesFromVariants(cont, generics) { variant -> variant.serBound() }

    return when (val serBound = cont.attrs.serBound()) {
        null -> withBound(
            cont,
            generics,
            ::needsSerializeBound,
            parseQuote("_serde.Serialize")
        )
        else -> withWherePredicates(generics, serBound)
    }
}

// Fields with a skip_serializing or serialize_with attribute, or which
// belong to a variant with a skip_serializing or serialize_with attribute,
// are not serialized by us so we do not generate a bound. Fields with a bound
// attribute specify their own bound so we do not generate one. All other fields
// may need a T: Serialize bound where T is the type of the field.
private fun needsSerializeBound(field: AttrField, variant: AttrVariant?): Boolean {
    return !field.skipSerializing()
        && field.serializeWith() == null
        && field.serBound() == null
        && (variant == null || (!variant.skipSerializing() && variant.serializeWith() == null && variant.serBound() == null))
}

private fun serializeBody(cont: Container, params: Parameters): Fragment {
    if (cont.attrs.transparent()) {
        return serializeTransparent(cont, params)
    }
    cont.attrs.typeInto()?.let { return serializeInto(params, it) }
    return when (val data = cont.data) {
        is Data.Enum -> serializeEnum(params, data.variants, cont.attrs)
        is Data.Struct -> when (data.style) {
            Style.Struct -> serializeStruct(params, data.fields, cont.attrs)
            Style.Tuple -> serializeTupleStruct(params, data.fields, cont.attrs)
            Style.Newtype -> serializeNewtypeStruct(params, data.fields[0], cont.attrs)
            Style.Unit -> serializeUnitStruct(cont.attrs)
        }
    }
}

private fun serializeTransparent(cont: Container, params: Parameters): Fragment {
    val fields = (cont.data as Data.Struct).fields
    val selfVar = params.selfVar
    val transparentField = fields.first { it.attrs.transparent() }
    val member = transparentField.member

    val path = when (val sw = transparentField.attrs.serializeWith()) {
        null -> {
            val span = transparentField.original.span()
            quoteSpanned(span, "_serde.Serialize.serialize")
        }
        else -> quote("`#`sw")
    }

    return Fragment.Block(quote("`#`path(&`#`selfVar.`#`member, __serializer)"))
}

private fun serializeInto(params: Parameters, typeInto: SynType): Fragment {
    val selfVar = params.selfVar
    return Fragment.Block(quote("""
        _serde.Serialize.serialize(
            &_serde.`#`Private.Into::<`#`typeInto>::into(_serde.`#`Private.Clone::clone(`#`selfVar)),
            __serializer)
    """))
}

private fun serializeUnitStruct(cattrs: AttrContainer): Fragment {
    val typeName = cattrs.name().serializeName()
    return Fragment.Expr(quote("_serde.Serializer.serialize_unit_struct(__serializer, `#`typeName)"))
}

private fun serializeNewtypeStruct(
    params: Parameters,
    field: Field,
    cattrs: AttrContainer
): Fragment {
    val typeName = cattrs.name().serializeName()

    var fieldExpr = getMember(
        params,
        field,
        Member.Unnamed(Index(0u))
    )
    field.attrs.serializeWith()?.let { path ->
        fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
    }

    val span = field.original.span()
    val func = quoteSpanned(span, "_serde.Serializer.serialize_newtype_struct")
    return Fragment.Expr(quote("`#`func(__serializer, `#`typeName, `#`fieldExpr)"))
}

private fun serializeTupleStruct(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer
): Fragment {
    val serializeStmts =
        serializeTupleStructVisitor(fields, params, false, TupleTrait.SerializeTupleStruct)

    val typeName = cattrs.name().serializeName()

    val serializedFields = fields.withIndex()
        .filter { (_, field) -> !field.attrs.skipSerializing() }
        .toList()

    val letMut = mutIf(serializedFields.isNotEmpty())

    val len = serializedFields
        .map { (i, field) ->
            when (val path = field.attrs.skipSerializingIf()) {
                null -> quote("1")
                else -> {
                    val index = Index(i.toUInt())
                    val fieldExpr = getMember(params, field, Member.Unnamed(index))
                    quote("if `#`path(`#`fieldExpr) { 0 } else { 1 }")
                }
            }
        }
        .fold(quote("0")) { sum, expr -> quote("`#`sum + `#`expr") }

    val result = TokenStream.new()
    result.appendAll(
        quote("let `#`letMut __serde_state = _serde.Serializer.serialize_tuple_struct(__serializer, `#`typeName, `#`len)?;"),
    )
    for (stmt in serializeStmts) {
        result.appendAll(stmt)
    }
    result.appendAll(quote("_serde.ser.SerializeTupleStruct::end(__serde_state)"))
    return Fragment.Block(result)
}

private fun serializeStruct(params: Parameters, fields: List<Field>, cattrs: AttrContainer): Fragment {
    check(fields.size.toLong() <= UInt.MAX_VALUE.toLong()) {
        "too many fields in ${cattrs.name().serializeName()}: ${fields.size}, maximum supported count is ${UInt.MAX_VALUE}"
    }

    val hasNonSkippedFlatten = fields.any { it.attrs.flatten() && !it.attrs.skipSerializing() }
    return if (hasNonSkippedFlatten) {
        serializeStructAsMap(params, fields, cattrs)
    } else {
        serializeStructAsStruct(params, fields, cattrs)
    }
}

private fun serializeStructTagField(cattrs: AttrContainer, structTrait: StructTrait): TokenStream {
    return when (val tag = cattrs.tag()) {
        is TagType.Internal -> {
            val typeName = cattrs.name().serializeName()
            val func = structTrait.serializeField(Span.callSite())
            quote("`#`func(&mut __serde_state, `#`tag, `#`typeName)?;")
        }
        else -> TokenStream.new()
    }
}

private fun serializeStructAsStruct(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer
): Fragment {
    val serializeFields =
        serializeStructVisitor(fields, params, false, StructTrait.SerializeStruct)

    val typeName = cattrs.name().serializeName()

    val tagField = serializeStructTagField(cattrs, StructTrait.SerializeStruct)
    val tagFieldExists = !tagField.isEmpty()

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }

    val letMut = mutIf(serializedFields.isNotEmpty() || tagFieldExists)

    val len = serializedFields
        .map { field ->
            when (val path = field.attrs.skipSerializingIf()) {
                null -> quote("1")
                else -> {
                    val fieldExpr = getMember(params, field, field.member)
                    quote("if `#`path(`#`fieldExpr) { 0 } else { 1 }")
                }
            }
        }
        .fold(if (tagFieldExists) quote("1") else quote("0")) { sum, expr ->
            quote("`#`sum + `#`expr")
        }

    val result = TokenStream.new()
    result.appendAll(
        quote("let `#`letMut __serde_state = _serde.Serializer::serialize_struct(__serializer, `#`typeName, `#`len)?;"),
    )
    result.appendAll(tagField)
    for (stmt in serializeFields) {
        result.appendAll(stmt)
    }
    result.appendAll(quote("_serde::ser::SerializeStruct::end(__serde_state)"))
    return Fragment.Block(result)
}

private fun serializeStructAsMap(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer
): Fragment {
    val serializeFields =
        serializeStructVisitor(fields, params, false, StructTrait.SerializeMap)

    val tagField = serializeStructTagField(cattrs, StructTrait.SerializeMap)
    val tagFieldExists = !tagField.isEmpty()

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }

    val letMut = mutIf(serializedFields.isNotEmpty() || tagFieldExists)

    val result = TokenStream.new()
    result.appendAll(
        quote("let `#`letMut __serde_state = _serde::Serializer::serialize_map(__serializer, _serde.`#`Private.None)?;"),
    )
    result.appendAll(tagField)
    for (stmt in serializeFields) {
        result.appendAll(stmt)
    }
    result.appendAll(quote("_serde::ser::SerializeMap::end(__serde_state)"))
    return Fragment.Block(result)
}

private fun serializeEnum(params: Parameters, variants: List<Variant>, cattrs: AttrContainer): Fragment {
    check(variants.size.toLong() <= UInt.MAX_VALUE.toLong())

    val selfVar = params.selfVar

    val arms = variants.withIndex()
        .map { (variantIndex, variant) ->
            serializeVariant(params, variant, variantIndex.toUInt(), cattrs)
        }
        .toMutableList()

    if (cattrs.remote() != null && cattrs.nonExhaustive()) {
        arms.add(quote("""
            ref unrecognized => _serde.`#`Private.Err(_serde::ser::Error::custom(_serde.`#`Private.ser::CannotSerializeVariant(unrecognized))),
        """))
    }

    val result = TokenStream.new()
    result.appendAll(quote("match *`#`selfVar {"))
    for (arm in arms) {
        result.appendAll(arm)
    }
    result.appendAll(quote("}"))
    return Fragment.Expr(result)
}

private fun serializeVariant(
    params: Parameters,
    variant: Variant,
    variantIndex: UInt,
    cattrs: AttrContainer
): TokenStream {
    val thisValue = params.thisValue
    val variantIdent = variant.ident

    return if (variant.attrs.skipSerializing()) {
        val skippedMsg = "the enum variant ${params.typeName()}::$variantIdent cannot be serialized"
        val skippedErr = quote("_serde.`#`Private.Err(_serde::ser::Error::custom(`#`skippedMsg))")
        val fieldsPat = when (variant.style) {
            Style.Unit -> TokenStream.new()
            Style.Newtype, Style.Tuple -> quote("(..)")
            Style.Struct -> quote("{ .. }")
        }
        quote("`#`thisValue::`#`variantIdent `#`fieldsPat => `#`skippedErr,")
    } else {
        val case = when (variant.style) {
            Style.Unit -> quote("`#`thisValue::`#`variantIdent")
            Style.Newtype -> quote("`#`thisValue::`#`variantIdent(ref __field0)")
            Style.Tuple -> {
                val fieldNames = (0 until variant.fields.size).map { fieldI(it) }
                quote("`#`thisValue::`#`variantIdent(`#`(`#`fieldNames: ref `#`fieldNames,*)")
            }
            Style.Struct -> {
                val members = variant.fields.map { it.member }
                quote("`#`thisValue::`#`variantIdent { `#`(`#`members: ref `#`members,*) }")
            }
        }

        val body = Match(when (val tag = cattrs.tag()) {
            TagType.External -> if (!variant.attrs.untagged()) {
                serializeExternallyTaggedVariant(params, variant, variantIndex, cattrs)
            } else {
                serializeUntaggedVariant(params, variant, cattrs)
            }
            is TagType.Internal -> if (!variant.attrs.untagged()) {
                serializeInternallyTaggedVariant(params, variant, cattrs, tag.tag)
            } else {
                serializeUntaggedVariant(params, variant, cattrs)
            }
            is TagType.Adjacent -> if (!variant.attrs.untagged()) {
                serializeAdjacentlyTaggedVariant(
                    params,
                    variant,
                    cattrs,
                    variantIndex,
                    tag.tag,
                    tag.content,
                )
            } else {
                serializeUntaggedVariant(params, variant, cattrs)
            }
            TagType.None -> serializeUntaggedVariant(params, variant, cattrs)
        })

        quote("`#`case => `#`body")
    }
}

private fun serializeExternallyTaggedVariant(
    params: Parameters,
    variant: Variant,
    variantIndex: UInt,
    cattrs: AttrContainer
): Fragment {
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()

    variant.attrs.serializeWith()?.let { path ->
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(quote("""
            _serde::Serializer::serialize_newtype_variant(
                __serializer,
                `#`typeName,
                `#`variantIndex,
                `#`variantName,
                `#`ser,
            )
        """))
    }

    return when (effectiveStyle(variant)) {
        Style.Unit -> Fragment.Expr(quote("""
            _serde::Serializer::serialize_unit_variant(
                __serializer,
                `#`typeName,
                `#`variantIndex,
                `#`variantName,
            )
        """))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = quote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = quoteSpanned(span, "_serde::Serializer::serialize_newtype_variant")
            Fragment.Expr(quote("""
                `#`func(
                    __serializer,
                    `#`typeName,
                    `#`variantIndex,
                    `#`variantName,
                    `#`fieldExpr,
                )
            """))
        }
        Style.Tuple -> serializeTupleVariant(
            TupleVariant.ExternallyTagged(typeName, variantIndex, variantName),
            params,
            variant.fields,
        )
        Style.Struct -> serializeStructVariant(
            StructVariant.ExternallyTagged(variantIndex, variantName),
            params,
            variant.fields,
            typeName,
        )
    }
}

private fun serializeInternallyTaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: AttrContainer,
    tag: String
): Fragment {
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()

    val enumIdentStr = params.typeName()
    val variantIdentStr = variant.ident.toString()

    variant.attrs.serializeWith()?.let { path ->
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(quote("""
            _serde.`#`Private.ser::serialize_tagged_newtype(
                __serializer,
                `#`enumIdentStr,
                `#`variantIdentStr,
                `#`tag,
                `#`variantName,
                `#`ser,
            )
        """))
    }

    return when (effectiveStyle(variant)) {
        Style.Unit -> Fragment.Block(quote("""
            let mut __struct = _serde::Serializer::serialize_struct(
                __serializer, `#`typeName, 1)?;
            _serde::ser::SerializeStruct::serialize_field(
                &mut __struct, `#`tag, `#`variantName)?;
            _serde::ser::SerializeStruct::end(__struct)
        """))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = quote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = quoteSpanned(span, "_serde.`#`Private.ser::serialize_tagged_newtype")
            Fragment.Expr(quote("""
                `#`func(
                    __serializer,
                    `#`enumIdentStr,
                    `#`variantIdentStr,
                    `#`tag,
                    `#`variantName,
                    `#`fieldExpr,
                )
            """))
        }
        Style.Struct -> serializeStructVariant(
            StructVariant.InternallyTagged(tag, variantName),
            params,
            variant.fields,
            typeName,
        )
        Style.Tuple -> error("checked in serde_derive_internals")
    }
}

private fun serializeAdjacentlyTaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: AttrContainer,
    variantIndex: UInt,
    tag: String,
    content: String
): Fragment {
    val thisType = params.thisType
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()
    val serializeVariant = quote("""
        &_serde.`#`Private.ser::AdjacentlyTaggedEnumVariant {
            enum_name: `#`typeName,
            variant_index: `#`variantIndex,
            variant_name: `#`variantName,
        }
    """)

    val inner = Stmts(variant.attrs.serializeWith()?.let { path ->
        val ser = wrapSerializeVariantWith(params, path, variant)
        Fragment.Expr(quote("_serde::Serialize::serialize(`#`ser, __serializer)"))
    } ?: when (effectiveStyle(variant)) {
        Style.Unit -> return Fragment.Block(quote("""
            let mut __struct = _serde::Serializer::serialize_struct(
                __serializer, `#`typeName, 1)?;
            _serde::ser::SerializeStruct::serialize_field(
                &mut __struct, `#`tag, `#`serializeVariant)?;
            _serde::ser::SerializeStruct::end(__struct)
        """))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = quote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = quoteSpanned(span, "_serde::ser::SerializeStruct::serialize_field")
            return Fragment.Block(quote("""
                let mut __struct = _serde::Serializer::serialize_struct(
                    __serializer, `#`typeName, 2)?;
                _serde::ser::SerializeStruct::serialize_field(
                    &mut __struct, `#`tag, `#`serializeVariant)?;
                `#`func(
                    &mut __struct, `#`content, `#`fieldExpr)?;
                _serde::ser::SerializeStruct::end(__struct)
            """))
        }
        Style.Tuple -> serializeTupleVariant(TupleVariant.Untagged, params, variant.fields)
        Style.Struct -> serializeStructVariant(
            StructVariant.Untagged,
            params,
            variant.fields,
            variantName,
        )
    })

    val fieldsTy = variant.fields.map { it.ty }
    val fieldsIdent: List<Member> = when (variant.style) {
        Style.Unit -> {
            if (variant.attrs.serializeWith() != null) {
                emptyList()
            } else {
                error("checked in serde_derive_internals")
            }
        }
        Style.Newtype -> listOf(Member.Named(fieldI(0)))
        Style.Tuple -> (0 until variant.fields.size).map { Member.Named(fieldI(it)) }
        Style.Struct -> variant.fields.map { it.member }
    }

    val (_, tyGenerics, whereClause) = params.generics.splitForImpl()

    val wrapperGenerics = if (fieldsIdent.isEmpty()) {
        params.generics.clone()
    } else {
        withLifetimeBound(params.generics, "'__a")
    }
    val (wrapperImplGenerics, wrapperTyGenerics, _) = wrapperGenerics.splitForImpl()

    return Fragment.Block(quote("""
        `#`[doc(hidden)]
        struct __AdjacentlyTagged `#`wrapperGenerics `#`whereClause {
            data: (`#`(&'__a `#`fieldsTy,)*),
            phantom: _serde.`#`Private.PhantomData<`#`thisType `#`tyGenerics>,
        }

        `#`[automatically_derived]
        impl `#`wrapperImplGenerics _serde::Serialize for __AdjacentlyTagged `#`wrapperTyGenerics `#`whereClause {
            fn serialize<__S>(&self, __serializer: __S) -> _serde.`#`Private.Result<__S::Ok, __S::Error>
            where
                __S: _serde::Serializer,
            {
                `#`[allow(unused_variables)]
                let (`#`(`#`fieldsIdent: `#`fieldsIdent,)*) = self.data;
                `#`inner
            }
        }

        let mut __struct = _serde::Serializer::serialize_struct(
            __serializer, `#`typeName, 2)?;
        _serde::ser::SerializeStruct::serialize_field(
            &mut __struct, `#`tag, `#`serializeVariant)?;
        _serde::ser::SerializeStruct::serialize_field(
            &mut __struct, `#`content, &__AdjacentlyTagged {
                data: (`#`(`#`fieldsIdent,)*),
                phantom: _serde.`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
            })?;
        _serde::ser::SerializeStruct::end(__struct)
    """))
}

private fun serializeUntaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: AttrContainer
): Fragment {
    variant.attrs.serializeWith()?.let { path ->
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(quote("_serde::Serialize::serialize(`#`ser, __serializer)"))
    }

    return when (effectiveStyle(variant)) {
        Style.Unit -> Fragment.Expr(quote("_serde::Serializer::serialize_unit(__serializer)"))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = quote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = quoteSpanned(span, "_serde::Serialize::serialize")
            Fragment.Expr(quote("`#`func(`#`fieldExpr, __serializer)"))
        }
        Style.Tuple -> serializeTupleVariant(TupleVariant.Untagged, params, variant.fields)
        Style.Struct -> {
            val typeName = cattrs.name().serializeName()
            serializeStructVariant(StructVariant.Untagged, params, variant.fields, typeName)
        }
    }
}

private sealed class TupleVariant {
    class ExternallyTagged(val typeName: Name, val variantIndex: UInt, val variantName: Name) : TupleVariant()
    object Untagged : TupleVariant()
}

private fun serializeTupleVariant(
    context: TupleVariant,
    params: Parameters,
    fields: List<Field>
): Fragment {
    val tupleTrait = when (context) {
        is TupleVariant.ExternallyTagged -> TupleTrait.SerializeTupleVariant
        TupleVariant.Untagged -> TupleTrait.SerializeTuple
    }

    val serializeStmts = serializeTupleStructVisitor(fields, params, true, tupleTrait)

    val serializedFields = fields.withIndex()
        .filter { (_, field) -> !field.attrs.skipSerializing() }
        .toList()

    val letMut = mutIf(serializedFields.isNotEmpty())

    val len = serializedFields
        .map { (i, field) ->
            when (val path = field.attrs.skipSerializingIf()) {
                null -> quote("1")
                else -> {
                    val fieldExpr = fieldI(i)
                    quote("if `#`path(`#`fieldExpr) { 0 } else { 1 }")
                }
            }
        }
        .fold(quote("0")) { sum, expr -> quote("`#`sum + `#`expr") }

    return when (context) {
        is TupleVariant.ExternallyTagged -> {
            val typeName = context.typeName
            val variantIndex = context.variantIndex
            val variantName = context.variantName
            val result = TokenStream.new()
            result.appendAll(quote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_tuple_variant(
                    __serializer,
                    `#`typeName,
                    `#`variantIndex,
                    `#`variantName,
                    `#`len)?;
            """))
            for (stmt in serializeStmts) {
                result.appendAll(stmt)
            }
            result.appendAll(quote("_serde::ser::SerializeTupleVariant::end(__serde_state)"))
            Fragment.Block(result)
        }
        TupleVariant.Untagged -> {
            val result = TokenStream.new()
            result.appendAll(quote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_tuple(
                    __serializer,
                    `#`len)?;
            """))
            for (stmt in serializeStmts) {
                result.appendAll(stmt)
            }
            result.appendAll(quote("_serde::ser::SerializeTuple::end(__serde_state)"))
            Fragment.Block(result)
        }
    }
}

private sealed class StructVariant {
    class ExternallyTagged(val variantIndex: UInt, val variantName: Name) : StructVariant()
    class InternallyTagged(val tag: String, val variantName: Name) : StructVariant()
    object Untagged : StructVariant()
}

private fun serializeStructVariant(
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
        is StructVariant.InternallyTagged, StructVariant.Untagged -> StructTrait.SerializeStruct
    }

    val serializeFields = serializeStructVisitor(fields, params, true, structTrait)

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }

    val letMut = mutIf(serializedFields.isNotEmpty())

    val len = serializedFields
        .map { field ->
            val member = field.member
            when (val path = field.attrs.skipSerializingIf()) {
                null -> quote("1")
                else -> quote("if `#`path(`#`member) { 0 } else { 1 }")
            }
        }
        .fold(quote("0")) { sum, expr -> quote("`#`sum + `#`expr") }

    return when (context) {
        is StructVariant.ExternallyTagged -> {
            val variantIndex = context.variantIndex
            val variantName = context.variantName
            val result = TokenStream.new()
            result.appendAll(quote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_struct_variant(
                    __serializer,
                    `#`name,
                    `#`variantIndex,
                    `#`variantName,
                    `#`len,
                )?;
            """))
            for (stmt in serializeFields) {
                result.appendAll(stmt)
            }
            result.appendAll(quote("_serde::ser::SerializeStructVariant::end(__serde_state)"))
            Fragment.Block(result)
        }
        is StructVariant.InternallyTagged -> {
            val tag = context.tag
            val variantName = context.variantName
            val result = TokenStream.new()
            result.appendAll(quote("""
                let mut __serde_state = _serde::Serializer::serialize_struct(
                    __serializer,
                    `#`name,
                    `#`len + 1,
                )?;
            """))
            result.appendAll(quote("""
                _serde::ser::SerializeStruct::serialize_field(
                    &mut __serde_state,
                    `#`tag,
                    `#`variantName,
                )?;
            """))
            for (stmt in serializeFields) {
                result.appendAll(stmt)
            }
            result.appendAll(quote("_serde::ser::SerializeStruct::end(__serde_state)"))
            Fragment.Block(result)
        }
        StructVariant.Untagged -> {
            val result = TokenStream.new()
            result.appendAll(quote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_struct(
                    __serializer,
                    `#`name,
                    `#`len,
                )?;
            """))
            for (stmt in serializeFields) {
                result.appendAll(stmt)
            }
            result.appendAll(quote("_serde::ser::SerializeStruct::end(__serde_state)"))
            Fragment.Block(result)
        }
    }
}

private fun serializeStructVariantWithFlatten(
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
            val variantIndex = context.variantIndex
            val variantName = context.variantName
            val thisType = params.thisType
            val fieldsTy = fields.map { it.ty }
            val members = fields.map { it.member }

            val (_, tyGenerics, whereClause) = params.generics.splitForImpl()
            val wrapperGenerics = withLifetimeBound(params.generics, "'__a")
            val (wrapperImplGenerics, wrapperTyGenerics, _) = wrapperGenerics.splitForImpl()

            Fragment.Block(quote("""
                `#`[doc(hidden)]
                struct __EnumFlatten `#`wrapperGenerics `#`whereClause {
                    data: (`#`(&'__a `#`fieldsTy,)*),
                    phantom: _serde.`#`Private.PhantomData<`#`thisType `#`tyGenerics>,
                }

                `#`[automatically_derived]
                impl `#`wrapperImplGenerics _serde::Serialize for __EnumFlatten `#`wrapperTyGenerics `#`whereClause {
                    fn serialize<__S>(&self, __serializer: __S) -> _serde.`#`Private.Result<__S::Ok, __S::Error>
                    where
                        __S: _serde::Serializer,
                    {
                        let (`#`(`#`members: `#`members,)*) = self.data;
                        let `#`letMut __serde_state = _serde::Serializer::serialize_map(
                            __serializer,
                            _serde.`#`Private.None)?;
                        `#`(`#`serializeFields)*
                        _serde::ser::SerializeMap::end(__serde_state)
                    }
                }

                _serde::Serializer::serialize_newtype_variant(
                    __serializer,
                    `#`name,
                    `#`variantIndex,
                    `#`variantName,
                    &__EnumFlatten {
                        data: (`#`(`#`members,)*),
                        phantom: _serde.`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
                    })
            """))
        }
        is StructVariant.InternallyTagged -> {
            val tag = context.tag
            val variantName = context.variantName
            val result = TokenStream.new()
            result.appendAll(quote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_map(
                    __serializer,
                    _serde.`#`Private.None)?;
            """))
            result.appendAll(quote("""
                _serde::ser::SerializeMap::serialize_entry(
                    &mut __serde_state,
                    `#`tag,
                    `#`variantName,
                )?;
            """))
            for (stmt in serializeFields) {
                result.appendAll(stmt)
            }
            result.appendAll(quote("_serde::ser::SerializeMap::end(__serde_state)"))
            Fragment.Block(result)
        }
        StructVariant.Untagged -> {
            val result = TokenStream.new()
            result.appendAll(quote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_map(
                    __serializer,
                    _serde.`#`Private.None)?;
            """))
            for (stmt in serializeFields) {
                result.appendAll(stmt)
            }
            result.appendAll(quote("_serde::ser::SerializeMap::end(__serde_state)"))
            Fragment.Block(result)
        }
    }
}

private fun serializeTupleStructVisitor(
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
            getMember(
                params,
                field,
                Member.Unnamed(Index(i.toUInt()))
            )
        }

        val skip = field.attrs.skipSerializingIf()?.let { path ->
            quote("`#`path(`#`fieldExpr)")
        }

        field.attrs.serializeWith()?.let { path ->
            fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
        }

        val span = field.original.span()
        val func = tupleTrait.serializeElement(span)
        val ser = quote("`#`func(&mut __serde_state, `#`fieldExpr)?;")

        dstFields.add(when (skip) {
            null -> ser
            else -> quote("if !`#`skip { `#`ser }")
        })
    }
    return dstFields
}

private fun serializeStructVisitor(
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

        val skip = field.attrs.skipSerializingIf()?.let { path ->
            quote("`#`path(`#`fieldExpr)")
        }

        field.attrs.serializeWith()?.let { path ->
            fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
        }

        val span = field.original.span()
        val ser = if (field.attrs.flatten()) {
            val func = quoteSpanned(span, "_serde::Serialize::serialize")
            quote("`#`func(&`#`fieldExpr, _serde.`#`Private.ser::FlatMapSerializer(&mut __serde_state))?;")
        } else {
            val func = structTrait.serializeField(span)
            quote("`#`func(&mut __serde_state, `#`keyExpr, `#`fieldExpr)?;")
        }

        dstFields.add(when (skip) {
            null -> ser
            else -> {
                val skipFunc = structTrait.skipField(span)
                if (skipFunc != null) {
                    quote("""
                        if !`#`skip {
                            `#`ser
                        } else {
                            `#`skipFunc(&mut __serde_state, `#`keyExpr)?;
                        }
                    """)
                } else {
                    quote("""
                        if !`#`skip {
                            `#`ser
                        }
                    """)
                }
            }
        })
    }
    return dstFields
}

private fun wrapSerializeFieldWith(
    params: Parameters,
    fieldTy: SynType,
    serializeWith: Expr.Path,
    fieldExpr: TokenStream
): TokenStream {
    return wrapSerializeWith(params, serializeWith, listOf(fieldTy), listOf(quote("`#`fieldExpr")))
}

private fun wrapSerializeVariantWith(
    params: Parameters,
    serializeWith: Expr.Path,
    variant: Variant
): TokenStream {
    val fieldTys = variant.fields.map { it.ty }
    val fieldExprs = variant.fields.map { field ->
        val id = when (val member = field.member) {
            is Member.Named -> member.ident.clone()
            is Member.Unnamed -> fieldI(member.index.index.toInt())
        }
        quote("`#`id")
    }
    return wrapSerializeWith(params, serializeWith, fieldTys, fieldExprs)
}

private fun wrapSerializeWith(
    params: Parameters,
    serializeWith: Expr.Path,
    fieldTys: List<SynType>,
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

    val fieldAccess = (0 until fieldExprs.size).map { n ->
        Member.Unnamed(Index(n.toUInt()))
    }

    val selfVar = quote("self")
    val serializerVar = quote("__s")

    // If the serialize_with path returns the wrong type, the error will be
    // reported on this piece. We attach the span of the path so the error
    // will be reported on the #[serde(with = "...")]
    //                       ^^^^^
    val wrapperSerialize = quoteSpanned(serializeWith.span(), """
        `#`serializeWith(`#`(`#`selfVar.values.`#`fieldAccess, )* `#`serializerVar)
    """)

    return quote("""
        &{
            `#`[doc(hidden)]
            struct __SerializeWith `#`wrapperImplGenerics `#`whereClause {
                values: (`#`(&'__a `#`fieldTys, )*),
                phantom: _serde.`#`Private.PhantomData<`#`thisType `#`tyGenerics>,
            }

            `#`[automatically_derived]
            impl `#`wrapperImplGenerics _serde::Serialize for __SerializeWith `#`wrapperTyGenerics `#`whereClause {
                fn serialize<__S>(&`#`selfVar, `#`serializerVar: __S) -> _serde.`#`Private.Result<__S::Ok, __S::Error>
                where
                    __S: _serde::Serializer,
                {
                    `#`wrapperSerialize
                }
            }

            __SerializeWith {
                values: (`#`(`#`fieldExprs, )*),
                phantom: _serde.`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
            }
        }
    """)
}

// Serialization of an empty struct results in code like:
//
//     let mut __serde_state = serializer.serialize_struct("S", 0)?;
//     _serde::ser::SerializeStruct::end(__serde_state)
//
// where we want to omit the mut to avoid a warning.
private fun mutIf(isMut: Boolean): TokenStream? {
    return if (isMut) {
        quote("mut")
    } else {
        null
    }
}

private fun getMember(params: Parameters, field: Field, member: Member): TokenStream {
    val selfVar = params.selfVar
    return when {
        !params.isRemote && field.attrs.getter() == null -> {
            if (params.isPacked) {
                quote("&{ `#`selfVar.`#`member }")
            } else {
                quote("&`#`selfVar.`#`member")
            }
        }
        params.isRemote && field.attrs.getter() == null -> {
            val inner = if (params.isPacked) {
                quote("&{ `#`selfVar.`#`member }")
            } else {
                quote("&`#`selfVar.`#`member")
            }
            val ty = field.ty
            quote("_serde.`#`Private.ser::constrain::<`#`ty>(`#`inner)")
        }
        params.isRemote -> {
            val ty = field.ty
            val getter = field.attrs.getter()!!
            quote("_serde.`#`Private.ser::constrain::<`#`ty>(&`#`getter(`#`selfVar))")
        }
        else -> {
            error("getter is only allowed for remote impls")
        }
    }
}

private fun effectiveStyle(variant: Variant): Style {
    return if (variant.style == Style.Newtype && variant.fields[0].attrs.skipSerializing()) {
        Style.Unit
    } else {
        variant.style
    }
}

private enum class StructTrait {
    SerializeMap,
    SerializeStruct,
    SerializeStructVariant;

    fun serializeField(span: Span): TokenStream {
        return when (this) {
            SerializeMap -> quoteSpanned(span, "_serde::ser::SerializeMap::serialize_entry")
            SerializeStruct -> quoteSpanned(span, "_serde::ser::SerializeStruct::serialize_field")
            SerializeStructVariant -> quoteSpanned(span, "_serde::ser::SerializeStructVariant::serialize_field")
        }
    }

    fun skipField(span: Span): TokenStream? {
        return when (this) {
            SerializeMap -> null
            SerializeStruct -> quoteSpanned(span, "_serde::ser::SerializeStruct::skip_field")
            SerializeStructVariant -> quoteSpanned(span, "_serde::ser::SerializeStructVariant::skip_field")
        }
    }
}

private enum class TupleTrait {
    SerializeTuple,
    SerializeTupleStruct,
    SerializeTupleVariant;

    fun serializeElement(span: Span): TokenStream {
        return when (this) {
            SerializeTuple -> quoteSpanned(span, "_serde::ser::SerializeTuple::serialize_element")
            SerializeTupleStruct -> quoteSpanned(span, "_serde::ser::SerializeTupleStruct::serialize_field")
            SerializeTupleVariant -> quoteSpanned(span, "_serde::ser::SerializeTupleVariant::serialize_field")
        }
    }
}