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

// The submodule functions are implemented in the sibling files:
//   Enum.kt — deserializeEnum, deserializeCustomIdentifier, prepareEnumVariantEnum
//   Struct.kt — deserializeStruct, deserializeStructInPlace
//   Tuple.kt — deserializeTuple, deserializeTupleInPlace
//   Unit.kt — deserializeUnit
//   Identifier.kt — deserializeGenerated, deserializeCustom
// All are in the same package and called directly.

// Temporary stubs — will be replaced by the real implementations in
// Struct.kt when that file is rewritten from upstream Rust.
internal fun deserializeStruct(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer, form: StructForm
): Fragment = Fragment.Expr(quote(""))

internal fun deserializeStructInPlace(
    params: Parameters, fields: List<Field>, cattrs: AttrContainer
): Fragment? = null

internal fun deserializeCustomIdentifier(
    params: Parameters, variants: List<Variant>, cattrs: AttrContainer
): Fragment = deserializeCustom(params, variants, cattrs)

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
            quote("val `#`varIdent = `#`default")
        } else {
            val visit = when (val path = field.attrs.deserializeWith()) {
                null -> {
                    val fieldTy = field.ty
                    val span = field.original.span()
                    val func = quoteSpanned(span, "_serde::de::SeqAccess::next_element::<`#`fieldTy>")
                    quote("`#`func(&mut __seq)?")
                }
                else -> {
                    val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, path)
                    quote("""{
                        `#`wrapper
                        _serde.`#`Private::Option::map(
                            _serde::de::SeqAccess::next_element::<`#`wrapperTy>(&mut __seq)?,
                            |__wrap| __wrap.value)
                    }""")
                }
            }
            val valueIfNone = exprIsMissingSeq(null, indexInSeq, field, cattrs, expectingVal)
            indexInSeq += 1
            quote("""
                val `#`varIdent = when `#`visit {
                    _serde.`#`Private::Some(__value) -> __value
                    _serde.`#`Private::None -> `#`valueIfNone
                }
            """)
        }
    }

    var result = if (isStruct) {
        val nameAssigns = fields.mapIndexed { i, field ->
            val member = field.member
            val v = vars[i]
            quote("`#`member: `#`v")
        }
        quote("`#`typePath { `#`(`#`nameAssigns),* }")
    } else {
        quote("`#`typePath(`#`(`#`vars),*)")
    }

    if (params.hasGetter) {
        val thisType = params.thisType
        val split = params.generics.splitForImpl()
        val tyGenerics = split.typeGenerics
        result = quote("_serde.`#`Private::Into::<`#`thisType `#`tyGenerics>::into(`#`result)")
    }

    val letDefault = when (cattrs.default()) {
        is Default.Plain -> quote("val __default: Self.Value = _serde.`#`Private::Default::default()")
        is Default.Path -> {
            val p = (cattrs.default() as Default.Path).path
            quoteSpanned(p.span(), "val __default: Self.Value = `#`p()")
        }
        is Default.None -> quote("")
    }

    return Fragment.Block(quote("""
        `#`letDefault
        `#`(`#`letValues)*
        _serde.`#`Private::Ok(`#`result)
    """))
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
        val member = field.member

        if (field.attrs.skipDeserializing()) {
            val default = Expr(exprIsMissing(field, cattrs))
            quote("self.place.`#`member = `#`default")
        } else {
            val valueIfNone = exprIsMissingSeq(quote("self.place.`#`member = "), indexInSeq, field, cattrs, expectingVal)
            val write = when (val path = field.attrs.deserializeWith()) {
                null -> quote("""
                    if _serde.`#`Private::None == _serde::de::SeqAccess::next_element_seed(&mut __seq,
                        _serde.`#`Private::de::InPlaceSeed(&mut self.place.`#`member))?
                    {
                        `#`valueIfNone
                    }
                """)
                else -> {
                    val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, path)
                    quote("""{
                        `#`wrapper
                        when _serde::de::SeqAccess::next_element::<`#`wrapperTy>(&mut __seq) {
                            _serde.`#`Private::Ok(_serde.`#`Private::Some(__wrap)) -> {
                                self.place.`#`member = __wrap.value
                            }
                            _serde.`#`Private::Ok(_serde.`#`Private::None) -> {
                                `#`valueIfNone
                            }
                            _serde.`#`Private::Err(__err) -> {
                                return _serde.`#`Private::Err(__err)
                            }
                        }
                    }""")
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
        is Default.Plain -> quote("val __default: `#`thisType `#`tyGenerics = _serde.`#`Private::Default::default()")
        is Default.Path -> {
            val p = (cattrs.default() as Default.Path).path
            quoteSpanned(p.span(), "val __default: `#`thisType `#`tyGenerics = `#`p()")
        }
        is Default.None -> quote("")
    }

    return Fragment.Block(quote("""
        `#`letDefault
        `#`(`#`writeValues)*
        _serde.`#`Private::Ok(())
    """))
}

