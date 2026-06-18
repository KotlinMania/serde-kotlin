package io.github.kotlinmania.serderive

/* STUBBED OUT FOR NOW
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Ident

public fun expandDeriveDeserialize(input: DeriveInput): Result<TokenStream> {
    replaceReceiver(input)

    val ctxt = Ctxt()
    val cont = Container.fromAst(ctxt, input, Derive.Deserialize, Private.ident())
        ?: return Result.failure(ctxt.check().exceptionOrNull()!!)
        
    precondition(ctxt, cont)
    ctxt.check().getOrThrow()

    val ident = cont.ident
    val params = Parameters(cont)
    val (deImplGenerics, _, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val body = Stmts(deserializeBody(cont, params))
    val delife = params.borrowed.deLifetime()
    val allowDeprecated = allowDeprecated(input)

    val implBlock = if (cont.attrs.remote() != null) {
        val remote = cont.attrs.remote()!!
        val vis = input.vis
        val used = pretendUsed(cont, params.isPacked)
        quote {
            `#`[automatically_derived]
            `#`allowDeprecated
            impl `#`deImplGenerics `#`ident `#`tyGenerics `#`whereClause {
                `#`vis fun deserialize<__D>(__deserializer: __D): _serde.Private.Result<`#`remote `#`tyGenerics, __D.Error>
                where
                    __D: _serde.Deserializer<`#`delife>
                {
                    `#`used
                    `#`body
                }
            }
        }
    } else {
        val fnDeserializeInPlace = deserializeInPlaceBody(cont, params)

        quote {
            `#`[automatically_derived]
            `#`allowDeprecated
            impl `#`deImplGenerics _serde.Deserialize<`#`delife> for `#`ident `#`tyGenerics `#`whereClause {
                fun deserialize<__D>(__deserializer: __D): _serde.Private.Result<Self, __D.Error>
                where
                    __D: _serde.Deserializer<`#`delife>
                {
                    `#`body
                }

                `#`fnDeserializeInPlace
            }
        }
    }

    return Result.success(wrapInConst(cont.attrs.customSerdePath(), implBlock))
}

internal fun precondition(cx: Ctxt, cont: Container) {
    preconditionSized(cx, cont)
    preconditionNoDeLifetime(cx, cont)
}

internal fun preconditionSized(cx: Ctxt, cont: Container) {
    val data = cont.data
    if (data is Data.Struct) {
        val fields = data.fields
        val last = fields.lastOrNull()
        if (last != null) {
            if (ungroup(last.ty) is syn.Type.Slice) {
                cx.errorSpannedBy(
                    cont.original,
                    "cannot deserialize a dynamically sized struct"
                )
            }
        }
    }
}

internal fun preconditionNoDeLifetime(cx: Ctxt, cont: Container) {
    if (borrowedLifetimes(cont) is BorrowedLifetimes.Borrowed) {
        for (param in cont.generics.lifetimes()) {
            if (param.lifetime.toString() == "'de") {
                cx.errorSpannedBy(
                    param.lifetime,
                    "cannot deserialize when there is a lifetime parameter called 'de"
                )
                return
            }
        }
    }
}

internal class Parameters(
    val local: syn.Ident,
    val thisType: syn.Path,
    val thisValue: syn.Path,
    val generics: syn.Generics,
    val borrowed: BorrowedLifetimes,
    val hasGetter: Boolean,
    val isPacked: Boolean
) {
    companion object {
        operator fun invoke(cont: Container): Parameters {
            val local = cont.ident.clone()
            val thisType = thisType(cont)
            val thisValue = thisValue(cont)
            val borrowed = borrowedLifetimes(cont)
            val generics = buildGenerics(cont, borrowed)
            val hasGetter = cont.data.hasGetter()
            val isPacked = cont.attrs.isPacked()

            return Parameters(
                local,
                thisType,
                thisValue,
                generics,
                borrowed,
                hasGetter,
                isPacked
            )
        }
    }

    fun typeName(): String {
        return thisType.segments.last()!!.ident.toString()
    }

    fun genericsWithDeLifetime(): Tuple4<DeImplGenerics, DeTypeGenerics, syn.TypeGenerics, syn.WhereClause?> {
        val deImplGenerics = DeImplGenerics(this)
        val deTyGenerics = DeTypeGenerics(this)
        val (implGenerics, tyGenerics, whereClause) = generics.splitForImpl()
        return Tuple4(deImplGenerics, deTyGenerics, tyGenerics, whereClause)
    }
}

internal fun buildGenerics(cont: Container, borrowed: BorrowedLifetimes): syn.Generics {
    var generics = withoutDefaults(cont.generics)

    generics = withWherePredicatesFromFields(cont, generics, attr.Field::deBound)
    generics = withWherePredicatesFromVariants(cont, generics, attr.Variant::deBound)

    val deBound = cont.attrs.deBound()
    if (deBound != null) {
        return withWherePredicates(generics, deBound)
    } else {
        generics = when (cont.attrs.default()) {
            is attr.Default.Default -> withSelfBound(
                cont,
                generics,
                parseQuote("_serde.Private.Default")
            )
            is attr.Default.None, is attr.Default.Path -> generics
        }

        val delife = borrowed.deLifetime()
        generics = withBound(
            cont,
            generics,
            ::needsDeserializeBound,
            parseQuote("_serde.Deserialize<#delife>")
        )

        return withBound(
            cont,
            generics,
            ::requiresDefault,
            parseQuote("_serde.Private.Default")
        )
    }
}

internal fun needsDeserializeBound(field: attr.Field, variant: attr.Variant?): Boolean {
    return !field.skipDeserializing()
        && field.deserializeWith() == null
        && field.deBound() == null
        && (variant == null || (
            !variant.skipDeserializing()
            && variant.deserializeWith() == null
            && variant.deBound() == null
        ))
}

internal fun requiresDefault(field: attr.Field, _variant: attr.Variant?): Boolean {
    return field.default() is attr.Default.Default
}

internal sealed class BorrowedLifetimes {
    class Borrowed(val lifetimes: Set<syn.Lifetime>) : BorrowedLifetimes()
    object Static : BorrowedLifetimes()

    fun deLifetime(): syn.Lifetime {
        return when (this) {
            is Borrowed -> syn.Lifetime("'de", Span.callSite())
            is Static -> syn.Lifetime("'static", Span.callSite())
        }
    }

    fun deLifetimeParam(): syn.LifetimeParam? {
        return when (this) {
            is Borrowed -> syn.LifetimeParam(
                attrs = emptyList(),
                lifetime = syn.Lifetime("'de", Span.callSite()),
                colonToken = null,
                bounds = syn.punctuated.Punctuated.fromList(lifetimes.toList())
            )
            is Static -> null
        }
    }
}

internal fun borrowedLifetimes(cont: Container): BorrowedLifetimes {
    val lifetimes = mutableSetOf<syn.Lifetime>()
    for (field in cont.data.allFields()) {
        if (!field.attrs.skipDeserializing()) {
            lifetimes.addAll(field.attrs.borrowedLifetimes())
        }
    }
    if (lifetimes.any { it.toString() == "'static" }) {
        return BorrowedLifetimes.Static
    } else {
        return BorrowedLifetimes.Borrowed(lifetimes)
    }
}

internal fun deserializeBody(cont: Container, params: Parameters): Fragment {
    if (cont.attrs.transparent()) {
        return deserializeTransparent(cont, params)
    } else if (cont.attrs.typeFrom() != null) {
        return deserializeFrom(cont.attrs.typeFrom()!!)
    } else if (cont.attrs.typeTryFrom() != null) {
        return deserializeTryFrom(cont.attrs.typeTryFrom()!!)
    } else if (cont.attrs.identifier() == attr.Identifier.No) {
        return when (val data = cont.data) {
            is Data.Enum -> io.github.kotlinmania.serderive.de.enum_.deserialize(params, data.variants, cont.attrs)
            is Data.Struct -> {
                when (data.style) {
                    Style.Struct -> io.github.kotlinmania.serderive.de.struct_.deserialize(params, data.fields, cont.attrs, StructForm.Struct)
                    Style.Tuple, Style.Newtype -> io.github.kotlinmania.serderive.de.tuple.deserialize(params, data.fields, cont.attrs, TupleForm.Tuple)
                    Style.Unit -> io.github.kotlinmania.serderive.de.unit.deserialize(params, cont.attrs)
                }
            }
        }
    } else {
        return when (val data = cont.data) {
            is Data.Enum -> io.github.kotlinmania.serderive.de.identifier.deserializeCustom(params, data.variants, cont.attrs)
            is Data.Struct -> error("checked in serde_derive_internals")
        }
    }
}

internal fun deserializeInPlaceBody(cont: Container, params: Parameters): Stmts? {
    // We do not implement deserialize_in_place in this port yet,
    // as it's feature-gated by #[cfg(feature = "deserialize_in_place")]
    // which is usually off by default or complex.
    return null
}

internal fun deserializeTransparent(cont: Container, params: Parameters): Fragment {
    val fields = (cont.data as Data.Struct).fields
    val thisValue = params.thisValue
    val transparentField = fields.first { it.attrs.transparent() }

    val path = transparentField.attrs.deserializeWith()?.let { path ->
        quote(path)
    } ?: run {
        val span = transparentField.original.span()
        quoteSpanned(span, "_serde.Deserialize.deserialize")
    }

    val assign = fields.map { field ->
        val member = field.member
        if (field === transparentField) {
            quote("`#`member: __transparent")
        } else {
            val value = when (val default = field.attrs.default()) {
                is attr.Default.Default -> quote("_serde.Private.Default.default()")
                is attr.Default.Path -> quoteSpanned(default.path.span(), "`#`default()")
                is attr.Default.None -> quote("_serde.Private.PhantomData")
            }
            quote("`#`member: `#`value")
        }
    }

    return Fragment.Expr(quoteBlock {
        _serde.Private.Result.map(
            `#`path(__deserializer),
            { __transparent -> `#`thisValue { `#`assign } }
        )
    })
}

internal fun deserializeFrom(typeFrom: syn.Type): Fragment {
    return Fragment.Expr(quoteBlock {
        _serde.Private.Result.map(
            <`#`typeFrom as _serde.Deserialize>.deserialize(__deserializer),
            _serde.Private.From.from
        )
    })
}

internal fun deserializeTryFrom(typeTryFrom: syn.Type): Fragment {
    return Fragment.Expr(quoteBlock {
        _serde.Private.Result.and_then(
            <`#`typeTryFrom as _serde.Deserialize>.deserialize(__deserializer),
            { v -> _serde.Private.TryFrom.try_from(v).map_err(_serde.de.Error.custom) }
        )
    })
}

internal sealed class TupleForm {
    object Tuple : TupleForm()
    class ExternallyTagged(val ident: syn.Ident) : TupleForm()
    class Untagged(val ident: syn.Ident) : TupleForm()
}

internal fun deserializeSeq(
    typePath: TokenStream,
    params: Parameters,
    fields: List<Field>,
    isStruct: Boolean,
    cattrs: attr.Container,
    expectingArg: String
): Fragment {
    val vars = (0 until fields.size).map { fieldI(it) }

    val deserializedCount = fields.count { !it.attrs.skipDeserializing() }
    val expectingDef = if (deserializedCount == 1) {
        "$expectingArg with 1 element"
    } else {
        "$expectingArg with $deserializedCount elements"
    }
    val expecting = cattrs.expecting() ?: expectingDef

    var indexInSeq = 0
    val letValues = vars.zip(fields).map { (v, field) ->
        if (field.attrs.skipDeserializing()) {
            val default = Fragment.Expr(exprIsMissing(field, cattrs))
            quote {
                val `#`v = `#`default
            }
        } else {
            val visit = if (field.attrs.deserializeWith() == null) {
                val fieldTy = field.ty
                val span = field.original.span()
                val func = quoteSpanned(span, "_serde.de.SeqAccess.next_element::<`#`fieldTy>")
                quote("`#`func(&mut __seq)?")
            } else {
                val path = field.attrs.deserializeWith()!!
                val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, path)
                quote {
                    `#`wrapper
                    _serde.Private.Option.map(
                        _serde.de.SeqAccess.next_element::<`#`wrapperTy>(&mut __seq)?,
                        { __wrap -> __wrap.value }
                    )
                }
            }
            val valueIfNone = exprIsMissingSeq(null, indexInSeq, field, cattrs, expecting)
            val assign = quote {
                val `#`v = match (`#`visit) {
                    _serde.Private.Some(__value) -> __value
                    _serde.Private.None -> `#`valueIfNone
                }
            }
            indexInSeq += 1
            assign
        }
    }

    var result = if (isStruct) {
        val names = fields.map { it.member }
        quote {
            `#`typePath { `#`names: `#`vars }
        }
    } else {
        quote {
            `#`typePath ( `#`vars )
        }
    }

    if (params.hasGetter) {
        val thisType = params.thisType
        val (_, tyGenerics, _) = params.generics.splitForImpl()
        result = quote {
            _serde.Private.Into::<`#`thisType `#`tyGenerics>.into(`#`result)
        }
    }

    val letDefault = when (val default = cattrs.default()) {
        is attr.Default.Default -> quote {
            val __default: Self.Value = _serde.Private.Default.default()
        }
        is attr.Default.Path -> quoteSpanned(default.path.span(), "val __default: Self.Value = `#`default()")
        is attr.Default.None -> null
    }

    return Fragment.Block(quoteBlock {
        `#`letDefault
        `#`letValues
        _serde.Private.Ok(`#`result)
    })
}

internal fun deserializeSeqInPlace(
    params: Parameters,
    fields: List<Field>,
    cattrs: attr.Container,
    expectingArg: String
): Fragment {
    // deserialize_in_place not implemented
    return Fragment.Expr(quote("()"))
}

internal sealed class StructForm {
    object Struct : StructForm()
    class ExternallyTagged(val ident: syn.Ident) : StructForm()
    class InternallyTagged(val ident: syn.Ident) : StructForm()
    class Untagged(val ident: syn.Ident) : StructForm()
}

internal class FieldWithAliases(
    val ident: Ident,
    val aliases: Set<Name>
)

internal fun fieldI(i: Int): Ident {
    return Ident("__field$i", Span.callSite())
}

internal fun wrapDeserializeWith(
    params: Parameters,
    valueTy: TokenStream,
    deserializeWith: syn.ExprPath
): Pair<TokenStream, TokenStream> {
    val thisType = params.thisType
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()
    val deserializerVar = quote("__deserializer")

    val value = quoteSpanned(deserializeWith.span(), "`#`deserializeWith(`#`deserializerVar)?")
    val wrapper = quote {
        `#`[doc(hidden)]
        struct __DeserializeWith `#`deImplGenerics `#`whereClause {
            value: `#`valueTy,
            phantom: _serde.Private.PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde.Private.PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde.Deserialize<`#`delife> for __DeserializeWith `#`deTyGenerics `#`whereClause {
            fun deserialize<__D>(`#`deserializerVar: __D): _serde.Private.Result<Self, __D.Error>
            where
                __D: _serde.Deserializer<`#`delife>
            {
                _serde.Private.Ok(__DeserializeWith {
                    value: `#`value,
                    phantom: _serde.Private.PhantomData,
                    lifetime: _serde.Private.PhantomData,
                })
            }
        }
    }

    val wrapperTy = quote("__DeserializeWith `#`deTyGenerics")

    return Pair(wrapper, wrapperTy)
}

internal fun wrapDeserializeFieldWith(
    params: Parameters,
    fieldTy: syn.Type,
    deserializeWith: syn.ExprPath
): Pair<TokenStream, TokenStream> {
    return wrapDeserializeWith(params, quote("`#`fieldTy"), deserializeWith)
}

internal fun unwrapToVariantClosure(
    params: Parameters,
    variant: Variant,
    withWrapper: Boolean
): TokenStream {
    val thisValue = params.thisValue
    val variantIdent = variant.ident

    val (arg, wrapper) = if (withWrapper) {
        Pair(quote("__wrap"), quote("__wrap.value"))
    } else {
        val fieldTys = variant.fields.map { it.ty }
        Pair(quote("__wrap: (`#`fieldTys)"), quote("__wrap"))
    }

    val fieldAccess = variant.fields.indices.map {
        syn.Member.Unnamed(syn.Index(it.toUInt(), Span.callSite()))
    }

    return when (variant.style) {
        Style.Struct -> {
            if (variant.fields.size == 1) {
                val member = variant.fields[0].member
                quote { |`#`arg| `#`thisValue::`#`variantIdent { `#`member: `#`wrapper } }
            } else {
                val members = variant.fields.map { it.member }
                quote { |`#`arg| `#`thisValue::`#`variantIdent { `#`members: `#`wrapper.`#`fieldAccess } }
            }
        }
        Style.Tuple -> quote { |`#`arg| `#`thisValue::`#`variantIdent(`#`wrapper.`#`fieldAccess) }
        Style.Newtype -> quote { |`#`arg| `#`thisValue::`#`variantIdent(`#`wrapper) }
        Style.Unit -> quote { |`#`arg| `#`thisValue::`#`variantIdent }
    }
}

internal fun exprIsMissing(field: Field, cattrs: attr.Container): Fragment {
    when (val default = field.attrs.default()) {
        is attr.Default.Default -> {
            val span = field.original.span()
            val func = quoteSpanned(span, "_serde.Private.Default.default")
            return Fragment.Expr(quote("`#`func()"))
        }
        is attr.Default.Path -> {
            return Fragment.Expr(quoteSpanned(default.path.span(), "`#`default()"))
        }
        is attr.Default.None -> {}
    }

    when (cattrs.default()) {
        is attr.Default.Default, is attr.Default.Path -> {
            val member = field.member
            return Fragment.Expr(quote("__default.`#`member"))
        }
        is attr.Default.None -> {}
    }

    val name = field.attrs.name().deserializeName()
    if (field.attrs.deserializeWith() == null) {
        val span = field.original.span()
        val func = quoteSpanned(span, "_serde.Private.de.missing_field")
        return Fragment.Expr(quote("`#`func(`#`name)?"))
    } else {
        return Fragment.Expr(quote {
            return _serde.Private.Err(<__A.Error as _serde.de.Error>.missing_field(`#`name))
        })
    }
}

internal fun exprIsMissingSeq(
    assignTo: TokenStream?,
    index: Int,
    field: Field,
    cattrs: attr.Container,
    expecting: String
): TokenStream {
    when (val default = field.attrs.default()) {
        is attr.Default.Default -> {
            val span = field.original.span()
            return quoteSpanned(span, "`#`assignTo _serde.Private.Default.default()")
        }
        is attr.Default.Path -> {
            return quoteSpanned(default.path.span(), "`#`assignTo `#`default()")
        }
        is attr.Default.None -> {}
    }

    when (cattrs.default()) {
        is attr.Default.Default, is attr.Default.Path -> {
            val member = field.member
            return quote("`#`assignTo __default.`#`member")
        }
        is attr.Default.None -> {
            return quote("return _serde.Private.Err(_serde.de.Error.invalid_length(`#`index, &`#`expecting))")
        }
    }
}

internal fun effectiveStyle(variant: Variant): Style {
    return if (variant.style == Style.Newtype && variant.fields[0].attrs.skipDeserializing()) {
        Style.Unit
    } else {
        variant.style
    }
}

internal fun hasFlatten(fields: List<Field>): Boolean {
    return fields.any { it.attrs.flatten() && !it.attrs.skipDeserializing() }
}

internal class DeImplGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        var generics = params.generics.clone()
        val deLifetime = params.borrowed.deLifetimeParam()
        if (deLifetime != null) {
            generics.params = syn.punctuated.Punctuated.fromList(
                listOf(syn.GenericParam.Lifetime(deLifetime)) + generics.params.toList()
            )
        }
        val (implGenerics, _, _) = generics.splitForImpl()
        implGenerics.toTokens(tokens)
    }
}

internal class DeTypeGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        deTypeGenericsToTokens(params.generics.clone(), params.borrowed, tokens)
    }
}

private fun deTypeGenericsToTokens(
    genericsArg: syn.Generics,
    borrowed: BorrowedLifetimes,
    tokens: TokenStream
) {
    var generics = genericsArg
    if (borrowed.deLifetimeParam() != null) {
        val def = syn.LifetimeParam(
            attrs = emptyList(),
            lifetime = syn.Lifetime("'de", Span.callSite()),
            colonToken = null,
            bounds = syn.punctuated.Punctuated.new()
        )
        generics.params = syn.punctuated.Punctuated.fromList(
            listOf(syn.GenericParam.Lifetime(def)) + generics.params.toList()
        )
    }
    val (_, tyGenerics, _) = generics.splitForImpl()
    tyGenerics.toTokens(tokens)
}

*/
