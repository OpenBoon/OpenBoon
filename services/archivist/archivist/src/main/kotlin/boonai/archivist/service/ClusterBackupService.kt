package boonai.archivist.service

import boonai.archivist.domain.IndexCluster
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.slm.ExecuteSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.ExecuteSnapshotLifecyclePolicyResponse
import org.elasticsearch.client.slm.GetSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.PutSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.SnapshotLifecyclePolicy
import org.elasticsearch.client.slm.SnapshotLifecyclePolicyMetadata
import org.elasticsearch.client.slm.SnapshotRetentionConfiguration
import org.elasticsearch.cluster.metadata.RepositoryMetadata
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.snapshots.SnapshotInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Used to store snapshot and set up snapshot policy to backup Cluster data
 */
@Service
interface ClusterBackupService {

    /**
     * Enable backups on the given IndexCluster using the available storage backend.
     */
    fun enableBackups(cluster: IndexCluster)

    /**
     * Get Cluster Repository with the same name as the cluster ID, or null it does not exist.
     *
     * @param cluster: [IndexCluster] Cluster reference
     */
    fun getRepository(cluster: IndexCluster): RepositoryMetadata?

    /**
     * Retrieve a Policy by ID
     *
     * @param cluster: [IndexCluster] Cluster reference
     * @param policyId: [String] Policy ID
     */
    fun getSnapshotPolicy(cluster: IndexCluster): SnapshotLifecyclePolicyMetadata?

    /**
     * Get All Snapshots from a cluster
     *
     * @param cluster: [IndexCluster] Cluster
     */
    fun getSnapshots(cluster: IndexCluster): List<SnapshotInfo>

    /**
     * Restore Cluster State from Another Cluster
     *
     * @param cluster: [IndexCluster] Brand new Cluster
     * @param snapshotName: [String] Name of the snapshot that will be restored
     * @param oldClusterId: [IndexCluster] Cluster that contains the snapshot at its repository
     */
    fun restoreSnapshot(cluster: IndexCluster, snapshotName: String, oldClusterId: UUID): RestoreSnapshotResponse
}

/**
 * Thrown when communication with the Cluster fails.
 */
class ClusterBackupException : RuntimeException {
    constructor(message: String) : super(message) {}
}

/**
 * Configuration for Cluster backups
 */
@ConfigurationProperties("archivist.es.backup")
@Configuration
class ClusterBackupConfiguration {

    /**
     * The bucket name where backups are being stored.  This defaults
     * to the system bucket.
     */
    lateinit var bucketName: String

    /**
     * The schedule in whichb backups should run.
     */
    var schedule: String = "0 30 1 * * ?"

    /**
     * The number of days to return backup snapshots.
     */
    var retenionDays: Long = 30

    /**
     * The minimum number of snapshots to retain.
     */
    var minSnapshots: Int = 5

    /**
     * The maximum number of snapshots top rtain.
     */
    var maxSnapshots: Int = 50
}

