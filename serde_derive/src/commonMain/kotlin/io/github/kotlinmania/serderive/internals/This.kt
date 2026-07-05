package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathArguments
import io.github.kotlinmania.syn.PathSegment
import io.github.kotlinmania.syn.token.PathSep

public fun thisType(cont: Container): Path {
    val remote = cont.attrs.remote()
    if (remote != null) {
        val thisPath = remote.deepCopy()
        for (segment in thisPath.segments.toList()) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed) {
                segment.arguments = arguments.copy(colon2Token = null)
            }
        }
        return thisPath
    } else {
        return Path.from(cont.ident.deepCopy())
    }
}

public fun thisValue(cont: Container): Path {
    val remote = cont.attrs.remote()
    if (remote != null) {
        val thisPath = remote.deepCopy()
        for (segment in thisPath.segments.toList()) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed) {
                if (arguments.colon2Token == null) {
                    segment.arguments = arguments.copy(colon2Token = PathSep.default())
                }
            }
        }
        return thisPath
    } else {
        return Path.from(cont.ident.deepCopy())
    }
}