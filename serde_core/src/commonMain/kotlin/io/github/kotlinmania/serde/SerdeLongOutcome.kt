package io.github.kotlinmania.serde

/** The concrete value or original diagnostic from a serialization operation. */
class SerdeLongOutcome private constructor(
    val value: Long?,
    val error: SerdeError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow(): Long = value ?: error!!.raise()

    fun exceptionOrNull(): SerdeError? = error

    companion object {
        fun success(value: Long): SerdeLongOutcome = SerdeLongOutcome(value, null)

        fun failure(error: SerdeError): SerdeLongOutcome = SerdeLongOutcome(null, error)
    }
}
