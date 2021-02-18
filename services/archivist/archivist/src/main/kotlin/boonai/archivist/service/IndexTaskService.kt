package boonai.archivist.service

import boonai.archivist.config.ArchivistConfiguration
import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexTask
import boonai.archivist.domain.IndexTaskState
import boonai.archivist.domain.IndexTaskType
import boonai.archivist.domain.IndexToIndexMigrationSpec
import boonai.archivist.domain.Project
import boonai.archivist.domain.ProjectIndexMigrationSpec
import boonai.archivist.repository.IndexRouteDao
import boonai.archivist.repository.IndexTaskDao
import boonai.archivist.repository.IndexTaskJdbcDao
import boonai.archivist.repository.ProjectDao
import boonai.archivist.security.InternalThreadAuthentication
import boonai.archivist.security.withAuth
import boonai.common.service.security.getZmlpActor
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.tasks.GetTaskResponse
import org.elasticsearch.common.bytes.BytesReference
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.ReindexRequest
import org.elasticsearch.index.reindex.RemoteInfo
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.URI
import java.util.Timer
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

interface IndexTaskService {

    /**
     * Create a ES task that moves data from one index to another.
     */
    fun createIndexMigrationTask(spec: IndexToIndexMigrationSpec): IndexTask

    /**
     * Get an internal task info.
     */
    fun getEsTaskInfo(task: IndexTask): GetTaskResponse

    /**
     * Migrate the specific project to a new index.
     */
    fun migrateProject(project: Project, spec: ProjectIndexMigrationSpec): IndexTask

    /**
     * Get a reindex script associated with an index version.
     */
    fun getReindexScript(route: IndexRoute): String?
}

@Service
class IndexTaskServiceImpl(
    val indexRouteDao: IndexRouteDao,
    val indexRoutingService: IndexRoutingService,
    val indexTaskDao: IndexTaskDao,
) : IndexTaskService {

    override fun migrateProject(project: Project, spec: ProjectIndexMigrationSpec): IndexTask {
        val projectRoute = indexRouteDao.getProjectRoute(project.id)
        val replicas = spec.size?.replicas ?: projectRoute.replicas
        val shards = spec.size?.shards ?: projectRoute.shards
        var clusterId = spec.clusterId ?: projectRoute.clusterId

        /**
         * Create the new index.
         */
        val idxSpec = IndexRouteSpec(
            spec.mapping,
            spec.majorVer,
            replicas = replicas,
            shards = shards,
            clusterId = clusterId,
            projectId = project.id
        )

        /**
         * Launch a new index to index migration.
         */
        val newIndex = indexRoutingService.createIndexRoute(idxSpec)
        return createIndexMigrationTask(
            IndexToIndexMigrationSpec(
                srcIndexRouteId = projectRoute.id,
                dstIndexRouteId = newIndex.id
            )
        )
    }

    override fun getReindexScript(route: IndexRoute): String? {
        val path = "db/migration/elasticsearch/${route.mapping}.v${route.majorVer}.painless"
        val resource = ClassPathResource(path)
        return resource?.inputStream.bufferedReader().use(BufferedReader::readText)
    }

    override fun createIndexMigrationTask(spec: IndexToIndexMigrationSpec): IndexTask {
        val srcRoute = indexRouteDao.get(spec.srcIndexRouteId)
        val dstRoute = indexRouteDao.get(spec.dstIndexRouteId)
        val dstRouteClient = indexRoutingService.getClusterRestClient(dstRoute)
        indexRoutingService.setIndexRefreshInterval(dstRoute, "-1")

        val req = ReindexRequest()
            .setSourceIndices(srcRoute.indexName)
            .setSourceQuery(QueryBuilders.matchAllQuery())
            .setDestIndex(dstRoute.indexName)
            .setSourceBatchSize(500)

        if (dstRoute.majorVer > srcRoute.majorVer) {
            getReindexScript(dstRoute)?.let {
                req.script = Script(ScriptType.INLINE, "painless", it, mapOf())
            }
        }

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

        logger.info("$esTask ES migration tasks created")
        logger.info("$esTask src index: ${srcRoute.indexName} dst index: ${dstRoute.indexName}")

        val indexTask = IndexTask(
            UUID.randomUUID(),
            dstRoute.projectId,
            srcRoute.id,
            dstRoute.id,
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

    override fun getEsTaskInfo(task: IndexTask): GetTaskResponse {
        val rest = indexRoutingService.getClusterRestClient(task.dstIndexRouteId ?: task.srcIndexRouteId)
        return rest.client.tasks().get(task.buildGetTaskRequest(), RequestOptions.DEFAULT).get()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexTaskServiceImpl::class.java)
    }
}

@Component
class IndexTaskMonitor(
    val projectDao: ProjectDao,
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

        if (!esTask.isCompleted) {
            logger.info("Still waiting on ${task.id} / ${task.esTaskId} to complete.")
            return false
        }

        logger.info("ES reindex task completed: ${task.esTaskId} : ${esTask.isCompleted}")
        logger.info("Status: ${esTask.taskInfo.status}")

        return withAuth(InternalThreadAuthentication(indexRoute.projectId, setOf())) {

            // If this instance actually changes the state of the task, then the route gets swapped.
            if (!indexTaskJdbcDao.updateState(task, IndexTaskState.FINISHED)) {
                false
            }

            // This puts the project on the new index.
            if (!indexRoutingService.setIndexRoute(projectDao.getOne(task.projectId), indexRoute)) {
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
