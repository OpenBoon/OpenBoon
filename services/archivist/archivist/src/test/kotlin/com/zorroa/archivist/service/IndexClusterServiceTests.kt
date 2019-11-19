package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexClusterState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexClusterServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean = true

    @Test
    fun pingAllClusters() {
        indexClusterService.pingAllClusters()
    }

    @Test
    fun pingCluster() {
        val cluster = indexClusterService.getNextAutoPoolCluster()
        assertTrue(indexClusterService.pingCluster(cluster))
        val cluster2 = indexClusterService.getNextAutoPoolCluster()
        assertEquals(IndexClusterState.UP, cluster2.state)
    }

    @Test
    fun getNextAutoPool() {
        val cluster = indexClusterService.getNextAutoPoolCluster()
        assertTrue(indexClusterService.pingCluster(cluster))
        val cluster2 = indexClusterService.getNextAutoPoolCluster()
        assertEquals(IndexClusterState.UP, cluster2.state)
    }
}