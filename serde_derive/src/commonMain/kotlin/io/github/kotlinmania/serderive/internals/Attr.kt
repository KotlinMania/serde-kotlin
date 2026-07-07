package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.*
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.Data as SynData

// Extension: convert any object with a toTokens(TokenStream) method to a TokenStream.
// Needed because syn-kotlin Path doesn't implement ToTokens.
private fun Path.toTokenStream(): TokenStream {
    val out = TokenStream.new()
    toTokens(out)
    return out
}

private fun SynType.toTokenStream(): TokenStream {
    val out = TokenStream.new()
    toTokens(out)
    return out
}

// Wrap a Path as a ToTokens for passing to Attr.set / Ctxt.errorSpannedBy
private class PathTokens(private val path: Path) : ToTokens {
    override fun toTokens(out: TokenStream) {
        path.toTokens(out)
    }
}

private fun Path.asToTokens(): ToTokens = PathTokens(this)

// Helper: build a PathSegmentList from a list of PathSegments
private fun pathSegmentListFrom(segments: List<PathSegment>): PathSegmentList {
    val list = PathSegmentList()
    for (seg in segments) {
        list.pushValue(seg)
    }
    return list
}

// Extension: "deep copy" an Ident (Ident has no deepCopy method)
private fun Ident.deepCopy(): Ident = Ident.new(this.toString(), this.span())

// Check if a ParseNestedMeta's input starts with `=` (i.e., `attr = "value"`)
private fun metaPeekEq(meta: ParseNestedMeta): Boolean {
    val valueTokens = meta.value()
    val eqParse = object : io.github.kotlinmania.syn.Parse<Boolean> {
        override fun parse(input: io.github.kotlinmania.syn.ParseBuffer): io.github.kotlinmania.syn.SynResult<Boolean> {
            val eqResult = input.parse(io.github.kotlinmania.syn.EqParse)
            return io.github.kotlinmania.syn.SynResult.success(eqResult.isSuccess)
        }
    }
    val result = io.github.kotlinmania.syn.parse2(eqParse, valueTokens)
    return result.isSuccess && result.getOrThrow()
}

// Convert any ToTokens or Path-like object to a TokenStream
private fun toTokenStream(obj: Any): TokenStream {
    return when (obj) {
        is ToTokens -> obj.toTokenStream()
        is Path -> {
            val out = TokenStream.new()
            obj.toTokens(out)
            out
        }
        is SynType -> {
            val out = TokenStream.new()
            obj.toTokens(out)
            out
        }
        is TokenStream -> obj
        else -> throw IllegalArgumentException("Cannot convert $obj to TokenStream")
    }
}

internal class Attr<T>(
    private val cx: Ctxt,
    private val name: Symbol,
    private var tokens: TokenStream = TokenStream.new(),
    private var value: T? = null
) {
    fun set(obj: Any, value: T) {
        val tokens = toTokenStream(obj)

        if (this.value != null) {
            val msg = "duplicate serde attribute `#`name"
            this.cx.errorSpannedBy(tokens, msg)
        } else {
            this.tokens = tokens
            this.value = value
        }
    }

    fun setOpt(obj: Any, value: T?) {
        if (value != null) {
            this.set(obj, value)
        }
    }

    fun setIfNone(value: T) {
        if (this.value == null) {
            this.value = value
        }
    }

    fun get(): T? {
        return this.value
    }

    fun getWithTokens(): Pair<TokenStream, T>? {
        return this.value?.let { Pair(this.tokens, it) }
    }
}

internal class BoolAttr(private val attr: Attr<Unit>) {
    fun setTrue(obj: Any) {
        attr.set(obj, Unit)
    }

    fun get(): Boolean {
        return attr.get() != null
    }

    companion object {
        fun none(cx: Ctxt, name: Symbol): BoolAttr {
            return BoolAttr(Attr(cx, name))
        }
    }
}

internal class VecAttr<T>(
    private val cx: Ctxt,
    private val name: Symbol,
    private var firstDupTokens: TokenStream = TokenStream.new(),
    private val values: MutableList<T> = mutableListOf()
) {
    fun insert(obj: Any, value: T) {
        if (values.size == 1) {
            this.firstDupTokens = toTokenStream(obj)
        }
        this.values.add(value)
    }

    fun atMostOne(): T? {
        if (values.size > 1) {
            val dupToken = this.firstDupTokens
            val msg = "duplicate serde attribute `#`name"
            this.cx.errorSpannedBy(dupToken, msg)
            return null
        } else {
            return values.lastOrNull()
        }
    }

    fun get(): List<T> {
        return this.values
    }

    companion object {
        fun <T> none(cx: Ctxt, name: Symbol): VecAttr<T> {
            return VecAttr(cx, name)
        }
    }
}

private fun unraw(ident: Ident): Ident {
    return Ident.new(ident.toString().removePrefix("r#"), ident.span())
}

public class RenameAllRules(
    public val serialize: RenameRule,
    public val deserialize: RenameRule
) {
    public fun or(otherRules: RenameAllRules): RenameAllRules {
        return RenameAllRules(
            serialize = this.serialize.or(otherRules.serialize),
            deserialize = this.deserialize.or(otherRules.deserialize)
        )
    }
}

