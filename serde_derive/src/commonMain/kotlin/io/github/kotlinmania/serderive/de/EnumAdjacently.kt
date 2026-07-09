// port-lint: source de/enum_adjacently.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.quote.quoteSpanned
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Match
import io.github.kotlinmania.serderive.internals.Style
import io.github.kotlinmania.serderive.internals.Variant
import io.github.kotlinmania.syn.span

// Generates the deserialize body for an enum with the tag and content attributes.
internal fun deserializeEnumAdjacently(
    params: Parameters,
    variants: List<Variant>,
    cattrs: AttrContainer,
    tag: String,
    content: String
): Fragment {
    val thisType = params.thisType
    val thisValue = params.thisValue
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()

    val (variantsStmt, variantVisitor) = prepareEnumVariantEnum(variants)

    val variantArms = variants.mapIndexedNotNull { i, variant ->
        if (variant.attrs.skipDeserializing()) return@mapIndexedNotNull null
        val variantIndex = fieldI(i)
        val block = Match(deserializeVariant(params, variant, cattrs))
        quote("__Field::`#`variantIndex => `#`block")
    }

    val rustName = params.typeName()
    val expecting = "adjacently tagged enum $rustName"
    val expectingVal = cattrs.expecting() ?: expecting
    val typeName = cattrs.name().deserializeName()
    val denyUnknownFields = cattrs.denyUnknownFields()

    val fieldVisitorTy = if (denyUnknownFields) {
        quote("_serde.`#`Private::de::TagOrContentFieldVisitor")
    } else {
        quote("_serde.`#`Private::de::TagContentOtherFieldVisitor")
    }

    var missingContent = quote("""
        _serde.`#`Private::Err(<__A::Error as _serde::de::Error>::missing_field(`#`content))
    """)
    var missingContentFallthrough = quote("")
    val missingContentArms = mutableListOf<TokenStream>()
    for ((i, variant) in variants.withIndex()) {
        if (variant.attrs.skipDeserializing()) continue
        val variantIndex = fieldI(i)
        val variantIdent = variant.ident

        val arm = when (variant.style) {
            Style.Unit -> quote("_serde.`#`Private::Ok(`#`thisValue::`#`variantIdent)")
            Style.Newtype -> {
                if (variant.attrs.deserializeWith() == null) {
                    val span = variant.original.span()
                    val func = quoteSpanned(span, "_serde.`#`Private::de::missing_field")
                    quote("`#`func(`#`content).map(`#`thisValue::`#`variantIdent)")
                } else {
                    missingContentFallthrough = quote("_ => `#`missingContent")
                    continue
                }
            }
            else -> {
                missingContentFallthrough = quote("_ => `#`missingContent")
                continue
            }
        }
        missingContentArms.add(quote("__Field::`#`variantIndex => `#`arm,"))
    }

    if (missingContentArms.isNotEmpty()) {
        missingContent = quote("""
            match __field {
                `#`(`#`missingContentArms)*
                `#`missingContentFallthrough
            }
        """)
    }

    val nextKey = quote("""
        _serde::de::MapAccess::next_key_seed(&mut __map, `#`fieldVisitorTy {
            tag: `#`tag,
            content: `#`content,
        })?
    """)

    val variantFromMap = quote("""
        _serde::de::MapAccess::next_value_seed(&mut __map, _serde.`#`Private::de::AdjacentlyTaggedEnumVariantSeed::<__Field> {
            enum_name: `#`rustName,
            variants: VARIANTS,
            fields_enum: _serde.`#`Private::PhantomData
        })?
    """)

    val nextRelevantKey = if (denyUnknownFields) {
        nextKey
    } else {
        quote("""
            {
                let mut __rk : _serde.`#`Private::Option<_serde.`#`Private::de::TagOrContentField> = _serde.`#`Private::None;
                while let _serde.`#`Private::Some(__k) = `#`nextKey {
                    match __k {
                        _serde.`#`Private::de::TagContentOtherField::Other => {
                            let _ = _serde::de::MapAccess::next_value::<_serde::de::IgnoredAny>(&mut __map)?;
                            continue;
                        },
                        _serde.`#`Private::de::TagContentOtherField::Tag => {
                            __rk = _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Tag);
                            break;
                        }
                        _serde.`#`Private::de::TagContentOtherField::Content => {
                            __rk = _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Content);
                            break;
                        }
                    }
                }
                __rk
            }
        """)
    }

    val visitRemainingKeys = quote("""
        match `#`nextRelevantKey {
            _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Tag) => {
                _serde.`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`tag))
            }
            _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Content) => {
                _serde.`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`content))
            }
            _serde.`#`Private::None => _serde.`#`Private::Ok(__ret),
        }
    """)

    val finishContentThenTag = if (variantArms.isEmpty()) {
        quote("match `#`variantFromMap {}")
    } else {
        quote("""
            let __seed = __Seed {
                variant: `#`variantFromMap,
                marker: _serde.`#`Private::PhantomData,
                lifetime: _serde.`#`Private::PhantomData,
            };
            let __deserializer = _serde.`#`Private::de::ContentDeserializer::<__A::Error>::new(__content);
            let __ret = _serde::de::DeserializeSeed::deserialize(__seed, __deserializer)?;
            `#`visitRemainingKeys
        """)
    }

    return Fragment.Block(quote("""
        `#`variantVisitor

        `#`variantsStmt

        `#`[doc(hidden)]
        struct __Seed `#`deImplGenerics `#`whereClause {
            variant: __Field,
            marker: _serde.`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde.`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::DeserializeSeed<`#`delife> for __Seed `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn deserialize<__D>(self, __deserializer: __D) -> _serde.`#`Private::Result<Self::Value, __D::Error>
            where
                __D: _serde::Deserializer<`#`delife>,
            {
                match self.variant {
                    `#`(`#`variantArms)*
                }
            }
        }

        `#`[doc(hidden)]
        struct __Visitor `#`deImplGenerics `#`whereClause {
            marker: _serde.`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde.`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn expecting(&self, __formatter: &mut _serde.`#`Private::Formatter) -> _serde.`#`Private::fmt::Result {
                _serde.`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
            }

            fn visit_map<__A>(self, mut __map: __A) -> _serde.`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::MapAccess<`#`delife>,
            {
                match `#`nextRelevantKey {
                    _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Tag) => {
                        let __field = `#`variantFromMap;
                        match `#`nextRelevantKey {
                            _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Tag) => {
                                _serde.`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`tag))
                            }
                            _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Content) => {
                                let __ret = _serde::de::MapAccess::next_value_seed(&mut __map,
                                    __Seed {
                                        variant: __field,
                                        marker: _serde.`#`Private::PhantomData,
                                        lifetime: _serde.`#`Private::PhantomData,
                                    })?;
                                `#`visitRemainingKeys
                            }
                            _serde.`#`Private::None => `#`missingContent
                        }
                    }
                    _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Content) => {
                        let __content = _serde::de::MapAccess::next_value_seed(&mut __map, _serde.`#`Private::de::ContentVisitor::new())?;
                        match `#`nextRelevantKey {
                            _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Tag) => {
                                `#`finishContentThenTag
                            }
                            _serde.`#`Private::Some(_serde.`#`Private::de::TagOrContentField::Content) => {
                                _serde.`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`content))
                            }
                            _serde.`#`Private::None => {
                                _serde.`#`Private::Err(<__A::Error as _serde::de::Error>::missing_field(`#`tag))
                            }
                        }
                    }
                    _serde.`#`Private::None => {
                        _serde.`#`Private::Err(<__A::Error as _serde::de::Error>::missing_field(`#`tag))
                    }
                }
            }

            fn visit_seq<__A>(self, mut __seq: __A) -> _serde.`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::SeqAccess<`#`delife>,
            {
                match _serde::de::SeqAccess::next_element(&mut __seq) {
                    _serde.`#`Private::Ok(_serde.`#`Private::Some(__variant)) => {
                        match _serde::de::SeqAccess::next_element_seed(
                            &mut __seq,
                            __Seed {
                                variant: __variant,
                                marker: _serde.`#`Private::PhantomData,
                                lifetime: _serde.`#`Private::PhantomData,
                            },
                        ) {
                            _serde.`#`Private::Ok(_serde.`#`Private::Some(__ret)) => _serde.`#`Private::Ok(__ret),
                            _serde.`#`Private::Ok(_serde.`#`Private::None) => {
                                _serde.`#`Private::Err(_serde::de::Error::invalid_length(1, &self))
                            }
                            _serde.`#`Private::Err(__err) => _serde.`#`Private::Err(__err),
                        }
                    }
                    _serde.`#`Private::Ok(_serde.`#`Private::None) => {
                        _serde.`#`Private::Err(_serde::de::Error::invalid_length(0, &self))
                    }
                    _serde.`#`Private::Err(__err) => _serde.`#`Private::Err(__err),
                }
            }
        }

        `#`[doc(hidden)]
        const FIELDS: &'static [&'static str] = &[`#`tag, `#`content];
        _serde::Deserializer::deserialize_struct(
            __deserializer,
            `#`typeName,
            FIELDS,
            __Visitor {
                marker: _serde.`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
                lifetime: _serde.`#`Private::PhantomData,
            },
        )
    """))
}