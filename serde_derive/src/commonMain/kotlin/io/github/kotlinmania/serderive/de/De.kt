// port-lint: source de.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.appendAll
import io.github.kotlinmania.serderive.checkedQuote as quoteTokens
import io.github.kotlinmania.serderive.checkedQuoteSpanned as quoteSpannedTokens
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.AttrField
import io.github.kotlinmania.serderive.internals.AttrVariant
import io.github.kotlinmania.serderive.internals.Container
import io.github.kotlinmania.serderive.internals.Ctxt
import io.github.kotlinmania.serderive.internals.Data
import io.github.kotlinmania.serderive.internals.Default
import io.github.kotlinmania.serderive.internals.Derive
import io.github.kotlinmania.serderive.internals.Expr
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Identifier
import io.github.kotlinmania.serderive.internals.Name
import io.github.kotlinmania.serderive.internals.Stmts
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
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.GenericParam
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Index
import io.github.kotlinmania.syn.Lifetime
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.TypeParamBound
import io.github.kotlinmania.syn.WhereClause
import io.github.kotlinmania.syn.span
import io.github.kotlinmania.syn.token.Comma
import io.github.kotlinmania.syn.token.Plus

public fun expandDeriveDeserialize(input: DeriveInput): TokenStream {
    val rewrittenInput = replaceReceiver(input)

    val ctxt = Ctxt()
    val cont = Container.fromAst(ctxt, rewrittenInput, Derive.Deserialize, Private.ident())
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
    val allowDeprecated = allowDeprecated(rewrittenInput) ?: TokenStream.new()

    val implBlock = if (cont.attrs.remote() != null) {
        val remote = cont.attrs.remote()!!
        val vis = rewrittenInput.vis
        val used = pretendUsed(cont, params.isPacked)
        quoteTokens("""
            #[automatically_derived]
            `#`allowDeprecated
            impl `#`deImplGenerics `#`ident `#`tyGenerics `#`whereClause {
                `#`vis fn deserialize<__D>(__deserializer: __D) -> _serde::`#`Private::Result<`#`remote `#`tyGenerics, __D::Error>
                where
                    __D: _serde::Deserializer<`#`delife>,
                {
                    `#`used
                    `#`body
                }
            }
        """, mapOf(
            "allowDeprecated" to allowDeprecated,
            "deImplGenerics" to deImplGenerics,
            "ident" to ident,
            "tyGenerics" to tyGenerics,
            "whereClause" to whereClause,
            "vis" to vis,
            "remote" to remote,
            "Private" to Private,
            "delife" to delife,
            "used" to used,
            "body" to body,
        ))
    } else {
        val fnDeserializeInPlace = deserializeInPlaceBody(cont, params) ?: Stmts(Fragment.Block(TokenStream.new()))

        quoteTokens("""
            #[automatically_derived]
            `#`allowDeprecated
            impl `#`deImplGenerics _serde::Deserialize<`#`delife> for `#`ident `#`tyGenerics `#`whereClause {
                fn deserialize<__D>(__deserializer: __D) -> _serde::`#`Private::Result<Self, __D::Error>
                where
                    __D: _serde::Deserializer<`#`delife>,
                {
                    `#`body
                }

                `#`fnDeserializeInPlace
            }
        """, mapOf(
            "allowDeprecated" to allowDeprecated,
            "deImplGenerics" to deImplGenerics,
            "delife" to delife,
            "ident" to ident,
            "tyGenerics" to tyGenerics,
            "whereClause" to whereClause,
            "Private" to Private,
            "body" to body,
            "fnDeserializeInPlace" to fnDeserializeInPlace,
        ))
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
        val deGenerics = genericsWithInsertedDeLifetime()
        val deImplGenerics = generatedImplGenerics(deGenerics)
        val deTyGenerics = generatedTypeGenerics(deGenerics)
        val tyGenerics = generatedTypeGenerics(generics)
        val whereClause = generatedWhereClause(generics.whereClause)
        return SplitForDeLifetime(deImplGenerics, deTyGenerics, tyGenerics, whereClause)
    }

    fun inPlaceGenericsWithDeLifetime(): InPlaceSplitForDeLifetime {
        val deImplGenerics = InPlaceImplGenerics(this)
        val deTyGenerics = InPlaceTypeGenerics(this)
        val tyGenerics = generatedTypeGenerics(generics)
        val whereClause = generatedWhereClause(generics.whereClause)
        return InPlaceSplitForDeLifetime(deImplGenerics, deTyGenerics, tyGenerics, whereClause)
    }

    private fun genericsWithInsertedDeLifetime(): Generics {
        val deLifetime = borrowed.deLifetimeParam() ?: return generics.copy()
        val newParams = io.github.kotlinmania.syn.GenericParamList()
        newParams.push(deLifetime) { Comma.default() }
        for (p in generics.params.toList()) {
            newParams.push(p) { Comma.default() }
        }
        return generics.copy(params = newParams)
    }
}

