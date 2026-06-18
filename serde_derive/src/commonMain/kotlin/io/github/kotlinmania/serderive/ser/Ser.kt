package io.github.kotlinmania.serderive

import io.github.kotlinmania.serderive.de.field_i
import io.github.kotlinmania.serderive.deprecated.allow_deprecated
import io.github.kotlinmania.serderive.fragment.Fragment
import io.github.kotlinmania.serderive.fragment.Match
import io.github.kotlinmania.serderive.fragment.Stmts
import io.github.kotlinmania.serderive.internals.ast.Container
import io.github.kotlinmania.serderive.internals.ast.Data
import io.github.kotlinmania.serderive.internals.ast.Field
import io.github.kotlinmania.serderive.internals.ast.Style
import io.github.kotlinmania.serderive.internals.ast.Variant
import io.github.kotlinmania.serderive.internals.name.Name
import io.github.kotlinmania.serderive.internals.attr
import io.github.kotlinmania.serderive.internals.replace_receiver
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
import io.github.kotlinmania.syn.spanned.Spanned
import io.github.kotlinmania.syn.parse_quote
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Index
import io.github.kotlinmania.syn.Member

public fun expand_derive_serialize(input: var syn.DeriveInput) : TokenStream {
    replace_receiver(input);

    val ctxt = Ctxt.new();
    val cont = Container.from_ast(ctxt, input, Derive.Serialize, private.ident()) else {
        return Err(ctxt.check().unwrap_err());
    };
    precondition(ctxt, cont);
    ctxt.check()?;

    val ident = cont.ident;
    val params = Parameters.new(cont);
    let (impl_generics, ty_generics, where_clause) = params.generics.split_for_impl();
    val body = Stmts(serialize_body(cont, params));
    val allow_deprecated = allow_deprecated(input);

    val impl_block = if val remote = cont.attrs.remote() {
        val vis = input.vis;
        val used = pretend.pretend_used(cont, params.is_packed);
        quote("""
            `#`[automatically_derived]
            #allow_deprecated
            impl #impl_generics #ident #ty_generics #where_clause {
                #vis fun serialize<__S>(__self: &#remote #ty_generics, __serializer: __S) -> _serde.#private.Result<__S.Ok, __S.Error>
                where
                    __S: _serde.Serializer,
                {
                    #used
                    #body
                }
            }
        """)
    } else {
        quote("""
            `#`[automatically_derived]
            #allow_deprecated
            impl #impl_generics _serde.Serialize for #ident #ty_generics #where_clause {
                fun serialize<__S>(self, __serializer: __S) -> _serde.#private.Result<__S.Ok, __S.Error>
                where
                    __S: _serde.Serializer,
                {
                    #body
                }
            }
        """)
    };

    Ok(dummy.wrap_in_const(
        cont.attrs.custom_serde_path(),
        impl_block,
    ))
}

fun precondition(cx: Ctxt, cont: Container) {
    when cont.attrs.identifier() {
        attr.Identifier.No -> {}
        attr.Identifier.Field -> {
            cx.error_spanned_by(cont.original, "field identifiers cannot be serialized");
        }
        attr.Identifier.Variant -> {
            cx.error_spanned_by(cont.original, "variant identifiers cannot be serialized");
        }
    }
}

class Parameters(
    val self_var: Ident,
    val this_type: SynPath,
    val this_value: SynPath,
    val generics: SynGenerics,
    val is_remote: Boolean,
    val is_packed: Boolean
)

// impl Parameters  {
    fun new(cont: Container) : this {
        val is_remote = cont.attrs.remote().is_some();
        val self_var = if is_remote {
            Ident.new("__self", Span.call_site())
        } else {
            Ident.new("self", Span.call_site())
        };

        val this_type = this.this_type(cont);
        val this_value = this.this_value(cont);
        val is_packed = cont.attrs.is_packed();
        val generics = build_generics(cont);

        Parameters {
            self_var,
            this_type,
            this_value,
            generics,
            is_remote,
            is_packed,
        }
    }

    /// Type name to use in error messages and `&'static str` arguments to
    /// various Serializer methods.
    fun type_name(self) : String {
        self.this_type.segments.last().unwrap().ident.to_string()
    }
}

// All the generics in the input, plus a bound `T: Serialize` for each generic
// field type that will be serialized by us.
fun build_generics(cont: Container) : syn.Generics {
    val generics = bound.without_defaults(cont.generics);

    val generics =
        bound.with_where_predicates_from_fields(cont, generics, attr.Field.ser_bound);

    val generics =
        bound.with_where_predicates_from_variants(cont, generics, attr.Variant.ser_bound);

    when cont.attrs.ser_bound() {
        predicates -> bound.with_where_predicates(generics, predicates),
        null -> bound.with_bound(
            cont,
            generics,
            needs_serialize_bound,
            parse_quote(""" _serde.Serialize """),
        ),
    }
}

