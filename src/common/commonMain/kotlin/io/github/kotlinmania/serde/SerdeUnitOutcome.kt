package io.github.kotlinmania.serde

/** Completion or the original diagnostic from a unit-valued operation. */
class SerdeUnitOutcome private constructor(
    val completed: Boolean,
    val error: SerdeError?,
) {
    init {
        require(completed == (error == null))
    }

    val isSuccess: Boolean get() = completed

    val isFailure: Boolean get() = !completed

    companion object {
        fun success(): SerdeUnitOutcome = SerdeUnitOutcome(true, null)

        fun failure(error: SerdeError): SerdeUnitOutcome = SerdeUnitOutcome(false, error)
    }
}