data class SplitForDeLifetime(
    val deImplGenerics: TokenStream,
    val deTyGenerics: TokenStream,
    val tyGenerics: TokenStream,
    val whereClause: TokenStream
)

data class InPlaceSplitForDeLifetime(
    val deImplGenerics: InPlaceImplGenerics,
    val deTyGenerics: InPlaceTypeGenerics,
    val tyGenerics: TokenStream,
    val whereClause: TokenStream
)
private fun buildGenerics(cont: Container, borrowed: BorrowedLifetimes): Generics {
    val g0 = withoutDefaults(cont.generics)

    val g1 = withWherePredicatesFromFields(cont, g0) { field -> field.deBound() }

    val g2 = withWherePredicatesFromVariants(cont, g1) { variant -> variant.deBound() }

    return when (val deBound = cont.attrs.deBound()) {
        null -> {
            val g3 = when (cont.attrs.default()) {
                is Default.Plain ->
                    withSelfBound(cont, g2, parseQuotePath("_serde::`#`Private::Default", "Private" to Private))
                is Default.None,
                is Default.Path -> g2
            }

            val delife = borrowed.deLifetime()
            val g4 = withBound(
                cont, g3,
                ::needsDeserializeBound,
                parseQuotePath("_serde::Deserialize<`#`delife>", "delife" to delife)
            )

            withBound(
                cont, g4,
                ::requiresDefault,
                parseQuotePath("_serde::`#`Private::Default", "Private" to Private)
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
                    for (lt in lifetimes) ll.push(lt) { Plus.default() }
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

    return Stmts(Fragment.Block(quoteTokens("""
        fn deserialize_in_place<__D>(__deserializer: __D, __place: &mut Self) -> _serde::`#`Private::Result<(), __D::Error>
        where
            __D: _serde::Deserializer<`#`delife>,
        {
            `#`stmts
        }
    """, mapOf("Private" to Private, "delife" to delife, "stmts" to stmts))))
}

private fun deserializeTransparent(cont: Container, params: Parameters): Fragment {
    val fields = (cont.data as Data.Struct).fields
    val thisValue = params.thisValue
    val transparentField = fields.first { it.attrs.transparent() }

    val path = when (val dw = transparentField.attrs.deserializeWith()) {
        null -> {
            val span = transparentField.original.span()
            quoteSpannedTokens(span, "_serde::Deserialize::deserialize")
        }
        else -> quoteTokens("`#`dw", "dw" to dw)
    }

    val assign = fields.map { field ->
        val member = field.member
        if (field === transparentField) {
            quoteTokens("`#`member: __transparent", "member" to member)
        } else {
            val value = when (field.attrs.default()) {
                is Default.Plain ->
                    quoteTokens("_serde::`#`Private::Default::default()", "Private" to Private)
                is Default.Path -> {
                    val p = (field.attrs.default() as Default.Path).path
                    quoteSpannedTokens(p.span(), "`#`p()", "p" to p)
                }
                is Default.None ->
                    quoteTokens("_serde::`#`Private::PhantomData", "Private" to Private)
            }
            quoteTokens("`#`member: `#`value", "member" to member, "value" to value)
        }
    }

    return Fragment.Expr(quoteTokens("""
        _serde::`#`Private::Result::map(
            `#`path(__deserializer),
            |__transparent| `#`thisValue { `#`(`#`assign),* })
    """,
        "Private" to Private,
        "path" to path,
        "thisValue" to thisValue,
        "assign" to assign))
}

private fun deserializeFrom(typeFrom: SynType): Fragment {
    return Fragment.Expr(quoteTokens("""
        _serde::`#`Private::Result::map(
            <`#`typeFrom as _serde::Deserialize>::deserialize(__deserializer),
            _serde::`#`Private::From::from)
    """, "Private" to Private, "typeFrom" to typeFrom))
}

private fun deserializeTryFrom(typeTryFrom: SynType): Fragment {
    return Fragment.Expr(quoteTokens("""
        _serde::`#`Private::Result::and_then(
            <`#`typeTryFrom as _serde::Deserialize>::deserialize(__deserializer),
            |v| _serde::`#`Private::TryFrom::try_from(v).map_err(_serde::de::Error::custom))
    """, "Private" to Private, "typeTryFrom" to typeTryFrom))
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

internal fun memberAccess(member: Member): TokenStream =
    when (member) {
        is Member.Named -> member.toTokenStream()
        is Member.Unnamed -> rustUnsuffixedLiteral(member.index.index)
    }

// The submodule functions are implemented in the sibling files:
//   Enum.kt — deserializeEnum, deserializeCustomIdentifier, prepareEnumVariantEnum
//   Struct.kt — deserializeStruct, deserializeStructInPlace
//   Tuple.kt — deserializeTuple, deserializeTupleInPlace
//   Unit.kt — deserializeUnit
//   Identifier.kt — deserializeGenerated, deserializeCustom
// All are in the same package and called directly.

internal fun deserializeCustomIdentifier(
    params: Parameters, variants: List<Variant>, cattrs: AttrContainer
): Fragment = deserializeCustom(params, variants, cattrs)

internal fun exprIsMissing(field: Field, cattrs: AttrContainer): Fragment {
    when (field.attrs.default()) {
        is Default.Plain -> {
            val span = field.original.span()
            return Fragment.Expr(quoteSpannedTokens(span, "_serde::`#`Private::Default::default()", "Private" to Private))
        }
        is Default.Path -> {
            val p = (field.attrs.default() as Default.Path).path
            return Fragment.Expr(quoteSpannedTokens(p.span(), "`#`p()", "p" to p))
        }
        is Default.None -> {}
    }

    when (cattrs.default()) {
        is Default.Plain,
        is Default.Path -> {
            val member = field.member
            return Fragment.Expr(quoteTokens("__default.`#`member", "member" to member))
        }
        is Default.None -> {}
    }

    val name = field.attrs.name().deserializeName()
    return when (field.attrs.deserializeWith()) {
        null -> Fragment.Expr(quoteTokens("_serde::`#`Private::de::missing_field(`#`name)?", mapOf("Private" to Private, "name" to name)))
        else -> Fragment.Expr(quoteTokens("return _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::missing_field(`#`name))", mapOf("Private" to Private, "name" to name)))
    }
}

internal fun exprIsMissingSeq(
    assignTo: TokenStream?, index: Int, field: Field, cattrs: AttrContainer, expecting: String
): TokenStream {
    val indexToken = rustUsizeLiteral(index)
    when (field.attrs.default()) {
        is Default.Plain -> {
            val span = field.original.span()
            return quoteSpannedTokens(
                span,
                "`#`assignTo _serde::`#`Private::Default::default()",
                mapOf("assignTo" to assignTo, "Private" to Private),
            )
        }
        is Default.Path -> {
            val p = (field.attrs.default() as Default.Path).path
            return quoteSpannedTokens(p.span(), "`#`assignTo `#`p()", mapOf("assignTo" to assignTo, "p" to p))
        }
        is Default.None -> {}
    }

    return when (cattrs.default()) {
        is Default.Plain,
        is Default.Path -> {
            val member = field.member
            quoteTokens("`#`assignTo __default.`#`member", mapOf("assignTo" to assignTo, "member" to member))
        }
        is Default.None -> {
            quoteTokens(
                "return _serde::`#`Private::Err(_serde::de::Error::invalid_length(`#`index, &`#`expecting))",
                mapOf("Private" to Private, "index" to indexToken, "expecting" to expecting),
            )
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
    override fun toTokens(tokens: TokenStream) {
        var generics = params.generics.copy()
        val deLifetime = params.borrowed.deLifetimeParam()
        if (deLifetime != null) {
            val newParams = mutableListOf<GenericParam>()
            newParams.add(deLifetime)
            for (p in generics.params.toList()) {
                newParams.add(p)
            }
            generics = generics.copy(params = io.github.kotlinmania.syn.GenericParamList().also { gpl ->
                for (p in newParams) gpl.push(p) { Comma.default() }
            })
        }
        tokens.extendTokenStreams(listOf(generatedImplGenerics(generics)))
    }
}

class DeTypeGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        deTypeGenericsToTokens(params.generics.copy(), params.borrowed, tokens)
    }
}

class InPlaceImplGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        val placeLife = placeLifetime()
        var generics = params.generics.copy()

        // Add the in-place bound to all existing generic parameters.
        for (p in generics.params.toList()) {
            when (p) {
                is GenericParam.LifetimeParam -> {
                    if (p.colonToken == null) {
                        p.colonToken = io.github.kotlinmania.syn.token.Colon.default()
                    }
                    p.bounds.push(placeLife.lifetime.deepCopy()) { Plus.default() }
                }
                is GenericParam.TypeParam -> {
                    if (p.colonToken == null) {
                        p.colonToken = io.github.kotlinmania.syn.token.Colon.default()
                    }
                    p.bounds.push(TypeParamBound.LifetimeBound(placeLife.lifetime.deepCopy())) { Plus.default() }
                }
                is GenericParam.ConstParam -> {}
            }
        }

        val newParams = mutableListOf<GenericParam>()
        newParams.add(placeLife)
        for (p in generics.params.toList()) {
            newParams.add(p)
        }
        val deLifetime = params.borrowed.deLifetimeParam()
        if (deLifetime != null) {
            newParams.add(0, deLifetime)
        }
        generics = generics.copy(params = io.github.kotlinmania.syn.GenericParamList().also { gpl ->
            for (p in newParams) gpl.push(p) { Comma.default() }
        })
        tokens.extendTokenStreams(listOf(generatedImplGenerics(generics)))
    }
}

class InPlaceTypeGenerics(val params: Parameters) : ToTokens {
    override fun toTokens(tokens: TokenStream) {
        var generics = params.generics.copy()
        val placeLife = placeLifetime()
        val newParams = mutableListOf<GenericParam>()
        newParams.add(placeLife)
        for (p in generics.params.toList()) {
            newParams.add(p)
        }
        generics = generics.copy(params = io.github.kotlinmania.syn.GenericParamList().also { gpl ->
            for (p in newParams) gpl.push(p) { Comma.default() }
        })
        deTypeGenericsToTokens(generics, params.borrowed, tokens)
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
            for (p in newParams) gpl.push(p) { Comma.default() }
        })
    }
    out.extendTokenStreams(listOf(generatedTypeGenerics(mutGenerics)))
}