public class AttrContainer(
    private val name: MultiName,
    private val transparent: Boolean,
    private val denyUnknownFields: Boolean,
    private val default: Default,
    private val renameAllRules: RenameAllRules,
    private val renameAllFieldsRules: RenameAllRules,
    private val serBound: List<WherePredicate>?,
    private val deBound: List<WherePredicate>?,
    private val tag: TagType,
    private val typeFrom: SynType?,
    private val typeTryFrom: SynType?,
    private val typeInto: SynType?,
    private val remote: Path?,
    private val identifier: Identifier,
    private val serdePath: Path?,
    private val isPacked: Boolean,
    private val expecting: String?,
    private val nonExhaustive: Boolean
) {
    public fun name(): MultiName = name
    public fun transparent(): Boolean = transparent
    public fun denyUnknownFields(): Boolean = denyUnknownFields
    public fun default(): Default = default
    public fun renameAllRules(): RenameAllRules = renameAllRules
    public fun renameAllFieldsRules(): RenameAllRules = renameAllFieldsRules
    public fun serBound(): List<WherePredicate>? = serBound
    public fun deBound(): List<WherePredicate>? = deBound
    public fun tag(): TagType = tag
    public fun typeFrom(): SynType? = typeFrom
    public fun typeTryFrom(): SynType? = typeTryFrom
    public fun typeInto(): SynType? = typeInto
    public fun remote(): Path? = remote
    public fun identifier(): Identifier = identifier
    public fun serdePath(): Path? = serdePath
    public fun isPacked(): Boolean = isPacked
    public fun expecting(): String? = expecting
    public fun nonExhaustive(): Boolean = nonExhaustive

    public companion object {
        public fun fromAst(cx: Ctxt, item: DeriveInput): AttrContainer {
            val serName = Attr<Name>(cx, RENAME)
            val deName = Attr<Name>(cx, RENAME)
            val transparent = BoolAttr.none(cx, TRANSPARENT)
            val denyUnknownFields = BoolAttr.none(cx, DENY_UNKNOWN_FIELDS)
            val default = Attr<Default>(cx, DEFAULT)
            val renameAllSerRule = Attr<RenameRule>(cx, RENAME_ALL)
            val renameAllDeRule = Attr<RenameRule>(cx, RENAME_ALL)
            val renameAllFieldsSerRule = Attr<RenameRule>(cx, RENAME_ALL_FIELDS)
            val renameAllFieldsDeRule = Attr<RenameRule>(cx, RENAME_ALL_FIELDS)
            val serBound = Attr<List<WherePredicate>>(cx, BOUND)
            val deBound = Attr<List<WherePredicate>>(cx, BOUND)
            val untagged = BoolAttr.none(cx, UNTAGGED)
            val internalTag = Attr<String>(cx, TAG)
            val content = Attr<String>(cx, CONTENT)
            val typeFrom = Attr<SynType>(cx, FROM)
            val typeTryFrom = Attr<SynType>(cx, TRY_FROM)
            val typeInto = Attr<SynType>(cx, INTO)
            val remote = Attr<Path>(cx, REMOTE)
            val fieldIdentifier = BoolAttr.none(cx, FIELD_IDENTIFIER)
            val variantIdentifier = BoolAttr.none(cx, VARIANT_IDENTIFIER)
            val serdePath = Attr<Path>(cx, CRATE)
            val expecting = Attr<String>(cx, EXPECTING)
            var nonExhaustive = false

            for (attr in item.attrs) {
                if (SERDE != attr.path()) {
                    val meta = attr.meta
                    if (meta is Meta.PathMeta && NON_EXHAUSTIVE == meta.path) {
                        nonExhaustive = true
                    }
                    continue
                }

                val metaVal = attr.meta
                if (metaVal is Meta.List && metaVal.tokens.isEmpty()) {
                    continue
                }

                try {
                    attr.parseNestedMeta { meta ->
                        if (RENAME == meta.path) {
                            val (ser, de) = getRenames(cx, RENAME, meta)
                            serName.setOpt(meta.path, ser?.let { Name.from(it) })
                            deName.setOpt(meta.path, de?.let { Name.from(it) })
                        } else if (RENAME_ALL == meta.path) {
                            val oneName = metaPeekEq(meta)
                            val (ser, de) = getRenames(cx, RENAME_ALL, meta)
                            if (ser != null) {
                                try {
                                    renameAllSerRule.set(meta.path, RenameRule.fromStr(ser.value()))
                                } catch (e: ParseError) {
                                    cx.errorSpannedBy(ser, e.message ?: "")
                                }
                            }
                            if (de != null) {
                                try {
                                    renameAllDeRule.set(meta.path, RenameRule.fromStr(de.value()))
                                } catch (e: ParseError) {
                                    if (!oneName) cx.errorSpannedBy(de, e.message ?: "")
                                }
                            }
                        } else if (RENAME_ALL_FIELDS == meta.path) {
                            val oneName = metaPeekEq(meta)
                            val (ser, de) = getRenames(cx, RENAME_ALL_FIELDS, meta)
                            when (item.data) {
                                is SynData.Enum -> {
                                    if (ser != null) {
                                        try {
                                            renameAllFieldsSerRule.set(meta.path, RenameRule.fromStr(ser.value()))
                                        } catch (e: ParseError) {
                                            cx.errorSpannedBy(ser, e.message ?: "")
                                        }
                                    }
                                    if (de != null) {
                                        try {
                                            renameAllFieldsDeRule.set(meta.path, RenameRule.fromStr(de.value()))
                                        } catch (e: ParseError) {
                                            if (!oneName) cx.errorSpannedBy(de, e.message ?: "")
                                        }
                                    }
                                }
                                is SynData.Struct -> cx.synError(meta.error("#[serde(rename_all_fields)] can only be used on enums"))
                                is SynData.Union -> cx.synError(meta.error("#[serde(rename_all_fields)] can only be used on enums"))
                            }
                        } else if (TRANSPARENT == meta.path) {
                            transparent.setTrue(meta.path)
                        } else if (DENY_UNKNOWN_FIELDS == meta.path) {
                            denyUnknownFields.setTrue(meta.path)
                        } else if (DEFAULT == meta.path) {
                            if (metaPeekEq(meta)) {
                                val path = parseLitIntoExprPath(cx, DEFAULT, meta)
                                if (path != null) {
                                    val itemData = item.data
                                    if (itemData is SynData.Struct) {
                                        if (itemData.fields is Fields.Named || itemData.fields is Fields.Unnamed) {
                                            default.set(meta.path, Default.Path(path))
                                        } else {
                                            cx.synError(meta.error("#[serde(default = \"...\")] can only be used on structs that have fields"))
                                        }
                                    } else {
                                        cx.synError(meta.error("#[serde(default = \"...\")] can only be used on structs"))
                                    }
                                }
                            } else {
                                val itemData = item.data
                                if (itemData is SynData.Struct) {
                                    if (itemData.fields is Fields.Named || itemData.fields is Fields.Unnamed) {
                                        default.set(meta.path, Default.Plain)
                                    } else {
                                        cx.errorSpannedBy(itemData.fields, "#[serde(default)] can only be used on structs that have fields")
                                    }
                                } else {
                                    cx.synError(meta.error("#[serde(default)] can only be used on structs"))
                                }
                            }
                        } else if (BOUND == meta.path) {
                            val (ser, de) = getWherePredicates(cx, meta)
                            serBound.setOpt(meta.path, ser)
                            deBound.setOpt(meta.path, de)
                        } else if (UNTAGGED == meta.path) {
                            if (item.data is SynData.Enum) {
                                untagged.setTrue(meta.path)
                            } else {
                                cx.synError(meta.error("#[serde(untagged)] can only be used on enums"))
                            }
                        } else if (TAG == meta.path) {
                            val s = getLitStr(cx, TAG, meta)
                            if (s != null) {
                                val itemData = item.data
                                if (itemData is SynData.Enum) {
                                    internalTag.set(meta.path, s.value())
                                } else if (itemData is SynData.Struct && itemData.fields is Fields.Named) {
                                    internalTag.set(meta.path, s.value())
                                } else {
                                    cx.synError(meta.error("#[serde(tag = \"...\")] can only be used on enums and structs with named fields"))
                                }
                            }
                        } else if (CONTENT == meta.path) {
                            val s = getLitStr(cx, CONTENT, meta)
                            if (s != null) {
                                if (item.data is SynData.Enum) {
                                    content.set(meta.path, s.value())
                                } else {
                                    cx.synError(meta.error("#[serde(content = \"...\")] can only be used on enums"))
                                }
                            }
                        } else if (FROM == meta.path) {
                            val fromTy = parseLitIntoTy(cx, FROM, meta)
                            typeFrom.setOpt(meta.path, fromTy)
                        } else if (TRY_FROM == meta.path) {
                            val tryFromTy = parseLitIntoTy(cx, TRY_FROM, meta)
                            typeTryFrom.setOpt(meta.path, tryFromTy)
                        } else if (INTO == meta.path) {
                            val intoTy = parseLitIntoTy(cx, INTO, meta)
                            typeInto.setOpt(meta.path, intoTy)
                        } else if (REMOTE == meta.path) {
                            val path = parseLitIntoPath(cx, REMOTE, meta)
                            if (path != null) {
                                if (isPrimitivePath(path, "this")) {
                                    remote.set(meta.path, Path.from(Path.from(item.ident)))
                                } else {
                                    remote.set(meta.path, path)
                                }
                            }
                        } else if (FIELD_IDENTIFIER == meta.path) {
                            fieldIdentifier.setTrue(meta.path)
                        } else if (VARIANT_IDENTIFIER == meta.path) {
                            variantIdentifier.setTrue(meta.path)
                        } else if (CRATE == meta.path) {
                            val path = parseLitIntoPath(cx, CRATE, meta)
                            serdePath.setOpt(meta.path, path)
                        } else if (EXPECTING == meta.path) {
                            val s = getLitStr(cx, EXPECTING, meta)
                            if (s != null) {
                                expecting.set(meta.path, s.value())
                            }
                        } else {
                            val pathStr = meta.path.toTokenStream().toString().replace(" ", "")
                            throw meta.error("unknown serde container attribute `$pathStr`")
                        }
                        SynResult.success(Unit)
                    }
                } catch (err: SynError) {
                    cx.synError(err)
                }
            }

            var isPacked = false
            for (attr in item.attrs) {
                if (REPR == attr.path()) {
                    val metaVal = attr.meta
                    if (metaVal is Meta.List) {
                        val tokensStr = metaVal.tokens.toString().replace(" ", "")
                        isPacked = tokensStr.contains("packed")
                    }
                }
            }

            return AttrContainer(
                name = MultiName.fromAttrs(Name.from(unraw(item.ident)), serName, deName, null),
                transparent = transparent.get(),
                denyUnknownFields = denyUnknownFields.get(),
                default = default.get() ?: Default.None,
                renameAllRules = RenameAllRules(
                    serialize = renameAllSerRule.get() ?: RenameRule.None,
                    deserialize = renameAllDeRule.get() ?: RenameRule.None
                ),
                renameAllFieldsRules = RenameAllRules(
                    serialize = renameAllFieldsSerRule.get() ?: RenameRule.None,
                    deserialize = renameAllFieldsDeRule.get() ?: RenameRule.None
                ),
                serBound = serBound.get(),
                deBound = deBound.get(),
                tag = decideTag(cx, item, untagged, internalTag, content),
                typeFrom = typeFrom.get(),
                typeTryFrom = typeTryFrom.get(),
                typeInto = typeInto.get(),
                remote = remote.get(),
                identifier = decideIdentifier(cx, item, fieldIdentifier, variantIdentifier),
                serdePath = serdePath.get(),
                isPacked = isPacked,
                expecting = expecting.get(),
                nonExhaustive = nonExhaustive
            )
        }
    }

    // APPEND_HERE
}

