package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.domain.PipelineSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataSourceServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    val spec = DataSourceSpec(
        "dev-data",
        "gs://zorroa-dev-data",
        credentials = "SECRET BLOB",
        fileTypes = listOf("jpg")
    )

    @Test
    fun testCreate() {
        val ds = dataSourceService.create(spec)
        assertEquals(spec.name, ds.name)
        assertEquals(spec.fileTypes, ds.fileTypes)
        val settings = projectService.getSettings()
        assertEquals(settings.defaultPipelineId, ds.pipelineId)
    }

    @Test
    fun testCreateWithPipeline() {
        val pipe = pipelineService.create(PipelineSpec("foo"))

        val spec = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            pipeline = "foo",
            credentials = "SECRET BLOB",
            fileTypes = listOf("jpg")
        )

        val ds = dataSourceService.create(spec)
        assertEquals(spec.name, ds.name)
        assertEquals(spec.fileTypes, ds.fileTypes)
        assertEquals(pipe.id, ds.pipelineId)
    }

    @Test
    fun testUpdate() {
        val pipe = pipelineService.create(PipelineSpec("foo"))

        val ds = dataSourceService.create(spec)
        val update = DataSourceUpdate(
            "cats",
            "gs://foo/bar",
            listOf("jpg"),
            pipe.id
        )
        dataSourceService.update(ds.id, update)

        val ds2 = dataSourceService.get(ds.id)
        assertEquals(update.name, ds2.name)
        assertEquals(update.fileTypes, ds2.fileTypes)
        assertEquals(update.uri, ds2.uri)
        assertEquals(update.pipelineId, ds2.pipelineId)
    }

    @Test
    fun testDelete() {
        val ds = dataSourceService.create(spec)
        dataSourceService.delete(ds.id)
        entityManager.flush()
        val count = jdbc.queryForObject(
            "SELECT COUNT(1) FROM datasource WHERE pk_datasource=?", Int::class.java, ds.id)
        assertEquals(0, count)
    }

    @Test
    fun testGet() {
        val ds1 = dataSourceService.create(spec)
        val ds2 = dataSourceService.get(ds1.id)
        assertEquals(ds1, ds2)
    }

    @Test
    fun testGetCredentials() {
        val ds1 = dataSourceService.create(spec)
        val creds = dataSourceService.getCredentials(ds1.id)
        assertEquals(spec.credentials, creds.blob)
        assertNull(creds.salt)
    }

    @Test
    fun testUpdateCredentials() {
        val ds1 = dataSourceService.create(spec)
        dataSourceService.updateCredentials(ds1.id, "BINGO")
        val creds = dataSourceService.getCredentials(ds1.id)
        assertEquals("BINGO", creds.blob)
    }

    @Test
    fun createAnalysisJob() {
        val ds = dataSourceService.create(spec)
        val job = dataSourceService.createAnalysisJob(ds)
        assertEquals(ds.id, job.dataSourceId)
    }
}