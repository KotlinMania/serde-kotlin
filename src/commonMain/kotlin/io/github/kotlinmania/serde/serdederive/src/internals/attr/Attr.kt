// port-lint: source serde_derive/src/internals/attr.rs
package io.github.kotlinmania.serde.serdederive.src.internals.attr

import io.github.kotlinmania.procmacro2.*
import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdederive.src.internals.*
import io.github.kotlinmania.syn.*
import io.github.kotlinmania.syn.token.PathSep
import io.github.kotlinmania.syn.Data as SynData
import io.github.kotlinmania.syn.Field as SynField
import io.github.kotlinmania.syn.Fields as SynFields
import io.github.kotlinmania.syn.Variant as SynVariant

/**
 * This module handles parsing of serde attributes. The entrypoints are
 * [Container.fromAst], [Variant.fromAst], and [Field.fromAst]. Each returns an
 * instance of the corresponding class. Unrecognized, malformed, or duplicated
 * attributes result in a span error but otherwise are ignored. The user will see
 * errors simultaneously for all bad attributes in the crate rather than just the
 * first.
 */

class Attr<T> private constructor(
    private val cx: Ctxt,
    private val name: Symbol,
    private var tokens: TokenStream,
    private var value: T?,
) {
    companion object {
        fun <T> none(
            cx: Ctxt,
            name: Symbol,
        ): Attr<T> = Attr(cx, name, TokenStream.new(), null)
    }

    fun set(
        obj: Any,
        value: T,
    ) {
        val newTokens = tokensFrom(obj)
        if (this.value != null) {
            cx.errorSpannedBy(newTokens, "duplicate serde attribute `$name`")
        } else {
            tokens = newTokens
            this.value = value
        }
    }

    fun setOpt(
        obj: Any,
        value: T?,
    ) {
        if (value != null) {
            set(obj, value)
        }
    }

    fun setIfNone(value: T) {
        if (this.value == null) {
            this.value = value
        }
    }

    fun get(): T? = value

    internal fun getWithTokens(): Pair<TokenStream, T>? = value?.let { tokens to it }
}

private class BoolAttr private constructor(
    val attr: Attr<Unit>,
) {
    companion object {
        fun none(
            cx: Ctxt,
            name: Symbol,
        ): BoolAttr = BoolAttr(Attr.none(cx, name))
    }

    fun setTrue(obj: Any) {
        attr.set(obj, Unit)
    }

    fun get(): Boolean = attr.get() != null
}

class VecAttr<T> private constructor(
    private val cx: Ctxt,
    private val name: Symbol,
    private var firstDupTokens: TokenStream,
    private val values: MutableList<T>,
) {
    companion object {
        fun <T> none(
            cx: Ctxt,
            name: Symbol,
        ): VecAttr<T> = VecAttr(cx, name, TokenStream.new(), mutableListOf())
    }

    fun insert(
        obj: Any,
        value: T,
    ) {
        if (values.size == 1) {
            firstDupTokens = tokensFrom(obj)
        }
        values += value
    }

    fun atMostOne(): T? {
        if (values.size > 1) {
            cx.errorSpannedBy(firstDupTokens, "duplicate serde attribute `$name`")
            return null
        }
        return values.firstOrNull()
    }

    fun get(): List<T> = values.toList()
}

private fun unraw(ident: Ident): Ident = Ident.new(ident.toString().removePrefix("r#"), ident.span())

data class RenameAllRules(
    val serialize: RenameRule,
    val deserialize: RenameRule,
) {
    /**
     * Returns a new [RenameAllRules] with the individual rules of `this` and
     * `otherRules` joined by [RenameRule.or].
     */
    fun or(otherRules: RenameAllRules): RenameAllRules =
        RenameAllRules(
            serialize = serialize.or(otherRules.serialize),
            deserialize = deserialize.or(otherRules.deserialize),
        )
}

/**
 * Represents struct or enum attribute information.
 */
