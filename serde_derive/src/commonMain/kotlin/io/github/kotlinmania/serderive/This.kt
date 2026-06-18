package io.github.kotlinmania.serderive

/* STUBBED OUT FOR NOW
package io.github.kotlinmania.serderive

import io.github.kotlinmania.syn.PathArguments

public fun thisType(cont: Container): syn.Path {
    val remote = cont.attrs.remote()
    if (remote != null) {
        val thisPath = remote.clone()
        for (segment in thisPath.segments) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed) {
                segment.arguments = arguments.copy(colon2Token = null)
            }
        }
        return thisPath
    } else {
        return syn.Path.from(cont.ident.clone())
    }
}

public fun thisValue(cont: Container): syn.Path {
    val remote = cont.attrs.remote()
    if (remote != null) {
        val thisPath = remote.clone()
        for (segment in thisPath.segments) {
            val arguments = segment.arguments
            if (arguments is PathArguments.AngleBracketed) {
                if (arguments.colon2Token == null) {
                    segment.arguments = arguments.copy(colon2Token = syn.token.PathSep(arguments.ltToken!!.span))
                }
            }
        }
        return thisPath
    } else {
        return syn.Path.from(cont.ident.clone())
    }
}

*/