private fun decideTag(
    cx: Ctxt,
    item: DeriveInput,
    untagged: BoolAttr,
    internalTag: Attr<String>,
    content: Attr<String>
): TagType {
    val untaggedTokens = untagged.getWithTokens()
    val internalTagTokens = internalTag.getWithTokens()
    val contentTokens = content.getWithTokens()

    if (untaggedTokens == null && internalTagTokens == null && contentTokens == null) {
        return TagType.External
    }
    if (untaggedTokens != null && internalTagTokens == null && contentTokens == null) {
        return TagType.None
    }
    if (untaggedTokens == null && internalTagTokens != null && contentTokens == null) {
        val tag = internalTagTokens.second
        val itemData = item.data
        if (itemData is SynData.Enum) {
            for (variant in itemData.variants) {
                val synVariant = variant as Variant
                if (synVariant.fields is Fields.Unnamed) {
                    if (synVariant.fields.fields.unnamed.size != 1) {
                        cx.errorSpannedBy(synVariant, "#[serde(tag = \"...\")] cannot be used with tuple variants")
                        break
                    }
                }
            }
        }
        return TagType.Internal(tag)
    }
    if (untaggedTokens != null && internalTagTokens != null && contentTokens == null) {
        val msg = "enum cannot be both untagged and internally tagged"
        cx.errorSpannedBy(untaggedTokens.first, msg)
        cx.errorSpannedBy(internalTagTokens.first, msg)
        return TagType.External
    }
    if (untaggedTokens == null && internalTagTokens == null && contentTokens != null) {
        val msg = "#[serde(tag = \"...\", content = \"...\")] must be used together"
        cx.errorSpannedBy(contentTokens.first, msg)
        return TagType.External
    }
    if (untaggedTokens != null && internalTagTokens == null && contentTokens != null) {
        val msg = "untagged enum cannot have #[serde(content = \"...\")]"
        cx.errorSpannedBy(untaggedTokens.first, msg)
        cx.errorSpannedBy(contentTokens.first, msg)
        return TagType.External
    }
    if (untaggedTokens == null && internalTagTokens != null && contentTokens != null) {
        return TagType.Adjacent(internalTagTokens.second, contentTokens.second)
    }
    if (untaggedTokens != null && internalTagTokens != null && contentTokens != null) {
        val msg = "untagged enum cannot have #[serde(tag = \"...\", content = \"...\")]"
        cx.errorSpannedBy(untaggedTokens.first, msg)
        cx.errorSpannedBy(internalTagTokens.first, msg)
        cx.errorSpannedBy(contentTokens.first, msg)
        return TagType.External
    }
    return TagType.External
}

private fun decideIdentifier(
    cx: Ctxt,
    item: DeriveInput,
    fieldIdentifier: BoolAttr,
    variantIdentifier: BoolAttr
): Identifier {
    val fieldTokens = fieldIdentifier.getWithTokens()
    val variantTokens = variantIdentifier.getWithTokens()

    if (fieldTokens == null && variantTokens == null) {
        return Identifier.No
    }
    if (fieldTokens != null && variantTokens != null) {
        val msg = "#[serde(field_identifier)] and #[serde(variant_identifier)] cannot both be set"
        cx.errorSpannedBy(fieldTokens.first, msg)
        cx.errorSpannedBy(variantTokens.first, msg)
        return Identifier.No
    }
    if (item.data is SynData.Enum && fieldTokens != null && variantTokens == null) {
        return Identifier.Field
    }
    if (item.data is SynData.Enum && fieldTokens == null && variantTokens != null) {
        return Identifier.Variant
    }
    if (item.data is SynData.Struct && fieldTokens != null && variantTokens == null) {
        val msg = "#[serde(field_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as SynData.Struct).value.structToken, msg)
        return Identifier.No
    }
    if (item.data is SynData.Union && fieldTokens != null && variantTokens == null) {
        val msg = "#[serde(field_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as SynData.Union).value.unionToken, msg)
        return Identifier.No
    }
    if (item.data is SynData.Struct && fieldTokens == null && variantTokens != null) {
        val msg = "#[serde(variant_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as SynData.Struct).value.structToken, msg)
        return Identifier.No
    }
    if (item.data is SynData.Union && fieldTokens == null && variantTokens != null) {
        val msg = "#[serde(variant_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as SynData.Union).value.unionToken, msg)
        return Identifier.No
    }
    return Identifier.No
}