// Fields with a `skip_serializing` or `serialize_with` attribute, or which
// belong to a variant with a `skip_serializing` or `serialize_with` attribute,
// are not serialized by us so we do not generate a bound. Fields with a `bound`
// attribute specify their own bound so we do not generate one. All other fields
// may need a `T: Serialize` bound where T is the type of the field.
fun needs_serialize_bound(field: attr.Field, variant: attr.Variant?) : bool {
    !field.skip_serializing()
        && field.serialize_with().is_none()
        && field.ser_bound().is_none()
        && variant.map_or(true, |variant| {
            !variant.skip_serializing()
                && variant.serialize_with().is_none()
                && variant.ser_bound().is_none()
        })
}

fun serialize_body(cont: Container, params: Parameters) : Fragment {
    if cont.attrs.transparent() {
        serialize_transparent(cont, params)
    } else if val type_into = cont.attrs.type_into() {
        serialize_into(params, type_into)
    } else {
        when (cont.data ) {
            Data.Enum(variants) -> serialize_enum(params, variants, cont.attrs),
            Data.Struct(Style.Struct, fields) -> serialize_struct(params, fields, cont.attrs),
            Data.Struct(Style.Tuple, fields) -> {
                serialize_tuple_struct(params, fields, cont.attrs)
            }
            Data.Struct(Style.Newtype, fields) -> {
                serialize_newtype_struct(params, fields[0], cont.attrs)
            }
            Data.Struct(Style.Unit, _) -> serialize_unit_struct(cont.attrs),
        }
    }
}

fun serialize_transparent(cont: Container, params: Parameters) : Fragment {
    val fields = when (cont.data ) {
        Data.Struct(_, fields) -> fields,
        Data.Enum(_) -> unreachable!(),
    };

    val self_var = params.self_var;
    val transparent_field = fields.iter().find(|f| f.attrs.transparent()).unwrap();
    val member = transparent_field.member;

    val path = when transparent_field.attrs.serialize_with() {
        path -> quote(""" #path """),
        null -> {
            val span = transparent_field.original.span();
            quote_spanned(span, """_serde.Serialize.serialize """)
        }
    };

    quote_block! {
        #path(&#self_var.#member, __serializer)
    }
}

fun serialize_into(params: Parameters, type_into: syn.Type) : Fragment {
    val self_var = params.self_var;
    quote_block! {
        _serde.Serialize.serialize(
            _serde.#private.Into.<#type_into>.into(_serde.#private.Clone.clone(#self_var)),
            __serializer)
    }
}

fun serialize_unit_struct(cattrs: attr.Container) : Fragment {
    val type_name = cattrs.name().serialize_name();

    quote_expr! {
        _serde.Serializer.serialize_unit_struct(__serializer, #type_name)
    }
}

fun serialize_newtype_struct(
    params: Parameters,
    field: Field,
    cattrs: attr.Container,
) : Fragment {
    val type_name = cattrs.name().serialize_name();

    val var field_expr = get_member(
        params,
        field,
        Member.Unnamed(Index {
            index: 0,
            span: Span.call_site(),
        }),
    );
    if val path = field.attrs.serialize_with() {
        field_expr = wrap_serialize_field_with(params, field.ty, path, field_expr);
    }

    val span = field.original.span();
    val func = quote_spanned(span, """_serde.Serializer.serialize_newtype_struct """);
    quote_expr! {
        #func(__serializer, #type_name, #field_expr)
    }
}

fun serialize_tuple_struct(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
) : Fragment {
    val serialize_stmts =
        serialize_tuple_struct_visitor(fields, params, false, TupleTrait.SerializeTupleStruct);

    val type_name = cattrs.name().serialize_name();

    val var serialized_fields = fields
        .iter()
        .enumerate()
        .filter(|(_, field)| !field.attrs.skip_serializing())
        .peekable();

    val let_mut = mut_if(serialized_fields.peek().is_some());

    val len = serialized_fields
        .map(|(i, field)| when field.attrs.skip_serializing_if() {
            null -> quote(""" 1 """),
            path -> {
                val index = syn.Index {
                    index: i as u32,
                    span: Span.call_site(),
                };
                val field_expr = get_member(params, field, Member.Unnamed(index));
                quote(""" if #path(#field_expr """) { 0 } else { 1 })
            }
        })
        .fold(quote(""" 0 """), |sum, expr| quote(""" #sum + #expr """));

    quote_block! {
        let #let_mut __serde_state = _serde.Serializer.serialize_tuple_struct(__serializer, #type_name, #len)?;
        #(#serialize_stmts)*
        _serde.ser.SerializeTupleStruct.end(__serde_state)
    }
}

fun serialize_struct(params: Parameters, fields: &[Field], cattrs: attr.Container) : Fragment {
    assert!(
        fields.len() as u64 <= u64.from(u32.MAX),
        "too many fields in {}: {}, maximum supported count is {}",
        cattrs.name().serialize_name(),
        fields.len(),
        u32.MAX,
    );

    val has_non_skipped_flatten = fields
        .iter()
        .any(|field| field.attrs.flatten() && !field.attrs.skip_serializing());
    if has_non_skipped_flatten {
        serialize_struct_as_map(params, fields, cattrs)
    } else {
        serialize_struct_as_struct(params, fields, cattrs)
    }
}

