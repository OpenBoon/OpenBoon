package com.zorroa.archivist.service

import com.zorroa.archivist.domain.ArchivistException
import com.zorroa.archivist.domain.IndexCluster
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.core.AcknowledgedResponse
import org.elasticsearch.client.slm.DeleteSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.ExecuteSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.ExecuteSnapshotLifecyclePolicyResponse
import org.elasticsearch.client.slm.GetSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.GetSnapshotLifecyclePolicyResponse
import org.elasticsearch.client.slm.PutSnapshotLifecyclePolicyRequest
import org.elasticsearch.client.slm.SnapshotLifecyclePolicy
import org.elasticsearch.client.slm.SnapshotRetentionConfiguration
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.repositories.fs.FsRepository
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
     * Asynchronously Create a repository where the snapshots will be stored
     * @param cluster: [IndexCluster] Cluster reference
     */
    fun createClusterRepositoryAsync(cluster: IndexCluster)

    /**
     * Get Cluster Repository
     * @param cluster: [IndexCluster] Cluster reference
     */
    fun getRepository(cluster: IndexCluster): GetRepositoriesResponse

    /**
     * Create a Snapshot Lifecycle Policy
     * @param cluster: [IndexCluster] Cluster reference
     * @param policyId: [String] Policy Name
     * @param schedule: [String] Schedule String in Cron Syntax
     * @param indices: [List] List of indices that will be considered to Back up
     * @param maxRetentionDays: [Long] Maximum Amount of Days that the Snapshots will be stored before being discarded
     * @param minimumSnapshotCount: [Int] Minimum Amount of Snapshot that will be keeped even if [maxRetentionDays] is achieved
     * @param maximumSnapshotCount: [Int] Maximum Amount of Snapshot that will be keeped even if [maxRetentionDays] is not achieved
     */
    fun createClusterSnapshotPolicy(
        cluster: IndexCluster,
        policyId: String,
        schedule: String,
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    )

    /**
     * Asynchronously Create a Snapshot Lifecycle Policy
     * @param cluster: [IndexCluster] Cluster reference
     * @param policyId: [String] Policy Name
     * @param schedule: [String] Schedule String in Cron Syntax
     * @param indices: [List] List of indices that will be considered to Back up
     * @param maxRetentionDays: [Long] Maximum Amount of Days that the Snapshots will be stored before being discarded
     * @param minimumSnapshotCount: [Int] Minimum Amount of Snapshot that will be keeped even if [maxRetentionDays] is achieved
     * @param maximumSnapshotCount: [Int] Maximum Amount of Snapshot that will be keeped even if [maxRetentionDays] is not achieved
     */
    fun createClusterSnaphotPolicyAsync(
        cluster: IndexCluster,
        policyId: String,
        schedule: String,
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    )

    /**
     * Execute a snapshot using specified policy
     * @param cluster: [IndexCluster] Cluster reference
     * @param policyId: [String] Policy ID
     */
    fun executeClusterSnapshotPolicy(cluster: IndexCluster, policyId: String): ExecuteSnapshotLifecyclePolicyResponse

    /**
     * Retrieve a Policy by ID
     * @param cluster: [IndexCluster] Cluster reference
     * @param policyId: [String] Policy ID
     */
    fun getClusterSnapshotPolicy(cluster: IndexCluster, policyId: String): GetSnapshotLifecyclePolicyResponse

    /**
     * Delete cluster policy by ID
     * @param cluster: [IndexCluster] Cluster reference
     * @param policyId: [String] Policy ID
     */
    fun deleteClusterSnapshotPolicy(cluster: IndexCluster, policyId: String): AcknowledgedResponse

    /**
     * Generate a Repository Based on IndexCluster name
     */
    fun getRepositoryName(cluster: IndexCluster) = Base64.getEncoder().encodeToString(cluster.url.toByteArray())

    /**
     * Get All Snapshots
     */
    fun getSnapshots(cluster: IndexCluster): GetSnapshotsResponse

    /**
     * Delete Snapshot by Name
     */
    fun deleteSnapshot(cluster: IndexCluster, snapshotName: String)

    /**
     * Create a Instant Snapshot of a Cluster
     */
    fun createClusterSnapshot(cluster: IndexCluster, name: String?)

    /**
     * Asynchronously Create a Instant Snapshot of a Cluster
     */

    fun createClusterSnapshotAsync(cluster: IndexCluster, name: String?)

    /**
     *
     */
    fun getBasePath(indexCluster: IndexCluster, basePath: String): String {
        return "${indexCluster.id}/$basePath"
    }

    fun getSnapshotPutRequest(
        name: String?,
        cluster: IndexCluster
    ): CreateSnapshotRequest {
        var snapshotName = name ?: Date().time.toString()
        var request = CreateSnapshotRequest()
        request.repository(getRepositoryName(cluster))
        request.snapshot(snapshotName)
        request.waitForCompletion(true)
        return request
    }

    fun getSnapshotPolicyPutRequest(
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int,
        policyId: String,
        schedule: String,
        cluster: IndexCluster
    ): PutSnapshotLifecyclePolicyRequest {
        val config: MutableMap<String, Any> = HashMap()
        config["indices"] = indices
        val retention = SnapshotRetentionConfiguration(
            TimeValue.timeValueDays(maxRetentionDays),
            minimumSnapshotCount,
            maximumSnapshotCount
        )

        val policy = SnapshotLifecyclePolicy(
            policyId, policyId, schedule,
            getRepositoryName(cluster), config, retention
        )
        return PutSnapshotLifecyclePolicyRequest(policy)
    }
}

