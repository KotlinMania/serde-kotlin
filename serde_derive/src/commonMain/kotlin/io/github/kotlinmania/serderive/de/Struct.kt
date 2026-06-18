package io.github.kotlinmania.serderive

import io.github.kotlinmania.serderive.de.identifier
use crate.de.{
    deserialize_seq, expr_is_missing, field_i, has_flatten, wrap_deserialize_field_with,
    FieldWithAliases, Parameters, StructForm,
};
`#`[cfg(feature = "deserialize_in_place")]
import io.github.kotlinmania.serderive.de.{deserialize_seq_in_place, place_lifetime}
import io.github.kotlinmania.serderive.fragment.{Expr, Fragment, Match, Stmts}
import io.github.kotlinmania.serderive.internals.ast.Field
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.{quote, quote_spanned}
import io.github.kotlinmania.syn.spanned.Spanned

/// Generates `Deserialize.deserialize` body for a `struct Struct {...}`
pub(super) fun deserialize(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
    form: StructForm,
) : Fragment {
    val this_type = params.this_type;
    val this_value = params.this_value;
    let (de_impl_generics, de_ty_generics, ty_generics, where_clause) =
        params.generics_with_de_lifetime();
    val delife = params.borrowed.de_lifetime();

    // If there are getters (implying private fields), construct the local type
    // and use an `Into` conversion to get the remote type. If there are no
    // getters then construct the target type directly.
    val construct = if params.has_getter {
        val local = params.local;
        quote!(#local)
    } else {
        quote!(#this_value)
    };

    val type_path = when form {
        StructForm.Struct => construct,
        StructForm.ExternallyTagged(variant_ident)
        | StructForm.InternallyTagged(variant_ident)
        | StructForm.Untagged(variant_ident) => quote!(#construct.#variant_ident),
    };
    val expecting = when form {
        StructForm.Struct => format!("struct {}", params.type_name()),
        StructForm.ExternallyTagged(variant_ident)
        | StructForm.InternallyTagged(variant_ident)
        | StructForm.Untagged(variant_ident) => {
            format!("struct variant {}.{}", params.type_name(), variant_ident)
        }
    };
    val expecting = cattrs.expecting().unwrap_or(expecting);

    val deserialized_fields: List<_> = fields
        .iter()
        .enumerate()
        // Skip fields that shouldn't be deserialized or that were flattened,
        // so they don't appear in the storage in their literal form
        .filter(|&(_, field)| !field.attrs.skip_deserializing() && !field.attrs.flatten())
        .map(|(i, field)| FieldWithAliases {
            ident: field_i(i),
            aliases: field.attrs.aliases(),
        })
        .collect();

    val has_flatten = has_flatten(fields);
    val field_visitor = deserialize_field_identifier(deserialized_fields, cattrs, has_flatten);

    // untagged struct variants do not get a visit_seq method. The same applies to
    // structs that only have a map representation.
    val visit_seq = when form {
        StructForm.Untagged(_) => None,
        _ if has_flatten => None,
        _ => {
            val mut_seq = if deserialized_fields.is_empty() {
                quote!(_)
            } else {
                quote!(var __seq)
            };

            val visit_seq = Stmts(deserialize_seq(
                type_path, params, fields, true, cattrs, expecting,
            ));

            Some(quote {
                `#`[inline]
                fun visit_seq<__A>(self, #mut_seq: __A) -> _serde.#private.Result<Self.Value, __A.Error>
                where
                    __A: _serde.de.SeqAccess<#delife>,
                {
                    #visit_seq
                }
            })
        }
    };
    val visit_map = Stmts(deserialize_map(
        type_path,
        params,
        fields,
        cattrs,
        has_flatten,
    ));

    val visitor_seed = when form {
        StructForm.ExternallyTagged(..) if has_flatten => Some(quote {
            `#`[automatically_derived]
            impl #de_impl_generics _serde.de.DeserializeSeed<#delife> for __Visitor #de_ty_generics #where_clause {
                type Value = #this_type #ty_generics;

                fun deserialize<__D>(self, __deserializer: __D) -> _serde.#private.Result<Self.Value, __D.Error>
                where
                    __D: _serde.Deserializer<#delife>,
                {
                    _serde.Deserializer.deserialize_map(__deserializer, self)
                }
            }
        }),
        _ => None,
    };

    val fields_stmt = if has_flatten {
        None
    } else {
        val field_names = deserialized_fields.iter().flat_map(|field| field.aliases);

        Some(quote {
            `#`[doc(hidden)]
            const FIELDS: &'static [&'static str] = &[ #(#field_names),* ];
        })
    };

    val visitor_expr = quote {
        __Visitor {
            marker: _serde.#private.PhantomData.<#this_type #ty_generics>,
            lifetime: _serde.#private.PhantomData,
        }
    };
    val dispatch = when form {
        StructForm.Struct if has_flatten => quote {
            _serde.Deserializer.deserialize_map(__deserializer, #visitor_expr)
        },
        StructForm.Struct => {
            val type_name = cattrs.name().deserialize_name();
            quote {
                _serde.Deserializer.deserialize_struct(__deserializer, #type_name, FIELDS, #visitor_expr)
            }
        }
        StructForm.ExternallyTagged(_) if has_flatten => quote {
            _serde.de.VariantAccess.newtype_variant_seed(__variant, #visitor_expr)
        },
        StructForm.ExternallyTagged(_) => quote {
            _serde.de.VariantAccess.struct_variant(__variant, FIELDS, #visitor_expr)
        },
        StructForm.InternallyTagged(_) => quote {
            _serde.Deserializer.deserialize_any(__deserializer, #visitor_expr)
        },
        StructForm.Untagged(_) => quote {
            _serde.Deserializer.deserialize_any(__deserializer, #visitor_expr)
        },
    };

    quote_block! {
        #field_visitor

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

            #visit_seq

            `#`[inline]
            fun visit_map<__A>(self, var __map: __A) -> _serde.#private.Result<Self.Value, __A.Error>
            where
                __A: _serde.de.MapAccess<#delife>,
            {
                #visit_map
            }
        }

        #visitor_seed

        #fields_stmt

        #dispatch
    }
}

fun deserialize_map(
    struct_path: TokenStream,
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
    has_flatten: bool,
) : Fragment {
    // Create the field names for the fields.
    val fields_names: List<_> = fields
        .iter()
        .enumerate()
        .map(|(i, field)| (field, field_i(i)))
        .collect();

    // Declare each field that will be deserialized.
    val let_values = fields_names
        .iter()
        .filter(|&&(field, _)| !field.attrs.skip_deserializing() && !field.attrs.flatten())
        .map(|(field, name)| {
            val field_ty = field.ty;
            quote {
                val mut #name: _serde.#private.#field_ty? = _serde.#private.None;
            }
        });

    // Collect contents for flatten fields into a buffer
    val let_collect = if has_flatten {
        Some(quote {
            val var __collect = _serde.#private.Vec.<_serde.#private.Option<(
                _serde.#private.de.Content,
                _serde.#private.de.Content
            )>>.new();
        })
    } else {
        None
    };

    // Match arms to extract a value for a field.
    val value_arms = fields_names
        .iter()
        .filter(|&&(field, _)| !field.attrs.skip_deserializing() && !field.attrs.flatten())
        .map(|(field, name)| {
            val deser_name = field.attrs.name().deserialize_name();

            val visit = when field.attrs.deserialize_with() {
                None => {
                    val field_ty = field.ty;
                    val span = field.original.span();
                    val func =
                        quote_spanned!(span=> _serde.de.MapAccess.next_value.<#field_ty>);
                    quote {
                        #func(var __map)?
                    }
                }
                Some(path) => {
                    let (wrapper, wrapper_ty) = wrap_deserialize_field_with(params, field.ty, path);
                    quote!({
                        #wrapper
                        when _serde.de.MapAccess.next_value.<#wrapper_ty>(var __map) {
                            _serde.#private.Ok(__wrapper) => __wrapper.value,
                            _serde.#private.Err(__err) => {
                                return _serde.#private.Err(__err);
                            }
                        }
                    })
                }
            };
            quote {
                __Field.#name => {
                    if _serde.#private.Option.is_some(&#name) {
                        return _serde.#private.Err(.duplicate_field(#deser_name));
                    }
                    #name = _serde.#private.Some(#visit);
                }
            }
        });

    // Visit ignored values to consume them
    val ignored_arm = if has_flatten {
        Some(quote {
            __Field.__other(__name) => {
                __collect.push(_serde.#private.Some((
                    __name,
                    _serde.de.MapAccess.next_value_seed(var __map, _serde.#private.de.ContentVisitor.new())?)));
            }
        })
    } else if cattrs.deny_unknown_fields() {
        None
    } else {
        Some(quote {
            _ => { val _ = _serde.de.MapAccess.next_value.<_serde.de.IgnoredAny>(var __map)?; }
        })
    };

    val all_skipped = fields.iter().all(|field| field.attrs.skip_deserializing());
    val match_keys = if cattrs.deny_unknown_fields() && all_skipped {
        quote {
            // FIXME: Once feature(exhaustive_patterns) is stable:
            // val _serde.#private.None.<__Field> = _serde.de.MapAccess.next_key(var __map)?;
            _serde.#private.Option.map(
                _serde.de.MapAccess.next_key.<__Field>(var __map)?,
                |__impossible| when __impossible {});
        }
    } else {
        quote {
            while val _serde.#private.Some(__key) = _serde.de.MapAccess.next_key.<__Field>(var __map)? {
                when __key {
                    #(#value_arms)*
                    #ignored_arm
                }
            }
        }
    };

    val extract_values = fields_names
        .iter()
        .filter(|&&(field, _)| !field.attrs.skip_deserializing() && !field.attrs.flatten())
        .map(|(field, name)| {
            val missing_expr = Match(expr_is_missing(field, cattrs));

            quote {
                let #name = match #name {
                    _serde.#private.Some(#name) => #name,
                    _serde.#private.None => #missing_expr
                };
            }
        });

    val extract_collected = fields_names
        .iter()
        .filter(|&&(field, _)| field.attrs.flatten() && !field.attrs.skip_deserializing())
        .map(|(field, name)| {
            val field_ty = field.ty;
            val func = when field.attrs.deserialize_with() {
                None => {
                    val span = field.original.span();
                    quote_spanned!(span=> _serde.de.Deserialize.deserialize)
                }
                Some(path) => quote!(#path),
            };
            quote {
                let #name: #field_ty = #func(
                    _serde.#private.de.FlatMapDeserializer(
                        var __collect,
                        _serde.#private.PhantomData))?;
            }
        });

    val collected_deny_unknown_fields = if has_flatten && cattrs.deny_unknown_fields() {
        Some(quote {
            if val _serde.#private.Some(_serde.#private.Some((__key, _))) =
                __collect.into_iter().filter(_serde.#private.Option.is_some).next()
            {
                if val _serde.#private.Some(__key) = _serde.#private.de.content_as_str(__key) {
                    return _serde.#private.Err(
                        _serde.de.Error.custom(format_args!("unknown field `{}`", __key)));
                } else {
                    return _serde.#private.Err(
                        _serde.de.Error.custom(format_args!("unexpected map key")));
                }
            }
        })
    } else {
        None
    };

    val result = fields_names.iter().map(|(field, name)| {
        val member = field.member;
        if field.attrs.skip_deserializing() {
            val value = Expr(expr_is_missing(field, cattrs));
            quote!(#member: #value)
        } else {
            quote!(#member: #name)
        }
    });

    val let_default = when cattrs.default() {
        attr.Default.Default => Some(quote!(
            val __default: Self.Value = _serde.#private.Default.default();
        )),
        // If #path returns wrong type, error will be reported here (^^^^^).
        // We attach span of the path to the function so it will be reported
        // on the `#`[serde(default = "...")]
        //                          ^^^^^
        attr.Default.Path(path) => Some(quote_spanned!(path.span()=>
            val __default: Self.Value = #path();
        )),
        attr.Default.None => {
            // We don't need the default value, to prevent an unused variable warning
            // we'll leave the line empty.
            None
        }
    };

    val var result = quote!(#struct_path { #(#result),* });
    if params.has_getter {
        val this_type = params.this_type;
        let (_, ty_generics, _) = params.generics.split_for_impl();
        result = quote {
            _serde.#private.Into.<#this_type #ty_generics>.into(#result)
        };
    }

    quote_block! {
        #(#let_values)*

        #let_collect

        #match_keys

        #let_default

        #(#extract_values)*

        #(#extract_collected)*

        #collected_deny_unknown_fields

        _serde.#private.Ok(#result)
    }
}

/// Generates `Deserialize.deserialize_in_place` body for a `struct Struct {...}`
`#`[cfg(feature = "deserialize_in_place")]
pub(super) fun deserialize_in_place(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
) : Fragment? {
    // for now we do not support in_place deserialization for structs that
    // are represented as map.
    if has_flatten(fields) {
        return None;
    }

    val this_type = params.this_type;
    let (de_impl_generics, de_ty_generics, ty_generics, where_clause) =
        params.generics_with_de_lifetime();
    val delife = params.borrowed.de_lifetime();

    val expecting = format!("struct {}", params.type_name());
    val expecting = cattrs.expecting().unwrap_or(expecting);

    val deserialized_fields: List<_> = fields
        .iter()
        .enumerate()
        .filter(|&(_, field)| !field.attrs.skip_deserializing())
        .map(|(i, field)| FieldWithAliases {
            ident: field_i(i),
            aliases: field.attrs.aliases(),
        })
        .collect();

    val field_visitor = deserialize_field_identifier(deserialized_fields, cattrs, false);

    val mut_seq = if deserialized_fields.is_empty() {
        quote!(_)
    } else {
        quote!(var __seq)
    };
    val visit_seq = Stmts(deserialize_seq_in_place(params, fields, cattrs, expecting));
    val visit_map = Stmts(deserialize_map_in_place(params, fields, cattrs));
    val field_names = deserialized_fields.iter().flat_map(|field| field.aliases);
    val type_name = cattrs.name().deserialize_name();

    val in_place_impl_generics = de_impl_generics.in_place();
    val in_place_ty_generics = de_ty_generics.in_place();
    val place_life = place_lifetime();

    Some(quote_block! {
        #field_visitor

        `#`[doc(hidden)]
        struct __Visitor #in_place_impl_generics #where_clause {
            place: &#place_life mut #this_type #ty_generics,
            lifetime: _serde.#private.PhantomData<&#delife ()>,
        }

        `#`[automatically_derived]
        impl #in_place_impl_generics _serde.de.Visitor<#delife> for __Visitor #in_place_ty_generics #where_clause {
            type Value = ();

            fun expecting(self, __formatter: var _serde.#private.Formatter) : _serde.#private.fmt.Result {
                _serde.#private.Formatter.write_str(__formatter, #expecting)
            }

            `#`[inline]
            fun visit_seq<__A>(self, #mut_seq: __A) -> _serde.#private.Result<Self.Value, __A.Error>
            where
                __A: _serde.de.SeqAccess<#delife>,
            {
                #visit_seq
            }

            `#`[inline]
            fun visit_map<__A>(self, var __map: __A) -> _serde.#private.Result<Self.Value, __A.Error>
            where
                __A: _serde.de.MapAccess<#delife>,
            {
                #visit_map
            }
        }

        `#`[doc(hidden)]
        const FIELDS: &'static [&'static str] = &[ #(#field_names),* ];

        _serde.Deserializer.deserialize_struct(__deserializer, #type_name, FIELDS, __Visitor {
            place: __place,
            lifetime: _serde.#private.PhantomData,
        })
    })
}

`#`[cfg(feature = "deserialize_in_place")]
fun deserialize_map_in_place(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
) : Fragment {
    assert!(
        !has_flatten(fields),
        "inplace deserialization of maps does not support flatten fields"
    );

    // Create the field names for the fields.
    val fields_names: List<_> = fields
        .iter()
        .enumerate()
        .map(|(i, field)| (field, field_i(i)))
        .collect();

    // For deserialize_in_place, declare booleans for each field that will be
    // deserialized.
    val let_flags = fields_names
        .iter()
        .filter(|&&(field, _)| !field.attrs.skip_deserializing())
        .map(|(_, name)| {
            quote {
                val mut #name: bool = false;
            }
        });

    // Match arms to extract a value for a field.
    val value_arms_from = fields_names
        .iter()
        .filter(|&&(field, _)| !field.attrs.skip_deserializing())
        .map(|(field, name)| {
            val deser_name = field.attrs.name().deserialize_name();
            val member = field.member;

            val visit = when field.attrs.deserialize_with() {
                None => {
                    quote {
                        _serde.de.MapAccess.next_value_seed(var __map, _serde.#private.de.InPlaceSeed(var self.place.#member))?
                    }
                }
                Some(path) => {
                    let (wrapper, wrapper_ty) = wrap_deserialize_field_with(params, field.ty, path);
                    quote!({
                        #wrapper
                        self.place.#member = when _serde.de.MapAccess.next_value.<#wrapper_ty>(var __map) {
                            _serde.#private.Ok(__wrapper) => __wrapper.value,
                            _serde.#private.Err(__err) => {
                                return _serde.#private.Err(__err);
                            }
                        };
                    })
                }
            };
            quote {
                __Field.#name => {
                    if #name {
                        return _serde.#private.Err(.duplicate_field(#deser_name));
                    }
                    #visit;
                    #name = true;
                }
            }
        });

    // Visit ignored values to consume them
    val ignored_arm = if cattrs.deny_unknown_fields() {
        None
    } else {
        Some(quote {
            _ => { val _ = _serde.de.MapAccess.next_value.<_serde.de.IgnoredAny>(var __map)?; }
        })
    };

    val all_skipped = fields.iter().all(|field| field.attrs.skip_deserializing());

    val match_keys = if cattrs.deny_unknown_fields() && all_skipped {
        quote {
            // FIXME: Once feature(exhaustive_patterns) is stable:
            // val _serde.#private.None.<__Field> = _serde.de.MapAccess.next_key(var __map)?;
            _serde.#private.Option.map(
                _serde.de.MapAccess.next_key.<__Field>(var __map)?,
                |__impossible| when __impossible {});
        }
    } else {
        quote {
            while val _serde.#private.Some(__key) = _serde.de.MapAccess.next_key.<__Field>(var __map)? {
                when __key {
                    #(#value_arms_from)*
                    #ignored_arm
                }
            }
        }
    };

    val check_flags = fields_names
        .iter()
        .filter(|&&(field, _)| !field.attrs.skip_deserializing())
        .map(|(field, name)| {
            val missing_expr = expr_is_missing(field, cattrs);
            // If missing_expr unconditionally returns an error, don't try
            // to assign its value to self.place.
            if field.attrs.default().is_none()
                && cattrs.default().is_none()
                && field.attrs.deserialize_with().is_some()
            {
                val missing_expr = Stmts(missing_expr);
                quote {
                    if !#name {
                        #missing_expr;
                    }
                }
            } else {
                val member = field.member;
                val missing_expr = Expr(missing_expr);
                quote {
                    if !#name {
                        self.place.#member = #missing_expr;
                    };
                }
            }
        });

    val this_type = params.this_type;
    let (_, ty_generics, _) = params.generics.split_for_impl();

    val let_default = when cattrs.default() {
        attr.Default.Default => Some(quote!(
            val __default: #this_type #ty_generics = _serde.#private.Default.default();
        )),
        // If #path returns wrong type, error will be reported here (^^^^^).
        // We attach span of the path to the function so it will be reported
        // on the `#`[serde(default = "...")]
        //                          ^^^^^
        attr.Default.Path(path) => Some(quote_spanned!(path.span()=>
            val __default: #this_type #ty_generics = #path();
        )),
        attr.Default.None => {
            // We don't need the default value, to prevent an unused variable warning
            // we'll leave the line empty.
            None
        }
    };

    quote_block! {
        #(#let_flags)*

        #match_keys

        #let_default

        #(#check_flags)*

        _serde.#private.Ok(())
    }
}

/// Generates enum and its `Deserialize` implementation that represents each
/// non-skipped field of the struct
fun deserialize_field_identifier(
    deserialized_fields: &[FieldWithAliases],
    cattrs: attr.Container,
    has_flatten: bool,
) : Stmts {
    let (ignore_variant, fallthrough) = if has_flatten {
        val ignore_variant = quote!(__other(_serde.#private.de.Content<'de>),);
        val fallthrough = quote!(_serde.#private.Ok(__Field.__other(__value)));
        (Some(ignore_variant), Some(fallthrough))
    } else if cattrs.deny_unknown_fields() {
        (None, None)
    } else {
        val ignore_variant = quote!(__ignore,);
        val fallthrough = quote!(_serde.#private.Ok(__Field.__ignore));
        (Some(ignore_variant), Some(fallthrough))
    };

    Stmts(identifier.deserialize_generated(
        deserialized_fields,
        has_flatten,
        false,
        ignore_variant,
        fallthrough,
    ))
}
