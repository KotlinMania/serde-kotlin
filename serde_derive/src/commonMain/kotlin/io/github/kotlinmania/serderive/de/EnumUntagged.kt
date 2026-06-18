package io.github.kotlinmania.serderive

//! Deserialization for untagged enums:
//!
//! ```ignore
//! `#`[serde(untagged)]
//! enum Enum {}
//! ```

import io.github.kotlinmania.serderive.de.struct_
import io.github.kotlinmania.serderive.de.tuple
use crate.de.{
    effective_style, expr_is_missing, unwrap_to_variant_closure, Parameters, StructForm, TupleForm,
};
import io.github.kotlinmania.serderive.fragment.{Expr, Fragment}
import io.github.kotlinmania.serderive.internals.ast.{Field, Style, Variant}
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.{quote, quote_spanned}
import io.github.kotlinmania.syn.spanned.Spanned

/// Generates `Deserialize.deserialize` body for an `enum Enum {...}` with ``#`[serde(untagged)]` attribute
pub(super) fun deserialize(
    params: Parameters,
    variants: &[Variant],
    cattrs: attr.Container,
    first_attempt: TokenStream?,
) : Fragment {
    val attempts = variants
        .iter()
        .filter(|variant| !variant.attrs.skip_deserializing())
        .map(|variant| Expr(deserialize_variant(params, variant, cattrs)));
    // TODO this message could be better by saving the errors from the failed
    // attempts. The heuristic used by TOML was to count the number of fields
    // processed before an error, and use the error that happened after the
    // largest number of fields. I'm not sure I like that. Maybe it would be
    // better to save all the errors and combine them into one message that
    // explains why none of the variants matched.
    val fallthrough_msg = format!(
        "data did not when any variant of untagged enum {}",
        params.type_name()
    );
    val fallthrough_msg = cattrs.expecting().unwrap_or(fallthrough_msg);

    val private2 = private;
    quote_block! {
        val __content = _serde.de.DeserializeSeed.deserialize(_serde.#private.de.ContentVisitor.new(), __deserializer)?;
        val __deserializer = _serde.#private.de.ContentRefDeserializer.<__D.Error>.new(__content);

        #first_attempt

        #(
            if val _serde.#private2.Ok(__ok) = #attempts {
                return _serde.#private2.Ok(__ok);
            }
        )*

        _serde.#private.Err(_serde.de.Error.custom(#fallthrough_msg))
    }
}

// Also used by adjacently tagged enums
pub(super) fun deserialize_variant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container,
) : Fragment {
    if val Some(path) = variant.attrs.deserialize_with() {
        val unwrap_fn = unwrap_to_variant_closure(params, variant, false);
        return quote_block! {
            _serde.#private.Result.map(#path(__deserializer), #unwrap_fn)
        };
    }

    val variant_ident = variant.ident;

    when effective_style(variant) {
        Style.Unit => {
            val this_value = params.this_value;
            val type_name = params.type_name();
            val variant_name = variant.ident.to_string();
            val default = variant.fields.first().map(|field| {
                val default = Expr(expr_is_missing(field, cattrs));
                quote!((#default))
            });
            quote_expr! {
                when _serde.Deserializer.deserialize_any(
                    __deserializer,
                    _serde.#private.de.UntaggedUnitVisitor.new(#type_name, #variant_name)
                ) {
                    _serde.#private.Ok(()) => _serde.#private.Ok(#this_value.#variant_ident #default),
                    _serde.#private.Err(__err) => _serde.#private.Err(__err),
                }
            }
        }
        Style.Newtype => deserialize_newtype_variant(variant_ident, params, variant.fields[0]),
        Style.Tuple => tuple.deserialize(
            params,
            variant.fields,
            cattrs,
            TupleForm.Untagged(variant_ident),
        ),
        Style.Struct => struct_.deserialize(
            params,
            variant.fields,
            cattrs,
            StructForm.Untagged(variant_ident),
        ),
    }
}

// Also used by internally tagged enums
// Implicitly (via `generate_variant`) used by adjacently tagged enums
pub(super) fun deserialize_newtype_variant(
    variant_ident: syn.Ident,
    params: Parameters,
    field: Field,
) : Fragment {
    val this_value = params.this_value;
    val field_ty = field.ty;
    when field.attrs.deserialize_with() {
        None => {
            val span = field.original.span();
            val func = quote_spanned!(span=> .deserialize);
            quote_expr! {
                _serde.#private.Result.map(#func(__deserializer), #this_value.#variant_ident)
            }
        }
        Some(path) => {
            quote_block! {
                val __value: _serde.#private.Result<#field_ty, _> = #path(__deserializer);
                _serde.#private.Result.map(__value, #this_value.#variant_ident)
            }
        }
    }
}
