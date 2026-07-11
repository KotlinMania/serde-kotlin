// port-lint: source ser.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.serderive.checkedQuote
import io.github.kotlinmania.serderive.checkedQuoteSpanned
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Container
import io.github.kotlinmania.serderive.internals.Ctxt
import io.github.kotlinmania.serderive.internals.Data
import io.github.kotlinmania.serderive.internals.Derive
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.serderive.internals.Identifier
import io.github.kotlinmania.serderive.internals.Match
import io.github.kotlinmania.serderive.internals.Name
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.TagType
import io.github.kotlinmania.serderive.internals.Variant
import io.github.kotlinmania.serderive.internals.allowDeprecated
import io.github.kotlinmania.serderive.internals.pretendUsed
import io.github.kotlinmania.serderive.internals.replaceReceiver
import io.github.kotlinmania.serderive.internals.thisType as thisTypeFn
import io.github.kotlinmania.serderive.internals.thisValue as thisValueFn
import io.github.kotlinmania.serderive.internals.withBound
import io.github.kotlinmania.serderive.internals.withLifetimeBound
import io.github.kotlinmania.serderive.internals.withWherePredicates
import io.github.kotlinmania.serderive.internals.withWherePredicatesFromFields
import io.github.kotlinmania.serderive.internals.withWherePredicatesFromVariants
import io.github.kotlinmania.serderive.internals.withoutDefaults
import io.github.kotlinmania.serderive.internals.wrapInConst
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Expr
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Index
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathParse
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.parse2
import io.github.kotlinmania.syn.span

