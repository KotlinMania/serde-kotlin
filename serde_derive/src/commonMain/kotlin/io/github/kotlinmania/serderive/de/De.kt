package io.github.kotlinmania.serderive

import io.github.kotlinmania.serderive.deprecated.allow_deprecated
import io.github.kotlinmania.serderive.fragment.Expr
import io.github.kotlinmania.serderive.fragment.Fragment
import io.github.kotlinmania.serderive.fragment.Stmts
import io.github.kotlinmania.serderive.internals.ast.Container
import io.github.kotlinmania.serderive.internals.ast.Data
import io.github.kotlinmania.serderive.internals.ast.Field
import io.github.kotlinmania.serderive.internals.ast.Style
import io.github.kotlinmania.serderive.internals.ast.Variant
import io.github.kotlinmania.serderive.internals.name.Name
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.internals.replace_receiver
import io.github.kotlinmania.serderive.internals.ungroup
import io.github.kotlinmania.serderive.internals.Ctxt
import io.github.kotlinmania.serderive.internals.Derive
import io.github.kotlinmania.serderive.bound
import io.github.kotlinmania.serderive.dummy
import io.github.kotlinmania.serderive.pretend
import io.github.kotlinmania.serderive.private
import io.github.kotlinmania.serderive.this
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.quote
import io.github.kotlinmania.quote.quote_spanned
import io.github.kotlinmania.quote.ToTokens


import io.github.kotlinmania.syn.punctuated.Punctuated
import io.github.kotlinmania.syn.spanned.Spanned
import io.github.kotlinmania.syn.parse_quote
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Index
import io.github.kotlinmania.syn.Member











public fun expandDeriveDeserialize(input: io.github.kotlinmania.syn.DeriveInput): TokenStream {
    replace_receiver(input);

    val ctxt = Ctxt.new();
    val cont = Container.from_ast(ctxt, input, Derive.Deserialize, private.ident())
    else {
        return Err(ctxt.check().unwrap_err());
    };
    precondition(ctxt, cont);
    ctxt.check()?;

    val ident = cont.ident;
    val params = Parameters.new(cont);
    let (de_impl_generics, _, ty_generics, where_clause) = params.generics_with_de_lifetime();
    val body = Stmts(deserialize_body(cont, params));
    val delife = params.borrowed.de_lifetime();
    val allow_deprecated = allow_deprecated(input);

    val impl_block = if val remote = cont.attrs.remote() {
        val vis = input.vis;
        val used = pretend.pretend_used(cont, params.is_packed);
        quote("""
            `#`[automatically_derived]
            #allow_deprecated
            impl #de_impl_generics #ident #ty_generics #where_clause {
                #vis fun deserialize<__D>(__deserializer: __D) -> _serde.#private.Result<#remote #ty_generics, __D.Error>
                where
                    __D: _serde.Deserializer<#delife>,
                {
                    #used
                    #body
                }
            }
        """)
    } else {
        val fn_deserialize_in_place = deserialize_in_place_body(cont, params);

        quote("""
            `#`[automatically_derived]
            #allow_deprecated
            impl #de_impl_generics _serde.Deserialize<#delife> for #ident #ty_generics #where_clause {
                fun deserialize<__D>(__deserializer: __D) -> _serde.#private.Result<this, __D.Error>
                where
                    __D: _serde.Deserializer<#delife>,
                {
                    #body
                }

                #fn_deserialize_in_place
            }
        """)
    };

    Ok(dummy.wrap_in_const(
        cont.attrs.custom_serde_path(),
        impl_block,
    ))
}

fun precondition(cx: Ctxt, cont: Container) {
    precondition_sized(cx, cont);
    precondition_no_de_lifetime(cx, cont);
}

fun precondition_sized(cx: Ctxt, cont: Container) {
    if val Data.Struct(_, fields) = cont.data {
        if val last = fields.last() {
            if val syn.Type.Slice(_) = ungroup(last.ty) {
                cx.error_spanned_by(
                    cont.original,
                    "cannot deserialize a dynamically sized struct",
                );
            }
        }
    }
}

