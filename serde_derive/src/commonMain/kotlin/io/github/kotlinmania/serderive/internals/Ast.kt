package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.syn.Data as SynData
import io.github.kotlinmania.syn.DeriveInput
import io.github.kotlinmania.syn.Fields
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Index
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.SynType
import io.github.kotlinmania.syn.Variant as SynVariant

public class Container(
    public val ident: Ident,
    public val attrs: AttrContainer,
    public val data: Data,
    public val generics: Generics,
    public val original: DeriveInput
) {
    public companion object {
        public fun fromAst(
            cx: Ctxt,
            item: DeriveInput,
            derive: Derive,
            private: Ident
        ): Container? {
            val attrs = AttrContainer.fromAst(cx, item)

            var data = when (val itemData = item.data) {
                is SynData.Enum -> {
                    Data.Enum(enumFromAst(cx, itemData.variants.toList(), attrs.default(), private))
                }
                is SynData.Struct -> {
                    val (style, fields) = structFromAst(cx, itemData.fields, null, attrs.default(), private)
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
                                variant.attrs.renameAllRules().or(attrs.renameAllFieldsRules())
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

            val container = Container(
                ident = item.ident.deepCopy(),
                attrs = attrs,
                data = data,
                generics = item.generics,
                original = item
            )
            check(cx, container, derive)
            return container
        }
    }
}

public sealed class Data {
    public class Enum(public val variants: List<Variant>) : Data()
    public class Struct(public val style: Style, public val fields: List<Field>) : Data()

    public fun allFields(): Sequence<Field> {
        return when (this) {
            is Enum -> variants.asSequence().flatMap { it.fields.asSequence() }
            is Struct -> fields.asSequence()
        }
    }

    public fun hasGetter(): Boolean {
        return allFields().any { it.attrs.getter() != null }
    }
}

public class Variant(
    public val ident: Ident,
    public val attrs: AttrVariant,
    public val style: Style,
    public val fields: List<Field>,
    public val original: SynVariant
)

public class Field(
    public val member: io.github.kotlinmania.syn.Member,
    public val attrs: AttrField,
    public val ty: SynType,
    public val original: io.github.kotlinmania.syn.Field
)

public enum class Style {
    Struct,
    Tuple,
    Newtype,
    Unit
}

private fun enumFromAst(
    cx: Ctxt,
    variants: Iterable<SynVariant>,
    containerDefault: Default,
    private: Ident
): List<Variant> {
    val resultVariants = variants.map { variant ->
        val attrs = AttrVariant.fromAst(cx, variant)
        val (style, fields) = structFromAst(
            cx,
            variant.fields,
            attrs,
            containerDefault,
            private
        )
        Variant(
            ident = variant.ident.deepCopy(),
            attrs = attrs,
            style = style,
            fields = fields,
            original = variant
        )
    }

    val indexOfLastTaggedVariant = resultVariants.indexOfLast { !it.attrs.untagged() }
    if (indexOfLastTaggedVariant != -1) {
        for (variant in resultVariants.take(indexOfLastTaggedVariant)) {
            if (variant.attrs.untagged()) {
                cx.errorSpannedBy(
                    variant.ident,
                    "all variants with the #[serde(untagged)] attribute must be placed at the end of the enum"
                )
            }
        }
    }

    return resultVariants
}

private fun structFromAst(
    cx: Ctxt,
    fields: io.github.kotlinmania.syn.Fields,
    attrs: AttrVariant?,
    containerDefault: Default,
    private: Ident
): Pair<Style, List<Field>> {
    return when (fields) {
        is Fields.Named -> Pair(
            Style.Struct,
            fieldsFromAst(cx, fields.fields.named.toList(), attrs, containerDefault, private)
        )
        is Fields.Unnamed -> {
            if (fields.fields.unnamed.size == 1) {
                Pair(
                    Style.Newtype,
                    fieldsFromAst(cx, fields.fields.unnamed.toList(), attrs, containerDefault, private)
                )
            } else {
                Pair(
                    Style.Tuple,
                    fieldsFromAst(cx, fields.fields.unnamed.toList(), attrs, containerDefault, private)
                )
            }
        }
        is Fields.Unit -> Pair(Style.Unit, emptyList())
    }
}

private fun fieldsFromAst(
    cx: Ctxt,
    fields: Iterable<io.github.kotlinmania.syn.Field>,
    attrs: AttrVariant?,
    containerDefault: Default,
    private: Ident
): List<Field> {
    val dstFields = mutableListOf<Field>()
    for (field in fields) {
        val ident = field.ident
        val member = if (ident != null) {
            Member.Named(ident.deepCopy())
        } else {
            Member.Unnamed(io.github.kotlinmania.syn.Index(dstFields.size.toUInt(), io.github.kotlinmania.procmacro2.Span.callSite()))
        }
        dstFields.add(
            Field(
                member = member,
                attrs = AttrField.fromAst(
                    cx,
                    dstFields.size,
                    field,
                    attrs,
                    containerDefault,
                    private
                ),
                ty = field.ty,
                original = field
            )
        )
    }
    return dstFields
}

