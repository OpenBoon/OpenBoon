package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.LockStatus
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
        assertEquals(LockStatus.Locked, clusterLockDao.lock(ClusterLockSpec("foo")))
        assertEquals(LockStatus.Wait, clusterLockDao.lock(ClusterLockSpec("foo")))
        // Can't have calls to JDBC after this, TX is dead.
    }

    @Test
    fun testUnlock() {
        assertEquals(LockStatus.Locked, clusterLockDao.lock(ClusterLockSpec("foo")))
        assertTrue(clusterLockDao.unlock("foo"))
        assertFalse(clusterLockDao.unlock("foo"))
    }

    @Test
    fun testIsLocked() {
        clusterLockDao.lock(ClusterLockSpec("foo", timeout = 1, timeoutUnits = TimeUnit.MILLISECONDS))
        assertTrue(clusterLockDao.isLocked("foo"))
    }

    @Test
    fun testCheckLock() {
        clusterLockDao.lock(ClusterLockSpec("foo", timeout = 1, timeoutUnits = TimeUnit.MINUTES))
        assertTrue(clusterLockDao.checkLock("foo"))
        clusterLockDao.unlock("foo")
        assertFalse(clusterLockDao.checkLock("foo"))
    }

    @Test
    fun testGetExpired() {
        assertTrue(clusterLockDao.getExpired().isEmpty())
        clusterLockDao.lock(ClusterLockSpec("foo", timeout = 1, timeoutUnits = TimeUnit.MILLISECONDS))
        Thread.sleep(5)
        assertEquals(1, clusterLockDao.getExpired().size)
    }

    @Test
    fun testCombineLocks() {
        assertEquals(LockStatus.Locked, clusterLockDao.lock(ClusterLockSpec.combineLock("foo")))
        assertEquals(LockStatus.Combined, clusterLockDao.lock(ClusterLockSpec.combineLock("foo")))
        assertEquals(LockStatus.Combined, clusterLockDao.lock(ClusterLockSpec.combineLock("foo")))
        assertEquals(LockStatus.Combined, clusterLockDao.lock(ClusterLockSpec.combineLock("foo")))

        val count = jdbc.queryForObject("SELECT int_combine_count FROM cluster_lock WHERE str_name=?",
                Int::class.java, "foo")
        assertEquals(3, count)
    }

    @Test
    fun testHoldLock() {
        assertEquals(LockStatus.Locked,
                clusterLockDao.lock(ClusterLockSpec.combineLock("foo").apply {
                    holdTillTimeout=true
                    timeout = 500
                    timeoutUnits = TimeUnit.MILLISECONDS
                }))

        assertTrue(clusterLockDao.isLocked("foo"))
        Thread.sleep(1001)
        clusterLockDao.unlock(clusterLockDao.getExpired()[0].name)
        assertFalse(clusterLockDao.isLocked("counter"))
    }
}