public class AttrVariant(
    private val name: MultiName,
    private val renameAllRules: RenameAllRules,
    private val serBound: List<WherePredicate>?,
    private val deBound: List<WherePredicate>?,
    private val skipDeserializing: Boolean,
    private val skipSerializing: Boolean,
    private val other: Boolean,
    private val serializeWith: io.github.kotlinmania.syn.Expr.Path?,
    private val deserializeWith: io.github.kotlinmania.syn.Expr.Path?,
    private val borrow: BorrowAttribute?,
    private val untagged: Boolean
) {
    public fun name(): MultiName = name
    public fun renameAllRules(): RenameAllRules = renameAllRules
    public fun serBound(): List<WherePredicate>? = serBound
    public fun deBound(): List<WherePredicate>? = deBound
    public fun skipDeserializing(): Boolean = skipDeserializing
    public fun skipSerializing(): Boolean = skipSerializing
    public fun other(): Boolean = other
    public fun serializeWith(): io.github.kotlinmania.syn.Expr.Path? = serializeWith
    public fun deserializeWith(): io.github.kotlinmania.syn.Expr.Path? = deserializeWith
    public fun untagged(): Boolean = untagged
    public fun borrow(): BorrowAttribute? = borrow

    public fun renameByRules(rules: RenameAllRules) {
        if (!name.serializeRenamed) {
            name.serialize.value = rules.serialize.applyToVariant(name.serialize.value)
        }
        if (!name.deserializeRenamed) {
            name.deserialize.value = rules.deserialize.applyToVariant(name.deserialize.value)
        }
        val aliases = name.deserializeAliases().toList()
        for (alias in aliases) {
            alias.value = rules.deserialize.applyToVariant(alias.value)
        }
    }

    public companion object {
        public fun fromAst(cx: Ctxt, variant: io.github.kotlinmania.syn.Variant): AttrVariant {
            val serName = Attr<Name>(cx, RENAME)
            val deName = Attr<Name>(cx, RENAME)
            val deAliases = VecAttr.none<Name>(cx, RENAME)
            val skipDeserializing = BoolAttr.none(cx, SKIP_DESERIALIZING)
            val skipSerializing = BoolAttr.none(cx, SKIP_SERIALIZING)
            val renameAllSerRule = Attr<RenameRule>(cx, RENAME_ALL)
            val renameAllDeRule = Attr<RenameRule>(cx, RENAME_ALL)
            val serBound = Attr<List<WherePredicate>>(cx, BOUND)
            val deBound = Attr<List<WherePredicate>>(cx, BOUND)
            val other = BoolAttr.none(cx, OTHER)
            val serializeWith = Attr<io.github.kotlinmania.syn.Expr.Path>(cx, SERIALIZE_WITH)
            val deserializeWith = Attr<io.github.kotlinmania.syn.Expr.Path>(cx, DESERIALIZE_WITH)
            val borrow = Attr<BorrowAttribute>(cx, BORROW)
            val untagged = BoolAttr.none(cx, UNTAGGED)

            for (attr in variant.attrs.toList()) {
                if (SERDE != attr.path()) {
                    continue
                }

                val metaVal = attr.meta
                if (metaVal is Meta.List && metaVal.tokens.isEmpty()) {
                    continue
                }

                try {
                    attr.parseNestedMeta { meta ->
                        if (RENAME == meta.path) {
                            val (ser, de) = getMultipleRenames(cx, meta)
                            serName.setOpt(meta.path, ser?.let { Name.from(it) })
                            for (deValue in de) {
                                deName.setIfNone(Name.from(deValue))
                                deAliases.insert(meta.path, Name.from(deValue))
                            }
                        } else if (ALIAS == meta.path) {
                            val s = getLitStr(cx, ALIAS, meta)
                            if (s != null) {
                                deAliases.insert(meta.path, Name.from(s))
                            }
                        } else if (RENAME_ALL == meta.path) {
                            val oneName = metaPeekEq(meta)
                            val (ser, de) = getRenames(cx, RENAME_ALL, meta)
                            if (ser != null) {
                                try {
                                    renameAllSerRule.set(meta.path, RenameRule.fromStr(ser.value()))
                                } catch (e: ParseError) {
                                    cx.errorSpannedBy(ser, e.message ?: "")
                                }
                            }
                            if (de != null) {
                                try {
                                    renameAllDeRule.set(meta.path, RenameRule.fromStr(de.value()))
                                } catch (e: ParseError) {
                                    if (!oneName) cx.errorSpannedBy(de, e.message ?: "")
                                }
                            }
                        } else if (SKIP == meta.path) {
                            skipSerializing.setTrue(meta.path)
                            skipDeserializing.setTrue(meta.path)
                        } else if (SKIP_DESERIALIZING == meta.path) {
                            skipDeserializing.setTrue(meta.path)
                        } else if (SKIP_SERIALIZING == meta.path) {
                            skipSerializing.setTrue(meta.path)
                        } else if (OTHER == meta.path) {
                            other.setTrue(meta.path)
                        } else if (BOUND == meta.path) {
                            val (ser, de) = getWherePredicates(cx, meta)
                            serBound.setOpt(meta.path, ser)
                            deBound.setOpt(meta.path, de)
                        } else if (WITH == meta.path) {
                            val path = parseLitIntoExprPath(cx, WITH, meta)
                            if (path != null) {
                                var serPath = path.deepCopy()
                                val serSegs = serPath.path.segments.toList().toMutableList()
                                serSegs.add(PathSegment(Ident.new("serialize", serPath.span()), PathArguments.None))
                                serPath = serPath.copy(path = Path(serPath.path.leadingColon, pathSegmentListFrom(serSegs)))
                                serializeWith.set(meta.path, serPath)

                                var dePath = path.deepCopy()
                                val deSegs = dePath.path.segments.toList().toMutableList()
                                deSegs.add(PathSegment(Ident.new("deserialize", dePath.span()), PathArguments.None))
                                dePath = dePath.copy(path = Path(dePath.path.leadingColon, pathSegmentListFrom(deSegs)))
                                deserializeWith.set(meta.path, dePath)
                            }
                        } else if (SERIALIZE_WITH == meta.path) {
                            val path = parseLitIntoExprPath(cx, SERIALIZE_WITH, meta)
                            serializeWith.setOpt(meta.path, path)
                        } else if (DESERIALIZE_WITH == meta.path) {
                            val path = parseLitIntoExprPath(cx, DESERIALIZE_WITH, meta)
                            deserializeWith.setOpt(meta.path, path)
                        } else if (BORROW == meta.path) {
                            val borrowAttribute = if (metaPeekEq(meta)) {
                                val lifetimes = parseLitIntoLifetimes(cx, meta)
                                BorrowAttribute(meta.path.deepCopy(), lifetimes.toSet())
                            } else {
                                BorrowAttribute(meta.path.deepCopy(), null)
                            }
                            if (variant.fields is Fields.Unnamed && (variant.fields as Fields.Unnamed).fields.unnamed.size == 1) {
                                borrow.set(meta.path, borrowAttribute)
                            } else {
                                cx.errorSpannedBy(variant, "#[serde(borrow)] may only be used on newtype variants")
                            }
                        } else if (UNTAGGED == meta.path) {
                            untagged.setTrue(meta.path)
                        } else {
                            val pathStr = meta.path.toTokenStream().toString().replace(" ", "")
                            throw meta.error("unknown serde variant attribute `$pathStr`")
                        }
                        SynResult.success(Unit)
                    }
                } catch (err: SynError) {
                    cx.synError(err)
                }
            }

            return AttrVariant(
                name = MultiName.fromAttrs(Name.from(unraw(variant.ident)), serName, deName, deAliases),
                renameAllRules = RenameAllRules(
                    serialize = renameAllSerRule.get() ?: RenameRule.None,
                    deserialize = renameAllDeRule.get() ?: RenameRule.None
                ),
                serBound = serBound.get(),
                deBound = deBound.get(),
                skipDeserializing = skipDeserializing.get(),
                skipSerializing = skipSerializing.get(),
                other = other.get(),
                serializeWith = serializeWith.get(),
                deserializeWith = deserializeWith.get(),
                borrow = borrow.get(),
                untagged = untagged.get()
            )
        }
    }

    // APPEND_HERE
}

