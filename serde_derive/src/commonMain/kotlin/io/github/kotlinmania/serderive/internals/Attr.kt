package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.*

internal class Attr<T>(
    private val cx: Ctxt,
    private val name: Symbol,
    private var tokens: TokenStream = TokenStream.new(),
    private var value: T? = null
) {
    fun set(obj: ToTokens, value: T) {
        val tokens = obj.toTokenStream()

        if (this.value != null) {
            val msg = "duplicate serde attribute `#`name"
            this.cx.errorSpannedBy(tokens, msg)
        } else {
            this.tokens = tokens
            this.value = value
        }
    }

    fun setOpt(obj: ToTokens, value: T?) {
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
    fun setTrue(obj: ToTokens) {
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
    private var firstDupTokens: TokenStream = TokenStream(),
    private val values: MutableList<T> = mutableListOf()
) {
    fun insert(obj: ToTokens, value: T) {
        if (values.size == 1) {
            this.firstDupTokens = obj.toTokenStream()
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
    return Ident(ident.toString().removePrefix("r#"), ident.span())
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
                    if (meta is syn.Meta.Path && NON_EXHAUSTIVE == meta.path) {
                        nonExhaustive = true
                    }
                    continue
                }

                val metaVal = attr.meta
                if (metaVal is syn.Meta.List && metaVal.tokens.isEmpty()) {
                    continue
                }

                try {
                    attr.parseNestedMeta { meta ->
                        if (RENAME == meta.path) {
                            val (ser, de) = getRenames(cx, RENAME, meta)
                            serName.setOpt(meta.path, ser?.let { Name.from(it) })
                            deName.setOpt(meta.path, de?.let { Name.from(it) })
                        } else if (RENAME_ALL == meta.path) {
                            val oneName = meta.input.peek(token.Eq::class)
                            val (ser, de) = getRenames(cx, RENAME_ALL, meta)
                            if (ser != null) {
                                try {
                                    renameAllSerRule.set(meta.path, RenameRule.fromStr(ser.value))
                                } catch (e: ParseError) {
                                    cx.errorSpannedBy(ser, e.message ?: "")
                                }
                            }
                            if (de != null) {
                                try {
                                    renameAllDeRule.set(meta.path, RenameRule.fromStr(de.value))
                                } catch (e: ParseError) {
                                    if (!oneName) cx.errorSpannedBy(de, e.message ?: "")
                                }
                            }
                        } else if (RENAME_ALL_FIELDS == meta.path) {
                            val oneName = meta.input.peek(token.Eq::class)
                            val (ser, de) = getRenames(cx, RENAME_ALL_FIELDS, meta)
                            when (item.data) {
                                is syn.Data.Enum -> {
                                    if (ser != null) {
                                        try {
                                            renameAllFieldsSerRule.set(meta.path, RenameRule.fromStr(ser.value))
                                        } catch (e: ParseError) {
                                            cx.errorSpannedBy(ser, e.message ?: "")
                                        }
                                    }
                                    if (de != null) {
                                        try {
                                            renameAllFieldsDeRule.set(meta.path, RenameRule.fromStr(de.value))
                                        } catch (e: ParseError) {
                                            if (!oneName) cx.errorSpannedBy(de, e.message ?: "")
                                        }
                                    }
                                }
                                is syn.Data.Struct -> cx.synError(meta.error("#[serde(rename_all_fields)] can only be used on enums"))
                                is syn.Data.Union -> cx.synError(meta.error("#[serde(rename_all_fields)] can only be used on enums"))
                            }
                        } else if (TRANSPARENT == meta.path) {
                            transparent.setTrue(meta.path)
                        } else if (DENY_UNKNOWN_FIELDS == meta.path) {
                            denyUnknownFields.setTrue(meta.path)
                        } else if (DEFAULT == meta.path) {
                            if (meta.input.peek(token.Eq::class)) {
                                val path = parseLitIntoExprPath(cx, DEFAULT, meta)
                                if (path != null) {
                                    val itemData = item.data
                                    if (itemData is syn.Data.Struct) {
                                        if (itemData.fields is syn.Fields.Named || itemData.fields is syn.Fields.Unnamed) {
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
                                if (itemData is syn.Data.Struct) {
                                    if (itemData.fields is syn.Fields.Named || itemData.fields is syn.Fields.Unnamed) {
                                        default.set(meta.path, Default.Default)
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
                            if (item.data is syn.Data.Enum) {
                                untagged.setTrue(meta.path)
                            } else {
                                cx.synError(meta.error("#[serde(untagged)] can only be used on enums"))
                            }
                        } else if (TAG == meta.path) {
                            val s = getLitStr(cx, TAG, meta)
                            if (s != null) {
                                val itemData = item.data
                                if (itemData is syn.Data.Enum) {
                                    internalTag.set(meta.path, s.value)
                                } else if (itemData is syn.Data.Struct && itemData.fields is syn.Fields.Named) {
                                    internalTag.set(meta.path, s.value)
                                } else {
                                    cx.synError(meta.error("#[serde(tag = \"...\")] can only be used on enums and structs with named fields"))
                                }
                            }
                        } else if (CONTENT == meta.path) {
                            val s = getLitStr(cx, CONTENT, meta)
                            if (s != null) {
                                if (item.data is syn.Data.Enum) {
                                    content.set(meta.path, s.value)
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
                                if (isPrimitivePath(path, "Self")) {
                                    remote.set(meta.path, parseQuote(item.ident.toString()) as Path)
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
                                expecting.set(meta.path, s.value)
                            }
                        } else {
                            val pathStr = meta.path.toTokenStream().toString().replace(" ", "")
                            throw meta.error("unknown serde container attribute `$pathStr`")
                        }
                    }
                } catch (err: syn.Error) {
                    cx.synError(err)
                }
            }

            var isPacked = false
            for (attr in item.attrs) {
                if (REPR == attr.path()) {
                    try {
                        attr.parseArgsWith { input ->
                            while (true) {
                                val token = input.parse<TokenTree>() ?: break
                                if (token is TokenTree.Ident) {
                                    isPacked = isPacked || token.ident.toString() == "packed"
                                }
                            }
                        }
                    } catch (e: Exception) {}
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
        if (itemData is syn.Data.Enum) {
            for (variant in itemData.variants) {
                if (variant.fields is syn.Fields.Unnamed) {
                    if (variant.fields.unnamed.size != 1) {
                        cx.errorSpannedBy(variant, "#[serde(tag = \"...\")] cannot be used with tuple variants")
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
    if (item.data is syn.Data.Enum && fieldTokens != null && variantTokens == null) {
        return Identifier.Field
    }
    if (item.data is syn.Data.Enum && fieldTokens == null && variantTokens != null) {
        return Identifier.Variant
    }
    if (item.data is syn.Data.Struct && fieldTokens != null && variantTokens == null) {
        val msg = "#[serde(field_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as syn.Data.Struct).structToken, msg)
        return Identifier.No
    }
    if (item.data is syn.Data.Union && fieldTokens != null && variantTokens == null) {
        val msg = "#[serde(field_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as syn.Data.Union).unionToken, msg)
        return Identifier.No
    }
    if (item.data is syn.Data.Struct && fieldTokens == null && variantTokens != null) {
        val msg = "#[serde(variant_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as syn.Data.Struct).structToken, msg)
        return Identifier.No
    }
    if (item.data is syn.Data.Union && fieldTokens == null && variantTokens != null) {
        val msg = "#[serde(variant_identifier)] can only be used on an enum"
        cx.errorSpannedBy((item.data as syn.Data.Union).unionToken, msg)
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
    private val serializeWith: Expr.Path?,
    private val deserializeWith: Expr.Path?,
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
    public fun serializeWith(): Expr.Path? = serializeWith
    public fun deserializeWith(): Expr.Path? = deserializeWith
    public fun untagged(): Boolean = untagged

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
        public fun fromAst(cx: Ctxt, variant: syn.Variant): AttrVariant {
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
            val serializeWith = Attr<Expr.Path>(cx, SERIALIZE_WITH)
            val deserializeWith = Attr<Expr.Path>(cx, DESERIALIZE_WITH)
            val borrow = Attr<BorrowAttribute>(cx, BORROW)
            val untagged = BoolAttr.none(cx, UNTAGGED)

            for (attr in variant.attrs) {
                if (SERDE != attr.path()) {
                    continue
                }

                val metaVal = attr.meta
                if (metaVal is syn.Meta.List && metaVal.tokens.isEmpty()) {
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
                            val oneName = meta.input.peek(token.Eq::class)
                            val (ser, de) = getRenames(cx, RENAME_ALL, meta)
                            if (ser != null) {
                                try {
                                    renameAllSerRule.set(meta.path, RenameRule.fromStr(ser.value))
                                } catch (e: ParseError) {
                                    cx.errorSpannedBy(ser, e.message ?: "")
                                }
                            }
                            if (de != null) {
                                try {
                                    renameAllDeRule.set(meta.path, RenameRule.fromStr(de.value))
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
                                var serPath = path.clone()
                                val serSegs = serPath.path.segments.toMutableList()
                                serSegs.add(PathSegment(Ident("serialize", serPath.span())))
                                serPath = serPath.copy(path = serPath.path.copy(segments = punctuated.Punctuated.fromList(serSegs)))
                                serializeWith.set(meta.path, serPath)

                                var dePath = path.clone()
                                val deSegs = dePath.path.segments.toMutableList()
                                deSegs.add(PathSegment(Ident("deserialize", dePath.span())))
                                dePath = dePath.copy(path = dePath.path.copy(segments = punctuated.Punctuated.fromList(deSegs)))
                                deserializeWith.set(meta.path, dePath)
                            }
                        } else if (SERIALIZE_WITH == meta.path) {
                            val path = parseLitIntoExprPath(cx, SERIALIZE_WITH, meta)
                            serializeWith.setOpt(meta.path, path)
                        } else if (DESERIALIZE_WITH == meta.path) {
                            val path = parseLitIntoExprPath(cx, DESERIALIZE_WITH, meta)
                            deserializeWith.setOpt(meta.path, path)
                        } else if (BORROW == meta.path) {
                            val borrowAttribute = if (meta.input.peek(token.Eq::class)) {
                                val lifetimes = parseLitIntoLifetimes(cx, meta)
                                BorrowAttribute(meta.path.clone(), lifetimes.toSet())
                            } else {
                                BorrowAttribute(meta.path.clone(), null)
                            }
                            if (variant.fields is syn.Fields.Unnamed && variant.fields.unnamed.size == 1) {
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
                    }
                } catch (err: syn.Error) {
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
    private val skipSerializingIf: Expr.Path?,
    private val default: Default,
    private val serializeWith: Expr.Path?,
    private val deserializeWith: Expr.Path?,
    private val serBound: List<WherePredicate>?,
    private val deBound: List<WherePredicate>?,
    private val borrowedLifetimes: Set<Lifetime>,
    private val getter: Expr.Path?,
    private val flatten: Boolean,
    private var transparent: Boolean
) {
    public fun name(): MultiName = name
    public fun skipSerializing(): Boolean = skipSerializing
    public fun skipDeserializing(): Boolean = skipDeserializing
    public fun skipSerializingIf(): Expr.Path? = skipSerializingIf
    public fun default(): Default = default
    public fun serializeWith(): Expr.Path? = serializeWith
    public fun deserializeWith(): Expr.Path? = deserializeWith
    public fun serBound(): List<WherePredicate>? = serBound
    public fun deBound(): List<WherePredicate>? = deBound
    public fun borrowedLifetimes(): Set<Lifetime> = borrowedLifetimes
    public fun getter(): Expr.Path? = getter
    public fun flatten(): Boolean = flatten
    public fun transparent(): Boolean = transparent

    public fun markTransparent() {
        transparent = true
    }

    public fun isNone(): Boolean {
        return false // None is not used here properly, just return false
    }
}

public companion object {
    public fun fromAst(
        cx: Ctxt,
        index: Int,
        field: syn.Field,
        attrs: AttrVariant?,
        containerDefault: Default,
        private: Ident
    ): AttrField {
        val serName = Attr<Name>(cx, RENAME)
        val deName = Attr<Name>(cx, RENAME)
        val deAliases = VecAttr.none<Name>(cx, RENAME)
        val skipSerializing = BoolAttr.none(cx, SKIP_SERIALIZING)
        val skipDeserializing = BoolAttr.none(cx, SKIP_DESERIALIZING)
        val skipSerializingIf = Attr<Expr.Path>(cx, SKIP_SERIALIZING_IF)
        val default = Attr<Default>(cx, DEFAULT)
        val serializeWith = Attr<Expr.Path>(cx, SERIALIZE_WITH)
        val deserializeWith = Attr<Expr.Path>(cx, DESERIALIZE_WITH)
        val serBound = Attr<List<WherePredicate>>(cx, BOUND)
        val deBound = Attr<List<WherePredicate>>(cx, BOUND)
        val borrowedLifetimes = Attr<Set<Lifetime>>(cx, BORROW)
        val getter = Attr<Expr.Path>(cx, GETTER)
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
            if (metaVal is syn.Meta.List && metaVal.tokens.isEmpty()) {
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
                        if (meta.input.peek(token.Eq::class)) {
                            val path = parseLitIntoExprPath(cx, DEFAULT, meta)
                            if (path != null) {
                                default.set(meta.path, Default.Path(path))
                            }
                        } else {
                            default.set(meta.path, Default.Default)
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
                            var serPath = path.clone()
                            val serSegs = serPath.path.segments.toMutableList()
                            serSegs.add(PathSegment(Ident("serialize", serPath.span())))
                            serPath = serPath.copy(path = serPath.path.copy(segments = punctuated.Punctuated.fromList(serSegs)))
                            serializeWith.set(meta.path, serPath)

                            var dePath = path.clone()
                            val deSegs = dePath.path.segments.toMutableList()
                            deSegs.add(PathSegment(Ident("deserialize", dePath.span())))
                            dePath = dePath.copy(path = dePath.path.copy(segments = punctuated.Punctuated.fromList(deSegs)))
                            deserializeWith.set(meta.path, dePath)
                        }
                    } else if (BOUND == meta.path) {
                        val (ser, de) = getWherePredicates(cx, meta)
                        serBound.setOpt(meta.path, ser)
                        deBound.setOpt(meta.path, de)
                    } else if (BORROW == meta.path) {
                        if (meta.input.peek(token.Eq::class)) {
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
                }
            } catch (err: syn.Error) {
                cx.synError(err)
            }
        }

        if (containerDefault.isNone() && skipDeserializing.get()) {
            default.setIfNone(Default.Default)
        }

        val resolvedBorrowedLifetimes = borrowedLifetimes.get() ?: emptySet()
        val finalDeserializeWith = deserializeWith.get()

        val borrowableIsStr = isCow(field.ty, ::isStr)
        val borrowableIsSliceU8 = isCow(field.ty, ::isSliceU8)
        
        val actualDeserializeWith = if (finalDeserializeWith == null && resolvedBorrowedLifetimes.isNotEmpty()) {
            if (borrowableIsStr) {
                val span = Span.callSite()
                val segments = mutableListOf(
                    PathSegment(Ident("_serde", span)),
                    PathSegment(private.clone()),
                    PathSegment(Ident("de", span)),
                    PathSegment(Ident("borrow_cow_str", span))
                )
                Expr.Path(
                    attrs = emptyList(),
                    qself = null,
                    path = Path(leadingColon = null, segments = punctuated.Punctuated.fromList(segments))
                )
            } else if (borrowableIsSliceU8) {
                val span = Span.callSite()
                val segments = mutableListOf(
                    PathSegment(Ident("_serde", span)),
                    PathSegment(private.clone()),
                    PathSegment(Ident("de", span)),
                    PathSegment(Ident("borrow_cow_bytes", span))
                )
                Expr.Path(
                    attrs = emptyList(),
                    qself = null,
                    path = Path(leadingColon = null, segments = punctuated.Punctuated.fromList(segments))
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
            args[0] is GenericArgument.Lifetime &&
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

private fun isPrimitivePath(path: Path, primitive: String): Boolean {
    return path.leadingColon == null &&
            path.segments.size == 1 &&
            path.segments[0].ident.toString() == primitive &&
            path.segments[0].arguments is PathArguments.None
}

private fun borrowableLifetimes(
    cx: Ctxt,
    name: String,
    field: syn.Field
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

internal class BorrowAttribute(
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
    public object Plain : Default() // Renamed Default to Plain to avoid cycle
    public class Path(public val path: Expr.Path) : Default()
}

