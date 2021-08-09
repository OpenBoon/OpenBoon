package boonai.archivist.service

import boonai.archivist.clients.EsRestClient
import boonai.archivist.config.ApplicationProperties
import boonai.archivist.domain.Asset
import boonai.archivist.domain.EsClientCacheKey
import boonai.archivist.domain.IndexCluster
import boonai.archivist.domain.IndexMappingVersion
import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.IndexRouteSimpleSpec
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexRouteState
import boonai.archivist.domain.Project
import boonai.archivist.repository.IndexClusterDao
import boonai.archivist.repository.IndexRouteDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.ProjectCustomDao
import boonai.archivist.repository.ProjectDao
import boonai.archivist.security.getProjectId
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.storage.SystemStorageService
import boonai.common.util.Json
import boonai.common.util.readValueOrNull
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import org.apache.http.HttpHost
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CloseIndexRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.Comparator

/**
 * Manages the creation and usage of ES indexes.  Currently this implementation only supports
 * a default ES server and index, but eventually will support large customers with their
 * own dedicated ES index.
 *
 * Most of this thing could be broken out into a new service eventually.
 *
 * The ES migration files naming convention.
 *
 * Major Version: <name>.v<major version>.json
 * Minor Version: <name>.<major version>-<year><month><day>.json
 *
 * Updating a major version will always kick off a reindex.
 *
 */
interface IndexRoutingService {

    /**
     * Create a new [IndexRoute] and return it.
     */
    fun createIndexRoute(spec: IndexRouteSpec): IndexRoute

    /**
     * A simple form of index route creation.
     */
    fun createIndexRoute(spec: IndexRouteSimpleSpec): IndexRoute

    /**
     * Get an [EsRestClient] for the current users project.
     *
     * @return: EsRestClient
     */
    fun getProjectRestClient(): EsRestClient

    /**
     * Get an non-routed [EsRestClient] for cluster wide operations.
     *
     * @param route: The [IndexRoute] to get a client for.
     *
     * @return: EsRestClient
     */
    fun getClusterRestClient(route: IndexRoute): EsRestClient

    /**
     * Get an non-routed [EsRestClient] for cluster wide operations.
     *
     * @param routeId: The [UUID] to get a client for.
     *
     * @return: EsRestClient
     */
    fun getClusterRestClient(routeId: UUID): EsRestClient

    /**
     * Apply any outstanding mapping patches to the given [IndexRoute]
     *
     * @param route The IndexRoute to version up.
     * @return the [ElasticMapping] the route was update to.
     */
    fun syncIndexRouteVersion(route: IndexRoute): ElasticMapping?

    /**
     * Apply all outstanding mapping patches to all active IndexRoutes.
     */
    fun syncAllIndexRoutes()

    /**
     * Apply all outstanding mapping patches to all active IndexRoutes for the cluster.
     */
    fun syncAllIndexRoutes(cluster: IndexCluster)

    /**
     * Return a list of all [ElasticMapping] patches for the given mappingType and major version.
     *
     * @param mappingType The type of mapping.
     *
     */
    fun getMinorVersionMappingFiles(mappingType: String, majorVersion: Int): List<ElasticMapping>

    /**
     * Return the [ElasticMapping] for the given type and major version.
     *
     * @param mappingType The type of mapping.
     * @param majorVersion The major version of the mapping.
     */
    fun getMajorVersionMappingFile(mappingType: String, majorVersion: Int): ElasticMapping

    /**
     * Perform a health check on all active [IndexRoute]s
     */
    fun performHealthCheck(): Health

    /**
     * Return a list of available ES index mappings.
     */
    fun getIndexMappingVersions(): List<IndexMappingVersion>

    /**
     * Return a given [IndexRoute] by its unique Id.
     */
    fun getIndexRoute(id: UUID): IndexRoute

    /**
     * Return all [IndexRoute]s that match the [IndexRouteFilter]
     */
    fun getAll(filter: IndexRouteFilter): KPagedList<IndexRoute>

