package io.github.kotlinmania.serderive

//! Deserialization of struct field identifiers and enum variant identifiers by
//! way of a Rust enum.

import io.github.kotlinmania.serderive.de.{FieldWithAliases, Parameters}
import io.github.kotlinmania.serderive.fragment.{Fragment, Stmts}
import io.github.kotlinmania.serderive.internals.ast.{Style, Variant}
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.procmacro2.{Literal, TokenStream}
import io.github.kotlinmania.quote.{quote, ToTokens}

// Generates `Deserialize.deserialize` body for an enum with
// `serde(field_identifier)` or `serde(variant_identifier)` attribute.
pub(super) fun deserialize_custom(
    params: Parameters,
    variants: &[Variant],
    cattrs: attr.Container,
) : Fragment {
    val is_variant = when cattrs.identifier() {
        attr.Identifier.Variant => true,
        attr.Identifier.Field => false,
        attr.Identifier.No => unreachable!(),
    };

    val this_type = params.this_type.to_token_stream();
    val this_value = params.this_value.to_token_stream();

    let (ordinary, fallthrough, fallthrough_borrowed) = if val Some(last) = variants.last() {
        val last_ident = last.ident;
        if last.attrs.other() {
            // Process `serde(other)` attribute. It would always be found on the
            // last variant (checked in `check_identifier`), so all preceding
            // are ordinary variants.
            val ordinary = variants[..variants.len() - 1];
            val fallthrough = quote!(_serde.#private.Ok(#this_value.#last_ident));
            (ordinary, Some(fallthrough), None)
        } else if val Style.Newtype = last.style {
            val ordinary = variants[..variants.len() - 1];
            val fallthrough = |value| {
                quote {
                    _serde.#private.Result.map(
                        _serde.Deserialize.deserialize(
                            _serde.#private.de.IdentifierDeserializer.from(#value)
                        ),
                        #this_value.#last_ident)
                }
            };
            (
                ordinary,
                Some(fallthrough(quote!(__value))),
                Some(fallthrough(quote!(_serde.#private.de.Borrowed(
                    __value
                )))),
            )
        } else {
            (variants, None, None)
        }
    } else {
        (variants, None, None)
    };

    val idents_aliases: List<_> = ordinary
        .iter()
        .map(|variant| FieldWithAliases {
            ident: variant.ident.clone(),
            aliases: variant.attrs.aliases(),
        })
        .collect();

    val names = idents_aliases.iter().flat_map(|variant| variant.aliases);

    val names_const = if fallthrough.is_some() {
        None
    } else if is_variant {
        val variants = quote {
            `#`[doc(hidden)]
            const VARIANTS: &'static [&'static str] = &[ #(#names),* ];
        };
        Some(variants)
    } else {
        val fields = quote {
            `#`[doc(hidden)]
            const FIELDS: &'static [&'static str] = &[ #(#names),* ];
        };
        Some(fields)
    };

    let (de_impl_generics, de_ty_generics, ty_generics, where_clause) =
        params.generics_with_de_lifetime();
    val delife = params.borrowed.de_lifetime();
    val visitor_impl = Stmts(deserialize_identifier(
        this_value,
        idents_aliases,
        is_variant,
        fallthrough,
        fallthrough_borrowed,
        false,
        cattrs.expecting(),
    ));

    quote_block! {
        #names_const

        `#`[doc(hidden)]
        struct __FieldVisitor #de_impl_generics #where_clause {
            marker: _serde.#private.PhantomData<#this_type #ty_generics>,
            lifetime: _serde.#private.PhantomData<&#delife ()>,
        }

        `#`[automatically_derived]
        impl #de_impl_generics _serde.de.Visitor<#delife> for __FieldVisitor #de_ty_generics #where_clause {
            type Value = #this_type #ty_generics;

            #visitor_impl
        }

        val __visitor = __FieldVisitor {
            marker: _serde.#private.PhantomData.<#this_type #ty_generics>,
            lifetime: _serde.#private.PhantomData,
        };
        _serde.Deserializer.deserialize_identifier(__deserializer, __visitor)
    }
}

