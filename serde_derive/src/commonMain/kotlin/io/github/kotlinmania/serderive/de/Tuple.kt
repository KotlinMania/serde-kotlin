// port-lint: source de/tuple.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.serderive.checkedQuote
import io.github.kotlinmania.serderive.checkedQuoteSpanned
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.syn.span

// Generates the deserialize body for a tuple struct, including newtype structs.
internal fun deserializeTuple(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer,
    form: TupleForm
): Fragment {
    check(!hasFlatten(fields)) { "tuples and tuple variants cannot have flatten fields" }

    val fieldCount = fields.count { !it.attrs.skipDeserializing() }
    val fieldCountToken = rustUsizeLiteral(fieldCount)

    val thisType = params.thisType
    val thisValue = params.thisValue
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()

    // If there are getters (implying private fields), construct the local type
    // and use an Into conversion to get the remote type. If there are no
    // getters then construct the target type directly.
    val construct = if (params.hasGetter) {
        val local = params.local
        checkedQuote("`#`local", "local" to local)
    } else {
        checkedQuote("`#`thisValue", "thisValue" to thisValue)
    }

    val typePath = when (form) {
        TupleForm.Tuple -> construct
        is TupleForm.ExternallyTagged -> checkedQuote(
            "`#`construct::`#`variantIdent",
            "construct" to construct,
            "variantIdent" to form.variantIdent,
        )
        is TupleForm.Untagged -> checkedQuote(
            "`#`construct::`#`variantIdent",
            "construct" to construct,
            "variantIdent" to form.variantIdent,
        )
    }
    val expecting = when (form) {
        TupleForm.Tuple -> "tuple struct ${params.typeName()}"
        is TupleForm.ExternallyTagged -> "tuple variant ${params.typeName()}::${form.variantIdent}"
        is TupleForm.Untagged -> "tuple variant ${params.typeName()}::${form.variantIdent}"
    }
    val expectingVal = cattrs.expecting() ?: expecting

    val nfields = fields.size

    val visitNewtypeStruct = when (form) {
        TupleForm.Tuple -> if (nfields == 1) {
            deserializeNewtypeStructTuple(typePath, params, fields[0])
        } else null
        else -> null
    }

    val visitSeq = Stmts(deserializeSeq(typePath, params, fields, false, cattrs, expectingVal))

    val visitorExpr = checkedQuote("""
        __Visitor {
            marker: _serde::`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData,
        }
    """, mapOf("Private" to Private, "thisType" to thisType, "tyGenerics" to tyGenerics))
    val dispatch = when (form) {
        TupleForm.Tuple -> if (nfields == 1) {
            val typeName = cattrs.name().deserializeName()
            checkedQuote(
                "_serde::Deserializer::deserialize_newtype_struct(__deserializer, `#`typeName, `#`visitorExpr)",
                "typeName" to typeName,
                "visitorExpr" to visitorExpr,
            )
        } else {
            val typeName = cattrs.name().deserializeName()
            checkedQuote(
                "_serde::Deserializer::deserialize_tuple_struct(__deserializer, `#`typeName, `#`fieldCount, `#`visitorExpr)",
                "typeName" to typeName,
                "fieldCount" to fieldCountToken,
                "visitorExpr" to visitorExpr,
            )
        }
        is TupleForm.ExternallyTagged -> checkedQuote(
            "_serde::de::VariantAccess::tuple_variant(__variant, `#`fieldCount, `#`visitorExpr)",
            "fieldCount" to fieldCountToken,
            "visitorExpr" to visitorExpr,
        )
        is TupleForm.Untagged -> checkedQuote(
            "_serde::Deserializer::deserialize_tuple(__deserializer, `#`fieldCount, `#`visitorExpr)",
            "fieldCount" to fieldCountToken,
            "visitorExpr" to visitorExpr,
        )
    }

    val visitorVar = if (fieldCount == 0) checkedQuote("_") else checkedQuote("mut __seq")

    return Fragment.Block(checkedQuote("""
        `#`[doc(hidden)]
        struct __Visitor `#`deImplGenerics `#`whereClause {
            marker: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn expecting(&self, __formatter: &mut _serde::`#`Private::Formatter) -> _serde::`#`Private::fmt::Result {
                _serde::`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
            }

            `#`visitNewtypeStruct

            `#`[inline]
            fn visit_seq<__A>(self, `#`visitorVar: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::SeqAccess<`#`delife>,
            {
                `#`visitSeq
            }
        }

        `#`dispatch
    """, mapOf(
        "deImplGenerics" to deImplGenerics,
        "whereClause" to whereClause,
        "Private" to Private,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "delife" to delife,
        "deTyGenerics" to deTyGenerics,
        "expectingVal" to expectingVal,
        "visitNewtypeStruct" to visitNewtypeStruct,
        "visitorVar" to visitorVar,
        "visitSeq" to visitSeq,
        "dispatch" to dispatch,
    )))
}

