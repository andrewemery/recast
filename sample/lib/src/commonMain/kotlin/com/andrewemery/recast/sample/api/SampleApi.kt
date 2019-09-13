package com.andrewemery.recast.sample.api

import com.andrewemery.recast.annotation.Recast
import com.andrewemery.recast.annotation.RecastSync
import com.andrewemery.recast.coroutines.Dispatchers
import kotlinx.coroutines.delay

interface InterfaceExample {

    @Recast
    @RecastSync
    suspend fun getUser(id: String): User = User(id)
}

class ClassExample {

    @Recast
    @RecastSync
    suspend fun getUser(id: String): User = User(id)
}

object ObjectExample {

    @Recast
    @RecastSync
    suspend fun getUser(id: String): User = User(id)
}

@Recast
@RecastSync
class OverrideExample {

    @RecastSync(suffix = "Synchronous")  // overrides class declaration
    suspend fun getUser(id: String): User = User(id)
}

@RecastSync
@Recast(scoped = true)
suspend fun getUserScoped(id: String): User = User(id)

@RecastSync
@Recast(scoped = true)
suspend fun getUserDelayed(id: String): User {
    delay(1000)
    return User(id)
}

data class User(val id: String)