/**
 * Log the storage of a snapshot.
 */
fun logCreateSnapshot(cluster: IndexCluster, request: CreateSnapshotRequest) {
    ProjectStorageService.logger.event(
        LogObject.CLUSTER_SNAPSHOT, LogAction.CREATE,
        mapOf(
            "clusterId" to cluster.id,
            "repository" to request.repository(),
            "snapshot" to request.snapshot()
        )
    )
}

fun logDeleteSnapshot(cluster: IndexCluster, request: DeleteSnapshotRequest) {
    ProjectStorageService.logger.event(
        LogObject.CLUSTER_SNAPSHOT, LogAction.DELETE,
        mapOf(
            "clusterId" to cluster.id,
            "repository" to request.repository(),
            "snapshot" to request.snapshot()
        )
    )
}

fun logCreateSnapshotPolicy(cluster: IndexCluster, request: PutSnapshotLifecyclePolicyRequest) {
    ProjectStorageService.logger.event(
        LogObject.CLUSTER_SNAPSHOT_POLICY, LogAction.CREATE,
        mapOf(
            "clusterId" to cluster.id,
            "repository" to request.policy.repository,
            "policyName" to request.policy.name
        )
    )
}

fun logDeleteSnapshotPolicy(cluster: IndexCluster, request: DeleteSnapshotLifecyclePolicyRequest) {
    ProjectStorageService.logger.event(
        LogObject.CLUSTER_SNAPSHOT_POLICY, LogAction.CREATE,
        mapOf(
            "clusterId" to cluster.id,
            "policyId" to request.policyId
        )
    )
}

