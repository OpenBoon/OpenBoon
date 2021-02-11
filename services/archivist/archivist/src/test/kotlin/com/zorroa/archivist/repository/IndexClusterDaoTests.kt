package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexClusterSpec
import com.zorroa.archivist.domain.IndexClusterState
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexClusterDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexClusterDao: IndexClusterDao

    override fun requiresElasticSearch(): Boolean = true

    val testSpec = IndexClusterSpec(
        "http://foo", false
    )

    @Test
    fun testCreate() {
        val cluster = indexClusterDao.create(testSpec)
        assertEquals(testSpec.url, cluster.url)
        assertEquals(testSpec.autoPool, cluster.autoPool)
        assertEquals(IndexClusterState.PENDING, cluster.state)
    }

    @Test
    fun testGetByUUID() {
        val cluster1 = indexClusterDao.create(testSpec)
        val cluster2 = indexClusterDao.get(cluster1.id)
        assertEquals(cluster1, cluster2)
    }

    @Test
    fun testGetByUrl() {
        val cluster1 = indexClusterDao.create(testSpec)
        val cluster2 = indexClusterDao.get(cluster1.url)
        assertEquals(cluster1, cluster2)
    }

    @Test
    fun testGetAll() {
        val cluster1 = indexClusterDao.create(testSpec)
        assertTrue(cluster1 in indexClusterDao.getAll())
    }

    @Test
    fun testExists() {
        val cluster1 = indexClusterDao.create(testSpec)
        assertTrue(indexClusterDao.exists(cluster1.url))
        assertFalse(indexClusterDao.exists("https://foobar"))
    }

    @Test
    fun getNextAutoPoolCluster() {
        val spec = IndexClusterSpec(
            "http://foo", true
        )
        val cluster1 = indexClusterDao.create(spec)
        // Force the cluster to ready
        jdbc.update("UPDATE index_cluster SET int_state=0")
        jdbc.update("UPDATE index_cluster SET int_state=1 WHERE pk_index_cluster=?", cluster1.id)
        // Our new cluster should be returned since it has less indexes.
        val cluster2 = indexClusterDao.getNextAutoPoolCluster()
        assertEquals(cluster1, cluster2)
    }

    @Test
    fun testUpdateStatus() {
        val spec = IndexClusterSpec(
            "http://foo", true
        )
        val cluster1 = indexClusterDao.create(spec)
        assertTrue(indexClusterDao.updateState(cluster1, IndexClusterState.DOWN))
        assertFalse(indexClusterDao.updateState(cluster1, IndexClusterState.DOWN))
        assertTrue(indexClusterDao.updateState(cluster1, IndexClusterState.UP))
    }

    @Test
    fun testUpdateAttrs() {
        val cluster = indexClusterDao.getNextAutoPoolCluster()
        indexClusterDao.updateAttrs(cluster, "{\"foo\": \"bar\" }")
        val cluster2 = indexClusterDao.get(cluster.id)
        assertEquals(cluster2.attrs["foo"], "bar")
    }
}
