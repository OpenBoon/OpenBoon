package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClusterLockDaoTests : AbstractTest() {

    @Autowired
    lateinit var clusterLockDao: ClusterLockDao

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun testLock() {
        assertTrue(clusterLockDao.lock("foo", 1, TimeUnit.MINUTES))
        assertFalse(clusterLockDao.lock("foo", 1, TimeUnit.MINUTES))
        // Can't have calls to JDBC after this, TX is dead.
    }

    @Test
    fun testUnlock() {
        assertTrue(clusterLockDao.lock("foo", 1, TimeUnit.MINUTES))
        assertTrue(clusterLockDao.unlock("foo"))
        assertFalse(clusterLockDao.unlock("foo"))
    }

    @Test
    fun testRemoveExpired() {
        assertTrue(clusterLockDao.lock("foo", 1, TimeUnit.MILLISECONDS))
        Thread.sleep(2)
        assertEquals(1, clusterLockDao.clearExpired())
        assertEquals(0, clusterLockDao.clearExpired())
    }
}