fun logCreateClusterRepository(cluster: IndexCluster, request: PutRepositoryRequest) {
    ProjectStorageService.logger.event(
        LogObject.CLUSTER_REPOSITORY, LogAction.CREATE,
        mapOf(
            "clusterId" to cluster.id,
            "repository" to request.name(),
            "clusterType" to request.type()
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
    override fun getRepository(cluster: IndexCluster): GetRepositoriesResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        return client.snapshot().getRepository(GetRepositoriesRequest(), RequestOptions.DEFAULT)
    }

    private fun getCreateRepositoryRequest(client: RestHighLevelClient, cluster: IndexCluster): PutRepositoryRequest {

        var settings =
            Settings
                .builder()
                .put("bucket", bucket)
                .put("base_path", getBasePath(cluster, basePath))
        return PutRepositoryRequest()
            .type("gcs")
            .name(getRepositoryName(cluster))
            .settings(settings)
            .verify(true)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-snapshot-create-repository.html
     */
    override fun createClusterRepository(cluster: IndexCluster) {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val putRepositoryRequest = getCreateRepositoryRequest(client, cluster)

        client.snapshot().createRepository(putRepositoryRequest, RequestOptions.DEFAULT)
    }

    override fun createClusterRepositoryAsync(cluster: IndexCluster) {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val putRepositoryRequest = getCreateRepositoryRequest(client, cluster)

        val listener = object : ActionListener<org.elasticsearch.action.support.master.AcknowledgedResponse> {
            override fun onResponse(putRepositoryResponse: org.elasticsearch.action.support.master.AcknowledgedResponse) {
                logCreateClusterRepository(cluster, putRepositoryRequest)
            }

            override fun onFailure(exception: Exception) {
                throw ArchivistException(exception)
            }
        }

        client.snapshot().createRepositoryAsync(putRepositoryRequest, RequestOptions.DEFAULT, listener)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-snapshot-create-snapshot.html
     */
    override fun createClusterSnapshot(cluster: IndexCluster, name: String?) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        var request = getSnapshotPutRequest(name, cluster)
        client.snapshot().create(request, RequestOptions.DEFAULT)
        logCreateSnapshot(cluster, request)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-snapshot-create-snapshot.html
     */
    override fun createClusterSnapshotAsync(cluster: IndexCluster, name: String?) {

        val client = indexClusterService.getRestHighLevelClient(cluster)

        var request = getSnapshotPutRequest(name, cluster)

        val listener = object : ActionListener<CreateSnapshotResponse> {
            override fun onResponse(createSnapshotResponse: CreateSnapshotResponse) {
                logCreateSnapshot(cluster, request)
            }

            override fun onFailure(exception: Exception) {
                throw ArchivistException(exception)
            }
        }
        client.snapshot().createAsync(request, RequestOptions.DEFAULT, listener)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-ilm-slm-put-snapshot-lifecycle-policy.html
     */

    override fun createClusterSnapshotPolicy(
        cluster: IndexCluster,
        policyId: String,
        schedule: String, // cron syntax
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    ) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        val policyRequest = getSnapshotPolicyPutRequest(
            indices,
            maxRetentionDays,
            minimumSnapshotCount,
            maximumSnapshotCount,
            policyId,
            schedule,
            cluster
        )

        client.indexLifecycle().putSnapshotLifecyclePolicy(policyRequest, RequestOptions.DEFAULT)
        logCreateSnapshotPolicy(cluster, policyRequest)
    }

    override fun createClusterSnaphotPolicyAsync(
        cluster: IndexCluster,
        policyId: String,
        schedule: String,
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    ) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        val policyRequest = getSnapshotPolicyPutRequest(
            indices,
            maxRetentionDays,
            minimumSnapshotCount,
            maximumSnapshotCount,
            policyId,
            schedule,
            cluster
        )

        val listener = object : ActionListener<AcknowledgedResponse> {

            override fun onResponse(resp: AcknowledgedResponse) {
                logCreateSnapshotPolicy(cluster, policyRequest)
            }

            override fun onFailure(exception: java.lang.Exception) {
                throw ArchivistException(exception)
            }
        }

        client.indexLifecycle().putSnapshotLifecyclePolicyAsync(policyRequest, RequestOptions.DEFAULT, listener)
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-ilm-slm-execute-snapshot-lifecycle-policy.html
     */
    override fun executeClusterSnapshotPolicy(
        cluster: IndexCluster,
        policyId: String
    ): ExecuteSnapshotLifecyclePolicyResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val executeRequest = ExecuteSnapshotLifecyclePolicyRequest(policyId)

        return client.indexLifecycle().executeSnapshotLifecyclePolicy(executeRequest, RequestOptions.DEFAULT)
    }

    override fun getClusterSnapshotPolicy(cluster: IndexCluster, policyId: String): GetSnapshotLifecyclePolicyResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        return client.indexLifecycle()
            .getSnapshotLifecyclePolicy(GetSnapshotLifecyclePolicyRequest(policyId), RequestOptions.DEFAULT)
    }

    override fun deleteClusterSnapshotPolicy(cluster: IndexCluster, policyId: String): AcknowledgedResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val deleteSnapshotLifecyclePolicyRequest = DeleteSnapshotLifecyclePolicyRequest(policyId)
        val deleteSnapshotLifecyclePolicy = client.indexLifecycle()
            .deleteSnapshotLifecyclePolicy(deleteSnapshotLifecyclePolicyRequest, RequestOptions.DEFAULT)

        logDeleteSnapshotPolicy(cluster, deleteSnapshotLifecyclePolicyRequest)

        return deleteSnapshotLifecyclePolicy
    }

    override fun getSnapshots(cluster: IndexCluster): GetSnapshotsResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        return client.snapshot().get(GetSnapshotsRequest(), RequestOptions.DEFAULT)
    }

    override fun deleteSnapshot(cluster: IndexCluster, snapshotName: String) {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val deleteSnapshotRequest = DeleteSnapshotRequest(getRepositoryName(cluster), snapshotName)
        client.snapshot()
            .delete(deleteSnapshotRequest, RequestOptions.DEFAULT)
        logDeleteSnapshot(cluster, deleteSnapshotRequest)
    }
}