class Container private constructor(
    private val nameValue: MultiName,
    private val transparentValue: Boolean,
    private val denyUnknownFieldsValue: Boolean,
    private val defaultValue: Default,
    private val renameAllRulesValue: RenameAllRules,
    private val renameAllFieldsRulesValue: RenameAllRules,
    private val serBoundValue: List<WherePredicate>?,
    private val deBoundValue: List<WherePredicate>?,
    private val tagValue: TagType,
    private val typeFromValue: SynType?,
    private val typeTryFromValue: SynType?,
    private val typeIntoValue: SynType?,
    private val remoteValue: Path?,
    private val identifierValue: Identifier,
    private val serdePathValue: Path?,
    private val isPackedValue: Boolean,
    private val expectingValue: String?,
    private val nonExhaustiveValue: Boolean,
) {
    companion object {
        /**
         * Extract out the serde attributes from an item.
         */
        fun fromAst(
            cx: Ctxt,
            item: DeriveInput,
        ): Container {
            val serName = Attr.none<Name>(cx, RENAME)
            val deName = Attr.none<Name>(cx, RENAME)
            val transparent = BoolAttr.none(cx, TRANSPARENT)
            val denyUnknownFields = BoolAttr.none(cx, DENY_UNKNOWN_FIELDS)
            val default = Attr.none<Default>(cx, DEFAULT)
            val renameAllSerRule = Attr.none<RenameRule>(cx, RENAME_ALL)
            val renameAllDeRule = Attr.none<RenameRule>(cx, RENAME_ALL)
            val renameAllFieldsSerRule = Attr.none<RenameRule>(cx, RENAME_ALL_FIELDS)
            val renameAllFieldsDeRule = Attr.none<RenameRule>(cx, RENAME_ALL_FIELDS)
            val serBound = Attr.none<List<WherePredicate>>(cx, BOUND)
            val deBound = Attr.none<List<WherePredicate>>(cx, BOUND)
            val untagged = BoolAttr.none(cx, UNTAGGED)
            val internalTag = Attr.none<String>(cx, TAG)
            val content = Attr.none<String>(cx, CONTENT)
            val typeFrom = Attr.none<SynType>(cx, FROM)
            val typeTryFrom = Attr.none<SynType>(cx, TRY_FROM)
            val typeInto = Attr.none<SynType>(cx, INTO)
            val remote = Attr.none<Path>(cx, REMOTE)
            val fieldIdentifier = BoolAttr.none(cx, FIELD_IDENTIFIER)
            val variantIdentifier = BoolAttr.none(cx, VARIANT_IDENTIFIER)
            val serdePath = Attr.none<Path>(cx, CRATE)
            val expecting = Attr.none<String>(cx, EXPECTING)
            var nonExhaustive = false
            var isPacked = false

            for (attr in item.attrs) {
                if (attr.path().eq(NON_EXHAUSTIVE)) {
                    nonExhaustive = true
                }
                if (attr.path().eq(REPR)) {
                    isPacked = attr.tokenText().contains("packed")
                }
                if (!attr.path().eq(SERDE)) {
                    continue
                }

                for (entry in serdeEntries(attr)) {
                    when {
                        entry.eq(RENAME) -> {
                            val (ser, de) = entry.renames()
                            serName.setOpt(entry.path, ser?.let { Name.from(it) })
                            deName.setOpt(entry.path, de?.let { Name.from(it) })
                        }

                        entry.eq(RENAME_ALL) -> {
                            val oneName = entry.value != null
                            val (ser, de) = entry.renames()
                            ser?.let { setRenameRule(cx, renameAllSerRule, entry.path, it) }
                            de?.let {
                                val result = RenameRule.fromStr(it.value())
                                result.onSuccess { rule -> renameAllDeRule.set(entry.path, rule) }
                                result.onFailure { err ->
                                    if (!oneName) {
                                        cx.errorSpannedBy(it, err)
                                    }
                                }
                            }
                        }

                        entry.eq(RENAME_ALL_FIELDS) -> {
                            val oneName = entry.value != null
                            if (item.data !is SynData.Enum) {
                                cx.synError(entry.error("#[serde(rename_all_fields)] can only be used on enums"))
                            } else {
                                val (ser, de) = entry.renames()
                                ser?.let { setRenameRule(cx, renameAllFieldsSerRule, entry.path, it) }
                                de?.let {
                                    val result = RenameRule.fromStr(it.value())
                                    result.onSuccess { rule -> renameAllFieldsDeRule.set(entry.path, rule) }
                                    result.onFailure { err ->
                                        if (!oneName) {
                                            cx.errorSpannedBy(it, err)
                                        }
                                    }
                                }
                            }
                        }

                        entry.eq(TRANSPARENT) -> transparent.setTrue(entry.path)
                        entry.eq(DENY_UNKNOWN_FIELDS) -> denyUnknownFields.setTrue(entry.path)
                        entry.eq(DEFAULT) -> {
                            if (entry.value != null) {
                                when (val data = item.data) {
                                    is SynData.Struct ->
                                        if (data.fields is SynFields.Unit) {
                                            cx.synError(
                                                entry.error("#[serde(default = \"...\")] can only be used on structs that have fields"),
                                            )
                                        } else {
                                            default.set(entry.path, Default.Path(entry.exprPathValue()))
                                        }

                                    is SynData.Enum,
                                    is SynData.Union,
                                    -> cx.synError(entry.error("#[serde(default = \"...\")] can only be used on structs"))
                                }
                            } else {
                                when (val data = item.data) {
                                    is SynData.Struct ->
                                        if (data.fields is SynFields.Unit) {
                                            cx.errorSpannedBy(data.fields, "#[serde(default)] can only be used on structs that have fields")
                                        } else {
                                            default.set(entry.path, Default.DefaultValue)
                                        }

                                    is SynData.Enum,
                                    is SynData.Union,
                                    -> cx.synError(entry.error("#[serde(default)] can only be used on structs"))
                                }
                            }
                        }

                        entry.eq(BOUND) -> {
                            val (ser, de) = entry.wherePredicates()
                            serBound.setOpt(entry.path, ser)
                            deBound.setOpt(entry.path, de)
                        }

                        entry.eq(UNTAGGED) -> {
                            if (item.data is SynData.Enum) {
                                untagged.setTrue(entry.path)
                            } else {
                                cx.synError(entry.error("#[serde(untagged)] can only be used on enums"))
                            }
                        }

                        entry.eq(TAG) -> {
                            entry.value?.let { tag ->
                                when (val data = item.data) {
                                    is SynData.Enum -> internalTag.set(entry.path, tag)
                                    is SynData.Struct ->
                                        if (data.fields is SynFields.Named) {
                                            internalTag.set(entry.path, tag)
                                        } else {
                                            cx.synError(
                                                entry.error(
                                                    "#[serde(tag = \"...\")] can only be used on enums and structs with named fields",
                                                ),
                                            )
                                        }

                                    is SynData.Union ->
                                        cx.synError(
                                            entry.error("#[serde(tag = \"...\")] can only be used on enums and structs with named fields"),
                                        )
                                }
                            }
                        }

                        entry.eq(CONTENT) -> {
                            entry.value?.let { value ->
                                if (item.data is SynData.Enum) {
                                    content.set(entry.path, value)
                                } else {
                                    cx.synError(entry.error("#[serde(content = \"...\")] can only be used on enums"))
                                }
                            }
                        }

                        entry.eq(FROM) -> entry.value?.let { typeFrom.set(entry.path, parseType(it)) }
                        entry.eq(TRY_FROM) -> entry.value?.let { typeTryFrom.set(entry.path, parseType(it)) }
                        entry.eq(INTO) -> entry.value?.let { typeInto.set(entry.path, parseType(it)) }
                        entry.eq(REMOTE) ->
                            entry.value?.let { value ->
                                val path = parsePath(value)
                                if (isPrimitivePath(path, "Self")) {
                                    remote.set(entry.path, Path.from(item.ident.copy()))
                                } else {
                                    remote.set(entry.path, path)
                                }
                            }

                        entry.eq(FIELD_IDENTIFIER) -> fieldIdentifier.setTrue(entry.path)
                        entry.eq(VARIANT_IDENTIFIER) -> variantIdentifier.setTrue(entry.path)
                        entry.eq(CRATE) -> entry.value?.let { serdePath.set(entry.path, parsePath(it)) }
                        entry.eq(EXPECTING) -> entry.value?.let { expecting.set(entry.path, it) }
                        else -> cx.synError(entry.error("unknown serde container attribute `${entry.pathText()}`"))
                    }
                }
            }

            return Container(
                nameValue = MultiName.fromAttrs(Name.from(unraw(item.ident)), serName, deName, null),
                transparentValue = transparent.get(),
                denyUnknownFieldsValue = denyUnknownFields.get(),
                defaultValue = default.get() ?: Default.None,
                renameAllRulesValue =
                    RenameAllRules(
                        serialize = renameAllSerRule.get() ?: RenameRule.None,
                        deserialize = renameAllDeRule.get() ?: RenameRule.None,
                    ),
                renameAllFieldsRulesValue =
                    RenameAllRules(
                        serialize = renameAllFieldsSerRule.get() ?: RenameRule.None,
                        deserialize = renameAllFieldsDeRule.get() ?: RenameRule.None,
                    ),
                serBoundValue = serBound.get(),
                deBoundValue = deBound.get(),
                tagValue = decideTag(cx, item, untagged, internalTag, content),
                typeFromValue = typeFrom.get(),
                typeTryFromValue = typeTryFrom.get(),
                typeIntoValue = typeInto.get(),
                remoteValue = remote.get(),
                identifierValue = decideIdentifier(cx, item, fieldIdentifier, variantIdentifier),
                serdePathValue = serdePath.get(),
                isPackedValue = isPacked,
                expectingValue = expecting.get(),
                nonExhaustiveValue = nonExhaustive,
            )
        }
    }

    fun name(): MultiName = nameValue

    fun renameAllRules(): RenameAllRules = renameAllRulesValue

    fun renameAllFieldsRules(): RenameAllRules = renameAllFieldsRulesValue

    fun transparent(): Boolean = transparentValue

    fun denyUnknownFields(): Boolean = denyUnknownFieldsValue

    fun default(): Default = defaultValue

    fun serBound(): List<WherePredicate>? = serBoundValue

    fun deBound(): List<WherePredicate>? = deBoundValue

    fun tag(): TagType = tagValue

    fun typeFrom(): SynType? = typeFromValue

    fun typeTryFrom(): SynType? = typeTryFromValue

    fun typeInto(): SynType? = typeIntoValue

    fun remote(): Path? = remoteValue?.deepCopy()

    fun isPacked(): Boolean = isPackedValue

    fun identifier(): Identifier = identifierValue

    fun customSerdePath(): Path? = serdePathValue?.deepCopy()

    fun expecting(): String? = expectingValue

    fun nonExhaustive(): Boolean = nonExhaustiveValue
}

