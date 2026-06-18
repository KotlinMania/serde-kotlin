package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.SynType

public fun check(cx: Ctxt, cont: Container, derive: Derive) {
    checkDefaultOnTuple(cx, cont)
    checkRemoteGeneric(cx, cont)
    checkGetter(cx, cont)
    checkFlatten(cx, cont)
    checkIdentifier(cx, cont)
    checkVariantSkipAttrs(cx, cont)
    checkInternalTagFieldNameConflict(cx, cont)
    checkAdjacentTagConflict(cx, cont)
    checkTransparent(cx, cont, derive)
    checkFromAndTryFrom(cx, cont)
}

private fun checkDefaultOnTuple(cx: Ctxt, cont: Container) {
    if (cont.attrs.default() == Default.null) {
        val data = cont.data
        if (data is Data.Struct && data.style == Style.Tuple) {
            var firstDefaultIndex: Int? = null
            for ((i, field) in data.fields.withIndex()) {
                if (field.attrs.skipDeserializing()) {
                    continue
                }
                if (field.attrs.default() == Default.null) {
                    if (firstDefaultIndex != null) {
                        cx.errorSpannedBy(
                            field.ty,
                            "field must have #[serde(default)] because previous field `#`firstDefaultIndex has #[serde(default)]"
                        )
                    }
                    continue
                }
                if (firstDefaultIndex == null) {
                    firstDefaultIndex = i
                }
            }
        }
    }
}

private fun checkRemoteGeneric(cx: Ctxt, cont: Container) {
    val remote = cont.attrs.remote()
    if (remote != null) {
        val localHasGeneric = cont.generics.params.isNotEmpty()
        val remoteHasGeneric = remote.segments.lastOrNull()?.arguments != syn.PathArguments.null
        if (localHasGeneric && remoteHasGeneric) {
            cx.errorSpannedBy(remote, "remove generic parameters from this path")
        }
    }
}

private fun checkGetter(cx: Ctxt, cont: Container) {
    when (cont.data) {
        is Data.Enum -> {
            if (cont.data.hasGetter()) {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(getter = \"...\")] is not allowed in an enum"
                )
            }
        }
        is Data.Struct -> {
            if (cont.data.hasGetter() && cont.attrs.remote() == null) {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(getter = \"...\")] can only be used in structs that have #[serde(remote = \"...\")]"
                )
            }
        }
    }
}

private fun checkFlatten(cx: Ctxt, cont: Container) {
    when (val data = cont.data) {
        is Data.Enum -> {
            for (variant in data.variants) {
                for (field in variant.fields) {
                    checkFlattenField(cx, variant.style, field)
                }
            }
        }
        is Data.Struct -> {
            for (field in data.fields) {
                checkFlattenField(cx, data.style, field)
            }
        }
    }
}

private fun checkFlattenField(cx: Ctxt, style: Style, field: Field) {
    if (!field.attrs.flatten()) {
        return
    }
    when (style) {
        Style.Tuple -> {
            cx.errorSpannedBy(
                field.original,
                "#[serde(flatten)] cannot be used on tuple structs"
            )
        }
        Style.Newtype -> {
            cx.errorSpannedBy(
                field.original,
                "#[serde(flatten)] cannot be used on newtype structs"
            )
        }
        else -> {}
    }
}

private fun checkIdentifier(cx: Ctxt, cont: Container) {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return
    }

    for ((i, variant) in variants.withIndex()) {
        val isVariantIdent = cont.attrs.identifier() == Identifier.Variant
        val isFieldIdent = cont.attrs.identifier() == Identifier.Field
        val isNoIdent = cont.attrs.identifier() == Identifier.No

        val isOther = variant.attrs.other()
        val isUntagged = cont.attrs.tag() == TagType.null

        if (isVariantIdent && isOther) {
            cx.errorSpannedBy(
                variant.original,
                "#[serde(other)] may not be used on a variant identifier"
            )
        }

        if (isNoIdent && isOther && isUntagged) {
            cx.errorSpannedBy(
                variant.original,
                "#[serde(other)] cannot appear on untagged enum"
            )
        }

        if ((variant.style == Style.Unit && isFieldIdent && isOther) || (variant.style == Style.Unit && isNoIdent && isOther)) {
            if (i < variants.size - 1) {
                cx.errorSpannedBy(
                    variant.original,
                    "#[serde(other)] must be on the last variant"
                )
            }
        }

        if ((isFieldIdent && isOther) || (isNoIdent && isOther)) {
            if (variant.style != Style.Unit) {
                cx.errorSpannedBy(
                    variant.original,
                    "#[serde(other)] must be on a unit variant"
                )
            }
        }

        if (variant.style == Style.Newtype && isFieldIdent && !isOther) {
            if (i < variants.size - 1) {
                cx.errorSpannedBy(
                    variant.original,
                    "``#`{variant.ident}` must be the last variant"
                )
            }
        } else if (isFieldIdent && !isOther && variant.style != Style.Unit) {
            cx.errorSpannedBy(
                variant.original,
                "#[serde(field_identifier)] may only contain unit variants"
            )
        } else if (isVariantIdent && !isOther && variant.style != Style.Unit) {
            cx.errorSpannedBy(
                variant.original,
                "#[serde(variant_identifier)] may only contain unit variants"
            )
        }
    }
}

