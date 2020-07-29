package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsType
import com.zorroa.archivist.domain.DataSourceFilter
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.service.CredentialsService
import com.zorroa.archivist.service.DataSourceService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class DataSourceDaoTests : AbstractTest() {

    @Autowired
    lateinit var dataSourceJdbcDao: DataSourceJdbcDao

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Test
    fun testSetCredentials() {
        val creds = credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )
        val ds = dataSourceService.create(DataSourceSpec("test", "gs://foo/bar"))
        dataSourceJdbcDao.setCredentials(ds.id, listOf(creds))

        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT COUNT(1) FROM x_credentials_datasource WHERE pk_datasource=?",
                Int::class.java, ds.id
            )
        )
    }

    @Test
    fun testFindOne() {
        val spec = DataSourceSpec("test", "gs://foo/bar")
        dataSourceService.create(spec)

        val filter = DataSourceFilter(names = listOf("test"))
        val ds = dataSourceJdbcDao.findOne(filter)
        assertEquals("test", ds.name)
    }

    @Test
    fun testFind() {
        val spec = DataSourceSpec("test", "gs://foo/bar")
        dataSourceService.create(spec)

        val filter = DataSourceFilter(names = listOf("test"))
        val all = dataSourceJdbcDao.find(filter)
        assertEquals(1, all.size())

        val none = dataSourceJdbcDao.find(DataSourceFilter(names = listOf("dog")))
        assertEquals(0, none.size())
    }

    @Test
    fun testCreatedOrderDesc() {
        val spec1 = DataSourceSpec("test1", "gs://foo/bar")
        Thread.sleep(10)
        val spec2 = DataSourceSpec("test2", "gs://foo/bar")
        Thread.sleep(10)
        val spec3 = DataSourceSpec("test3", "gs://foo/bar")
        dataSourceService.create(spec1)
        dataSourceService.create(spec2)
        dataSourceService.create(spec3)

        val filter = DataSourceFilter(names = listOf("test1", "test2", "test3"))
        val all = dataSourceJdbcDao.find(filter)
        assertEquals(3, all.size())
        assertEquals(spec3.name, all[0].name)
        assertEquals(spec2.name, all[1].name)
        assertEquals(spec1.name, all[2].name)
    }
}
