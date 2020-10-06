package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AutomlSessionSpec
import com.zorroa.archivist.domain.Category
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.FileType
import com.zorroa.archivist.domain.IndexRouteFilter
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.ModelObjective
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineMode
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.Provider
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.service.AutomlService
import com.zorroa.archivist.service.CredentialsService
import com.zorroa.archivist.service.DataSourceService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.service.ModelService
import com.zorroa.archivist.service.PipelineModService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class ProjectDaoTests : AbstractTest() {

    @Autowired
    lateinit var projectDao: ProjectDao

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

    @Test
    fun testDeleteProjectRelatedObjects() {
        createJobAndTasks()
        createPipelineAndModule()
        createAutoMl()
        createDataSource()

        val indexRoute = indexRoutingService.findOne(IndexRouteFilter(projectIds = listOf(getProjectId())))
        indexRoutingService.closeAndDeleteIndex(indexRoute)

        projectCustomDao.deleteProjectRelatedObjects(getProjectId())

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
            Provider.ZORROA,
            Category.ZORROA_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(),
            true
        )
        pipelineModService.create(modSpec)
        modularSpec.modules = listOf(modSpec.name)
        pipelineService.create(modularSpec)
    }

    private fun createAutoMl() {
        val modelSpec = ModelSpec("animals", ModelType.GCP_LABEL_DETECTION)
        val model = modelService.createModel(modelSpec)

        val automlSpec = AutomlSessionSpec(
            "project/foo/region/us-central/datasets/foo",
            "/foo/bar"
        )

        automlService.createSession(model, automlSpec)
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
