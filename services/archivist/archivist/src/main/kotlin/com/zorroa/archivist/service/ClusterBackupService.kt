package com.zorroa.archivist.service

import com.zorroa.archivist.domain.ArchivistException
import com.zorroa.archivist.domain.IndexCluster
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.settings.Settings
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date

@Service
interface ClusterBackupService {

    fun createClusterRepository(cluster: IndexCluster)

    fun createClusterSnapshot(cluster: IndexCluster)

    fun hasRepository(cluster: IndexCluster): Boolean
}

/**
 * Log the storage of a file.
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

    override fun hasRepository(cluster: IndexCluster): Boolean {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        var repositories = arrayOf(getRepositoryName(cluster))
        var repository: GetRepositoriesResponse?
        try {
            repository =
                client.snapshot().getRepository(GetRepositoriesRequest(repositories), RequestOptions.DEFAULT)
        } catch (ex: ElasticsearchStatusException) {
            return false
        }
        return repository.repositories().isNotEmpty()
    }

    private fun getRepositoryName(cluster: IndexCluster) = Base64.getEncoder().encodeToString(cluster.url.toByteArray())

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

        client.snapshot().createRepository(putRepositoryRequest, RequestOptions.DEFAULT)
    }

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

        client.snapshot().createAsync(request, RequestOptions.DEFAULT, listener)
    }
}