package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexClusterState
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexClusterServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean = true

    @Test
    fun testGetCluster() {
        val defaultCluster = indexClusterService.createDefaultCluster()
        val cluster = indexClusterService.getIndexCluster(defaultCluster.id)
        assertEquals(defaultCluster.id, cluster.id)
    }

    // TODO: more tests
}

class IndexClusterMonitorTests() : AbstractTest() {

    override fun requiresElasticSearch(): Boolean = true

    @Autowired
    lateinit var indexClusterMonitor: IndexClusterMonitor

    @Test
    fun pingAllClusters() {
        indexClusterMonitor.pingAllClusters()
    }

    @Test
    fun pingCluster() {
        val cluster = indexClusterService.getNextAutoPoolCluster()
        assertTrue(indexClusterMonitor.pingCluster(cluster))
        val cluster2 = indexClusterService.getNextAutoPoolCluster()
        assertEquals(IndexClusterState.UP, cluster2.state)
    }

    @Test
    fun getNextAutoPool() {
        val cluster = indexClusterService.getNextAutoPoolCluster()
        assertTrue(indexClusterMonitor.pingCluster(cluster))
        val cluster2 = indexClusterService.getNextAutoPoolCluster()
        assertEquals(IndexClusterState.UP, cluster2.state)
    }
}