fun serialize_struct_tag_field(cattrs: attr.Container, struct_trait: StructTrait) : TokenStream {
    when cattrs.tag() {
        attr.TagType.Internal { tag } -> {
            val type_name = cattrs.name().serialize_name();
            val func = struct_trait.serialize_field(Span.call_site());
            quote("""
                #func(var __serde_state, #tag, #type_name)?;
            """)
        }
        _ -> quote(""""""),
    }
}

fun serialize_struct_as_struct(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
) : Fragment {
    val serialize_fields =
        serialize_struct_visitor(fields, params, false, StructTrait.SerializeStruct);

    val type_name = cattrs.name().serialize_name();

    val tag_field = serialize_struct_tag_field(cattrs, StructTrait.SerializeStruct);
    val tag_field_exists = !tag_field.is_empty();

    val var serialized_fields = fields
        .iter()
        .filter(|field| !field.attrs.skip_serializing())
        .peekable();

    val let_mut = mut_if(serialized_fields.peek().is_some() || tag_field_exists);

    val len = serialized_fields
        .map(|field| when field.attrs.skip_serializing_if() {
            null -> quote(""" 1 """),
            path -> {
                val field_expr = get_member(params, field, field.member);
                quote(""" if #path(#field_expr """) { 0 } else { 1 })
            }
        })
        .fold(
            quote(""" #tag_field_exists as usize """),
            |sum, expr| quote(""" #sum + #expr """),
        );

    quote_block! {
        let #let_mut __serde_state = _serde.Serializer.serialize_struct(__serializer, #type_name, #len)?;
        #tag_field
        #(#serialize_fields)*
        _serde.ser.SerializeStruct.end(__serde_state)
    }
}

fun serialize_struct_as_map(
    params: Parameters,
    fields: &[Field],
    cattrs: attr.Container,
) : Fragment {
    val serialize_fields =
        serialize_struct_visitor(fields, params, false, StructTrait.SerializeMap);

    val tag_field = serialize_struct_tag_field(cattrs, StructTrait.SerializeMap);
    val tag_field_exists = !tag_field.is_empty();

    val var serialized_fields = fields
        .iter()
        .filter(|field| !field.attrs.skip_serializing())
        .peekable();

    val let_mut = mut_if(serialized_fields.peek().is_some() || tag_field_exists);

    quote_block! {
        let #let_mut __serde_state = _serde.Serializer.serialize_map(__serializer, _serde.#private.null)?;
        #tag_field
        #(#serialize_fields)*
        _serde.ser.SerializeMap.end(__serde_state)
    }
}

fun serialize_enum(params: Parameters, variants: &[Variant], cattrs: attr.Container) : Fragment {
    assert!(variants.len() as u64 <= u64.from(u32.MAX));

    val self_var = params.self_var;

    val var arms: List<_> = variants
        .iter()
        .enumerate()
        .map(|(variant_index, variant)| {
            serialize_variant(params, variant, variant_index as u32, cattrs)
        })
        .collect();

    if cattrs.remote().is_some() && cattrs.non_exhaustive() {
        arms.push(quote("""
            ref unrecognized -> _serde.#private.Err(_serde.ser.Error.custom(_serde.#private.ser.CannotSerializeVariant(unrecognized))),
        """));
    }

    quote_expr! {
        when (*#self_var ) {
            #(#arms)*
        }
    }
}

fun serialize_variant(
    params: Parameters,
    variant: Variant,
    variant_index: u32,
    cattrs: attr.Container,
) : TokenStream {
    val this_value = params.this_value;
    val variant_ident = variant.ident;

    if variant.attrs.skip_serializing() {
        val skipped_msg = format!(
            "the enum variant {}.{} cannot be serialized",
            params.type_name(),
            variant_ident
        );
        val skipped_err = quote("""
            _serde.#private.Err(_serde.ser.Error.custom(#skipped_msg))
        """);
        val fields_pat = when variant.style {
            Style.Unit -> quote("""  """),
            Style.Newtype | Style.Tuple -> quote(""" (.. """)),
            Style.Struct -> quote(""" { .. } """),
        };
        quote("""
            #this_value.#variant_ident #fields_pat -> #skipped_err,
        """)
    } else {
        // variant wasn't skipped
        val case = when variant.style {
            Style.Unit -> {
                quote("""
                    #this_value.#variant_ident
                """)
            }
            Style.Newtype -> {
                quote("""
                    #this_value.#variant_ident(ref __field0)
                """)
            }
            Style.Tuple -> {
                val field_names = (0..variant.fields.len()).map(field_i);
                quote("""
                    #this_value.#variant_ident(#(ref #field_names),*)
                """)
            }
            Style.Struct -> {
                val members = variant.fields.iter().map(|f| f.member);
                quote("""
                    #this_value.#variant_ident { #(ref #members),* }
                """)
            }
        };

        val body = Match(when ((cattrs.tag(), variant.attrs.untagged()) ) {
            (attr.TagType.External, false) -> {
                serialize_externally_tagged_variant(params, variant, variant_index, cattrs)
            }
            (attr.TagType.Internal { tag }, false) -> {
                serialize_internally_tagged_variant(params, variant, cattrs, tag)
            }
            (attr.TagType.Adjacent { tag, content }, false) -> {
                serialize_adjacently_tagged_variant(
                    params,
                    variant,
                    cattrs,
                    variant_index,
                    tag,
                    content,
                )
            }
            (attr.TagType.null, _) | (_, true) -> {
                serialize_untagged_variant(params, variant, cattrs)
            }
        });

        quote("""
            #case -> #body
        """)
    }
}