public fun expandDeriveSerialize(input: DeriveInput): TokenStream {
    val rewrittenInput = replaceReceiver(input)

    val ctxt = Ctxt()
    val cont = Container.fromAst(ctxt, rewrittenInput, Derive.Serialize, Private.ident())
    if (cont == null) {
        ctxt.check()
        return TokenStream.new()
    }
    precondition(ctxt, cont)
    ctxt.check()

    val ident = cont.ident
    val params = SerParameters(cont)
    val implGenerics = generatedImplGenerics(params.generics)
    val tyGenerics = generatedTypeGenerics(params.generics)
    val whereClause = generatedWhereClause(params.generics.whereClause)
    val body = Stmts(serializeBody(cont, params))
    val allowDeprecated = allowDeprecated(rewrittenInput)

    val implBlock = if (cont.attrs.remote() != null) {
        val remote = cont.attrs.remote()!!
        val vis = rewrittenInput.vis
        val used = pretendUsed(cont, params.isPacked)
        checkedQuote("""
            #[automatically_derived]
            `#`allowDeprecated
            impl `#`implGenerics `#`ident `#`tyGenerics `#`whereClause {
                `#`vis fn serialize<__S>(__self: &`#`remote `#`tyGenerics, __serializer: __S) -> _serde::`#`Private::Result<__S::Ok, __S::Error>
                where
                    __S: _serde::Serializer,
                {
                    `#`used
                    `#`body
                }
            }
        """, mapOf(
            "allowDeprecated" to allowDeprecated,
            "implGenerics" to implGenerics,
            "ident" to ident,
            "tyGenerics" to tyGenerics,
            "whereClause" to whereClause,
            "vis" to vis,
            "remote" to remote,
            "Private" to Private,
            "used" to used,
            "body" to body,
        ))
    } else {
        checkedQuote("""
            #[automatically_derived]
            `#`allowDeprecated
            impl `#`implGenerics _serde::Serialize for `#`ident `#`tyGenerics `#`whereClause {
                fn serialize<__S>(&self, __serializer: __S) -> _serde::`#`Private::Result<__S::Ok, __S::Error>
                where
                    __S: _serde::Serializer,
                {
                    `#`body
                }
            }
        """, mapOf(
            "allowDeprecated" to allowDeprecated,
            "implGenerics" to implGenerics,
            "ident" to ident,
            "tyGenerics" to tyGenerics,
            "whereClause" to whereClause,
            "Private" to Private,
            "body" to body,
        ))
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

private class SerParameters(cont: Container) {
    // Variable holding the value being serialized. Either the receiver for local
    // types or the remote receiver alias for remote types.
    val selfVar: Ident = if (cont.attrs.remote() != null) {
        Ident.new("__self", Span.callSite())
    } else {
        Ident.new("self", Span.callSite())
    }

    // Path to the type the implementation is for. Either a single Ident for local
    // types (does not include generic parameters) or a remote path for
    // remote types.
    val thisType: Path = thisTypeFn(cont)

    // Same as thisType but using explicit generic parameters for use in
    // expression position.
    val thisValue: Path = thisValueFn(cont)

    // Generics including any explicit and inferred bounds for the impl.
    val generics: Generics = buildGenerics(cont)

    // Type has a serde remote attribute.
    val isRemote: Boolean = cont.attrs.remote() != null

    // Type has a packed representation attribute.
    val isPacked: Boolean = cont.attrs.isPacked()

    // Type name to use in error messages and static string arguments to
    // various Serializer methods.
    fun typeName(): String {
        return thisType.segments.last()!!.ident.toString()
    }
}

// All the generics in the input, plus a Serialize bound for each generic
// field type that will be serialized by us.
private fun buildGenerics(cont: Container): Generics {
    val g0 = withoutDefaults(cont.generics)

    val g1 = withWherePredicatesFromFields(cont, g0) { field -> field.serBound() }

    val g2 = withWherePredicatesFromVariants(cont, g1) { variant -> variant.serBound() }

    return when (val serBound = cont.attrs.serBound()) {
        null -> withBound(
            cont,
            g2,
            ::needsSerializeBound,
            parseQuotePath("_serde.Serialize")
        )
        else -> withWherePredicates(g2, serBound)
    }
}

// Parse a quote string into a Path, equivalent to the upstream parse-quote macro.
private fun parseQuotePath(template: String): Path {
    val tokens = checkedQuote(template.replace(".", "::"))
    val result = parse2(PathParse, tokens)
    return result.getOrThrow()
}

// Fields with a skipSerializing or serializeWith attribute, or which
// belong to a variant with a skipSerializing or serializeWith attribute,
// are not serialized by us so we do not generate a bound. Fields with a bound
// attribute specify their own bound so we do not generate one. All other fields
// may need a Serialize bound where T is the type of the field.
private fun needsSerializeBound(field: Field, variant: Variant?): Boolean {
    return !field.attrs.skipSerializing()
        && field.attrs.serializeWith() == null
        && field.attrs.serBound() == null
        && (variant == null || (!variant.attrs.skipSerializing() && variant.attrs.serializeWith() == null && variant.attrs.serBound() == null))
}

private fun serializeBody(cont: Container, params: SerParameters): Fragment {
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

private fun serializeTransparent(cont: Container, params: SerParameters): Fragment {
    val fields = (cont.data as Data.Struct).fields
    val selfVar = params.selfVar
    val transparentField = fields.first { it.attrs.transparent() }
    val member = transparentField.member

    val path = when (val sw = transparentField.attrs.serializeWith()) {
        null -> {
            val span = transparentField.original.span()
            checkedQuoteSpanned(span, "_serde.Serialize.serialize")
        }
        else -> checkedQuote("`#`sw")
    }

    return Fragment.Block(checkedQuote("`#`path(&`#`selfVar.`#`member, __serializer)"))
}

private fun serializeInto(params: SerParameters, typeInto: SynType): Fragment {
    val selfVar = params.selfVar
    return Fragment.Block(checkedQuote("""
        _serde.Serialize.serialize(
            &_serde::`#`Private::Into::<`#`typeInto>::into(_serde::`#`Private::Clone::clone(`#`selfVar)),
            __serializer)
    """))
}

private fun serializeUnitStruct(cattrs: AttrContainer): Fragment {
    val typeName = cattrs.name().serializeName()
    return Fragment.Expr(checkedQuote("_serde::Serializer::serialize_unit_struct(__serializer, `#`typeName)"))
}

private fun serializeNewtypeStruct(
    params: SerParameters,
    field: Field,
    cattrs: AttrContainer
): Fragment {
    val typeName = cattrs.name().serializeName()

    var fieldExpr = getMember(
        params,
        field,
        Member.Unnamed(Index(0u, Span.callSite()))
    )
    field.attrs.serializeWith()?.let { path ->
        fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
    }

    val span = field.original.span()
    val func = checkedQuoteSpanned(span, "_serde::Serializer::serialize_newtype_struct")
    return Fragment.Expr(
        checkedQuote(
            "`#`func(__serializer, `#`typeName, `#`fieldExpr)",
            mapOf("func" to func, "typeName" to typeName, "fieldExpr" to fieldExpr),
        )
    )
}

private fun serializeTupleStruct(
    params: SerParameters,
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
    val zero = rustUsizeLiteral(0)
    val one = rustUsizeLiteral(1)

    val len = serializedFields
        .map { (i, field) ->
            when (val path = field.attrs.skipSerializingIf()) {
                null -> one
                else -> {
                    val index = Index(i.toUInt(), Span.callSite())
                    val fieldExpr = getMember(params, field, Member.Unnamed(index))
                    checkedQuote(
                        "if `#`path(`#`fieldExpr) { `#`zero } else { `#`one }",
                        mapOf("path" to path, "fieldExpr" to fieldExpr, "zero" to zero, "one" to one),
                    )
                }
            }
        }
        .fold(zero) { sum, expr -> checkedQuote("`#`sum + `#`expr", "sum" to sum, "expr" to expr) }

    val stmts = checkedQuote("`#`(`#`serializeStmts)*", "serializeStmts" to serializeStmts)
    return Fragment.Block(checkedQuote("""
        let `#`letMut __serde_state = _serde::Serializer::serialize_tuple_struct(__serializer, `#`typeName, `#`len)?;
        `#`stmts
        _serde::ser::SerializeTupleStruct::end(__serde_state)
    """, mapOf("letMut" to letMut, "typeName" to typeName, "len" to len, "stmts" to stmts)))
}

private fun serializeStruct(params: SerParameters, fields: List<Field>, cattrs: AttrContainer): Fragment {
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
            checkedQuote(
                "`#`func(&mut __serde_state, `#`tag, `#`typeName)?;",
                mapOf("func" to func, "tag" to tag.tag, "typeName" to typeName),
            )
        }
        else -> TokenStream.new()
    }
}

private fun serializeStructAsStruct(
    params: SerParameters,
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
    val zero = rustUsizeLiteral(0)
    val one = rustUsizeLiteral(1)

    val len = serializedFields
        .map { field ->
            when (val path = field.attrs.skipSerializingIf()) {
                null -> one
                else -> {
                    val fieldExpr = getMember(params, field, field.member)
                    checkedQuote(
                        "if `#`path(`#`fieldExpr) { `#`zero } else { `#`one }",
                        mapOf("path" to path, "fieldExpr" to fieldExpr, "zero" to zero, "one" to one),
                    )
                }
            }
        }
        .fold(if (tagFieldExists) one else zero) { sum, expr ->
            checkedQuote("`#`sum + `#`expr", mapOf("sum" to sum, "expr" to expr))
        }

    return Fragment.Block(checkedQuote("""
        let `#`letMut __serde_state = _serde::Serializer::serialize_struct(__serializer, `#`typeName, `#`len)?;
        `#`tagField
        `#`(`#`serializeFields)*
        _serde::ser::SerializeStruct::end(__serde_state)
    """, mapOf(
        "letMut" to letMut,
        "typeName" to typeName,
        "len" to len,
        "tagField" to tagField,
        "serializeFields" to serializeFields,
    )))
}

private fun serializeStructAsMap(
    params: SerParameters,
    fields: List<Field>,
    cattrs: AttrContainer
): Fragment {
    val serializeFields =
        serializeStructVisitor(fields, params, false, StructTrait.SerializeMap)

    val tagField = serializeStructTagField(cattrs, StructTrait.SerializeMap)
    val tagFieldExists = !tagField.isEmpty()

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }

    val letMut = mutIf(serializedFields.isNotEmpty() || tagFieldExists)

    return Fragment.Block(checkedQuote("""
        let `#`letMut __serde_state = _serde::Serializer::serialize_map(__serializer, _serde::`#`Private::None)?;
        `#`tagField
        `#`(`#`serializeFields)*
        _serde::ser::SerializeMap::end(__serde_state)
    """, mapOf(
        "letMut" to letMut,
        "Private" to Private,
        "tagField" to tagField,
        "serializeFields" to serializeFields,
    )))
}

