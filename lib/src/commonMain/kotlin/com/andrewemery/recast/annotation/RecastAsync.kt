package com.andrewemery.recast.annotation

/**
 * The [RecastAsync] annotation is set against a suspending function and is used to generate an
 * equivalent asynchronous method that uses a callback to return the result.
 *
 * For example, the following annotated interface:
 * ```
 * @RecastAsync
 * suspend fun getUser(id: Int): User
 * ```
 *
 * Generates an asynchronous function with the following signature:
 * ```
 * fun getUsers(id: Int, scope: CoroutineScope = MainScope(), callback: (Result<User>) -> Unit): Job
 * ```
 *
 * @param suffix The suffix to add to the generated method name.
 * @param scoped Whether generated methods include a scope parameter (true) or use the ```GlobalScope``` (false).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class RecastAsync(
        val suffix: String = "",
        val scoped: Boolean = false)
