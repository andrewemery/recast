package com.andrewemery.recast.sample

import com.andrewemery.recast.sample.api.SampleClass
import com.andrewemery.recast.sample.api.User

/**
 * Created by Andrew Emery on 2019-07-30.
 */
class Example {
    suspend fun getUser(id: String): User {
        return SampleClass().getUser(id)
    }
}