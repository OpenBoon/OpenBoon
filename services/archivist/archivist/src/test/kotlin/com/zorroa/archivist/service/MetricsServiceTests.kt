package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.emptyZpsScripts
import com.zorroa.zmlp.service.logging.MeterRegistryHolder
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class MetricsServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Test
    fun testPendingTasksGauge() {
        var pending = MeterRegistryHolder.meterRegistry.get("tasks.pending")
        var max = MeterRegistryHolder.meterRegistry.get("tasks.max_running")
        assertEquals(0.0, pending.gauge().value())

        val spec1 = JobSpec(
            "test_job",
            emptyZpsScripts("priority"),
            args = mutableMapOf("captain" to "kirk")
        )
        val create = jobService.create(spec1)

        assertEquals(1.0, pending.gauge().value())
        assertEquals(create.maxRunningTasks.toDouble(), max.gauge().value())
    }
}