@Service
class ClusterBackupServiceImpl(
    val properties: ClusterBackupConfiguration,
    val esClientCache: EsClientCache
) : ClusterBackupService {

    @Autowired
    lateinit var env: Environment

    override fun enableBackups(cluster: IndexCluster) {
        createSnapshotRepository(cluster)
        createSnapshotPolicy(cluster)
        executeSnapshotPolicy(cluster)
    }

    override fun getRepository(cluster: IndexCluster): RepositoryMetadata? {
        val client = esClientCache.getRestHighLevelClient(cluster)
        val repos = client.snapshot().getRepository(
            GetRepositoriesRequest(), RequestOptions.DEFAULT
        ).repositories()
        return repos.firstOrNull { it.name() == getRepositoryName(cluster) }
    }

    override fun restoreSnapshot(
        cluster: IndexCluster,
        snapshotName: String,
        oldClusterId: UUID
    ): RestoreSnapshotResponse {

        val client = esClientCache.getRestHighLevelClient(cluster)

        // Get repository name on old cluster
        var repositoryName = getRepositoryName(oldClusterId)
        // Assign Repository on old Cluster to new Cluster
        assignExistingRepositoryToCluster(cluster, oldClusterId)

        // Restore Snapshot
        val request = RestoreSnapshotRequest(repositoryName, snapshotName)
        return client.snapshot().restore(request, RequestOptions.DEFAULT)
    }

    override fun getSnapshotPolicy(cluster: IndexCluster): SnapshotLifecyclePolicyMetadata? {
        val client = esClientCache.getRestHighLevelClient(cluster)
        val rsp = client.indexLifecycle()
            .getSnapshotLifecyclePolicy(
                GetSnapshotLifecyclePolicyRequest(policyId), RequestOptions.DEFAULT
            )
        return rsp.policies[policyId]
    }

    override fun getSnapshots(cluster: IndexCluster): List<SnapshotInfo> {
        val client = esClientCache.getRestHighLevelClient(cluster)
        val rsp = client.snapshot().get(
            GetSnapshotsRequest().repository(getRepositoryName(cluster)), RequestOptions.DEFAULT
        )
        return rsp.snapshots
    }

    /**
     * In order to restore to a new cluster, you have to put add the repository to the
     * server.
     */
    private fun assignExistingRepositoryToCluster(cluster: IndexCluster, oldClusterId: UUID) {

        val client = esClientCache.getRestHighLevelClient(cluster)
        var repositoryLocation = getBasePath(oldClusterId)
        var repositoryName = getRepositoryName(oldClusterId)

        var settings =
            Settings
                .builder()
                .put("bucket", properties.bucketName)
                .put("base_path", repositoryLocation)
                .put("compress", true)

        val putRepositoryRequest = PutRepositoryRequest()
            .type(getStorageType())
            .name(repositoryName)
            .settings(settings)
            .verify(true)

        client.snapshot().createRepository(putRepositoryRequest, RequestOptions.DEFAULT)
    }

    /**
     * Create a Snapshot Lifecycle Policy
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-ilm-slm-put-snapshot-lifecycle-policy.html
     *
     *  @param cluster: [IndexCluster] Cluster reference
     */
    private fun createSnapshotPolicy(
        cluster: IndexCluster
    ) {
        val client = esClientCache.getRestHighLevelClient(cluster)
        val req = getSnapshotPolicyPutRequest(cluster)
        val rsp = client.indexLifecycle().putSnapshotLifecyclePolicy(req, RequestOptions.DEFAULT)

        if (!rsp.isAcknowledged) {
            throw ClusterBackupException(
                "Attempted to create backup policy on repository but request was not acked."
            )
        }

        logger.event(
            LogObject.CLUSTER_SNAPSHOT_POLICY, LogAction.CREATE,
            mapOf(
                "clusterId" to cluster.id,
                "repositoryName" to req.policy.repository,
                "policyName" to req.policy.name,
                "schedule" to properties.schedule
            )
        )
    }

    /**
     * Create a repository where the snapshots will be stored
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-snapshot-create-repository.html
     * @param cluster: [IndexCluster] Cluster reference
     */
    private fun createSnapshotRepository(cluster: IndexCluster) {
        val client = esClientCache.getRestHighLevelClient(cluster)
        val req = getCreateRepositoryRequest(cluster)
        val rsp = client.snapshot().createRepository(req, RequestOptions.DEFAULT)

        if (!rsp.isAcknowledged) {
            val stype = getStorageType()
            throw ClusterBackupException(
                "Attempted to create $stype repository but request was not acked."
            )
        }

        logger.event(
            LogObject.CLUSTER_REPOSITORY, LogAction.CREATE,
            mapOf(
                "clusterId" to cluster.id,
                "basePath" to req.settings().get("base_path"),
                "bucket" to properties.bucketName,
                "repositoryName" to req.name(),
                "repositoryType" to req.type()
            )
        )
    }

    /**
     * Execute a snapshot using specified policy
     *
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-ilm-slm-execute-snapshot-lifecycle-policy.html
     * @param cluster: [IndexCluster] Cluster reference
     */
    private fun executeSnapshotPolicy(
        cluster: IndexCluster
    ): ExecuteSnapshotLifecyclePolicyResponse {
        val client = esClientCache.getRestHighLevelClient(cluster)
        val executeRequest = ExecuteSnapshotLifecyclePolicyRequest(policyId)
        return client.indexLifecycle().executeSnapshotLifecyclePolicy(executeRequest, RequestOptions.DEFAULT)
    }

    private fun getSnapshotPolicyPutRequest(
        cluster: IndexCluster
    ): PutSnapshotLifecyclePolicyRequest {

        val retention = SnapshotRetentionConfiguration(
            TimeValue.timeValueDays(properties.retenionDays),
            properties.minSnapshots,
            properties.maxSnapshots
        )

        val policy = SnapshotLifecyclePolicy(
            policyId, "<daily-snapshot-{now/d}>",
            properties.schedule,
            getRepositoryName(cluster),
            mapOf(
                "indicies" to listOf("*")
            ),
            retention
        )
        return PutSnapshotLifecyclePolicyRequest(policy)
    }

    private fun getStorageType(): String {
        val profiles = env.activeProfiles
        return if ("gcs" in profiles) {
            "gcs"
        } else {
            "s3"
        }
    }

    private fun getCreateRepositoryRequest(cluster: IndexCluster): PutRepositoryRequest {
        val settings = Settings
            .builder()
            .put("bucket", properties.bucketName)
            .put("base_path", getBasePath(cluster))
            .put("compress", true)
        return PutRepositoryRequest()
            .type(getStorageType())
            .name(getRepositoryName(cluster))
            .settings(settings)
            .verify(true)
    }

    /**
     * Generate a Repository name Based on IndexCluster ID
     *
     * @param cluster: Cluster which name will be generated
     */
    private fun getRepositoryName(cluster: IndexCluster) = cluster.id.toString()

    /**
     * Generate a Repository name Based on IndexCluster ID
     *
     * @param id: The cluster id.
     */
    private fun getRepositoryName(id: UUID) = id.toString()

    /**
     * Get the repository base path.
     *
     * @param indexCluster: The IndexCluster instance.
     */
    private fun getBasePath(indexCluster: IndexCluster): String {
        return getBasePath(indexCluster.id)
    }

    /**
     * Get the repository base bath.
     *
     * @param id: The cluster ID.
     */
    private fun getBasePath(id: UUID): String {
        return "index-clusters/$id/backups"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterBackupServiceImpl::class.java)

        /**
         * The name of the backup policy.
         */
        const val policyId = "daily-snapshot"
    }
}
