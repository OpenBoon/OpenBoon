package boonai.archivist.service

import boonai.archivist.config.ApplicationProperties
import boonai.archivist.config.ArchivistConfiguration
import boonai.archivist.domain.IndexCluster
import boonai.archivist.domain.IndexClusterFilter
import boonai.archivist.domain.IndexClusterSpec
import boonai.archivist.domain.IndexClusterState
import boonai.archivist.repository.IndexClusterDao
import boonai.archivist.repository.IndexRouteDao
import boonai.archivist.repository.KPagedList
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import org.elasticsearch.client.Request
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.fixedRateTimer

/**
 * A service for managing ES clusters available for project use.
 */
interface IndexClusterService {

    /**
     * Create a new [IndexCluster]. The new cluster will be available for use
     * when it's successfully pinged.
     */
    fun createIndexCluster(spec: IndexClusterSpec): IndexCluster

    /**
     * Get a paged list of [IndexCluster]s that match the given filter.
     */
    fun getAll(filter: IndexClusterFilter): KPagedList<IndexCluster>

    /**
     * Get an ES cluster by ID.
     */
    fun getIndexCluster(id: UUID): IndexCluster

    /**
     * Get an [IndexCluster] in the auto-poll with the least amount of active indexes.
     */
    fun getNextAutoPoolCluster(): IndexCluster

    /**
     * Runs at startup and attempts to detect the default cluster
     * configured with the 'archivist.es.url' property.  This is mainly
     * for limiting the amount of manual setup that has to get done
     * before the platform can be used.  If the cluster already
     * exists then this operation is skipped.
     */
    fun createDefaultCluster(): IndexCluster

    /**
     * Return a [RestClient] to the given IndexCluster
     */
    fun getLowLevelClient(cluster: IndexCluster): RestClient

    /**
     * Return a [RestHighLevelClient] to the given IndexCluster
     */
    fun getRestHighLevelClient(cluster: IndexCluster): RestHighLevelClient
}

@Service
class IndexClusterServiceImpl constructor(
    val indexClusterDao: IndexClusterDao,
    val esClientCache: EsClientCache,
    val properties: ApplicationProperties

) : IndexClusterService {

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (ArchivistConfiguration.unittest) {
            return
        }

        // See AbstractTest for how these are setup for testsl
        createDefaultCluster()
    }

    override fun createIndexCluster(spec: IndexClusterSpec): IndexCluster {

        val indexCluster = indexClusterDao.create(spec)

        logger.event(
            LogObject.INDEX_CLUSTER, LogAction.CREATE,
            mapOf(
                "indexClusterId" to indexCluster.id
            )
        )
        return indexCluster
    }

    override fun getIndexCluster(id: UUID): IndexCluster {
        return indexClusterDao.get(id)
    }

    override fun getAll(filter: IndexClusterFilter): KPagedList<IndexCluster> {
        return indexClusterDao.getAll(filter)
    }

    override fun createDefaultCluster(): IndexCluster {
        val clusterUrl = properties.getString("archivist.es.url")
        return if (!indexClusterDao.exists(clusterUrl)) {
            val spec = IndexClusterSpec(
                clusterUrl,
                true
            )
            createIndexCluster(spec)
        } else {
            indexClusterDao.get(clusterUrl)
        }
    }

    override fun getNextAutoPoolCluster(): IndexCluster {
        return indexClusterDao.getNextAutoPoolCluster()
    }

    override fun getRestHighLevelClient(cluster: IndexCluster): RestHighLevelClient {
        return esClientCache.getRestHighLevelClient(cluster.url)
    }

    override fun getLowLevelClient(cluster: IndexCluster): RestClient {
        return getRestHighLevelClient(cluster).lowLevelClient
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexClusterServiceImpl::class.java)
    }
}

/**
 * IndexClusterPingTimer handles keeping track of up/down state of a cluster
 * by communicating with the cluster on a frequent basis.
 *
 * The first time communication is established with the server, the backups
 * are enabled from this class.
 */
@Component
class IndexClusterMonitor(
    val indexClusterDao: IndexClusterDao,
    val indexRouteDao: IndexRouteDao,
    val indexClusterService: IndexClusterService,
    val clusterBackupService: ClusterBackupService

) {

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    var pingTimer: Timer = setupClusterPingTimer()

    val backupsEnabled = mutableSetOf<UUID>()

    val syncedClusters = mutableSetOf<UUID>()

    fun setupClusterPingTimer(): Timer {
        return fixedRateTimer(
            name = "index-ping-timer",
            initialDelay = 500, period = 5000
        ) {
            // Don't ping clusters during unittest.
            if (!ArchivistConfiguration.unittest) {
                pingAllClusters()
            }
        }
    }

    fun pingAllClusters() {
        for (cluster in indexClusterDao.getAll()) {
            if (pingCluster(cluster)) {

                // Migrate to new index versions.
                syncIndexRoutes(cluster)

                // Ensure backups enabled.
                enableBackups(cluster)
            }
        }
    }

    fun syncIndexRoutes(cluster: IndexCluster) {
        if (cluster.id in syncedClusters) {
            return
        }
        syncedClusters.add(cluster.id)
        indexRoutingService.syncAllIndexRoutes(cluster)
    }

    fun enableBackups(cluster: IndexCluster) {

        /**
         * Check if we've already enabled backups.
         */
        if (cluster.id in backupsEnabled) {
            return
        }

        /**
         * Double check if we've already enabled backups.
         */
        val repos = clusterBackupService.getRepository(cluster)
        if (repos == null) {
            try {
                clusterBackupService.enableBackups(cluster)
                backupsEnabled.add(cluster.id)
            } catch (e: Exception) {
                logger.warn("Failed to enable backups on cluster ${cluster.id}", e)
            }
        } else {
            backupsEnabled.add(cluster.id)
        }
    }

    fun pingCluster(cluster: IndexCluster): Boolean {
        val client = indexClusterService.getRestHighLevelClient(cluster)
        return try {
            val rsp = client.lowLevelClient.performRequest(Request("GET", "/"))
            val content = rsp.entity.content.bufferedReader().use(BufferedReader::readText)
            indexClusterDao.updateAttrs(cluster, content)
            // The status is up if we got a good ping.
            indexClusterDao.updateState(cluster, IndexClusterState.UP)
            true
        } catch (e: Exception) {
            logger.warn("Failed to contact ${cluster.url} for status information", e)
            indexClusterDao.updateState(cluster, IndexClusterState.DOWN)
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexClusterMonitor::class.java)
    }
}
