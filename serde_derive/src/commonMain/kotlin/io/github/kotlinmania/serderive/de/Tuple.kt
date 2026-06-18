package io.github.kotlinmania.serderive

import io.github.kotlinmania.serderive.de.deserialize_seq
import io.github.kotlinmania.serderive.de.has_flatten
import io.github.kotlinmania.serderive.de.Parameters
import io.github.kotlinmania.serderive.de.TupleForm
`#`[cfg(feature = "deserialize_in_place")]
import io.github.kotlinmania.serderive.de.deserialize_seq_in_place
import io.github.kotlinmania.serderive.de.place_lifetime
import io.github.kotlinmania.serderive.fragment.Fragment
import io.github.kotlinmania.serderive.fragment.Stmts
import io.github.kotlinmania.serderive.internals.ast.Field
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.quote.quote_spanned
import io.github.kotlinmania.syn.spanned.Spanned

/// Generates `Deserialize.deserialize` body for a `struct Tuple(...);` including `struct Newtype(T);`
pub(super) fun deserialize(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
    form: TupleForm,
) : Fragment {
    assert!(
        !has_flatten(fields),
        "tuples and tuple variants cannot have flatten fields"
    );

    val field_count = fields
        .iter()
        .filter(|field| !field.attrs.skip_deserializing())
        .count();

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
        quote(""" #local """)
    } else {
        quote(""" #this_value """)
    };

    val type_path = when form {
        TupleForm.Tuple -> construct,
        TupleForm.ExternallyTagged(variant_ident) | TupleForm.Untagged(variant_ident) -> {
            quote(""" #construct.#variant_ident """)
        }
    };
    val expecting = when form {
        TupleForm.Tuple -> format!("tuple struct {}", params.type_name()),
        TupleForm.ExternallyTagged(variant_ident) | TupleForm.Untagged(variant_ident) -> {
            format!("tuple variant {}.{}", params.type_name(), variant_ident)
        }
    };
    val expecting = cattrs.expecting().unwrap_or(expecting);

    val nfields = fields.len();

    val visit_newtype_struct = when form {
        TupleForm.Tuple if nfields == 1 -> {
            deserialize_newtype_struct(type_path, params, fields[0])
        }
        _ -> null,
    };

    val visit_seq = Stmts(deserialize_seq(
        type_path, params, fields, false, cattrs, expecting,
    ));

    val visitor_expr = quote("""
        __Visitor {
            marker: _serde.#private.PhantomData.<#this_type #ty_generics>,
            lifetime: _serde.#private.PhantomData,
        }
    """);
    val dispatch = when form {
        TupleForm.Tuple if nfields == 1 -> {
            val type_name = cattrs.name().deserialize_name();
            quote("""
                _serde.Deserializer.deserialize_newtype_struct(__deserializer, #type_name, #visitor_expr)
            """)
        }
        TupleForm.Tuple -> {
            val type_name = cattrs.name().deserialize_name();
            quote("""
                _serde.Deserializer.deserialize_tuple_struct(__deserializer, #type_name, #field_count, #visitor_expr)
            """)
        }
        TupleForm.ExternallyTagged(_) -> quote("""
            _serde.de.VariantAccess.tuple_variant(__variant, #field_count, #visitor_expr)
        """),
        TupleForm.Untagged(_) -> quote("""
            _serde.Deserializer.deserialize_tuple(__deserializer, #field_count, #visitor_expr)
        """),
    };

    val visitor_var = if field_count == 0 {
        quote(""" _ """)
    } else {
        quote(""" var __seq """)
    };

    quote_block! {
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

            #visit_newtype_struct

            `#`[inline]
            fun visit_seq<__A>(self, #visitor_var: __A) -> _serde.#private.Result<this.Value, __A.Error>
            where
                __A: _serde.de.SeqAccess<#delife>,
            {
                #visit_seq
            }
        }

        #dispatch
    }
}

