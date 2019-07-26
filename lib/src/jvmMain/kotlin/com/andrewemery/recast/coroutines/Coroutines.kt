package com.andrewemery.recast.coroutines

import com.andrewemery.recast.job.Job
import com.andrewemery.recast.job.Result
import com.andrewemery.recast.job.launch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual object Dispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
}

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    kotlinx.coroutines.runBlocking(context, block)

actual fun <T> runBackground(scope: CoroutineScope, context: CoroutineContext, operation: suspend () -> T, callback: (Result<T>) -> Unit): Job {
    return callback.launch(scope, context) { operation() }
}