package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexCluster
import org.elasticsearch.ElasticsearchStatusException
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ClusterBackupServiceTests : AbstractTest() {

    lateinit var cluster: IndexCluster

    val policyId = "test-policy-id"
    val schedule = "0 30 2 * * ?"
    val indices = listOf("*")
    val maxRetentionDays = 30L
    val minimumSnapshotCount = 10
    val maximumSnapshotCount = 50

    @Before
    fun createClusterRepository() {
        cluster = indexClusterService.createDefaultCluster()
        clusterBackupService.createClusterRepository(cluster)
    }

    @Test
    fun createSnapshot() {
        var snapshots = clusterBackupService.getSnapshots(cluster)
        clusterBackupService.createClusterSnapshot(cluster, "test")

        var snapshotTest = clusterBackupService
            .getSnapshots(cluster)
            .snapshots.find { it.snapshotId().name == "test" }

        assertEquals("test", snapshotTest?.snapshotId()?.name)

        clusterBackupService.deleteSnapshot(cluster, "test")
    }

    @Test
    fun deleteSnapshot() {

        var snapshots = clusterBackupService.getSnapshots(cluster)

        assertEquals(0, snapshots.snapshots.size)

        clusterBackupService.createClusterSnapshot(cluster, "test")

        assertEquals(1, clusterBackupService.getSnapshots(cluster).snapshots.size)

        clusterBackupService.deleteSnapshot(cluster, "test")

        assertEquals(0, clusterBackupService.getSnapshots(cluster).snapshots.size)
    }

    @Test
    fun createSnapshotPolicy() {

        clusterBackupService.createClusterSnapshotPolicy(
            cluster,
            policyId,
            schedule,
            indices,
            maxRetentionDays,
            minimumSnapshotCount,
            maximumSnapshotCount
        )

        val clusterPolicy = clusterBackupService.getClusterSnapshotPolicy(cluster, policyId)

        assertEquals(policyId, clusterPolicy.policies[policyId]?.name)
        assertEquals(schedule, clusterPolicy.policies[policyId]?.policy?.schedule)
        assertEquals(maxRetentionDays, clusterPolicy.policies[policyId]?.policy?.retentionPolicy?.expireAfter?.days)
        assertEquals(
            minimumSnapshotCount,
            clusterPolicy.policies[policyId]?.policy?.retentionPolicy?.minimumSnapshotCount
        )
        assertEquals(
            maximumSnapshotCount,
            clusterPolicy.policies[policyId]?.policy?.retentionPolicy?.maximumSnapshotCount
        )
        assertEquals(
            indices[0],
            (clusterPolicy.policies[policyId]?.policy?.config?.get("indices") as ArrayList<String>)?.get(0)
        )
    }

    @Test(expected = ElasticsearchStatusException::class)
    fun deleteSnapshotPolicy() {
        clusterBackupService.createClusterSnapshotPolicy(
            cluster,
            policyId,
            schedule,
            indices,
            maxRetentionDays,
            minimumSnapshotCount,
            maximumSnapshotCount
        )

        var clusterPolicy = clusterBackupService.getClusterSnapshotPolicy(cluster, policyId)

        assertEquals(1, clusterPolicy.policies.size)

        clusterBackupService.deleteClusterSnapshotPolicy(cluster, policyId)
        clusterBackupService.getClusterSnapshotPolicy(cluster, policyId)
    }
}
