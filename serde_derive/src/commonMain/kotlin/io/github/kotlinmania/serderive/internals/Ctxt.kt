// port-lint: source internals/ctxt.rs
package io.github.kotlinmania.serderive.internals


import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.append
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.Field
import io.github.kotlinmania.syn.Variant

public class Ctxt {
    private var errors: MutableList<SynError>? = mutableListOf()

    public fun errorSpannedBy(obj: ToTokens, msg: String) {
        errors?.add(SynError.newSpanned(obj.toTokenStream(), msg))
    }

    public fun errorSpannedBy(tokens: TokenStream, msg: String) {
        errors?.add(SynError.newSpanned(tokens, msg))
    }

    public fun errorSpannedBy(path: Path, msg: String) {
        val out = TokenStream.new()
        path.toTokens(out)
        errors?.add(SynError.newSpanned(out, msg))
    }

    public fun errorSpannedBy(field: Field, msg: String) {
        val out = TokenStream.new()
        field.toTokens(out)
        errors?.add(SynError.newSpanned(out, msg))
    }

    public fun errorSpannedBy(variant: Variant, msg: String) {
        val out = TokenStream.new()
        variant.toTokens(out)
        errors?.add(SynError.newSpanned(out, msg))
    }

    public fun errorSpannedBy(ident: io.github.kotlinmania.procmacro2.Ident, msg: String) {
        val ts = TokenStream.new()
        ts.append(ident)
        errors?.add(SynError.newSpanned(ts, msg))
    }

    public fun errorSpannedBy(synType: io.github.kotlinmania.syn.SynType, msg: String) {
        val out = TokenStream.new()
        synType.toTokens(out)
        errors?.add(SynError.newSpanned(out, msg))
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
