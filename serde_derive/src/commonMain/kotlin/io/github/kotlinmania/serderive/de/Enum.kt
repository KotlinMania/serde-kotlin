// port-lint: source de/enum_.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Expr
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.serderive.internals.TagType
import io.github.kotlinmania.serderive.internals.Variant

// Generates the deserialize body for an enum.
internal fun deserializeEnum(
    params: Parameters,
    variants: List<Variant>,
    cattrs: AttrContainer
): Fragment {
    // The variants have already been checked (in Ast.kt) that all untagged variants appear at the end
    val untaggedIdx = variants.indexOfFirst { it.attrs.untagged() }
    return if (untaggedIdx >= 0) {
        val tagged = variants.subList(0, untaggedIdx)
        val untagged = variants.subList(untaggedIdx, variants.size)
        val taggedFrag = Expr(deserializeHomogeneousEnum(params, tagged, cattrs))
        // Ignore any error associated with non-untagged deserialization so that we
        // can fall through to the untagged variants. This may be infallible so we
        // need to provide the error type.
        val firstAttempt = quote("""
            if let _serde.`#`Private::Result::<_, __D::Error>::Ok(__ok) = (|| `#`taggedFrag)() {
                return _serde.`#`Private::Ok(__ok);
            }
        """)
        deserializeEnumUntagged(params, untagged, cattrs, firstAttempt)
    } else {
        deserializeHomogeneousEnum(params, variants, cattrs)
    }
}

private fun deserializeHomogeneousEnum(
    params: Parameters,
    variants: List<Variant>,
    cattrs: AttrContainer
): Fragment {
    return when (val tag = cattrs.tag()) {
        is TagType.External -> deserializeEnumExternally(params, variants, cattrs)
        is TagType.Internal -> deserializeEnumInternally(params, variants, cattrs, tag.tag)
        is TagType.Adjacent -> deserializeEnumAdjacently(params, variants, cattrs, tag.tag, tag.content)
        is TagType.None -> deserializeEnumUntagged(params, variants, cattrs, null)
    }
}

internal fun prepareEnumVariantEnum(variants: List<Variant>): Pair<TokenStream, Stmts> {
    val deserializedVariants = variants.mapIndexedNotNull { i, variant ->
        if (variant.attrs.skipDeserializing()) null else Pair(i, variant)
    }

    val fallthrough = deserializedVariants.find { (_, variant) -> variant.attrs.other() }
        ?.let { (i, _) ->
            val ignoreVariant = fieldI(i)
            quote("_serde.`#`Private::Ok(__Field::`#`ignoreVariant)")
        }

    val variantsStmt = quote("""
        `#`[doc(hidden)]
        const VARIANTS: &'static [&'static str] = &[ `#`(`#`deserializedVariants.map { it.second.attrs.aliases() }.flatten()),* ];
    """)

    val fieldWithAliases = deserializedVariants.map { (i, variant) ->
        FieldWithAliases(
            ident = fieldI(i),
            aliases = variant.attrs.aliases()
        )
    }

    val variantVisitor = Stmts(deserializeGenerated(
        fieldWithAliases,
        false,
        true,
        null,
        fallthrough
    ))

    return Pair(variantsStmt, variantVisitor)
}