public class AttrField(
    private val name: MultiName,
    private val skipSerializing: Boolean,
    private val skipDeserializing: Boolean,
    private val skipSerializingIf: io.github.kotlinmania.syn.Expr.Path?,
    private val default: Default,
    private val serializeWith: io.github.kotlinmania.syn.Expr.Path?,
    private val deserializeWith: io.github.kotlinmania.syn.Expr.Path?,
    private val serBound: List<WherePredicate>?,
    private val deBound: List<WherePredicate>?,
    private val borrowedLifetimes: Set<Lifetime>,
    private val getter: io.github.kotlinmania.syn.Expr.Path?,
    private val flatten: Boolean,
    private var transparent: Boolean
) {
    public fun name(): MultiName = name
    public fun skipSerializing(): Boolean = skipSerializing
    public fun skipDeserializing(): Boolean = skipDeserializing
    public fun skipSerializingIf(): io.github.kotlinmania.syn.Expr.Path? = skipSerializingIf
    public fun default(): Default = default
    public fun serializeWith(): io.github.kotlinmania.syn.Expr.Path? = serializeWith
    public fun deserializeWith(): io.github.kotlinmania.syn.Expr.Path? = deserializeWith
    public fun serBound(): List<WherePredicate>? = serBound
    public fun deBound(): List<WherePredicate>? = deBound
    public fun borrowedLifetimes(): Set<Lifetime> = borrowedLifetimes
    public fun getter(): io.github.kotlinmania.syn.Expr.Path? = getter
    public fun flatten(): Boolean = flatten
    public fun transparent(): Boolean = transparent

    public fun markTransparent() {
        transparent = true
    }

    public fun isNone(): Boolean {
        return false // null is not used here properly, just return false
    }

    public companion object {
    public fun fromAst(
        cx: Ctxt,
        index: Int,
        field: io.github.kotlinmania.syn.Field,
        attrs: AttrVariant?,
        containerDefault: Default,
        private: Ident
    ): AttrField {
        val serName = Attr<Name>(cx, RENAME)
        val deName = Attr<Name>(cx, RENAME)
        val deAliases = VecAttr.none<Name>(cx, RENAME)
        val skipSerializing = BoolAttr.none(cx, SKIP_SERIALIZING)
        val skipDeserializing = BoolAttr.none(cx, SKIP_DESERIALIZING)
        val skipSerializingIf = Attr<io.github.kotlinmania.syn.Expr.Path>(cx, SKIP_SERIALIZING_IF)
        val default = Attr<Default>(cx, DEFAULT)
        val serializeWith = Attr<io.github.kotlinmania.syn.Expr.Path>(cx, SERIALIZE_WITH)
        val deserializeWith = Attr<io.github.kotlinmania.syn.Expr.Path>(cx, DESERIALIZE_WITH)
        val serBound = Attr<List<WherePredicate>>(cx, BOUND)
        val deBound = Attr<List<WherePredicate>>(cx, BOUND)
        val borrowedLifetimes = Attr<Set<Lifetime>>(cx, BORROW)
        val getter = Attr<io.github.kotlinmania.syn.Expr.Path>(cx, GETTER)
        val flatten = BoolAttr.none(cx, FLATTEN)

        val identName = field.ident?.let { Name.from(unraw(it)) } ?: Name(index.toString(), Span.callSite())

        val borrowAttribute = attrs?.borrow()
        if (borrowAttribute != null) {
            val borrowableResult = borrowableLifetimes(cx, identName.value, field)
            if (borrowableResult != null) {
                if (borrowAttribute.lifetimes != null) {
                    for (lifetime in borrowAttribute.lifetimes) {
                        if (!borrowableResult.contains(lifetime)) {
                            val msg = "field ``#`identName` does not have lifetime `#`lifetime"
                            cx.errorSpannedBy(field, msg)
                        }
                    }
                    borrowedLifetimes.set(borrowAttribute.path, borrowAttribute.lifetimes)
                } else {
                    borrowedLifetimes.set(borrowAttribute.path, borrowableResult)
                }
            }
        }

        for (attr in field.attrs) {
            if (SERDE != attr.path()) {
                continue
            }

            val metaVal = attr.meta
            if (metaVal is Meta.List && metaVal.tokens.isEmpty()) {
                continue
            }

            try {
                attr.parseNestedMeta { meta ->
                    if (RENAME == meta.path) {
                        val (ser, de) = getMultipleRenames(cx, meta)
                        serName.setOpt(meta.path, ser?.let { Name.from(it) })
                        for (deValue in de) {
                            deName.setIfNone(Name.from(deValue))
                            deAliases.insert(meta.path, Name.from(deValue))
                        }
                    } else if (ALIAS == meta.path) {
                        val s = getLitStr(cx, ALIAS, meta)
                        if (s != null) {
                            deAliases.insert(meta.path, Name.from(s))
                        }
                    } else if (DEFAULT == meta.path) {
                        if (metaPeekEq(meta)) {
                            val path = parseLitIntoExprPath(cx, DEFAULT, meta)
                            if (path != null) {
                                default.set(meta.path, Default.Path(path))
                            }
                        } else {
                            default.set(meta.path, Default.Plain)
                        }
                    } else if (SKIP_SERIALIZING == meta.path) {
                        skipSerializing.setTrue(meta.path)
                    } else if (SKIP_DESERIALIZING == meta.path) {
                        skipDeserializing.setTrue(meta.path)
                    } else if (SKIP == meta.path) {
                        skipSerializing.setTrue(meta.path)
                        skipDeserializing.setTrue(meta.path)
                    } else if (SKIP_SERIALIZING_IF == meta.path) {
                        val path = parseLitIntoExprPath(cx, SKIP_SERIALIZING_IF, meta)
                        if (path != null) {
                            skipSerializingIf.set(meta.path, path)
                        }
                    } else if (SERIALIZE_WITH == meta.path) {
                        val path = parseLitIntoExprPath(cx, SERIALIZE_WITH, meta)
                        if (path != null) {
                            serializeWith.set(meta.path, path)
                        }
                    } else if (DESERIALIZE_WITH == meta.path) {
                        val path = parseLitIntoExprPath(cx, DESERIALIZE_WITH, meta)
                        if (path != null) {
                            deserializeWith.set(meta.path, path)
                        }
                    } else if (WITH == meta.path) {
                        val path = parseLitIntoExprPath(cx, WITH, meta)
                        if (path != null) {
                            var serPath = path.deepCopy()
                            val serSegs = serPath.path.segments.toList().toMutableList()
                            serSegs.add(PathSegment(Ident.new("serialize", serPath.span())))
                            serPath = serPath.copy(path = Path(serPath.path.leadingColon, pathSegmentListFrom(serSegs)))
                            serializeWith.set(meta.path, serPath)

                            var dePath = path.deepCopy()
                            val deSegs = dePath.path.segments.toList().toMutableList()
                            deSegs.add(PathSegment(Ident.new("deserialize", dePath.span())))
                            dePath = dePath.copy(path = Path(dePath.path.leadingColon, pathSegmentListFrom(deSegs)))
                            deserializeWith.set(meta.path, dePath)
                        }
                    } else if (BOUND == meta.path) {
                        val (ser, de) = getWherePredicates(cx, meta)
                        serBound.setOpt(meta.path, ser)
                        deBound.setOpt(meta.path, de)
                    } else if (BORROW == meta.path) {
                        if (metaPeekEq(meta)) {
                            val lifetimes = parseLitIntoLifetimes(cx, meta)
                            val borrowable = borrowableLifetimes(cx, identName.value, field)
                            if (borrowable != null) {
                                for (lifetime in lifetimes) {
                                    if (!borrowable.contains(lifetime)) {
                                        val msg = "field ``#`identName` does not have lifetime `#`lifetime"
                                        cx.errorSpannedBy(field, msg)
                                    }
                                }
                                borrowedLifetimes.set(meta.path, lifetimes.toSet())
                            }
                        } else {
                            val borrowable = borrowableLifetimes(cx, identName.value, field)
                            if (borrowable != null) {
                                borrowedLifetimes.set(meta.path, borrowable)
                            }
                        }
                    } else if (GETTER == meta.path) {
                        val path = parseLitIntoExprPath(cx, GETTER, meta)
                        if (path != null) {
                            getter.set(meta.path, path)
                        }
                    } else if (FLATTEN == meta.path) {
                        flatten.setTrue(meta.path)
                    } else {
                        val pathStr = meta.path.toTokenStream().toString().replace(" ", "")
                        throw meta.error("unknown serde field attribute `$pathStr`")
                    }
                    SynResult.success(Unit)
                }
            } catch (err: SynError) {
                cx.synError(err)
            }
        }

        if (containerDefault.isNone() && skipDeserializing.get()) {
            default.setIfNone(Default.Plain)
        }

        val resolvedBorrowedLifetimes = borrowedLifetimes.get() ?: emptySet()
        val finalDeserializeWith = deserializeWith.get()

        val borrowableIsStr = isCow(field.ty, ::isStr)
        val borrowableIsSliceU8 = isCow(field.ty, ::isSliceU8)
        
        val actualDeserializeWith = if (finalDeserializeWith == null && resolvedBorrowedLifetimes.isNotEmpty()) {
            if (borrowableIsStr) {
                val span = Span.callSite()
                val segments = mutableListOf(
                    PathSegment(Ident.new("_serde", span)),
                    PathSegment(private.deepCopy()),
                    PathSegment(Ident.new("de", span)),
                    PathSegment(Ident.new("borrow_cow_str", span))
                )
                io.github.kotlinmania.syn.Expr.Path(
                    attrs = emptyList(),
                    qself = null,
                    path = Path(leadingColon = null, segments = pathSegmentListFrom(segments))
                )
            } else if (borrowableIsSliceU8) {
                val span = Span.callSite()
                val segments = mutableListOf(
                    PathSegment(Ident.new("_serde", span)),
                    PathSegment(private.deepCopy()),
                    PathSegment(Ident.new("de", span)),
                    PathSegment(Ident.new("borrow_cow_bytes", span))
                )
                io.github.kotlinmania.syn.Expr.Path(
                    attrs = emptyList(),
                    qself = null,
                    path = Path(leadingColon = null, segments = pathSegmentListFrom(segments))
                )
            } else {
                null
            }
        } else {
            finalDeserializeWith
        }

        var finalBorrowedLifetimes = resolvedBorrowedLifetimes
        if (isImplicitlyBorrowed(field.ty)) {
            val collectedLifetimes = mutableSetOf<Lifetime>()
            collectLifetimes(field.ty, collectedLifetimes)
            finalBorrowedLifetimes = finalBorrowedLifetimes + collectedLifetimes
        }

        return AttrField(
            name = MultiName.fromAttrs(identName, serName, deName, deAliases),
            skipSerializing = skipSerializing.get(),
            skipDeserializing = skipDeserializing.get(),
            skipSerializingIf = skipSerializingIf.get(),
            default = default.get() ?: Default.None,
            serializeWith = serializeWith.get(),
            deserializeWith = actualDeserializeWith,
            serBound = serBound.get(),
            deBound = deBound.get(),
            borrowedLifetimes = finalBorrowedLifetimes,
            getter = getter.get(),
            flatten = flatten.get(),
            transparent = false
        )
    }
}
}

