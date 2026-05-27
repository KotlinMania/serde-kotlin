// port-lint: source serde_derive/src/internals/symbol.rs
package io.github.kotlinmania.serde.serdederive.src.internals

import io.github.kotlinmania.syn.Ident
import io.github.kotlinmania.syn.Path

class Symbol(
    val value: String,
) {
    fun fmt(formatter: Appendable): Result<Unit> =
        runCatching {
            formatter.append(value)
            Unit
        }

    override fun toString(): String = value
}

val ALIAS: Symbol = Symbol("alias")
val BORROW: Symbol = Symbol("borrow")
val BOUND: Symbol = Symbol("bound")
val CONTENT: Symbol = Symbol("content")
val CRATE: Symbol = Symbol("crate")
val DEFAULT: Symbol = Symbol("default")
val DENY_UNKNOWN_FIELDS: Symbol = Symbol("deny_unknown_fields")
val DESERIALIZE: Symbol = Symbol("deserialize")
val DESERIALIZE_WITH: Symbol = Symbol("deserialize_with")
val EXPECTING: Symbol = Symbol("expecting")
val FIELD_IDENTIFIER: Symbol = Symbol("field_identifier")
val FLATTEN: Symbol = Symbol("flatten")
val FROM: Symbol = Symbol("from")
val GETTER: Symbol = Symbol("getter")
val INTO: Symbol = Symbol("into")
val NON_EXHAUSTIVE: Symbol = Symbol("non_exhaustive")
val OTHER: Symbol = Symbol("other")
val REMOTE: Symbol = Symbol("remote")
val RENAME: Symbol = Symbol("rename")
val RENAME_ALL: Symbol = Symbol("rename_all")
val RENAME_ALL_FIELDS: Symbol = Symbol("rename_all_fields")
val REPR: Symbol = Symbol("repr")
val SERDE: Symbol = Symbol("serde")
val SERIALIZE: Symbol = Symbol("serialize")
val SERIALIZE_WITH: Symbol = Symbol("serialize_with")
val SKIP: Symbol = Symbol("skip")
val SKIP_DESERIALIZING: Symbol = Symbol("skip_deserializing")
val SKIP_SERIALIZING: Symbol = Symbol("skip_serializing")
val SKIP_SERIALIZING_IF: Symbol = Symbol("skip_serializing_if")
val TAG: Symbol = Symbol("tag")
val TRANSPARENT: Symbol = Symbol("transparent")
val TRY_FROM: Symbol = Symbol("try_from")
val UNTAGGED: Symbol = Symbol("untagged")
val VARIANT_IDENTIFIER: Symbol = Symbol("variant_identifier")
val WITH: Symbol = Symbol("with")

fun Ident.eq(word: Symbol): Boolean = toString() == word.value

fun Path.eq(word: Symbol): Boolean = isIdent(word.value)
