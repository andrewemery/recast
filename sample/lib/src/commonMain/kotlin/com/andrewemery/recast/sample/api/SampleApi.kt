package com.andrewemery.recast.sample.api

import com.andrewemery.recast.annotation.RecastAsync
import com.andrewemery.recast.annotation.RecastSync
import com.andrewemery.recast.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SampleInterface {

    @RecastSync
    @RecastAsync
    suspend fun getUser(id: String): User = withContext(Dispatchers.IO) {
        getUsers()[0]
    }
}

class SampleClass {

    @RecastSync
    @RecastAsync
    suspend fun getUser(id: String): User = withContext(Dispatchers.IO) {
        User(id)
    }
}

object SampleObject {

    @RecastSync
    @RecastAsync
    suspend fun getUser(id: String): User = withContext(Dispatchers.IO) {
        User(id)
    }
}

@RecastSync
@RecastAsync
class SampleRecastClass {

    suspend fun getUser(id: String): User = withContext(Dispatchers.IO) {
        User(id)
    }
}

@RecastSync
@RecastAsync(suffix = "Async")
suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
    listOf(User("1"))
}

data class User(val id: String)