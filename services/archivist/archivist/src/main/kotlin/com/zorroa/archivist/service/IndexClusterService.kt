package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.IndexCluster
import com.zorroa.archivist.domain.IndexClusterFilter
import com.zorroa.archivist.domain.IndexClusterSpec
import com.zorroa.archivist.domain.IndexClusterState
import com.zorroa.archivist.repository.IndexClusterDao
import com.zorroa.archivist.repository.KPagedList
import org.elasticsearch.client.Request
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
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
     * Pings all ES clusters for their status.
     */
    fun pingAllClusters()

    /**
     * Ping the given [IndexCluster] and return true if the
     * cluster was repsonsive.
     */
    fun pingCluster(cluster: IndexCluster): Boolean

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

    var pingTimer: Timer? = null

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (ArchivistConfiguration.unittest) {
            return
        }

        // See AbstractTest for how these are setup for testsl
        createDefaultCluster()
        setupClusterPingTimer()
    }

    fun setupClusterPingTimer() {
        pingTimer = fixedRateTimer(
            name = "index-ping-timer",
            initialDelay = 3000, period = 10000
        ) {
            pingAllClusters()
        }
    }

    override fun createIndexCluster(spec: IndexClusterSpec): IndexCluster {
        return indexClusterDao.create(spec)
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

    override fun pingAllClusters() {
        for (cluster in indexClusterDao.getAll()) {
            pingCluster(cluster)
        }
    }

    override fun pingCluster(cluster: IndexCluster): Boolean {
        val client = getRestHighLevelClient(cluster)
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
