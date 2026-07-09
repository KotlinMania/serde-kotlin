// port-lint: source de/enum_untagged.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.quote.quoteSpanned
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Expr
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.Variant
import io.github.kotlinmania.syn.span

// Generates the deserialize body for an enum with the untagged attribute.
internal fun deserializeEnumUntagged(
    params: Parameters,
    variants: List<Variant>,
    cattrs: AttrContainer,
    firstAttempt: TokenStream?
): Fragment {
    val attempts = variants
        .filter { !it.attrs.skipDeserializing() }
        .map { variant -> Expr(deserializeVariant(params, variant, cattrs)) }

    val fallthroughMsg = "data did not match any variant of untagged enum ${params.typeName()}"
    val fallthroughMsgVal = cattrs.expecting() ?: fallthroughMsg

    return Fragment.Block(quote("""
        let __content = _serde::de::DeserializeSeed::deserialize(_serde.`#`Private::de::ContentVisitor::new(), __deserializer)?;
        let __deserializer = _serde.`#`Private::de::ContentRefDeserializer::<__D::Error>::new(&__content);

        `#`firstAttempt

        `#`(
            if let _serde.`#`Private::Ok(__ok) = `#`attempts {
                return _serde.`#`Private::Ok(__ok);
            }
        )*

        _serde.`#`Private::Err(_serde::de::Error::custom(`#`fallthroughMsgVal))
    """))
}

// Also used by adjacently tagged enums
internal fun deserializeVariant(
    params: Parameters,
    variant: Variant,
    cattrs: AttrContainer
): Fragment {
    val path = variant.attrs.deserializeWith()
    if (path != null) {
        val unwrapFn = unwrapToVariantClosure(params, variant, false)
        return Fragment.Block(quote("""
            _serde.`#`Private::Result::map(`#`path(__deserializer), `#`unwrapFn)
        """))
    }

    val variantIdent = variant.ident

    return when (effectiveStyle(variant)) {
        Style.Unit -> {
            val thisValue = params.thisValue
            val typeName = params.typeName()
            val variantName = variant.ident.toString()
            val default = variant.fields.firstOrNull()?.let { field ->
                val defaultExpr = Stmts(exprIsMissing(field, cattrs))
                quote("(`#`defaultExpr)")
            } ?: quote("")
            Fragment.Expr(quote("""
                match _serde::Deserializer::deserialize_any(
                    __deserializer,
                    _serde.`#`Private::de::UntaggedUnitVisitor::new(`#`typeName, `#`variantName)
                ) {
                    _serde.`#`Private::Ok(()) => _serde.`#`Private::Ok(`#`thisValue::`#`variantIdent `#`default),
                    _serde.`#`Private::Err(__err) => _serde.`#`Private::Err(__err),
                }
            """))
        }
        Style.Newtype -> deserializeNewtypeVariant(variantIdent, params, variant.fields[0])
        Style.Tuple -> deserializeTuple(
            params,
            variant.fields,
            cattrs,
            TupleForm.Untagged(variantIdent)
        )
        Style.Struct -> deserializeStruct(
            params,
            variant.fields,
            cattrs,
            StructForm.Untagged(variantIdent)
        )
    }
}

// Also used by internally tagged enums
// Also used by adjacently tagged enums (via generateVariant).
internal fun deserializeNewtypeVariant(
    variantIdent: Ident,
    params: Parameters,
    field: Field
): Fragment {
    val thisValue = params.thisValue
    val fieldTy = field.ty
    val fieldPath = field.attrs.deserializeWith()
    return if (fieldPath == null) {
        val span = field.original.span()
        val func = quoteSpanned(span, "<`#`fieldTy as _serde::Deserialize>::deserialize")
        Fragment.Expr(quote("""
            _serde.`#`Private::Result::map(`#`func(__deserializer), `#`thisValue::`#`variantIdent)
        """))
    } else {
        Fragment.Block(quote("""
            let __value: _serde.`#`Private::Result<`#`fieldTy, _> = `#`fieldPath(__deserializer);
            _serde.`#`Private::Result::map(__value, `#`thisValue::`#`variantIdent)
        """))
    }
}