@Service
@Profile("test", "none", "awsClusterBackup")
class S3ClusterBackupService(
    val indexClusterService: IndexClusterService
) : ClusterBackupService {

    @Value("\${archivist.es.backup.aws.bucket}")
    lateinit var bucket: String

    @Value("\${archivist.es.backup.aws.base-path}")
    lateinit var basePath: String

    override fun createClusterRepository(cluster: IndexCluster) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        var settings =
            Settings
                .builder()
                .put("bucket", bucket)
                .put("base_path", getBasePath(cluster, basePath))
                .put("compress", true)

        val putRepositoryRequest = PutRepositoryRequest()
            .type("s3")
            .name(getRepositoryName(cluster))
            .settings(settings)
            .verify(true)

        client.snapshot().createRepository(putRepositoryRequest, RequestOptions.DEFAULT)
        logCreateClusterRepository(cluster, putRepositoryRequest)
    }

    override fun createClusterRepositoryAsync(cluster: IndexCluster) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        var settings =
            Settings
                .builder()
                .put("bucket", bucket)
                .put("base_path", basePath)
                .put(FsRepository.LOCATION_SETTING.key, "./config/backups")
                .put(FsRepository.COMPRESS_SETTING.key, true)

        val putRepositoryRequest = PutRepositoryRequest()
            .type(FsRepository.TYPE)
            .name(getRepositoryName(cluster))
            .settings(settings)
            .verify(true)

        val listener = object : ActionListener<org.elasticsearch.action.support.master.AcknowledgedResponse> {
            override fun onResponse(putRepositoryResponse: org.elasticsearch.action.support.master.AcknowledgedResponse) {
                logCreateClusterRepository(cluster, putRepositoryRequest)
            }

            override fun onFailure(exception: Exception) {
                throw ArchivistException(exception)
            }
        }

        client.snapshot().createRepositoryAsync(putRepositoryRequest, RequestOptions.DEFAULT, listener)
    }

    override fun createClusterSnapshot(cluster: IndexCluster, name: String?) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        var request = getSnapshotPutRequest(name, cluster)
        client.snapshot().create(request, RequestOptions.DEFAULT)
        logCreateSnapshot(cluster, request)
    }

    override fun createClusterSnapshotAsync(cluster: IndexCluster, name: String?) {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        var request = getSnapshotPutRequest(name, cluster)

        val listener = object : ActionListener<CreateSnapshotResponse> {
            override fun onResponse(createSnapshotResponse: CreateSnapshotResponse) {
                logCreateSnapshot(cluster, request)
            }

            override fun onFailure(exception: Exception) {
                throw ArchivistException(exception)
            }
        }

        client.snapshot().createAsync(request, RequestOptions.DEFAULT, listener)
    }

    override fun getRepository(cluster: IndexCluster): GetRepositoriesResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        return client.snapshot().getRepository(GetRepositoriesRequest(), RequestOptions.DEFAULT)
    }

    override fun createClusterSnapshotPolicy(
        cluster: IndexCluster,
        policyId: String,
        schedule: String, // cron syntax
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    ) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        val snapshotPolicyPutRequest = getSnapshotPolicyPutRequest(
            indices,
            maxRetentionDays,
            minimumSnapshotCount,
            maximumSnapshotCount,
            policyId,
            schedule,
            cluster
        )

        client.indexLifecycle().putSnapshotLifecyclePolicy(snapshotPolicyPutRequest, RequestOptions.DEFAULT)
    }

    override fun createClusterSnaphotPolicyAsync(
        cluster: IndexCluster,
        policyId: String,
        schedule: String,
        indices: List<String>,
        maxRetentionDays: Long,
        minimumSnapshotCount: Int,
        maximumSnapshotCount: Int
    ) {
        val client = indexClusterService.getRestHighLevelClient(cluster)

        val policyRequest = getSnapshotPolicyPutRequest(
            indices,
            maxRetentionDays,
            minimumSnapshotCount,
            maximumSnapshotCount,
            policyId,
            schedule,
            cluster
        )

        val listener = object : ActionListener<AcknowledgedResponse> {

            override fun onResponse(resp: AcknowledgedResponse) {
                logCreateSnapshotPolicy(cluster, policyRequest)
            }

            override fun onFailure(exception: java.lang.Exception) {
                throw ArchivistException(exception)
            }
        }
        client.indexLifecycle().putSnapshotLifecyclePolicyAsync(policyRequest, RequestOptions.DEFAULT, listener)
    }

    override fun executeClusterSnapshotPolicy(
        cluster: IndexCluster,
        policyId: String
    ): ExecuteSnapshotLifecyclePolicyResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val executeRequest = ExecuteSnapshotLifecyclePolicyRequest(policyId)

        return client.indexLifecycle().executeSnapshotLifecyclePolicy(executeRequest, RequestOptions.DEFAULT)
    }

    override fun getClusterSnapshotPolicy(cluster: IndexCluster, policyId: String): GetSnapshotLifecyclePolicyResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        return client.indexLifecycle()
            .getSnapshotLifecyclePolicy(GetSnapshotLifecyclePolicyRequest(policyId), RequestOptions.DEFAULT)
    }

    override fun deleteClusterSnapshotPolicy(cluster: IndexCluster, policyId: String): AcknowledgedResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val deleteSnapshotLifecyclePolicyRequest = DeleteSnapshotLifecyclePolicyRequest(policyId)
        val deleteSnapshotLifecyclePolicy = client.indexLifecycle()
            .deleteSnapshotLifecyclePolicy(deleteSnapshotLifecyclePolicyRequest, RequestOptions.DEFAULT)

        logDeleteSnapshotPolicy(cluster, deleteSnapshotLifecyclePolicyRequest)
        return deleteSnapshotLifecyclePolicy
    }

    override fun getSnapshots(cluster: IndexCluster): GetSnapshotsResponse {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        return client.snapshot().get(GetSnapshotsRequest(getRepositoryName(cluster)), RequestOptions.DEFAULT)
    }

    override fun deleteSnapshot(cluster: IndexCluster, snapshotName: String) {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        val deleteSnapshotRequest = DeleteSnapshotRequest(getRepositoryName(cluster), snapshotName)
        client.snapshot().delete(deleteSnapshotRequest, RequestOptions.DEFAULT)
        logDeleteSnapshot(cluster, deleteSnapshotRequest)
    }
}
