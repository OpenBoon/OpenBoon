package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.CredentialsSpec
import boonai.archivist.domain.CredentialsType
import boonai.archivist.domain.DataSourceDelete
import boonai.archivist.domain.DataSourceImportOptions
import boonai.archivist.domain.DataSourceSpec
import boonai.archivist.domain.FileType
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.ReprocessAssetSearchRequest
import boonai.archivist.domain.StandardContainers
import boonai.archivist.domain.TaskState
import boonai.archivist.domain.emptyZpsScripts
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataRetrievalFailureException
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class JobLauncherServiceTests : AbstractTest() {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var jobLaunchService: JobLaunchService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var pipelineResolverService: PipelineResolverService

    @Autowired
    lateinit var jobService: JobService

    val dsSpec = DataSourceSpec(
        "dev-data",
        "gs://boonai-dev-data",
        fileTypes = FileType.allTypes()
    )

    @Before
    fun setUp() {
        pipelineModService.updateStandardMods()
    }

    @Test
    fun testCreateAnalysisJobWithCredentials() {
        credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )
        val spec2 = DataSourceSpec(
            "dev-data",
            "gs://boonai-dev-data",
            fileTypes = FileType.allTypes(),
            credentials = setOf("test")
        )
        val ds = dataSourceService.create(spec2)
        entityManager.flush()
        entityManager.clear()
        val ds2 = dataSourceService.get(ds.id)
        val job = jobLaunchService.launchJob(ds2, DataSourceImportOptions())

        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT COUNT(1) FROM x_credentials_job WHERE pk_job=?",
                Int::class.java, job.jobId
            )
        )
    }

    @Test
    fun testLaunchReprocessAssetSearch() {

        val spec = AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
        spec.attrs = mapOf("analysis.boonai.similarity.vector" to "AABBCC00")

        val batchCreate = BatchCreateAssetsRequest(listOf(spec), state = AssetState.Analyzed)
        assetService.batchCreate(batchCreate)

        val req = ReprocessAssetSearchRequest(
            mapOf(),
            listOf("boonai-label-detection")
        )
        val rsp = jobLaunchService.launchJob(req)
        assertEquals("Applying modules: boonai-label-detection to 1 assets", rsp.job.name)
        assertEquals(1, rsp.assetCount)
    }

    @Test
    fun testLaunchReprocessAssetSearchRename() {

        val spec = AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
        spec.attrs = mapOf("analysis.boonai.similarity.vector" to "AABBCC00")

        val batchCreate = BatchCreateAssetsRequest(listOf(spec), state = AssetState.Analyzed)
        assetService.batchCreate(batchCreate)

        val req = ReprocessAssetSearchRequest(
            mapOf(),
            listOf("boonai-label-detection"),
            name = "boomsauce"
        )
        val rsp = jobLaunchService.launchJob(req)
        assertEquals(req.name, rsp.job.name)
        assertEquals(1, rsp.assetCount)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testLaunchReprocessAssetSearch_noAssetsFailure() {
        val req = ReprocessAssetSearchRequest(
            mapOf(),
            listOf("boonai-labels")
        )
        jobLaunchService.launchJob(req)
    }

    @Test
    fun testLaunchReprocessAssetSearchWithDepends() {

        val spec = AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
        spec.attrs = mapOf("analysis.boonai.similarity.vector" to "AABBCC00")

        val batchCreate = BatchCreateAssetsRequest(listOf(spec), state = AssetState.Analyzed)
        assetService.batchCreate(batchCreate)

        val otherJob = jobService.create(
            JobSpec(
                "foo",
                emptyZpsScripts("foo")
            )
        )

        val req = ReprocessAssetSearchRequest(
            mapOf(),
            listOf("boonai-label-detection"),
            dependOnJobIds = listOf(otherJob.id)
        )

        val rsp = jobLaunchService.launchJob(req)
        assertEquals("Applying modules: boonai-label-detection to 1 assets", rsp.job.name)
        assertEquals(1, rsp.assetCount)

        val tasks = jobService.getTasks(rsp.job.id).list
        assertEquals(TaskState.Depend, tasks[0].state)
    }

    @Test
    fun testLaunchJobWithGenerator() {
        val name = "test"
        val gen = ProcessorRef("boonai_core.core.generators.GcsBucketGenerator", StandardContainers.CORE)
        val pipeline = pipelineResolverService.resolve()
        val job = jobLaunchService.launchJob(name, gen, pipeline)
        assertEquals("test", job.name)

        val tasks = jobService.getTasks(job.id)
        val script = jobService.getZpsScript(tasks.first().id)
        assertEquals("boonai_core.core.generators.GcsBucketGenerator", script.generate!![0]!!.className)
    }

    @Test
    fun testLaunchJobWithAssetIds() {
        val assets = listOf(
            Asset("abc123", mutableMapOf("foo" to "bar")),
            Asset("abc234", mutableMapOf("bing" to "bong"))
        )
        val name = "test"

        val pipeline = pipelineResolverService.resolve()
        val job = jobLaunchService.launchJob(name, assets.map { it.id }, pipeline)

        val tasks = jobService.getTasks(job.id)
        val script = jobService.getZpsScript(tasks.first().id)
        assertEquals(2, script.assetIds?.size)
    }

    @Test
    fun testLaunchDeleteDataSourceJob() {

        credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )
        val spec = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            fileTypes = FileType.allTypes(),
            credentials = setOf("test")
        )
        val ds = dataSourceService.create(spec)

        val launchJob = jobLaunchService.launchJob(ds, DataSourceDelete(deleteAssets = true))
        val tasks = jobService.getTasks(launchJob.id)
        val script = jobService.getZpsScript(tasks.first().id)

        assertEquals("boonai_core.core.processors.DeleteBySearchProcessor", script.execute!![0]!!.className)
        assertEquals(ds.id.toString(), script.execute?.get(0)?.args?.get("dataSourceId"))
    }

    @Test
    fun testLaunchTimelineAnalysisJob() {
        val job = jobLaunchService.launchTimelineAnalysisJob("abc123", "test")
        val tasks = jobService.getTasks(job.id)
        val script = jobService.getZpsScript(tasks.first().id)

        assertEquals("boonai_analysis.boonai.TimelineAnalysisProcessor", script.execute!![0]!!.className)
        assertEquals("abc123", script.execute?.get(0)?.args?.get("asset_id"))
        assertEquals("test", script.execute?.get(0)?.args?.get("timeline"))
    }

    @Test
    fun testAddTimelineAnalysisTask() {
        val assets = listOf(
            Asset("abc123", mutableMapOf("foo" to "bar")),
            Asset("abc234", mutableMapOf("bing" to "bong"))
        )

        val pipeline = pipelineResolverService.resolve()
        val job = jobLaunchService.launchJob("test", assets.map { it.id }, pipeline)

        val task = jobLaunchService.addTimelineAnalysisTask(job.id, "abc123", "test")
        val script = jobService.getZpsScript(task.id)

        assertEquals("boonai_analysis.boonai.TimelineAnalysisProcessor", script.execute!![0]!!.className)
        assertEquals("abc123", script.execute?.get(0)?.args?.get("asset_id"))
        assertEquals("test", script.execute?.get(0)?.args?.get("timeline"))
    }
}