/**
 * Styles of representing an enum.
 */
sealed class TagType {
    /**
     * The default.
     */
    data object External : TagType()

    /**
     * `serde(tag = "type")`.
     */
    data class Internal(
        val tag: String,
    ) : TagType()

    /**
     * `serde(tag = "t", content = "c")`.
     */
    data class Adjacent(
        val tag: String,
        val content: String,
    ) : TagType()

    /**
     * `serde(untagged)`.
     */
    data object None : TagType()
}

/**
 * Whether this enum represents the fields of a struct or the variants of an enum.
 */
enum class Identifier {
    /**
     * It does not.
     */
    No,

    /**
     * This enum represents the fields of a struct. All of the variants must be
     * unit variants, except possibly one which is annotated with `serde(other)`
     * and is a newtype variant.
     */
    Field,

    /**
     * This enum represents the variants of an enum. All of the variants must be
     * unit variants.
     */
    Variant,
    ;

    fun isSome(): Boolean =
        when (this) {
            No -> false
            Field,
            Variant,
            -> true
        }
}

private fun decideTag(
    cx: Ctxt,
    item: DeriveInput,
    untagged: BoolAttr,
    internalTag: Attr<String>,
    content: Attr<String>,
): TagType {
    val untaggedTokens = untagged.attr.getWithTokens()
    val internalTagValue = internalTag.getWithTokens()
    val contentValue = content.getWithTokens()
    return when {
        untaggedTokens == null && internalTagValue == null && contentValue == null -> TagType.External
        untaggedTokens != null && internalTagValue == null && contentValue == null -> TagType.None
        untaggedTokens == null && internalTagValue != null && contentValue == null -> {
            val tag = internalTagValue.second
            val data = item.data
            if (data is SynData.Enum) {
                for (variant in data.variants) {
                    val fields = variant.fields
                    if (fields is SynFields.Unnamed && fields.fields.unnamed.len() != 1) {
                        cx.errorSpannedBy(variant, "#[serde(tag = \"...\")] cannot be used with tuple variants")
                        break
                    }
                }
            }
            TagType.Internal(tag)
        }

        untaggedTokens != null && internalTagValue != null && contentValue == null -> {
            val msg = "enum cannot be both untagged and internally tagged"
            cx.errorSpannedBy(untaggedTokens.first, msg)
            cx.errorSpannedBy(internalTagValue.first, msg)
            TagType.External
        }

        untaggedTokens == null && internalTagValue == null && contentValue != null -> {
            cx.errorSpannedBy(contentValue.first, "#[serde(tag = \"...\", content = \"...\")] must be used together")
            TagType.External
        }

        untaggedTokens != null && internalTagValue == null && contentValue != null -> {
            val msg = "untagged enum cannot have #[serde(content = \"...\")]"
            cx.errorSpannedBy(untaggedTokens.first, msg)
            cx.errorSpannedBy(contentValue.first, msg)
            TagType.External
        }

        untaggedTokens == null && internalTagValue != null && contentValue != null ->
            TagType.Adjacent(internalTagValue.second, contentValue.second)

        else -> {
            val msg = "untagged enum cannot have #[serde(tag = \"...\", content = \"...\")]"
            cx.errorSpannedBy(untaggedTokens!!.first, msg)
            cx.errorSpannedBy(internalTagValue!!.first, msg)
            cx.errorSpannedBy(contentValue!!.first, msg)
            TagType.External
        }
    }
}

