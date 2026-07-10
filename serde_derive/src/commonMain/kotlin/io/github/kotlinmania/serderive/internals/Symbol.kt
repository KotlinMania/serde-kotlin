// port-lint: source internals/symbol.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Path

public class Symbol(public val word: String) {
    override fun toString(): String = word

    public operator fun component1(): String = word

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Symbol -> this.word == other.word
            is Ident -> this.word == other.toString()
            is Path -> other.isIdent(this.word)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return word.hashCode()
    }
}

public fun Ident.eq(word: Symbol): Boolean = this.toString() == word.word

public fun Path.eq(word: Symbol): Boolean = this.isIdent(word.word)

public val ALIAS: Symbol = Symbol("alias")
public val BORROW: Symbol = Symbol("borrow")
public val BOUND: Symbol = Symbol("bound")
public val CONTENT: Symbol = Symbol("content")
public val CRATE: Symbol = Symbol("crate")
public val DEFAULT: Symbol = Symbol("default")
public val DENY_UNKNOWN_FIELDS: Symbol = Symbol("deny_unknown_fields")
public val DESERIALIZE: Symbol = Symbol("deserialize")
public val DESERIALIZE_WITH: Symbol = Symbol("deserialize_with")
public val EXPECTING: Symbol = Symbol("expecting")
public val FIELD_IDENTIFIER: Symbol = Symbol("field_identifier")
public val FLATTEN: Symbol = Symbol("flatten")
public val FROM: Symbol = Symbol("from")
public val GETTER: Symbol = Symbol("getter")
public val INTO: Symbol = Symbol("into")
public val NON_EXHAUSTIVE: Symbol = Symbol("non_exhaustive")
public val OTHER: Symbol = Symbol("other")
public val REMOTE: Symbol = Symbol("remote")
public val RENAME: Symbol = Symbol("rename")
public val RENAME_ALL: Symbol = Symbol("rename_all")
public val RENAME_ALL_FIELDS: Symbol = Symbol("rename_all_fields")
public val REPR: Symbol = Symbol("repr")
public val SERDE: Symbol = Symbol("serde")
public val SERIALIZE: Symbol = Symbol("serialize")
public val SERIALIZE_WITH: Symbol = Symbol("serialize_with")
public val SKIP: Symbol = Symbol("skip")
public val SKIP_DESERIALIZING: Symbol = Symbol("skip_deserializing")
public val SKIP_SERIALIZING: Symbol = Symbol("skip_serializing")
public val SKIP_SERIALIZING_IF: Symbol = Symbol("skip_serializing_if")
public val TAG: Symbol = Symbol("tag")
public val TRANSPARENT: Symbol = Symbol("transparent")
public val TRY_FROM: Symbol = Symbol("try_from")
public val UNTAGGED: Symbol = Symbol("untagged")
public val VARIANT_IDENTIFIER: Symbol = Symbol("variant_identifier")
public val WITH: Symbol = Symbol("with")
