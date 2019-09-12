package com.andrewemery.recast.sample.android

import com.andrewemery.recast.sample.api.ObjectExample
import com.andrewemery.recast.sample.api.getUserSync
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun `test sync`() {
        assertEquals("12", ObjectExample.getUserSync("12").id)
    }
}
