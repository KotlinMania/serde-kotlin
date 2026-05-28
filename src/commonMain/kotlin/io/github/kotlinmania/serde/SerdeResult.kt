@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.serde

import kotlin.native.HiddenFromObjC

/**
 * The result of a serde serialization or deserialization operation.
 *
 * Upstream carries typed errors via `Result<Ok, Error>` where `Ok` and
 * `Error` are associated types on the `Serializer`/`Deserializer` traits.
 * Kotlin's stdlib `Result<T>` erases the error type to `Throwable`, which
 * produces an unchecked-cast bridge under Swift export. `SerdeResult`
 * keeps the error typed, avoiding that hazard.
 *
 * Named `SerdeResult` to avoid colliding with Swift's built-in `Result`
 * type.
 */
@HiddenFromObjC
sealed class SerdeResult<out V, out E> {
    /** Successful result carrying the serialized/deserialized value. */
    @HiddenFromObjC
    data class Success<out V, out E>(val value: V) : SerdeResult<V, E>()

    /** Failed result carrying a typed error. */
    @HiddenFromObjC
    data class Failure<out V, out E>(val error: @UnsafeVariance E) : SerdeResult<V, E>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrThrow(): V = when (this) {
        is Success -> value
        is Failure -> throw IllegalStateException(error.toString())
    }

    fun getOrNull(): V? = (this as? Success)?.value

    fun exceptionOrNull(): E? = (this as? Failure)?.error

    fun getOrElse(onFailure: (E) -> @UnsafeVariance V): V = when (this) {
        is Success -> value
        is Failure -> onFailure(error)
    }

    fun <R> fold(onSuccess: (V) -> R, onFailure: (E) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }

    fun <R> map(transform: (V) -> R): SerdeResult<R, E> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(error)
    }

    companion object {
        fun <T> success(value: T): SerdeResult<T, Nothing> = Success(value)
        fun <T, Err> failure(error: Err): SerdeResult<T, Err> = Failure(error)
    }
}

/**
 * A generic serde error carrying a human-readable message.
 *
 * Not a `Throwable` subclass — Swift export's Class Stdlib hazard
 * (unchecked-cast bridge on `Throwable.getStackTrace()`) makes any
 * `Throwable` subclass unsafe in a public `SerdeResult` failure position.
 * When `getOrThrow()` is called on a `SerdeResult.Failure<SerdeError>`,
 * it throws `IllegalStateException(message)`.
 */
@HiddenFromObjC
class SerdeError(message: String) : IllegalStateException(message) {
    override fun toString(): String = message ?: "null"
}