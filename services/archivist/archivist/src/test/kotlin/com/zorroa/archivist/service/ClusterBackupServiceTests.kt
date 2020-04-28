package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexCluster
import org.junit.Before
import org.junit.Test

class ClusterBackupServiceTests : AbstractTest() {

    lateinit var cluster: IndexCluster

    @Before
    fun createClusterRepository() {
        cluster = indexClusterService.createDefaultCluster()
        clusterBackupService.createClusterRepository(cluster)
    }

    @Test
    fun createSnapshot() {

        clusterBackupService.createClusterSnapshot(cluster, "test")
        var snapshots = clusterBackupService.getSnapshots(cluster)
        clusterBackupService.deleteSnapshot(cluster, "test")
        snapshots = clusterBackupService.getSnapshots(cluster)
    }
}