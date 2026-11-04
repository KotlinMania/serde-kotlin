// port-lint: source private/seed.rs
package io.github.kotlinmania.serdecore.priv

// Generated from Syn AST; exact original retained in input.rs.
// Borrowed location and codec witness are explicit. Keep the caller slot exclusively borrowed for this seed's lifetime; custom Drop, allocation and address observations are not modeled.
// Result specializes the bound Kotlin error API. Provider errors and exceptions are returned or propagated without interception.
// / A DeserializeSeed helper for implementing deserialize_in_place Visitors.
// /
// / Wraps a mutable reference and calls deserialize_in_place on it.
class `InPlaceSeed`<`T`>(
    val __rtkField0: io.github.kotlinmania.serdecore.de.MutablePlace<`T`>,
    private val __rtkCodec: io.github.kotlinmania.serdecore.de.Deserialize<`T`>,
) : io.github.kotlinmania.serdecore.de.DeserializeSeed<kotlin.Unit> {
    private var __rtkConsumed: kotlin.Boolean = false
    override fun <`D` : io.github.kotlinmania.serdecore.de.Deserializer> `deserialize`(`deserializer`: `D`): io.github.kotlinmania.serde.SerdeResult<kotlin.Unit> {
        kotlin.check(!__rtkConsumed) { "consuming Rust value already moved" }
        __rtkConsumed = true
        return __rtkCodec.`deserializeInPlace`(`deserializer`, __rtkField0)
    }
}
