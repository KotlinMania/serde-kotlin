// port-lint: source de/identifier.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.Literal
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Expr
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Identifier
import io.github.kotlinmania.serderive.internals.Name
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.Variant

// Deserialization of struct field identifiers and enum variant identifiers by
// way of a Rust enum.

// Generates `Deserialize::deserialize` body for an enum with
// `serde(field_identifier)` or `serde(variant_identifier)` attribute.
internal fun deserializeCustom(
    params: Parameters,
    variants: List<Variant>,
    cattrs: AttrContainer
): Fragment {
    val isVariant = when (cattrs.identifier()) {
        Identifier.Variant -> true
        Identifier.Field -> false
        Identifier.No -> error("checked in serde_derive_internals")
    }

    val thisType = params.thisType
    val thisValue = params.thisValue

    val (ordinary, fallthrough, fallthroughBorrowed) = if (variants.isNotEmpty()) {
        val last = variants.last()
        val lastIdent = last.ident
        if (last.attrs.other()) {
            // Process `serde(other)` attribute. It would always be found on the
            // last variant (checked in `check_identifier`), so all preceding
            // are ordinary variants.
            val ordinary = variants.subList(0, variants.size - 1)
            val fallthrough = quote("_serde.`#`Private::Ok(`#`thisValue::`#`lastIdent)")
            Triple(ordinary, fallthrough, null)
        } else if (last.style == Style.Newtype) {
            val ordinary = variants.subList(0, variants.size - 1)
            val fallthrough = { value: TokenStream ->
                quote("""
                    _serde.`#`Private::Result::map(
                        _serde::Deserialize::deserialize(
                            _serde.`#`Private::de::IdentifierDeserializer::from(`#`value)
                        ),
                        `#`thisValue::`#`lastIdent)
                """)
            }
            Triple(
                ordinary,
                fallthrough(quote("__value")),
                fallthrough(quote("_serde.`#`Private::de::Borrowed(__value)"))
            )
        } else {
            Triple(variants, null, null)
        }
    } else {
        Triple(variants, null, null)
    }

    val identsAliases = ordinary.map { variant ->
        FieldWithAliases(
            ident = variant.ident,
            aliases = variant.attrs.aliases()
        )
    }

    val names = identsAliases.flatMap { it.aliases }

    val namesConst = if (fallthrough != null) {
        null
    } else if (isVariant) {
        quote("""
            `#`[doc(hidden)]
            const VARIANTS: &'static [&'static str] = &[ `#`(`#`names),* ];
        """)
    } else {
        quote("""
            `#`[doc(hidden)]
            const FIELDS: &'static [&'static str] = &[ `#`(`#`names),* ];
        """)
    }

    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()
    val thisValueTs = TokenStream.new().also { thisValue.toTokens(it) }
    val visitorImpl = Stmts(deserializeIdentifier(
        thisValueTs,
        identsAliases,
        isVariant,
        fallthrough,
        fallthroughBorrowed,
        false,
        cattrs.expecting()
    ))

    return Fragment.Block(quote("""
        `#`namesConst

        `#`[doc(hidden)]
        struct __FieldVisitor `#`deImplGenerics `#`whereClause {
            marker: _serde.`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde.`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __FieldVisitor `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            `#`visitorImpl
        }

        let __visitor = __FieldVisitor {
            marker: _serde.`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
            lifetime: _serde.`#`Private::PhantomData,
        };
        _serde::Deserializer::deserialize_identifier(__deserializer, __visitor)
    """))
}

internal fun deserializeGenerated(
    deserializedFields: List<FieldWithAliases>,
    hasFlatten: Boolean,
    isVariant: Boolean,
    ignoreVariant: TokenStream?,
    fallthrough: TokenStream?
): Fragment {
    val thisValue = quote("__Field")
    val fieldIdents = deserializedFields.map { it.ident }

    val visitorImpl = Stmts(deserializeIdentifier(
        thisValue,
        deserializedFields,
        isVariant,
        fallthrough,
        null,
        !isVariant && hasFlatten,
        null
    ))

    val lifetime = if (!isVariant && hasFlatten) {
        quote("<'de>")
    } else {
        null
    }

    return Fragment.Block(quote("""
        `#`[allow(non_camel_case_types)]
        `#`[doc(hidden)]
        enum __Field `#`lifetime {
            `#`(`#`fieldIdents),*
            `#`ignoreVariant
        }

        `#`[doc(hidden)]
        struct __FieldVisitor;

        `#`[automatically_derived]
        impl<'de> _serde::de::Visitor<'de> for __FieldVisitor {
            type Value = __Field `#`lifetime;

            `#`visitorImpl
        }

        `#`[automatically_derived]
        impl<'de> _serde::Deserialize<'de> for __Field `#`lifetime {
            `#`[inline]
            fn deserialize<__D>(__deserializer: __D) -> _serde.`#`Private::Result<Self, __D::Error>
            where
                __D: _serde::Deserializer<'de>,
            {
                _serde::Deserializer::deserialize_identifier(__deserializer, __FieldVisitor)
            }
        }
    """))
}

