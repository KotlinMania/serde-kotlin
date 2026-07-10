// port-lint: source de/enum_externally.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.serderive.checkedQuote
import io.github.kotlinmania.serderive.checkedQuoteSpanned
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Expr
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Match
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.Variant
import io.github.kotlinmania.syn.span

// Generates the deserialize body for an enum without additional attributes.
internal fun deserializeEnumExternally(
    params: Parameters,
    variants: List<Variant>,
    cattrs: AttrContainer
): Fragment {
    val thisType = params.thisType
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()

    val typeName = cattrs.name().deserializeName()
    val expecting = "enum ${params.typeName()}"
    val expectingVal = cattrs.expecting() ?: expecting

    val (variantsStmt, variantVisitor) = prepareEnumVariantEnum(variants)

    // Match arms to extract a variant from a string
    val variantArms = variants.mapIndexedNotNull { i, variant ->
        if (variant.attrs.skipDeserializing()) return@mapIndexedNotNull null

        val variantName = fieldI(i)

        val block = Match(deserializeExternallyTaggedVariant(params, variant, cattrs))

        checkedQuote("_serde::`#`Private::Ok((__Field::`#`variantName, __variant)) => `#`block")
    }

    val allSkipped = variants.all { it.attrs.skipDeserializing() }
    val matchVariant = if (allSkipped) {
        checkedQuote("""
            _serde::`#`Private::Result::map(
                _serde::de::EnumAccess::variant::<__Field>(__data),
                |(__impossible, _)| match __impossible {})
        """)
    } else {
        checkedQuote("""
            match _serde::de::EnumAccess::variant(__data) {
                `#`(`#`variantArms)*
                _serde::`#`Private::Err(__err) => _serde::`#`Private::Err(__err),
            }
        """)
    }

    return Fragment.Block(checkedQuote("""
        `#`variantVisitor

        `#`[doc(hidden)]
        struct __Visitor `#`deImplGenerics `#`whereClause {
            marker: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn expecting(&self, __formatter: &mut _serde::`#`Private::Formatter) -> _serde::`#`Private::fmt::Result {
                _serde::`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
            }

            fn visit_enum<__A>(self, __data: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::EnumAccess<`#`delife>,
            {
                `#`matchVariant
            }
        }

        `#`variantsStmt

        _serde::Deserializer::deserialize_enum(
            __deserializer,
            `#`typeName,
            VARIANTS,
            __Visitor {
                marker: _serde::`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
                lifetime: _serde::`#`Private::PhantomData,
            },
        )
    """))
}

private fun deserializeExternallyTaggedVariant(
    params: Parameters,
    variant: Variant,
    cattrs: AttrContainer
): Fragment {
    val path = variant.attrs.deserializeWith()
    if (path != null) {
        val (wrapper, wrapperTy, unwrapFn) = wrapDeserializeVariantWith(params, variant, path)
        return Fragment.Block(checkedQuote("""
            `#`wrapper
            _serde::`#`Private::Result::map(
                _serde::de::VariantAccess::newtype_variant::<`#`wrapperTy>(__variant), `#`unwrapFn)
        """))
    }

    val variantIdent = variant.ident

    return when (variant.style) {
        Style.Unit -> {
            val thisValue = params.thisValue
            Fragment.Block(checkedQuote("""
                _serde::de::VariantAccess::unit_variant(__variant)?;
                _serde::`#`Private::Ok(`#`thisValue::`#`variantIdent)
            """))
        }
        Style.Newtype -> deserializeExternallyTaggedNewtypeVariant(
            variantIdent,
            params,
            variant.fields[0],
            cattrs
        )
        Style.Tuple -> deserializeTuple(
            params,
            variant.fields,
            cattrs,
            TupleForm.ExternallyTagged(variantIdent)
        )
        Style.Struct -> deserializeStruct(
            params,
            variant.fields,
            cattrs,
            StructForm.ExternallyTagged(variantIdent)
        )
    }
}

private fun wrapDeserializeVariantWith(
    params: Parameters,
    variant: Variant,
    deserializeWith: io.github.kotlinmania.syn.Expr.Path
): Triple<TokenStream, TokenStream, TokenStream> {
    val fieldTys = variant.fields.map { it.ty }
    val (wrapper, wrapperTy) = wrapDeserializeWith(params, checkedQuote("(`#`(`#`fieldTys),*)"), deserializeWith)

    val unwrapFn = unwrapToVariantClosure(params, variant, true)

    return Triple(wrapper, wrapperTy, unwrapFn)
}

private fun deserializeExternallyTaggedNewtypeVariant(
    variantIdent: Ident,
    params: Parameters,
    field: Field,
    cattrs: AttrContainer
): Fragment {
    val thisValue = params.thisValue

    if (field.attrs.skipDeserializing()) {
        val default = Expr(exprIsMissing(field, cattrs))
        return Fragment.Block(checkedQuote("""
            _serde::de::VariantAccess::unit_variant(__variant)?;
            _serde::`#`Private::Ok(`#`thisValue::`#`variantIdent(`#`default))
        """))
    }

    val fieldPath = field.attrs.deserializeWith()
    return if (fieldPath == null) {
        val fieldTy = field.ty
        val span = field.original.span()
        val func = checkedQuoteSpanned(span, "_serde::de::VariantAccess::newtype_variant::<`#`fieldTy>")
        Fragment.Expr(checkedQuote("""
            _serde::`#`Private::Result::map(`#`func(__variant), `#`thisValue::`#`variantIdent)
        """))
    } else {
        val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, fieldPath)
        Fragment.Block(checkedQuote("""
            `#`wrapper
            _serde::`#`Private::Result::map(
                _serde::de::VariantAccess::newtype_variant::<`#`wrapperTy>(__variant),
                |__wrapper| `#`thisValue::`#`variantIdent(__wrapper.value))
        """))
    }
}