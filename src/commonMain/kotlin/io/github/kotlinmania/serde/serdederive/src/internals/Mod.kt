// port-lint: source serde_derive/src/internals/mod.rs
package io.github.kotlinmania.serde.serdederive.src.internals

import io.github.kotlinmania.syn.SynType

enum class Derive {
    Serialize,
    Deserialize,
}

tailrec fun ungroup(ty: SynType): SynType =
    when (ty) {
        is SynType.Group -> ungroup(ty.elem)
        else -> ty
    }