// Parse a generated token template into a path.
private fun parseQuotePath(
    template: String,
    vararg substitutions: Pair<String, Any?>,
): Path {
    val tokens = quoteTokens(template, *substitutions)
    val result = io.github.kotlinmania.syn.parse2(io.github.kotlinmania.syn.PathParse::parse, tokens)
    return result.getOrThrow()
}

// Generates the sequence of let-statements and result expression for
// deserializing a tuple-like sequence of fields.
internal fun deserializeSeq(
    typePath: TokenStream,
    params: Parameters,
    fields: List<Field>,
    isStruct: Boolean,
    cattrs: AttrContainer,
    expecting: String
): Fragment {
    val vars = (0 until fields.size).map { fieldI(it) }

    val deserializedCount = fields.count { !it.attrs.skipDeserializing() }
    val expectingWithCount = if (deserializedCount == 1) {
        "$expecting with 1 element"
    } else {
        "$expecting with $deserializedCount elements"
    }
    val expectingVal = cattrs.expecting() ?: expectingWithCount

    var indexInSeq = 0
    val letValues = vars.zip(fields).map { (varIdent, field) ->
        if (field.attrs.skipDeserializing()) {
            val default = Expr(exprIsMissing(field, cattrs))
            quoteTokens("let `#`varIdent = `#`default;", mapOf("varIdent" to varIdent, "default" to default))
        } else {
            val visit = when (val path = field.attrs.deserializeWith()) {
                null -> {
                    val fieldTy = field.ty
                    val span = field.original.span()
                    val func = quoteSpannedTokens(
                        span,
                        "_serde::de::SeqAccess::next_element::<`#`fieldTy>",
                        "fieldTy" to fieldTy,
                    )
                    quoteTokens("`#`func(&mut __seq)?", "func" to func)
                }
                else -> {
                    val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, path)
                    quoteTokens("""{
                        `#`wrapper
                        _serde::`#`Private::Option::map(
                            _serde::de::SeqAccess::next_element::<`#`wrapperTy>(&mut __seq)?,
                            |__wrap| __wrap.value)
                    }""", mapOf("wrapper" to wrapper, "Private" to Private, "wrapperTy" to wrapperTy))
                }
            }
            val valueIfNone = exprIsMissingSeq(null, indexInSeq, field, cattrs, expectingVal)
            indexInSeq += 1
            quoteTokens("""
                let `#`varIdent = match `#`visit {
                    _serde::`#`Private::Some(__value) => __value,
                    _serde::`#`Private::None => `#`valueIfNone,
                };
            """, mapOf(
                "varIdent" to varIdent,
                "visit" to visit,
                "Private" to Private,
                "valueIfNone" to valueIfNone,
            ))
        }
    }

    var result = if (isStruct) {
        val nameAssigns = fields.mapIndexed { i, field ->
            val member = field.member
            val v = vars[i]
            quoteTokens("`#`member: `#`v", mapOf("member" to member, "v" to v))
        }
        quoteTokens("`#`typePath { `#`(`#`nameAssigns),* }", mapOf("typePath" to typePath, "nameAssigns" to nameAssigns))
    } else {
        quoteTokens("`#`typePath(`#`(`#`vars),*)", mapOf("typePath" to typePath, "vars" to vars))
    }

    if (params.hasGetter) {
        val thisType = params.thisType
        val split = params.generics.splitForImpl()
        val tyGenerics = split.typeGenerics
        result = quoteTokens(
            "_serde::`#`Private::Into::<`#`thisType `#`tyGenerics>::into(`#`result)",
            mapOf("Private" to Private, "thisType" to thisType, "tyGenerics" to tyGenerics, "result" to result),
        )
    }

    val letDefault = when (cattrs.default()) {
        is Default.Plain -> quoteTokens("let __default: Self::Value = _serde::`#`Private::Default::default();", "Private" to Private)
        is Default.Path -> {
            val p = (cattrs.default() as Default.Path).path
            quoteSpannedTokens(p.span(), "let __default: Self::Value = `#`p();", "p" to p)
        }
        is Default.None -> quoteTokens("")
    }

    return Fragment.Block(quoteTokens("""
        `#`letDefault
        `#`(`#`letValues)*
        _serde::`#`Private::Ok(`#`result)
    """, mapOf(
        "letDefault" to letDefault,
        "letValues" to letValues,
        "Private" to Private,
        "result" to result,
    )))
}

