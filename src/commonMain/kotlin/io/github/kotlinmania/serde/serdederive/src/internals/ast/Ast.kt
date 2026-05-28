// port-lint: source serde_derive/src/internals/ast.rs
package io.github.kotlinmania.serde.serdederive.src.internals.ast

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.serde.serdederive.src.internals.Ctxt
import io.github.kotlinmania.serde.serdederive.src.internals.Derive
import io.github.kotlinmania.serde.serdederive.src.internals.check.check
import io.github.kotlinmania.syn.*
import io.github.kotlinmania.syn.token.Comma
import io.github.kotlinmania.serde.serdederive.src.internals.attr.Container as AttrContainer
import io.github.kotlinmania.serde.serdederive.src.internals.attr.Default as AttrDefault
import io.github.kotlinmania.serde.serdederive.src.internals.attr.Field as AttrField
import io.github.kotlinmania.serde.serdederive.src.internals.attr.Variant as AttrVariant
import io.github.kotlinmania.syn.Data as SynData
import io.github.kotlinmania.syn.Field as SynField
import io.github.kotlinmania.syn.Fields as SynFields
import io.github.kotlinmania.syn.Variant as SynVariant

/**
 * A Serde syntax tree, parsed from the Syn syntax tree and ready to generate output code.
 */

/**
 * A source data structure annotated with derive Serialize and/or derive Deserialize,
 * parsed into an internal representation.
 */
class Container(
    /**
     * The struct or enum name, without generics.
     */
    val ident: Ident,
    /**
     * Attributes on the structure, parsed for Serde.
     */
    val attrs: AttrContainer,
    /**
     * The contents of the struct or enum.
     */
    var data: Data,
    /**
     * Any generics on the struct or enum.
     */
    val generics: Generics,
    /**
     * Original input.
     */
    val original: DeriveInput,
) {
    companion object {
        /**
         * Convert the raw Syn syntax tree into a parsed container object, collecting errors in `cx`.
         */
        fun fromAst(
            cx: Ctxt,
            item: DeriveInput,
            derive: Derive,
            private: Ident,
        ): Container? {
            val attrs = AttrContainer.fromAst(cx, item)

            val data =
                when (val itemData = item.data) {
                    is SynData.Enum ->
                        Data.Enum(enumFromAst(cx, itemData.variants, attrs.default(), private))

                    is SynData.Struct -> {
                        val (style, fields) =
                            structFromAst(cx, itemData.fields, null, attrs.default(), private)
                        Data.Struct(style, fields)
                    }

                    is SynData.Union -> {
                        cx.errorSpannedBy(item, "Serde does not support derive for unions")
                        return null
                    }
                }

            when (data) {
                is Data.Enum -> {
                    for (variant in data.variants) {
                        variant.attrs.renameByRules(attrs.renameAllRules())
                        for (field in variant.fields) {
                            field.attrs.renameByRules(
                                variant.attrs.renameAllRules().or(attrs.renameAllFieldsRules()),
                            )
                        }
                    }
                }

                is Data.Struct -> {
                    for (field in data.fields) {
                        field.attrs.renameByRules(attrs.renameAllRules())
                    }
                }
            }

            val container =
                Container(
                    ident = item.ident.copy(),
                    attrs = attrs,
                    data = data,
                    generics = item.generics,
                    original = item,
                )
            check(cx, container, derive)
            return container
        }
    }
}

/**
 * The fields of a struct or enum.
 *
 * Analogous to `syn.Data`.
 */
sealed class Data {
    data class Enum(
        val variants: List<Variant>,
    ) : Data()

    data class Struct(
        val style: Style,
        val fields: List<Field>,
    ) : Data()

    fun allFields(): Sequence<Field> =
        when (this) {
            is Enum -> variants.asSequence().flatMap { variant -> variant.fields.asSequence() }
            is Struct -> fields.asSequence()
        }

    fun hasGetter(): Boolean = allFields().any { field -> field.attrs.getter() != null }
}

/**
 * A variant of an enum.
 */
class Variant(
    val ident: Ident,
    val attrs: AttrVariant,
    val style: Style,
    val fields: List<Field>,
    val original: SynVariant,
)

/**
 * A field of a struct.
 */
class Field(
    val member: Member,
    val attrs: AttrField,
    val ty: SynType,
    val original: SynField,
)

enum class Style {
    /**
     * Named fields.
     */
    Struct,

    /**
     * Many unnamed fields.
     */
    Tuple,

    /**
     * One unnamed field.
     */
    Newtype,

    /**
     * No fields.
     */
    Unit,
}

private fun enumFromAst(
    cx: Ctxt,
    variants: Punctuated<SynVariant, Comma>,
    containerDefault: AttrDefault,
    private: Ident,
): List<Variant> {
    val parsedVariants =
        variants.map { variant ->
            val attrs = AttrVariant.fromAst(cx, variant)
            val (style, fields) =
                structFromAst(
                    cx,
                    variant.fields,
                    attrs,
                    containerDefault,
                    private,
                )
            Variant(
                ident = variant.ident.copy(),
                attrs = attrs,
                style = style,
                fields = fields,
                original = variant,
            )
        }

    val indexOfLastTaggedVariant =
        parsedVariants.indexOfLast { variant -> !variant.attrs.untagged() }
    if (indexOfLastTaggedVariant >= 0) {
        for (variant in parsedVariants.take(indexOfLastTaggedVariant)) {
            if (variant.attrs.untagged()) {
                cx.errorSpannedBy(
                    variant.ident,
                    "all variants with the #[serde(untagged)] attribute must be placed at the end of the enum",
                )
            }
        }
    }

    return parsedVariants
}

private fun structFromAst(
    cx: Ctxt,
    fields: SynFields,
    attrs: AttrVariant?,
    containerDefault: AttrDefault,
    private: Ident,
): Pair<Style, List<Field>> =
    when (fields) {
        is SynFields.Named ->
            Style.Struct to fieldsFromAst(cx, fields.fields.named, attrs, containerDefault, private)

        is SynFields.Unnamed ->
            if (fields.fields.unnamed.len() == 1) {
                Style.Newtype to fieldsFromAst(cx, fields.fields.unnamed, attrs, containerDefault, private)
            } else {
                Style.Tuple to fieldsFromAst(cx, fields.fields.unnamed, attrs, containerDefault, private)
            }

        SynFields.Unit -> Style.Unit to emptyList()
    }

private fun fieldsFromAst(
    cx: Ctxt,
    fields: Punctuated<SynField, Comma>,
    attrs: AttrVariant?,
    containerDefault: AttrDefault,
    private: Ident,
): List<Field> {
    val dstFields = mutableListOf<Field>()
    for (field in fields) {
        dstFields +=
            Field(
                member =
                    field.ident?.let { ident -> Member.Named(ident.copy()) }
                        ?: Member.Unnamed(Index(dstFields.size.toUInt(), field.tySpan())),
                attrs =
                    AttrField.fromAst(
                        cx,
                        dstFields.size,
                        field,
                        attrs,
                        containerDefault,
                        private,
                    ),
                ty = field.ty,
                original = field,
            )
    }
    return dstFields
}

private fun SynField.tySpan(): Span =
    when (val type = ty) {
        is SynType.Path -> type.path.getIdent()?.span() ?: Span.callSite()
        else -> Span.callSite()
    }
