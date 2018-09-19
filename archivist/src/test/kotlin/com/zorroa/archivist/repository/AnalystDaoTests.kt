package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.AnalystState
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalystDaoTests : AbstractTest() {

    @Autowired
    lateinit var analystDao: AnalystDao

    lateinit var analyst: Analyst
    lateinit var spec: AnalystSpec

    @Before
    fun init() {
        spec = AnalystSpec(
                1024,
                648,
                0.5f,
                null)
        analyst = analystDao.create(spec)
    }

    @Test
    fun testCreate() {
        assertEquals(spec.taskId, analyst.taskId)
        assertEquals(spec.totalRamMb, analyst.totalRamMb)
        assertEquals(spec.freeRamMb, analyst.freeRamMb)
        assertEquals(spec.load, analyst.load)
    }

    @Test
    fun testGet() {
        val a1 = analystDao.get(analyst.id)
        assertEquals(a1.endpoint, analyst.endpoint)
        assertEquals(a1.taskId, analyst.taskId)
        assertEquals(a1.totalRamMb, analyst.totalRamMb)
        assertEquals(a1.freeRamMb, analyst.freeRamMb)
        assertEquals(a1.load, analyst.load)
    }

    @Test
    fun testGetByEndpoint() {
        val a1 = analystDao.get(analyst.endpoint)
        assertEquals(a1.endpoint, analyst.endpoint)
        assertEquals(a1.taskId, analyst.taskId)
        assertEquals(a1.totalRamMb, analyst.totalRamMb)
        assertEquals(a1.freeRamMb, analyst.freeRamMb)
        assertEquals(a1.load, analyst.load)
    }

    @Test
    fun testExists() {
        assertTrue(analystDao.exists(analyst.endpoint))
        assertFalse(analystDao.exists("fooo"))
    }

    @Test
    fun testSetState() {
        assertTrue(analystDao.setState(analyst, AnalystState.Down))
        assertFalse(analystDao.setState(analyst, AnalystState.Down))
        assertTrue(analystDao.setState(analyst, AnalystState.Up))
    }

}