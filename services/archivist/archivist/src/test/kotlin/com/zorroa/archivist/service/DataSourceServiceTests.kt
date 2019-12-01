package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.DataSourceSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataSourceServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSourceService: DataSourceService

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