    /**
     * Return an [IndexRoute] that matches the [IndexRouteFilter].  If the filter does
     * not return one and only one [IndexRoute] then an exception is raised.
     */
    fun findOne(filter: IndexRouteFilter): IndexRoute

    /**
     * Close the index and return True.  If the index is already closed then return false.
     */
    fun closeIndex(route: IndexRoute): Boolean

    /**
     * Close indexes grouping by clusters
     */
    fun batchCloseIndex(routes: List<IndexRoute>)

    /**
     * Reopen the index
     */
    fun openIndex(route: IndexRoute): Boolean

    /**
     * Return the ES index state as a Map
     */
    fun getEsIndexState(route: IndexRoute): Map<String, Any>

    /**
     * Delete the given IndexRoute, must be closed first.
     */
    fun deleteIndex(route: IndexRoute, force: Boolean = false): Boolean

    /**
     * Delete a batch of IndexRoute grouping by cluster
     */
    fun batchDeleteIndex(routes: List<IndexRoute>): Boolean

    /**
     * Close and delete the given index.
     */
    fun closeAndDeleteIndex(route: IndexRoute): Boolean

    /**
     * Close and delete all Indexes of a Project.
     */
    fun closeAndDeleteProjectIndexes(projectUUID: UUID)

    /**
     * Sets the index refresh interval. Setting to -1 disables refreshing, this is used
     * to speed up reindexing.
     */
    fun setIndexRefreshInterval(route: IndexRoute, interval: String): Boolean

    /**
     * Reset the project index route.
     */
    fun setIndexRoute(project: Project, route: IndexRoute): Boolean

    /**
     * Invalidate route cache.
     */
    fun invalidateCache()
}

/**
 * Represents a versioned ElasticSearch mapping.
 */
class ElasticMapping(
    val name: String,
    val majorVersion: Int,
    val minorVersion: Int,
    val mapping: MutableMap<String, Any>
)

