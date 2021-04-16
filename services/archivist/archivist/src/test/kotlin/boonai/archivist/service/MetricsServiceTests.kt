package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.emptyZpsScripts
import boonai.common.service.logging.MeterRegistryHolder
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class MetricsServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Test
    @Ignore
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
