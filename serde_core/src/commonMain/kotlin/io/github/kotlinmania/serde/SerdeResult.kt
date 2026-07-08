package io.github.kotlinmania.serde

import io.github.kotlinmania.serde.SerdeResult

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serdecore.StdError

/**
 * The result of a serde serialization or deserialization operation.
 *
 * Upstream serde returns `Result<S::Ok, S::Error>`, where `Ok` and `Error`
 * are associated types projected from the concrete `Serializer`/`Deserializer`.
 * Kotlin has no associated types, and the original port modeled the return as
 * the stdlib `kotlin.Result<Ok>` — an inline value class over `Any?` whose
 * error is erased to `Throwable`. Swift Export cannot bridge that nominally:
 * it erases the value to `Any?` and emits unchecked casts into the generated
 * `KotlinStdlib.kt`, which fail under `allWarningsAsErrors = true`.
 *
 * `SerdeResult` is a concrete, serde-owned nominal type. It is strongly typed
 * over the success value [T] and carries a concrete, non-`Throwable` [SerdeError]
 * in the failure case, so Swift Export can bridge it by construction — no
 * `@HiddenFromObjC`, no stdlib erasure.
 *
 * Named `SerdeResult` (not `Result`) so it does not collide with Swift's
 * built-in `Result` type nor with `kotlin.Result`.
 */
sealed class SerdeResult<out T> {
    /** Successful result carrying the serialized/deserialized value. */
    class Success<out T>(
        val value: T,
    ) : SerdeResult<T>() {
        override fun equals(other: Any?): Boolean = other is Success<*> && other.value == value

        override fun hashCode(): Int = value?.hashCode() ?: 0

        override fun toString(): String = "SerdeResult.Success($value)"
    }

    /** Failed result carrying a typed, non-`Throwable` error. */
    class Failure(
        val error: SerdeError,
    ) : SerdeResult<Nothing>() {
        override fun equals(other: Any?): Boolean = other is Failure && other.error == error

        override fun hashCode(): Int = error.hashCode()

        override fun toString(): String = "SerdeResult.Failure($error)"
    }

    /** Returns `true` when this result is a [Success]. */
    val isSuccess: Boolean get() = this is Success

    /** Returns `true` when this result is a [Failure]. */
    val isFailure: Boolean get() = this is Failure

    /** Returns the success value, or throws [SerdeException] carrying the [SerdeError]. */
    fun getOrThrow(): T =
        when (this) {
            is Success -> value
            is Failure -> throw SerdeException(error)
        }

    /** Returns the success value, or `null` when this is a [Failure]. */
    fun getOrNull(): T? = (this as? Success)?.value

    /** Returns the [SerdeError], or `null` when this is a [Success]. */
    fun exceptionOrNull(): SerdeError? = (this as? Failure)?.error

    /** Returns the success value, or [default] when this is a [Failure]. */
    fun getOrDefault(default: @UnsafeVariance T): T =
        when (this) {
            is Success -> value
            is Failure -> default
        }

    /** Returns the success value, or the result of [onFailure] applied to the error. */
    inline fun getOrElse(onFailure: (SerdeError) -> @UnsafeVariance T): T =
        when (this) {
            is Success -> value
            is Failure -> onFailure(error)
        }

    /** Folds this result into a single value of type [R]. */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (SerdeError) -> R,
    ): R =
        when (this) {
            is Success -> onSuccess(value)
            is Failure -> onFailure(error)
        }

    /** Maps the success value with [transform], propagating a [Failure] unchanged. */
    inline fun <R> map(transform: (T) -> R): SerdeResult<R> =
        when (this) {
            is Success -> Success(transform(value))
            is Failure -> this
        }

    /** Maps the success value to another [SerdeResult] with [transform] (monadic bind). */
    inline fun <R> flatMap(transform: (T) -> SerdeResult<R>): SerdeResult<R> =
        when (this) {
            is Success -> transform(value)
            is Failure -> this
        }

    /** Runs [action] on the success value, returning this result unchanged. */
    inline fun onSuccess(action: (T) -> Unit): SerdeResult<T> {
        if (this is Success) action(value)
        return this
    }

    /** Runs [action] on the error, returning this result unchanged. */
    inline fun onFailure(action: (SerdeError) -> Unit): SerdeResult<T> {
        if (this is Failure) action(error)
        return this
    }

    /**
     * Recovers from a [Failure] by computing a replacement value with [transform],
     * capturing any exception [transform] throws as a new [Failure].
     *
     * Replacement for `kotlin.Result.recoverCatching`.
     */
    inline fun recoverCatching(transform: (SerdeError) -> @UnsafeVariance T): SerdeResult<T> =
        when (this) {
            is Success -> this
            is Failure -> serdeCatching { transform(error) }
        }

    companion object {
        /** Builds a successful result. */
        fun <T> success(value: T): SerdeResult<T> = Success(value)

        /** Builds a failed result from a [SerdeError]. */
        fun failure(error: SerdeError): SerdeResult<Nothing> = Failure(error)
    }
}

