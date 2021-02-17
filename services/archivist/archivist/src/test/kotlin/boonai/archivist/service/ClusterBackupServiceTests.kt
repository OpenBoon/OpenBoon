package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexCluster
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClusterBackupServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean = true

    lateinit var cluster: IndexCluster

    @Autowired
    internal lateinit var clusterBackupService: ClusterBackupService

    @Before
    fun init() {
        // Backup are not enabled automatically in unittests.
        cluster = indexClusterService.createDefaultCluster()
    }

    /**
     * Tests almost everything in a single function because once
     * backups are enabled some async processors kick off on ES
     * which make testing from multiple functions impossible
     * without random failures.
     */
    @Test
    fun testEnableBackups() {
        val rsp1 = clusterBackupService.getRepository(cluster)
        assertNull(rsp1)
        clusterBackupService.enableBackups(cluster)

        // Test getting the repository
        val repos = clusterBackupService.getRepository(cluster)
        assertNotNull(repos)

        // Test getting the
        val policy = clusterBackupService.getSnapshotPolicy(cluster)
        assertNotNull(policy)
        assertEquals("<daily-snapshot-{now/d}>", policy?.name)

        Thread.sleep(5000)

        val snapshots = clusterBackupService.getSnapshots(cluster)
        assertEquals(1, snapshots.size)
    }

    /*
    @Test
    @Ignore
    // docker run -d --name elasticsearch-test  -p 9201:9200 -p 9301:9300 -e "discovery.type=single-node" -e "MINIO_URL={yourlocalip}:9000" -e "network.host=0.0.0.0" boonai/elasticsearch:latest
    fun restoreBackupIntoNewCluster() {
        val newCluster =
            indexClusterService.createIndexCluster(IndexClusterSpec("http://localhost:9201", false))

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/cat-movie.m4v"))
        )

        // Add a label
        val assetId = assetService.batchCreate(batchCreate).created[0]
        var asset = assetService.getAsset(assetId)

        // Create a snapshot on Old Repository
        val snapshotName = "test-snapshot"
        clusterBackupService.createClusterSnapshot(cluster, snapshotName)

        // Restore snapshot on new Repository
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

        assertEquals(assetOnOldCluster.id, assetOnNewCluster.id)
        assertEquals(assetOnOldCluster, assetOnNewCluster)
    }
    */
}
