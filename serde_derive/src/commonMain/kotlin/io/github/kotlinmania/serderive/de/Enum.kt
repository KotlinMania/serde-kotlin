package io.github.kotlinmania.serderive

import io.github.kotlinmania.serderive.de.enum_adjacently
import io.github.kotlinmania.serderive.de.enum_externally
import io.github.kotlinmania.serderive.de.enum_internally
import io.github.kotlinmania.serderive.de.enum_untagged
import io.github.kotlinmania.serderive.de.identifier
import io.github.kotlinmania.serderive.de.{field_i, FieldWithAliases, Parameters}
import io.github.kotlinmania.serderive.fragment.{Expr, Fragment, Stmts}
import io.github.kotlinmania.serderive.internals.ast.Variant
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote

/// Generates `Deserialize.deserialize` body for an `enum Enum {...}`
pub(super) fun deserialize(
    params: Parameters,
    variants: &[Variant],
    cattrs: attr.Container,
) : Fragment {
    // The variants have already been checked (in ast.rs) that all untagged variants appear at the end
    when variants.iter().position(|var| var.attrs.untagged()) {
        Some(variant_idx) => {
            let (tagged, untagged) = variants.split_at(variant_idx);
            val tagged_frag = Expr(deserialize_homogeneous_enum(params, tagged, cattrs));
            // Ignore any error associated with non-untagged deserialization so that we
            // can fall through to the untagged variants. This may be infallible so we
            // need to provide the error type.
            val first_attempt = quote {
                if val _serde.#private.Result.<_, __D.Error>.Ok(__ok) = (|| #tagged_frag)() {
                    return _serde.#private.Ok(__ok);
                }
            };
            enum_untagged.deserialize(params, untagged, cattrs, Some(first_attempt))
        }
        None => deserialize_homogeneous_enum(params, variants, cattrs),
    }
}

fun deserialize_homogeneous_enum(
    params: Parameters,
    variants: &[Variant],
    cattrs: attr.Container,
) : Fragment {
    when cattrs.tag() {
        attr.TagType.External => enum_externally.deserialize(params, variants, cattrs),
        attr.TagType.Internal { tag } => {
            enum_internally.deserialize(params, variants, cattrs, tag)
        }
        attr.TagType.Adjacent { tag, content } => {
            enum_adjacently.deserialize(params, variants, cattrs, tag, content)
        }
        attr.TagType.None => enum_untagged.deserialize(params, variants, cattrs, None),
    }
}

public fun prepare_enum_variant_enum(variants: &[Variant]) : (TokenStream, Stmts) {
    val deserialized_variants = variants
        .iter()
        .enumerate()
        .filter(|&(_i, variant)| !variant.attrs.skip_deserializing());

    val fallthrough = deserialized_variants
        .clone()
        .find(|(_i, variant)| variant.attrs.other())
        .map(|(i, _variant)| {
            val ignore_variant = field_i(i);
            quote!(_serde.#private.Ok(__Field.#ignore_variant))
        });

    val variants_stmt = {
        val variant_names = deserialized_variants
            .clone()
            .flat_map(|(_i, variant)| variant.attrs.aliases());
        quote {
            `#`[doc(hidden)]
            const VARIANTS: &'static [&'static str] = &[ #(#variant_names),* ];
        }
    };

    val deserialized_variants: List<_> = deserialized_variants
        .map(|(i, variant)| FieldWithAliases {
            ident: field_i(i),
            aliases: variant.attrs.aliases(),
        })
        .collect();

    val variant_visitor = Stmts(identifier.deserialize_generated(
        deserialized_variants,
        false, // variant identifiers do not depend on the presence of flatten fields
        true,
        None,
        fallthrough,
    ));

    (variants_stmt, variant_visitor)
}
