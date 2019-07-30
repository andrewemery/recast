package com.andrewemery.recast.job

/**
 * The [Result] represents the result (successful or exceptional) of an operation.
 *
 * The [Result] interface closely resembles the [kotlin.Result] (which is currently not recommended
 * for use outside of inline usage).
 */
class Result<T> internal constructor(private val value: Any?) {

    /**
     * Returns `true` if this instance represents successful outcome.
     * In this case [isFailure] returns `false`.
     */
    val isSuccess: Boolean get() = value !is Failure

    /**
     * Returns `true` if this instance represents failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    val isFailure: Boolean get() = value is Failure

    /**
     * Returns the encapsulated value if this instance represents [success][ResultImpl.isSuccess] or
     * `null` if it is [failure][ResultImpl.isFailure].
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrNull(): T? = when {
        isFailure -> null
        else -> value as T
    }

    /**
     * Returns the encapsulated exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     */
    fun exceptionOrNull(): Throwable? = when (value) {
        is Failure -> value.exception
        else -> null
    }

    override fun toString(): String = when (value) {
        is Failure -> value.toString()
        else -> "Success($value)"
    }

    companion object {
        fun <T> success(value: T): Result<T> = Result(value)
        fun <T> failure(exception: Throwable): Result<T> =
            Result(Failure(exception))

        fun <T> of(operation: () -> T): Result<T> {
            return try {
                success(operation())
            } catch (e: Exception) {
                failure(e)
            }
        }

        suspend fun <T> ofSuspend(operation: suspend () -> T): Result<T> {
            return try {
                success(operation())
            } catch (e: Exception) {
                failure(e)
            }
        }
    }

    private class Failure(val exception: Throwable) {
        override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }

}