// Generates the sequence of write-statements for in-place deserialization
// of a tuple-like sequence of fields.
internal fun deserializeSeqInPlace(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer,
    expecting: String
): Fragment {
    val deserializedCount = fields.count { !it.attrs.skipDeserializing() }
    val expectingWithCount = if (deserializedCount == 1) {
        "$expecting with 1 element"
    } else {
        "$expecting with $deserializedCount elements"
    }
    val expectingVal = cattrs.expecting() ?: expectingWithCount

    var indexInSeq = 0
    val writeValues = fields.map { field ->
        val member = memberAccess(field.member)

        if (field.attrs.skipDeserializing()) {
            val default = Expr(exprIsMissing(field, cattrs))
            quoteTokens("self.place.`#`member = `#`default;", mapOf("member" to member, "default" to default))
        } else {
            val valueIfNone = exprIsMissingSeq(
                quoteTokens("self.place.`#`member = ", "member" to member),
                indexInSeq,
                field,
                cattrs,
                expectingVal,
            )
            val write = when (val path = field.attrs.deserializeWith()) {
                null -> quoteTokens("""
                    if let _serde::`#`Private::None = _serde::de::SeqAccess::next_element_seed(&mut __seq,
                        _serde::`#`Private::de::InPlaceSeed(&mut self.place.`#`member))?
                    {
                        `#`valueIfNone;
                    }
                """, mapOf("Private" to Private, "member" to member, "valueIfNone" to valueIfNone))
                else -> {
                    val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, path)
                    quoteTokens("""{
                        `#`wrapper
                        match _serde::de::SeqAccess::next_element::<`#`wrapperTy>(&mut __seq) {
                            _serde::`#`Private::Ok(_serde::`#`Private::Some(__wrap)) => {
                                self.place.`#`member = __wrap.value;
                            }
                            _serde::`#`Private::Ok(_serde::`#`Private::None) => {
                                `#`valueIfNone;
                            }
                            _serde::`#`Private::Err(__err) => {
                                return _serde::`#`Private::Err(__err);
                            }
                        }
                    }""", mapOf(
                        "wrapper" to wrapper,
                        "wrapperTy" to wrapperTy,
                        "Private" to Private,
                        "member" to member,
                        "valueIfNone" to valueIfNone,
                    ))
                }
            }
            indexInSeq += 1
            write
        }
    }

    val thisType = params.thisType
    val split = params.generics.splitForImpl()
    val tyGenerics = split.typeGenerics
    val letDefault = when (cattrs.default()) {
        is Default.Plain -> quoteTokens(
            "let __default: `#`thisType `#`tyGenerics = _serde::`#`Private::Default::default();",
            mapOf("thisType" to thisType, "tyGenerics" to tyGenerics, "Private" to Private),
        )
        is Default.Path -> {
            val p = (cattrs.default() as Default.Path).path
            quoteSpannedTokens(
                p.span(),
                "let __default: `#`thisType `#`tyGenerics = `#`p();",
                mapOf("thisType" to thisType, "tyGenerics" to tyGenerics, "p" to p),
            )
        }
        is Default.None -> quoteTokens("")
    }

    return Fragment.Block(quoteTokens("""
        `#`letDefault
        `#`(`#`writeValues)*
        _serde::`#`Private::Ok(())
    """, mapOf("letDefault" to letDefault, "writeValues" to writeValues, "Private" to Private)))
}

