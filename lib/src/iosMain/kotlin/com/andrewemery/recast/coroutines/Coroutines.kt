package com.andrewemery.recast.coroutines

import com.andrewemery.recast.job.Job
import com.andrewemery.recast.job.Result
import com.andrewemery.recast.job.launch
import kotlinx.coroutines.*
import platform.darwin.*
import kotlin.coroutines.CoroutineContext

actual object Dispatchers {
    // main loop dispatcher:
    // multithreaded coroutines are currently unsupported in kotlin/native.
    // https://github.com/Kotlin/kotlinx.coroutines/issues/462
    actual val IO: CoroutineDispatcher = MainLoopDispatcher
}

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    kotlinx.coroutines.runBlocking(context, block)

actual fun <T> runBackground(scope: CoroutineScope, context: CoroutineContext, operation: suspend () -> T, callback: (Result<T>) -> Unit): Job {
    return callback.launch(scope, context) { operation() }
}

@UseExperimental(InternalCoroutinesApi::class)
private object MainLoopDispatcher : CoroutineDispatcher(), Delay {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatch_async(dispatch_get_main_queue()) {
            block.run()
        }
    }

    @InternalCoroutinesApi
    @ExperimentalCoroutinesApi
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
            with(continuation) {
                resumeUndispatched(Unit)
            }
        }
    }

    @InternalCoroutinesApi
    override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
        val handle = object : DisposableHandle {
            var disposed = false
                private set

            override fun dispose() {
                disposed = true
            }
        }
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
            if (!handle.disposed) {
                block.run()
            }
        }

        return handle
    }

}