/**
 * Runs [block], capturing any thrown exception as a [SerdeResult.Failure].
 *
 * Replacement for `kotlin.runCatching`, which returns a `kotlin.Result`.
 * A [SerdeException] is unwrapped to its underlying [SerdeError]; any other
 * throwable is wrapped in a fresh [SerdeError] carrying its message.
 */
inline fun <T> serdeCatching(block: () -> T): SerdeResult<T> =
    try {
        SerdeResult.success(block())
    } catch (e: SerdeException) {
        SerdeResult.failure(e.error)
    } catch (e: Throwable) {
        SerdeResult.failure(SerdeError(e.message ?: e.toString()))
    }

/**
 * A generic serde error carrying a human-readable message.
 *
 * Intentionally **not** a `Throwable` subclass. Swift Export's Class-Stdlib
 * hazard drags the `Throwable.getStackTrace()` → `Array` bridge into the
 * generated `KotlinStdlib.kt` for any public `Throwable` subtype, which fails
 * under `allWarningsAsErrors = true`. Keeping `SerdeError` a plain class makes
 * it bridgeable, so it can sit in a public [SerdeResult.Failure] position.
 *
 * Kotlin callers that need to throw an error wrap it at the throw site via
 * [SerdeException] (see [SerdeResult.getOrThrow]).
 */
class SerdeError(
    val message: String,
    override val source: SerdeError? = null,
) : StdError {
    override fun equals(other: Any?): Boolean = other is SerdeError && other.message == message && other.source == source

    override fun hashCode(): Int = message.hashCode() * 31 + (source?.hashCode() ?: 0)

    override fun toString(): String = message

    companion object {
        /**
         * Raised when there is a general error.
         *
         * The message should not be capitalized and should not end with a period.
         */
        fun custom(msg: String): SerdeError = SerdeError(msg)

        /**
         * Raised when a `Deserialize` receives a type different from what it was expecting.
         */
        fun invalidType(
            unexp: io.github.kotlinmania.serdecore.de.Unexpected,
            exp: io.github.kotlinmania.serdecore.de.Expected,
        ): SerdeError = custom("invalid type: $unexp, expected ${exp.expecting()}")

        /**
         * Raised when a `Deserialize` receives a value of the right type but that is wrong for some
         * other reason.
         */
        fun invalidValue(
            unexp: io.github.kotlinmania.serdecore.de.Unexpected,
            exp: io.github.kotlinmania.serdecore.de.Expected,
        ): SerdeError = custom("invalid value: $unexp, expected ${exp.expecting()}")

        /**
         * Raised when deserializing a sequence or map and the input data contains too many or too
         * few elements.
         */
        fun invalidLength(
            len: Int,
            exp: io.github.kotlinmania.serdecore.de.Expected,
        ): SerdeError = custom("invalid length $len, expected ${exp.expecting()}")

        /**
         * Raised when a `Deserialize` enum type received a variant with an unrecognized name.
         */
        fun unknownVariant(
            variant: String,
            expected: List<String>,
        ): SerdeError =
            if (expected.isEmpty()) {
                custom("unknown variant `$variant`, there are no variants")
            } else {
                custom("unknown variant `$variant`, expected ${io.github.kotlinmania.serdecore.de.OneOf(expected)}")
            }

        /**
         * Raised when a `Deserialize` class type received a field with an unrecognized name.
         */
        fun unknownField(
            field: String,
            expected: List<String>,
        ): SerdeError =
            if (expected.isEmpty()) {
                custom("unknown field `$field`, there are no fields")
            } else {
                custom("unknown field `$field`, expected ${io.github.kotlinmania.serdecore.de.OneOf(expected)}")
            }

        /**
         * Raised when a `Deserialize` class type expected to receive a required field with a
         * particular name but that field was not present in the input.
         */
        fun missingField(field: String): SerdeError = custom("missing field `$field`")

        /**
         * Raised when a `Deserialize` class type received more than one of the same field.
         */
        fun duplicateField(field: String): SerdeError = custom("duplicate field `$field`")
    }
}

/**
 * Throwable wrapper used at Kotlin throw sites for a [SerdeError].
 *
 * This type only ever appears as a thrown exception — never in a public,
 * Swift-exported signature position — so its `Throwable` ancestry does not
 * reach the Swift Export bridge.
 */
class SerdeException(
    val error: SerdeError,
) : IllegalStateException(error.message)
