// port-lint: source internals/symbol.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.Ident
import io.github.kotlinmania.syn.Path

internal class Symbol(
    public val word: String,
) {
    override fun toString(): String = word

    public operator fun component1(): String = word

    override fun equals(other: Any?): Boolean =
        when (other) {
            is Symbol -> this.word == other.word
            is Ident -> this.word == other.toString()
            is Path -> other.isIdent(this.word)
            else -> false
        }

    override fun hashCode(): Int = word.hashCode()
}

internal fun Ident.eq(word: Symbol): Boolean = this.toString() == word.word

internal fun Path.eq(word: Symbol): Boolean = this.isIdent(word.word)

internal val ALIAS: Symbol = Symbol("alias")
internal val BORROW: Symbol = Symbol("borrow")
internal val BOUND: Symbol = Symbol("bound")
internal val CONTENT: Symbol = Symbol("content")
internal val CRATE: Symbol = Symbol("crate")
internal val DEFAULT: Symbol = Symbol("default")
internal val DENY_UNKNOWN_FIELDS: Symbol = Symbol("deny_unknown_fields")
internal val DESERIALIZE: Symbol = Symbol("deserialize")
internal val DESERIALIZE_WITH: Symbol = Symbol("deserialize_with")
internal val EXPECTING: Symbol = Symbol("expecting")
internal val FIELD_IDENTIFIER: Symbol = Symbol("field_identifier")
internal val FLATTEN: Symbol = Symbol("flatten")
internal val FROM: Symbol = Symbol("from")
internal val GETTER: Symbol = Symbol("getter")
internal val INTO: Symbol = Symbol("into")
internal val NON_EXHAUSTIVE: Symbol = Symbol("non_exhaustive")
internal val OTHER: Symbol = Symbol("other")
internal val REMOTE: Symbol = Symbol("remote")
internal val RENAME: Symbol = Symbol("rename")
internal val RENAME_ALL: Symbol = Symbol("rename_all")
internal val RENAME_ALL_FIELDS: Symbol = Symbol("rename_all_fields")
internal val REPR: Symbol = Symbol("repr")
internal val SERDE: Symbol = Symbol("serde")
internal val SERIALIZE: Symbol = Symbol("serialize")
internal val SERIALIZE_WITH: Symbol = Symbol("serialize_with")
internal val SKIP: Symbol = Symbol("skip")
internal val SKIP_DESERIALIZING: Symbol = Symbol("skip_deserializing")
internal val SKIP_SERIALIZING: Symbol = Symbol("skip_serializing")
internal val SKIP_SERIALIZING_IF: Symbol = Symbol("skip_serializing_if")
internal val TAG: Symbol = Symbol("tag")
internal val TRANSPARENT: Symbol = Symbol("transparent")
internal val TRY_FROM: Symbol = Symbol("try_from")
internal val UNTAGGED: Symbol = Symbol("untagged")
internal val VARIANT_IDENTIFIER: Symbol = Symbol("variant_identifier")
internal val WITH: Symbol = Symbol("with")