pub(super) fun deserialize_generated(
    deserialized_fields: &[FieldWithAliases],
    has_flatten: bool,
    is_variant: bool,
    ignore_variant: TokenStream?,
    fallthrough: TokenStream?,
) : Fragment {
    val this_value = quote!(__Field);
    val field_idents: List<_> = deserialized_fields
        .iter()
        .map(|field| field.ident)
        .collect();

    val visitor_impl = Stmts(deserialize_identifier(
        this_value,
        deserialized_fields,
        is_variant,
        fallthrough,
        None,
        !is_variant && has_flatten,
        None,
    ));

    val lifetime = if !is_variant && has_flatten {
        Some(quote!(<'de>))
    } else {
        None
    };

    quote_block! {
        `#`[allow(non_camel_case_types)]
        `#`[doc(hidden)]
        enum __Field #lifetime {
            #(#field_idents,)*
            #ignore_variant
        }

        `#`[doc(hidden)]
        struct __FieldVisitor;

        `#`[automatically_derived]
        impl<'de> _serde.de.Visitor<'de> for __FieldVisitor {
            type Value = __Field #lifetime;

            #visitor_impl
        }

        `#`[automatically_derived]
        impl<'de> _serde.Deserialize<'de> for __Field #lifetime {
            `#`[inline]
            fun deserialize<__D>(__deserializer: __D) -> _serde.#private.Result<Self, __D.Error>
            where
                __D: _serde.Deserializer<'de>,
            {
                _serde.Deserializer.deserialize_identifier(__deserializer, __FieldVisitor)
            }
        }
    }
}

