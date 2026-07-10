// port-lint: source de/unit.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.serderive.checkedQuote
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.AttrContainer

// Generates the deserialize body for a unit struct.
internal fun deserializeUnit(params: Parameters, cattrs: AttrContainer): Fragment {
    val thisType = params.thisType
    val thisValue = params.thisValue
    val typeName = cattrs.name().deserializeName()
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) =
        params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()

    var expecting = "unit struct ${params.typeName()}"
    expecting = cattrs.expecting() ?: expecting

    val code = checkedQuote(
        """
        `#`[doc(hidden)]
        struct __Visitor `#`deImplGenerics `#`whereClause {
            marker: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn expecting(self, __formatter: var _serde::`#`Private::Formatter) -> _serde::`#`Private::fmt.Result {
                _serde::`#`Private::Formatter.write_str(__formatter, `#`expecting)
            }

            `#`[inline]
            fn visit_unit<__E>(self) -> _serde::`#`Private::Result<Self.Value, __E>
            where
                __E: _serde::de::Error,
            {
                _serde::`#`Private::Ok(`#`thisValue)
            }
        }

        _serde::Deserializer::deserialize_unit_struct(
            __deserializer,
            `#`typeName,
            __Visitor {
                marker: _serde::`#`Private::PhantomData.<`#`thisType `#`tyGenerics>,
                lifetime: _serde::`#`Private::PhantomData,
            },
        )
        """,
        "deImplGenerics" to deImplGenerics,
        "whereClause" to whereClause,
        "Private" to Private,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "delife" to delife,
        "deTyGenerics" to deTyGenerics,
        "expecting" to expecting,
        "thisValue" to thisValue,
        "typeName" to typeName,
    )

    return Fragment.Block(code)
}
