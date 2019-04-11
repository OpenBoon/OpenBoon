package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.security.getOrgId
<<<<<<< HEAD
import com.zorroa.common.clients.ElasticMapping
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobState
=======
import com.zorroa.common.util.Json
>>>>>>> development
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingServiceTests : AbstractTest() {

    @Autowired
<<<<<<< HEAD
    lateinit var jobService : JobService

    @Autowired
    lateinit var elasticSearchConfiguration : ElasticSearchConfiguration
=======
    lateinit var indexRouteDao: IndexRouteDao
>>>>>>> development

    @Autowired
    lateinit var indexDao: IndexDao

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun setupDefaultIndexRoute() {
        indexRoutingService.setupDefaultIndexRoute()
        assertEquals(1, jdbc.update("UPDATE index_route SET str_url='http://foo'"))
        indexRoutingService.setupDefaultIndexRoute()
        assertEquals("http://localhost:9200", jdbc.queryForObject(
                "SELECT str_url FROM index_route", String::class.java))
    }

    @Test
    fun syncAllIndexRoutes() {
        jdbc.update("UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index='test123'")

        indexRoutingService.syncAllIndexRoutes()

        val route = indexRouteDao.getRandomDefaultRoute()
        assertEquals(route.mappingMinorVer, 20001231)

        assertTrue(indexRoutingService.getOrgRestClient().indexExists())
    }

    @Test
    fun syncIndexRouteVersion() {
        jdbc.update("UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index='test123'")

        var route = indexRouteDao.getRandomDefaultRoute()
        indexRoutingService.syncIndexRouteVersion(route)

        route = indexRouteDao.getRandomDefaultRoute()
        assertEquals(route.mappingMinorVer, 20001231)

        assertTrue(indexRoutingService.getOrgRestClient().indexExists())
    }

    @Test
    fun getMajorVersionMappingFile() {
        val mappingFile = indexRoutingService.getMajorVersionMappingFile("test", 1)
        assertEquals(1, mappingFile.majorVersion)
        assertEquals(0, mappingFile.minorVersion)
        assertEquals("test", mappingFile.name)
    }

    @Test
    fun getMinorVersionMappingFiles() {
        val files = indexRoutingService.getMinorVersionMappingFiles("test", 1)
        assertEquals(2, files.size)
        assertEquals(19991231, files[0].minorVersion)
        assertEquals(20001231, files[1].minorVersion)
    }

    @Test
    fun getOrgRestClient() {
        val client = indexRoutingService.getOrgRestClient()
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
        assertEquals(getOrgId().toString(), client.route.routingKey)
    }

    @Test
    fun getClusterRestClient() {
        var route = indexRouteDao.getRandomDefaultRoute()
        val client = indexRoutingService.getClusterRestClient(route)
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
        assertEquals(null, client.route.routingKey)
    }

    @Test
    fun performHealthCheck() {
        val good = indexRoutingService.performHealthCheck()
        assertEquals("UP", good.status.code)

        jdbc.update("UPDATE index_route SET str_url='http://foo'")

        val bad = indexRoutingService.performHealthCheck()
        assertEquals("DOWN", bad.status.code)
    }

    @Test
    fun refreshAll() {
        /**
         * Create a document with refresh=false, then try to search for it.
         * It should not be found. Refresh the index and the doc should appear.
         */
        val doc = Document()
        doc.setAttr("source.path", "/cat/dog.jpg")
        indexDao.index(doc, false)
        assertEquals(0, indexDao.getAll(Pager.first()).size())
        indexRoutingService.refreshAll()
        Thread.sleep(250)
        assertEquals(1, indexDao.getAll(Pager.first()).size())

    }
<<<<<<< HEAD

    @Test
    fun testLaunchReindexJob() {
        var job = indexRoutingService.launchReindexJob()
        var jobCount = jobService.getAll(JobFilter(names=listOf(job.name))).size()
        assertEquals(1, jobCount)

        job = indexRoutingService.launchReindexJob()
        jobCount = jobService.getAll(JobFilter(names=listOf(job.name))).size()
        assertEquals(2, jobCount)

        jobCount = jobService.getAll(JobFilter(
                states=listOf(JobState.Active),
                names=listOf(job.name))).size()
        assertEquals(1, jobCount)
    }
}
=======
}

>>>>>>> development
