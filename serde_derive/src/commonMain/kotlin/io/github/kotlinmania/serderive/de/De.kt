// port-lint: source de.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.appendAll
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.AttrField
import io.github.kotlinmania.serderive.internals.AttrVariant
import io.github.kotlinmania.serderive.internals.Container
import io.github.kotlinmania.serderive.internals.Ctxt
import io.github.kotlinmania.serderive.internals.Data
import io.github.kotlinmania.serderive.internals.Derive
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.Variant
import io.github.kotlinmania.serderive.internals.allowDeprecated
import io.github.kotlinmania.serderive.internals.replaceReceiver
import io.github.kotlinmania.serderive.internals.ungroup
import io.github.kotlinmania.serderive.internals.withoutDefaults
import io.github.kotlinmania.serderive.internals.withWherePredicates
import io.github.kotlinmania.serderive.internals.withWherePredicatesFromFields
import io.github.kotlinmania.serderive.internals.withWherePredicatesFromVariants
import io.github.kotlinmania.serderive.internals.withBound
import io.github.kotlinmania.serderive.internals.withSelfBound
import io.github.kotlinmania.serderive.internals.wrapInConst
import io.github.kotlinmania.serderive.internals.pretendUsed
import io.github.kotlinmania.serderive.internals.thisType
import io.github.kotlinmania.serderive.internals.thisValue
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Index
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.LifetimeParam
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.WherePredicate
import io.github.kotlinmania.syn.parseQuote
import io.github.kotlinmania.syn.spanned.Spanned
import io.github.kotlinmania.serderive.internals.name.Name

public fun expandDeriveDeserialize(input: DeriveInput): TokenStream {
    replaceReceiver(input)

    val ctxt = Ctxt()
    val cont = Container.fromAst(ctxt, input, Derive.Deserialize, Private.ident())
    if (cont == null) {
        ctxt.check()
        return TokenStream.new()
    }
    precondition(ctxt, cont)
    ctxt.check()

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
        quote("""
            `#`[automatically_derived]
            `#`allowDeprecated
            impl `#`deImplGenerics `#`ident `#`tyGenerics `#`whereClause {
                `#`vis fun deserialize<__D>(__deserializer: __D) -> _serde.`#`Private.Result<`#`remote `#`tyGenerics, __D.Error>
                where
                    __D: _serde.Deserializer<`#`delife>,
                {
                    `#`used
                    `#`body
                }
            }
        """)
    } else {
        val fnDeserializeInPlace = deserializeInPlaceBody(cont, params)

        quote("""
            `#`[automatically_derived]
            `#`allowDeprecated
            impl `#`deImplGenerics _serde.Deserialize<`#`delife> for `#`ident `#`tyGenerics `#`whereClause {
                fun deserialize<__D>(__deserializer: __D) -> _serde.`#`Private.Result<this, __D.Error>
                where
                    __D: _serde.Deserializer<`#`delife>,
                {
                    `#`body
                }

                `#`fnDeserializeInPlace
            }
        """)
    }

    return wrapInConst(cont.attrs.serdePath(), implBlock)
}

private fun precondition(cx: Ctxt, cont: Container) {
    preconditionSized(cx, cont)
    preconditionNoDeLifetime(cx, cont)
}

private fun preconditionSized(cx: Ctxt, cont: Container) {
    if (cont.data is Data.Struct) {
        val fields = cont.data.fields
        if (fields.isNotEmpty()) {
            val last = fields.last()
            if (ungroup(last.ty) is SynType.Slice) {
                cx.errorSpannedBy(cont.original, "cannot deserialize a dynamically sized struct")
            }
        }
    }
}

private fun preconditionNoDeLifetime(cx: Ctxt, cont: Container) {
    if (borrowedLifetimes(cont) is BorrowedLifetimes.Borrowed) {
        for (param in cont.generics.lifetimes()) {
            if (param.lifetime.toString() == "'de") {
                cx.errorSpannedBy(param.lifetime, "cannot deserialize when there is a lifetime parameter called 'de")
                return
            }
        }
    }
}

class Parameters(cont: Container) {
    val local: Ident = cont.ident.clone()
    val thisType: io.github.kotlinmania.syn.Path = thisType(cont)
    val thisValue: io.github.kotlinmania.syn.Path = thisValue(cont)
    val borrowed: BorrowedLifetimes = borrowedLifetimes(cont)
    val generics: Generics = buildGenerics(cont, borrowed)
    val hasGetter: Boolean = cont.data.hasGetter()
    val isPacked: Boolean = cont.attrs.isPacked()

