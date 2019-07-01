package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.EsClientCacheKey
import com.zorroa.archivist.domain.IndexMappingVersion
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.IndexRouteFilter
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.clients.EsRestClient
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobPriority
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.apache.http.HttpHost
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the creation and usage of ES indexes.  Currently this implementation only supports
 * a default ES server and index, but eventually will support large customers with their
 * own dedicated ES index.
 *
 * Most of this thing could be broken out into a new service eventually.
 *
 * The ES migration files naming convention.
 *
 * Major Version: V<major version>__<name>.json
 * Minor Version: V<major version>.<year><month><day>__<name>.json
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
     * Get an [EsRestClient] for the current users organization.
     *
     * @return: EsRestClient
     */
    fun getOrgRestClient(): EsRestClient

    /**
     * Get an non-routed [EsRestClient] for cluster wide operations.
     *
     * @param route: The [IndexRoute] to get a client for.
     *
     * @return: EsRestClient
     */
    fun getClusterRestClient(route: IndexRoute): EsRestClient

    /**
     * Apply any outstanding mapping patches to the given [IndexRoute]
     *
     * @param route The IndexRoute to version up.
     * @return the [ElasticMapping] the route was updated to.
     */
    fun syncIndexRouteVersion(route: IndexRoute): ElasticMapping?

    /**
     * Apply all outstanding mapping patches to all active IndexRoutes.
     */
    fun syncAllIndexRoutes()

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
     * Refresh all index routes, this flushes all changes from memory to disk. This
     * is mainly used for testing, or something to run after a reindex.
     */
    fun refreshAll()

    /**
     * Setup the default index configured in application.properties.  This will update any
     * route that is marked as being in the public pool with the value of the
     * archivist.index.default-url property.
     *
     * This method will be removed as part of index routing phase 2
     */
    fun setupDefaultIndexRoute()

    /**
     * Launch a reindex job for the current authorized user's organization.  The job
     * will kill any existing reindex job.
     */
    fun launchReindexJob(): Job

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
     * Return true if the index route being used is for reindexing.
     */
    fun isReIndexRoute(): Boolean

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
     * Open the index and return True.  If the index is already open then return false.
     */
    fun openIndex(route: IndexRoute): Boolean

    /**
     * Return the ES index state as a Map
     */
    fun getEsIndexState(route: IndexRoute): Map<String, Any>
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
    val properties: ApplicationProperties
) :
    IndexRoutingService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    lateinit var clusterLockExecutor: ClusterLockExecutor

    @Autowired
    lateinit var jobService: JobService

    var esClientCache = EsClientCache()

    val migrated = AtomicBoolean(false)

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        setupDefaultIndexRoute()
        if (!ArchivistConfiguration.unittest) {
            // This is run manually by unittests we can test it.
            syncAllIndexRoutes()
        }
    }

    @Transactional
    override fun createIndexRoute(spec: IndexRouteSpec): IndexRoute {

        if (!indexMappingVersionExists(spec.mapping, spec.mappingMajorVer)) {
            throw IllegalArgumentException(
                "Failed to find index mapping ${spec.mapping} v${spec.mappingMajorVer}"
            )
        }

        // These are always false for single tenant
        if (!properties.getBoolean("archivist.organization.multiTenant")) {
            spec.useRouteKey = false
            spec.defaultPool = false
        }

        val route = indexRouteDao.create(spec)
        syncIndexRouteVersion(route)
        return route
    }

    @Transactional(readOnly = true)
    override fun getIndexRoute(id: UUID): IndexRoute {
        return indexRouteDao.get(id)
    }

    override fun getIndexMappingVersions(): List<IndexMappingVersion> {

        val result = mutableListOf<IndexMappingVersion>()
        val searchPath = listOf("classpath:/db/migration/elasticsearch")
        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)

        fun addMatch(filename: String) {
            val match = MAP_MAJOR_REGEX.matchEntire(filename)
            if (match != null) {
                val majorVersion = match.groupValues[1]
                val name = match.groupValues[2]
                result.add(IndexMappingVersion(name, majorVersion.toInt()))
            }
        }

        searchPath.forEach {
            if (it.startsWith("classpath:")) {
                val resources = resolver.getResources("$it/*.json")
                for (resource in resources) {
                    addMatch(resource.filename)
                }
            }
        }

        return result
    }

    override fun isReIndexRoute(): Boolean {
        val req = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        return req.request.getHeader("X-Zorroa-Index-Route") != null
    }

    override fun setupDefaultIndexRoute() {
        val defaultUrl = properties.getString("archivist.index.default-url")
        val defaultRoutingKey = properties.getBoolean("archivist.index.default-use-routing-key")
        indexRouteDao.updateDefaultIndexRoutes(defaultUrl, defaultRoutingKey)
    }

    override fun syncAllIndexRoutes() {
        val routes = indexRouteDao.getAll()
        logger.info("Syncing all ${routes.size} index routes.")
        routes.forEach { syncIndexRouteVersion(it) }
        migrated.set(true)
    }

    override fun refreshAll() {
        indexRouteDao.getAll().forEach {
            if (!it.closed) {
                logger.info("refreshing index route ${it.indexUrl}")
                val req = Request("POST", "/_refresh")
                val client = getClusterRestClient(it).client.lowLevelClient
                client.performRequest(req)
            }
        }
    }

    override fun syncIndexRouteVersion(route: IndexRoute): ElasticMapping? {
        var result: ElasticMapping? = null

        val es = getClusterRestClient(route)
        waitForElasticSearch(es)

        val lock = ClusterLockSpec.softLock("create-es-${route.id}")
        clusterLockExecutor.inline(lock) {

            val indexExisted = es.indexExists()
            if (!indexExisted) {
                logger.info(
                    "Creating index:" +
                        "type: '${route.mapping}'  index: '${route.indexName}' " +
                        "ver: '${route.mappingMajorVer}'" +
                        "shards: '${route.shards}' replicas: '${route.replicas}'"
                )

                val mappingFile = getMajorVersionMappingFile(
                    route.mapping, route.mappingMajorVer
                )

                val mapping = Document(mappingFile.mapping)
                mapping.setAttr("settings.index.number_of_shards", route.shards)
                mapping.setAttr("settings.index.number_of_replicas", route.replicas)

                val req = CreateIndexRequest()
                req.index(route.indexName)
                req.source(mapping.document, DeprecationHandler.THROW_UNSUPPORTED_OPERATION)
                es.client.indices().create(req, RequestOptions.DEFAULT)
                result = mappingFile
            } else {
                logger.info("Not creating ${route.indexUrl}, already exists")
            }

            val patches = getMinorVersionMappingFiles(route.mapping, route.mappingMajorVer)
            for (patch in patches) {
                /**
                 * If the index already existed, then only apply new patches. If
                 * the index was just created, then apply all patches.
                 */
                if (indexExisted && route.mappingMinorVer >= patch.minorVersion) {
                    continue
                }
                applyMinorVersionMappingFile(route, patch)
                result = patch
            }
        }

        return result
    }

    override fun getMajorVersionMappingFile(mappingType: String, majorVersion: Int): ElasticMapping {
        val path = "db/migration/elasticsearch/V${majorVersion}__$mappingType.json"
        val resource = ClassPathResource(path)
        val mapping = Json.Mapper.readValue<MutableMap<String, Any>>(
            resource.inputStream, Json.GENERIC_MAP
        )
        return ElasticMapping(mappingType, majorVersion, 0, mapping)
    }

    override fun launchReindexJob(): Job {
        val name = "Reindexing All Assets"
        val script = ZpsScript(
            name,
            type = PipelineType.Import,
            settings = mutableMapOf("inline" to true),
            over = listOf(),
            execute = listOf(),
            generate = listOf(
                ProcessorRef(
                    "zplugins.core.generators.AssetSearchGenerator",
                    mapOf("search" to mapOf<String, Any>())
                )
            )
        )

        val spec = JobSpec(
            name,
            script,
            priority = JobPriority.Reindex,
            replace = true,
            paused = true,
            pauseDurationSeconds = REINDEX_JOB_DELAY_SEC
        )

        return jobService.create(spec, PipelineType.Import)
    }

    override fun getMinorVersionMappingFiles(mappingType: String, majorVersion: Int): List<ElasticMapping> {
        val result = mutableListOf<ElasticMapping>()
        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
        val resources = resolver.getResources("classpath*:/db/migration/elasticsearch/*.json")

        for (resource in resources) {
            val matcher = MAP_PATCH_REGEX.matchEntire(resource.filename)
            matcher?.let {
                val major = it.groupValues[1].toInt()
                val minor = it.groupValues[2].toInt()
                val type = it.groupValues[3]

                if (major == majorVersion && type == mappingType) {
                    val json = Json.Mapper.readValue<MutableMap<String, Any>>(
                        resource.inputStream, Json.GENERIC_MAP
                    )
                    result.add(ElasticMapping(mappingType, major, minor, json))
                }
            }
        }

        result.sortWith(Comparator { o1, o2 -> Integer.compare(o1.minorVersion, o2.minorVersion) })
        logger.info("mapping '$mappingType v$majorVersion has ${result.size} available patches.")
        return result
    }

    override fun getOrgRestClient(): EsRestClient {

        val req = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
        val routeId = req?.request?.getHeader("X-Zorroa-Index-Route")

        val route = if (routeId != null) {
            indexRouteDao.get(UUID.fromString(routeId))
        } else {
            indexRouteDao.getOrgRoute()
        }

        return esClientCache.get(route.esClientCacheKey(getOrgId().toString()))
    }

    override fun getClusterRestClient(route: IndexRoute): EsRestClient {
        return esClientCache.get(route.esClientCacheKey())
    }

    fun waitForElasticSearch(client: EsRestClient) {
        while (!client.isAvailable()) {
            logger.info("Waiting for ES to be available.....{}", client.route.clusterUrl)
            Thread.sleep(1000)
        }
    }

    override fun performHealthCheck(): Health {
        if (!migrated.get()) {
            return Health.down().withDetail(
                "ElasticSearch routes have not been migrated", migrated
            ).build()
        }
        for (route in indexRouteDao.getAll()) {
            if (route.closed) {
                continue
            }
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
            request.type("asset")
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
        val rsp = getClusterRestClient(route).client.indices()
            .close(CloseIndexRequest(route.indexName), RequestOptions.DEFAULT)
        if (rsp.isAcknowledged) {
            return indexRouteDao.setClosed(route, true)
        }
        return false
    }

    override fun openIndex(route: IndexRoute): Boolean {
        val rsp = getClusterRestClient(route).client.indices()
            .open(OpenIndexRequest(route.indexName), RequestOptions.DEFAULT)
        if (rsp.isAcknowledged) {
            return indexRouteDao.setClosed(route, false)
        }
        return false
    }

    override fun getEsIndexState(route: IndexRoute): Map<String, Any> {
        val client = getClusterRestClient(route).client
        val req = Request("GET", "/_cat/indices/${route.indexName}?format=json")
        val list = Json.Mapper.readValue<List<Map<String, Any>>>(
            client.lowLevelClient.performRequest(req).entity.content
        )
        return list[0]
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRoutingServiceImpl::class.java)

        /**
         * Number of seconds to delay a reindex job, which allows users to make more selections
         * which might kick off another reindex job to happen.
         */
        const val REINDEX_JOB_DELAY_SEC = 20L

        private val MAP_MAJOR_REGEX = Regex("^V(\\d+)__(.*?).json$")

        private val MAP_PATCH_REGEX = Regex("^V(\\d+)\\.(\\d{8})__(.*?).json$")
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
