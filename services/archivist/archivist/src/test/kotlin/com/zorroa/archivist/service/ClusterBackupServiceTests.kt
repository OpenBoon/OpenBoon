package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.IndexCluster
import com.zorroa.archivist.domain.IndexClusterSpec
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.client.RequestOptions
import org.junit.Before
import org.junit.Ignore
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

    @Test
    @Ignore
    //docker run -d --name elasticsearch-test  -p 9201:9200 -p 9301:9300 -e "discovery.type=single-node" -e "MINIO_URL={yourlocalip}:9000" -e "network.host=0.0.0.0" zmlp/elasticsearch:latest
    fun restoreBackupIntoNewCluster() {
        val newCluster =
            indexClusterService.createIndexCluster(IndexClusterSpec("http://localhost:9201", false))

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/cat-movie.m4v"))
        )

        // Add a label
        val assetId = assetService.batchCreate(batchCreate).created[0]
        var asset = assetService.getAsset(assetId)

        //Create a snapshot on Old Repository
        val snapshotName = "test-snapshot"
        clusterBackupService.createClusterSnapshot(cluster, snapshotName)

        //Restore snapshot on new Repository
        clusterBackupService.restoreSnapshot(newCluster, snapshotName, cluster.id)
        Thread.sleep(1000)

        val rest = indexRoutingService.getProjectRestClient()

        // Get asset from new Cluster
        val assetOnNewCluster =
            indexClusterService.getRestHighLevelClient(newCluster)
                .get(GetRequest(rest.route.indexName).id(assetId), RequestOptions.DEFAULT)
        // Get asset from old Cluster
        val assetOnOldCluster =
            indexClusterService.getRestHighLevelClient(cluster)
                .get(GetRequest(rest.route.indexName).id(assetId), RequestOptions.DEFAULT)

        //Evaluate
        assertEquals(assetOnOldCluster.id, assetOnNewCluster.id)
        assertEquals(assetOnOldCluster, assetOnNewCluster)

    }
}