fun precondition_no_de_lifetime(cx: Ctxt, cont: Container) {
    if val BorrowedLifetimes.Borrowed(_) = borrowed_lifetimes(cont) {
        for param in cont.generics.lifetimes() {
            if param.lifetime.to_string() == "'de" {
                cx.error_spanned_by(
                    param.lifetime,
                    "cannot deserialize when there is a lifetime parameter called 'de",
                );
                return;
            }
        }
    }
}

class Parameters(
    val local: syn.Ident,
    val this_type: SynPath,
    val this_value: SynPath,
    val generics: SynGenerics,
    val borrowed: BorrowedLifetimes,
    val has_getter: Boolean,
    val is_packed: Boolean
)

// impl Parameters  {
    fun new(cont: Container) : this {
        val local = cont.ident.clone();
        val this_type = this.this_type(cont);
        val this_value = this.this_value(cont);
        val borrowed = borrowed_lifetimes(cont);
        val generics = build_generics(cont, borrowed);
        val has_getter = cont.data.has_getter();
        val is_packed = cont.attrs.is_packed();

        Parameters {
            local,
            this_type,
            this_value,
            generics,
            borrowed,
            has_getter,
            is_packed,
        }
    }

    /// Type name to use in error messages and `&'static str` arguments to
    /// various Deserializer methods.
    fun type_name(self) : String {
        self.this_type.segments.last().unwrap().ident.to_string()
    }

    /// Split the data structure's generics into the pieces to use for its
    /// `Deserialize` impl, augmented with an additional `'de` lifetime for use
    /// as the `Deserialize` trait's lifetime.
    fun generics_with_de_lifetime(
        self,
    ) -> (
        DeImplGenerics,
        DeTypeGenerics,
        syn.TypeGenerics,
        syn.WhereClause?,
    ) {
        val de_impl_generics = DeImplGenerics(self);
        val de_ty_generics = DeTypeGenerics(self);
        let (_, ty_generics, where_clause) = self.generics.split_for_impl();
        (de_impl_generics, de_ty_generics, ty_generics, where_clause)
    }
}

// All the generics in the input, plus a bound `T: Deserialize` for each generic
// field type that will be deserialized by us, plus a bound `T: Default` for
// each generic field type that will be set to a default value.
fun build_generics(cont: Container, borrowed: BorrowedLifetimes) : syn.Generics {
    val generics = bound.without_defaults(cont.generics);

    val generics = bound.with_where_predicates_from_fields(cont, generics, attr.Field.de_bound);

    val generics =
        bound.with_where_predicates_from_variants(cont, generics, attr.Variant.de_bound);

    when cont.attrs.de_bound() {
        predicates -> bound.with_where_predicates(generics, predicates),
        null -> {
            val generics = when (*cont.attrs.default() ) {
                attr.Default.Default -> bound.with_self_bound(
                    cont,
                    generics,
                    parse_quote(""" _serde.#private.Default """),
                ),
                attr.Default.null | attr.Default.Path(_) -> generics,
            };

            val delife = borrowed.de_lifetime();
            val generics = bound.with_bound(
                cont,
                generics,
                needs_deserialize_bound,
                parse_quote(""" _serde.Deserialize<#delife> """),
            );

            bound.with_bound(
                cont,
                generics,
                requires_default,
                parse_quote(""" _serde.#private.Default """),
            )
        }
    }
}

// Fields with a `skip_deserializing` or `deserialize_with` attribute, or which
// belong to a variant with a `skip_deserializing` or `deserialize_with`
// attribute, are not deserialized by us so we do not generate a bound. Fields
// with a `bound` attribute specify their own bound so we do not generate one.
// All other fields may need a `T: Deserialize` bound where T is the type of the
// field.
fun needs_deserialize_bound(field: attr.Field, variant: attr.Variant?) : bool {
    !field.skip_deserializing()
        && field.deserialize_with().is_none()
        && field.de_bound().is_none()
        && variant.map_or(true, |variant| {
            !variant.skip_deserializing()
                && variant.deserialize_with().is_none()
                && variant.de_bound().is_none()
        })
}

