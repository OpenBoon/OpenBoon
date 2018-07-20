package com.zorroa.analyst.service

import com.zorroa.analyst.AbstractTest
import com.zorroa.analyst.repository.LockDao
import com.zorroa.common.domain.JobSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.web.WebAppConfiguration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@WebAppConfiguration
class JobServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var lockDao: LockDao

    @Test
    fun testCreate() {
        val spec = JobSpec(
                "unit-test-task",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t = jobService.create(spec)
        assertNotNull(t.id)
        assertEquals(spec.assetId, t.assetId)
        assertEquals(spec.organizationId, t.organizationId)
        assertEquals(spec.pipelines, t.pipelines)
        assertEquals(spec.name, t.name)
    }

    @Test
    fun testStart() {
        val spec = JobSpec(
                "unit-test-task",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t = jobService.create(spec)
        jobService.start(t)

        val lock = lockDao.getByAsset(spec.assetId)
        assertEquals(lock.jobId, t.id)
    }
}
