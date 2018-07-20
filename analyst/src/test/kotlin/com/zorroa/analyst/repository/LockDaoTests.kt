package com.zorroa.analyst.repository

import com.zorroa.analyst.AbstractTest
import com.zorroa.analyst.domain.LockSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class LockDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var lockDao: LockDao

    @Test
    fun testCreate() {
        val spec = LockSpec(UUID.randomUUID(), UUID.randomUUID())
        val lock = lockDao.create(spec)
        assertEquals(lock.assetId, spec.assetId)
        assertEquals(lock.jobId, spec.jobId)
    }

    @Test
    fun testGet() {
        val spec = LockSpec(UUID.randomUUID(), UUID.randomUUID())
        val lock1 = lockDao.create(spec)
        val lock2 = lockDao.get(lock1.id)
        assertEquals(lock1, lock2)
    }

    @Test
    fun testGetByAsset() {
        val spec = LockSpec(UUID.randomUUID(), UUID.randomUUID())
        val lock1 = lockDao.create(spec)
        val lock2 = lockDao.getByAsset(spec.assetId)
        assertEquals(lock1, lock2)
    }

    @Test
    fun testDeleteByJob() {
        val spec = LockSpec(UUID.randomUUID(), UUID.randomUUID())
        lockDao.create(spec)
        assertEquals(1, lockDao.deleteByJob(spec.jobId))
    }
}