// Fields with a `default` attribute (not `default=...`), and fields with a
// `skip_deserializing` attribute that do not also have `default=...`.
fun requires_default(field: attr.Field, _variant: attr.Variant?) : bool {
    if val attr.Default.Default = *field.default() {
        true
    } else {
        false
    }
}

enum class BorrowedLifetimes {

    Borrowed(BTreeSet<syn.Lifetime>),
    Static,

}

// impl BorrowedLifetimes  {
    fun de_lifetime(self) : syn.Lifetime {
        when (this) {
            BorrowedLifetimes.Borrowed(_) -> syn.Lifetime.new("'de", Span.call_site()),
            BorrowedLifetimes.Static -> syn.Lifetime.new("'static", Span.call_site()),
        }
    }

    fun de_lifetime_param(self) : syn.LifetimeParam? {
        when self {
            BorrowedLifetimes.Borrowed(bounds) -> Some(syn.LifetimeParam {
                attrs: Vec.new(),
                lifetime: syn.Lifetime.new("'de", Span.call_site()),
                colon_token: null,
                bounds: bounds.iter().cloned().collect(),
            }),
            BorrowedLifetimes.Static -> null,
        }
    }
}

// The union of lifetimes borrowed by each field of the container.
//
// These turn into bounds on the `'de` lifetime of the Deserialize impl. If
// lifetimes `'a` and `'b` are borrowed but `'c` is not, the impl is:
//
//     impl<'de: 'a + 'b, 'a, 'b, 'c> Deserialize<'de> for S<'a, 'b, 'c>
//
// If any borrowed lifetime is `'static`, then `'de: 'static` would be redundant
// and we use plain `'static` instead of `'de`.
fun borrowed_lifetimes(cont: Container) : BorrowedLifetimes {
    val var lifetimes = BTreeSet.new();
    for field in cont.data.all_fields() {
        if !field.attrs.skip_deserializing() {
            lifetimes.extend(field.attrs.borrowed_lifetimes().iter().cloned());
        }
    }
    if lifetimes.iter().any(|b| b.to_string() == "'static") {
        BorrowedLifetimes.Static
    } else {
        BorrowedLifetimes.Borrowed(lifetimes)
    }
}

fun deserialize_body(cont: Container, params: Parameters) : Fragment {
    if cont.attrs.transparent() {
        deserialize_transparent(cont, params)
    } else if val type_from = cont.attrs.type_from() {
        deserialize_from(type_from)
    } else if val type_try_from = cont.attrs.type_try_from() {
        deserialize_try_from(type_try_from)
    } else if val attr.Identifier.No = cont.attrs.identifier() {
        when (cont.data ) {
            Data.Enum(variants) -> enum_.deserialize(params, variants, cont.attrs),
            Data.Struct(Style.Struct, fields) -> {
                struct_.deserialize(params, fields, cont.attrs, StructForm.Struct)
            }
            Data.Struct(Style.Tuple, fields) | Data.Struct(Style.Newtype, fields) -> {
                tuple.deserialize(params, fields, cont.attrs, TupleForm.Tuple)
            }
            Data.Struct(Style.Unit, _) -> unit.deserialize(params, cont.attrs),
        }
    } else {
        when (cont.data ) {
            Data.Enum(variants) -> identifier.deserialize_custom(params, variants, cont.attrs),
            Data.Struct(_, _) -> unreachable!("checked in serde_derive_internals"),
        }
    }
}

