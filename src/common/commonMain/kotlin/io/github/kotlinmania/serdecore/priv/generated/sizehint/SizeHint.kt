// port-lint: source private/size_hint.rs
package io.github.kotlinmania.serdecore.priv.generated.sizehint

// Generated from Syn AST; exact original retained in input.rs.
// Rust use resolved through explicit library bindings.
fun <`I`> `from_bounds`(`iter`: `I`, `__rtkBound_I`: RtkBound0<`I`>): RtkOption<kotlin.ULong> = `helper`(`__rtkBound_I`.`size_hint`(`iter`))

// Required Rust size evidence for Element; aarch64-apple-darwin, rustc 7189d39d9ce936881b8132c6addeaf3e295cd455. Generic callers forward the same type's evidence; no default.
fun `cautious`(`hint`: RtkOption<kotlin.ULong>, `__rtkSize_Element`: kotlin.ULong): kotlin.ULong =
    kotlin.run {
        val `MAX_PREALLOC_BYTES`: kotlin.ULong = 1048576uL
        (
            if ((`__rtkSize_Element` == 0uL)) {
                0uL
            } else {
                (
                    kotlin.comparisons.minOf(
                        __rtkUnwrapOr(`hint`, 0uL),
                        (
                            io.github.kotlinmania.numtraits.U64Num
                                .checkedDiv(`MAX_PREALLOC_BYTES`, `__rtkSize_Element`) ?: throw RtkPanic("attempt to divide by zero")
                        ),
                    )
                )
            }
        )
    }

private fun `helper`(`bounds`: kotlin.Pair<kotlin.ULong, RtkOption<kotlin.ULong>>): RtkOption<kotlin.ULong> =
    kotlin.run {
        val __rtkValue0: kotlin.Pair<kotlin.ULong, RtkOption<kotlin.ULong>> = `bounds`
        when {
            ((true && ((__rtkValue0).second is RtkOption.Some && true)) && (((__rtkValue0).first == (((__rtkValue0).second) as RtkOption.Some<kotlin.ULong>).value))) -> RtkOption.Some((((__rtkValue0).second) as RtkOption.Some<kotlin.ULong>).value)
            else -> RtkOption.None
        }
    }

// Explicit borrowed Rust trait evidence: core::iter::Iterator. Existing library operations.
sealed class RtkBound0<T> {
    abstract fun `size_hint`(receiver: T): kotlin.Pair<kotlin.ULong, RtkOption<kotlin.ULong>>
    class Case0<T : io.github.kotlinmania.serdecore.priv.IteratorWithSizeHint<*>> : RtkBound0<T>() {
        override fun `size_hint`(receiver: T): kotlin.Pair<kotlin.ULong, RtkOption<kotlin.ULong>> =
            kotlin.run {
                val bounds: kotlin.Pair<kotlin.ULong, kotlin.ULong?> = receiver.`sizeHint`()
                val upper = bounds.second
                kotlin.Pair(bounds.first, if (upper == null) RtkOption.None else RtkOption.Some(upper))
            }
    }
}

// Tagged Rust Option; owned payloads require a checked fresh-value context.
sealed class RtkOption<out T> {
    data object None : RtkOption<kotlin.Nothing>()
    data class Some<T>(
        val value: T,
    ) : RtkOption<T>()
}
private fun <T> __rtkUnwrapOr(value: RtkOption<T>, fallback: T): T =
    when (value) {
        RtkOption.None -> fallback
        is RtkOption.Some -> value.value
    }

private class RtkPanic(
    message: String,
) : kotlin.RuntimeException(message)