private fun serializeEnum(params: SerParameters, variants: List<Variant>, cattrs: AttrContainer): Fragment {
    check(variants.size.toLong() <= UInt.MAX_VALUE.toLong())

    val selfVar = params.selfVar

    val arms = variants.withIndex()
        .map { (variantIndex, variant) ->
            serializeVariant(params, variant, variantIndex.toUInt(), cattrs)
        }
        .toMutableList()

    if (cattrs.remote() != null && cattrs.nonExhaustive()) {
        arms.add(checkedQuote(
            """
            ref unrecognized => _serde::`#`Private::Err(_serde::ser::Error::custom(_serde::`#`Private::ser::CannotSerializeVariant(unrecognized))),
            """,
            "Private" to Private,
        ))
    }

    val armsStr = arms.joinToString(separator = "") { it.toString() }
    return Fragment.Expr(checkedQuote(
        """
        match *`#`selfVar {
            `#`armsStr
        }
        """,
        "selfVar" to selfVar,
        "armsStr" to armsStr,
    ))
}

private fun serializeVariant(
    params: SerParameters,
    variant: Variant,
    variantIndex: UInt,
    cattrs: AttrContainer
): TokenStream {
    val thisValue = params.thisValue
    val variantIdent = variant.ident

    return if (variant.attrs.skipSerializing()) {
        val skippedMsg = "the enum variant ${params.typeName()}::$variantIdent cannot be serialized"
        val skippedErr = checkedQuote(
            "_serde::`#`Private::Err(_serde::ser::Error::custom(`#`skippedMsg))",
            "Private" to Private,
            "skippedMsg" to skippedMsg,
        )
        val fieldsPat = when (variant.style) {
            Style.Unit -> TokenStream.new()
            Style.Newtype, Style.Tuple -> checkedQuote("(..)")
            Style.Struct -> checkedQuote("{ .. }")
        }
        checkedQuote(
            "`#`thisValue::`#`variantIdent `#`fieldsPat => `#`skippedErr,",
            "thisValue" to thisValue,
            "variantIdent" to variantIdent,
            "fieldsPat" to fieldsPat,
            "skippedErr" to skippedErr,
        )
    } else {
        val case = when (variant.style) {
            Style.Unit -> checkedQuote(
                "`#`thisValue::`#`variantIdent",
                "thisValue" to thisValue,
                "variantIdent" to variantIdent,
            )
            Style.Newtype -> checkedQuote(
                "`#`thisValue::`#`variantIdent(ref __field0)",
                "thisValue" to thisValue,
                "variantIdent" to variantIdent,
            )
            Style.Tuple -> {
                val fieldNames = (0 until variant.fields.size).map { fieldI(it) }
                checkedQuote(
                    "`#`thisValue::`#`variantIdent(`#`(`#`fieldNames: ref `#`fieldNames,*)",
                    "thisValue" to thisValue,
                    "variantIdent" to variantIdent,
                    "fieldNames" to fieldNames,
                )
            }
            Style.Struct -> {
                val members = variant.fields.map { it.member }
                checkedQuote(
                    "`#`thisValue::`#`variantIdent { `#`(`#`members: ref `#`members,*) }",
                    "thisValue" to thisValue,
                    "variantIdent" to variantIdent,
                    "members" to members,
                )
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

        checkedQuote("`#`case => `#`body")
    }
}

private fun serializeExternallyTaggedVariant(
    params: SerParameters,
    variant: Variant,
    variantIndex: UInt,
    cattrs: AttrContainer
): Fragment {
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()

    variant.attrs.serializeWith()?.let { path ->
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(checkedQuote("""
            _serde::Serializer::serialize_newtype_variant(
                __serializer,
                `#`typeName,
                `#`variantIndex,
                `#`variantName,
                `#`ser,
            )
        """))
    }

    return when (effectiveSerializeStyle(variant)) {
        Style.Unit -> Fragment.Expr(checkedQuote("""
            _serde::Serializer::serialize_unit_variant(
                __serializer,
                `#`typeName,
                `#`variantIndex,
                `#`variantName,
            )
        """))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = checkedQuote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = checkedQuoteSpanned(span, "_serde::Serializer::serialize_newtype_variant")
            Fragment.Expr(checkedQuote("""
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
    params: SerParameters,
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
        return Fragment.Expr(checkedQuote("""
            _serde::`#`Private::ser::serialize_tagged_newtype(
                __serializer,
                `#`enumIdentStr,
                `#`variantIdentStr,
                `#`tag,
                `#`variantName,
                `#`ser,
            )
        """))
    }

    return when (effectiveSerializeStyle(variant)) {
        Style.Unit -> Fragment.Block(checkedQuote("""
            let mut __struct = _serde::Serializer::serialize_struct(
                __serializer, `#`typeName, 1)?;
            _serde::ser::SerializeStruct::serialize_field(
                &mut __struct, `#`tag, `#`variantName)?;
            _serde::ser::SerializeStruct::end(__struct)
        """))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = checkedQuote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = checkedQuoteSpanned(span, "_serde::`#`Private::ser::serialize_tagged_newtype")
            Fragment.Expr(checkedQuote("""
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
    params: SerParameters,
    variant: Variant,
    cattrs: AttrContainer,
    variantIndex: UInt,
    tag: String,
    content: String
): Fragment {
    val thisType = params.thisType
    val typeName = cattrs.name().serializeName()
    val variantName = variant.attrs.name().serializeName()
    val serializeVariant = checkedQuote("""
        &_serde::`#`Private::ser::AdjacentlyTaggedEnumVariant {
            enum_name: `#`typeName,
            variant_index: `#`variantIndex,
            variant_name: `#`variantName,
        }
    """)

    val inner = Stmts(variant.attrs.serializeWith()?.let { path ->
        val ser = wrapSerializeVariantWith(params, path, variant)
        Fragment.Expr(checkedQuote("_serde::Serialize::serialize(`#`ser, __serializer)"))
    } ?: when (effectiveSerializeStyle(variant)) {
        Style.Unit -> return Fragment.Block(checkedQuote("""
            let mut __struct = _serde::Serializer::serialize_struct(
                __serializer, `#`typeName, 1)?;
            _serde::ser::SerializeStruct::serialize_field(
                &mut __struct, `#`tag, `#`serializeVariant)?;
            _serde::ser::SerializeStruct::end(__struct)
        """))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = checkedQuote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = checkedQuoteSpanned(span, "_serde::ser::SerializeStruct::serialize_field")
            return Fragment.Block(checkedQuote("""
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

    val tyGenerics = generatedTypeGenerics(params.generics)
    val whereClause = generatedWhereClause(params.generics.whereClause)

    val wrapperGenerics = if (fieldsIdent.isEmpty()) {
        params.generics.copy()
    } else {
        withLifetimeBound(params.generics, "'__a")
    }
    val wrapperImplGenerics = generatedImplGenerics(wrapperGenerics)
    val wrapperTyGenerics = generatedTypeGenerics(wrapperGenerics)

    return Fragment.Block(checkedQuote("""
        `#`[doc(hidden)]
            struct __AdjacentlyTagged `#`wrapperImplGenerics `#`whereClause {
                data: (`#`(&'__a `#`fieldsTy,)*),
                phantom: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            }

            `#`[automatically_derived]
            impl `#`wrapperImplGenerics _serde::Serialize for __AdjacentlyTagged `#`wrapperTyGenerics `#`whereClause {
            fn serialize<__S>(&self, __serializer: __S) -> _serde::`#`Private::Result<__S::Ok, __S::Error>
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
                phantom: _serde::`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
            })?;
        _serde::ser::SerializeStruct::end(__struct)
    """))
}

private fun serializeUntaggedVariant(
    params: SerParameters,
    variant: Variant,
    cattrs: AttrContainer
): Fragment {
    variant.attrs.serializeWith()?.let { path ->
        val ser = wrapSerializeVariantWith(params, path, variant)
        return Fragment.Expr(checkedQuote("_serde::Serialize::serialize(`#`ser, __serializer)"))
    }

    return when (effectiveSerializeStyle(variant)) {
        Style.Unit -> Fragment.Expr(checkedQuote("_serde::Serializer::serialize_unit(__serializer)"))
        Style.Newtype -> {
            val field = variant.fields[0]
            var fieldExpr = checkedQuote("__field0")
            field.attrs.serializeWith()?.let { path ->
                fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
            }

            val span = field.original.span()
            val func = checkedQuoteSpanned(span, "_serde::Serialize::serialize")
            Fragment.Expr(checkedQuote(
                "`#`func(`#`fieldExpr, __serializer)",
                "func" to func,
                "fieldExpr" to fieldExpr,
            ))
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
    params: SerParameters,
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
    val zero = rustUsizeLiteral(0)
    val one = rustUsizeLiteral(1)

    val len = serializedFields
        .map { (i, field) ->
            when (val path = field.attrs.skipSerializingIf()) {
                null -> one
                else -> {
                    val fieldExpr = fieldI(i)
                    checkedQuote(
                        "if `#`path(`#`fieldExpr) { `#`zero } else { `#`one }",
                        "path" to path,
                        "fieldExpr" to fieldExpr,
                        "zero" to zero,
                        "one" to one,
                    )
                }
            }
        }
        .fold(zero) { sum, expr -> checkedQuote("`#`sum + `#`expr", "sum" to sum, "expr" to expr) }

    val stmts = serializeStmts.joinToString(separator = "") { it.toString() }
    return when (context) {
        is TupleVariant.ExternallyTagged -> {
            val typeName = context.typeName
            val variantIndex = context.variantIndex
            val variantName = context.variantName
            Fragment.Block(checkedQuote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_tuple_variant(
                    __serializer,
                    `#`typeName,
                    `#`variantIndex,
                    `#`variantName,
                    `#`len)?;
                `#`stmts
                _serde::ser::SerializeTupleVariant::end(__serde_state)
            """))
        }
        TupleVariant.Untagged -> {
            Fragment.Block(checkedQuote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_tuple(
                    __serializer,
                    `#`len)?;
                `#`stmts
                _serde::ser::SerializeTuple::end(__serde_state)
            """))
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
    params: SerParameters,
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
    val zero = rustUsizeLiteral(0)
    val one = rustUsizeLiteral(1)

    val len = serializedFields
        .map { field ->
            val member = field.member
            when (val path = field.attrs.skipSerializingIf()) {
                null -> one
                else -> checkedQuote(
                    "if `#`path(`#`member) { `#`zero } else { `#`one }",
                    "path" to path,
                    "member" to member,
                    "zero" to zero,
                    "one" to one,
                )
            }
        }
        .fold(zero) { sum, expr -> checkedQuote("`#`sum + `#`expr", "sum" to sum, "expr" to expr) }

    val stmts = serializeFields.joinToString(separator = "") { it.toString() }
    return when (context) {
        is StructVariant.ExternallyTagged -> {
            val variantIndex = context.variantIndex
            val variantName = context.variantName
            Fragment.Block(checkedQuote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_struct_variant(
                    __serializer,
                    `#`name,
                    `#`variantIndex,
                    `#`variantName,
                    `#`len,
                )?;
                `#`stmts
                _serde::ser::SerializeStructVariant::end(__serde_state)
            """))
        }
        is StructVariant.InternallyTagged -> {
            val tag = context.tag
            val variantName = context.variantName
            Fragment.Block(checkedQuote("""
                let mut __serde_state = _serde::Serializer::serialize_struct(
                    __serializer,
                    `#`name,
                    `#`len + 1,
                )?;
                _serde::ser::SerializeStruct::serialize_field(
                    &mut __serde_state,
                    `#`tag,
                    `#`variantName,
                )?;
                `#`stmts
                _serde::ser::SerializeStruct::end(__serde_state)
            """))
        }
        StructVariant.Untagged -> {
            Fragment.Block(checkedQuote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_struct(
                    __serializer,
                    `#`name,
                    `#`len,
                )?;
                `#`stmts
                _serde::ser::SerializeStruct::end(__serde_state)
            """))
        }
    }
}

private fun serializeStructVariantWithFlatten(
    context: StructVariant,
    params: SerParameters,
    fields: List<Field>,
    name: Name
): Fragment {
    val structTrait = StructTrait.SerializeMap
    val serializeFields = serializeStructVisitor(fields, params, true, structTrait)

    val serializedFields = fields.filter { !it.attrs.skipSerializing() }

    val letMut = mutIf(serializedFields.isNotEmpty())

    val stmts = serializeFields.joinToString(separator = "") { it.toString() }
    return when (context) {
        is StructVariant.ExternallyTagged -> {
            val variantIndex = context.variantIndex
            val variantName = context.variantName
            val thisType = params.thisType
            val fieldsTy = fields.map { it.ty }
            val members = fields.map { it.member }

            val tyGenerics = generatedTypeGenerics(params.generics)
            val whereClause = generatedWhereClause(params.generics.whereClause)
            val wrapperGenerics = withLifetimeBound(params.generics, "'__a")
            val wrapperImplGenerics = generatedImplGenerics(wrapperGenerics)
            val wrapperTyGenerics = generatedTypeGenerics(wrapperGenerics)

            Fragment.Block(checkedQuote("""
                `#`[doc(hidden)]
                struct __EnumFlatten `#`wrapperImplGenerics `#`whereClause {
                    data: (`#`(&'__a `#`fieldsTy,)*),
                    phantom: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
                }

                `#`[automatically_derived]
                impl `#`wrapperImplGenerics _serde::Serialize for __EnumFlatten `#`wrapperTyGenerics `#`whereClause {
                    fn serialize<__S>(&self, __serializer: __S) -> _serde::`#`Private::Result<__S::Ok, __S::Error>
                    where
                        __S: _serde::Serializer,
                    {
                        let (`#`(`#`members: `#`members,)*) = self.data;
                        let `#`letMut __serde_state = _serde::Serializer::serialize_map(
                            __serializer,
                            _serde::`#`Private::None)?;
                        `#`stmts
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
                        phantom: _serde::`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
                    })
            """))
        }
        is StructVariant.InternallyTagged -> {
            val tag = context.tag
            val variantName = context.variantName
            Fragment.Block(checkedQuote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_map(
                    __serializer,
                    _serde::`#`Private::None)?;
                _serde::ser::SerializeMap::serialize_entry(
                    &mut __serde_state,
                    `#`tag,
                    `#`variantName,
                )?;
                `#`stmts
                _serde::ser::SerializeMap::end(__serde_state)
            """))
        }
        StructVariant.Untagged -> {
            Fragment.Block(checkedQuote("""
                let `#`letMut __serde_state = _serde::Serializer::serialize_map(
                    __serializer,
                    _serde::`#`Private::None)?;
                `#`stmts
                _serde::ser::SerializeMap::end(__serde_state)
            """))
        }
    }
}

private fun serializeTupleStructVisitor(
    fields: List<Field>,
    params: SerParameters,
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
            checkedQuote("`#`id")
        } else {
            getMember(
                params,
                field,
                Member.Unnamed(Index(i.toUInt(), Span.callSite()))
            )
        }

        val skip = field.attrs.skipSerializingIf()?.let { path ->
            checkedQuote("`#`path(`#`fieldExpr)")
        }

        field.attrs.serializeWith()?.let { path ->
            fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
        }

        val span = field.original.span()
        val func = tupleTrait.serializeElement(span)
        val ser = checkedQuote("`#`func(&mut __serde_state, `#`fieldExpr)?;")

        dstFields.add(when (skip) {
            null -> ser
            else -> checkedQuote("if !`#`skip { `#`ser }")
        })
    }
    return dstFields
}

private fun serializeStructVisitor(
    fields: List<Field>,
    params: SerParameters,
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
            checkedQuote("`#`member", "member" to member)
        } else {
            getMember(params, field, member)
        }

        val keyExpr = field.attrs.name().serializeName()

        val skip = field.attrs.skipSerializingIf()?.let { path ->
            checkedQuote("`#`path(`#`fieldExpr)", mapOf("path" to path, "fieldExpr" to fieldExpr))
        }

        field.attrs.serializeWith()?.let { path ->
            fieldExpr = wrapSerializeFieldWith(params, field.ty, path, fieldExpr)
        }

        val span = field.original.span()
        val ser = if (field.attrs.flatten()) {
            val func = checkedQuoteSpanned(span, "_serde::Serialize::serialize")
            checkedQuote(
                "`#`func(&`#`fieldExpr, _serde::`#`Private::ser::FlatMapSerializer(&mut __serde_state))?;",
                mapOf("func" to func, "fieldExpr" to fieldExpr, "Private" to Private),
            )
        } else {
            val func = structTrait.serializeField(span)
            checkedQuote(
                "`#`func(&mut __serde_state, `#`keyExpr, `#`fieldExpr)?;",
                mapOf("func" to func, "keyExpr" to keyExpr, "fieldExpr" to fieldExpr),
            )
        }

        dstFields.add(when (skip) {
            null -> ser
            else -> {
                val skipFunc = structTrait.skipField(span)
                if (skipFunc != null) {
                    checkedQuote("""
                        if !`#`skip {
                            `#`ser
                        } else {
                            `#`skipFunc(&mut __serde_state, `#`keyExpr)?;
                        }
                    """, mapOf("skip" to skip, "ser" to ser, "skipFunc" to skipFunc, "keyExpr" to keyExpr))
                } else {
                    checkedQuote("""
                        if !`#`skip {
                            `#`ser
                        }
                    """, mapOf("skip" to skip, "ser" to ser))
                }
            }
        })
    }
    return dstFields
}

private fun wrapSerializeFieldWith(
    params: SerParameters,
    fieldTy: SynType,
    serializeWith: Expr.Path,
    fieldExpr: TokenStream
): TokenStream {
    return wrapSerializeWith(
        params,
        serializeWith,
        listOf(fieldTy),
        listOf(checkedQuote("`#`fieldExpr", "fieldExpr" to fieldExpr)),
    )
}

private fun wrapSerializeVariantWith(
    params: SerParameters,
    serializeWith: Expr.Path,
    variant: Variant
): TokenStream {
    val fieldTys = variant.fields.map { it.ty }
    val fieldExprs = variant.fields.map { field ->
        val id = when (val member = field.member) {
            is Member.Named -> member.ident
            is Member.Unnamed -> fieldI(member.index.index.toInt())
        }
        checkedQuote("`#`id", "id" to id)
    }
    return wrapSerializeWith(params, serializeWith, fieldTys, fieldExprs)
}

private fun wrapSerializeWith(
    params: SerParameters,
    serializeWith: Expr.Path,
    fieldTys: List<SynType>,
    fieldExprs: List<TokenStream>
): TokenStream {
    val thisType = params.thisType
    val tyGenerics = generatedTypeGenerics(params.generics)
    val whereClause = generatedWhereClause(params.generics.whereClause)

    val wrapperGenerics = if (fieldExprs.isEmpty()) {
        params.generics.copy()
    } else {
        withLifetimeBound(params.generics, "'__a")
    }
    val wrapperImplGenerics = generatedImplGenerics(wrapperGenerics)
    val wrapperTyGenerics = generatedTypeGenerics(wrapperGenerics)

    val fieldAccess = (0 until fieldExprs.size).map { n ->
        Member.Unnamed(Index(n.toUInt(), Span.callSite()))
    }

    val selfVar = checkedQuote("self")
    val serializerVar = checkedQuote("__s")

    // If the serializeWith path returns the wrong type, the error will be
    // reported on this piece. We attach the span of the path so the error
    // will be reported on the serde with attribute.
    val wrapperSerialize = checkedQuoteSpanned(serializeWith.span(), """
        `#`serializeWith(`#`(`#`selfVar.values.`#`fieldAccess, )* `#`serializerVar)
    """, mapOf(
        "serializeWith" to serializeWith,
        "selfVar" to selfVar,
        "fieldAccess" to fieldAccess,
        "serializerVar" to serializerVar,
    ))

    return checkedQuote("""
        &{
            `#`[doc(hidden)]
            struct __SerializeWith `#`wrapperImplGenerics `#`whereClause {
                values: (`#`(&'__a `#`fieldTys, )*),
                phantom: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            }

            `#`[automatically_derived]
            impl `#`wrapperImplGenerics _serde::Serialize for __SerializeWith `#`wrapperTyGenerics `#`whereClause {
                fn serialize<__S>(&`#`selfVar, `#`serializerVar: __S) -> _serde::`#`Private::Result<__S::Ok, __S::Error>
                where
                    __S: _serde::Serializer,
                {
                    `#`wrapperSerialize
                }
            }

            __SerializeWith {
                values: (`#`(`#`fieldExprs, )*),
                phantom: _serde::`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
            }
        }
    """, mapOf(
        "wrapperImplGenerics" to wrapperImplGenerics,
        "whereClause" to whereClause,
        "fieldTys" to fieldTys,
        "Private" to Private,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "wrapperTyGenerics" to wrapperTyGenerics,
        "selfVar" to selfVar,
        "serializerVar" to serializerVar,
        "wrapperSerialize" to wrapperSerialize,
        "fieldExprs" to fieldExprs,
    ))
}

// Serialization of an empty struct produces a state token that does not
// need to be mutable. This helper omits the mut keyword in that case to
// avoid a compiler warning.
private fun mutIf(isMut: Boolean): TokenStream? {
    return if (isMut) {
        checkedQuote("mut")
    } else {
        null
    }
}

private fun getMember(params: SerParameters, field: Field, member: Member): TokenStream {
    val selfVar = params.selfVar
    return when {
        !params.isRemote && field.attrs.getter() == null -> {
            if (params.isPacked) {
                checkedQuote("&{ `#`selfVar.`#`member }", mapOf("selfVar" to selfVar, "member" to member))
            } else {
                checkedQuote("&`#`selfVar.`#`member", mapOf("selfVar" to selfVar, "member" to member))
            }
        }
        params.isRemote && field.attrs.getter() == null -> {
            val inner = if (params.isPacked) {
                checkedQuote("&{ `#`selfVar.`#`member }", mapOf("selfVar" to selfVar, "member" to member))
            } else {
                checkedQuote("&`#`selfVar.`#`member", mapOf("selfVar" to selfVar, "member" to member))
            }
            val ty = field.ty
            checkedQuote(
                "_serde::`#`Private::ser::constrain::<`#`ty>(`#`inner)",
                mapOf("Private" to Private, "ty" to ty, "inner" to inner),
            )
        }
        params.isRemote -> {
            val ty = field.ty
            val getter = field.attrs.getter()!!
            checkedQuote(
                "_serde::`#`Private::ser::constrain::<`#`ty>(&`#`getter(`#`selfVar))",
                mapOf("Private" to Private, "ty" to ty, "getter" to getter, "selfVar" to selfVar),
            )
        }
        else -> {
            error("getter is only allowed for remote impls")
        }
    }
}

private fun effectiveSerializeStyle(variant: Variant): Style {
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
            SerializeMap -> checkedQuoteSpanned(span, "_serde::ser::SerializeMap::serialize_entry")
            SerializeStruct -> checkedQuoteSpanned(span, "_serde::ser::SerializeStruct::serialize_field")
            SerializeStructVariant -> checkedQuoteSpanned(span, "_serde::ser::SerializeStructVariant::serialize_field")
        }
    }

    fun skipField(span: Span): TokenStream? {
        return when (this) {
            SerializeMap -> null
            SerializeStruct -> checkedQuoteSpanned(span, "_serde::ser::SerializeStruct::skip_field")
            SerializeStructVariant -> checkedQuoteSpanned(span, "_serde::ser::SerializeStructVariant::skip_field")
        }
    }
}

private enum class TupleTrait {
    SerializeTuple,
    SerializeTupleStruct,
    SerializeTupleVariant;

    fun serializeElement(span: Span): TokenStream {
        return when (this) {
            SerializeTuple -> checkedQuoteSpanned(span, "_serde::ser::SerializeTuple::serialize_element")
            SerializeTupleStruct -> checkedQuoteSpanned(span, "_serde::ser::SerializeTupleStruct::serialize_field")
            SerializeTupleVariant -> checkedQuoteSpanned(span, "_serde::ser::SerializeTupleVariant::serialize_field")
        }
    }
}
