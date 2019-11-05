package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.JobType
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.domain.JobPriority
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class IndexMigrationServiceTests : AbstractTest() {

    @Autowired
    lateinit var indexMigrationService: IndexMigrationService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun testMigrate() {

        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        val mspec = IndexMigrationSpec(route.id, true, listOf("bob", "dole"), mapOf("bob" to "dole"))
        val job = indexMigrationService.migrate(mspec)
        val task = dispatcherService.getWaitingTasks(getProjectId(), 1)[0]

        /*
         * Test that our migration job as all the right settings.
         */
        assertEquals(JobPriority.Reindex, job.priority)
        assertEquals(JobType.Batch, job.type)
        assertEquals(task?.script.generate!![0]!!.env["ZORROA_INDEX_ROUTE_ID"], "00000000-0000-0000-0000-000000000000")
        assertEquals(task?.script.execute!![1]!!.env["ZORROA_INDEX_ROUTE_ID"], route.id.toString())

        assertEquals(task?.script.execute!![0]!!.args!!["attrs"], mapOf("bob" to "dole"))
        assertEquals(task?.script.execute!![0]!!.args!!["removeAttrs"], listOf("bob", "dole"))
    }

    @Test
    fun testMigrateDontSwapRoute() {

        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        val mspec = IndexMigrationSpec(route.id, false)
        indexMigrationService.migrate(mspec)
    }
}
