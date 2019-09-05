package com.andrewemery.recast.sample

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by Andrew Emery on 2019-09-05.
 */
class ExampleUnitTest {

    @Test
    fun `test get user equality`() = runBlocking {
        assertEquals("12", Example().getUser("12").id)
    }
}