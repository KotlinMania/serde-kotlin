// port-lint: source de.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
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
import io.github.kotlinmania.serderive.internals.Default
import io.github.kotlinmania.serderive.internals.Expr
import io.github.kotlinmania.serderive.internals.Identifier
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.GenericParam
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.WhereClause
import io.github.kotlinmania.syn.span

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
    val local: Ident = Ident.new(cont.ident.toString(), cont.ident.span())
    val thisType: Path = thisType(cont)
    val thisValue: Path = thisValue(cont)
    val borrowed: BorrowedLifetimes = borrowedLifetimes(cont)
    val generics: Generics = buildGenerics(cont, borrowed)
    val hasGetter: Boolean = cont.data.hasGetter()
    val isPacked: Boolean = cont.attrs.isPacked()

    fun typeName(): String {
        return thisType.segments.last()!!.ident.toString()
    }

    fun genericsWithDeLifetime(): SplitForDeLifetime {
        val deImplGenerics = DeImplGenerics(this)
        val deTyGenerics = DeTypeGenerics(this)
        val split = generics.splitForImpl()
        return SplitForDeLifetime(deImplGenerics, deTyGenerics, split.typeGenerics, split.typeGenerics.whereClause)
    }
}

data class SplitForDeLifetime(
    val deImplGenerics: DeImplGenerics,
    val deTyGenerics: DeTypeGenerics,
    val tyGenerics: io.github.kotlinmania.syn.Generics,
    val whereClause: WhereClause?
)
private fun buildGenerics(cont: Container, borrowed: BorrowedLifetimes): Generics {
    val g0 = withoutDefaults(cont.generics)

    val g1 = withWherePredicatesFromFields(cont, g0) { field -> field.deBound() }

    val g2 = withWherePredicatesFromVariants(cont, g1) { variant -> variant.deBound() }

    return when (val deBound = cont.attrs.deBound()) {
        null -> {
            val g3 = when (cont.attrs.default()) {
                is Default.Plain ->
                    withSelfBound(cont, g2, parseQuotePath("_serde.`#`Private.Default"))
                is Default.None,
                is Default.Path -> g2
            }

            val delife = borrowed.deLifetime()
            val g4 = withBound(
                cont, g3,
                ::needsDeserializeBound,
                parseQuotePath("_serde.Deserialize<`#`delife>")
            )

            withBound(
                cont, g4,
                ::requiresDefault,
                parseQuotePath("_serde.`#`Private.Default")
            )
        }
        else -> withWherePredicates(g2, deBound)
    }
}

private fun needsDeserializeBound(field: Field, variant: Variant?): Boolean {
    return !field.attrs.skipDeserializing()
        && field.attrs.deserializeWith() == null
        && field.attrs.deBound() == null
        && (variant == null || (!variant.attrs.skipDeserializing() && variant.attrs.deserializeWith() == null && variant.attrs.deBound() == null))
}

