// port-lint: source serde_derive/src/internals/name.rs
package io.github.kotlinmania.serde.serdederive.src.internals

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.serde.serdederive.src.internals.attr.Attr
import io.github.kotlinmania.serde.serdederive.src.internals.attr.VecAttr
import io.github.kotlinmania.syn.LitStr

class MultiName internal constructor(
    internal val serialize: Name,
    internal val serializeRenamed: Boolean,
    internal val deserialize: Name,
    internal val deserializeRenamed: Boolean,
    private val deserializeAliasesValue: MutableList<Name>,
) {
    companion object {
        internal fun fromAttrs(
            sourceName: Name,
            serName: Attr<Name>,
            deName: Attr<Name>,
            deAliases: VecAttr<Name>?,
        ): MultiName {
            val aliasSet = mutableListOf<Name>()
            if (deAliases != null) {
                for (aliasName in deAliases.get()) {
                    if (aliasName !in aliasSet) {
                        aliasSet += aliasName
                    }
                }
                aliasSet.sort()
            }

            val serializedName = serName.get()
            val serializedRenamed = serializedName != null
            val deserializedName = deName.get()
            val deserializedRenamed = deserializedName != null
            return MultiName(
                serialize = serializedName ?: sourceName.copy(),
                serializeRenamed = serializedRenamed,
                deserialize = deserializedName ?: sourceName,
                deserializeRenamed = deserializedRenamed,
                deserializeAliasesValue = aliasSet,
            )
        }
    }

    /**
     * Return the container name for the container when serializing.
     */
    fun serializeName(): Name = serialize

    /**
     * Return the container name for the container when deserializing.
     */
    fun deserializeName(): Name = deserialize

    internal fun deserializeAliases(): List<Name> = deserializeAliasesValue

    internal fun addDeserializeAlias(alias: Name) {
        if (alias !in deserializeAliasesValue) {
            deserializeAliasesValue += alias
            deserializeAliasesValue.sort()
        }
    }
}

class Name(
    var value: String,
    val span: Span,
) : ToTokens,
    Comparable<Name> {
    override fun toTokens(tokens: TokenStream) {
        LitStr.new(value, span).toTokens(tokens)
    }

    fun cmp(other: Name): Int = value.compareTo(other.value)

    override fun compareTo(other: Name): Int = cmp(other)

    fun partialCmp(other: Name): Int = cmp(other)

    fun eq(other: Name): Boolean = value == other.value

    fun copy(
        value: String = this.value,
        span: Span = this.span,
    ): Name = Name(value, span)

    override fun equals(other: Any?): Boolean = other is Name && eq(other)

    override fun hashCode(): Int = value.hashCode()

    fun fmt(formatter: Appendable): Result<Unit> =
        runCatching {
            formatter.append(value)
            Unit
        }

    override fun toString(): String = value

    companion object {
        fun from(ident: Ident): Name =
            Name(
                value = ident.toString(),
                span = ident.span(),
            )

        fun from(lit: LitStr): Name =
            Name(
                value = lit.value(),
                span = lit.span(),
            )
    }
}
