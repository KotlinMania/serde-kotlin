package io.github.kotlinmania.serde

/** The concrete value or original diagnostic from a serialization operation. */
class SerdeStringOutcome private constructor(
    val value: String?,
    val error: SerdeError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow(): String =
        when {
            value != null -> value
            error != null -> error.raise()
            else -> kotlin.error("SerdeStringOutcome invariant violated")
        }

    fun exceptionOrNull(): SerdeError? = error

    companion object {
        fun success(value: String): SerdeStringOutcome = SerdeStringOutcome(value, null)

        fun failure(error: SerdeError): SerdeStringOutcome = SerdeStringOutcome(null, error)
    }
}