private fun deserializeNewtypeStructTuple(
    typePath: TokenStream,
    params: Parameters,
    field: Field
): TokenStream {
    val delife = params.borrowed.deLifetime()
    val fieldTy = field.ty
    val deserializerVar = checkedQuote("__e")

    val value = field.attrs.deserializeWith()?.let { path ->
        checkedQuoteSpanned(
            path.span(),
            "`#`path(`#`deserializerVar)?",
            "path" to path,
            "deserializerVar" to deserializerVar,
        )
    } ?: run {
        val span = field.original.span()
        val func = checkedQuoteSpanned(span, "<`#`fieldTy as _serde::Deserialize>::deserialize", "fieldTy" to fieldTy)
        checkedQuote("`#`func(`#`deserializerVar)?", "func" to func, "deserializerVar" to deserializerVar)
    }

    var result = checkedQuote("`#`typePath(__field0)", "typePath" to typePath)
    if (params.hasGetter) {
        val thisType = params.thisType
        val split = params.generics.splitForImpl()
        val tyGenerics = split.typeGenerics
        result = checkedQuote(
            "_serde::`#`Private::Into::<`#`thisType `#`tyGenerics>::into(`#`result)",
            "Private" to Private,
            "thisType" to thisType,
            "tyGenerics" to tyGenerics,
            "result" to result,
        )
    }

    return checkedQuote("""
        `#`[inline]
        fn visit_newtype_struct<__E>(self, `#`deserializerVar: __E) -> _serde::`#`Private::Result<Self::Value, __E::Error>
        where
            __E: _serde::Deserializer<`#`delife>,
        {
            let __field0: `#`fieldTy = `#`value;
            _serde::`#`Private::Ok(`#`result)
        }
    """, mapOf(
        "deserializerVar" to deserializerVar,
        "Private" to Private,
        "delife" to delife,
        "fieldTy" to fieldTy,
        "value" to value,
        "result" to result,
    ))
}

// Generates the deserialize-in-place body for a tuple struct, including newtype structs.
internal fun deserializeTupleInPlace(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer
): Fragment {
    check(!hasFlatten(fields)) { "tuples and tuple variants cannot have flatten fields" }

    val fieldCount = fields.count { !it.attrs.skipDeserializing() }
    val fieldCountToken = rustUsizeLiteral(fieldCount)

    val thisType = params.thisType
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) = params.inPlaceGenericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()
    val placeLife = placeLifetime()

    val expecting = "tuple struct ${params.typeName()}"
    val expectingVal = cattrs.expecting() ?: expecting

    val nfields = fields.size

    val visitNewtypeStruct = if (nfields == 1) {
        check(fields[0].attrs.deserializeWith() == null)
        val index = rustUnsuffixedLiteral(0u)
        checkedQuote("""
            `#`[inline]
            fn visit_newtype_struct<__E>(self, __e: __E) -> _serde::`#`Private::Result<Self::Value, __E::Error>
            where
                __E: _serde::Deserializer<`#`delife>,
            {
                _serde::Deserialize::deserialize_in_place(__e, &mut self.place.`#`index)
            }
        """, "Private" to Private, "delife" to delife, "index" to index)
    } else {
        checkedQuote("")
    }

    val visitSeq = Stmts(deserializeSeqInPlace(params, fields, cattrs, expectingVal))

    val visitorExpr = checkedQuote("""
        __Visitor {
            place: __place,
            lifetime: _serde::`#`Private::PhantomData,
        }
    """, "Private" to Private)

    val typeName = cattrs.name().deserializeName()
    val dispatch = if (nfields == 1) {
        checkedQuote(
            "_serde::Deserializer::deserialize_newtype_struct(__deserializer, `#`typeName, `#`visitorExpr)",
            "typeName" to typeName,
            "visitorExpr" to visitorExpr,
        )
    } else {
        checkedQuote(
            "_serde::Deserializer::deserialize_tuple_struct(__deserializer, `#`typeName, `#`fieldCount, `#`visitorExpr)",
            "typeName" to typeName,
            "fieldCount" to fieldCountToken,
            "visitorExpr" to visitorExpr,
        )
    }

    val visitorVar = if (fieldCount == 0) checkedQuote("_") else checkedQuote("mut __seq")

    return Fragment.Block(checkedQuote("""
        `#`[doc(hidden)]
        struct __Visitor `#`deImplGenerics `#`whereClause {
            place: &`#`placeLife mut `#`thisType `#`tyGenerics,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
            type Value = ();

            fn expecting(&self, __formatter: &mut _serde::`#`Private::Formatter) -> _serde::`#`Private::fmt::Result {
                _serde::`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
            }

            `#`visitNewtypeStruct

            `#`[inline]
            fn visit_seq<__A>(self, `#`visitorVar: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::SeqAccess<`#`delife>,
            {
                `#`visitSeq
            }
        }

        `#`dispatch
    """, mapOf(
        "deImplGenerics" to deImplGenerics,
        "whereClause" to whereClause,
        "placeLife" to placeLife,
        "delife" to delife,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "Private" to Private,
        "deTyGenerics" to deTyGenerics,
        "expectingVal" to expectingVal,
        "visitNewtypeStruct" to visitNewtypeStruct,
        "visitorVar" to visitorVar,
        "visitSeq" to visitSeq,
        "dispatch" to dispatch,
    )))
}
