package com.zorroa.archivist.service

import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexTask
import com.zorroa.archivist.domain.IndexTaskState
import com.zorroa.archivist.domain.IndexTaskType
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.repository.IndexTaskDao
import com.zorroa.archivist.repository.IndexTaskJdbcDao
import com.zorroa.archivist.repository.ProjectCustomDao
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.withAuth
import com.zorroa.zmlp.service.security.getProjectId
import com.zorroa.zmlp.service.security.getZmlpActor
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.bytes.BytesReference
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.ReindexRequest
import org.elasticsearch.index.reindex.RemoteInfo
import org.elasticsearch.tasks.TaskInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.net.URI
import java.util.Timer
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

interface IndexTaskService {

    /**
     * Create a ES task that moves data from one index to another.
     */
    fun createIndexMigrationTask(spec: IndexMigrationSpec): IndexTask

    /**
     * Get an intenral task info.
     */
    fun getEsTaskInfo(task: IndexTask): TaskInfo
}

@Service
class IndexTaskServiceImpl(
    val indexRouteDao: IndexRouteDao,
    val indexRoutingService: IndexRoutingService,
    val indexTaskDao: IndexTaskDao
) : IndexTaskService {

    override fun createIndexMigrationTask(spec: IndexMigrationSpec): IndexTask {
        // If the source route is null then just use the project default
        val srcRouteId = spec.srcIndexRouteId ?: indexRouteDao.getProjectRoute().id
        val srcRoute = indexRouteDao.get(srcRouteId)
        val dstRoute = indexRouteDao.get(spec.dstIndexRouteId)
        val dstRouteClient = indexRoutingService.getClusterRestClient(dstRoute)
        indexRoutingService.setIndexRefreshInterval(dstRoute, "-1")


        val req = ReindexRequest()
            .setSourceIndices(srcRoute.indexName)
            .setSourceQuery(QueryBuilders.matchAllQuery())
            .setDestIndex(dstRoute.indexName)

        val uri = URI(srcRoute.clusterUrl)

        // If the source route is some place new
        if (srcRoute.clusterId != dstRoute.clusterId) {
            val builder = XContentBuilder.builder(RemoteInfo.QUERY_CONTENT_TYPE).prettyPrint()
            val search =
                BytesReference.bytes(QueryBuilders.matchAllQuery().toXContent(builder, ToXContent.EMPTY_PARAMS))

            req.remoteInfo = RemoteInfo(
                "http",
                uri.host,
                uri.port,
                null,
                search,
                null,
                null,
                emptyMap(),
                TimeValue.timeValueSeconds(120),
                TimeValue.timeValueSeconds(120)
            )
        }

        val esTask = dstRouteClient.client.submitReindexTask(req, RequestOptions.DEFAULT).task
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()

        val indexTask = IndexTask(
            UUID.randomUUID(),
            getProjectId(),
            srcRouteId,
            spec.dstIndexRouteId,
            "Reindex ${srcRoute.id} to ${dstRoute.id}",
            IndexTaskType.REINDEX,
            IndexTaskState.RUNNING,
            esTask,
            time,
            time,
            actor,
            actor
        )

        return indexTaskDao.saveAndFlush(indexTask)
    }

    override fun getEsTaskInfo(task: IndexTask): TaskInfo {
        val rest = indexRoutingService.getClusterRestClient(task.dstIndexRouteId ?: task.srcIndexRouteId)
        return rest.client.tasks().get(task.buildGetTaskRequest(), RequestOptions.DEFAULT).get().taskInfo
    }
}

@Component
class IndexTaskMonitor(
    val projectCustomDao: ProjectCustomDao,
    val indexTaskDao: IndexTaskDao,
    val indexTaskJdbcDao: IndexTaskJdbcDao,
    val indexRoutingService: IndexRoutingService

) {

    var pingTimer: Timer = setupTimer()

    private final fun setupTimer(): Timer {
        logger.info("Staring index task monitor")
        return fixedRateTimer(
            name = "index-ping-timer",
            initialDelay = 10000, period = 10000
        ) {
            // Don't ping clusters during unittest.
            if (!ArchivistConfiguration.unittest) {
                runOneIteration()
            }
        }
    }

    fun runOneIteration() {
        try {
            handleCompletedTasks()
            removeExpired()
        } catch (e: Exception) {
            logger.error("Failed to run IndexTask Monitor iteration", e)
        }
    }

    fun handleCompletedTasks(): Int {
        var handled = 0
        for (task in indexTaskDao.getAllByStateOrderByTimeCreatedDesc(IndexTaskState.RUNNING)) {
            try {
                if (task.type == IndexTaskType.REINDEX) {
                    if (handleReindexTask(task)) {
                        handled += 1
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to handle completed index task: ${task.id}, ${task.name}", e)
            }
        }

        return handled
    }

    fun removeExpired() {
        val count = indexTaskJdbcDao.deleteExpiredTasks(
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        )
        if (count > 0) {
            logger.info("Removed $count expired index tasks.")
        }
    }

    /**
     * Handles completed reindex tasks.  This involves swapping a project into a
     * new index after a reindex task has completed.
     */
    fun handleReindexTask(task: IndexTask): Boolean {
        val indexRoute = indexRoutingService.getIndexRoute(
            task.dstIndexRouteId ?: throw IllegalArgumentException("Destination index route cannot be null")
        )
        val rest = indexRoutingService.getClusterRestClient(indexRoute)
        val esTask = rest.client.tasks().get(task.buildGetTaskRequest(), RequestOptions.DEFAULT).get()

        return withAuth(InternalThreadAuthentication(indexRoute.projectId, setOf())) {

            // Flip the project into the new index.
            if (!esTask.isCompleted) {
                logger.info("Still waiting on index task ${task.name} to complete.")
                false
            }

            // If this instance actually changes the state of the task, then the route gets swapped.
            if (!indexTaskJdbcDao.updateState(task, IndexTaskState.FINISHED)) {
                false
            }

            if (!projectCustomDao.updateIndexRoute(task.projectId, indexRoute)) {
                logger.warn("Unable to set new index route for project ${task.projectId}")
                false
            }

            // Close down the src index.
            val srcRoute = indexRoutingService.getIndexRoute(task.srcIndexRouteId)
            indexRoutingService.closeIndex(srcRoute)

            // reset the refresh
            indexRoutingService.setIndexRefreshInterval(indexRoute, "5s")

            logger.info(
                "Index route for project ${task.projectId} " +
                    "swapped to ${indexRoute.id} / ${indexRoute.indexUrl}"
            )
            true
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexTaskMonitor::class.java)
    }
}