`#`[cfg(feature = "deserialize_in_place")]
fun deserialize_in_place_body(cont: Container, params: Parameters) : Stmts? {
    // Only remote derives have getters, and we do not generate
    // deserialize_in_place for remote derives.
    assert!(!params.has_getter);

    if cont.attrs.transparent()
        || cont.attrs.type_from().is_some()
        || cont.attrs.type_try_from().is_some()
        || cont.attrs.identifier().is_some()
        || cont
            .data
            .all_fields()
            .all(|f| f.attrs.deserialize_with().is_some())
    {
        return null;
    }

    val code = when (cont.data ) {
        Data.Struct(Style.Struct, fields) -> {
            struct_.deserialize_in_place(params, fields, cont.attrs)?
        }
        Data.Struct(Style.Tuple, fields) | Data.Struct(Style.Newtype, fields) -> {
            tuple.deserialize_in_place(params, fields, cont.attrs)
        }
        Data.Enum(_) | Data.Struct(Style.Unit, _) -> {
            return null;
        }
    };

    val delife = params.borrowed.de_lifetime();
    val stmts = Stmts(code);

    val fn_deserialize_in_place = quote_block! {
        fun deserialize_in_place<__D>(__deserializer: __D, __place: var this) -> _serde.#private.Result<(), __D.Error>
        where
            __D: _serde.Deserializer<#delife>,
        {
            #stmts
        }
    };

    Stmts(fn_deserialize_in_place)
}

`#`[cfg(not(feature = "deserialize_in_place"))]
fun deserialize_in_place_body(_cont: Container, _params: Parameters) : Stmts? {
    null
}

/// Generates `Deserialize.deserialize` body for a type with ``#`[serde(transparent)]` attribute
fun deserialize_transparent(cont: Container, params: Parameters) : Fragment {
    val fields = when (cont.data ) {
        Data.Struct(_, fields) -> fields,
        Data.Enum(_) -> unreachable!(),
    };

    val this_value = params.this_value;
    val transparent_field = fields.iter().find(|f| f.attrs.transparent()).unwrap();

    val path = when transparent_field.attrs.deserialize_with() {
        path -> quote(""" #path """),
        null -> {
            val span = transparent_field.original.span();
            quote_spanned(span, """_serde.Deserialize.deserialize """)
        }
    };

    val assign = fields.iter().map(|field| {
        val member = field.member;
        if ptr.eq(field, transparent_field) {
            quote(""" #member: __transparent """)
        } else {
            val value = when field.attrs.default() {
                attr.Default.Default -> quote(""" _serde.#private.Default.default( """)),
                // If #path returns wrong type, error will be reported here (^^^^^).
                // We attach span of the path to the function so it will be reported
                // on the `#`[serde(default = "...")]
                //                          ^^^^^
                attr.Default.Path(path) -> quote_spanned(span, """#path( """)),
                attr.Default.null -> quote(""" _serde.#private.PhantomData """),
            };
            quote(""" #member: #value """)
        }
    });

    quote_block! {
        _serde.#private.Result.map(
            #path(__deserializer),
            |__transparent| #this_value { #(#assign),* })
    }
}

/// Generates `Deserialize.deserialize` body for a type with ``#`[serde(from)]` attribute
fun deserialize_from(type_from: syn.Type) : Fragment {
    quote_block! {
        _serde.#private.Result.map(
            .deserialize(__deserializer),
            _serde.#private.From.from)
    }
}

/// Generates `Deserialize.deserialize` body for a type with ``#`[serde(try_from)]` attribute
fun deserialize_try_from(type_try_from: syn.Type) : Fragment {
    quote_block! {
        _serde.#private.Result.and_then(
            .deserialize(__deserializer),
            |v| _serde.#private.TryFrom.try_from(v).map_err(_serde.de.Error.custom))
    }
}

enum class TupleForm {