    fun typeName(): String {
        return thisType.segments.last().ident.toString()
    }

    fun genericsWithDeLifetime(): Quadruple<DeImplGenerics, DeTypeGenerics, io.github.kotlinmania.syn.TypeGenerics, io.github.kotlinmania.syn.WhereClause?> {
        val deImplGenerics = DeImplGenerics(this)
        val deTyGenerics = DeTypeGenerics(this)
        val (_, tyGenerics, whereClause) = generics.splitForImpl()
        return Quadruple(deImplGenerics, deTyGenerics, tyGenerics, whereClause)
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A, val second: B, val third: C, val fourth: D
)

private fun buildGenerics(cont: Container, borrowed: BorrowedLifetimes): Generics {
    val generics = withoutDefaults(cont.generics)

    val generics = withWherePredicatesFromFields(cont, generics) { field -> field.deBound() }

    val generics = withWherePredicatesFromVariants(cont, generics) { variant -> variant.deBound() }

    return when (val deBound = cont.attrs.deBound()) {
        null -> {
            val generics = when (cont.attrs.default()) {
                is io.github.kotlinmania.serderive.internals.Default.Default ->
                    withSelfBound(cont, generics, parseQuote("_serde.`#`Private.Default"))
                is io.github.kotlinmania.serderive.internals.Default.None,
                is io.github.kotlinmania.serderive.internals.Default.Path -> generics
            }

            val delife = borrowed.deLifetime()
            val generics = withBound(
                cont, generics,
                ::needsDeserializeBound,
                parseQuote("_serde.Deserialize<`#`delife>")
            )

            withBound(
                cont, generics,
                ::requiresDefault,
                parseQuote("_serde.`#`Private.Default")
            )
        }
        else -> withWherePredicates(generics, deBound)
    }
}

private fun needsDeserializeBound(field: AttrField, variant: AttrVariant?): Boolean {
    return !field.skipDeserializing()
        && field.deserializeWith() == null
        && field.deBound() == null
        && (variant == null || (!variant.skipDeserializing() && variant.deserializeWith() == null && variant.deBound() == null))
}

private fun requiresDefault(field: AttrField, variant: AttrVariant?): Boolean {
    return field.default() is io.github.kotlinmania.serderive.internals.Default.Default
}

sealed class BorrowedLifetimes {
    class Borrowed(val lifetimes: Set<Lifetime>) : BorrowedLifetimes()
    object Static : BorrowedLifetimes()

    fun deLifetime(): Lifetime {
        return when (this) {
            is Borrowed -> Lifetime.new("'de", Span.callSite())
            is Static -> Lifetime.new("'static", Span.callSite())
        }
    }