@Component
class IndexRoutingServiceImpl @Autowired
constructor(
    val indexRouteDao: IndexRouteDao,
    val indexClusterDao: IndexClusterDao,
    val systemStorageService: SystemStorageService,
    val projectCustomDao: ProjectCustomDao,
    val projectDao: ProjectDao,
    val properties: ApplicationProperties,
    val txEvent: TransactionEventManager,
    val jedis: JedisPool
) : IndexRoutingService {

    @Autowired
    lateinit var esClientCache: EsClientCache

    @Autowired
    lateinit var indexMappingService: IndexMappingService

    @Transactional
    override fun createIndexRoute(spec: IndexRouteSimpleSpec): IndexRoute {
        val newSpec = IndexRouteSpec(
            properties.getString("archivist.es.default-mapping-type"),
            properties.getInt("archivist.es.default-mapping-version"),
            shards = spec.size.shards,
            replicas = spec.size.replicas,
            projectId = spec.projectId
        )
        return createIndexRoute(newSpec)
    }

    @Transactional
    override fun createIndexRoute(spec: IndexRouteSpec): IndexRoute {

        if (!indexMappingVersionExists(spec.mapping, spec.majorVer)) {
            throw IllegalArgumentException(
                "Failed to find index mapping ${spec.mapping} v${spec.majorVer}"
            )
        }

        // If no cluster ID was specified, find a ES cluster to use.
        val cluster = if (spec.clusterId != null) {
            indexClusterDao.get(spec.clusterId as UUID)
        } else {
            indexClusterDao.getNextAutoPoolCluster()
        }

        spec.clusterId = cluster.id
        val route = indexRouteDao.create(spec)

        logger.event(
            LogObject.INDEX_ROUTE, LogAction.CREATE,
            mapOf(
                "indexRouteId" to route.id,
                "indexRouteName" to route.indexName
            )
        )

        // Makes the ES Index after its committed to db.
        txEvent.afterCommit {
            syncIndexRouteVersion(route)
        }
        return route
    }

    @Transactional(readOnly = true)
    override fun getIndexRoute(id: UUID): IndexRoute {
        return indexRouteDao.get(id)
    }

    override fun getIndexMappingVersions(): List<IndexMappingVersion> {

        val result = mutableListOf<IndexMappingVersion>()
        val searchPath = listOf("classpath*:/db/migration/elasticsearch")
        val resolver = PathMatchingResourcePatternResolver()

        fun addMatch(filename: String) {
            val match = MAP_MAJOR_REGEX.matchEntire(filename)
            if (match != null) {
                val name = match.groupValues[1]
                val majorVersion = match.groupValues[2]
                result.add(IndexMappingVersion(name, majorVersion.toInt()))
            }
        }

        searchPath.forEach {
            val resources = resolver.getResources("$it/*.json")
            for (resource in resources) {
                addMatch(resource.filename)
            }
        }
        return result
    }

    override fun syncAllIndexRoutes() {
        val routes = indexRouteDao.getOpen()
        logger.info("Syncing all ${routes.size} index routes.")
        routes.forEach { syncIndexRouteVersion(it) }
    }

    override fun syncAllIndexRoutes(cluster: IndexCluster) {
        val routes = indexRouteDao.getOpen(cluster)
        logger.info("Syncing ${routes.size} index routes for ${cluster.url}")
        routes.forEach { syncIndexRouteVersion(it) }
    }

    override fun syncIndexRouteVersion(route: IndexRoute): ElasticMapping? {
        var result: ElasticMapping? = null

        val es = getClusterRestClient(route)
        waitForElasticSearch(es)

        // TODO: cluster lock

        val indexExisted = es.indexExists()
        if (!indexExisted) {
            logger.info(
                "Creating index:" +
                    "type: '${route.mapping}'  index: '${route.indexName}' " +
                    "ver: '${route.majorVer}'" +
                    "shards: '${route.shards}' replicas: '${route.replicas}'"
            )

            val mappingFile = getMajorVersionMappingFile(
                route.mapping, route.majorVer
            )

            val mapping = Asset(mappingFile.mapping)
            mapping.setAttr("settings.index.number_of_shards", route.shards)
            mapping.setAttr("settings.index.number_of_replicas", route.replicas)

            val req = CreateIndexRequest(route.indexName)
            req.source(mapping.document)
            es.client.indices().create(req, RequestOptions.DEFAULT)

            result = mappingFile
        } else {
            logger.info("Not creating ${route.indexUrl}, already exists")
        }

        val patches = getMinorVersionMappingFiles(route.mapping, route.majorVer)
        for (patch in patches) {
            /**
             * If the index already existed, then only apply new patches. If
             * the index was just created, then apply all patches.
             */
            if (indexExisted && route.minorVer >= patch.minorVersion) {
                continue
            }
            applyMinorVersionMappingFile(route, patch)
            result = patch
        }

        // Add custom fields after the patches.
        if (!indexExisted) {
            // Add all custom fields to the new index.
            indexMappingService.addAllFieldsToIndex(route)
        }

        return result
    }

    override fun getMajorVersionMappingFile(mappingType: String, majorVersion: Int): ElasticMapping {
        val path = "db/migration/elasticsearch/$mappingType.v$majorVersion.json"
        val resource = ClassPathResource(path)
        val mapping = Json.Mapper.readValue<MutableMap<String, Any>>(resource.inputStream)
        return ElasticMapping(mappingType, majorVersion, 0, mapping)
    }

    override fun getMinorVersionMappingFiles(mappingType: String, majorVersion: Int): List<ElasticMapping> {
        val result = mutableListOf<ElasticMapping>()
        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
        val resources = resolver.getResources("classpath*:/db/migration/elasticsearch/*.json")

        for (resource in resources) {
            val matcher = MAP_PATCH_REGEX.matchEntire(resource.filename)
            matcher?.let {
                val type = it.groupValues[1]
                val major = it.groupValues[2].toInt()
                val minor = it.groupValues[3].toInt()

                if (major == majorVersion && type == mappingType) {
                    val json = Json.Mapper.readValue<MutableMap<String, Any>>(
                        resource.inputStream
                    )
                    result.add(ElasticMapping(mappingType, major, minor, json))
                }
            }
        }

        result.sortWith(Comparator { o1, o2 -> Integer.compare(o1.minorVersion, o2.minorVersion) })
        logger.info("mapping '$mappingType v$majorVersion has ${result.size} available patches.")
        return result
    }

    override fun getProjectRestClient(): EsRestClient {
        val route = jedis.resource.use { cache ->
            val projectId = getProjectId().toString()

            Json.Mapper.readValueOrNull(cache.get(projectId)) ?: {
                val freshCopy = indexRouteDao.getProjectRoute()
                cache.set(
                    freshCopy.redisCacheKey(), Json.serializeToString(freshCopy),
                    SetParams().px(TimeUnit.HOURS.toMillis(1))
                )
                freshCopy
            }()
        }
        return esClientCache.get(route.esClientCacheKey())
    }

    override fun getClusterRestClient(route: IndexRoute): EsRestClient {
        return esClientCache.get(route.esClientCacheKey())
    }

    override fun getClusterRestClient(routeId: UUID): EsRestClient {
        return getClusterRestClient(indexRouteDao.get(routeId))
    }

    fun waitForElasticSearch(client: EsRestClient) {
        while (!client.isAvailable()) {
            logger.info("Waiting for ES to be available.....{}", client.route.clusterUrl)
            Thread.sleep(1000)
        }
    }

    override fun performHealthCheck(): Health {
        for (route in indexRouteDao.getAll()) {
            val client = getClusterRestClient(route)
            if (!client.isAvailable()) {
                return Health.down().withDetail(
                    "ElasticSearch ${route.clusterUrl}down or not ready", client.route
                ).build()
            }
        }
        return Health.up().build()
    }

    /**
     * Return true if the given mapping and major version exists.
     */
    private fun indexMappingVersionExists(mapping: String, majorVersion: Int): Boolean {
        return getIndexMappingVersions().find { it.mapping == mapping && it.mappingMajorVer == majorVersion } != null
    }

    /**
     * Apply the given [ElasticMapping] file to the [IndexRoute].  This is a private function
     * because only the syncIndexRouteVersion() function should call this method.
     * This should only be called from within the context of syncing index routes.
     *
     * No version checking is done to ensure the patch file isn't already applied.
     */
    private fun applyMinorVersionMappingFile(route: IndexRoute, patchFile: ElasticMapping) {
        val es = getClusterRestClient(route)
        try {
            val patch = patchFile.mapping
            val request = PutMappingRequest(route.indexName)
            request.source(Json.serializeToString(patch.getValue("patch")), XContentType.JSON)

            logger.info(
                "Applying ES patch '{} {}.{}' - '{}' to index '{}'",
                patchFile.name, patchFile.majorVersion, patchFile.minorVersion,
                patch["description"], route.indexUrl
            )

            val response = es.client.indices().putMapping(request, RequestOptions.DEFAULT)
            if (response.isAcknowledged) {
                indexRouteDao.setMinorVersion(route, patchFile.minorVersion)
            } else {
                indexRouteDao.setErrorVersion(route, patchFile.minorVersion)
                throw RuntimeException("ES server did not ack patch.")
            }
        } catch (e: Exception) {
            logger.warn(
                "Failed to apply patch: {}.{}",
                patchFile.majorVersion, patchFile.minorVersion, e
            )
            indexRouteDao.setErrorVersion(route, patchFile.minorVersion)
        }
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: IndexRouteFilter): KPagedList<IndexRoute> {
        return indexRouteDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: IndexRouteFilter): IndexRoute {
        return indexRouteDao.findOne(filter)
    }

    override fun closeIndex(route: IndexRoute): Boolean {
        return try {
            val rsp = getClusterRestClient(route).client.indices()
                .close(CloseIndexRequest(route.indexName), RequestOptions.DEFAULT)

            if (rsp.isAcknowledged) {
                indexRouteDao.setState(route, IndexRouteState.CLOSED)
                logger.event(
                    LogObject.INDEX_ROUTE, LogAction.STATE_CHANGE,
                    mapOf(
                        "indexRouteId" to route.id,
                        "indexRouteName" to route.indexName,
                        "indexRouteState" to IndexRouteState.CLOSED.name
                    )
                )
            }
            return rsp.isAcknowledged
        } catch (e: ElasticsearchStatusException) {
            logger.warn("Failed to close index: ${route.indexUrl}", e)
            false
        }
    }

    override fun batchCloseIndex(routes: List<IndexRoute>) {

        routes.groupBy {
            it.clusterUrl
        }
            .forEach {

                val indexMapNameIndex = it.value.map { i -> i.indexName to i.id }.toMap()
                val rsp = esClientCache
                    .getRestHighLevelClient(it.key)
                    .indices()
                    .close(
                        CloseIndexRequest(*indexMapNameIndex.keys.toTypedArray()),
                        RequestOptions.DEFAULT
                    )

                if (rsp.isAcknowledged) {
                    rsp.indices.forEach { indexResult ->
                        if (!indexResult.hasFailures()) {
                            indexMapNameIndex[indexResult.index]?.let { uuid ->
                                indexRouteDao.setState(uuid, IndexRouteState.CLOSED)
                                logger.event(
                                    LogObject.INDEX_ROUTE, LogAction.STATE_CHANGE,
                                    mapOf(
                                        "indexRouteId" to uuid,
                                        "indexRouteState" to IndexRouteState.CLOSED.name
                                    )
                                )
                            }
                        }
                    }
                }
            }
    }

    override fun batchDeleteIndex(routes: List<IndexRoute>): Boolean {

        routes.groupBy {
            it.clusterUrl
        }.forEach {

            val indexMapNameIndex = it.value.map { i -> i.indexName to i.id }.toMap()
            val rsp = esClientCache
                .getRestHighLevelClient(it.key)
                .indices()
                .delete(
                    DeleteIndexRequest(*indexMapNameIndex.keys.toTypedArray()),
                    RequestOptions.DEFAULT
                )

            if (rsp.isAcknowledged) {
                it.value.forEach { indexRoute ->
                    logger.event(
                        LogObject.INDEX_ROUTE, LogAction.DELETE,
                        mapOf(
                            "indexRouteId" to indexRoute.id,
                            "indexRouteName" to indexRoute.indexName
                        )
                    )
                    indexRouteDao.delete(indexRoute)
                    deleteIndexBackups(indexRoute)
                }
            }
        }
        return true
    }

    private fun deleteIndexBackups(indexRoute: IndexRoute) {
        systemStorageService.recursiveDelete("index-clusters/${indexRoute.id}")
    }

    override fun openIndex(route: IndexRoute): Boolean {
        val rsp = getClusterRestClient(route).client.indices()
            .open(OpenIndexRequest(route.indexName), RequestOptions.DEFAULT)
        if (rsp.isAcknowledged) {
            indexRouteDao.setState(route, IndexRouteState.READY)
            logger.event(
                LogObject.INDEX_ROUTE, LogAction.STATE_CHANGE,
                mapOf(
                    "indexRouteId" to route.id,
                    "indexRouteName" to route.indexName,
                    "indexRouteState" to IndexRouteState.READY.name
                )
            )
        }
        return rsp.isAcknowledged
    }

    override fun setIndexRefreshInterval(route: IndexRoute, interval: String): Boolean {
        val request = UpdateSettingsRequest(route.indexName)
        val settings: Settings = Settings.builder()
            .put("index.refresh_interval", interval)
            .build()
        request.settings(settings)
        val rest = getClusterRestClient(route)
        logger.info("Set index $route refresh to $interval")
        return rest.client.indices().putSettings(request, RequestOptions.DEFAULT).isAcknowledged
    }

    override fun deleteIndex(route: IndexRoute, force: Boolean): Boolean {
        val uroute = indexRouteDao.get(route.id)
        if (uroute.state == IndexRouteState.READY) {
            throw IllegalArgumentException("The index must be closed before it can be deleted.")
        }

        val proj = projectDao.getOne(route.projectId)
        if (proj.indexRouteId == route.id) {
            throw IllegalArgumentException("You can't delete the active index for a project.")
        }

        val rsp = getClusterRestClient(route).client.indices()
            .delete(DeleteIndexRequest(route.indexName), RequestOptions.DEFAULT)
        if (rsp.isAcknowledged) {
            logger.event(
                LogObject.INDEX_ROUTE, LogAction.DELETE,
                mapOf(
                    "indexRouteId" to route.id,
                    "indexRouteName" to route.indexName
                )
            )
            return indexRouteDao.delete(route)
        }
        return false
    }

    override fun closeAndDeleteProjectIndexes(projectUUID: UUID) {
        val indexes = getAll(IndexRouteFilter(projectIds = listOf(projectUUID))).toList()
        batchCloseIndex(indexes)
        batchDeleteIndex(indexes)
    }

    override fun closeAndDeleteIndex(route: IndexRoute): Boolean {
        closeIndex(route)
        return deleteIndex(route, force = true)
    }

    override fun getEsIndexState(route: IndexRoute): Map<String, Any> {
        val client = getClusterRestClient(route).client
        val req = Request("GET", "/_cat/indices/${route.indexName}?format=json")
        val list = Json.Mapper.readValue<List<Map<String, Any>>>(
            client.lowLevelClient.performRequest(req).entity.content
        )
        return list[0]
    }

    @Transactional
    override fun setIndexRoute(project: Project, route: IndexRoute): Boolean {
        if (project.id != route.projectId) {
            throw IllegalStateException("The index route does not belong to this project")
        }

        return if (projectCustomDao.updateIndexRoute(project.id, route)) {
            logger.event(
                LogObject.PROJECT, LogAction.UPDATE,
                mapOf(
                    "projectId" to project.id,
                    "oldIndexRoute" to project.indexRouteId,
                    "newIndexRoute" to route.id
                )
            )

            openIndex(route)

            // Make sure this comes after the
            txEvent.afterCommit {
                jedis.resource.use {
                    it.del(route.redisCacheKey())
                }
            }
            true
        } else {
            logger.warn("Failed to set new index route for project ${project.id}, likely already set to same index.")
            false
        }
    }

    override fun invalidateCache() {
        jedis.resource.use {
            it.flushAll()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRoutingServiceImpl::class.java)

        // Matches major version file foo.v1.json
        private val MAP_MAJOR_REGEX = Regex("^(.*?).v(\\d+).json$")

        // Matches minor version file foo.v1-02020202.json
        private val MAP_PATCH_REGEX = Regex("^(.*?).v(\\d+)-(\\d{8}).json$")
    }
}

