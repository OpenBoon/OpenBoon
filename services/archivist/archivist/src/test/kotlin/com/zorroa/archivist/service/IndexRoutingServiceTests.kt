package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobState
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.client.RequestOptions
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexRoutingServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var indexDao: IndexDao

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @After
    fun reset() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun setupDefaultIndexRoute() {
        indexRoutingService.setupDefaultIndexRoute()
        assertEquals(1, jdbc.update("UPDATE index_route SET str_url='http://foo'"))
        indexRoutingService.setupDefaultIndexRoute()
        assertEquals(
            "http://localhost:9200", jdbc.queryForObject(
                "SELECT str_url FROM index_route", String::class.java
            )
        )
    }

    @Test
    fun syncAllIndexRoutes() {
        jdbc.update(
            "UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index='test123'"
        )

        indexRoutingService.syncAllIndexRoutes()

        val route = indexRouteDao.getProjectRoute()
        assertEquals(route.mappingMinorVer, 20001231)
        assertTrue(indexRoutingService.getOrgRestClient().indexExists())
    }

    /**
     * Test the case where an index is deleted and has to be
     * fully synced with the latest patches.
     */
    @Test
    fun testSyncDeletedIndex() {

        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )
        val newRoute = indexRoutingService.createIndexRoute(spec)
        val rest = indexRoutingService.getOrgRestClient()
        val reqDel = DeleteIndexRequest(newRoute.indexName)

        try {
            rest.client.indices().delete(reqDel, RequestOptions.DEFAULT)
            logger.info("${newRoute.indexUrl} Elastic DB Removed")
        } catch (e: Exception) {
            logger.warn("Failed to delete 'unittest' index, this is usually ok.")
        }

        // The ES index is deleted but our route still shows an updated index
        val route = indexRouteDao.get(newRoute.id)
        assertTrue(route.mappingMinorVer > 0)

        val lastMapping = indexRoutingService.syncIndexRouteVersion(route)
        assertEquals(route.mappingMinorVer, lastMapping!!.minorVersion)
        assertTrue(indexRoutingService.getOrgRestClient().indexExists())
    }

    @Test
    fun syncIndexRouteVersion() {
        jdbc.update(
            "UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index='test123'"
        )

        var route = indexRouteDao.getProjectRoute()
        indexRoutingService.syncIndexRouteVersion(route)

        route = indexRouteDao.getProjectRoute()
        assertEquals(route.mappingMinorVer, 20001231)

        assertTrue(indexRoutingService.getOrgRestClient().indexExists())
    }

    @Test
    fun syncIndexRouteVersionShardsAndReplicas() {
        val index = "test123"
        jdbc.update(
            "UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index=?, int_shards=1, int_replicas=0", index
        )

        val rest = indexRoutingService.getOrgRestClient()
        try {
            val reqDel = DeleteIndexRequest(index)
            rest.client.indices().delete(reqDel, RequestOptions.DEFAULT)
        } catch (e: Exception) {
            // ignore
        }

        var route = indexRouteDao.getProjectRoute()
        indexRoutingService.syncIndexRouteVersion(route)

        // Validate the index has our settings.
        val getIndex = GetIndexRequest().indices("test123")
        val rsp = rest.client.indices().get(getIndex, RequestOptions.DEFAULT)
        assertEquals(1, rsp.settings["test123"].get("index.number_of_shards").toInt())
        assertEquals(0, rsp.settings["test123"].get("index.number_of_replicas").toInt())
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
    fun getOrgRestClientReIndex() {

        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        val request = MockHttpServletRequest()
        request.addHeader("X-Zorroa-Index-Route", route.id)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        indexRoutingService.getOrgRestClient()
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun getOrgRestClientReIndexMissing() {

        val request = MockHttpServletRequest()
        request.addHeader("X-Zorroa-Index-Route", UUID.randomUUID().toString())
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        indexRoutingService.getOrgRestClient()
    }

    @Test
    fun getClusterRestClient() {
        var route = indexRouteDao.getProjectRoute()
        val client = indexRoutingService.getClusterRestClient(route)
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
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

        indexDao.index(listOf(doc), false)
        var passed = false
        try {
            indexDao.get(doc.id)
        } catch (e: EmptyResultDataAccessException) {
            passed = true
        }
        assertTrue(passed)
        assertEquals(0, indexDao.getAll(Pager.first()).size())
        indexRoutingService.refreshAll()
        Thread.sleep(250)
        assertEquals(1, indexDao.getAll(Pager.first()).size())
    }

    @Test
    fun testLaunchReindexJob() {
        var job = indexRoutingService.launchReindexJob()
        var jobCount = jobService.getAll(JobFilter(names = listOf(job.name))).size()
        assertEquals(1, jobCount)

        job = indexRoutingService.launchReindexJob()
        jobCount = jobService.getAll(JobFilter(names = listOf(job.name))).size()
        assertEquals(2, jobCount)

        jobCount = jobService.getAll(
            JobFilter(
                states = listOf(JobState.Active),
                names = listOf(job.name)
            )
        ).size()
        assertEquals(1, jobCount)
    }

    @Test
    fun testGetIndexMappingVersions() {
        val mappings = indexRoutingService.getIndexMappingVersions()
        assertTrue(mappings.isNotEmpty())
    }

    @Test
    fun testCreate() {

        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        assertEquals(spec.clusterUrl, route.clusterUrl)
        assertEquals(spec.indexName, route.indexName)
        assertEquals(spec.mapping, route.mapping)
        assertEquals(spec.mappingMajorVer, route.mappingMajorVer)
    }

    @Test
    fun testCloseAndDelete() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "v13_test",
            "strict",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        indexRoutingService.closeAndDeleteIndex(route)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateMissingMapping() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "kirk",
            33
        )

        indexRoutingService.createIndexRoute(spec)
    }

    @Test
    fun testIsReindexRoute() {
        assertFalse(indexRoutingService.isReIndexRoute())

        val request = MockHttpServletRequest()
        request.addHeader("X-Zorroa-Index-Route", UUID.randomUUID().toString())
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        assertTrue(indexRoutingService.isReIndexRoute())
    }

    @Test
    fun testOpenIndex() {
        var route = indexRouteDao.getProjectRoute()
        assertTrue(indexRoutingService.closeIndex(route))
        assertTrue(indexRoutingService.openIndex(route))

        val state = indexRoutingService.getEsIndexState(route)
        assertEquals("open", state["status"])
    }

    @Test
    fun testCloseIndex() {
        var route = indexRouteDao.getProjectRoute()
        assertTrue(indexRoutingService.closeIndex(route))
        assertFalse(indexRoutingService.closeIndex(route))

        val state = indexRoutingService.getEsIndexState(route)
        assertEquals("close", state["status"])
    }

    @Test
    fun testGetEsIndexState() {
        val route = indexRouteDao.getProjectRoute()
        val result = indexRoutingService.getEsIndexState(route)
        assertEquals("yellow", result["health"])
        assertEquals("open", result["status"])
        assertEquals("5", result["pri"])
        assertEquals("2", result["rep"])
    }

    @Test
    fun testDeleteIndex() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "v13_test",
            "strict",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        indexRoutingService.closeIndex(route)
        assertTrue(indexRoutingService.deleteIndex(route, force = true))
    }
}
