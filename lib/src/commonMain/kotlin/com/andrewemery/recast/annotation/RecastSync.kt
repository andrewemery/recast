package com.andrewemery.recast.annotation

/**
 * The [RecastSync] annotation is set against a suspending function and is used to generate an
 * equivalent synchronous method.
 *
 * For example, the following annotated interface:
 * ```
 * @RecastSync
 * suspend fun getUser(id: Int): User
 * ```
 *
 * Generates a synchronous function with the following signature:
 * ```
 * fun getUsers(id: Int): User
 * ```
 *
 * @param suffix The suffix to add to the generated method name.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class RecastSync(
    val suffix: String = "Sync"
)
