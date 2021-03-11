package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.security.getProjectId
import boonai.archivist.service.JobService
import boonai.archivist.service.CredentialsService
import boonai.archivist.service.PipelineModService
import boonai.archivist.service.AutomlService
import boonai.archivist.service.ModelService
import boonai.archivist.service.DataSourceService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class ProjectDaoTests : AbstractTest() {

    @Autowired
    lateinit var projectCustomDao: ProjectCustomDao

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var automlService: AutomlService

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var dataSourceService: DataSourceService

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