private fun isCow(ty: SynType, elem: (SynType) -> Boolean): Boolean {
    val path = when (val ungrouped = ungroup(ty)) {
        is SynType.Path -> ungrouped.path
        else -> return false
    }
    val seg = path.segments.lastOrNull() ?: return false
    val args = when (val arguments = seg.arguments) {
        is PathArguments.AngleBracketed -> arguments.args
        else -> return false
    }
    return seg.ident.toString() == "Cow" && args.size == 2 &&
            args[0] is GenericArgument.LifetimeArg &&
            args[1] is GenericArgument.SynType && elem((args[1] as GenericArgument.SynType).ty)
}

private fun isOption(ty: SynType, elem: (SynType) -> Boolean): Boolean {
    val path = when (val ungrouped = ungroup(ty)) {
        is SynType.Path -> ungrouped.path
        else -> return false
    }
    val seg = path.segments.lastOrNull() ?: return false
    val args = when (val arguments = seg.arguments) {
        is PathArguments.AngleBracketed -> arguments.args
        else -> return false
    }
    return seg.ident.toString() == "Option" && args.size == 1 &&
            args[0] is GenericArgument.SynType && elem((args[0] as GenericArgument.SynType).ty)
}

private fun isReference(ty: SynType, elem: (SynType) -> Boolean): Boolean {
    return when (val ungrouped = ungroup(ty)) {
        is SynType.Reference -> ungrouped.mutability == null && elem(ungrouped.elem)
        else -> false
    }
}

private fun isStr(ty: SynType): Boolean {
    return isPrimitiveType(ty, "str")
}

