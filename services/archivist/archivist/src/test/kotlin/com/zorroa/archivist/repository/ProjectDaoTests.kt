package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.security.getProjectId
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class ProjectDaoTests : AbstractTest() {

    @Autowired
    lateinit var projectDao: ProjectDao

    @Autowired
    lateinit var projectCustomDao: ProjectCustomDao

    @Test
    fun testUpdateDefaultPipeline() {
        val pipeline = pipelineService.create(PipelineSpec("footest"))
        projectCustomDao.updateDefaultPipeline(getProjectId(), pipeline)

        val pipelineId = jdbc.queryForObject(
            "SELECT pk_pipeline_default FROM project WHERE pk_project=?", UUID::class.java, getProjectId()
        )

        assertEquals(pipeline.id, pipelineId)
    }

    @Test
    fun testUpdateIndexRoute() {
        val testSpec = IndexRouteSpec("test", 1)
        val route = indexRoutingService.createIndexRoute(testSpec)

        projectCustomDao.updateIndexRoute(getProjectId(), route)
        val indexRouteId = jdbc.queryForObject(
            "SELECT pk_index_route FROM project WHERE pk_project=?", UUID::class.java, getProjectId()
        )

        assertEquals(route.id, indexRouteId)
    }
}