    fun deLifetimeParam(): LifetimeParam? {
        return when (this) {
            is Borrowed -> LifetimeParam(
                attrs = emptyList(),
                lifetime = Lifetime.new("'de", Span.callSite()),
                colonToken = null,
                bounds = lifetimes.toList()
            )
            is Static -> null
        }
    }
}

private fun borrowedLifetimes(cont: Container): BorrowedLifetimes {
    val lifetimes = mutableSetOf<Lifetime>()
    for (field in cont.data.allFields()) {
        if (!field.attrs.skipDeserializing()) {
            lifetimes.addAll(field.attrs.borrowedLifetimes())
        }
    }
    return if (lifetimes.any { it.toString() == "'static" }) {
        BorrowedLifetimes.Static
    } else {
        BorrowedLifetimes.Borrowed(lifetimes)
    }
}

private fun deserializeBody(cont: Container, params: Parameters): Fragment {
    if (cont.attrs.transparent()) {
        return deserializeTransparent(cont, params)
    }
    cont.attrs.typeFrom()?.let { return deserializeFrom(it) }
    cont.attrs.typeTryFrom()?.let { return deserializeTryFrom(it) }
    if (cont.attrs.identifier() is io.github.kotlinmania.serderive.internals.Identifier.No) {
        return when (val data = cont.data) {
            is Data.Enum -> deserializeEnum(params, data.variants, cont.attrs)
            is Data.Struct -> when (data.style) {
                Style.Struct -> deserializeStruct(params, data.fields, cont.attrs, StructForm.Struct)
                Style.Tuple, Style.Newtype -> deserializeTuple(params, data.fields, cont.attrs, TupleForm.Tuple)
                Style.Unit -> deserializeUnit(params, cont.attrs)
            }
        }
    } else {
        return when (val data = cont.data) {
            is Data.Enum -> deserializeCustomIdentifier(params, data.variants, cont.attrs)
            is Data.Struct -> error("checked in serde_derive_internals")
        }
    }
}

private fun deserializeInPlaceBody(cont: Container, params: Parameters): Stmts? {
    // Only remote derives have getters, and we do not generate
    // deserialize_in_place for remote derives.
    check(!params.hasGetter)

    if (cont.attrs.transparent()
        || cont.attrs.typeFrom() != null
        || cont.attrs.typeTryFrom() != null
        || cont.attrs.identifier().isSome()
        || cont.data.allFields().all { it.attrs.deserializeWith() != null }
    ) {
        return null
    }

    val code = when (val data = cont.data) {
        is Data.Struct -> when (data.style) {
            Style.Struct -> deserializeStructInPlace(params, data.fields, cont.attrs) ?: return null
            Style.Tuple, Style.Newtype -> deserializeTupleInPlace(params, data.fields, cont.attrs)
            Style.Unit -> return null
        }
        is Data.Enum -> return null
    }

    val delife = params.borrowed.deLifetime()
    val stmts = Stmts(code)

    return Stmts(quote("""
        fun deserialize_in_place<__D>(__deserializer: __D, __place: var this) -> _serde.`#`Private.Result<(), __D.Error>
        where
            __D: _serde.Deserializer<`#`delife>,
        {
            `#`stmts
        }
    """))
}

private fun deserializeTransparent(cont: Container, params: Parameters): Fragment {
    val fields = (cont.data as Data.Struct).fields
    val thisValue = params.thisValue
    val transparentField = fields.first { it.attrs.transparent() }

    val path = when (val dw = transparentField.attrs.deserializeWith()) {
        null -> {
            val span = transparentField.original.span()
            quote("`#`span _serde.Deserialize::deserialize")
        }
        else -> quote("`#`dw")
    }

    val assign = fields.map { field ->
        val member = field.member
        if (field === transparentField) {
            quote("`#`member: __transparent")
        } else {
            val value = when (field.attrs.default()) {
                is io.github.kotlinmania.serderive.internals.Default.Default ->
                    quote("_serde.`#`Private.Default::default()")
                is io.github.kotlinmania.serderive.internals.Default.Path -> {
                    val p = (field.attrs.default() as io.github.kotlinmania.serderive.internals.Default.Path).path
                    quote("`#`p()")
                }
                is io.github.kotlinmania.serderive.internals.Default.None ->
                    quote("_serde.`#`Private.PhantomData")
            }
            quote("`#`member: `#`value")
        }
    }

    return Fragment.Expr(quote("""
        _serde.`#`Private.Result::map(
            `#`path(__deserializer),
            |__transparent| `#`thisValue { `#`assign })
    """))
}

private fun deserializeFrom(typeFrom: SynType): Fragment {
    return Fragment.Expr(quote("""
        _serde.`#`Private.Result::map(
            <`#`typeFrom as _serde.Deserialize>::deserialize(__deserializer),
            _serde.`#`Private.From::from)
    """))
}

private fun deserializeTryFrom(typeTryFrom: SynType): Fragment {
    return Fragment.Expr(quote("""
        _serde.`#`Private.Result::and_then(
            <`#`typeTryFrom as _serde.Deserialize>::deserialize(__deserializer),
            |v| _serde.`#`Private.TryFrom::try_from(v).map_err(_serde::de::Error::custom))
    """))
}

sealed class TupleForm {
    object Tuple : TupleForm()
    class ExternallyTagged(val variantIdent: Ident) : TupleForm()
    class Untagged(val variantIdent: Ident) : TupleForm()
}

sealed class StructForm {
    object Struct : StructForm()
    class ExternallyTagged(val variantIdent: Ident) : StructForm()
    class InternallyTagged(val variantIdent: Ident) : StructForm()
    class Untagged(val variantIdent: Ident) : StructForm()
}

class FieldWithAliases(
    val ident: Ident,
    val aliases: Set<Name>
)

internal fun fieldI(i: Int): Ident {
    return Ident.new("__field$i", Span.callSite())
}

// Placeholder for deserializeSeq — delegates to the enum/struct/tuple submodules.
// The submodule functions are in separate files (Enum.kt, Struct.kt, etc.).
private fun deserializeEnum(
    params: Parameters, variants: List<Variant>, cattrs: AttrContainer
): Fragment = io.github.kotlinmania.serderive.de.deserializeEnum(params, variants, cattrs)

private fun deserializeStruct(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer, form: StructForm
): Fragment = io.github.kotlinmania.serderive.de.deserializeStruct(params, fields, cattrs, form)

private fun deserializeTuple(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer, form: TupleForm
): Fragment = io.github.kotlinmania.serderive.de.deserializeTuple(params, fields, cattrs, form)

private fun deserializeUnit(
    params: Parameters, cattrs: AttrContainer
): Fragment = io.github.kotlinmania.serderive.de.deserializeUnit(params, cattrs)

private fun deserializeCustomIdentifier(
    params: Parameters, variants: List<Variant>, cattrs: AttrContainer
): Fragment = io.github.kotlinmania.serderive.de.deserializeCustomIdentifier(params, variants, cattrs)

private fun deserializeStructInPlace(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer
): Fragment? = io.github.kotlinmania.serderive.de.deserializeStructInPlace(params, fields, cattrs)

private fun deserializeTupleInPlace(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer
): Fragment = io.github.kotlinmania.serderive.de.deserializeTupleInPlace(params, fields, cattrs)

internal fun exprIsMissing(field: Field, cattrs: AttrContainer): Fragment {
    when (field.attrs.default()) {
        is io.github.kotlinmania.serderive.internals.Default.Default -> {
            val span = field.original.span()
            return Fragment.Expr(quote("_serde.`#`Private.Default::default()"))
        }
        is io.github.kotlinmania.serderive.internals.Default.Path -> {
            val p = (field.attrs.default() as io.github.kotlinmania.serderive.internals.Default.Path).path
            return Fragment.Expr(quote("`#`p()"))
        }
        is io.github.kotlinmania.serderive.internals.Default.None -> {}
    }

    when (cattrs.default()) {
        is io.github.kotlinmania.serderive.internals.Default.Default,
        is io.github.kotlinmania.serderive.internals.Default.Path -> {
            val member = field.member
            return Fragment.Expr(quote("__default.`#`member"))
        }
        is io.github.kotlinmania.serderive.internals.Default.None -> {}
    }

    val name = field.attrs.name().deserializeName()
    return when (field.attrs.deserializeWith()) {
        null -> Fragment.Expr(quote("_serde.`#`Private.de::missing_field(`#`name)?"))
        else -> Fragment.Expr(quote("return _serde.`#`Private.Err(<__A.Error as _serde::de::Error>::missing_field(`#`name))"))
    }
}

internal fun exprIsMissingSeq(
    assignTo: TokenStream?, index: Int, field: Field, cattrs: AttrContainer, expecting: String
): TokenStream {
    when (field.attrs.default()) {
        is io.github.kotlinmania.serderive.internals.Default.Default -> {
            return quote("`#`assignTo _serde.`#`Private.Default::default()")
        }
        is io.github.kotlinmania.serderive.internals.Default.Path -> {
            val p = (field.attrs.default() as io.github.kotlinmania.serderive.internals.Default.Path).path
            return quote("`#`assignTo `#`p()")
        }
        is io.github.kotlinmania.serderive.internals.Default.None -> {}
    }

    return when (cattrs.default()) {
        is io.github.kotlinmania.serderive.internals.Default.Default,
        is io.github.kotlinmania.serderive.internals.Default.Path -> {
            val member = field.member
            quote("`#`assignTo __default.`#`member")
        }
        is io.github.kotlinmania.serderive.internals.Default.None -> {
            quote("return _serde.`#`Private.Err(_serde::de::Error::invalid_length(`#`index, &`#`expecting))")
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

class DeImplGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(out: TokenStream) {
        var generics = params.generics.clone()
        val deLifetime = params.borrowed.deLifetimeParam()
        if (deLifetime != null) {
            generics.params = listOf(deLifetime as io.github.kotlinmania.syn.GenericParam) + generics.params
        }
        val (implGenerics, _, _) = generics.splitForImpl()
        implGenerics.toTokens(out)
    }
}

class DeTypeGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(out: TokenStream) {
        deTypeGenericsToTokens(params.generics.clone(), params.borrowed, this)
    }
}

private fun deTypeGenericsToTokens(
    generics: Generics, borrowed: BorrowedLifetimes, tokens: DeTypeGenerics
) {
    val out = TokenStream.new()
    if (borrowed.deLifetimeParam() != null) {
        val def = LifetimeParam(
            attrs = emptyList(),
            lifetime = Lifetime.new("'de", Span.callSite()),
            colonToken = null,
            bounds = emptyList()
        )
        generics.params = listOf(def as io.github.kotlinmania.syn.GenericParam) + generics.params
    }
    val (_, tyGenerics, _) = generics.splitForImpl()
    tyGenerics.toTokens(out)
}