package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsType
import com.zorroa.archivist.domain.DataSourceDelete
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.domain.FileType

import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.emptyZpsScripts
import com.zorroa.zmlp.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataSourceServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var piplineModService: PipelineModService

    @Autowired
    lateinit var jobService: JobService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    val spec = DataSourceSpec(
        "dev-data",
        "gs://zorroa-dev-data",
        fileTypes = FileType.allTypes()
    )

    @Test
    fun testCreate() {
        val ds = dataSourceService.create(spec)
        assertEquals(spec.name, ds.name)
        assertEquals(spec.fileTypes, ds.fileTypes)
    }

    @Test
    fun testCreateWithModules() {
        piplineModService.updateStandardMods()

        val spec2 = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            fileTypes = FileType.allTypes(),
            modules = setOf("zvi-object-detection")
        )

        val ds = dataSourceService.create(spec2)
        assertEquals(spec.name, ds.name)
        assertEquals(spec.fileTypes, ds.fileTypes)
        assertTrue(ds.modules.isNotEmpty())
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(1) FROM x_module_datasource", Int::class.java))
    }

    @Test
    fun testCreateWithCredentials() {
        credentialsService.create(CredentialsSpec("test", CredentialsType.AWS, TEST_AWS_CREDS))
        val spec2 = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            fileTypes = FileType.allTypes(),
            credentials = setOf("test")
        )

        val ds = dataSourceService.create(spec2)
        val ds2 = dataSourceService.get(ds.id)
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(1) FROM x_credentials_datasource", Int::class.java))
        assertTrue(ds2.credentials.isNotEmpty())
        assertTrue(ds.credentials.isNotEmpty())
    }

    @Test
    fun testUpdate() {
        val ds = dataSourceService.create(spec)
        val update = DataSourceUpdate(
            "cats",
            "gs://foo/bar",
            listOf("images", "videos", "documents"),
            setOf(),
            setOf()
        )
        dataSourceService.update(ds.id, update)

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(update.name, ds2.name)
        assertEquals(FileType.fromArray(update.fileTypes), ds2.fileTypes)
        assertEquals(update.uri, ds2.uri)
    }

    @Test
    fun testUpdateWithModules() {
        piplineModService.updateStandardMods()
        val ds = dataSourceService.create(spec)

        val update = DataSourceUpdate(
            "cats",
            "gs://foo/bar",
            listOf("images", "videos", "documents"),
            setOf(),
            setOf("zvi-object-detection")
        )
        dataSourceService.update(ds.id, update)

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(update.name, ds2.name)
        assertEquals(FileType.fromArray(update.fileTypes), ds2.fileTypes)
        assertEquals(update.uri, ds2.uri)
        assertEquals(1, ds2.modules.size)
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(1) FROM x_module_datasource", Int::class.java))
    }

    @Test
    fun testUpdateWithCredentials() {
        val ds = dataSourceService.create(spec)
        val creds = credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )

        val update = DataSourceUpdate(
            "cats",
            "gs://foo/bar",
            listOf("images", "videos", "documents"),
            setOf(creds.name),
            setOf()
        )
        dataSourceService.update(ds.id, update)

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(update.name, ds2.name)
        assertEquals(FileType.fromArray(update.fileTypes), ds2.fileTypes)
        assertEquals(update.uri, ds2.uri)

        val credsIds = jdbc.queryForList(
            "SELECT pk_credentials FROM x_credentials_datasource WHERE pk_datasource=?",
            UUID::class.java, ds2.id
        )
        Json.prettyPrint(credsIds)
        assertTrue(creds.id in credsIds)
    }

    @Test
    fun testDelete() {
        val ds = dataSourceService.create(spec)
        dataSourceService.delete(ds.id, DataSourceDelete())
        entityManager.flush()
        val count = jdbc.queryForObject(
            "SELECT COUNT(1) FROM datasource WHERE pk_datasource=?", Int::class.java, ds.id
        )
        assertEquals(0, count)
    }

    @Test
    fun testDeleteWithJob() {
        val ds = dataSourceService.create(spec)
        val jspec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            dataSourceId = ds.id,
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job = jobService.create(jspec)
        dataSourceService.delete(ds.id, DataSourceDelete())
        entityManager.flush()
        val count = jdbc.queryForObject(
            "SELECT COUNT(1) FROM datasource WHERE pk_datasource=?", Int::class.java, ds.id
        )
        assertEquals(0, count)

        val dsJob = jobService.get(job.id)
        assertNull(dsJob.dataSourceId)
        assertEquals(JobState.Cancelled, dsJob.state)
    }

    @Test
    fun testGet() {
        val ds1 = dataSourceService.create(spec)
        val ds2 = dataSourceService.get(ds1.id)
        assertEquals(ds1, ds2)
    }
}
