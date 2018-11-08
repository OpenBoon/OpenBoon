package com.zorroa.archivist

import com.zorroa.archivist.domain.UniqueRunnable
import com.zorroa.archivist.domain.UniqueTaskExecutor
import org.junit.Test

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class UniqueTaskExecutorTests {

    @Test
    @Throws(InterruptedException::class)
    fun testExecute() {
        val t = UniqueTaskExecutor()
        var result = t.execute(UniqueRunnable("butt") {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })
        assertTrue(result)
        Thread.sleep(100)

        result = t.execute(UniqueRunnable("butt") {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })
        assertTrue(result)

        result = t.execute(UniqueRunnable("butt") {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        })
        assertFalse(result)
    }


}