/**
 * Caches one client per cluster URL.
 */
class EsClientCache {

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(20)
        .initialCapacity(5)
        .concurrencyLevel(1)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .removalListener<String, RestHighLevelClient> {
            try {
                it.value.close()
            } catch (e: Exception) {
            }
        }
        .build(object : CacheLoader<String, RestHighLevelClient>() {
            @Throws(Exception::class)
            override fun load(clusterUrl: String): RestHighLevelClient {
                val uri = URI.create(clusterUrl)
                return RestHighLevelClient(
                    RestClient.builder(HttpHost(uri.host, uri.port, uri.scheme))
                )
            }
        })

    /**
     * Return an [EsRestClient] instance for the given [EsClientCacheKey]
     */
    fun get(route: EsClientCacheKey): EsRestClient {
        return EsRestClient(route, cache.get(route.clusterUrl))
    }

    fun getRestHighLevelClient(url: String): RestHighLevelClient {
        return cache.get(url)
    }

    fun getRestHighLevelClient(cluster: IndexCluster): RestHighLevelClient {
        return cache.get(cluster.url)
    }

    /**
     * Invalidate the [EsRestClient] for the given [EsClientCacheKey]
     */
    fun invalidate(route: EsClientCacheKey) {
        cache.invalidate(route.clusterUrl)
    }

    /**
     * Invalidate all [EsRestClient] entries.
     */
    fun invalidateAll() {
        cache.invalidateAll()
    }
}