private fun decideIdentifier(
    cx: Ctxt,
    item: DeriveInput,
    fieldIdentifier: BoolAttr,
    variantIdentifier: BoolAttr,
): Identifier {
    val fieldTokens = fieldIdentifier.attr.getWithTokens()
    val variantTokens = variantIdentifier.attr.getWithTokens()
    return when {
        fieldTokens == null && variantTokens == null -> Identifier.No
        fieldTokens != null && variantTokens != null -> {
            val msg = "#[serde(field_identifier)] and #[serde(variant_identifier)] cannot both be set"
            cx.errorSpannedBy(fieldTokens.first, msg)
            cx.errorSpannedBy(variantTokens.first, msg)
            Identifier.No
        }

        item.data is SynData.Enum && fieldTokens != null -> Identifier.Field
        item.data is SynData.Enum && variantTokens != null -> Identifier.Variant
        fieldTokens != null -> {
            cx.errorSpannedBy(item, "#[serde(field_identifier)] can only be used on an enum")
            Identifier.No
        }

        else -> {
            cx.errorSpannedBy(item, "#[serde(variant_identifier)] can only be used on an enum")
            Identifier.No
        }
    }
}

/**
 * Represents variant attribute information.
 */
class Variant private constructor(
    private val nameValue: MultiName,
    private val renameAllRulesValue: RenameAllRules,
    private val serBoundValue: List<WherePredicate>?,
    private val deBoundValue: List<WherePredicate>?,
    private val skipDeserializingValue: Boolean,
    private val skipSerializingValue: Boolean,
    private val otherValue: Boolean,
    private val serializeWithValue: Expr.Path?,
    private val deserializeWithValue: Expr.Path?,
    internal val borrow: BorrowAttribute?,
    private val untaggedValue: Boolean,
) {
    companion object {
        fun fromAst(
            cx: Ctxt,
            variant: SynVariant,
        ): Variant {
            val serName = Attr.none<Name>(cx, RENAME)
            val deName = Attr.none<Name>(cx, RENAME)
            val deAliases = VecAttr.none<Name>(cx, RENAME)
            val skipDeserializing = BoolAttr.none(cx, SKIP_DESERIALIZING)
            val skipSerializing = BoolAttr.none(cx, SKIP_SERIALIZING)
            val renameAllSerRule = Attr.none<RenameRule>(cx, RENAME_ALL)
            val renameAllDeRule = Attr.none<RenameRule>(cx, RENAME_ALL)
            val serBound = Attr.none<List<WherePredicate>>(cx, BOUND)
            val deBound = Attr.none<List<WherePredicate>>(cx, BOUND)
            val other = BoolAttr.none(cx, OTHER)
            val serializeWith = Attr.none<Expr.Path>(cx, SERIALIZE_WITH)
            val deserializeWith = Attr.none<Expr.Path>(cx, DESERIALIZE_WITH)
            val borrow = Attr.none<BorrowAttribute>(cx, BORROW)
            val untagged = BoolAttr.none(cx, UNTAGGED)

            for (attr in variant.attrs) {
                if (!attr.path().eq(SERDE)) {
                    continue
                }
                for (entry in serdeEntries(attr)) {
                    when {
                        entry.eq(RENAME) -> {
                            val (ser, de) = entry.multipleRenames()
                            serName.setOpt(entry.path, ser?.let { Name.from(it) })
                            for (value in de) {
                                val name = Name.from(value)
                                deName.setIfNone(name)
                                deAliases.insert(entry.path, name.copy())
                            }
                        }

                        entry.eq(ALIAS) -> entry.value?.let { deAliases.insert(entry.path, Name(it, entry.path.span())) }
                        entry.eq(RENAME_ALL) -> {
                            val oneName = entry.value != null
                            val (ser, de) = entry.renames()
                            ser?.let { setRenameRule(cx, renameAllSerRule, entry.path, it) }
                            de?.let {
                                val result = RenameRule.fromStr(it.value())
                                result.onSuccess { rule -> renameAllDeRule.set(entry.path, rule) }
                                result.onFailure { err ->
                                    if (!oneName) {
                                        cx.errorSpannedBy(it, err)
                                    }
                                }
                            }
                        }

                        entry.eq(SKIP) -> {
                            skipSerializing.setTrue(entry.path)
                            skipDeserializing.setTrue(entry.path)
                        }

                        entry.eq(SKIP_DESERIALIZING) -> skipDeserializing.setTrue(entry.path)
                        entry.eq(SKIP_SERIALIZING) -> skipSerializing.setTrue(entry.path)
                        entry.eq(OTHER) -> other.setTrue(entry.path)
                        entry.eq(BOUND) -> {
                            val (ser, de) = entry.wherePredicates()
                            serBound.setOpt(entry.path, ser)
                            deBound.setOpt(entry.path, de)
                        }

                        entry.eq(WITH) ->
                            entry.value?.let { value ->
                                val path = parseExprPath(value)
                                serializeWith.set(entry.path, appendPath(path, "serialize"))
                                deserializeWith.set(entry.path, appendPath(path, "deserialize"))
                            }

                        entry.eq(SERIALIZE_WITH) -> entry.value?.let { serializeWith.set(entry.path, parseExprPath(it)) }
                        entry.eq(DESERIALIZE_WITH) -> entry.value?.let { deserializeWith.set(entry.path, parseExprPath(it)) }
                        entry.eq(BORROW) -> {
                            val borrowAttribute =
                                if (entry.value != null) {
                                    BorrowAttribute(entry.path, parseLifetimes(entry.value))
                                } else {
                                    BorrowAttribute(entry.path, null)
                                }
                            val fields = variant.fields
                            if (fields is SynFields.Unnamed && fields.fields.unnamed.len() == 1) {
                                borrow.set(entry.path, borrowAttribute)
                            } else {
                                cx.errorSpannedBy(variant, "#[serde(borrow)] may only be used on newtype variants")
                            }
                        }

                        entry.eq(UNTAGGED) -> untagged.setTrue(entry.path)
                        else -> cx.synError(entry.error("unknown serde variant attribute `${entry.pathText()}`"))
                    }
                }
            }

            return Variant(
                nameValue =
                    MultiName.fromAttrs(
                        Name.from(unraw(variant.ident)),
                        serName,
                        deName,
                        deAliases,
                    ),
                renameAllRulesValue =
                    RenameAllRules(
                        serialize = renameAllSerRule.get() ?: RenameRule.None,
                        deserialize = renameAllDeRule.get() ?: RenameRule.None,
                    ),
                serBoundValue = serBound.get(),
                deBoundValue = deBound.get(),
                skipDeserializingValue = skipDeserializing.get(),
                skipSerializingValue = skipSerializing.get(),
                otherValue = other.get(),
                serializeWithValue = serializeWith.get(),
                deserializeWithValue = deserializeWith.get(),
                borrow = borrow.get(),
                untaggedValue = untagged.get(),
            )
        }
    }

    fun name(): MultiName = nameValue

    fun aliases(): List<Name> = nameValue.deserializeAliases()

    fun renameByRules(rules: RenameAllRules) {
        if (!nameValue.serializeRenamed) {
            nameValue.serialize.value = rules.serialize.applyToVariant(nameValue.serialize.value)
        }
        if (!nameValue.deserializeRenamed) {
            nameValue.deserialize.value = rules.deserialize.applyToVariant(nameValue.deserialize.value)
        }
        nameValue.addDeserializeAlias(nameValue.deserialize.copy())
    }

    fun renameAllRules(): RenameAllRules = renameAllRulesValue

    fun serBound(): List<WherePredicate>? = serBoundValue

    fun deBound(): List<WherePredicate>? = deBoundValue

    fun skipDeserializing(): Boolean = skipDeserializingValue

    fun skipSerializing(): Boolean = skipSerializingValue

    fun other(): Boolean = otherValue

    fun serializeWith(): Expr.Path? =
        serializeWithValue?.copy(
            attrs =
                serializeWithValue.attrs.map {
                    it.deepCopy()
                },
            path = serializeWithValue.path.deepCopy(),
        )

    fun deserializeWith(): Expr.Path? =
        deserializeWithValue?.copy(
            attrs =
                deserializeWithValue.attrs.map {
                    it.deepCopy()
                },
            path = deserializeWithValue.path.deepCopy(),
        )

    fun untagged(): Boolean = untaggedValue
}