private fun isSliceU8(ty: SynType): Boolean {
    return when (val ungrouped = ungroup(ty)) {
        is SynType.Slice -> isPrimitiveType(ungrouped.elem, "u8")
        else -> false
    }
}

private fun isPrimitiveType(ty: SynType, primitive: String): Boolean {
    return when (val ungrouped = ungroup(ty)) {
        is SynType.Path -> ungrouped.qself == null && isPrimitivePath(ungrouped.path, primitive)
        else -> false
    }
}

// Helper functions ported from upstream Rust attr.rs

private fun getLitStr(
    cx: Ctxt,
    attrName: Symbol,
    meta: ParseNestedMeta
): LitStr? {
    return getLitStr2(cx, attrName, attrName, meta)
}

private fun getLitStr2(
    cx: Ctxt,
    attrName: Symbol,
    metaItemName: Symbol,
    meta: ParseNestedMeta
): LitStr? {
    val valueTokens = meta.value()
    val exprResult = io.github.kotlinmania.syn.parse2(io.github.kotlinmania.syn.ExprParse, valueTokens)
    if (exprResult.isFailure) {
        cx.errorSpannedBy(valueTokens, "expected serde $attrName attribute to be a string: `$metaItemName = \"...\"`")
        return null
    }
    val expr = exprResult.getOrThrow()
    var value: Expr = expr
    while (value is Expr.Group) {
        value = value.expr
    }
    if (value is Expr.Lit) {
        val lit = value.lit
        if (lit is Lit.Str) {
            return lit
        }
    }
    cx.errorSpannedBy(valueTokens, "expected serde $attrName attribute to be a string: `$metaItemName = \"...\"`")
    return null
}

private fun parseLitIntoPath(
    cx: Ctxt,
    attrName: Symbol,
    meta: ParseNestedMeta
): Path? {
    val string = getLitStr(cx, attrName, meta) ?: return null
    val result = io.github.kotlinmania.syn.parse2(io.github.kotlinmania.syn.PathParse, string.toTokenStream())
    if (result.isFailure) {
        cx.errorSpannedBy(string, "failed to parse path: ${string.value()}")
        return null
    }
    return result.getOrThrow()
}

private fun parseLitIntoExprPath(
    cx: Ctxt,
    attrName: Symbol,
    meta: ParseNestedMeta
): io.github.kotlinmania.syn.Expr.Path? {
    val string = getLitStr(cx, attrName, meta) ?: return null
    val result = io.github.kotlinmania.syn.parse2(io.github.kotlinmania.syn.ExprParse, string.toTokenStream())
    if (result.isFailure) {
        cx.errorSpannedBy(string, "failed to parse path: ${string.value()}")
        return null
    }
    val expr = result.getOrThrow()
    if (expr is io.github.kotlinmania.syn.Expr.Path) {
        return expr
    }
    cx.errorSpannedBy(string, "expected path expression")
    return null
}

private fun parseLitIntoTy(
    cx: Ctxt,
    attrName: Symbol,
    meta: ParseNestedMeta
): SynType? {
    val string = getLitStr(cx, attrName, meta) ?: return null
    // Parse the string as a type expression. Use parserFromFunction to create a Parse<SynType>.
    val typeParse = io.github.kotlinmania.syn.parserFromFunction { input ->
        io.github.kotlinmania.syn.parseTypeFull(input)
    }
    val typeResult = io.github.kotlinmania.syn.parseStr(typeParse, string.value())
    if (typeResult.isFailure) {
        cx.errorSpannedBy(string, "failed to parse type: $attrName = ${string.value()}")
        return null
    }
    return typeResult.getOrThrow()
}

private fun parseLitIntoWhere(
    cx: Ctxt,
    attrName: Symbol,
    metaItemName: Symbol,
    meta: ParseNestedMeta
): List<WherePredicate> {
    val string = getLitStr2(cx, attrName, metaItemName, meta) ?: return emptyList()
    // Parse as a comma-separated list of WherePredicate using parserFromFunction
    val whereParse = io.github.kotlinmania.syn.parserFromFunction { input ->
        val predicates = mutableListOf<WherePredicate>()
        while (!input.isEmpty()) {
            val predResult = io.github.kotlinmania.syn.parseWherePredicate(input)
            if (predResult.isFailure) return@parserFromFunction io.github.kotlinmania.syn.SynResult.failure(predResult.exceptionOrNull()!!)
            predicates.add(predResult.getOrThrow())
            if (!input.isEmpty()) {
                val commaResult = input.parse(io.github.kotlinmania.syn.CommaParse)
                if (commaResult.isFailure) return@parserFromFunction io.github.kotlinmania.syn.SynResult.failure(commaResult.exceptionOrNull()!!)
            }
        }
        io.github.kotlinmania.syn.SynResult.success(predicates)
    }
    val result = io.github.kotlinmania.syn.parseStr(whereParse, string.value())
    if (result.isFailure) {
        cx.errorSpannedBy(string, result.exceptionOrNull()?.message ?: "failed to parse where predicates")
        return emptyList()
    }
    return result.getOrThrow()
}

private fun getWherePredicates(
    cx: Ctxt,
    meta: ParseNestedMeta
): Pair<List<WherePredicate>?, List<WherePredicate>?> {
    val (ser, de) = getSerAndDe(cx, BOUND, meta) { cx2, attrName, metaItemName, m ->
        parseLitIntoWhere(cx2, attrName, metaItemName, m)
    }
    return Pair(ser?.atMostOne(), de?.atMostOne())
}

private fun getRenames(
    cx: Ctxt,
    attrName: Symbol,
    meta: ParseNestedMeta
): Pair<LitStr?, LitStr?> {
    val (ser, de) = getSerAndDe(cx, attrName, meta) { cx2, _, metaItemName, m ->
        getLitStr2(cx2, attrName, metaItemName, m)
    }
    return Pair(ser?.atMostOne(), de?.atMostOne())
}

private fun getMultipleRenames(
    cx: Ctxt,
    meta: ParseNestedMeta
): Pair<LitStr?, List<LitStr>> {
    val (ser, de) = getSerAndDe(cx, RENAME, meta) { cx2, _, metaItemName, m ->
        getLitStr2(cx2, RENAME, metaItemName, m)
    }
    return Pair(ser?.atMostOne(), de?.get() ?: emptyList())
}

