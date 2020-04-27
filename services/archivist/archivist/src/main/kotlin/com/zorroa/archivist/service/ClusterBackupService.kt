package com.zorroa.archivist.service

import com.zorroa.archivist.domain.ArchivistException
import com.zorroa.archivist.domain.IndexCluster
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest
import org.elasticsearch.action.admin.cluster.repositories.verify.VerifyRepositoryRequest
import org.elasticsearch.action.admin.cluster.repositories.verify.VerifyRepositoryResponse
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.slm.ExecuteSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.PutSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.SnapshotLifecyclePolicy
import org.elasticsearch.client.slm.SnapshotRetentionConfiguration
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date
import java.util.HashMap

/**
 * Used to store snapshot and set up snapshot policy to backup Cluster data
 */
@Service
interface ClusterBackupService {

    /**
     * Create a repository where the snapshots will be stored
     * @param cluster: [IndexCluster] Cluster reference
     */
    fun createClusterRepository(cluster: IndexCluster)

    /**
     * Create a instant Snapshot
     * @param cluster: [IndexCluster] Cluster reference
     */
    fun createClusterSnapshot(cluster: IndexCluster)

    /**
     * Check if a Cluster already has a repository
     * @param cluster: [IndexCluster] Cluster reference
     */
    fun hasRepository(cluster: IndexCluster): Boolean

    /**
     * Create a Snapshot Lifecycle Policy
     * @param cluster: [IndexCluster] Cluster reference
     * @param policyName: [String] Policy Name
     * @param schedule: [String] Schedule String in Cron Syntax
     * @param indices: [List] List of indices that will be considered to Back up
     * @param maxRetentionDays: [Long] Maximum Amount of Days that the Snapshots will be stored before being discarded
     * @param minimumSnapshotCount: [Int] Minimum Amount of Snapshot that will be keeped even if [maxRetentionDays] is achieved
     * @param maximumSnapshotCount: [Int] Maximum Amount of Snapshot that will be keeped even if [maxRetentionDays] is not achieved
     */
    fun createClusterPolicy(
        cluster: IndexCluster,
        policyName: String,
        schedule: String,
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    )

    fun executeClusterPolicy(cluster: IndexCluster, policyId: String): String
}

/**
 * Log the storage of a snapshot.
 */
fun logCreateSnapshot(snapshotRequest: CreateSnapshotRequest) {
    ProjectStorageService.logger.event(
        LogObject.CLUSTER_SNAPSHOT, LogAction.CREATE,
        mapOf(
            "repository" to snapshotRequest.repository(),
            "snapshot" to snapshotRequest.snapshot()
        )
    )
}

@Service
@Profile("gcsClusterBackup")
class GcsClusterBackupService(
    val indexClusterService: IndexClusterService
) : ClusterBackupService {

    @Value("\${archivist.es.backup.gcs.bucket}")
    lateinit var bucket: String

    @Value("\${archivist.es.backup.gcs.base-path}")
    lateinit var basePath: String

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-snapshot-verify-repository.html
     */
    override fun hasRepository(cluster: IndexCluster): Boolean {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        var repository: VerifyRepositoryResponse?
        try {
            // sync
            repository =
                client.snapshot()
                    .verifyRepository(VerifyRepositoryRequest(getRepositoryName(cluster)), RequestOptions.DEFAULT)
        } catch (ex: ElasticsearchStatusException) {
            return false
        }
        return !repository.nodes.isNullOrEmpty()
    }

    private fun getRepositoryName(cluster: IndexCluster) = Base64.getEncoder().encodeToString(cluster.url.toByteArray())

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-snapshot-create-repository.html
     */

    override fun createClusterRepository(cluster: IndexCluster) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        var settings =
            Settings
                .builder()
                .put("bucket", bucket)
                .put("base_path", basePath)
        val putRepositoryRequest = PutRepositoryRequest()
            .type("gcs")
            .name(getRepositoryName(cluster))
            .settings(settings)
            .verify(true)

        // Sync
        client.snapshot().createRepository(putRepositoryRequest, RequestOptions.DEFAULT)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-snapshot-create-snapshot.html
     */
    override fun createClusterSnapshot(cluster: IndexCluster) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        var request = CreateSnapshotRequest()
        request.repository(getRepositoryName(cluster))
        request.snapshot(Date().time.toString())
        request.waitForCompletion(true)

        val listener = object : ActionListener<CreateSnapshotResponse> {
            override fun onResponse(createSnapshotResponse: CreateSnapshotResponse) {
                logCreateSnapshot(request)
            }

            override fun onFailure(exception: Exception) {
                throw ArchivistException(exception)
            }
        }

        // Async
        client.snapshot().createAsync(request, RequestOptions.DEFAULT, listener)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-ilm-slm-put-snapshot-lifecycle-policy.html
     */

    override fun createClusterPolicy(
        cluster: IndexCluster,
        policyName: String,
        schedule: String, // cron syntax
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    ) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        val config: MutableMap<String, Any> = HashMap()
        config["indices"] = indices
        val retention = SnapshotRetentionConfiguration(
            TimeValue.timeValueDays(maxRetentionDays),
            minimumSnapshotCount,
            maximumSnapshotCount
        )

        val policy = SnapshotLifecyclePolicy(
            policyName, policyName, schedule,
            getRepositoryName(cluster), config, retention
        )
        val policyRequest = PutSnapshotLifecyclePolicyRequest(policy)

        // Sync
        client.indexLifecycle().putSnapshotLifecyclePolicy(policyRequest, RequestOptions.DEFAULT)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-ilm-slm-execute-snapshot-lifecycle-policy.html
     */
    override fun executeClusterPolicy(cluster: IndexCluster, policyId: String): String {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val executeRequest = ExecuteSnapshotLifecyclePolicyRequest(policyId)

        val executeSnapshotLifecyclePolicy =
            client.indexLifecycle().executeSnapshotLifecyclePolicy(executeRequest, RequestOptions.DEFAULT)

        return executeSnapshotLifecyclePolicy.snapshotName
    }
}

@Service
@Profile ("test", "none")
class nullClusterBackupService(): ClusterBackupService{
    override fun createClusterRepository(cluster: IndexCluster) {
        TODO("Not yet implemented")
    }

    override fun createClusterSnapshot(cluster: IndexCluster) {
        TODO("Not yet implemented")
    }

    override fun hasRepository(cluster: IndexCluster): Boolean {
        TODO("Not yet implemented")
    }

    override fun createClusterPolicy(
        cluster: IndexCluster,
        policyName: String,
        schedule: String,
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun executeClusterPolicy(cluster: IndexCluster, policyId: String): String {
        TODO("Not yet implemented")
    }
}