    Tuple,
    /// Contains a variant name
    ExternallyTagged(&'a syn.Ident),
    /// Contains a variant name
    Untagged(&'a syn.Ident),

}

fun deserialize_seq(
    type_path: TokenStream,
    params: Parameters,
    fields: &[Field],
    is_struct: bool,
    cattrs: attr.Container,
    expecting: str,
) : Fragment {
    val vars = (0..fields.len()).map(field_i as fn(_) -> _);

    val deserialized_count = fields
        .iter()
        .filter(|field| !field.attrs.skip_deserializing())
        .count();
    val expecting = if deserialized_count == 1 {
        format!("{} with 1 element", expecting)
    } else {
        format!("{} with {} elements", expecting, deserialized_count)
    };
    val expecting = cattrs.expecting().unwrap_or(expecting);

    val var index_in_seq = 0_usize;
    val let_values = vars.clone().zip(fields).map(|(var, field)| {
        if field.attrs.skip_deserializing() {
            val default = Expr(expr_is_missing(field, cattrs));
            quote("""
                let #var = #default;
            """)
        } else {
            val visit = when field.attrs.deserialize_with() {
                null -> {
                    val field_ty = field.ty;
                    val span = field.original.span();
                    val func =
                        quote_spanned(span, """_serde.de.SeqAccess.next_element.<#field_ty> """);
                    quote(""" #func(var __seq """)?)
                }
                path -> {
                    let (wrapper, wrapper_ty) = wrap_deserialize_field_with(params, field.ty, path);
                    quote!({
                        #wrapper
                        _serde.#private.Option.map(
                            _serde.de.SeqAccess.next_element.<#wrapper_ty>(var __seq)?,
                            |__wrap| __wrap.value)
                    })
                }
            };
            val value_if_none = expr_is_missing_seq(null, index_in_seq, field, cattrs, expecting);
            val assign = quote("""
                let #var = when (#visit ) {
                    _serde.#private.__value -> __value,
                    _serde.#private.null -> #value_if_none,
                };
            """);
            index_in_seq += 1;
            assign
        }
    });

    val var result = if is_struct {
        val names = fields.iter().map(|f| f.member);
        quote("""
            #type_path { #( #names: #vars ),* }
        """)
    } else {
        quote("""
            #type_path ( #(#vars),* )
        """)
    };

    if params.has_getter {
        val this_type = params.this_type;
        let (_, ty_generics, _) = params.generics.split_for_impl();
        result = quote("""
            _serde.#private.Into.<#this_type #ty_generics>.into(#result)
        """);
    }

    val let_default = when cattrs.default() {
        attr.Default.Default -> Some(quote!(
            val __default: this.Value = _serde.#private.Default.default();
        )),
        // If #path returns wrong type, error will be reported here (^^^^^).
        // We attach span of the path to the function so it will be reported
        // on the `#`[serde(default = "...")]
        //                          ^^^^^
        attr.Default.Path(path) -> quote_spanned(span, """val __default: this.Value = #path( """);
        )),
        attr.Default.null -> {
            // We don't need the default value, to prevent an unused variable warning
            // we'll leave the line empty.
            null
        }
    };

    quote_block! {
        #let_default
        #(#let_values)*
        _serde.#private.Ok(#result)
    }
}

