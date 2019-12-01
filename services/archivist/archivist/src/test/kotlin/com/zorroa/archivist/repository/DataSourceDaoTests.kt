package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceFilter
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.service.DataSourceService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataSourceDaoTests : AbstractTest() {

    @Autowired
    lateinit var dataSourceJdbcDao: DataSourceJdbcDao

    @Autowired
    lateinit var dataSourceDao: DataSourceDao

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Test
    fun testSetAndGetCredentials() {
        val id = UUID.randomUUID()
        val spec = DataSource(
            id,
            getProjectId(),
            "test",
            "gs://foo-bar",
            null,
            null,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            "admin",
            "admin"
        )

        val ds = dataSourceDao.saveAndFlush(spec)
        val blob = "ABC123"
        val salt = "ababababababab"
        assertTrue(dataSourceJdbcDao.updateCredentials(ds.id, blob, salt))

        val creds = dataSourceJdbcDao.getCredentials(ds.id)
        assertEquals(blob, creds.blob)
        assertEquals(salt, creds.salt)

    }

    @Test
    fun testFindOne() {
        val spec = DataSourceSpec("test", "gs://foo/bar")
        dataSourceService.create(spec)

        val filter = DataSourceFilter(names=listOf("test"))
        val ds = dataSourceJdbcDao.findOne(filter)
        assertEquals("test", ds.name)
    }

    @Test
    fun testFind() {
        val spec = DataSourceSpec("test", "gs://foo/bar")
        dataSourceService.create(spec)

        val filter = DataSourceFilter(names=listOf("test"))
        val all = dataSourceJdbcDao.find(filter)
        assertEquals(1, all.size())

        val none = dataSourceJdbcDao.find(DataSourceFilter(names=listOf("dog")))
        assertEquals(0, none.size())
    }
}