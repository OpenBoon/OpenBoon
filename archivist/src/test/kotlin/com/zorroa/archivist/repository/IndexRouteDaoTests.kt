package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexRouteDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    override fun requiresElasticSearch() : Boolean {
        return true
    }

    @Test
    fun testUpdateDefaultIndexRoutes() {
        val url = "http://dog:1234"
        indexRouteDao.updateDefaultIndexRoutes("http://dog:1234", false)
        assertEquals(url, jdbc.queryForObject("SELECT str_url FROM index_route",
                String::class.java))
        assertFalse(jdbc.queryForObject("SELECT bool_use_rkey FROM index_route",
                Boolean::class.java))

    }

    @Test
    fun testGetOrgRoute() {
        val route = indexRouteDao.getOrgRoute()
        assertEquals("http://localhost:9200", route.clusterUrl)
        assertEquals("http://localhost:9200/unittest", route.indexUrl)
        assertEquals("unittest", route.indexName)
        assertEquals("asset", route.mappingType)
        assertEquals(false, route.closed)
        assertEquals(true, route.defaultPool)
        assertEquals(2, route.replicas)
        assertEquals(5, route.shards)
    }

    @Test
    fun testGetRadomDefaultRoute() {
        val route = indexRouteDao.getRandomDefaultRoute()
        assertEquals("http://localhost:9200", route.clusterUrl)
        assertEquals("http://localhost:9200/unittest", route.indexUrl)
        assertEquals("unittest", route.indexName)
        assertEquals("asset", route.mappingType)
        assertEquals(false, route.closed)
        assertEquals(true, route.defaultPool)
        assertEquals(2, route.replicas)
        assertEquals(5, route.shards)
    }

    @Test
    fun testGetAll() {
        val routes = indexRouteDao.getAll()
        assertEquals(1, routes.size)
    }

    @Test
    fun testSetMinorVersion() {
        val ver = 131337
        val route = indexRouteDao.getOrgRoute()
        assertTrue(indexRouteDao.setMinorVersion(route, ver))
        assertEquals(ver, jdbc.queryForObject(
                "SELECT int_mapping_minor_ver FROM index_route", Int::class.java))
    }

    @Test
    fun testErrorVersion() {
        val ver = 666
        val route = indexRouteDao.getOrgRoute()
        assertTrue(indexRouteDao.setErrorVersion(route, ver))
        assertEquals(ver, jdbc.queryForObject(
                "SELECT int_mapping_error_ver FROM index_route", Int::class.java))
    }
}