package io.github.kotlinmania.serde

/** The concrete value or original diagnostic from a serialization operation. */
class SerdeBooleanOutcome private constructor(
    val value: Boolean?,
    val error: SerdeError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow(): Boolean = value ?: error!!.raise()

    fun exceptionOrNull(): SerdeError? = error

    companion object {
        fun success(value: Boolean): SerdeBooleanOutcome = SerdeBooleanOutcome(value, null)

        fun failure(error: SerdeError): SerdeBooleanOutcome = SerdeBooleanOutcome(null, error)
    }
}