internal data class BorrowAttribute(
    val path: Path,
    val lifetimes: Set<Lifetime>?,
)

/**
 * Represents field attribute information.
 */
class Field private constructor(
    private val nameValue: MultiName,
    private val skipSerializingValue: Boolean,
    private val skipDeserializingValue: Boolean,
    private val skipSerializingIfValue: Expr.Path?,
    private val defaultValue: Default,
    private val serializeWithValue: Expr.Path?,
    private val deserializeWithValue: Expr.Path?,
    private val serBoundValue: List<WherePredicate>?,
    private val deBoundValue: List<WherePredicate>?,
    private val borrowedLifetimesValue: Set<Lifetime>,
    private val getterValue: Expr.Path?,
    private val flattenValue: Boolean,
    private var transparentValue: Boolean,
) {
    companion object {
        /**
         * Extract out the serde attributes from a struct field.
         */
        fun fromAst(
            cx: Ctxt,
            index: Int,
            field: SynField,
            attrs: Variant?,
            containerDefault: Default,
            private: Ident,
        ): Field {
            val serName = Attr.none<Name>(cx, RENAME)
            val deName = Attr.none<Name>(cx, RENAME)
            val deAliases = VecAttr.none<Name>(cx, RENAME)
            val skipSerializing = BoolAttr.none(cx, SKIP_SERIALIZING)
            val skipDeserializing = BoolAttr.none(cx, SKIP_DESERIALIZING)
            val skipSerializingIf = Attr.none<Expr.Path>(cx, SKIP_SERIALIZING_IF)
            val default = Attr.none<Default>(cx, DEFAULT)
            val serializeWith = Attr.none<Expr.Path>(cx, SERIALIZE_WITH)
            val deserializeWith = Attr.none<Expr.Path>(cx, DESERIALIZE_WITH)
            val serBound = Attr.none<List<WherePredicate>>(cx, BOUND)
            val deBound = Attr.none<List<WherePredicate>>(cx, BOUND)
            val borrowedLifetimes = Attr.none<Set<Lifetime>>(cx, BORROW)
            val getter = Attr.none<Expr.Path>(cx, GETTER)
            val flatten = BoolAttr.none(cx, FLATTEN)

            val ident =
                field.ident?.let { Name.from(unraw(it)) }
                    ?: Name(index.toString(), Span.callSite())

            val borrowAttribute = attrs?.borrow
            if (borrowAttribute != null) {
                borrowableLifetimes(cx, ident, field).onSuccess { borrowable ->
                    val lifetimes = borrowAttribute.lifetimes
                    if (lifetimes != null) {
                        for (lifetime in lifetimes) {
                            if (lifetime !in borrowable) {
                                cx.errorSpannedBy(field, "field `$ident` does not have lifetime $lifetime")
                            }
                        }
                        borrowedLifetimes.set(borrowAttribute.path, lifetimes)
                    } else {
                        borrowedLifetimes.set(borrowAttribute.path, borrowable)
                    }
                }
            }

            for (attr in field.attrs) {
                if (!attr.path().eq(SERDE)) {
                    continue
                }
                for (entry in serdeEntries(attr)) {
                    when {
                        entry.eq(RENAME) -> {
                            val (ser, de) = entry.multipleRenames()
                            serName.setOpt(entry.path, ser?.let { Name.from(it) })
                            for (value in de) {
                                val name = Name.from(value)
                                deName.setIfNone(name)
                                deAliases.insert(entry.path, name.copy())
                            }
                        }

                        entry.eq(ALIAS) -> entry.value?.let { deAliases.insert(entry.path, Name(it, entry.path.span())) }
                        entry.eq(DEFAULT) ->
                            if (entry.value != null) {
                                default.set(entry.path, Default.Path(entry.exprPathValue()))
                            } else {
                                default.set(entry.path, Default.DefaultValue)
                            }

                        entry.eq(SKIP_SERIALIZING) -> skipSerializing.setTrue(entry.path)
                        entry.eq(SKIP_DESERIALIZING) -> skipDeserializing.setTrue(entry.path)
                        entry.eq(SKIP) -> {
                            skipSerializing.setTrue(entry.path)
                            skipDeserializing.setTrue(entry.path)
                        }

                        entry.eq(SKIP_SERIALIZING_IF) -> entry.value?.let { skipSerializingIf.set(entry.path, parseExprPath(it)) }
                        entry.eq(SERIALIZE_WITH) -> entry.value?.let { serializeWith.set(entry.path, parseExprPath(it)) }
                        entry.eq(DESERIALIZE_WITH) -> entry.value?.let { deserializeWith.set(entry.path, parseExprPath(it)) }
                        entry.eq(WITH) ->
                            entry.value?.let { value ->
                                val path = parseExprPath(value)
                                serializeWith.set(entry.path, appendPath(path, "serialize"))
                                deserializeWith.set(entry.path, appendPath(path, "deserialize"))
                            }

                        entry.eq(BOUND) -> {
                            val (ser, de) = entry.wherePredicates()
                            serBound.setOpt(entry.path, ser)
                            deBound.setOpt(entry.path, de)
                        }

                        entry.eq(BORROW) -> {
                            val lifetimes =
                                if (entry.value != null) {
                                    parseLifetimes(entry.value)
                                } else {
                                    null
                                }
                            borrowableLifetimes(cx, ident, field).onSuccess { borrowable ->
                                if (lifetimes != null) {
                                    for (lifetime in lifetimes) {
                                        if (lifetime !in borrowable) {
                                            cx.errorSpannedBy(field, "field `$ident` does not have lifetime $lifetime")
                                        }
                                    }
                                    borrowedLifetimes.set(entry.path, lifetimes)
                                } else {
                                    borrowedLifetimes.set(entry.path, borrowable)
                                }
                            }
                        }

                        entry.eq(GETTER) -> entry.value?.let { getter.set(entry.path, parseExprPath(it)) }
                        entry.eq(FLATTEN) -> flatten.setTrue(entry.path)
                        else -> cx.synError(entry.error("unknown serde field attribute `${entry.pathText()}`"))
                    }
                }
            }

            if (containerDefault.isNone() && skipDeserializing.attr.get() != null) {
                default.setIfNone(Default.DefaultValue)
            }

            val borrowSet = borrowedLifetimes.get()?.toMutableSet() ?: mutableSetOf()
            if (borrowSet.isNotEmpty()) {
                if (isCow(field.ty, ::isStr)) {
                    deserializeWith.setIfNone(borrowCowPath(private, "borrow_cow_str"))
                } else if (isCow(field.ty, ::isSliceU8)) {
                    deserializeWith.setIfNone(borrowCowPath(private, "borrow_cow_bytes"))
                }
            } else if (isImplicitlyBorrowed(field.ty)) {
                collectLifetimes(field.ty, borrowSet)
            }

            return Field(
                nameValue = MultiName.fromAttrs(ident, serName, deName, deAliases),
                skipSerializingValue = skipSerializing.get(),
                skipDeserializingValue = skipDeserializing.get(),
                skipSerializingIfValue = skipSerializingIf.get(),
                defaultValue = default.get() ?: Default.None,
                serializeWithValue = serializeWith.get(),
                deserializeWithValue = deserializeWith.get(),
                serBoundValue = serBound.get(),
                deBoundValue = deBound.get(),
                borrowedLifetimesValue = borrowSet,
                getterValue = getter.get(),
                flattenValue = flatten.get(),
                transparentValue = false,
            )
        }
    }

    fun name(): MultiName = nameValue

    fun aliases(): List<Name> = nameValue.deserializeAliases()

    fun renameByRules(rules: RenameAllRules) {
        if (!nameValue.serializeRenamed) {
            nameValue.serialize.value = rules.serialize.applyToField(nameValue.serialize.value)
        }
        if (!nameValue.deserializeRenamed) {
            nameValue.deserialize.value = rules.deserialize.applyToField(nameValue.deserialize.value)
        }
        nameValue.addDeserializeAlias(nameValue.deserialize.copy())
    }

    fun skipSerializing(): Boolean = skipSerializingValue

    fun skipDeserializing(): Boolean = skipDeserializingValue

    fun skipSerializingIf(): Expr.Path? = skipSerializingIfValue

    fun default(): Default = defaultValue

    fun serializeWith(): Expr.Path? = serializeWithValue

    fun deserializeWith(): Expr.Path? = deserializeWithValue

    fun serBound(): List<WherePredicate>? = serBoundValue

    fun deBound(): List<WherePredicate>? = deBoundValue

    fun borrowedLifetimes(): Set<Lifetime> = borrowedLifetimesValue

    fun getter(): Expr.Path? = getterValue

    fun flatten(): Boolean = flattenValue

    fun transparent(): Boolean = transparentValue

    fun markTransparent() {
        transparentValue = true
    }
}