private fun deserializeIdentifier(
    thisValue: TokenStream,
    deserializedFields: List<FieldWithAliases>,
    isVariant: Boolean,
    fallthrough: TokenStream?,
    fallthroughBorrowed: TokenStream?,
    collectOtherFields: Boolean,
    expecting: String?
): Fragment {
    val strMapping = deserializedFields.map { field ->
        val ident = field.ident
        val aliases = field.aliases
        // `aliases` also contains a main name
        quote("`#`(`#`aliases),* => _serde.`#`Private::Ok(`#`thisValue::`#`ident),")
    }

    val bytesMapping = deserializedFields.map { field ->
        val ident = field.ident
        // `aliases` also contains a main name
        val byteAliases = field.aliases.map { alias ->
            Literal.byteString(alias.value.toByteArray())
        }
        quote("`#`(`#`byteAliases),* => _serde.`#`Private::Ok(`#`thisValue::`#`ident),")
    }

    val expectingVal = expecting ?: if (isVariant) "variant identifier" else "field identifier"

    val bytesToStr = if (fallthrough != null || collectOtherFields) {
        null
    } else {
        quote("let __value = &_serde.`#`Private::from_utf8_lossy(__value);")
    }

    val (valueAsStrContent, valueAsBorrowedStrContent, valueAsBytesContent, valueAsBorrowedBytesContent) =
        if (collectOtherFields) {
            Quad(
                quote("let __value = _serde.`#`Private::de::Content::String(_serde.`#`Private::ToString::to_string(__value));"),
                quote("let __value = _serde.`#`Private::de::Content::Str(__value);"),
                quote("let __value = _serde.`#`Private::de::Content::ByteBuf(__value.to_vec());"),
                quote("let __value = _serde.`#`Private::de::Content::Bytes(__value);")
            )
        } else {
            Quad(null, null, null, null)
        }

    val fallthroughArm = if (fallthrough != null) {
        fallthrough
    } else if (isVariant) {
        quote("_serde.`#`Private::Err(_serde::de::Error::unknown_variant(__value, VARIANTS))")
    } else {
        quote("_serde.`#`Private::Err(_serde::de::Error::unknown_field(__value, FIELDS))")
    }

    val visitOther = if (collectOtherFields) {
        quote("""
            fn visit_bool<__E>(self, __value: bool) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::Bool(__value)))
            }

            fn visit_i8<__E>(self, __value: i8) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::I8(__value)))
            }

            fn visit_i16<__E>(self, __value: i16) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::I16(__value)))
            }

            fn visit_i32<__E>(self, __value: i32) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::I32(__value)))
            }

            fn visit_i64<__E>(self, __value: i64) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::I64(__value)))
            }

            fn visit_u8<__E>(self, __value: u8) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::U8(__value)))
            }

            fn visit_u16<__E>(self, __value: u16) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::U16(__value)))
            }

            fn visit_u32<__E>(self, __value: u32) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::U32(__value)))
            }

            fn visit_u64<__E>(self, __value: u64) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::U64(__value)))
            }

            fn visit_f32<__E>(self, __value: f32) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::F32(__value)))
            }

            fn visit_f64<__E>(self, __value: f64) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::F64(__value)))
            }

            fn visit_char<__E>(self, __value: char) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::Char(__value)))
            }

            fn visit_unit<__E>(self) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde.`#`Private::Ok(__Field::__other(_serde.`#`Private::de::Content::Unit))
            }
        """)
    } else {
        val u64Mapping = deserializedFields.mapIndexed { i, field ->
            val ident = field.ident
            quote("`#`i => _serde.`#`Private::Ok(`#`thisValue::`#`ident)")
        }

        val u64FallthroughArm = if (fallthrough != null) {
            fallthrough
        } else {
            val indexExpecting = if (isVariant) "variant" else "field"
            val fallthroughMsg = "$indexExpecting index 0 <= i < ${deserializedFields.size}"
            quote("""
                _serde.`#`Private::Err(_serde::de::Error::invalid_value(
                    _serde::de::Unexpected::Unsigned(__value),
                    &`#`fallthroughMsg,
                ))
            """)
        }

        quote("""
            fn visit_u64<__E>(self, __value: u64) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                match __value {
                    `#`(`#`u64Mapping),*
                    _ => `#`u64FallthroughArm,
                }
            }
        """)
    }

    val visitBorrowed = if (fallthroughBorrowed != null || collectOtherFields) {
        val fallthroughBorrowedArm = fallthroughBorrowed ?: fallthroughArm
        quote("""
            fn visit_borrowed_str<__E>(self, __value: &'de str) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                match __value {
                    `#`(`#`strMapping)*
                    _ => {
                        `#`valueAsBorrowedStrContent
                        `#`fallthroughBorrowedArm
                    }
                }
            }

            fn visit_borrowed_bytes<__E>(self, __value: &'de [u8]) -> _serde.`#`Private::Result<Self::Value, __E>
            where
                __E: _serde::de::Error,
            {
                match __value {
                    `#`(`#`bytesMapping)*
                    _ => {
                        `#`bytesToStr
                        `#`valueAsBorrowedBytesContent
                        `#`fallthroughBorrowedArm
                    }
                }
            }
        """)
    } else {
        null
    }

    return Fragment.Block(quote("""
        fn expecting(&self, __formatter: &mut _serde.`#`Private::Formatter) -> _serde.`#`Private::fmt::Result {
            _serde.`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
        }

        `#`visitOther

        fn visit_str<__E>(self, __value: &str) -> _serde.`#`Private::Result<Self::Value, __E>
        where
            __E: _serde::de::Error,
        {
            match __value {
                `#`(`#`strMapping)*
                _ => {
                    `#`valueAsStrContent
                    `#`fallthroughArm
                }
            }
        }

        fn visit_bytes<__E>(self, __value: &[u8]) -> _serde.`#`Private::Result<Self::Value, __E>
        where
            __E: _serde::de::Error,
        {
            match __value {
                `#`(`#`bytesMapping)*
                _ => {
                    `#`bytesToStr
                    `#`valueAsBytesContent
                    `#`fallthroughArm
                }
            }
        }

        `#`visitBorrowed
    """))
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)