package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Category
import boonai.archivist.domain.DataSourceSpec
import boonai.archivist.domain.FileType
import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.ModelObjective
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineMode
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.Provider
import boonai.archivist.domain.emptyZpsScript
import boonai.archivist.security.getProjectId
import boonai.archivist.service.CredentialsService
import boonai.archivist.service.DataSourceService
import boonai.archivist.service.JobService
import boonai.archivist.service.ModelService
import boonai.archivist.service.PipelineModService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class ProjectDaoTests : AbstractTest() {

    @Autowired
    lateinit var projectCustomDao: ProjectCustomDao

    @Autowired
    lateinit var projectDao: ProjectDao

    @Autowired
    lateinit var projectDeleteDao: ProjectDeleteDao

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Test
    fun testGetMiniProject() {
        val minip = projectDao.getById(getProjectId())
        assertEquals(getProjectId(), minip.id)
        assertEquals("unittest", minip.name)
    }

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

    @Test
    fun testDeleteProjectRelatedObjects() {
        createJobAndTasks()
        createPipelineAndModule()
        createDataSource()

        val indexRoute = indexRoutingService.findOne(IndexRouteFilter(projectIds = listOf(getProjectId())))
        indexRoutingService.closeAndDeleteIndex(indexRoute)

        projectDeleteDao.deleteProjectRelatedObjects(getProjectId())

        val listOfTables = listOf(
            "project_quota",
            "project_quota_time_series",
            "processor",
            "module",
            "credentials",
            "pipeline",
            "automl",
            "model",
            "job",
            "datasource",
            "project"

        )
        listOfTables.forEach {
            assertEquals(
                0,
                jdbc.queryForObject("SELECT COUNT(*) FROM $it where pk_project=?", Int::class.java, getProjectId())
            )
        }
    }

    private fun createJobAndTasks() {

        val tspec = listOf(
            emptyZpsScript("foo"),
            emptyZpsScript("bar")
        )
        tspec[0].children = listOf(emptyZpsScript("foo1"))
        tspec[1].children = listOf(emptyZpsScript("bar"))

        val spec2 = JobSpec(
            null,
            tspec,
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(spec2)
    }

    private fun createPipelineAndModule() {
        val modularSpec = PipelineSpec(
            "mod-test",
            mode = PipelineMode.MODULAR,
            processors = listOf(
                ProcessorRef("com.zorroa.IngestImages", "image-foo"),
                ProcessorRef("com.zorroa.IngestVideo", "image-foo")
            )
        )

        val modSpec = PipelineModSpec(
            "test0", "test",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(),
            true
        )
        pipelineModService.create(modSpec)
        modularSpec.modules = listOf(modSpec.name)
        pipelineService.create(modularSpec)
    }

    private fun createDataSource() {
        val spec = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            fileTypes = FileType.allTypes()
        )
        dataSourceService.create(spec)
    }
}