// Wraps the expression in a serde deserializeWith attribute in a trait
// to prevent it from accessing the internal Deserialize state.
internal fun wrapDeserializeWith(
    params: Parameters,
    valueTy: TokenStream,
    deserializeWith: io.github.kotlinmania.syn.Expr.Path
): Pair<TokenStream, TokenStream> {
    val thisType = params.thisType
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()
    val deserializerVar = quoteTokens("__deserializer")

    // If the path returns the wrong type, the error will be reported on the
    // span of the path itself — the same span the user wrote in the attribute.
    val value = quoteSpannedTokens(
        deserializeWith.span(),
        "`#`deserializeWith(`#`deserializerVar)?",
        mapOf("deserializeWith" to deserializeWith, "deserializerVar" to deserializerVar),
    )
    val wrapper = quoteTokens("""
        `#`[doc(hidden)]
        struct __DeserializeWith `#`deImplGenerics `#`whereClause {
            value: `#`valueTy,
            phantom: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::Deserialize<`#`delife> for __DeserializeWith `#`deTyGenerics `#`whereClause {
            fn deserialize<__D>(`#`deserializerVar: __D) -> _serde::`#`Private::Result<Self, __D::Error>
            where
                __D: _serde::Deserializer<`#`delife>,
            {
                _serde::`#`Private::Ok(__DeserializeWith {
                    value: `#`value,
                    phantom: _serde::`#`Private::PhantomData,
                    lifetime: _serde::`#`Private::PhantomData,
                })
            }
        }
    """, mapOf(
        "deImplGenerics" to deImplGenerics,
        "whereClause" to whereClause,
        "valueTy" to valueTy,
        "Private" to Private,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "delife" to delife,
        "deTyGenerics" to deTyGenerics,
        "deserializerVar" to deserializerVar,
        "value" to value,
    ))
    val wrapperTy = quoteTokens("__DeserializeWith `#`deTyGenerics", "deTyGenerics" to deTyGenerics)
    return Pair(wrapper, wrapperTy)
}

