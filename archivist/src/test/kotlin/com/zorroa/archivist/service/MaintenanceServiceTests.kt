package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class MaintenanceServiceTests : AbstractTest() {

    @Autowired
    lateinit var maintenanceService: MaintenanceService

    @Test
    fun testRunAll() {
        maintenanceService.runAll()
    }

    @Test
    fun testRemoveExpiredJobData() {
        maintenanceService.handleExpiredJobs()
    }
}