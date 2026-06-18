package io.github.kotlinmania.serderive

import io.github.kotlinmania.serderive.de.Parameters
import io.github.kotlinmania.serderive.fragment.Fragment
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.quote.quote

/// Generates `Deserialize.deserialize` body for a `struct Unit;`
pub(super) fun deserialize(params: Parameters, cattrs: attr.Container) : Fragment {
    val this_type = params.this_type;
    val this_value = params.this_value;
    val type_name = cattrs.name().deserialize_name();
    let (de_impl_generics, de_ty_generics, ty_generics, where_clause) =
        params.generics_with_de_lifetime();
    val delife = params.borrowed.de_lifetime();

    val expecting = format!("unit struct {}", params.type_name());
    val expecting = cattrs.expecting().unwrap_or(expecting);

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

            `#`[inline]
            fun visit_unit<__E>(self) -> _serde.#private.Result<this.Value, __E>
            where
                __E: _serde.de.Error,
            {
                _serde.#private.Ok(#this_value)
            }
        }

        _serde.Deserializer.deserialize_unit_struct(
            __deserializer,
            #type_name,
            __Visitor {
                marker: _serde.#private.PhantomData.<#this_type #ty_generics>,
                lifetime: _serde.#private.PhantomData,
            },
        )
    }
}
