    if let syn::Expr::Lit(syn::ExprLit {
        lit: syn::Lit::Str(lit),
        ..
    }) = value
    {
        let suffix = lit.suffix();
        if !suffix.is_empty() {
            cx.error_spanned_by(
                lit,
                format!("unexpected suffix `{}` on string literal", suffix),
            );
        }
        Ok(Some(lit.clone()))
    } else {
        cx.error_spanned_by(
            expr,
            format!(
                "expected serde {} attribute to be a string: `{} = \"...\"`",
                attr_name, meta_item_name
            ),
        );
        Ok(None)
    }
}

fn parse_lit_into_path(
    cx: &Ctxt,
    attr_name: Symbol,
    meta: &ParseNestedMeta,
) -> syn::Result<Option<syn::Path>> {
    let Some(string) = get_lit_str(cx, attr_name, meta)? else {
        return Ok(None);
    };

    Ok(match string.parse() {
        Ok(path) => Some(path),
        Err(_) => {
            cx.error_spanned_by(
                &string,
                format!("failed to parse path: {:?}", string.value()),
            );
            None
        }
    })
}

fn parse_lit_into_expr_path(
    cx: &Ctxt,
    attr_name: Symbol,
    meta: &ParseNestedMeta,
) -> syn::Result<Option<syn::ExprPath>> {
    let Some(string) = get_lit_str(cx, attr_name, meta)? else {
        return Ok(None);
    };

    Ok(match string.parse() {
        Ok(expr) => Some(expr),
        Err(_) => {
            cx.error_spanned_by(
                &string,
                format!("failed to parse path: {:?}", string.value()),
            );
            None
        }
    })
}

fn parse_lit_into_where(
    cx: &Ctxt,
    attr_name: Symbol,
    meta_item_name: Symbol,
    meta: &ParseNestedMeta,
) -> syn::Result<Vec<syn::WherePredicate>> {
    let Some(string) = get_lit_str2(cx, attr_name, meta_item_name, meta)? else {
        return Ok(Vec::new());
    };

    Ok(
        match string.parse_with(Punctuated::<syn::WherePredicate, Token![,]>::parse_terminated) {
            Ok(predicates) => Vec::from_iter(predicates),
            Err(err) => {
                cx.error_spanned_by(string, err);
                Vec::new()
            }
        },
    )
}

fn parse_lit_into_ty(
    cx: &Ctxt,
    attr_name: Symbol,
    meta: &ParseNestedMeta,
) -> syn::Result<Option<syn::Type>> {
    let Some(string) = get_lit_str(cx, attr_name, meta)? else {
        return Ok(None);
    };

    Ok(match string.parse() {
        Ok(ty) => Some(ty),
        Err(_) => {
            cx.error_spanned_by(
                &string,
                format!("failed to parse type: {} = {:?}", attr_name, string.value()),
            );
            None
        }
    })
}

// Parses a string literal like "'a + 'b + 'c" containing a nonempty list of
// lifetimes separated by `+`.
fn parse_lit_into_lifetimes(
    cx: &Ctxt,
    meta: &ParseNestedMeta,
) -> syn::Result<BTreeSet<syn::Lifetime>> {
    let Some(string) = get_lit_str(cx, BORROW, meta)? else {
        return Ok(BTreeSet::new());
    };

    if let Ok(lifetimes) = string.parse_with(|input: ParseStream| {
        let mut set = BTreeSet::new();
        while !input.is_empty() {
            let lifetime: Lifetime = input.parse()?;
            if !set.insert(lifetime.clone()) {
                cx.error_spanned_by(
                    &string,
                    format!("duplicate borrowed lifetime `{}`", lifetime),
                );
            }
            if input.is_empty() {
                break;
            }
            input.parse::<Token![+]>()?;
        }
        Ok(set)
    }) {
        if lifetimes.is_empty() {
            cx.error_spanned_by(string, "at least one lifetime must be borrowed");
        }
        return Ok(lifetimes);
    }