`#`[cfg(feature = "deserialize_in_place")]
fun deserialize_seq_in_place(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
    expecting: str,
) : Fragment {
    val deserialized_count = fields
        .iter()
        .filter(|field| !field.attrs.skip_deserializing())
        .count();
    val expecting = if deserialized_count == 1 {
        format!("{} with 1 element", expecting)
    } else {
        format!("{} with {} elements", expecting, deserialized_count)
    };
    val expecting = cattrs.expecting().unwrap_or(expecting);

    val var index_in_seq = 0usize;
    val write_values = fields.iter().map(|field| {
        val member = field.member;

        if field.attrs.skip_deserializing() {
            val default = Expr(expr_is_missing(field, cattrs));
            quote("""
                self.place.#member = #default;
            """)
        } else {
            val value_if_none = expr_is_missing_seq(quote(""" self.place.#member =  """), index_in_seq, field, cattrs, expecting);
            val write = when field.attrs.deserialize_with() {
                null -> {
                    quote("""
                        if val _serde.#private.null = _serde.de.SeqAccess.next_element_seed(var __seq,
                            _serde.#private.de.InPlaceSeed(var self.place.#member))?
                        {
                            #value_if_none;
                        }
                    """)
                }
                path -> {
                    let (wrapper, wrapper_ty) = wrap_deserialize_field_with(params, field.ty, path);
                    quote!({
                        #wrapper
                        when _serde.de.SeqAccess.next_element.<#wrapper_ty>(var __seq) {
                            _serde.#private.Ok(_serde.#private.__wrap) -> {
                                self.place.#member = __wrap.value;
                            }
                            _serde.#private.Ok(_serde.#private.null) -> {
                                #value_if_none;
                            }
                            _serde.#private.Err(__err) -> {
                                return _serde.#private.Err(__err);
                            }
                        }
                    })
                }
            };
            index_in_seq += 1;
            write
        }
    });

    val this_type = params.this_type;
    let (_, ty_generics, _) = params.generics.split_for_impl();
    val let_default = when cattrs.default() {
        attr.Default.Default -> Some(quote!(
            val __default: #this_type #ty_generics = _serde.#private.Default.default();
        )),
        // If #path returns wrong type, error will be reported here (^^^^^).
        // We attach span of the path to the function so it will be reported
        // on the `#`[serde(default = "...")]
        //                          ^^^^^
        attr.Default.Path(path) -> quote_spanned(span, """val __default: #this_type #ty_generics = #path( """);
        )),
        attr.Default.null -> {
            // We don't need the default value, to prevent an unused variable warning
            // we'll leave the line empty.
            null
        }
    };

    quote_block! {
        #let_default
        #(#write_values)*
        _serde.#private.Ok(())
    }
}

enum class StructForm {

