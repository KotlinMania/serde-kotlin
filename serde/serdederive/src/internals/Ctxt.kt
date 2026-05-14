// port-lint: source serde_derive/src/internals/ctxt.rs
package io.github.kotlinmania.serde.serdederive.src.internals

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.Error as SynError

/**
 * A type to collect errors together and format them.
 *
 * Letting this object go unchecked is a programming error. It must be consumed using `check`.
 *
 * References can be shared since this type uses run-time exclusive mutation checking.
 */
public class Ctxt private constructor(
    private var errors: MutableList<SynError>?,
) {
    public companion object {
        /**
         * Create a new context object.
         *
         * This object contains no errors, but still must be `check`ed.
         */
        public fun new(): Ctxt =
            Ctxt(mutableListOf())
    }

    /**
     * Add an error to the context object with a tokenizable object.
     *
     * The object is used for spanning in error messages.
     */
    public fun errorSpannedBy(obj: ToTokens, msg: Any?) {
        val tokens = TokenStream.new()
        obj.toTokens(tokens)
        requireNotNull(errors)
            // Curb monomorphization from generating too many identical methods.
            .add(SynError.newSpanned(tokens, msg.toString()))
    }

    /**
     * Add one of Syn's parse errors.
     */
    public fun synError(err: SynError) {
        requireNotNull(errors).add(err)
    }

    /**
     * Consume this object, producing a formatted error string if there are errors.
     */
    public fun check(): Result<Unit> {
        val collected = requireNotNull(errors)
        errors = null

        val iterator = collected.iterator()
        if (!iterator.hasNext()) {
            return Result.success(Unit)
        }

        val combined = iterator.next()
        for (rest in iterator) {
            combined.combine(rest)
        }

        return Result.failure(combined)
    }
}
