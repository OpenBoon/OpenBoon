package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ClusterLockSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.atomic.LongAdder
import kotlin.test.assertEquals

class ClusterLockExecutorTests : AbstractTest() {

    @Autowired
    lateinit var clusterLockExecutor: ClusterLockExecutor

    @Test
    fun testLaunchSoftLock() {

        val count = LongAdder()

        clusterLockExecutor.launch(ClusterLockSpec.softLock("counter")) {
            count.increment()
            Thread.sleep(200)
        }

        // Will be skipped because counter is already
        clusterLockExecutor.launch(ClusterLockSpec.softLock("counter")) {
            count.increment()
        }

        Thread.sleep(1000)
        assertEquals(1, count.toInt())
    }

    @Test
    fun testLaunchHardLock() {

        val count = LongAdder()

        clusterLockExecutor.launch(ClusterLockSpec.hardLock("counter")) {
            count.increment()
        }

        // Will be skipped because counter is already
        clusterLockExecutor.launch(ClusterLockSpec.hardLock("counter")) {
            count.increment()
        }

        Thread.sleep(500)
        assertEquals(2, count.toInt())
    }

    @Test
    fun testCombinableLock() {

        val count = LongAdder()

        clusterLockExecutor.launch(ClusterLockSpec.combineLock("counter")) {
            count.increment()
            Thread.sleep(100)
        }

        // Sleep here so the first launch is running
        Thread.sleep(50)

        // Will be skipped because counter is already
        clusterLockExecutor.launch(ClusterLockSpec.combineLock("counter")) {
            // look mom, no code!
        }

        Thread.sleep(500)
        assertEquals(2, count.toInt())
    }
}