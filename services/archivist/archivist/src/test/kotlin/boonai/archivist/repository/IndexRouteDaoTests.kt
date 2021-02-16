package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexRouteState
import boonai.archivist.security.getProjectId
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexRouteDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Value("\${archivist.es.url}")
    lateinit var esUrl: String

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    fun getTestSpec(): IndexRouteSpec {
        return IndexRouteSpec(
            "testing123", 1,
            clusterId = indexClusterService.getNextAutoPoolCluster().id
        )
    }

    @Test
    fun testCreate() {
        val spec = getTestSpec()
        val route = indexRouteDao.create(spec)
        assertEquals(spec.majorVer, route.majorVer)
        assertEquals(0, route.minorVer)
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
        assertEquals(esUrl, route.clusterUrl)
        assertEquals("english_strict", route.mapping)
        assertEquals(0, route.replicas)
        assertEquals(1, route.shards)
    }

    @Test
    fun testGetProjectRouteById() {
        val spec = getTestSpec()
        indexRouteDao.create(spec)
        val route = indexRouteDao.getProjectRoute(getProjectId())
        assertEquals(esUrl, route.clusterUrl)
        assertEquals("english_strict", route.mapping)
        assertEquals(0, route.replicas)
        assertEquals(1, route.shards)
    }

    @Test
    fun testGetAll() {
        val routes = indexRouteDao.getAll()
        assertEquals(1, routes.size)
    }

    @Test
    fun testGetAllByCluster() {
        val routes = indexRouteDao.getAll(indexClusterService.getNextAutoPoolCluster())
        assertEquals(1, routes.size)
    }

    @Test
    fun testGetOpen() {
        val routes = indexRouteDao.getOpen()
        assertEquals(1, routes.size)
    }

    @Test
    fun testGetOpenByCluster() {
        val cluster = indexClusterService.getNextAutoPoolCluster()
        val routes = indexRouteDao.getOpen(cluster)
        assertEquals(1, routes.size)

        indexRouteDao.setState(routes[0], IndexRouteState.CLOSED)
        assertTrue(indexRouteDao.getOpen(cluster).isEmpty())
    }

    @Test
    fun testGetAllByFilter() {
        val route = indexRouteDao.getProjectRoute()

        val filter = IndexRouteFilter(
            ids = listOf(route.id),
            mappings = listOf(route.mapping),
            clusterIds = listOf(route.clusterId),
            projectIds = listOf(getProjectId())
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
            ver,
            jdbc.queryForObject(
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
            ver,
            jdbc.queryForObject(
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

    @Test
    fun testSetState() {
        val spec = getTestSpec()
        val route = indexRouteDao.create(spec)
        indexRouteDao.setState(route, IndexRouteState.CLOSED)
        assertEquals(
            IndexRouteState.CLOSED.ordinal,
            jdbc.queryForObject("SELECT int_state FROM index_route WHERE pk_index_route=?", Int::class.java, route.id)
        )
    }

    @Test
    fun testSetStateById() {
        val spec = getTestSpec()
        val route = indexRouteDao.create(spec)
        indexRouteDao.setState(route.id, IndexRouteState.CLOSED)
        assertEquals(
            IndexRouteState.CLOSED.ordinal,
            jdbc.queryForObject("SELECT int_state FROM index_route WHERE pk_index_route=?", Int::class.java, route.id)
        )
    }
}
