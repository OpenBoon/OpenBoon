package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.service.TransactionEventManager
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import com.zorroa.archivist.security.getUserId
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import java.util.*

/**
 * Created by chambers on 7/11/17.
 */
class SharedLinkDaoTests : AbstractTest() {

    @Autowired
    lateinit var sharedLinkDao: SharedLinkDao

    @Test
    fun testCreate() {
        val spec = SharedLinkSpec()
        spec.isSendEmail = true
        spec.state = mapOf("foo" to "bar")
        spec.userIds = setOf(getUserId())
        spec.expireTimeMs = 1L
        val link = sharedLinkDao!!.create(spec)
        assertEquals(spec.state, link.state)
        assertTrue(link.expireTime > 0)
    }

    @Test
    fun testGet() {
        val spec = SharedLinkSpec()
        spec.isSendEmail = true
        spec.state = mapOf("foo" to "bar")
        spec.userIds = setOf(getUserId())
        spec.expireTimeMs = 1L
        val link1 = sharedLinkDao!!.create(spec)
        val link2 = sharedLinkDao!!.get(link1.id)
        assertEquals(link1, link2)
    }

    @Test
    fun testDeleteExpiredMiss() {
        val spec = SharedLinkSpec()
        spec.isSendEmail = true
        spec.state = mapOf<String, Any>("foo" to "bar")
        spec.userIds = setOf(getUserId())
        spec.expireTimeMs = 86400 * 1000L
        val link = sharedLinkDao!!.create(spec)

        assertEquals(0, sharedLinkDao!!.deleteExpired(System.currentTimeMillis()).toLong())
        assertEquals(1, (jdbc.queryForObject("SELECT COUNT(1) FROM shared_link", Int::class.java) as Int).toLong())
    }

    @Test
    fun testDeleteExpiredHit() {
        val spec = SharedLinkSpec()
        spec.isSendEmail = true
        spec.state = mapOf("foo" to "bar")
        spec.userIds = setOf(getUserId())
        spec.expireTimeMs = 1L
        val link = sharedLinkDao!!.create(spec)

        assertEquals(1, sharedLinkDao!!.deleteExpired(System.currentTimeMillis() + 10).toLong())
        assertEquals(0, (jdbc.queryForObject("SELECT COUNT(1) FROM shared_link", Int::class.java) as Int).toLong())
    }
}
