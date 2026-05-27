// port-lint: source serde_derive/src/pretend.rs
package io.github.kotlinmania.serde.serdederive.src

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.serde.serdederive.src.internals.ast.*
import io.github.kotlinmania.syn.GenericParam
import io.github.kotlinmania.syn.Generics
import io.github.kotlinmania.syn.Member

/**
 * Emit hidden token references that keep remote derive fields and variants visible to diagnostics.
 */
public fun pretendUsed(
    cont: Container,
    isPacked: Boolean,
): TokenStream =
    tokenStream(
        listOf(
            pretendFieldsUsed(cont, isPacked),
            pretendVariantsUsed(cont),
        ).joinToString("\n") { stream -> stream.toString() },
    )

private fun pretendFieldsUsed(
    cont: Container,
    isPacked: Boolean,
): TokenStream =
    when (val data = cont.data) {
        is Data.Enum -> pretendFieldsUsedEnum(cont, data.variants)
        is Data.Struct ->
            when (data.style) {
                Style.Struct, Style.Tuple, Style.Newtype ->
                    if (isPacked) {
                        pretendFieldsUsedStructPacked(cont, data.fields)
                    } else {
                        pretendFieldsUsedStruct(cont, data.fields)
                    }

                Style.Unit -> TokenStream.new()
            }
    }

private fun pretendFieldsUsedStruct(
    cont: Container,
    fields: List<Field>,
): TokenStream {
    val typeIdent = cont.ident.toString()
    val typeGenerics = typeGenerics(cont.generics)
    val members =
        fields
            .mapIndexed { index, field ->
                "${member(field.member)}: __v$index"
            }.joinToString(", ")

    return tokenStream(
        """
        match _serde::__private::None::<&$typeIdent$typeGenerics> {
            _serde::__private::Some($typeIdent { $members }) => {}
            _ => {}
        }
        """.trimIndent(),
    )
}

private fun pretendFieldsUsedStructPacked(
    cont: Container,
    fields: List<Field>,
): TokenStream {
    val typeIdent = cont.ident.toString()
    val typeGenerics = typeGenerics(cont.generics)
    val wildcardMembers =
        fields.joinToString(", ") { field ->
            "${member(field.member)}: _"
        }
    val addrOfMembers =
        fields.joinToString("\n") { field ->
            "let _ = _serde::__private::ptr::addr_of!(__v.${member(field.member)});"
        }

    return tokenStream(
        """
        match _serde::__private::None::<&$typeIdent$typeGenerics> {
            _serde::__private::Some(__v @ $typeIdent { $wildcardMembers }) => {
                $addrOfMembers
            }
            _ => {}
        }
        """.trimIndent(),
    )
}

private fun pretendFieldsUsedEnum(
    cont: Container,
    variants: List<Variant>,
): TokenStream {
    val typeIdent = cont.ident.toString()
    val typeGenerics = typeGenerics(cont.generics)
    val patterns =
        variants
            .mapNotNull { variant ->
                when (variant.style) {
                    Style.Struct, Style.Tuple, Style.Newtype -> {
                        val members =
                            variant.fields
                                .mapIndexed { index, field ->
                                    "${member(field.member)}: __v$index"
                                }.joinToString(", ")
                        "_serde::__private::Some($typeIdent::${variant.ident} { $members }) => {}"
                    }

                    Style.Unit -> null
                }
            }.joinToString("\n")

    return tokenStream(
        """
        match _serde::__private::None::<&$typeIdent$typeGenerics> {
            $patterns
            _ => {}
        }
        """.trimIndent(),
    )
}

private fun pretendVariantsUsed(cont: Container): TokenStream {
    val variants =
        when (val data = cont.data) {
            is Data.Enum -> data.variants
            is Data.Struct -> return TokenStream.new()
        }

    val typeIdent = cont.ident.toString()
    val turbofish = turbofish(cont.generics)
    val cases =
        variants.joinToString("\n") { variant ->
            val placeholders = variant.fields.indices.joinToString(", ") { index -> "__v$index" }
            val tuplePlaceholders =
                variant.fields.indices.joinToString("") { index -> "__v$index," }
            val pattern =
                when (variant.style) {
                    Style.Struct -> {
                        val members =
                            variant.fields
                                .mapIndexed { index, field ->
                                    "${member(field.member)}: __v$index"
                                }.joinToString(", ")
                        "{ $members }"
                    }

                    Style.Tuple, Style.Newtype -> "( $placeholders )"
                    Style.Unit -> ""
                }

            """
            match _serde::__private::None {
                _serde::__private::Some(($tuplePlaceholders)) => {
                    let _ = $typeIdent::${variant.ident}$turbofish $pattern;
                }
                _ => {}
            }
            """.trimIndent()
        }

    return tokenStream(cases)
}

private fun typeGenerics(generics: Generics): String {
    val params = genericParamNames(generics)
    return if (params.isEmpty()) "" else "<${params.joinToString(", ")}>"
}

private fun turbofish(generics: Generics): String {
    val params = genericParamNames(generics)
    return if (params.isEmpty()) "" else "::<${params.joinToString(", ")}>"
}

private fun genericParamNames(generics: Generics): List<String> =
    generics.params.mapNotNull { param ->
        when (param) {
            is GenericParam.ConstParam -> param.ident.toString()
            is GenericParam.LifetimeParam -> null
            is GenericParam.TypeParam -> param.ident.toString()
        }
    }

private fun member(member: Member): String =
    when (member) {
        is Member.Named -> member.ident.toString()
        is Member.Unnamed -> member.index.index.toString()
    }

private fun tokenStream(tokens: String): TokenStream =
    if (tokens.isBlank()) {
        TokenStream.new()
    } else {
        TokenStream.fromString(tokens).getOrThrow()
    }
