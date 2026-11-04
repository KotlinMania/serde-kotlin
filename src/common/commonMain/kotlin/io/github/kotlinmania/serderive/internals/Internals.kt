package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.syn.SynType

internal enum class Derive {
    Serialize,
    Deserialize,
}

internal fun ungroup(ty: SynType): SynType {
    var currentTy = ty
    while (currentTy is SynType.Group) {
        currentTy = currentTy.elem
    }
    return currentTy
}
