// port-lint: source internals/ctxt.rs
package io.github.kotlinmania.serderive.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.quote.append
import io.github.kotlinmania.syn.Field
import io.github.kotlinmania.syn.Path
import io.github.kotlinmania.syn.SynError
import io.github.kotlinmania.syn.SynResult
import io.github.kotlinmania.syn.Variant

public class Ctxt {
    private var errors: MutableList<SynError>? = mutableListOf()

    public fun errorSpannedBy(obj: ToTokens, msg: String) {
        checkNotNull(errors).add(SynError.newSpanned(obj.toTokenStream(), msg))
    }

    public fun errorSpannedBy(tokens: TokenStream, msg: String) {
        checkNotNull(errors).add(SynError.newSpanned(tokens, msg))
    }

    public fun errorSpannedBy(path: Path, msg: String) {
        val out = TokenStream.new()
        path.toTokens(out)
        checkNotNull(errors).add(SynError.newSpanned(out, msg))
    }

    public fun errorSpannedBy(field: Field, msg: String) {
        val out = TokenStream.new()
        field.toTokens(out)
        checkNotNull(errors).add(SynError.newSpanned(out, msg))
    }

    public fun errorSpannedBy(variant: Variant, msg: String) {
        val out = TokenStream.new()
        variant.toTokens(out)
        checkNotNull(errors).add(SynError.newSpanned(out, msg))
    }

    public fun errorSpannedBy(ident: io.github.kotlinmania.procmacro2.Ident, msg: String) {
        val ts = TokenStream.new()
        ts.append(ident)
        checkNotNull(errors).add(SynError.newSpanned(ts, msg))
    }

    public fun errorSpannedBy(synType: io.github.kotlinmania.syn.SynType, msg: String) {
        val out = TokenStream.new()
        synType.toTokens(out)
        checkNotNull(errors).add(SynError.newSpanned(out, msg))
    }

    public fun synError(err: SynError) {
        checkNotNull(errors).add(err)
    }

    public fun check(): SynResult<Unit> {
        val currentErrors = checkNotNull(errors)
        errors = null

        val iterator = currentErrors.iterator()
        if (!iterator.hasNext()) {
            return SynResult.success(Unit)
        }

        val combined = iterator.next()
        while (iterator.hasNext()) {
            combined.combine(iterator.next())
        }

        return SynResult.failure(combined)
    }

    // Note: Kotlin doesn't have Drop. Calling `check()` manually is strictly required!
}