// Wraps the expression in `#[serde(deserialize_with = "...")]` in a trait
// to prevent it from accessing the internal Deserialize state.
internal fun wrapDeserializeWith(
    params: Parameters,
    valueTy: TokenStream,
    deserializeWith: io.github.kotlinmania.syn.Expr.Path
): Pair<TokenStream, TokenStream> {
    val thisType = params.thisType
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()
    val deserializerVar = quote("__deserializer")

    // If the path returns the wrong type, the error will be reported on the
    // span of the path itself — the same span the user wrote in the attribute.
    val value = quoteSpanned(deserializeWith.span(), "`#`deserializeWith(`#`deserializerVar)?")
    val wrapper = quote("""
        `#`[doc(hidden)]
        struct __DeserializeWith `#`deImplGenerics `#`whereClause {
            value: `#`valueTy,
            phantom: _serde.`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde.`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::Deserialize<`#`delife> for __DeserializeWith `#`deTyGenerics `#`whereClause {
            fun deserialize<__D>(`#`deserializerVar: __D) -> _serde.`#`Private::Result<this, __D.Error>
            where
                __D: _serde::Deserializer<`#`delife>,
            {
                _serde.`#`Private::Ok(__DeserializeWith {
                    value: `#`value,
                    phantom: _serde.`#`Private::PhantomData,
                    lifetime: _serde.`#`Private::PhantomData,
                })
            }
        }
    """)
    val wrapperTy = quote("__DeserializeWith `#`deTyGenerics")
    return Pair(wrapper, wrapperTy)
}

internal fun wrapDeserializeFieldWith(
    params: Parameters,
    fieldTy: SynType,
    deserializeWith: io.github.kotlinmania.syn.Expr.Path
): Pair<TokenStream, TokenStream> {
    return wrapDeserializeWith(params, quote("`#`fieldTy"), deserializeWith)
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
        Pair(quote("__wrap"), quote("__wrap.value"))
    } else {
        val fieldTys = variant.fields.map { it.ty }
        Pair(quote("__wrap: (`#`(`#`fieldTys),*)"), quote("__wrap"))
    }

    val fieldAccess = (0 until variant.fields.size).map { n ->
        Member.Unnamed(Index(n.toUInt(), Span.callSite()))
    }

    return when (variant.style) {
        Style.Struct -> if (variant.fields.size == 1) {
            val member = variant.fields[0].member
            quote("|`#`arg| `#`thisValue::`#`variantIdent { `#`member: `#`wrapper }")
        } else {
            val memberAssigns = variant.fields.mapIndexed { i, field ->
                val m = field.member
                val fa = fieldAccess[i]
                quote("`#`m: `#`wrapper.`#`fa")
            }
            quote("|`#`arg| `#`thisValue::`#`variantIdent { `#`(`#`memberAssigns),* }")
        }
        Style.Tuple -> {
            val fieldAccesses = fieldAccess.map { fa -> quote("`#`wrapper.`#`fa") }
            quote("|`#`arg| `#`thisValue::`#`variantIdent(`#`(`#`fieldAccesses),*)")
        }
        Style.Newtype -> quote("|`#`arg| `#`thisValue::`#`variantIdent(`#`wrapper)")
        Style.Unit -> quote("|`#`arg| `#`thisValue::`#`variantIdent")
    }
}

// The lifetime parameter used for in-place deserialization.
internal fun placeLifetime(): GenericParam.LifetimeParam {
    return GenericParam.LifetimeParam(
        attrs = mutableListOf(),
        lifetime = Lifetime.new("'place", Span.callSite()),
        colonToken = null,
        bounds = io.github.kotlinmania.syn.LifetimeList()
    )
}