internal fun wrapDeserializeFieldWith(
    params: Parameters,
    fieldTy: SynType,
    deserializeWith: io.github.kotlinmania.syn.Expr.Path
): Pair<TokenStream, TokenStream> {
    return wrapDeserializeWith(params, quoteTokens("`#`fieldTy", "fieldTy" to fieldTy), deserializeWith)
}

// Generates a closure that converts a single input parameter to the final value.
internal fun unwrapToVariantClosure(
    params: Parameters,
    variant: Variant,
    withWrapper: Boolean
): TokenStream {
    val thisValue = params.thisValue
    val variantIdent = variant.ident

    val (arg, wrapper) = if (withWrapper) {
        Pair(quoteTokens("__wrap"), quoteTokens("__wrap.value"))
    } else {
        val fieldTys = variant.fields.map { it.ty }
        Pair(quoteTokens("__wrap: (`#`(`#`fieldTys),*)", "fieldTys" to fieldTys), quoteTokens("__wrap"))
    }

    val fieldAccess = (0 until variant.fields.size).map { n ->
        Member.Unnamed(Index(n.toUInt(), Span.callSite()))
    }

    return when (variant.style) {
        Style.Struct -> if (variant.fields.size == 1) {
            val member = variant.fields[0].member
            quoteTokens(
                "|`#`arg| `#`thisValue::`#`variantIdent { `#`member: `#`wrapper }",
                mapOf(
                    "arg" to arg,
                    "thisValue" to thisValue,
                    "variantIdent" to variantIdent,
                    "member" to member,
                    "wrapper" to wrapper,
                ),
            )
        } else {
            val memberAssigns = variant.fields.mapIndexed { i, field ->
                val m = field.member
                val fa = fieldAccess[i]
                quoteTokens("`#`m: `#`wrapper.`#`fa", mapOf("m" to m, "wrapper" to wrapper, "fa" to fa))
            }
            quoteTokens(
                "|`#`arg| `#`thisValue::`#`variantIdent { `#`(`#`memberAssigns),* }",
                mapOf(
                    "arg" to arg,
                    "thisValue" to thisValue,
                    "variantIdent" to variantIdent,
                    "memberAssigns" to memberAssigns,
                ),
            )
        }
        Style.Tuple -> {
            val fieldAccesses = fieldAccess.map { fa -> quoteTokens("`#`wrapper.`#`fa", mapOf("wrapper" to wrapper, "fa" to fa)) }
            quoteTokens(
                "|`#`arg| `#`thisValue::`#`variantIdent(`#`(`#`fieldAccesses),*)",
                mapOf(
                    "arg" to arg,
                    "thisValue" to thisValue,
                    "variantIdent" to variantIdent,
                    "fieldAccesses" to fieldAccesses,
                ),
            )
        }
        Style.Newtype -> quoteTokens(
            "|`#`arg| `#`thisValue::`#`variantIdent(`#`wrapper)",
            mapOf("arg" to arg, "thisValue" to thisValue, "variantIdent" to variantIdent, "wrapper" to wrapper),
        )
        Style.Unit -> quoteTokens(
            "|`#`arg| `#`thisValue::`#`variantIdent",
            mapOf("arg" to arg, "thisValue" to thisValue, "variantIdent" to variantIdent),
        )
    }
}

// The generated parameter used for in-place deserialization.
internal fun placeLifetime(): GenericParam.LifetimeParam {
    return GenericParam.LifetimeParam(
        attrs = mutableListOf(),
        lifetime = Lifetime.new("'place", Span.callSite()),
        colonToken = null,
        bounds = io.github.kotlinmania.syn.LifetimeList()
    )
}