/**
 * Represents the default to use for a field when deserializing.
 */
sealed class Default {
    /**
     * Field must always be specified because it does not have a default.
     */
    data object None : Default()

    /**
     * The default is given by `std.default.Default.default`.
     */
    data object DefaultValue : Default()

    /**
     * The default is given by this function.
     */
    data class Path(
        val value: Expr.Path,
    ) : Default()

    fun isNone(): Boolean = this is None
}

private data class SerAndDe<T>(
    val serialize: T?,
    val deserialize: T?,
)

private data class AttributeEntry(
    val path: Path,
    val value: String?,
    val nested: List<AttributeEntry>,
    val tokens: TokenStream,
) {
    fun eq(symbol: Symbol): Boolean = path.eq(symbol)

    fun renames(): Pair<io.github.kotlinmania.syn.LitStr?, io.github.kotlinmania.syn.LitStr?> {
        if (value != null) {
            val literal = litStr(value, path.span())
            return literal to literal.copy()
        }
        var ser: io.github.kotlinmania.syn.LitStr? = null
        var de: io.github.kotlinmania.syn.LitStr? = null
        for (entry in nested) {
            when {
                entry.eq(SERIALIZE) -> ser = entry.value?.let { litStr(it, entry.path.span()) }
                entry.eq(DESERIALIZE) -> de = entry.value?.let { litStr(it, entry.path.span()) }
            }
        }
        return ser to de
    }

    fun multipleRenames(): Pair<io.github.kotlinmania.syn.LitStr?, List<io.github.kotlinmania.syn.LitStr>> {
        val (ser, de) = renames()
        return ser to listOfNotNull(de)
    }

    fun wherePredicates(): SerAndDe<List<WherePredicate>> = SerAndDe(emptyList(), emptyList())

    fun exprPathValue(): Expr.Path = parseExprPath(value.orEmpty())

    fun pathText(): String = path.toString().replace(" ", "")

    fun error(message: String): io.github.kotlinmania.syn.SynError =
        io.github.kotlinmania.syn.SynError
            .new(path.span(), message)
}