// Generic helper: parse `serialize = "..."` / `deserialize = "..."` sub-attributes
private fun <T> getSerAndDe(
    cx: Ctxt,
    attrName: Symbol,
    meta: ParseNestedMeta,
    f: (Ctxt, Symbol, Symbol, ParseNestedMeta) -> T?
): Pair<VecAttr<T>, VecAttr<T>> {
    val serMeta = VecAttr.none<T>(cx, attrName)
    val deMeta = VecAttr.none<T>(cx, attrName)

    // Check if input starts with `=` (single value for both ser and de)
    val valueTokens = meta.value()
    // Try parsing as `= "..."` (the Eq token followed by a literal)
    val eqParse = io.github.kotlinmania.syn.parserFromFunction { input ->
        val eqParseResult = input.parse(io.github.kotlinmania.syn.EqParse)
        if (eqParseResult.isFailure) {
            io.github.kotlinmania.syn.SynResult.success(false)
        } else {
            io.github.kotlinmania.syn.SynResult.success(true)
        }
    }
    val eqResult = io.github.kotlinmania.syn.parse2(eqParse, valueTokens)

    if (eqResult.isSuccess && eqResult.getOrThrow()) {
        // It's `attr = "..."` — applies to both ser and de
        val both = f(cx, attrName, attrName, meta)
        if (both != null) {
            serMeta.insert(meta.path, both)
            deMeta.insert(meta.path, both)
        }
    } else {
        // Try parsing as `(serialize = "...", deserialize = "...")`
        meta.parseNestedMeta { subMeta ->
            if (subMeta.path == SERIALIZE) {
                val v = f(cx, attrName, SERIALIZE, subMeta)
                if (v != null) {
                    serMeta.insert(subMeta.path, v)
                }
            } else if (subMeta.path == DESERIALIZE) {
                val v = f(cx, attrName, DESERIALIZE, subMeta)
                if (v != null) {
                    deMeta.insert(subMeta.path, v)
                }
            } else {
                return@parseNestedMeta io.github.kotlinmania.syn.SynResult.failure(
                    subMeta.error("malformed $attrName attribute, expected `$attrName(serialize = ..., deserialize = ...)`")
                )
            }
            io.github.kotlinmania.syn.SynResult.success(Unit)
        }
    }

    return Pair(serMeta, deMeta)
}

// Extension to get at most one value from VecAttr
private fun <T> VecAttr<T>.atMostOne(): T? {
    return this.get().lastOrNull()
}

private fun isPrimitivePath(path: Path, primitive: String): Boolean {
    return path.leadingColon == null &&
            path.segments.size == 1 &&
            path.segments[0].ident.toString() == primitive &&
            path.segments[0].arguments is PathArguments.None
}

// Parse a string literal like "'a + 'b + 'c" containing lifetimes separated by `+`
private fun parseLitIntoLifetimes(
    cx: Ctxt,
    meta: ParseNestedMeta
): Set<Lifetime> {
    val string = getLitStr(cx, BORROW, meta) ?: return emptySet()
    val lifetimesParse = io.github.kotlinmania.syn.parserFromFunction { input ->
        val set = mutableSetOf<Lifetime>()
        while (!input.isEmpty()) {
            val ltResult = io.github.kotlinmania.syn.LifetimeParse.parse(input)
            if (ltResult.isFailure) {
                return@parserFromFunction io.github.kotlinmania.syn.SynResult.failure(ltResult.exceptionOrNull()!!)
            }
            val lt = ltResult.getOrThrow()
            if (!set.add(lt)) {
                cx.errorSpannedBy(string, "duplicate borrowed lifetime `$lt`")
            }
            if (input.isEmpty()) break
            val plusResult = input.parse(io.github.kotlinmania.syn.PlusParse)
            if (plusResult.isFailure) {
                return@parserFromFunction io.github.kotlinmania.syn.SynResult.failure(plusResult.exceptionOrNull()!!)
            }
        }
        if (set.isEmpty()) {
            cx.errorSpannedBy(string, "at least one lifetime must be borrowed")
        }
        io.github.kotlinmania.syn.SynResult.success(set)
    }
    val result = io.github.kotlinmania.syn.parseStr(lifetimesParse, string.value())
    if (result.isFailure) {
        cx.errorSpannedBy(string, "failed to parse borrowed lifetimes: ${string.value()}")
        return emptySet()
    }
    return result.getOrThrow()
}

private fun borrowableLifetimes(
    cx: Ctxt,
    name: String,
    field: io.github.kotlinmania.syn.Field
): Set<Lifetime>? {
    val lifetimes = mutableSetOf<Lifetime>()
    collectLifetimes(field.ty, lifetimes)
    return if (lifetimes.isEmpty()) {
        cx.errorSpannedBy(field, "field `$name` has no lifetimes to borrow")
        null
    } else {
        lifetimes
    }
}

private fun collectLifetimes(ty: SynType, out: MutableSet<Lifetime>) {
    when (ty) {
        is SynType.Slice -> collectLifetimes(ty.elem, out)
        is SynType.Array -> collectLifetimes(ty.elem, out)
        is SynType.Ptr -> collectLifetimes(ty.elem, out)
        is SynType.Reference -> {
            ty.lifetime?.let { out.add(it) }
            collectLifetimes(ty.elem, out)
        }
        is SynType.Tuple -> {
            for (elem in ty.elems) {
                collectLifetimes(elem, out)
            }
        }
        is SynType.Path -> {
            ty.qself?.let { collectLifetimes(it.ty, out) }
            for (seg in ty.path.segments) {
                val args = seg.arguments
                if (args is PathArguments.AngleBracketed) {
                    for (arg in args.args) {
                        when (arg) {
                            is GenericArgument.LifetimeArg -> out.add(arg.lifetime)
                            is GenericArgument.TypeArg -> collectLifetimes(arg.type, out)
                            is GenericArgument.AssocTypeArg -> collectLifetimes(arg.assoc.ty, out)
                            else -> {}
                        }
                    }
                }
            }
        }
        is SynType.Paren -> collectLifetimes(ty.elem, out)
        is SynType.Group -> collectLifetimes(ty.elem, out)
        is SynType.Macro -> collectLifetimesFromTokens(ty.mac.tokens, out)
        else -> {}
    }
}

private fun collectLifetimesFromTokens(tokens: TokenStream, out: MutableSet<Lifetime>) {
    val iterator = tokens.iterator()
    while (iterator.hasNext()) {
        val tt = iterator.next()
        if (tt is io.github.kotlinmania.procmacro2.TokenTree.Punct && tt.value.asChar() == '\'' && tt.value.spacing() == io.github.kotlinmania.procmacro2.Spacing.Joint) {
            if (iterator.hasNext()) {
                val nextTt = iterator.next()
                if (nextTt is io.github.kotlinmania.procmacro2.TokenTree.Ident) {
                    out.add(Lifetime(tt.value.span(), nextTt.value))
                }
            }
        } else if (tt is io.github.kotlinmania.procmacro2.TokenTree.Group) {
            collectLifetimesFromTokens(tt.value.stream(), out)
        }
    }
}

private fun ungroup(ty: SynType): SynType {
    var result = ty
    while (result is SynType.Group) {
        result = result.elem
    }
    return result
}

public class BorrowAttribute(
    public val path: Path,
    public val lifetimes: Set<Lifetime>?
)

// Extension functions for BoolAttr
private fun BoolAttr.getWithTokens(): Pair<TokenStream, Unit>? {
    return if (this.get()) {
        Pair(TokenStream.new(), Unit)
    } else {
        null
    }
}

public enum class Identifier {
    No,
    Field,
    Variant;

    public fun isSome(): Boolean = this != No
}

public sealed class TagType {
    public object External : TagType()
    public class Internal(public val tag: String) : TagType()
    public class Adjacent(public val tag: String, public val content: String) : TagType()
    public object None : TagType()
}

public sealed class Default {
    public object None : Default()
    public object Plain : Default()
    public class Path(public val path: io.github.kotlinmania.syn.Expr.Path) : Default()
}

