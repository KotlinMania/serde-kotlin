// port-lint: source de/enum_adjacently.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.serderive.checkedQuote
import io.github.kotlinmania.serderive.checkedQuoteSpanned
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
        checkedQuote(
            "__Field::`#`variantIndex => `#`block",
            "variantIndex" to variantIndex,
            "block" to block,
        )
    }

    val rustName = params.typeName()
    val expecting = "adjacently tagged enum $rustName"
    val expectingVal = cattrs.expecting() ?: expecting
    val typeName = cattrs.name().deserializeName()
    val denyUnknownFields = cattrs.denyUnknownFields()

    val fieldVisitorTy = if (denyUnknownFields) {
        checkedQuote("_serde::`#`Private::de::TagOrContentFieldVisitor", "Private" to Private)
    } else {
        checkedQuote("_serde::`#`Private::de::TagContentOtherFieldVisitor", "Private" to Private)
    }

    var missingContent = checkedQuote("""
        _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::missing_field(`#`content))
    """, "Private" to Private, "content" to content)
    var missingContentFallthrough = checkedQuote("")
    val missingContentArms = mutableListOf<TokenStream>()
    for ((i, variant) in variants.withIndex()) {
        if (variant.attrs.skipDeserializing()) continue
        val variantIndex = fieldI(i)
        val variantIdent = variant.ident

        val arm = when (variant.style) {
            Style.Unit -> checkedQuote(
                "_serde::`#`Private::Ok(`#`thisValue::`#`variantIdent)",
                "Private" to Private,
                "thisValue" to thisValue,
                "variantIdent" to variantIdent,
            )
            Style.Newtype -> {
                if (variant.attrs.deserializeWith() == null) {
                    val span = variant.original.span()
                    val func = checkedQuoteSpanned(
                        span,
                        "_serde::`#`Private::de::missing_field",
                        "Private" to Private,
                    )
                    checkedQuote(
                        "`#`func(`#`content).map(`#`thisValue::`#`variantIdent)",
                        "func" to func,
                        "content" to content,
                        "thisValue" to thisValue,
                        "variantIdent" to variantIdent,
                    )
                } else {
                    missingContentFallthrough = checkedQuote(
                        "_ => `#`missingContent",
                        "missingContent" to missingContent,
                    )
                    continue
                }
            }
            else -> {
                missingContentFallthrough = checkedQuote(
                    "_ => `#`missingContent",
                    "missingContent" to missingContent,
                )
                continue
            }
        }
        missingContentArms.add(
            checkedQuote(
                "__Field::`#`variantIndex => `#`arm,",
                "variantIndex" to variantIndex,
                "arm" to arm,
            ),
        )
    }

    if (missingContentArms.isNotEmpty()) {
        missingContent = checkedQuote("""
            match __field {
                `#`(`#`missingContentArms)*
                `#`missingContentFallthrough
            }
        """, mapOf(
            "missingContentArms" to missingContentArms,
            "missingContentFallthrough" to missingContentFallthrough,
        ))
    }

    val nextKey = checkedQuote("""
        _serde::de::MapAccess::next_key_seed(&mut __map, `#`fieldVisitorTy {
            tag: `#`tag,
            content: `#`content,
        })?
    """, mapOf(
        "fieldVisitorTy" to fieldVisitorTy,
        "tag" to tag,
        "content" to content,
    ))

    val variantFromMap = checkedQuote("""
        _serde::de::MapAccess::next_value_seed(&mut __map, _serde::`#`Private::de::AdjacentlyTaggedEnumVariantSeed::<__Field> {
            enum_name: `#`rustName,
            variants: VARIANTS,
            fields_enum: _serde::`#`Private::PhantomData
        })?
    """, mapOf(
        "Private" to Private,
        "rustName" to rustName,
    ))

    val nextRelevantKey = if (denyUnknownFields) {
        nextKey
    } else {
        checkedQuote("""
            {
                let mut __rk : _serde::`#`Private::Option<_serde::`#`Private::de::TagOrContentField> = _serde::`#`Private::None;
                while let _serde::`#`Private::Some(__k) = `#`nextKey {
                    match __k {
                        _serde::`#`Private::de::TagContentOtherField::Other => {
                            let _ = _serde::de::MapAccess::next_value::<_serde::de::IgnoredAny>(&mut __map)?;
                            continue;
                        },
                        _serde::`#`Private::de::TagContentOtherField::Tag => {
                            __rk = _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Tag);
                            break;
                        }
                        _serde::`#`Private::de::TagContentOtherField::Content => {
                            __rk = _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Content);
                            break;
                        }
                    }
                }
                __rk
            }
        """, mapOf(
            "Private" to Private,
            "nextKey" to nextKey,
        ))
    }

    val visitRemainingKeys = checkedQuote("""
        match `#`nextRelevantKey {
            _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Tag) => {
                _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`tag))
            }
            _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Content) => {
                _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`content))
            }
            _serde::`#`Private::None => _serde::`#`Private::Ok(__ret),
        }
    """, mapOf(
        "nextRelevantKey" to nextRelevantKey,
        "Private" to Private,
        "tag" to tag,
        "content" to content,
    ))

    val finishContentThenTag = if (variantArms.isEmpty()) {
        checkedQuote("match `#`variantFromMap {}", "variantFromMap" to variantFromMap)
    } else {
        checkedQuote("""
            let __seed = __Seed {
                variant: `#`variantFromMap,
                marker: _serde::`#`Private::PhantomData,
                lifetime: _serde::`#`Private::PhantomData,
            };
            let __deserializer = _serde::`#`Private::de::ContentDeserializer::<__A::Error>::new(__content);
            let __ret = _serde::de::DeserializeSeed::deserialize(__seed, __deserializer)?;
            `#`visitRemainingKeys
        """, mapOf(
            "variantFromMap" to variantFromMap,
            "Private" to Private,
            "visitRemainingKeys" to visitRemainingKeys,
        ))
    }

    return Fragment.Block(checkedQuote("""
        `#`variantVisitor

        `#`variantsStmt

        `#`[doc(hidden)]
        struct __Seed `#`deImplGenerics `#`whereClause {
            variant: __Field,
            marker: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::DeserializeSeed<`#`delife> for __Seed `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn deserialize<__D>(self, __deserializer: __D) -> _serde::`#`Private::Result<Self::Value, __D::Error>
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
            marker: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn expecting(&self, __formatter: &mut _serde::`#`Private::Formatter) -> _serde::`#`Private::fmt::Result {
                _serde::`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
            }

            fn visit_map<__A>(self, mut __map: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::MapAccess<`#`delife>,
            {
                match `#`nextRelevantKey {
                    _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Tag) => {
                        let __field = `#`variantFromMap;
                        match `#`nextRelevantKey {
                            _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Tag) => {
                                _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`tag))
                            }
                            _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Content) => {
                                let __ret = _serde::de::MapAccess::next_value_seed(&mut __map,
                                    __Seed {
                                        variant: __field,
                                        marker: _serde::`#`Private::PhantomData,
                                        lifetime: _serde::`#`Private::PhantomData,
                                    })?;
                                `#`visitRemainingKeys
                            }
                            _serde::`#`Private::None => `#`missingContent
                        }
                    }
                    _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Content) => {
                        let __content = _serde::de::MapAccess::next_value_seed(&mut __map, _serde::`#`Private::de::ContentVisitor::new())?;
                        match `#`nextRelevantKey {
                            _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Tag) => {
                                `#`finishContentThenTag
                            }
                            _serde::`#`Private::Some(_serde::`#`Private::de::TagOrContentField::Content) => {
                                _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`content))
                            }
                            _serde::`#`Private::None => {
                                _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::missing_field(`#`tag))
                            }
                        }
                    }
                    _serde::`#`Private::None => {
                        _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::missing_field(`#`tag))
                    }
                }
            }

            fn visit_seq<__A>(self, mut __seq: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::SeqAccess<`#`delife>,
            {
                match _serde::de::SeqAccess::next_element(&mut __seq) {
                    _serde::`#`Private::Ok(_serde::`#`Private::Some(__variant)) => {
                        match _serde::de::SeqAccess::next_element_seed(
                            &mut __seq,
                            __Seed {
                                variant: __variant,
                                marker: _serde::`#`Private::PhantomData,
                                lifetime: _serde::`#`Private::PhantomData,
                            },
                        ) {
                            _serde::`#`Private::Ok(_serde::`#`Private::Some(__ret)) => _serde::`#`Private::Ok(__ret),
                            _serde::`#`Private::Ok(_serde::`#`Private::None) => {
                                _serde::`#`Private::Err(_serde::de::Error::invalid_length(1, &self))
                            }
                            _serde::`#`Private::Err(__err) => _serde::`#`Private::Err(__err),
                        }
                    }
                    _serde::`#`Private::Ok(_serde::`#`Private::None) => {
                        _serde::`#`Private::Err(_serde::de::Error::invalid_length(0, &self))
                    }
                    _serde::`#`Private::Err(__err) => _serde::`#`Private::Err(__err),
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
                marker: _serde::`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
                lifetime: _serde::`#`Private::PhantomData,
            },
        )
    """, mapOf(
        "variantVisitor" to variantVisitor,
        "variantsStmt" to variantsStmt,
        "deImplGenerics" to deImplGenerics,
        "whereClause" to whereClause,
        "Private" to Private,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "delife" to delife,
        "deTyGenerics" to deTyGenerics,
        "variantArms" to variantArms,
        "expectingVal" to expectingVal,
        "nextRelevantKey" to nextRelevantKey,
        "variantFromMap" to variantFromMap,
        "tag" to tag,
        "visitRemainingKeys" to visitRemainingKeys,
        "missingContent" to missingContent,
        "finishContentThenTag" to finishContentThenTag,
        "content" to content,
        "typeName" to typeName,
    )))
}
