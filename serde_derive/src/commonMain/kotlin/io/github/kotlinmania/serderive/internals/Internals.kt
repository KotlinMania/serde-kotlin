// port-lint: source internals/mod.rs
package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.syn.SynType

public enum class Derive {
    Serialize,
    Deserialize
}

public fun ungroup(ty: SynType): SynType {
    var currentTy = ty
    while (currentTy is SynType.Group) {
        currentTy = currentTy.elem
    }
    return currentTy
}

