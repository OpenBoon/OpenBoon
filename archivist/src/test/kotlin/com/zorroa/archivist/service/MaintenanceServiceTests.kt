package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.AnalystState
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import kotlin.test.assertEquals

class MaintenanceServiceTests : AbstractTest() {

    @Autowired
    lateinit var maintenanceService: MaintenanceService

    @Autowired
    lateinit var analystService: AnalystService

    @Test
    fun testRunAll() {
        maintenanceService.runAll()
    }

    @Test
    fun testRemoveExpiredJobData() {
        maintenanceService.handleExpiredJobs()
    }

    @Test
    fun testHandleUnresponsiveAnalysts() {
        val spec = AnalystSpec(
                1024,
                648,
                1024,
                0.5f,
                "unknown",
                null)
        authenticateAsAnalyst()
        analystService.upsert(spec)

        jdbc.update("UPDATE analyst SET time_ping=?",
                System.currentTimeMillis() - Duration.parse("PT3M1S").toMillis())
        maintenanceService.handleUnresponsiveAnalysts()

        assertEquals(AnalystState.Down.ordinal,
                jdbc.queryForObject("SELECT int_state FROM analyst", Int::class.java))
    }

    @Test
    fun testHandleRemovingtUnresponsiveAnalysts() {
        val spec = AnalystSpec(
                1024,
                648,
                1024,
                0.5f,
                "unknown",
                null)
        authenticateAsAnalyst()
        analystService.upsert(spec)

        jdbc.update("UPDATE analyst SET int_state=?, time_ping=?",
                AnalystState.Down.ordinal,
                System.currentTimeMillis() - Duration.parse("P1D").toMillis())

        maintenanceService.handleUnresponsiveAnalysts()

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(1) FROM analyst", Int::class.java))
    }
}