fun deserialize_identifier(
    this_value: TokenStream,
    deserialized_fields: &[FieldWithAliases],
    is_variant: bool,
    fallthrough: TokenStream?,
    fallthrough_borrowed: TokenStream?,
    collect_other_fields: bool,
    expecting: str?,
) : Fragment {
    val str_mapping = deserialized_fields.iter().map(|field| {
        val ident = field.ident;
        val aliases = field.aliases;
        val private2 = private;
        // `aliases` also contains a main name
        quote {
            #(
                #aliases => _serde.#private2.Ok(#this_value.#ident),
            )*
        }
    });
    val bytes_mapping = deserialized_fields.iter().map(|field| {
        val ident = field.ident;
        // `aliases` also contains a main name
        val aliases = field
            .aliases
            .iter()
            .map(|alias| Literal.byte_string(alias.value.as_bytes()));
        val private2 = private;
        quote {
            #(
                #aliases => _serde.#private2.Ok(#this_value.#ident),
            )*
        }
    });

    val expecting = expecting.unwrap_or(if is_variant {
        "variant identifier"
    } else {
        "field identifier"
    });

    val bytes_to_str = if fallthrough.is_some() || collect_other_fields {
        None
    } else {
        Some(quote {
            val __value = _serde.#private.from_utf8_lossy(__value);
        })
    };

    let (
        value_as_str_content,
        value_as_borrowed_str_content,
        value_as_bytes_content,
        value_as_borrowed_bytes_content,
    ) = if collect_other_fields {
        (
            Some(quote {
                val __value = _serde.#private.de.Content.String(_serde.#private.ToString.to_string(__value));
            }),
            Some(quote {
                val __value = _serde.#private.de.Content.Str(__value);
            }),
            Some(quote {
                val __value = _serde.#private.de.Content.ByteBuf(__value.to_vec());
            }),
            Some(quote {
                val __value = _serde.#private.de.Content.Bytes(__value);
            }),
        )
    } else {
        (None, None, None, None)
    };

    val fallthrough_arm_tokens;
    val fallthrough_arm = if val Some(fallthrough) = fallthrough {
        fallthrough
    } else if is_variant {
        fallthrough_arm_tokens = quote {
            _serde.#private.Err(_serde.de.Error.unknown_variant(__value, VARIANTS))
        };
        fallthrough_arm_tokens
    } else {
        fallthrough_arm_tokens = quote {
            _serde.#private.Err(_serde.de.Error.unknown_field(__value, FIELDS))
        };
        fallthrough_arm_tokens
    };

    val visit_other = if collect_other_fields {
        quote {
            fun visit_bool<__E>(self, __value: bool) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.Bool(__value)))
            }

            fun visit_i8<__E>(self, __value: i8) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.I8(__value)))
            }

            fun visit_i16<__E>(self, __value: i16) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.I16(__value)))
            }

            fun visit_i32<__E>(self, __value: i32) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.I32(__value)))
            }

            fun visit_i64<__E>(self, __value: i64) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.I64(__value)))
            }

            fun visit_u8<__E>(self, __value: u8) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.U8(__value)))
            }

            fun visit_u16<__E>(self, __value: u16) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.U16(__value)))
            }

            fun visit_u32<__E>(self, __value: u32) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.U32(__value)))
            }

            fun visit_u64<__E>(self, __value: u64) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.U64(__value)))
            }

            fun visit_f32<__E>(self, __value: f32) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.F32(__value)))
            }

            fun visit_f64<__E>(self, __value: f64) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.F64(__value)))
            }

            fun visit_char<__E>(self, __value: char) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.Char(__value)))
            }

            fun visit_unit<__E>(self) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(__Field.__other(_serde.#private.de.Content.Unit))
            }
        }
    } else {
        val u64_mapping = deserialized_fields.iter().enumerate().map(|(i, field)| {
            val i = i as u64;
            val ident = field.ident;
            quote!(#i => _serde.#private.Ok(#this_value.#ident))
        });

        val u64_fallthrough_arm_tokens;
        val u64_fallthrough_arm = if val Some(fallthrough) = fallthrough {
            fallthrough
        } else {
            val index_expecting = if is_variant { "variant" } else { "field" };
            val fallthrough_msg = format!(
                "{} index 0 <= i < {}",
                index_expecting,
                deserialized_fields.len(),
            );
            u64_fallthrough_arm_tokens = quote {
                _serde.#private.Err(_serde.de.Error.invalid_value(
                    _serde.de.Unexpected.Unsigned(__value),
                    &#fallthrough_msg,
                ))
            };
            u64_fallthrough_arm_tokens
        };

        quote {
            fun visit_u64<__E>(self, __value: u64) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                when __value {
                    #(#u64_mapping,)*
                    _ => #u64_fallthrough_arm,
                }
            }
        }
    };

    val visit_borrowed = if fallthrough_borrowed.is_some() || collect_other_fields {
        val str_mapping = str_mapping.clone();
        val bytes_mapping = bytes_mapping.clone();
        val fallthrough_borrowed_arm = fallthrough_borrowed.as_ref().unwrap_or(fallthrough_arm);
        Some(quote {
            fun visit_borrowed_str<__E>(self, __value: &'de str) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                when __value {
                    #(#str_mapping)*
                    _ => {
                        #value_as_borrowed_str_content
                        #fallthrough_borrowed_arm
                    }
                }
            }

            fun visit_borrowed_bytes<__E>(self, __value: &'de [u8]) -> _serde.#private.Result<Self.Value, __E>
            where
                __E: _serde.de.Error,
            {
                when __value {
                    #(#bytes_mapping)*
                    _ => {
                        #bytes_to_str
                        #value_as_borrowed_bytes_content
                        #fallthrough_borrowed_arm
                    }
                }
            }
        })
    } else {
        None
    };

    quote_block! {
        fun expecting(self, __formatter: var _serde.#private.Formatter) : _serde.#private.fmt.Result {
            _serde.#private.Formatter.write_str(__formatter, #expecting)
        }

        #visit_other

        fun visit_str<__E>(self, __value: str) -> _serde.#private.Result<Self.Value, __E>
        where
            __E: _serde.de.Error,
        {
            when __value {
                #(#str_mapping)*
                _ => {
                    #value_as_str_content
                    #fallthrough_arm
                }
            }
        }

        fun visit_bytes<__E>(self, __value: &[u8]) -> _serde.#private.Result<Self.Value, __E>
        where
            __E: _serde.de.Error,
        {
            when __value {
                #(#bytes_mapping)*
                _ => {
                    #bytes_to_str
                    #value_as_bytes_content
                    #fallthrough_arm
                }
            }
        }

        #visit_borrowed
    }
}
