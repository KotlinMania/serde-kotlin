// port-lint: source internals/name.rs
package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.LitStr

public class MultiName(
    internal val serialize: Name,
    internal val serializeRenamed: Boolean,
    internal val deserialize: Name,
    internal val deserializeRenamed: Boolean,
    deserializeAliases: Set<Name>,
) {
    private val mutableDeserializeAliases: MutableSet<Name> = deserializeAliases.toMutableSet()

    public fun serializeName(): Name = serialize

    public fun deserializeName(): Name = deserialize

    internal fun deserializeAliases(): Set<Name> = mutableDeserializeAliases

    internal fun applyDeserializeAliasRule(transform: (String) -> String) {
        val aliases = mutableDeserializeAliases.toList()
        mutableDeserializeAliases.clear()
        for (alias in aliases) {
            mutableDeserializeAliases.add(Name(transform(alias.value), alias.span))
        }
        mutableDeserializeAliases.add(deserialize.clone())
    }

    internal companion object {
        fun fromAttrs(
            sourceName: Name,
            serName: Attr<Name>,
            deName: Attr<Name>,
            deAliases: VecAttr<Name>?
        ): MultiName {
            val aliasSet = mutableSetOf<Name>()
            if (deAliases != null) {
                for (aliasName in deAliases.get()) {
                    aliasSet.add(aliasName)
                }
            }

            val serNameVal = serName.get()
            val serRenamed = serNameVal != null
            val deNameVal = deName.get()
            val deRenamed = deNameVal != null

            return MultiName(
                serialize = serNameVal ?: sourceName.clone(),
                serializeRenamed = serRenamed,
                deserialize = deNameVal ?: sourceName.clone(),
                deserializeRenamed = deRenamed,
                deserializeAliases = aliasSet
            )
        }
    }
}

public class Name(
    public var value: String,
    public val span: Span
) : ToTokens, Comparable<Name> {

    public fun clone(): Name = Name(value, span)

    override fun toTokens(tokens: TokenStream) {
        LitStr.new(value, span).toTokens(tokens)
    }

    override fun compareTo(other: Name): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Name) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }

    public companion object {
        public fun from(ident: Ident): Name {
            return Name(ident.toString(), ident.span())
        }

        public fun from(lit: LitStr): Name {
            return Name(lit.value(), lit.span())
        }
    }
}