private fun serdeEntries(attr: Attribute): List<AttributeEntry> =
    when (val meta = attr.meta) {
        is Meta.List -> entriesFromTokens(meta.tokens)
        is Meta.NameValue -> listOf(AttributeEntry(meta.path, litString(meta.value), emptyList(), tokensFrom(meta.value)))
        is Meta.PathMeta -> emptyList()
    }

private fun entriesFromTokens(tokens: TokenStream): List<AttributeEntry> {
    val result = mutableListOf<AttributeEntry>()
    val iterator = tokens.iterator()
    while (iterator.hasNext()) {
        val tree = iterator.next()
        if (tree !is TokenTree.Ident) {
            continue
        }
        val path = Path.from(tree.value.copy())
        var value: String? = null
        var nested = emptyList<AttributeEntry>()
        val captured = mutableListOf<TokenTree>(tree)
        if (iterator.hasNext()) {
            val next = iterator.next()
            captured += next
            when {
                next is TokenTree.Punct && next.value.toString() == "=" -> {
                    if (iterator.hasNext()) {
                        val literal = iterator.next()
                        captured += literal
                        value = literalText(literal)
                    }
                }

                next is TokenTree.Group -> nested = entriesFromTokens(next.value.stream())
                next is TokenTree.Punct && next.value.toString() == "," -> Unit
            }
        }
        result += AttributeEntry(path, value, nested, TokenStream.fromTokenTrees(captured))
    }
    return result
}

private fun setRenameRule(
    cx: Ctxt,
    attr: Attr<RenameRule>,
    path: Path,
    value: io.github.kotlinmania.syn.LitStr,
) {
    RenameRule
        .fromStr(value.value())
        .onSuccess { rule -> attr.set(path, rule) }
        .onFailure { err -> cx.errorSpannedBy(value, err) }
}

private fun parseExprPath(value: String): Expr.Path = Expr.Path(emptyList(), null, parsePath(value))

private fun parseType(value: String): SynType = SynType.Path(null, parsePath(value))

private fun parsePath(value: String): Path {
    val cleaned = value.substringBefore('<').trim()
    val segments = cleaned.split("::").filter { it.isNotBlank() }
    val span = Span.callSite()
    val path = Path(null, Punctuated.new())
    for (segment in segments.ifEmpty { listOf(cleaned.ifBlank { "_" }) }) {
        path.segments.push(PathSegment.from(Ident.new(segment.removePrefix("r#"), span))) { PathSep.from(span) }
    }
    if ('<' in value && path.segments.len() > 0) {
        val last = path.segments[path.segments.len() - 1]
        last.arguments =
            PathArguments.AngleBracketed(
                colon2Token = null,
                ltToken =
                    io.github.kotlinmania.syn.token.Lt
                        .from(span),
                args = Punctuated.new(),
                gtToken =
                    io.github.kotlinmania.syn.token.Gt
                        .from(span),
            )
    }
    return path
}

private fun appendPath(
    expr: Expr.Path,
    segment: String,
): Expr.Path {
    val copied = expr.copy(attrs = expr.attrs.map { it.deepCopy() }, path = expr.path.deepCopy())
    val span =
        copied.path.segments
            .last()
            ?.ident
            ?.span() ?: Span.callSite()
    copied.path.segments.push(PathSegment.from(Ident.new(segment, span))) { PathSep.from(span) }
    return copied
}

private fun parseLifetimes(value: String): Set<Lifetime> =
    value
        .split('+')
        .map { it.trim() }
        .filter { it.startsWith("'") && it.length > 1 }
        .mapTo(mutableSetOf()) { Lifetime.new(it, Span.callSite()) }

private fun borrowCowPath(
    private: Ident,
    function: String,
): Expr.Path =
    Expr.Path(
        attrs = emptyList(),
        qself = null,
        path = pathFromSegments(listOf("_serde", private.toString(), "de", function)),
    )

private fun pathFromSegments(segments: List<String>): Path {
    val span = Span.callSite()
    val path = Path(null, Punctuated.new())
    for (segment in segments) {
        path.segments.push(PathSegment.from(Ident.new(segment, span))) { PathSep.from(span) }
    }
    return path
}

private fun isImplicitlyBorrowed(ty: SynType): Boolean = isImplicitlyBorrowedReference(ty) || isOption(ty, ::isImplicitlyBorrowedReference)

private fun isImplicitlyBorrowedReference(ty: SynType): Boolean = isReference(ty, ::isStr) || isReference(ty, ::isSliceU8)

private fun isCow(
    ty: SynType,
    elem: (SynType) -> Boolean,
): Boolean {
    val path = (ungroup(ty) as? SynType.Path)?.path ?: return false
    val seg = path.segments.last() ?: return false
    val args = (seg.arguments as? PathArguments.AngleBracketed)?.args ?: return false
    if (seg.ident.toString() != "Cow" || args.len() != 2) {
        return false
    }
    return args[0] is GenericArgument.LifetimeArg &&
        (args[1] as? GenericArgument.TypeArg)?.type?.let(elem) == true
}