fun serialize_externally_tagged_variant(
    params: Parameters,
    variant: Variant,
    variant_index: u32,
    cattrs: attr.Container,
) : Fragment {
    val type_name = cattrs.name().serialize_name();
    val variant_name = variant.attrs.name().serialize_name();

    if val path = variant.attrs.serialize_with() {
        val ser = wrap_serialize_variant_with(params, path, variant);
        return quote_expr! {
            _serde.Serializer.serialize_newtype_variant(
                __serializer,
                #type_name,
                #variant_index,
                #variant_name,
                #ser,
            )
        };
    }

    when effective_style(variant) {
        Style.Unit -> {
            quote_expr! {
                _serde.Serializer.serialize_unit_variant(
                    __serializer,
                    #type_name,
                    #variant_index,
                    #variant_name,
                )
            }
        }
        Style.Newtype -> {
            val field = variant.fields[0];
            val var field_expr = quote(""" __field0 """);
            if val path = field.attrs.serialize_with() {
                field_expr = wrap_serialize_field_with(params, field.ty, path, field_expr);
            }

            val span = field.original.span();
            val func = quote_spanned(span, """_serde.Serializer.serialize_newtype_variant """);
            quote_expr! {
                #func(
                    __serializer,
                    #type_name,
                    #variant_index,
                    #variant_name,
                    #field_expr,
                )
            }
        }
        Style.Tuple -> serialize_tuple_variant(
            TupleVariant.ExternallyTagged {
                type_name,
                variant_index,
                variant_name,
            },
            params,
            variant.fields,
        ),
        Style.Struct -> serialize_struct_variant(
            StructVariant.ExternallyTagged {
                variant_index,
                variant_name,
            },
            params,
            variant.fields,
            type_name,
        ),
    }
}

fun serialize_internally_tagged_variant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container,
    tag: str,
) : Fragment {
    val type_name = cattrs.name().serialize_name();
    val variant_name = variant.attrs.name().serialize_name();

    val enum_ident_str = params.type_name();
    val variant_ident_str = variant.ident.to_string();

    if val path = variant.attrs.serialize_with() {
        val ser = wrap_serialize_variant_with(params, path, variant);
        return quote_expr! {
            _serde.#private.ser.serialize_tagged_newtype(
                __serializer,
                #enum_ident_str,
                #variant_ident_str,
                #tag,
                #variant_name,
                #ser,
            )
        };
    }

    when effective_style(variant) {
        Style.Unit -> {
            quote_block! {
                val var __struct = _serde.Serializer.serialize_struct(
                    __serializer, #type_name, 1)?;
                _serde.ser.SerializeStruct.serialize_field(
                    var __struct, #tag, #variant_name)?;
                _serde.ser.SerializeStruct.end(__struct)
            }
        }
        Style.Newtype -> {
            val field = variant.fields[0];
            val var field_expr = quote(""" __field0 """);
            if val path = field.attrs.serialize_with() {
                field_expr = wrap_serialize_field_with(params, field.ty, path, field_expr);
            }

            val span = field.original.span();
            val func = quote_spanned(span, """_serde.#private.ser.serialize_tagged_newtype """);
            quote_expr! {
                #func(
                    __serializer,
                    #enum_ident_str,
                    #variant_ident_str,
                    #tag,
                    #variant_name,
                    #field_expr,
                )
            }
        }
        Style.Struct -> serialize_struct_variant(
            StructVariant.InternallyTagged { tag, variant_name },
            params,
            variant.fields,
            type_name,
        ),
        Style.Tuple -> unreachable!("checked in serde_derive_internals"),
    }
}

