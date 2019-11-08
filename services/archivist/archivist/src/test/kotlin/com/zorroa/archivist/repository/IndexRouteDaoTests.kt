package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexRouteFilter
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

    fun getTestSpec(): IndexRouteSpec {
        return IndexRouteSpec(
            "testing123", 1, clusterId = indexClusterService.getNextAutoPoolCluster().id
        )
    }
    
    @Test
    fun testCreate() {
        val spec = getTestSpec()
        val route = indexRouteDao.create(spec)
        assertEquals(spec.mappingMajorVer, route.mappingMajorVer)
        assertEquals(0, route.mappingMinorVer)
        assertEquals(spec.shards, route.shards)
        assertEquals(spec.replicas, route.replicas)
    }

    @Test
    fun testGetById() {
        val spec = getTestSpec()
        val route1 = indexRouteDao.create(spec)
        val route2 = indexRouteDao.get(route1.id)
        assertEquals(route1.id, route2.id)
    }

    @Test
    fun testGetByUrl() {
        val spec = getTestSpec()
        val route1 = indexRouteDao.create(spec)
        val route2 = indexRouteDao.get(route1.id)
        assertEquals(route1.id, route2.id)
    }

    @Test
    fun testGetProjectRoute() {
        val spec = getTestSpec()
        indexRouteDao.create(spec)
        val route = indexRouteDao.getProjectRoute()
        assertEquals("http://localhost:9200", route.clusterUrl)
        assertEquals("asset", route.mapping)
        assertEquals(0, route.replicas)
        assertEquals(2, route.shards)
    }

    @Test
    fun testGetAll() {
        val routes = indexRouteDao.getAll()
        assertEquals(1, routes.size)
    }

    @Test
    fun testGetAllByFilter() {
        val route = indexRouteDao.getProjectRoute()

        val filter = IndexRouteFilter(
            ids = listOf(route.id),
            mappings = listOf(route.mapping),
            clusterIds = listOf(route.clusterId)
        )

        assertEquals(1, indexRouteDao.getAll(filter).size())
    }

    @Test
    fun testGetAllByFilterSorted() {
        val filter = IndexRouteFilter()
        filter.sort = listOf("id:a", "clusterUrl:a", "mapping:a", "timeCreated:a")
        assertEquals(1, indexRouteDao.getAll(filter).size())
    }

    @Test
    fun testSetMinorVersion() {
        val ver = 131337
        val route = indexRouteDao.getProjectRoute()
        assertTrue(indexRouteDao.setMinorVersion(route, ver))
        assertEquals(
            ver, jdbc.queryForObject(
                "SELECT int_mapping_minor_ver FROM index_route", Int::class.java
            )
        )
    }

    @Test
    fun testErrorVersion() {
        val ver = 666
        val route = indexRouteDao.getProjectRoute()
        assertTrue(indexRouteDao.setErrorVersion(route, ver))
        assertEquals(
            ver, jdbc.queryForObject(
                "SELECT int_mapping_error_ver FROM index_route", Int::class.java
            )
        )
    }

    @Test
    fun testDelete() {
        val spec = getTestSpec()
        val route = indexRouteDao.create(spec)
        assertTrue(indexRouteDao.delete(route))
        assertFalse(indexRouteDao.delete(route))
    }
}
