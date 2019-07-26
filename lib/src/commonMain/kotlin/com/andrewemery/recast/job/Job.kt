package com.andrewemery.recast.job

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface Job {

    /**
     * Cancel the ongoing job.
     */
    fun cancel()

}

private class JobImpl(private val job: kotlinx.coroutines.Job) : Job {
    override fun cancel() {
        job.cancel()
    }
}

/**
 * Launch a coroutine from the given scope, calling the suspending function to perform the actual work.
 *
 * @param scope The scope to launch the coroutine within.
 * @param block The suspending function to perform the actual work.
 * @param T The type of successful result.
 * @return The launched job.
 */
fun <T> Function1<Result<T>, Unit>.launch(
    scope: CoroutineScope = GlobalScope,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> T
): Job {
    return JobImpl(scope.launch(context) {
        try {
            this@launch.invoke(Result.success(block()))
        } catch (e: Throwable) {
            this@launch.invoke(Result.failure(e))
        }
    })
}