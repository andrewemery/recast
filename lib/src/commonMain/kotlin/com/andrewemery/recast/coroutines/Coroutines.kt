package com.andrewemery.recast.coroutines

import com.andrewemery.recast.job.Job
import com.andrewemery.recast.job.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

expect object Dispatchers {
    val IO: CoroutineDispatcher
}

/**
 * Runs new coroutine and blocks the current thread interruptibly until its completion.
 * This function should not be used from coroutine. It is designed to bridge regular blocking code
 * to libraries that are written in suspending style, to be used in `main` functions and in tests.
 */
expect fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

/**
 * Run the given operation in the background, returning the result to the main thread.
 * This method must be called from the main thread.
 *
 * @param scope The coroutine scope to launch the operation within.
 * @param operation The operation to perform in the background.
 * @param callback The callback to call on the completion of the operation.
 */
expect fun <T> runBackground(scope: CoroutineScope, context: CoroutineContext = EmptyCoroutineContext, operation: suspend () -> T, callback: (Result<T>) -> Unit): Job
