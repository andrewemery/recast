package com.andrewemery.recast.coroutines

import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value
import com.andrewemery.recast.job.Job
import com.andrewemery.recast.job.Result
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.*
import platform.Foundation.NSThread
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.*

actual object Dispatchers {
    // unconfined: working under the assumption that all asynchronous tasks will
    // either be run synchronously (with @RecastSync) on a background thread spawned
    // manually, or run asynchronously (with @RecastAsync) on a background thread
    // spawned as part of the recasting. this approach is taken because
    // multithreaded coroutines are currently unsupported in kotlin/native.
    // https://github.com/Kotlin/kotlinx.coroutines/issues/462
    actual val IO: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
}

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    kotlinx.coroutines.runBlocking(context, block)

actual fun <T> runBackground(
    scope: CoroutineScope,
    context: CoroutineContext,
    operation: suspend () -> T,
    callback: (Result<T>) -> Unit
): Job {
    assert(NSThread.isMainThread()) { "RecastAsync jobs must be called from the main thread" }
    val worker = Worker.start()
    val job = WorkerJob(worker)
    backgroundTask(worker, { Result.of { kotlinx.coroutines.runBlocking { operation() } } }) {
        if (job.cancelled || scope.coroutineContext[kotlinx.coroutines.Job]?.isCancelled == true) return@backgroundTask
        callback(it)
    }
    return job
}

private fun <B> backgroundTask(worker: Worker, backJob: () -> B, mainJob: (B) -> Unit) {
    val mainJobHolder = ThreadLocalRef<(B) -> Unit>()
    mainJobHolder.value = mainJob

    worker.execute(TransferMode.SAFE, { JobWrapper(backJob, mainJobHolder).freeze() }) { wrapper ->
        backToFront(wrapper.backgroundJob, {
            wrapper.mainJob.get()!!.invoke(it)
        })
    }
}

private fun <B> backToFront(b: () -> B, job: (B) -> Unit) {
    dispatch_async_f(dispatch_get_main_queue(), DetachedObjectGraph {
        JobResult(job, b()).freeze()
    }.asCPointer(), staticCFunction { it: COpaquePointer? ->
        initRuntimeIfNeeded()
        @Suppress("UNCHECKED_CAST") val result = DetachedObjectGraph<Any>(it).attach() as JobResult<B>
        result.job(result.result)
    })
}

private data class JobWrapper<B>(val backgroundJob: () -> B, val mainJob: ThreadLocalRef<(B) -> Unit>)
private data class JobResult<B>(val job: (B) -> Unit, val result: B)

private class WorkerJob(private val worker: Worker) : Job {
    var cancelled: Boolean = false
    override fun cancel() {
        cancelled = true
        worker.requestTermination(false)
    }
}

// convenience method to construct a scope generally suitable to oversee a set of coroutines (within a view model for example).
// note: to use this method within your application, expose a similar method in your multiplatform library.
// see the sample for an example.
fun supervisorScope(): SupervisorScope = SupervisorScope(CoroutineScope(SupervisorJob() + Dispatchers.IO))

class SupervisorScope(private val scope: CoroutineScope) : CoroutineScope by scope {
    fun cancel() = scope.cancel()
}