// port-lint: source serde_derive/src/this.rs
package io.github.kotlinmania.serde.serdederive.src

import io.github.kotlinmania.serde.serdederive.src.internals.ast.Container
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathArguments
import io.github.kotlinmania.syn.token.PathSep

public fun thisType(cont: Container): Path {
    val remote = cont.attrs.remote()
    return if (remote != null) {
        val thisPath = remote.copy()
        for (segment in thisPath.segments) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed) {
                arguments.colon2Token = null
            }
        }
        thisPath
    } else {
        Path.from(cont.ident.copy())
    }
}

public fun thisValue(cont: Container): Path {
    val remote = cont.attrs.remote()
    return if (remote != null) {
        val thisPath = remote.copy()
        for (segment in thisPath.segments) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed && arguments.colon2Token == null) {
                arguments.colon2Token = PathSep.from(arguments.ltToken.span)
            }
        }
        thisPath
    } else {
        Path.from(cont.ident.copy())
    }
}