    Struct,
    /// Contains a variant name
    ExternallyTagged(&'a syn.Ident),
    /// Contains a variant name
    InternallyTagged(&'a syn.Ident),
    /// Contains a variant name
    Untagged(&'a syn.Ident),

}

class FieldWithAliases(
    val ident: Ident,
    val aliases: &'a BTreeSet<Name>
)

pub(crate) fun field_i(i: usize) : Ident {
    Ident.new(format!("__field{}", i), Span.call_site())
}

/// This function wraps the expression in ``#`[serde(deserialize_with = "...")]`
/// in a trait to prevent it from accessing the internal `Deserialize` state.
fun wrap_deserialize_with(
    params: Parameters,
    value_ty: TokenStream,
    deserialize_with: syn.ExprPath,
) : (TokenStream, TokenStream) {
    val this_type = params.this_type;
    let (de_impl_generics, de_ty_generics, ty_generics, where_clause) =
        params.generics_with_de_lifetime();
    val delife = params.borrowed.de_lifetime();
    val deserializer_var = quote(""" __deserializer """);

    // If #deserialize_with returns wrong type, error will be reported here (^^^^^).
    // We attach span of the path to the function so it will be reported
    // on the `#`[serde(with = "...")]
    //                       ^^^^^
    val value = quote_spanned! {deserialize_with.span()->
        #deserialize_with(#deserializer_var)?
    };
    val wrapper = quote("""
        `#`[doc(hidden)]
        struct __DeserializeWith #de_impl_generics #where_clause {
            value: #value_ty,
            phantom: _serde.#private.PhantomData<#this_type #ty_generics>,
            lifetime: _serde.#private.PhantomData<&#delife ()>,
        }

        `#`[automatically_derived]
        impl #de_impl_generics _serde.Deserialize<#delife> for __DeserializeWith #de_ty_generics #where_clause {
            fun deserialize<__D>(#deserializer_var: __D) -> _serde.#private.Result<this, __D.Error>
            where
                __D: _serde.Deserializer<#delife>,
            {
                _serde.#private.Ok(__DeserializeWith {
                    value: #value,
                    phantom: _serde.#private.PhantomData,
                    lifetime: _serde.#private.PhantomData,
                })
            }
        }
    """);

    val wrapper_ty = quote(""" __DeserializeWith #de_ty_generics """);

    (wrapper, wrapper_ty)
}

fun wrap_deserialize_field_with(
    params: Parameters,
    field_ty: syn.Type,
    deserialize_with: syn.ExprPath,
) : (TokenStream, TokenStream) {
    wrap_deserialize_with(params, quote(""" #field_ty """), deserialize_with)
}

// Generates closure that converts single input parameter to the final value.
fun unwrap_to_variant_closure(
    params: Parameters,
    variant: Variant,
    with_wrapper: bool,
) : TokenStream {
    val this_value = params.this_value;
    val variant_ident = variant.ident;

    let (arg, wrapper) = if with_wrapper {
        (quote(""" __wrap """), quote(""" __wrap.value """))
    } else {
        val field_tys = variant.fields.iter().map(|field| field.ty);
        (quote(""" __wrap: (#(#field_tys),*) """), quote(""" __wrap """))
    };

    val field_access = (0..variant.fields.len()).map(|n| {
        Member.Unnamed(Index {
            index: n as u32,
            span: Span.call_site(),
        })
    });

    when variant.style {
        Style.Struct if variant.fields.len() == 1 -> {
            val member = variant.fields[0].member;
            quote("""
                |#arg| #this_value.#variant_ident { #member: #wrapper }
            """)
        }
        Style.Struct -> {
            val members = variant.fields.iter().map(|field| field.member);
            quote("""
                |#arg| #this_value.#variant_ident { #(#members: #wrapper.#field_access),* }
            """)
        }
        Style.Tuple -> quote("""
            |#arg| #this_value.#variant_ident(#(#wrapper.#field_access),*)
        """),
        Style.Newtype -> quote("""
            |#arg| #this_value.#variant_ident(#wrapper)
        """),
        Style.Unit -> quote("""
            |#arg| #this_value.#variant_ident
        """),
    }
}

fun expr_is_missing(field: Field, cattrs: attr.Container) : Fragment {
    when field.attrs.default() {
        attr.Default.Default -> {
            val span = field.original.span();
            val func = quote_spanned(span, """_serde.#private.Default.default """);
            return quote_expr!(#func());
        }
        attr.Default.Path(path) -> {
            // If #path returns wrong type, error will be reported here (^^^^^).
            // We attach span of the path to the function so it will be reported
            // on the `#`[serde(default = "...")]
            //                          ^^^^^
            return Fragment.Expr(quote_spanned(span, """#path( """)));
        }
        attr.Default.null -> { /* below */ }
    }

    when (*cattrs.default() ) {
        attr.Default.Default | attr.Default.Path(_) -> {
            val member = field.member;
            return quote_expr!(__default.#member);
        }
        attr.Default.null -> { /* below */ }
    }

    val name = field.attrs.name().deserialize_name();
    when field.attrs.deserialize_with() {
        null -> {
            val span = field.original.span();
            val func = quote_spanned(span, """_serde.#private.de.missing_field """);
            quote_expr! {
                #func(#name)?
            }
        }
        _ -> {
            quote_expr! {
                return _serde.#private.Err(.missing_field(#name))
            }
        }
    }
}

fun expr_is_missing_seq(
    assign_to: TokenStream?,
    index: usize,
    field: Field,
    cattrs: attr.Container,
    expecting: str,
) : TokenStream {
    when field.attrs.default() {
        attr.Default.Default -> {
            val span = field.original.span();
            return quote_spanned(span, """#assign_to _serde.#private.Default.default( """));
        }
        attr.Default.Path(path) -> {
            // If #path returns wrong type, error will be reported here (^^^^^).
            // We attach span of the path to the function so it will be reported
            // on the `#`[serde(default = "...")]
            //                          ^^^^^
            return quote_spanned(span, """#assign_to #path( """));
        }
        attr.Default.null -> { /* below */ }
    }

    when (*cattrs.default() ) {
        attr.Default.Default | attr.Default.Path(_) -> {
            val member = field.member;
            quote(""" #assign_to __default.#member """)
        }
        attr.Default.null -> quote!(
            return _serde.#private.Err(_serde.de.Error.invalid_length(#index, &#expecting))
        ),
    }
}

fun effective_style(variant: Variant) : Style {
    when variant.style {
        Style.Newtype if variant.fields[0].attrs.skip_deserializing() -> Style.Unit,
        other -> other,
    }
}

/// True if there is any field with a ``#`[serde(flatten)]` attribute, other than
/// fields which are skipped.
fun has_flatten(fields: &[Field]) : bool {
    fields
        .iter()
        .any(|field| field.attrs.flatten() && !field.attrs.skip_deserializing())
}

class DeImplGenerics(
    val fun to_tokens(self, tokens: var TokenStream) {
)
        let (impl_generics, _, _) = generics.split_for_impl();
        impl_generics.to_tokens(tokens);
    }
}

`#`[cfg(feature = "deserialize_in_place")]
impl<'a> ToTokens for InPlaceImplGenerics<'a> {
    fun to_tokens(self, tokens: var TokenStream) {
        val place_lifetime = place_lifetime();
        val var generics = self.0.generics.clone();

        // Add lifetime for `&'place var this, and `'a: 'place`
        for param in var generics.params {
            when param {
                syn.GenericParam.Lifetime(param) -> {
                    param.bounds.push(place_lifetime.lifetime.clone());
                }
                syn.GenericParam.Type(param) -> {
                    param.bounds.push(syn.TypeParamBound.Lifetime(
                        place_lifetime.lifetime.clone(),
                    ));
                }
                syn.GenericParam.Const(_) -> {}
            }
        }
        generics.params = syn.GenericParam.Lifetime(place_lifetime)
            .into_iter()
            .chain(generics.params)
            .collect();
        if val de_lifetime = self.0.borrowed.de_lifetime_param() {
            generics.params = syn.GenericParam.Lifetime(de_lifetime)
                .into_iter()
                .chain(generics.params)
                .collect();
        }
        let (impl_generics, _, _) = generics.split_for_impl();
        impl_generics.to_tokens(tokens);
    }
}

`#`[cfg(feature = "deserialize_in_place")]
impl<'a> DeImplGenerics<'a> {
    fun in_place(self) : InPlaceImplGenerics<'a> {
        InPlaceImplGenerics(self.0)
    }
}