fun serialize_adjacently_tagged_variant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container,
    variant_index: u32,
    tag: str,
    content: str,
) : Fragment {
    val this_type = params.this_type;
    val type_name = cattrs.name().serialize_name();
    val variant_name = variant.attrs.name().serialize_name();
    val serialize_variant = quote("""
        _serde.#private.ser.AdjacentlyTaggedEnumVariant {
            enum_name: #type_name,
            variant_index: #variant_index,
            variant_name: #variant_name,
        }
    """);

    val inner = Stmts(if val path = variant.attrs.serialize_with() {
        val ser = wrap_serialize_variant_with(params, path, variant);
        quote_expr! {
            _serde.Serialize.serialize(#ser, __serializer)
        }
    } else {
        when effective_style(variant) {
            Style.Unit -> {
                return quote_block! {
                    val var __struct = _serde.Serializer.serialize_struct(
                        __serializer, #type_name, 1)?;
                    _serde.ser.SerializeStruct.serialize_field(
                        var __struct, #tag, #serialize_variant)?;
                    _serde.ser.SerializeStruct.end(__struct)
                };
            }
            Style.Newtype -> {
                val field = variant.fields[0];
                val var field_expr = quote(""" __field0 """);
                if val path = field.attrs.serialize_with() {
                    field_expr = wrap_serialize_field_with(params, field.ty, path, field_expr);
                }

                val span = field.original.span();
                val func = quote_spanned(span, """_serde.ser.SerializeStruct.serialize_field """);
                return quote_block! {
                    val var __struct = _serde.Serializer.serialize_struct(
                        __serializer, #type_name, 2)?;
                    _serde.ser.SerializeStruct.serialize_field(
                        var __struct, #tag, #serialize_variant)?;
                    #func(
                        var __struct, #content, #field_expr)?;
                    _serde.ser.SerializeStruct.end(__struct)
                };
            }
            Style.Tuple -> {
                serialize_tuple_variant(TupleVariant.Untagged, params, variant.fields)
            }
            Style.Struct -> serialize_struct_variant(
                StructVariant.Untagged,
                params,
                variant.fields,
                variant_name,
            ),
        }
    });

    val fields_ty = variant.fields.iter().map(|f| f.ty);
    val fields_ident: &[_] = when variant.style {
        Style.Unit -> {
            if variant.attrs.serialize_with().is_some() {
                vec![]
            } else {
                unreachable!()
            }
        }
        Style.Newtype -> vec![Member.Named(field_i(0))],
        Style.Tuple -> (0..variant.fields.len())
            .map(|i| Member.Named(field_i(i)))
            .collect(),
        Style.Struct -> variant.fields.iter().map(|f| f.member.clone()).collect(),
    };

    let (_, ty_generics, where_clause) = params.generics.split_for_impl();

    val wrapper_generics = if fields_ident.is_empty() {
        params.generics.clone()
    } else {
        bound.with_lifetime_bound(params.generics, "'__a")
    };
    let (wrapper_impl_generics, wrapper_ty_generics, _) = wrapper_generics.split_for_impl();

    quote_block! {
        `#`[doc(hidden)]
        struct __AdjacentlyTagged #wrapper_generics #where_clause {
            data: (#(&'__a #fields_ty,)*),
            phantom: _serde.#private.PhantomData<#this_type #ty_generics>,
        }

        `#`[automatically_derived]
        impl #wrapper_impl_generics _serde.Serialize for __AdjacentlyTagged #wrapper_ty_generics #where_clause {
            fun serialize<__S>(self, __serializer: __S) -> _serde.#private.Result<__S.Ok, __S.Error>
            where
                __S: _serde.Serializer,
            {
                // Elements that have skip_serializing will be unused.
                `#`[allow(unused_variables)]
                let (#(#fields_ident,)*) = self.data;
                #inner
            }
        }

        val var __struct = _serde.Serializer.serialize_struct(
            __serializer, #type_name, 2)?;
        _serde.ser.SerializeStruct.serialize_field(
            var __struct, #tag, #serialize_variant)?;
        _serde.ser.SerializeStruct.serialize_field(
            var __struct, #content, __AdjacentlyTagged {
                data: (#(#fields_ident,)*),
                phantom: _serde.#private.PhantomData.<#this_type #ty_generics>,
            })?;
        _serde.ser.SerializeStruct.end(__struct)
    }
}

fun serialize_untagged_variant(
    params: Parameters,
    variant: Variant,
    cattrs: attr.Container,
) : Fragment {
    if val path = variant.attrs.serialize_with() {
        val ser = wrap_serialize_variant_with(params, path, variant);
        return quote_expr! {
            _serde.Serialize.serialize(#ser, __serializer)
        };
    }

    when effective_style(variant) {
        Style.Unit -> {
            quote_expr! {
                _serde.Serializer.serialize_unit(__serializer)
            }
        }
        Style.Newtype -> {
            val field = variant.fields[0];
            val var field_expr = quote(""" __field0 """);
            if val path = field.attrs.serialize_with() {
                field_expr = wrap_serialize_field_with(params, field.ty, path, field_expr);
            }

            val span = field.original.span();
            val func = quote_spanned(span, """_serde.Serialize.serialize """);
            quote_expr! {
                #func(#field_expr, __serializer)
            }
        }
        Style.Tuple -> serialize_tuple_variant(TupleVariant.Untagged, params, variant.fields),
        Style.Struct -> {
            val type_name = cattrs.name().serialize_name();
            serialize_struct_variant(StructVariant.Untagged, params, variant.fields, type_name)
        }
    }
}

enum class TupleVariant {

    ExternallyTagged {
        type_name: &'a Name,
        variant_index: u32,
        variant_name: &'a Name,
    
},
    Untagged,
}

fun serialize_tuple_variant(
    context: TupleVariant,
    params: Parameters,
    fields: &[Field],
) : Fragment {
    val tuple_trait = when context {
        TupleVariant.ExternallyTagged { .. } -> TupleTrait.SerializeTupleVariant,
        TupleVariant.Untagged -> TupleTrait.SerializeTuple,
    };

    val serialize_stmts = serialize_tuple_struct_visitor(fields, params, true, tuple_trait);

    val var serialized_fields = fields
        .iter()
        .enumerate()
        .filter(|(_, field)| !field.attrs.skip_serializing())
        .peekable();

    val let_mut = mut_if(serialized_fields.peek().is_some());

    val len = serialized_fields
        .map(|(i, field)| when field.attrs.skip_serializing_if() {
            null -> quote(""" 1 """),
            path -> {
                val field_expr = field_i(i);
                quote(""" if #path(#field_expr """) { 0 } else { 1 })
            }
        })
        .fold(quote(""" 0 """), |sum, expr| quote(""" #sum + #expr """));

    when context {
        TupleVariant.ExternallyTagged {
            type_name,
            variant_index,
            variant_name,
        } -> {
            quote_block! {
                let #let_mut __serde_state = _serde.Serializer.serialize_tuple_variant(
                    __serializer,
                    #type_name,
                    #variant_index,
                    #variant_name,
                    #len)?;
                #(#serialize_stmts)*
                _serde.ser.SerializeTupleVariant.end(__serde_state)
            }
        }
        TupleVariant.Untagged -> {
            quote_block! {
                let #let_mut __serde_state = _serde.Serializer.serialize_tuple(
                    __serializer,
                    #len)?;
                #(#serialize_stmts)*
                _serde.ser.SerializeTuple.end(__serde_state)
            }
        }
    }
}

enum class StructVariant {

    ExternallyTagged {
        variant_index: u32,
        variant_name: &'a Name,
    
},
    InternallyTagged {
        tag: &'a str,
        variant_name: &'a Name,
    },
    Untagged,
}

fun serialize_struct_variant(
    context: StructVariant,
    params: Parameters,
    fields: &[Field],
    name: Name,
) : Fragment {
    if fields.iter().any(|field| field.attrs.flatten()) {
        return serialize_struct_variant_with_flatten(context, params, fields, name);
    }

    val struct_trait = when context {
        StructVariant.ExternallyTagged { .. } -> StructTrait.SerializeStructVariant,
        StructVariant.InternallyTagged { .. } | StructVariant.Untagged -> {
            StructTrait.SerializeStruct
        }
    };

    val serialize_fields = serialize_struct_visitor(fields, params, true, struct_trait);

    val var serialized_fields = fields
        .iter()
        .filter(|field| !field.attrs.skip_serializing())
        .peekable();

    val let_mut = mut_if(serialized_fields.peek().is_some());

    val len = serialized_fields
        .map(|field| {
            val member = field.member;

            when field.attrs.skip_serializing_if() {
                path -> quote(""" if #path(#member """) { 0 } else { 1 }),
                null -> quote(""" 1 """),
            }
        })
        .fold(quote(""" 0 """), |sum, expr| quote(""" #sum + #expr """));

    when context {
        StructVariant.ExternallyTagged {
            variant_index,
            variant_name,
        } -> {
            quote_block! {
                let #let_mut __serde_state = _serde.Serializer.serialize_struct_variant(
                    __serializer,
                    #name,
                    #variant_index,
                    #variant_name,
                    #len,
                )?;
                #(#serialize_fields)*
                _serde.ser.SerializeStructVariant.end(__serde_state)
            }
        }
        StructVariant.InternallyTagged { tag, variant_name } -> {
            quote_block! {
                val var __serde_state = _serde.Serializer.serialize_struct(
                    __serializer,
                    #name,
                    #len + 1,
                )?;
                _serde.ser.SerializeStruct.serialize_field(
                    var __serde_state,
                    #tag,
                    #variant_name,
                )?;
                #(#serialize_fields)*
                _serde.ser.SerializeStruct.end(__serde_state)
            }
        }
        StructVariant.Untagged -> {
            quote_block! {
                let #let_mut __serde_state = _serde.Serializer.serialize_struct(
                    __serializer,
                    #name,
                    #len,
                )?;
                #(#serialize_fields)*
                _serde.ser.SerializeStruct.end(__serde_state)
            }
        }
    }
}

fun serialize_struct_variant_with_flatten(
    context: StructVariant,
    params: Parameters,
    fields: &[Field],
    name: Name,
) : Fragment {
    val struct_trait = StructTrait.SerializeMap;
    val serialize_fields = serialize_struct_visitor(fields, params, true, struct_trait);

    val var serialized_fields = fields
        .iter()
        .filter(|field| !field.attrs.skip_serializing())
        .peekable();

    val let_mut = mut_if(serialized_fields.peek().is_some());

    when context {
        StructVariant.ExternallyTagged {
            variant_index,
            variant_name,
        } -> {
            val this_type = params.this_type;
            val fields_ty = fields.iter().map(|f| f.ty);
            val members = fields.iter().map(|f| f.member).collect.<List<_>>();

            let (_, ty_generics, where_clause) = params.generics.split_for_impl();
            val wrapper_generics = bound.with_lifetime_bound(params.generics, "'__a");
            let (wrapper_impl_generics, wrapper_ty_generics, _) = wrapper_generics.split_for_impl();

            quote_block! {
                `#`[doc(hidden)]
                struct __EnumFlatten #wrapper_generics #where_clause {
                    data: (#(&'__a #fields_ty,)*),
                    phantom: _serde.#private.PhantomData<#this_type #ty_generics>,
                }

                `#`[automatically_derived]
                impl #wrapper_impl_generics _serde.Serialize for __EnumFlatten #wrapper_ty_generics #where_clause {
                    fun serialize<__S>(self, __serializer: __S) -> _serde.#private.Result<__S.Ok, __S.Error>
                    where
                        __S: _serde.Serializer,
                    {
                        let (#(#members,)*) = self.data;
                        let #let_mut __serde_state = _serde.Serializer.serialize_map(
                            __serializer,
                            _serde.#private.null)?;
                        #(#serialize_fields)*
                        _serde.ser.SerializeMap.end(__serde_state)
                    }
                }

                _serde.Serializer.serialize_newtype_variant(
                    __serializer,
                    #name,
                    #variant_index,
                    #variant_name,
                    __EnumFlatten {
                        data: (#(#members,)*),
                        phantom: _serde.#private.PhantomData.<#this_type #ty_generics>,
                    })
            }
        }
        StructVariant.InternallyTagged { tag, variant_name } -> {
            quote_block! {
                let #let_mut __serde_state = _serde.Serializer.serialize_map(
                    __serializer,
                    _serde.#private.null)?;
                _serde.ser.SerializeMap.serialize_entry(
                    var __serde_state,
                    #tag,
                    #variant_name,
                )?;
                #(#serialize_fields)*
                _serde.ser.SerializeMap.end(__serde_state)
            }
        }
        StructVariant.Untagged -> {
            quote_block! {
                let #let_mut __serde_state = _serde.Serializer.serialize_map(
                    __serializer,
                    _serde.#private.null)?;
                #(#serialize_fields)*
                _serde.ser.SerializeMap.end(__serde_state)
            }
        }
    }
}

fun serialize_tuple_struct_visitor(
    fields: &[Field],
    params: Parameters,
    is_enum: bool,
    tuple_trait: TupleTrait,
) : List<TokenStream> {
    val var dst_fields = Vec.new();

    for (i, field) in fields.iter().enumerate() {
        if field.attrs.skip_serializing() {
            continue;
        }
        val var field_expr = if is_enum {
            val id = field_i(i);
            quote(""" #id """)
        } else {
            get_member(
                params,
                field,
                Member.Unnamed(Index {
                    index: i as u32,
                    span: Span.call_site(),
                }),
            )
        };

        val skip = field
            .attrs
            .skip_serializing_if()
            .map(|path| quote(""" #path(#field_expr """)));

        if val path = field.attrs.serialize_with() {
            field_expr = wrap_serialize_field_with(params, field.ty, path, field_expr);
        }

        val span = field.original.span();
        val func = tuple_trait.serialize_element(span);
        val ser = quote("""
            #func(var __serde_state, #field_expr)?;
        """);

        dst_fields.push(when skip {
            null -> ser,
            skip -> quote(""" if !#skip { #ser } """),
        });
    }
    dst_fields
}

fun serialize_struct_visitor(
    fields: &[Field],
    params: Parameters,
    is_enum: bool,
    struct_trait: StructTrait,
) : List<TokenStream> {
    val var dst_fields = Vec.new();

    for field in fields {
        if field.attrs.skip_serializing() {
            continue;
        }
        val member = field.member;

        val var field_expr = if is_enum {
            quote(""" #member """)
        } else {
            get_member(params, field, member)
        };

        val key_expr = field.attrs.name().serialize_name();

        val skip = field
            .attrs
            .skip_serializing_if()
            .map(|path| quote(""" #path(#field_expr """)));

        if val path = field.attrs.serialize_with() {
            field_expr = wrap_serialize_field_with(params, field.ty, path, field_expr);
        }

        val span = field.original.span();
        val ser = if field.attrs.flatten() {
            val func = quote_spanned(span, """_serde.Serialize.serialize """);
            quote("""
                #func(&#field_expr, _serde.#private.ser.FlatMapSerializer(var __serde_state))?;
            """)
        } else {
            val func = struct_trait.serialize_field(span);
            quote("""
                #func(var __serde_state, #key_expr, #field_expr)?;
            """)
        };

        dst_fields.push(when skip {
            null -> ser,
            skip -> {
                if val skip_func = struct_trait.skip_field(span) {
                    quote("""
                        if !#skip {
                            #ser
                        } else {
                            #skip_func(var __serde_state, #key_expr)?;
                        }
                    """)
                } else {
                    quote("""
                        if !#skip {
                            #ser
                        }
                    """)
                }
            }
        });
    }
    dst_fields
}

fun wrap_serialize_field_with(
    params: Parameters,
    field_ty: syn.Type,
    serialize_with: syn.ExprPath,
    field_expr: TokenStream,
) : TokenStream {
    wrap_serialize_with(params, serialize_with, &[field_ty], &[quote(""" #field_expr """)])
}

fun wrap_serialize_variant_with(
    params: Parameters,
    serialize_with: syn.ExprPath,
    variant: Variant,
) : TokenStream {
    val field_tys: List<_> = variant.fields.iter().map(|field| field.ty).collect();
    val field_exprs: List<_> = variant
        .fields
        .iter()
        .map(|field| {
            val id = when (field.member ) {
                Member.Named(ident) -> ident.clone(),
                Member.Unnamed(member) -> field_i(member.index as usize),
            };
            quote(""" #id """)
        })
        .collect();
    wrap_serialize_with(
        params,
        serialize_with,
        field_tys.as_slice(),
        field_exprs.as_slice(),
    )
}

fun wrap_serialize_with(
    params: Parameters,
    serialize_with: syn.ExprPath,
    field_tys: &[syn.Type],
    field_exprs: &[TokenStream],
) : TokenStream {
    val this_type = params.this_type;
    let (_, ty_generics, where_clause) = params.generics.split_for_impl();

    val wrapper_generics = if field_exprs.is_empty() {
        params.generics.clone()
    } else {
        bound.with_lifetime_bound(params.generics, "'__a")
    };
    let (wrapper_impl_generics, wrapper_ty_generics, _) = wrapper_generics.split_for_impl();

    val field_access = (0..field_exprs.len()).map(|n| {
        Member.Unnamed(Index {
            index: n as u32,
            span: Span.call_site(),
        })
    });

    val self_var = quote(""" self """);
    val serializer_var = quote(""" __s """);

    // If #serialize_with returns wrong type, error will be reported on here.
    // We attach span of the path to this piece so error will be reported
    // on the `#`[serde(with = "...")]
    //                       ^^^^^
    val wrapper_serialize = quote_spanned! {serialize_with.span()->
        #serialize_with(#(#self_var.values.#field_access, )* #serializer_var)
    };

    quote!(&{
        `#`[doc(hidden)]
        struct __SerializeWith #wrapper_impl_generics #where_clause {
            values: (#(&'__a #field_tys, )*),
            phantom: _serde.#private.PhantomData<#this_type #ty_generics>,
        }

        `#`[automatically_derived]
        impl #wrapper_impl_generics _serde.Serialize for __SerializeWith #wrapper_ty_generics #where_clause {
            fun serialize<__S>(&#self_var, #serializer_var: __S) -> _serde.#private.Result<__S.Ok, __S.Error>
            where
                __S: _serde.Serializer,
            {
                #wrapper_serialize
            }
        }

        __SerializeWith {
            values: (#(#field_exprs, )*),
            phantom: _serde.#private.PhantomData.<#this_type #ty_generics>,
        }
    })
}

// Serialization of an empty struct results in code like:
//
//     val var __serde_state = serializer.serialize_struct("S", 0)?;
//     _serde.ser.SerializeStruct.end(__serde_state)
//
// where we want to omit the `mut` to avoid a warning.
fun mut_if(is_mut: bool) : TokenStream? {
    if is_mut {
        quote(""" mut """)
    } else {
        null
    }
}

fun get_member(params: Parameters, field: Field, member: Member) : TokenStream {
    val self_var = params.self_var;
    when ((params.is_remote, field.attrs.getter()) ) {
        (false, null) -> {
            if params.is_packed {
                quote(""" &{#self_var.#member} """)
            } else {
                quote(""" &#self_var.#member """)
            }
        }
        (true, null) -> {
            val inner = if params.is_packed {
                quote(""" &{#self_var.#member} """)
            } else {
                quote(""" &#self_var.#member """)
            };
            val ty = field.ty;
            quote(""" _serde.#private.ser.constrain.<#ty>(#inner """))
        }
        (true, getter) -> {
            val ty = field.ty;
            quote(""" _serde.#private.ser.constrain.<#ty>(&#getter(#self_var """)))
        }
        (false, _) -> {
            unreachable!("getter is only allowed for remote impls");
        }
    }
}

fun effective_style(variant: Variant) : Style {
    when variant.style {
        Style.Newtype if variant.fields[0].attrs.skip_serializing() -> Style.Unit,
        other -> other,
    }
}

enum class StructTrait {

    SerializeMap,
    SerializeStruct,
    SerializeStructVariant,

}

// impl StructTrait  {
    fun serialize_field(self, span: Span) : TokenStream {
        when (this) {
            StructTrait.SerializeMap -> {
                quote_spanned(span, """_serde.ser.SerializeMap.serialize_entry """)
            }
            StructTrait.SerializeStruct -> {
                quote_spanned(span, """_serde.ser.SerializeStruct.serialize_field """)
            }
            StructTrait.SerializeStructVariant -> {
                quote_spanned(span, """_serde.ser.SerializeStructVariant.serialize_field """)
            }
        }
    }

    fun skip_field(self, span: Span) : TokenStream? {
        when (this) {
            StructTrait.SerializeMap -> null,
            StructTrait.SerializeStruct -> {
                quote_spanned(span { _serde.ser.SerializeStruct.skip_field })
            }
            StructTrait.SerializeStructVariant -> {
                quote_spanned(span { _serde.ser.SerializeStructVariant.skip_field })
            }
        }
    }
}

enum class TupleTrait {

    SerializeTuple,
    SerializeTupleStruct,
    SerializeTupleVariant,

}

// impl TupleTrait  {
    fun serialize_element(self, span: Span) : TokenStream {
        when (this) {
            TupleTrait.SerializeTuple -> {
                quote_spanned(span, """_serde.ser.SerializeTuple.serialize_element """)
            }
            TupleTrait.SerializeTupleStruct -> {
                quote_spanned(span, """_serde.ser.SerializeTupleStruct.serialize_field """)
            }
            TupleTrait.SerializeTupleVariant -> {
                quote_spanned(span, """_serde.ser.SerializeTupleVariant.serialize_field """)
            }
        }
    }
}
