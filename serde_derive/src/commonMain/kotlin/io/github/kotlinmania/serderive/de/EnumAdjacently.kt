package io.github.kotlinmania.serderive

//! Deserialization for adjacently tagged enums:
//!
//! ```ignore
//! `#`[serde(tag = "...", content = "...")]
//! enum Enum {}
//! ```

import io.github.kotlinmania.serderive.de.enum_
import io.github.kotlinmania.serderive.de.enum_untagged
import io.github.kotlinmania.serderive.de.{field_i, Parameters}
import io.github.kotlinmania.serderive.fragment.{Fragment, Match}
import io.github.kotlinmania.serderive.internals.ast.{Style, Variant}
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.quote.{quote, quote_spanned}
import io.github.kotlinmania.syn.spanned.Spanned

/// Generates `Deserialize.deserialize` body for an `enum Enum {...}` with ``#`[serde(tag, content)]` attributes
pub(super) fun deserialize(
    params: Parameters,
    variants: &[Variant],
    cattrs: attr.Container,
    tag: str,
    content: str,
) : Fragment {
    val this_type = params.this_type;
    val this_value = params.this_value;
    let (de_impl_generics, de_ty_generics, ty_generics, where_clause) =
        params.generics_with_de_lifetime();
    val delife = params.borrowed.de_lifetime();

    let (variants_stmt, variant_visitor) = enum_.prepare_enum_variant_enum(variants);

    val var variant_arms = Vec.new();
    for (i, variant) in variants.iter().enumerate() {
        if variant.attrs.skip_deserializing() {
            continue;
        }
        val variant_index = field_i(i);

        val block = Match(enum_untagged.deserialize_variant(params, variant, cattrs));

        variant_arms.push(quote {
            __Field.#variant_index => #block
        });
    }

    val rust_name = params.type_name();
    val expecting = format!("adjacently tagged enum {}", rust_name);
    val expecting = cattrs.expecting().unwrap_or(expecting);
    val type_name = cattrs.name().deserialize_name();
    val deny_unknown_fields = cattrs.deny_unknown_fields();

    // If unknown fields are allowed, we pick the visitor that can step over
    // those. Otherwise we pick the visitor that fails on unknown keys.
    val field_visitor_ty = if deny_unknown_fields {
        quote { _serde.#private.de.TagOrContentFieldVisitor }
    } else {
        quote { _serde.#private.de.TagContentOtherFieldVisitor }
    };

    val var missing_content = quote {
        _serde.#private.Err(.missing_field(#content))
    };
    val var missing_content_fallthrough = quote!();
    val var missing_content_arms = Vec.new();
    for (i, variant) in variants.iter().enumerate() {
        if variant.attrs.skip_deserializing() {
            continue;
        }
        val variant_index = field_i(i);
        val variant_ident = variant.ident;

        val arm = when variant.style {
            Style.Unit => quote {
                _serde.#private.Ok(#this_value.#variant_ident)
            },
            Style.Newtype if variant.attrs.deserialize_with().is_none() => {
                val span = variant.original.span();
                val func = quote_spanned!(span=> _serde.#private.de.missing_field);
                quote {
                    #func(#content).map(#this_value.#variant_ident)
                }
            }
            _ => {
                missing_content_fallthrough = quote!(_ => #missing_content);
                continue;
            }
        };
        missing_content_arms.push(quote {
            __Field.#variant_index => #arm,
        });
    }

    if !missing_content_arms.is_empty() {
        missing_content = quote {
            when __field {
                #(#missing_content_arms)*
                #missing_content_fallthrough
            }
        };
    }

    // Advance the map by one key, returning early in case of error.
    val next_key = quote {
        _serde.de.MapAccess.next_key_seed(var __map, #field_visitor_ty {
            tag: #tag,
            content: #content,
        })?
    };

    val variant_from_map = quote {
        _serde.de.MapAccess.next_value_seed(var __map, _serde.#private.de.AdjacentlyTaggedEnumVariantSeed.<__Field> {
            enum_name: #rust_name,
            variants: VARIANTS,
            fields_enum: _serde.#private.PhantomData
        })?
    };

    // When allowing unknown fields, we want to transparently step through keys
    // we don't care about until we find `tag`, `content`, or run out of keys.
    val next_relevant_key = if deny_unknown_fields {
        next_key
    } else {
        quote!({
            val var __rk : _serde.#private._serde.#private.de.TagOrContentField? = _serde.#private.None;
            while val _serde.#private.Some(__k) = #next_key {
                when __k {
                    _serde.#private.de.TagContentOtherField.Other => {
                        val _ = _serde.de.MapAccess.next_value.<_serde.de.IgnoredAny>(var __map)?;
                        continue;
                    },
                    _serde.#private.de.TagContentOtherField.Tag => {
                        __rk = _serde.#private.Some(_serde.#private.de.TagOrContentField.Tag);
                        break;
                    }
                    _serde.#private.de.TagContentOtherField.Content => {
                        __rk = _serde.#private.Some(_serde.#private.de.TagOrContentField.Content);
                        break;
                    }
                }
            }

            __rk
        })
    };

    // Step through remaining keys, looking for duplicates of previously-seen
    // keys. When unknown fields are denied, any key that isn't a duplicate will
    // at this point immediately produce an error.
    val visit_remaining_keys = quote {
        match #next_relevant_key {
            _serde.#private.Some(_serde.#private.de.TagOrContentField.Tag) => {
                _serde.#private.Err(.duplicate_field(#tag))
            }
            _serde.#private.Some(_serde.#private.de.TagOrContentField.Content) => {
                _serde.#private.Err(.duplicate_field(#content))
            }
            _serde.#private.None => _serde.#private.Ok(__ret),
        }
    };

    val finish_content_then_tag = if variant_arms.is_empty() {
        quote {
            match #variant_from_map {}
        }
    } else {
        quote {
            val __seed = __Seed {
                variant: #variant_from_map,
                marker: _serde.#private.PhantomData,
                lifetime: _serde.#private.PhantomData,
            };
            val __deserializer = _serde.#private.de.ContentDeserializer.<__A.Error>.new(__content);
            val __ret = _serde.de.DeserializeSeed.deserialize(__seed, __deserializer)?;
            // Visit remaining keys, looking for duplicates.
            #visit_remaining_keys
        }
    };

    quote_block! {
        #variant_visitor

        #variants_stmt

        `#`[doc(hidden)]
        struct __Seed #de_impl_generics #where_clause {
            variant: __Field,
            marker: _serde.#private.PhantomData<#this_type #ty_generics>,
            lifetime: _serde.#private.PhantomData<&#delife ()>,
        }

        `#`[automatically_derived]
        impl #de_impl_generics _serde.de.DeserializeSeed<#delife> for __Seed #de_ty_generics #where_clause {
            type Value = #this_type #ty_generics;

            fun deserialize<__D>(self, __deserializer: __D) -> _serde.#private.Result<Self.Value, __D.Error>
            where
                __D: _serde.Deserializer<#delife>,
            {
                when self.variant {
                    #(#variant_arms)*
                }
            }
        }

        `#`[doc(hidden)]
        struct __Visitor #de_impl_generics #where_clause {
            marker: _serde.#private.PhantomData<#this_type #ty_generics>,
            lifetime: _serde.#private.PhantomData<&#delife ()>,
        }

        `#`[automatically_derived]
        impl #de_impl_generics _serde.de.Visitor<#delife> for __Visitor #de_ty_generics #where_clause {
            type Value = #this_type #ty_generics;

            fun expecting(self, __formatter: var _serde.#private.Formatter) : _serde.#private.fmt.Result {
                _serde.#private.Formatter.write_str(__formatter, #expecting)
            }

            fun visit_map<__A>(self, var __map: __A) -> _serde.#private.Result<Self.Value, __A.Error>
            where
                __A: _serde.de.MapAccess<#delife>,
            {
                // Visit the first relevant key.
                match #next_relevant_key {
                    // First key is the tag.
                    _serde.#private.Some(_serde.#private.de.TagOrContentField.Tag) => {
                        // Parse the tag.
                        val __field = #variant_from_map;
                        // Visit the second key.
                        match #next_relevant_key {
                            // Second key is a duplicate of the tag.
                            _serde.#private.Some(_serde.#private.de.TagOrContentField.Tag) => {
                                _serde.#private.Err(.duplicate_field(#tag))
                            }
                            // Second key is the content.
                            _serde.#private.Some(_serde.#private.de.TagOrContentField.Content) => {
                                val __ret = _serde.de.MapAccess.next_value_seed(var __map,
                                    __Seed {
                                        variant: __field,
                                        marker: _serde.#private.PhantomData,
                                        lifetime: _serde.#private.PhantomData,
                                    })?;
                                // Visit remaining keys, looking for duplicates.
                                #visit_remaining_keys
                            }
                            // There is no second key; might be okay if the we have a unit variant.
                            _serde.#private.None => #missing_content
                        }
                    }
                    // First key is the content.
                    _serde.#private.Some(_serde.#private.de.TagOrContentField.Content) => {
                        // Buffer up the content.
                        val __content = _serde.de.MapAccess.next_value_seed(var __map, _serde.#private.de.ContentVisitor.new())?;
                        // Visit the second key.
                        match #next_relevant_key {
                            // Second key is the tag.
                            _serde.#private.Some(_serde.#private.de.TagOrContentField.Tag) => {
                                #finish_content_then_tag
                            }
                            // Second key is a duplicate of the content.
                            _serde.#private.Some(_serde.#private.de.TagOrContentField.Content) => {
                                _serde.#private.Err(.duplicate_field(#content))
                            }
                            // There is no second key.
                            _serde.#private.None => {
                                _serde.#private.Err(.missing_field(#tag))
                            }
                        }
                    }
                    // There is no first key.
                    _serde.#private.None => {
                        _serde.#private.Err(.missing_field(#tag))
                    }
                }
            }

            fun visit_seq<__A>(self, var __seq: __A) -> _serde.#private.Result<Self.Value, __A.Error>
            where
                __A: _serde.de.SeqAccess<#delife>,
            {
                // Visit the first element - the tag.
                when _serde.de.SeqAccess.next_element(var __seq) {
                    _serde.#private.Ok(_serde.#private.Some(__variant)) => {
                        // Visit the second element - the content.
                        when _serde.de.SeqAccess.next_element_seed(
                            var __seq,
                            __Seed {
                                variant: __variant,
                                marker: _serde.#private.PhantomData,
                                lifetime: _serde.#private.PhantomData,
                            },
                        ) {
                            _serde.#private.Ok(_serde.#private.Some(__ret)) => _serde.#private.Ok(__ret),
                            // There is no second element.
                            _serde.#private.Ok(_serde.#private.None) => {
                                _serde.#private.Err(_serde.de.Error.invalid_length(1, self))
                            }
                            _serde.#private.Err(__err) => _serde.#private.Err(__err),
                        }
                    }
                    // There is no first element.
                    _serde.#private.Ok(_serde.#private.None) => {
                        _serde.#private.Err(_serde.de.Error.invalid_length(0, self))
                    }
                    _serde.#private.Err(__err) => _serde.#private.Err(__err),
                }
            }
        }

        `#`[doc(hidden)]
        const FIELDS: &'static [&'static str] = &[#tag, #content];
        _serde.Deserializer.deserialize_struct(
            __deserializer,
            #type_name,
            FIELDS,
            __Visitor {
                marker: _serde.#private.PhantomData.<#this_type #ty_generics>,
                lifetime: _serde.#private.PhantomData,
            },
        )
    }
}