private fun isOption(
    ty: SynType,
    elem: (SynType) -> Boolean,
): Boolean {
    val path = (ungroup(ty) as? SynType.Path)?.path ?: return false
    val seg = path.segments.last() ?: return false
    val args = (seg.arguments as? PathArguments.AngleBracketed)?.args ?: return false
    return seg.ident.toString() == "Option" &&
        args.len() == 1 &&
        (args[0] as? GenericArgument.TypeArg)?.type?.let(elem) == true
}

private fun isReference(
    ty: SynType,
    elem: (SynType) -> Boolean,
): Boolean = (ungroup(ty) as? SynType.Reference)?.elem?.let(elem) == true

private fun isStr(ty: SynType): Boolean = isPrimitiveType(ty, "str")

private fun isSliceU8(ty: SynType): Boolean = (ungroup(ty) as? SynType.Slice)?.elem?.let { isPrimitiveType(it, "u8") } == true

private fun isPrimitiveType(
    ty: SynType,
    primitive: String,
): Boolean {
    val pathType = ungroup(ty) as? SynType.Path ?: return false
    return pathType.qself == null && isPrimitivePath(pathType.path, primitive)
}

private fun isPrimitivePath(
    path: Path,
    primitive: String,
): Boolean =
    path.leadingColon == null &&
        path.segments.len() == 1 &&
        path.segments[0].ident.toString() == primitive &&
        path.segments[0].arguments.isEmpty()

private fun borrowableLifetimes(
    cx: Ctxt,
    name: Name,
    field: SynField,
): SerdeResult<Set<Lifetime>> {
    val lifetimes = mutableSetOf<Lifetime>()
    collectLifetimes(field.ty, lifetimes)
    return if (lifetimes.isEmpty()) {
        cx.errorSpannedBy(field, "field `$name` has no lifetimes to borrow")
        SerdeResult.failure(SerdeError("no borrowable lifetimes"))
    } else {
        SerdeResult.success(lifetimes)
    }
}

private fun collectLifetimes(
    ty: SynType,
    out: MutableSet<Lifetime>,
) {
    when (val type = ty) {
        is SynType.Slice -> collectLifetimes(type.elem, out)
        is SynType.Array -> collectLifetimes(type.elem, out)
        is SynType.Ptr -> collectLifetimes(type.elem, out)
        is SynType.Reference -> {
            type.lifetime?.let(out::add)
            collectLifetimes(type.elem, out)
        }

        is SynType.Tuple -> {
            for (elem in type.elems) {
                collectLifetimes(elem, out)
            }
        }

        is SynType.Path -> {
            type.qself?.let { collectLifetimes(it.ty, out) }
            for (seg in type.path.segments) {
                val bracketed = seg.arguments as? PathArguments.AngleBracketed ?: continue
                for (arg in bracketed.args) {
                    when (arg) {
                        is GenericArgument.LifetimeArg -> out.add(arg.lifetime)
                        is GenericArgument.TypeArg -> collectLifetimes(arg.type, out)
                        is GenericArgument.AssocTypeArg -> collectLifetimes(arg.assoc.ty, out)
                        is GenericArgument.ConstArg,
                        is GenericArgument.AssocConstArg,
                        is GenericArgument.ConstraintArg,
                        -> Unit
                    }
                }
            }
        }

        is SynType.Paren -> collectLifetimes(type.elem, out)
        is SynType.Group -> collectLifetimes(type.elem, out)
        is SynType.Macro -> collectLifetimesFromTokens(type.mac.tokens, out)
        is SynType.BareFn,
        is SynType.Never,
        is SynType.TraitObject,
        is SynType.ImplTrait,
        is SynType.Infer,
        is SynType.Verbatim,
        -> Unit
    }
}

private fun collectLifetimesFromTokens(
    tokens: TokenStream,
    out: MutableSet<Lifetime>,
) {
    val iterator = tokens.iterator()
    while (iterator.hasNext()) {
        when (val tree = iterator.next()) {
            is TokenTree.Punct ->
                if (tree.value.toString() == "'" && iterator.hasNext()) {
                    val ident = iterator.next()
                    if (ident is TokenTree.Ident) {
                        out.add(Lifetime.new("'${ident.value}", tree.value.span()))
                    }
                }

            is TokenTree.Group -> collectLifetimesFromTokens(tree.value.stream(), out)
            else -> Unit
        }
    }
}

private fun tokensFrom(obj: Any): TokenStream =
    when (obj) {
        is TokenStream -> obj
        is TokenTree -> TokenStream.fromTokenTree(obj)
        is ToTokens -> obj.intoTokenStream()
        is Path -> TokenStream.fromString(obj.toString()).getOrDefault(TokenStream.new())
        is Expr -> tokensFrom(obj.toString())
        is Attribute -> tokensFrom(obj.meta.path())
        else -> TokenStream.fromString(obj.toString()).getOrDefault(TokenStream.new())
    }

private fun Attribute.tokenText(): String =
    when (val meta = meta) {
        is Meta.List -> meta.tokens.toString()
        is Meta.NameValue -> meta.value.toString()
        is Meta.PathMeta -> meta.path.toString()
    }

private fun litString(expr: Expr): String? = ((expr as? Expr.Lit)?.lit as? Lit.Str)?.value?.value()

private fun literalText(tree: TokenTree): String? =
    when (tree) {
        is TokenTree.Literal -> unquoteLiteral(tree.value)
        else -> null
    }

private fun unquoteLiteral(literal: Literal): String? {
    val text = literal.toString()
    if (text.length < 2 || text.first() != '"' || text.last() != '"') {
        return null
    }
    return buildString {
        var escaped = false
        for (ch in text.substring(1, text.length - 1)) {
            if (escaped) {
                append(
                    when (ch) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        '\\' -> '\\'
                        '"' -> '"'
                        else -> ch
                    },
                )
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else {
                append(ch)
            }
        }
    }
}

private fun litStr(
    value: String,
    span: Span,
): io.github.kotlinmania.syn.LitStr =
    io.github.kotlinmania.syn.LitStr
        .new(value, span)
