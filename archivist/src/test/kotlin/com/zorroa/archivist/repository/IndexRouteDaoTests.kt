package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexRouteSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexRouteDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    override fun requiresElasticSearch(): Boolean {
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
    fun testCreate() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "on_prem",
            1,
            false)

        val route = indexRouteDao.create(spec)
        assertEquals(spec.clusterUrl, route.clusterUrl)
        assertEquals(spec.indexName, route.indexName)
        assertEquals(spec.defaultPool, route.defaultPool)
        assertEquals(spec.mappingMajorVer, route.mappingMajorVer)
        assertEquals(0, route.mappingMinorVer)
        assertEquals(spec.shards, route.shards)
        assertEquals(spec.replicas, route.replicas)
    }

    @Test
    fun testGetById() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "on_prem",
            1,
            false)

        val route1 = indexRouteDao.create(spec)
        val route2 = indexRouteDao.get(route1.id)
        assertEquals(route1.id, route2.id)
    }

    @Test
    fun testGetByUrl() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "on_prem",
            1,
            false)

        val route1 = indexRouteDao.create(spec)
        val route2 = indexRouteDao.get(route1.id)
        assertEquals(route1.id, route2.id)
    }
    @Test
    fun testGetOrgRoute() {
        val route = indexRouteDao.getOrgRoute()
        assertEquals("http://localhost:9200", route.clusterUrl)
        assertEquals("http://localhost:9200/unittest", route.indexUrl)
        assertEquals("unittest", route.indexName)
        assertEquals("asset", route.mapping)
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
        assertEquals("asset", route.mapping)
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