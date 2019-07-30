package com.andrewemery.recast.sample.api

import com.andrewemery.recast.annotation.RecastAsync
import com.andrewemery.recast.annotation.RecastSync

interface SampleInterface {

    @RecastSync
    @RecastAsync
    suspend fun getUser(id: String): User {
        return User(id)
    }
}

class SampleClass {

    @RecastSync
    @RecastAsync
    suspend fun getUser(id: String): User {
        return User(id)
    }
}

object SampleObject {

    @RecastSync
    @RecastAsync
    suspend fun getUser(id: String): User {
        return User(id)
    }
}

@RecastSync
@RecastAsync
class SampleRecastClass {

    suspend fun getUser(id: String): User {
        return User(id)
    }
}

@RecastSync
@RecastAsync(suffix = "Async")
suspend fun getUsers(): List<User> {
    return listOf(User("1"))
}

data class User(val id: String)
