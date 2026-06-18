package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.PathArguments
import io.github.kotlinmania.syn.token

public fun thisType(cont: Container): Path {
    val remote = cont.attrs.remote()
    if (remote != null) {
        var thisPath = remote.clone()
        for (segment in thisPath.segments) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed) {
                segment.arguments = arguments.copy(colon2Token = null)
            }
        }
        return thisPath
    } else {
        return Path.from(cont.ident.clone())
    }
}

public fun thisValue(cont: Container): Path {
    val remote = cont.attrs.remote()
    if (remote != null) {
        var thisPath = remote.clone()
        for (segment in thisPath.segments) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed) {
                if (arguments.colon2Token == null) {
                    segment.arguments = arguments.copy(colon2Token = token.PathSep())
                }
            }
        }
        return thisPath
    } else {
        return Path.from(cont.ident.clone())
    }
}