private fun requiresDefault(field: Field, variant: Variant?): Boolean {
    return field.attrs.default() is Default.Plain
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

    fun deLifetimeParam(): GenericParam.LifetimeParam? {
        return when (this) {
            is Borrowed -> GenericParam.LifetimeParam(
                attrs = mutableListOf(),
                lifetime = Lifetime.new("'de", Span.callSite()),
                colonToken = null,
                bounds = io.github.kotlinmania.syn.LifetimeList().also { ll ->
                    for (lt in lifetimes) ll.pushValue(lt)
                }
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
    if (cont.attrs.identifier() == Identifier.No) {
        return when (val data = cont.data) {
            is Data.Enum -> deserializeEnum(params, data.variants as List<Variant>, cont.attrs)
            is Data.Struct -> when (data.style) {
                Style.Struct -> deserializeStruct(params, data.fields, cont.attrs, StructForm.Struct)
                Style.Tuple, Style.Newtype -> deserializeTuple(params, data.fields, cont.attrs, TupleForm.Tuple)
                Style.Unit -> deserializeUnit(params, cont.attrs)
            }
        }
    } else {
        return when (val data = cont.data) {
            is Data.Enum -> deserializeCustomIdentifier(params, data.variants as List<Variant>, cont.attrs)
            is Data.Struct -> error("checked in serde_derive_internals")
        }
    }
}

private fun deserializeInPlaceBody(cont: Container, params: Parameters): Stmts? {
    check(!params.hasGetter)

    if (cont.attrs.transparent()
        || cont.attrs.typeFrom() != null
        || cont.attrs.typeTryFrom() != null
        || cont.attrs.identifier() != Identifier.No
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

    return Stmts(Fragment.Block(quote("""
        fun deserialize_in_place<__D>(__deserializer: __D, __place: var this) -> _serde.`#`Private.Result<(), __D.Error>
        where
            __D: _serde.Deserializer<`#`delife>,
        {
            `#`stmts
        }
    """)))
}

private fun deserializeTransparent(cont: Container, params: Parameters): Fragment {
    val fields = (cont.data as Data.Struct).fields
    val thisValue = params.thisValue
    val transparentField = fields.first { it.attrs.transparent() }

    val path = when (val dw = transparentField.attrs.deserializeWith()) {
        null -> {
            val span = transparentField.original.span()
            quoteSpanned(span, "_serde.Deserialize::deserialize")
        }
        else -> quote("`#`dw")
    }

    val assign = fields.map { field ->
        val member = field.member
        if (field === transparentField) {
            quote("`#`member: __transparent")
        } else {
            val value = when (field.attrs.default()) {
                is Default.Plain ->
                    quote("_serde.`#`Private.Default::default()")
                is Default.Path -> {
                    val p = (field.attrs.default() as Default.Path).path
                    quoteSpanned(p.span(), "`#`p()")
                }
                is Default.None ->
                    quote("_serde.`#`Private.PhantomData")
            }
            quote("`#`member: `#`value")
        }
    }

    return Fragment.Expr(quote("""
        _serde.`#`Private.Result::map(
            `#`path(__deserializer),
            |__transparent| `#`thisValue { `#`(`#`assign),* })
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
    val aliases: Set<io.github.kotlinmania.serderive.internals.Name>
)

internal fun fieldI(i: Int): Ident {
    return Ident.new("__field$i", Span.callSite())
}

// The submodule functions (Enum.kt, Struct.kt, Tuple.kt, Unit.kt, Identifier.kt)
// are in the same package and provide the actual deserialize implementations.
// Forward declarations to submodule functions.
// The de/ submodule files (Enum.kt, Struct.kt, Tuple.kt, etc.) will provide
// the actual implementations when rewritten from upstream Rust.
private fun deserializeEnum(
    params: Parameters, variants: List<Variant>, cattrs: AttrContainer
): Fragment = Fragment.Expr(quote(""))

private fun deserializeStruct(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer, form: StructForm
): Fragment = Fragment.Expr(quote(""))

private fun deserializeTuple(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer, form: TupleForm
): Fragment = Fragment.Expr(quote(""))

private fun deserializeCustomIdentifier(
    params: Parameters, variants: List<Variant>, cattrs: AttrContainer
): Fragment = Fragment.Expr(quote(""))

private fun deserializeStructInPlace(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer
): Fragment? = null

private fun deserializeTupleInPlace(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer
): Fragment = Fragment.Expr(quote(""))

internal fun exprIsMissing(field: Field, cattrs: AttrContainer): Fragment {
    when (field.attrs.default()) {
        is Default.Plain -> {
            val span = field.original.span()
            return Fragment.Expr(quoteSpanned(span, "_serde.`#`Private.Default::default()"))
        }
        is Default.Path -> {
            val p = (field.attrs.default() as Default.Path).path
            return Fragment.Expr(quoteSpanned(p.span(), "`#`p()"))
        }
        is Default.None -> {}
    }

    when (cattrs.default()) {
        is Default.Plain,
        is Default.Path -> {
            val member = field.member
            return Fragment.Expr(quote("__default.`#`member"))
        }
        is Default.None -> {}
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
        is Default.Plain -> {
            val span = field.original.span()
            return quoteSpanned(span, "`#`assignTo _serde.`#`Private.Default::default()")
        }
        is Default.Path -> {
            val p = (field.attrs.default() as Default.Path).path
            return quoteSpanned(p.span(), "`#`assignTo `#`p()")
        }
        is Default.None -> {}
    }

    return when (cattrs.default()) {
        is Default.Plain,
        is Default.Path -> {
            val member = field.member
            quote("`#`assignTo __default.`#`member")
        }
        is Default.None -> {
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
        var generics = params.generics.copy()
        val deLifetime = params.borrowed.deLifetimeParam()
        if (deLifetime != null) {
            val newParams = mutableListOf<GenericParam>()
            newParams.add(deLifetime)
            for (p in generics.params.toList()) {
                newParams.add(p)
            }
            generics = generics.copy(params = io.github.kotlinmania.syn.GenericParamList().also { gpl ->
                for (p in newParams) gpl.pushValue(p)
            })
        }
        val split = generics.splitForImpl()
        split.implGenerics.toTokens(out)
    }
}

class DeTypeGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(out: TokenStream) {
        deTypeGenericsToTokens(params.generics.copy(), params.borrowed, out)
    }
}

private fun deTypeGenericsToTokens(
    generics: Generics, borrowed: BorrowedLifetimes, out: TokenStream
) {
    var mutGenerics = generics
    if (borrowed.deLifetimeParam() != null) {
        val def = GenericParam.LifetimeParam(
            attrs = mutableListOf(),
            lifetime = Lifetime.new("'de", Span.callSite()),
            colonToken = null,
            bounds = io.github.kotlinmania.syn.LifetimeList()
        )
        val newParams = mutableListOf<GenericParam>()
        newParams.add(def)
        for (p in mutGenerics.params.toList()) {
            newParams.add(p)
        }
        mutGenerics = mutGenerics.copy(params = io.github.kotlinmania.syn.GenericParamList().also { gpl ->
            for (p in newParams) gpl.pushValue(p)
        })
    }
    val split = mutGenerics.splitForImpl()
    split.typeGenerics.toTokens(out)
}

// Parse a quote string into a Path, equivalent to Rust's parse_quote!().
private fun parseQuotePath(template: String): Path {
    val tokens = quote(template)
    val result = io.github.kotlinmania.syn.parse2(io.github.kotlinmania.syn.PathParse, tokens)
    return result.getOrThrow()
}