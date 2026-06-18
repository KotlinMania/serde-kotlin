package io.github.kotlinmania.serderive

//! Deserialization for internally tagged enums:
//!
//! ```ignore
//! `#`[serde(tag = "...")]
//! enum Enum {}
//! ```

import io.github.kotlinmania.serderive.de.enum_
import io.github.kotlinmania.serderive.de.enum_untagged
import io.github.kotlinmania.serderive.de.struct_
use crate.de.{
    effective_style, expr_is_missing, field_i, unwrap_to_variant_closure, Parameters, StructForm,
};
import io.github.kotlinmania.serderive.fragment.Expr
import io.github.kotlinmania.serderive.fragment.Fragment
import io.github.kotlinmania.serderive.fragment.Match
import io.github.kotlinmania.serderive.internals.ast.Style
import io.github.kotlinmania.serderive.internals.ast.Variant
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.quote.quote

/// Generates `Deserialize.deserialize` body for an `enum Enum {...}` with ``#`[serde(tag)]` attribute
pub(super) fun deserialize(
    params: Parameters,
    variants: &[Variant],
    cattrs: attr.Container,
    tag: str,
) : Fragment {
    let (variants_stmt, variant_visitor) = enum_.prepare_enum_variant_enum(variants);

    // Match arms to extract a variant from a string
    val variant_arms = variants
        .iter()
        .enumerate()
        .filter(|&(_, variant)| !variant.attrs.skip_deserializing())
        .map(|(i, variant)| {
            val variant_name = field_i(i);

            val block = Match(deserialize_internally_tagged_variant(
                params, variant, cattrs,
            ));

            quote("""
                __Field.#variant_name -> #block
            """)
        });

    val expecting = format!("internally tagged enum {}", params.type_name());
    val expecting = cattrs.expecting().unwrap_or(expecting);

    quote_block! {
        #variant_visitor

        #variants_stmt

        let (__tag, __content) = _serde.Deserializer.deserialize_any(
            __deserializer,
            _serde.#private.de.TaggedContentVisitor.<__Field>.new(#tag, #expecting))?;
        val __deserializer = _serde.#private.de.ContentDeserializer.<__D.Error>.new(__content);

        when __tag {
            #(#variant_arms)*
        }
    }
}

// Generates significant part of the visit_seq and visit_map bodies of visitors
// for the variants of internally tagged enum.
fun deserialize_internally_tagged_variant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container,
) : Fragment {
    if val path = variant.attrs.deserialize_with() {
        val unwrap_fn = unwrap_to_variant_closure(params, variant, false);
        return quote_block! {
            _serde.#private.Result.map(#path(__deserializer), #unwrap_fn)
        };
    }

    val variant_ident = variant.ident;

    when effective_style(variant) {
        Style.Unit -> {
            val this_value = params.this_value;
            val type_name = params.type_name();
            val variant_name = variant.ident.to_string();
            val default = variant.fields.first().map(|field| {
                val default = Expr(expr_is_missing(field, cattrs));
                quote(""" (#default """))
            });
            quote_block! {
                _serde.Deserializer.deserialize_any(__deserializer, _serde.#private.de.InternallyTaggedUnitVisitor.new(#type_name, #variant_name))?;
                _serde.#private.Ok(#this_value.#variant_ident #default)
            }
        }
        Style.Newtype -> {
            enum_untagged.deserialize_newtype_variant(variant_ident, params, variant.fields[0])
        }
        Style.Struct -> struct_.deserialize(
            params,
            variant.fields,
            cattrs,
            StructForm.InternallyTagged(variant_ident),
        ),
        Style.Tuple -> unreachable!("checked in serde_derive_internals"),
    }
}