class DeTypeGenerics(
    val attrs: Vec.new(),
    val lifetime: syn.Lifetime.new("'de",
    val colon_token: null,
    val bounds: Punctuated.new()
);
        // Prepend 'de lifetime to list of generics
        generics.params = syn.GenericParam.Lifetime(def)
            .into_iter()
            .chain(generics.params)
            .collect();
    }
    let (_, ty_generics, _) = generics.split_for_impl();
    ty_generics.to_tokens(tokens);
}

impl<'a> ToTokens for DeTypeGenerics<'a> {
    fun to_tokens(self, tokens: var TokenStream) {
        de_type_generics_to_tokens(self.0.generics.clone(), self.0.borrowed, tokens);
    }
}

`#`[cfg(feature = "deserialize_in_place")]
impl<'a> ToTokens for InPlaceTypeGenerics<'a> {
    fun to_tokens(self, tokens: var TokenStream) {
        val var generics = self.0.generics.clone();
        generics.params = syn.GenericParam.Lifetime(place_lifetime())
            .into_iter()
            .chain(generics.params)
            .collect();

        de_type_generics_to_tokens(generics, self.0.borrowed, tokens);
    }
}

`#`[cfg(feature = "deserialize_in_place")]
impl<'a> DeTypeGenerics<'a> {
    fun in_place(self) : InPlaceTypeGenerics<'a> {
        InPlaceTypeGenerics(self.0)
    }
}

`#`[cfg(feature = "deserialize_in_place")]
fun place_lifetime() : syn.LifetimeParam {
    syn.LifetimeParam {
        attrs: Vec.new(),
        lifetime: syn.Lifetime.new("'place", Span.call_site()),
        colon_token: null,
        bounds: Punctuated.new(),
    }
}
