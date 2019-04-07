package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.EsClientCacheKey
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.clients.EsRestClient
import com.zorroa.common.util.Json
import org.apache.http.HttpHost
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest
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
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Manages the creation and usage of ES indexes.  Currently this implementation only supports
 * a default ES server and index, but eventually will support large customers with their
 * own dedicated ES index.
 */
interface IndexRoutingService {

    /**
     * Get an [EsRestClient] for the current users organization.
     *
     * @return: EsRestClient
     */
    fun getOrgRestClient() : EsRestClient

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
     */
    fun syncIndexRouteVersion(route: IndexRoute)

    /**
     * Apply all outstanding mapping patches to all active IndexRoutes.
     */
    fun syncAllIndexRoutes()

    fun getMinorVersionMappingFiles(mappingType: String, majorVersion: Int): List<ElasticMapping>

    fun getMajorVersionMappingFile(mappingType: String, majorVersion: Int): ElasticMapping

    fun applyMinorVersionMappingFile(route: IndexRoute, patchFile: ElasticMapping)

    fun refreshAll()
    /**
     * Setup the default index configured in application.properties.
     */
    fun setupDefaultIndexRoute()

    fun performHealthCheck() : Health
}

/**
 * Represents a versioned ElasticSearch mapping.
 */
class ElasticMapping(
        val name: String,
        val majorVersion: Int,
        val minorVersion: Int,
        val mapping: Map<String, Any>
)

@Component
class IndexRoutingServiceImpl @Autowired
    constructor(val indexRouteDao: IndexRouteDao,
                val properties: ApplicationProperties): IndexRoutingService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    lateinit var clusterLockExecutor: ClusterLockExecutor

    var esClientCache = EsClientCache()

    override fun onApplicationEvent(cre: ContextRefreshedEvent) {
        setupDefaultIndexRoute()
    }

    override fun setupDefaultIndexRoute() {
        val defaultUrl = properties.getString("archivist.index.default-url")
        indexRouteDao.updateDefaultIndexRoutes(defaultUrl)
    }

    override fun syncAllIndexRoutes() {
        indexRouteDao.getAll().forEach { syncIndexRouteVersion(it) }
    }

    override fun refreshAll() {
        indexRouteDao.getAll().forEach {
            getClusterRestClient(it).client.lowLevelClient.performRequest(
                    Request("POST", "/_refresh"))
        }
    }

    override fun syncIndexRouteVersion(route: IndexRoute) {
        val es = getClusterRestClient(route)
        waitForElasticSearch(es)

        val lock = ClusterLockSpec.softLock("create-es-${route.indexName}")
        clusterLockExecutor.inline(lock) {
            if (!es.indexExists()) {
                logger.info("Creating index: ${route.indexName}")

                val mappingFile = getMajorVersionMappingFile(
                        route.mappingType, route.mappingMajorVer)
                val req = CreateIndexRequest()
                req.index(route.indexName)
                req.source(mappingFile.mapping, DeprecationHandler.THROW_UNSUPPORTED_OPERATION)
                es.client.indices().create(req, RequestOptions.DEFAULT)
            }
            else {
                logger.info("Not creating ${route.indexUrl}, already exists")
            }

            val patches = getMinorVersionMappingFiles(route.mappingType, route.mappingMajorVer)
            for (patch in patches) {
                // Skip over patches we have.
                if (route.mappingMinorVer >= patch.minorVersion) {
                    continue
                }
                applyMinorVersionMappingFile(route, patch)
            }
        }
    }

    override fun applyMinorVersionMappingFile(route: IndexRoute, patchFile: ElasticMapping) {
        val es = getClusterRestClient(route)
        try {
            val patch = patchFile.mapping
            val request = PutMappingRequest(route.indexName)
            request.type(route.mappingType)
            request.source(Json.serializeToString(patch.getValue("patch")), XContentType.JSON)

            logger.info("Applying ES patch '{} {}.{}' - '{}' to index '{}'",
                    patchFile.name, patchFile.majorVersion, patchFile.minorVersion,
                    patch["description"], route.indexUrl)

            val response = es.client.indices().putMapping(request)
            if (response.isAcknowledged) {
                indexRouteDao.setMinorVersion(route, patchFile.minorVersion)
            }
        }
        catch (e:Exception) {
            logger.warn("Failed to apply patch: {}.{}",
                    patchFile.majorVersion, patchFile.minorVersion, e)
        }
    }

    override fun getMajorVersionMappingFile(mappingType: String, majorVersion: Int): ElasticMapping {
        val path = "db/migration/elasticsearch/V${majorVersion}__$mappingType.json"
        val resource = ClassPathResource(path)
        val mapping = Json.Mapper.readValue<Map<String, Any>>(
                resource.inputStream, Json.GENERIC_MAP)
        return ElasticMapping(mappingType, majorVersion, 0, mapping)
    }


    override fun getMinorVersionMappingFiles(mappingType: String, majorVersion: Int): List<ElasticMapping> {
        val result = mutableListOf<ElasticMapping>()
        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
        val resources = resolver.getResources("classpath:/db/migration/elasticsearch/*.json")

        for (resource in resources) {
            val matcher = MAP_PATCH_REGEX.matchEntire(resource.filename)
            matcher?.let {
                val major = it.groupValues[1].toInt()
                val minor = it.groupValues[2].toInt()
                val type = it.groupValues[3]

                if (major == majorVersion && type == mappingType) {
                    val json = Json.Mapper.readValue<Map<String, Any>>(
                            resource.inputStream, Json.GENERIC_MAP)
                    result.add(ElasticMapping(mappingType, major, minor, json))
                }
            }
        }
        result.sortWith(Comparator { o1, o2 -> Integer.compare(o2.majorVersion, o1.minorVersion) })
        return result
    }

    override fun getOrgRestClient(): EsRestClient {
        val route = indexRouteDao.getOrgRoute()
        return esClientCache.get(route.esClientCacheKey(getOrgId().toString()))
    }

    override fun getClusterRestClient(route: IndexRoute): EsRestClient {
        return esClientCache.get(route.esClientCacheKey())
    }

    fun waitForElasticSearch(client: EsRestClient) {
        while(!client.isAvailable()) {
            logger.info("Waiting for ES to be available.....{}", client.route.clusterUrl)
            Thread.sleep(1000)
        }
    }

    override fun performHealthCheck() : Health {
        for (route in indexRouteDao.getAll()) {
            if (route.closed) { continue }
            val client = getClusterRestClient(route)
            if (!client.isAvailable()) {
                return Health.down().withDetail(
                        "ElasticSearch ${route.clusterUrl }down or not ready", client.route).build()
            }

        }
        return Health.up().build()

    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRoutingServiceImpl::class.java)

        private val MAP_FILE_REGEX = Regex("^V(\\d+)__(.*?).json$")

        private val MAP_PATCH_REGEX = Regex("^V(\\d+)\\.(\\d{8})__(.*?).json$")
    }

}

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
                            RestClient.builder(HttpHost(uri.host, uri.port, uri.scheme)))
                }
            })

    fun get(route: EsClientCacheKey): EsRestClient {
        return EsRestClient(route, cache.get(route.clusterUrl))
    }
}