private fun checkVariantSkipAttrs(cx: Ctxt, cont: Container) {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return
    }

    for (variant in variants) {
        if (variant.attrs.serializeWith() != null) {
            if (variant.attrs.skipSerializing()) {
                cx.errorSpannedBy(
                    variant.original,
                    "variant ``#`{variant.ident}` cannot have both #[serde(serialize_with)] and #[serde(skip_serializing)]"
                )
            }

            for (field in variant.fields) {
                val member = memberMessage(field.member)

                if (field.attrs.skipSerializing()) {
                    cx.errorSpannedBy(
                        variant.original,
                        "variant ``#`{variant.ident}` cannot have both #[serde(serialize_with)] and a field `#`member marked with #[serde(skip_serializing)]"
                    )
                }

                if (field.attrs.skipSerializingIf() != null) {
                    cx.errorSpannedBy(
                        variant.original,
                        "variant ``#`{variant.ident}` cannot have both #[serde(serialize_with)] and a field `#`member marked with #[serde(skip_serializing_if)]"
                    )
                }
            }
        }

        if (variant.attrs.deserializeWith() != null) {
            if (variant.attrs.skipDeserializing()) {
                cx.errorSpannedBy(
                    variant.original,
                    "variant ``#`{variant.ident}` cannot have both #[serde(deserialize_with)] and #[serde(skip_deserializing)]"
                )
            }

            for (field in variant.fields) {
                if (field.attrs.skipDeserializing()) {
                    val member = memberMessage(field.member)

                    cx.errorSpannedBy(
                        variant.original,
                        "variant ``#`{variant.ident}` cannot have both #[serde(deserialize_with)] and a field `#`member marked with #[serde(skip_deserializing)]"
                    )
                }
            }
        }
    }
}

private fun checkInternalTagFieldNameConflict(cx: Ctxt, cont: Container) {
    val variants = when (val data = cont.data) {
        is Data.Enum -> data.variants
        is Data.Struct -> return
    }

    val tag = when (val t = cont.attrs.tag()) {
        is TagType.Internal -> t.tag
        else -> return
    }

    fun diagnoseConflict() {
        cx.errorSpannedBy(
            cont.original,
            "variant field name ``#`tag` conflicts with internal tag"
        )
    }

    for (variant in variants) {
        when (variant.style) {
            Style.Struct -> {
                if (variant.attrs.untagged()) {
                    continue
                }
                for (field in variant.fields) {
                    val checkSer = !(field.attrs.skipSerializing() || variant.attrs.skipSerializing())
                    val checkDe = !(field.attrs.skipDeserializing() || variant.attrs.skipDeserializing())
                    val name = field.attrs.name()
                    val serName = name.serializeName()

                    if (checkSer && serName.value == tag) {
                        diagnoseConflict()
                        return
                    }

                    for (deName in field.attrs.aliases()) {
                        if (checkDe && deName.value == tag) {
                            diagnoseConflict()
                            return
                        }
                    }
                }
            }
            Style.Unit, Style.Newtype, Style.Tuple -> {}
        }
    }
}

private fun checkAdjacentTagConflict(cx: Ctxt, cont: Container) {
    val (typeTag, contentTag) = when (val t = cont.attrs.tag()) {
        is TagType.Adjacent -> Pair(t.tag, t.content)
        else -> return
    }

    if (typeTag == contentTag) {
        cx.errorSpannedBy(
            cont.original,
            "enum tags ``#`typeTag` for type and content conflict with each other"
        )
    }
}

private fun checkTransparent(cx: Ctxt, cont: Container, derive: Derive) {
    if (!cont.attrs.transparent()) {
        return
    }

    if (cont.attrs.typeFrom() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(transparent)] is not allowed with #[serde(from = \"...\")]"
        )
    }

    if (cont.attrs.typeTryFrom() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(transparent)] is not allowed with #[serde(try_from = \"...\")]"
        )
    }

    if (cont.attrs.typeInto() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(transparent)] is not allowed with #[serde(into = \"...\")]"
        )
    }

    val fields = when (val data = cont.data) {
        is Data.Enum -> {
            cx.errorSpannedBy(
                cont.original,
                "#[serde(transparent)] is not allowed on an enum"
            )
            return
        }
        is Data.Struct -> {
            if (data.style == Style.Unit) {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] is not allowed on a unit struct"
                )
                return
            }
            data.fields
        }
    }

    var transparentField: Field? = null

    for (field in fields) {
        if (allowTransparent(field, derive)) {
            if (transparentField != null) {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] requires struct to have at most one transparent field"
                )
                return
            }
            transparentField = field
        }
    }

    if (transparentField != null) {
        transparentField.attrs.markTransparent()
    } else {
        when (derive) {
            Derive.Serialize -> {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] requires at least one field that is not skipped"
                )
            }
            Derive.Deserialize -> {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] requires at least one field that is neither skipped nor has a default"
                )
            }
        }
    }
}

private fun memberMessage(member: Member): String {
    return when (member) {
        is Member.Named -> "``#`{member.ident}`"
        is Member.Unnamed -> "#`#`{member.index.index}"
    }
}

private fun allowTransparent(field: Field, derive: Derive): Boolean {
    val ty = ungroup(field.ty)
    if (ty is SynType.Path) {
        if (ty.path.segments.lastOrNull()?.ident?.toString() == "PhantomData") {
            return false
        }
    }

    return when (derive) {
        Derive.Serialize -> !field.attrs.skipSerializing()
        Derive.Deserialize -> !field.attrs.skipDeserializing() && field.attrs.default() == Default.null
    }
}

private fun checkFromAndTryFrom(cx: Ctxt, cont: Container) {
    if (cont.attrs.typeFrom() != null && cont.attrs.typeTryFrom() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(from = \"...\")] and #[serde(try_from = \"...\")] conflict with each other"
        )
    }
}

