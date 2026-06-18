package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.SynError

public class Ctxt {
    private var errors: MutableList<SynError>? = mutableListOf()

    public fun errorSpannedBy(obj: ToTokens, msg: String) {
        errors?.add(SynError.newSpanned(obj.toTokenStream(), msg))
    }

    public fun synError(err: SynError) {
        errors?.add(err)
    }

    public fun check() {
        val currentErrors = errors ?: return
        errors = null

        val iterator = currentErrors.iterator()
        if (!iterator.hasNext()) {
            return
        }

        val combined = iterator.next()
        while (iterator.hasNext()) {
            combined.combine(iterator.next())
        }

        throw combined
    }

    // Note: Kotlin doesn't have Drop. Calling `check()` manually is strictly required!
}

