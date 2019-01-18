package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.common.clients.ElasticMapping
import com.zorroa.common.clients.EsRestClient
import com.zorroa.common.clients.IndexRoute
import com.zorroa.common.util.Json
import org.apache.http.HttpHost
import org.elasticsearch.action.admin.indices.alias.Alias
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
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
     * Get an EsRestClient for a given Org Id.
     *
     * @param orgId: The UUID of the org.
     * @return: EsRestClient
     */
    operator fun get(orgId: UUID): EsRestClient

    /**
     * Get an EsRestClient for a given Org Id.
     *
     * @param orgId: The UUID of the org.
     * @return: EsRestClient
     */
    fun getEsRestClient(orgId: UUID) : EsRestClient

    /**
     * Get an EsRestClient for a given IndexRoute
     *
     * @param route: The IndexRoute to get a client for.
     * @return: EsRestClient
     */
    fun getEsRestClient(route: IndexRoute): EsRestClient

    /**
     * Return a route for the given ES mapping file using the default cluster.
     *
     * @parm mapping: An ElasticMapping file named in the V<version>__<name>.json format.
     * @return IndexRoute
     */
    fun getIndexRoute(mapping: ElasticMapping): IndexRoute

    /**
     * Return te current index name.
     *
     * @param mapping: The ES mapping
     * @return the index name
     */
    fun getIndexName(mapping: ElasticMapping) : String

    /**
     * Create the default index using the latest highest mapping version found.
     */
    fun setupDefaultIndex() : IndexRoute

    /**
     * Create an index using the given mapping file and cluster url.
     *
     * @param clusterUrl: The url to the cluster
     * @param mapfile: A parsed ES mapping file
     * @return: An IndexRoute to the new index.
     */
    fun createIndex(clusterUrl: String, mapfile: ElasticMapping) : IndexRoute

}

@Configuration
@ConfigurationProperties("archivist.index")
class ElasticSearchConfiguration {

    var autoCreateIndex: Boolean = false
    var shards: Int = 5
    var replicas: Int = 2
    lateinit var defaultUrl : String
    lateinit var indexName: String
}

@Component
class IndexRoutingServiceImpl @Autowired
    constructor(val config: ElasticSearchConfiguration): IndexRoutingService, ApplicationListener<ContextRefreshedEvent> {

    var esClientCache = EsClientCache()

    val defaultRoute: IndexRoute

    val defaultMapFile : ElasticMapping

    init {
        defaultMapFile = getEmbeddedMappingVersion()
        defaultRoute = getIndexRoute(defaultMapFile)
    }

    override fun onApplicationEvent(cre: ContextRefreshedEvent) {

        if (!config.autoCreateIndex) {
            logger.info("Not auto-creating ES index: disabled")
            return
        }

        setupDefaultIndex()
    }

    override fun setupDefaultIndex() : IndexRoute {
        return createIndex(config.defaultUrl, defaultMapFile)
    }

    override fun createIndex(clusterUrl: String, mapfile: ElasticMapping) : IndexRoute {
        val route = IndexRoute(clusterUrl, getIndexName(mapfile))
        val es = getEsRestClient(route)
        waitForElasticSearch(es)

        // Check if index exists already
        if (es.indexExists()) {
            logger.info("Not creating index already exists")
            return route
        }

        val indexName = getIndexName(mapfile)
        logger.info("Creating index '$indexName'")

        // This code still has to handle multiple archivists attempting to do this at the same time.
        val req = CreateIndexRequest()
        req.index(indexName)

        val settings = mapfile.mapping["settings"] as MutableMap<String, Any>?
        if (settings != null) {
            settings["number_of_replicas"] = config.replicas
            settings["number_of_shards"] = config.shards
        }
        req.source(mapfile.mapping)
        es.client.indices().create(req)
        return route
    }

    override fun getIndexRoute(mapping: ElasticMapping) : IndexRoute {
        return IndexRoute(config.defaultUrl, getIndexName(mapping), mapping.alias)
    }

    override fun getIndexName(mapping: ElasticMapping) : String {
        return if (config.indexName == "auto") {
            mapping.indexName
        }
        else {
            config.indexName
        }
    }

    /**
     * Get the latest ES Mapping for Assets.
     */
    fun getEmbeddedMappingVersion(): ElasticMapping {

        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
        val resources = resolver.getResources("classpath:/db/migration/assets/*.json")
        val allVersions = mutableListOf<ElasticMapping>()

        for (resource in resources) {
            val matcher = MAP_FILE_REGEX.matchEntire(resource.filename)
            matcher?.let {
                val version = it.groupValues[1].toInt()
                val name = it.groupValues[2]
                val mapping = Json.Mapper.readValue<Map<String, Any>>(
                        resource.inputStream, Json.GENERIC_MAP)

                logger.info("Found embedded mapping in {} version {}", resource.filename, version)
                val esmapping = ElasticMapping(name, version, mapping)
                allVersions.add(esmapping)
            }
        }

        allVersions.sortWith(Comparator { o1, o2 -> Integer.compare(o2.version, o1.version) })
        return allVersions[0]
    }

    override operator fun get(orgId: UUID): EsRestClient {
        return getEsRestClient(orgId)
    }

    override fun getEsRestClient(orgId: UUID): EsRestClient {
        /**
         * Eventually this implementation should handle custom org ES servers.  For
         * now this assumes a single ES cluster, single index, single alias.
         */

        return esClientCache.get(defaultRoute.withKey(orgId))
    }

    override fun getEsRestClient(route: IndexRoute): EsRestClient {
        return esClientCache.get(route)
    }

    fun waitForElasticSearch(client: EsRestClient) {
        while(!client.isAvailable()) {
            logger.info("Waiting for ES to be available.....")
            Thread.sleep(1000)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRoutingServiceImpl::class.java)

        private val MAP_FILE_REGEX = Regex("^V(\\d+)__(.*?).json$")
    }

}

class EsClientCache {

    private val cache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .initialCapacity(5)
            .concurrencyLevel(2)
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

    fun get(route: IndexRoute): EsRestClient {
        return EsRestClient(route, cache.get(route.clusterUrl))
    }
}
