// port-lint: source serde_derive/src/internals/mod.rs
package io.github.kotlinmania.serde.serdederive.src.internals

import io.github.kotlinmania.syn.SynType as Type

public enum class Derive {
    Serialize,
    Deserialize,
}

public tailrec fun ungroup(ty: Type): Type =
    when (ty) {
        is Type.Group -> ungroup(ty.elem)
        else -> ty
    }
