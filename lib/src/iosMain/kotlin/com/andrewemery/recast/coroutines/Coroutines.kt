package com.andrewemery.recast.coroutines

import com.andrewemery.recast.job.Job
import com.andrewemery.recast.job.Result
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.darwin.dispatch_async_f
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.*
import kotlin.test.assertNotNull

actual object Dispatchers {
    // unconfined: working under the assumption that all asynchronous tasks will
    // either be run synchronously (with @RecastSync) on a background thread spawned
    // manually, or run asynchronously (with @RecastAsync) on a background thread
    // spawned as part of the recasting.
    actual val IO: CoroutineDispatcher = Dispatchers.Unconfined
}

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T =
    kotlinx.coroutines.runBlocking(context, block)

actual fun <T> runBackground(scope: CoroutineScope, context: CoroutineContext, operation: suspend () -> T, callback: (Result<T>) -> Unit): Job {
    val worker = Worker.start()
    val job = WorkerJob(worker)
    val op1 = Continuator.wrap({ Result.of { kotlinx.coroutines.runBlocking { operation() } } }.freeze()) {
        if (!job.cancelled && scope.coroutineContext[kotlinx.coroutines.Job]?.isCancelled == false) callback(it)
    }
    worker.execute(TransferMode.SAFE, { op1 }) { input -> input() }
    return job
}

@ThreadLocal
private object Continuator {
    private val map = mutableMapOf<Any, Pair<Int, *>>()

    @Suppress("RedundantLambdaArrow")
    fun <P> wrap(operation: () -> P, block: (P) -> Unit): () -> Unit {
        assert(NSThread.isMainThread())
        assert(operation.isFrozen)
        val id = Any().freeze()
        map[id] = Pair(1, block)
        return {
            initRuntimeIfNeeded()
            executeAsync(NSOperationQueue.mainQueue) {
                Pair(Pair(id, operation()), { it: Pair<Any, P> ->
                    execute(it.first, it.second)
                })
            }
        }.freeze()
    }

    @Suppress("UNCHECKED_CAST")
    fun <P> execute(id: Any, parameter: P) {
        val countAndBlock = map.remove(id)
        assertNotNull(countAndBlock)
        assert(countAndBlock.first == 1)
        (countAndBlock.second as Function1<P, Unit>)(parameter)
    }
}

private inline fun <reified T> executeAsync(queue: NSOperationQueue, crossinline producerConsumer: () -> Pair<T, (T) -> Unit>) {
    dispatch_async_f(queue.underlyingQueue, DetachedObjectGraph {
        producerConsumer()
    }.asCPointer(), staticCFunction { it ->
        val result = DetachedObjectGraph<Pair<T, (T) -> Unit>>(it).attach()
        result.second(result.first)
    })
}

private class WorkerJob(private val worker: Worker) : Job {
    var cancelled: Boolean = false
    override fun cancel() {
        cancelled = true
        worker.requestTermination(false)
    }
}
