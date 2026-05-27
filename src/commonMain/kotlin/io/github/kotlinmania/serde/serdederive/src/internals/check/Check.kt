// port-lint: source serde_derive/src/internals/check.rs
package io.github.kotlinmania.serde.serdederive.src.internals.check

import io.github.kotlinmania.serde.serdederive.src.internals.Ctxt
import io.github.kotlinmania.serde.serdederive.src.internals.Derive
import io.github.kotlinmania.serde.serdederive.src.internals.ast.Container
import io.github.kotlinmania.serde.serdederive.src.internals.ast.Data
import io.github.kotlinmania.serde.serdederive.src.internals.ast.Field
import io.github.kotlinmania.serde.serdederive.src.internals.ast.Style
import io.github.kotlinmania.serde.serdederive.src.internals.attr.Default
import io.github.kotlinmania.serde.serdederive.src.internals.attr.Identifier
import io.github.kotlinmania.serde.serdederive.src.internals.attr.TagType
import io.github.kotlinmania.serde.serdederive.src.internals.ungroup
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.SynType

/**
 * Cross-cutting checks that require looking at more than a single attrs object.
 * Simpler checks should happen when parsing and building the attrs.
 */
fun check(
    cx: Ctxt,
    cont: Container,
    derive: Derive,
) {
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

/**
 * If some field of a tuple struct is marked `serde(default)` then all fields
 * after it must also be marked with that attribute, or the struct must have a
 * container-level `serde(default)` attribute. A field's default value is only
 * used for tuple fields if the sequence is exhausted at that point; that means
 * all subsequent fields will fail to deserialize if they don't have their own
 * default.
 */
private fun checkDefaultOnTuple(
    cx: Ctxt,
    cont: Container,
) {
    if (cont.attrs.default() is Default.None) {
        val data = cont.data
        if (data is Data.Struct && data.style == Style.Tuple) {
            var firstDefaultIndex: Int? = null
            for ((index, field) in data.fields.withIndex()) {
                if (field.attrs.skipDeserializing()) {
                    continue
                }
                if (field.attrs.default() is Default.None) {
                    val first = firstDefaultIndex
                    if (first != null) {
                        cx.errorSpannedBy(
                            field.ty,
                            "field must have #[serde(default)] because previous field $first has #[serde(default)]",
                        )
                    }
                    continue
                }
                if (firstDefaultIndex == null) {
                    firstDefaultIndex = index
                }
            }
        }
    }
}

/**
 * Remote derive definition type must have either all of the generics of the
 * remote type:
 *
 *     serde(remote = "Generic")
 *     class Generic<T>
 *
 * or none of them, i.e. defining impls for one concrete instantiation of the
 * remote type only:
 *
 *     serde(remote = "Generic<T>")
 *     class ConcreteDef
 */
private fun checkRemoteGeneric(
    cx: Ctxt,
    cont: Container,
) {
    val remote = cont.attrs.remote() ?: return
    val localHasGeneric = !cont.generics.params.isEmpty()
    val remoteLast = remote.segments.last()
    val remoteHasGeneric = remoteLast?.arguments?.isNone() == false
    if (localHasGeneric && remoteHasGeneric) {
        cx.errorSpannedBy(remote, "remove generic parameters from this path")
    }
}

/**
 * Getters are only allowed inside structs, not enums, with the `remote`
 * attribute.
 */
private fun checkGetter(
    cx: Ctxt,
    cont: Container,
) {
    when (cont.data) {
        is Data.Enum -> {
            if (cont.data.hasGetter()) {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(getter = \"...\")] is not allowed in an enum",
                )
            }
        }

        is Data.Struct -> {
            if (cont.data.hasGetter() && cont.attrs.remote() == null) {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(getter = \"...\")] can only be used in structs that have #[serde(remote = \"...\")]",
                )
            }
        }
    }
}

/**
 * Flattening has some restrictions we can test.
 */
private fun checkFlatten(
    cx: Ctxt,
    cont: Container,
) {
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

private fun checkFlattenField(
    cx: Ctxt,
    style: Style,
    field: Field,
) {
    if (!field.attrs.flatten()) {
        return
    }
    when (style) {
        Style.Tuple ->
            cx.errorSpannedBy(
                field.original,
                "#[serde(flatten)] cannot be used on tuple structs",
            )

        Style.Newtype ->
            cx.errorSpannedBy(
                field.original,
                "#[serde(flatten)] cannot be used on newtype structs",
            )

        Style.Struct,
        Style.Unit,
        -> Unit
    }
}

/**
 * The `other` attribute must be used at most once and it must be the last
 * variant of an enum.
 *
 * Inside a variant identifier all variants must be unit variants. Inside a
 * field identifier all but possibly one variant must be unit variants. The
 * last variant may be a newtype variant which is an implicit "other" case.
 */
private fun checkIdentifier(
    cx: Ctxt,
    cont: Container,
) {
    val variants =
        when (val data = cont.data) {
            is Data.Enum -> data.variants
            is Data.Struct -> return
        }

    for ((index, variant) in variants.withIndex()) {
        val style = variant.style
        val identifier = cont.attrs.identifier()
        val other = variant.attrs.other()
        val tag = cont.attrs.tag()
        when {
            identifier == Identifier.Variant && other ->
                cx.errorSpannedBy(
                    variant.original,
                    "#[serde(other)] may not be used on a variant identifier",
                )

            identifier == Identifier.No && other && tag is TagType.None ->
                cx.errorSpannedBy(
                    variant.original,
                    "#[serde(other)] cannot appear on untagged enum",
                )

            style == Style.Unit && other && (identifier == Identifier.Field || identifier == Identifier.No) ->
                if (index < variants.size - 1) {
                    cx.errorSpannedBy(
                        variant.original,
                        "#[serde(other)] must be on the last variant",
                    )
                }

            other && (identifier == Identifier.Field || identifier == Identifier.No) ->
                cx.errorSpannedBy(
                    variant.original,
                    "#[serde(other)] must be on a unit variant",
                )

            identifier == Identifier.No && !other -> Unit
            style == Style.Unit && !other -> Unit
            style == Style.Newtype && identifier == Identifier.Field && !other ->
                if (index < variants.size - 1) {
                    cx.errorSpannedBy(
                        variant.original,
                        "`${variant.ident}` must be the last variant",
                    )
                }

            identifier == Identifier.Field && !other ->
                cx.errorSpannedBy(
                    variant.original,
                    "#[serde(field_identifier)] may only contain unit variants",
                )

            identifier == Identifier.Variant && !other ->
                cx.errorSpannedBy(
                    variant.original,
                    "#[serde(variant_identifier)] may only contain unit variants",
                )
        }
    }
}

/**
 * Skip-(de)serializing attributes are not allowed on variants marked
 * serialize-with or deserialize-with.
 */
private fun checkVariantSkipAttrs(
    cx: Ctxt,
    cont: Container,
) {
    val variants =
        when (val data = cont.data) {
            is Data.Enum -> data.variants
            is Data.Struct -> return
        }

    for (variant in variants) {
        if (variant.attrs.serializeWith() != null) {
            if (variant.attrs.skipSerializing()) {
                cx.errorSpannedBy(
                    variant.original,
                    "variant `${variant.ident}` cannot have both #[serde(serialize_with)] and #[serde(skip_serializing)]",
                )
            }

            for (field in variant.fields) {
                val member = memberMessage(field.member)

                if (field.attrs.skipSerializing()) {
                    cx.errorSpannedBy(
                        variant.original,
                        "variant `${variant.ident}` cannot have both #[serde(serialize_with)] and a field $member marked with #[serde(skip_serializing)]",
                    )
                }

                if (field.attrs.skipSerializingIf() != null) {
                    cx.errorSpannedBy(
                        variant.original,
                        "variant `${variant.ident}` cannot have both #[serde(serialize_with)] and a field $member marked with #[serde(skip_serializing_if)]",
                    )
                }
            }
        }

        if (variant.attrs.deserializeWith() != null) {
            if (variant.attrs.skipDeserializing()) {
                cx.errorSpannedBy(
                    variant.original,
                    "variant `${variant.ident}` cannot have both #[serde(deserialize_with)] and #[serde(skip_deserializing)]",
                )
            }

            for (field in variant.fields) {
                if (field.attrs.skipDeserializing()) {
                    val member = memberMessage(field.member)

                    cx.errorSpannedBy(
                        variant.original,
                        "variant `${variant.ident}` cannot have both #[serde(deserialize_with)] and a field $member marked with #[serde(skip_deserializing)]",
                    )
                }
            }
        }
    }
}

/**
 * The tag of an internally-tagged struct variant must not be the same as either
 * one of its fields, as this would result in duplicate keys in the serialized
 * output and/or ambiguity in the to-be-deserialized input.
 */
private fun checkInternalTagFieldNameConflict(
    cx: Ctxt,
    cont: Container,
) {
    val variants =
        when (val data = cont.data) {
            is Data.Enum -> data.variants
            is Data.Struct -> return
        }

    val tag =
        when (val tagType = cont.attrs.tag()) {
            is TagType.Internal -> tagType.tag
            is TagType.External,
            is TagType.Adjacent,
            is TagType.None,
            -> return
        }

    fun diagnoseConflict() {
        cx.errorSpannedBy(
            cont.original,
            "variant field name `$tag` conflicts with internal tag",
        )
    }

    for (variant in variants) {
        when (variant.style) {
            Style.Struct -> {
                if (variant.attrs.untagged()) {
                    continue
                }
                for (field in variant.fields) {
                    val checkSer =
                        !(field.attrs.skipSerializing() || variant.attrs.skipSerializing())
                    val checkDe =
                        !(field.attrs.skipDeserializing() || variant.attrs.skipDeserializing())
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

            Style.Unit,
            Style.Newtype,
            Style.Tuple,
            -> Unit
        }
    }
}

/**
 * In the case of adjacently-tagged enums, the type and the contents tag must
 * differ, for the same reason.
 */
private fun checkAdjacentTagConflict(
    cx: Ctxt,
    cont: Container,
) {
    val tagType = cont.attrs.tag()
    if (tagType !is TagType.Adjacent) {
        return
    }

    if (tagType.tag == tagType.content) {
        cx.errorSpannedBy(
            cont.original,
            "enum tags `${tagType.tag}` for type and content conflict with each other",
        )
    }
}

/**
 * Enums and unit structs cannot be transparent.
 */
private fun checkTransparent(
    cx: Ctxt,
    cont: Container,
    derive: Derive,
) {
    if (!cont.attrs.transparent()) {
        return
    }

    if (cont.attrs.typeFrom() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(transparent)] is not allowed with #[serde(from = \"...\")]",
        )
    }

    if (cont.attrs.typeTryFrom() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(transparent)] is not allowed with #[serde(try_from = \"...\")]",
        )
    }

    if (cont.attrs.typeInto() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(transparent)] is not allowed with #[serde(into = \"...\")]",
        )
    }

    val fields =
        when (val data = cont.data) {
            is Data.Enum -> {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] is not allowed on an enum",
                )
                return
            }

            is Data.Struct ->
                if (data.style == Style.Unit) {
                    cx.errorSpannedBy(
                        cont.original,
                        "#[serde(transparent)] is not allowed on a unit struct",
                    )
                    return
                } else {
                    data.fields
                }
        }

    var transparentField: Field? = null

    for (field in fields) {
        if (allowTransparent(field, derive)) {
            if (transparentField != null) {
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] requires struct to have at most one transparent field",
                )
                return
            }
            transparentField = field
        }
    }

    val selected = transparentField
    if (selected != null) {
        selected.attrs.markTransparent()
    } else {
        when (derive) {
            Derive.Serialize ->
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] requires at least one field that is not skipped",
                )

            Derive.Deserialize ->
                cx.errorSpannedBy(
                    cont.original,
                    "#[serde(transparent)] requires at least one field that is neither skipped nor has a default",
                )
        }
    }
}

private fun memberMessage(member: Member): String =
    when (member) {
        is Member.Named -> "`${member.ident}`"
        is Member.Unnamed -> "#${member.index.index}"
    }

private fun allowTransparent(
    field: Field,
    derive: Derive,
): Boolean {
    val type = ungroup(field.ty)
    if (type is SynType.Path) {
        val segment = type.path.segments.last()
        if (segment?.ident?.toString() == "PhantomData") {
            return false
        }
    }

    return when (derive) {
        Derive.Serialize -> !field.attrs.skipSerializing()
        Derive.Deserialize -> !field.attrs.skipDeserializing() && field.attrs.default().isNone()
    }
}

private fun checkFromAndTryFrom(
    cx: Ctxt,
    cont: Container,
) {
    if (cont.attrs.typeFrom() != null && cont.attrs.typeTryFrom() != null) {
        cx.errorSpannedBy(
            cont.original,
            "#[serde(from = \"...\")] and #[serde(try_from = \"...\")] conflict with each other",
        )
    }
}
