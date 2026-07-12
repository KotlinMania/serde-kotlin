// port-lint: source de/enum_internally.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Match
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.Variant

// Generates the deserialize body for an enum with the tag attribute.
internal fun deserializeEnumInternally(
    params: Parameters,
    variants: List<Variant>,
    cattrs: AttrContainer,
    tag: String
): Fragment {
    val (variantsStmt, variantVisitor) = prepareEnumVariantEnum(variants)

    // Match arms to extract a variant from a string
    val variantArms = variants.mapIndexedNotNull { i, variant ->
        if (variant.attrs.skipDeserializing()) return@mapIndexedNotNull null

        val variantName = fieldI(i)

        val block = Match(deserializeInternallyTaggedVariant(params, variant, cattrs))

        quote(
            "__Field::`#`variantName => `#`block",
            "variantName" to variantName,
            "block" to block,
        )
    }

    val expecting = "internally tagged enum ${params.typeName()}"
    val expectingVal = cattrs.expecting() ?: expecting

    return Fragment.Block(quote("""
        `#`variantVisitor

        `#`variantsStmt

        let (__tag, __content) = _serde::Deserializer::deserialize_any(
            __deserializer,
            _serde::`#`Private::de::TaggedContentVisitor::<__Field>::new(`#`tag, `#`expectingVal))?;
        let __deserializer = _serde::`#`Private::de::ContentDeserializer::<__D::Error>::new(__content);

        match __tag {
            `#`(`#`variantArms)*
        }
    """, mapOf(
        "variantVisitor" to variantVisitor,
        "variantsStmt" to variantsStmt,
        "Private" to Private,
        "tag" to tag,
        "expectingVal" to expectingVal,
        "variantArms" to variantArms,
    )))
}

// Generates significant parts of the sequence and map visitor bodies
// for the variants of internally tagged enum.
private fun deserializeInternallyTaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: AttrContainer
): Fragment {
    val path = variant.attrs.deserializeWith()
    if (path != null) {
        val unwrapFn = unwrapToVariantClosure(params, variant, false)
        return Fragment.Block(quote("""
            _serde::`#`Private::Result::map(`#`path(__deserializer), `#`unwrapFn)
        """, "Private" to Private, "path" to path, "unwrapFn" to unwrapFn))
    }

    val variantIdent = variant.ident

    return when (effectiveStyle(variant)) {
        Style.Unit -> {
            val thisValue = params.thisValue
            val typeName = params.typeName()
            val variantName = variant.ident.toString()
            val default = variant.fields.firstOrNull()?.let { field ->
                val defaultExpr = Stmts(exprIsMissing(field, cattrs))
                quote("(`#`defaultExpr)", "defaultExpr" to defaultExpr)
            } ?: quote("")
            Fragment.Block(quote("""
                _serde::Deserializer::deserialize_any(__deserializer, _serde::`#`Private::de::InternallyTaggedUnitVisitor::new(`#`typeName, `#`variantName))?;
                _serde::`#`Private::Ok(`#`thisValue::`#`variantIdent `#`default)
            """, mapOf(
                "Private" to Private,
                "typeName" to typeName,
                "variantName" to variantName,
                "thisValue" to thisValue,
                "variantIdent" to variantIdent,
                "default" to default,
            )))
        }
        Style.Newtype -> {
            deserializeNewtypeVariant(variantIdent, params, variant.fields[0])
        }
        Style.Struct -> {
            deserializeStruct(
                params,
                variant.fields,
                cattrs,
                StructForm.InternallyTagged(variantIdent)
            )
        }
        Style.Tuple -> error("checked in serde_derive_internals")
    }
}
