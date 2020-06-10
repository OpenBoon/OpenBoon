package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsType
import com.zorroa.archivist.domain.DataSourceImportOptions
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ReprocessAssetSearchRequest
import com.zorroa.archivist.domain.StandardContainers
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
        "gs://zorroa-dev-data",
        fileTypes = listOf("jpg")
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
            "gs://zorroa-dev-data",
            fileTypes = listOf("jpg"),
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
        spec.attrs = mapOf("analysis.zmlp.similarity.vector" to "AABBCC00")

        val batchCreate = BatchCreateAssetsRequest(listOf(spec), state = AssetState.Analyzed)
        assetService.batchCreate(batchCreate)

        val req = ReprocessAssetSearchRequest(
            mapOf(),
            listOf("zvi-label-detection")
        )
        val rsp = jobLaunchService.launchJob(req)
        assertEquals("Applying modules: zvi-label-detection to 1 assets", rsp.job.name)
        assertEquals(1, rsp.assetCount)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testLaunchReprocessAssetSearch_noAssetsFailure() {
        val req = ReprocessAssetSearchRequest(
            mapOf(),
            listOf("zmlp-labels")
        )
        jobLaunchService.launchJob(req)
    }

    @Test
    fun testLaunchJobWithGenerator() {
        val name = "test"
        val gen = ProcessorRef("zmlp_core.core.generators.GcsBucketGenerator", StandardContainers.CORE)
        val pipeline = pipelineResolverService.resolve()
        val job = jobLaunchService.launchJob(name, gen, pipeline)
        assertEquals("test", job.name)

        val tasks = jobService.getTasks(job.id)
        val script = jobService.getZpsScript(tasks.first().id)
        assertEquals("zmlp_core.core.generators.GcsBucketGenerator", script.generate!![0]!!.className)
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
}