fun deserialize_newtype_struct(
    type_path: TokenStream,
    params: Parameters,
    field: Field,
) : TokenStream {
    val delife = params.borrowed.de_lifetime();
    val field_ty = field.ty;
    val deserializer_var = quote(""" __e """);

    val value = when field.attrs.deserialize_with() {
        null -> {
            val span = field.original.span();
            val func = quote_spanned(span, """.deserialize """);
            quote("""
                #func(#deserializer_var)?
            """)
        }
        path -> {
            // If #path returns wrong type, error will be reported here (^^^^^).
            // We attach span of the path to the function so it will be reported
            // on the `#`[serde(with = "...")]
            //                       ^^^^^
            quote_spanned! {path.span()->
                #path(#deserializer_var)?
            }
        }
    };

    val var result = quote(""" #type_path(__field0 """));
    if params.has_getter {
        val this_type = params.this_type;
        let (_, ty_generics, _) = params.generics.split_for_impl();
        result = quote("""
            _serde.#private.Into.<#this_type #ty_generics>.into(#result)
        """);
    }

    quote("""
        `#`[inline]
        fun visit_newtype_struct<__E>(self, #deserializer_var: __E) -> _serde.#private.Result<this.Value, __E.Error>
        where
            __E: _serde.Deserializer<#delife>,
        {
            val __field0: #field_ty = #value;
            _serde.#private.Ok(#result)
        }
    """)
}

/// Generates `Deserialize.deserialize_in_place` body for a `struct Tuple(...);` including `struct Newtype(T);`
`#`[cfg(feature = "deserialize_in_place")]
pub(super) fun deserialize_in_place(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
) : Fragment {
    assert!(
        !has_flatten(fields),
        "tuples and tuple variants cannot have flatten fields"
    );

    val field_count = fields
        .iter()
        .filter(|field| !field.attrs.skip_deserializing())
        .count();

    val this_type = params.this_type;
    let (de_impl_generics, de_ty_generics, ty_generics, where_clause) =
        params.generics_with_de_lifetime();
    val delife = params.borrowed.de_lifetime();

    val expecting = format!("tuple struct {}", params.type_name());
    val expecting = cattrs.expecting().unwrap_or(expecting);

    val nfields = fields.len();

    val visit_newtype_struct = if nfields == 1 {
        // We do not generate deserialize_in_place if every field has a
        // deserialize_with.
        assert!(fields[0].attrs.deserialize_with().is_none());

        quote("""
            `#`[inline]
            fun visit_newtype_struct<__E>(self, __e: __E -> _serde.#private.Result<this.Value, __E.Error>
            where
                __E: _serde.Deserializer<#delife>,
            {
                _serde.Deserialize.deserialize_in_place(__e, var self.place.0)
            }
        """))
    } else {
        null
    };

    val visit_seq = Stmts(deserialize_seq_in_place(params, fields, cattrs, expecting));

    val visitor_expr = quote("""
        __Visitor {
            place: __place,
            lifetime: _serde.#private.PhantomData,
        }
    """);

    val type_name = cattrs.name().deserialize_name();
    val dispatch = if nfields == 1 {
        quote(""" _serde.Deserializer.deserialize_newtype_struct(__deserializer, #type_name, #visitor_expr """))
    } else {
        quote(""" _serde.Deserializer.deserialize_tuple_struct(__deserializer, #type_name, #field_count, #visitor_expr """))
    };

    val visitor_var = if field_count == 0 {
        quote(""" _ """)
    } else {
        quote(""" var __seq """)
    };

    val in_place_impl_generics = de_impl_generics.in_place();
    val in_place_ty_generics = de_ty_generics.in_place();
    val place_life = place_lifetime();

    quote_block! {
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

            #visit_newtype_struct

            `#`[inline]
            fun visit_seq<__A>(self, #visitor_var: __A) -> _serde.#private.Result<this.Value, __A.Error>
            where
                __A: _serde.de.SeqAccess<#delife>,
            {
                #visit_seq
            }
        }

        #dispatch
    }
}
