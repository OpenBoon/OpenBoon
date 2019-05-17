package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.security.getUserId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

/**
 * Created by chambers on 7/11/17.
 */
class SharedLinkDaoTests : AbstractTest() {

    @Autowired
    lateinit var sharedLinkDao: SharedLinkDao

    @Test
    fun testCreate() {
        val spec = SharedLinkSpec(mapOf("foo" to "bar"))
        spec.userIds = setOf(getUserId())
        val link = sharedLinkDao.create(spec)
        assertEquals(spec.state, link.state)
    }

    @Test
    fun testGet() {
        val spec = SharedLinkSpec(mapOf("foo" to "bar"))
        spec.userIds = setOf(getUserId())
        val link1 = sharedLinkDao.create(spec)
        val link2 = sharedLinkDao.get(link1.id)
        assertEquals(link1.id, link2.id)
    }

    @Test
    fun testDeleteExpiredMiss() {
        val spec = SharedLinkSpec(mapOf<String, Any>("foo" to "bar"))
        spec.userIds = setOf(getUserId())
        sharedLinkDao.create(spec)

        assertEquals(0, sharedLinkDao.deleteExpired(Duration.parse("P1D")).toLong())
        assertEquals(1, (jdbc.queryForObject("SELECT COUNT(1) FROM shared_link",
                Int::class.java) as Int).toLong())
    }

    @Test
    fun testDeleteExpiredHit() {
        val spec = SharedLinkSpec(mapOf("foo" to "bar"))
        spec.userIds = setOf(getUserId())
        sharedLinkDao.create(spec)

        Thread.sleep(500)
        assertEquals(0, sharedLinkDao.deleteExpired(Duration.parse("PT1S")).toLong())
        Thread.sleep(600)
        assertEquals(1, sharedLinkDao.deleteExpired(Duration.parse("PT1S")).toLong())
        assertEquals(0, (jdbc.queryForObject("SELECT COUNT(1) FROM shared_link",
                Int::class.java) as Int).toLong())
    }
}
