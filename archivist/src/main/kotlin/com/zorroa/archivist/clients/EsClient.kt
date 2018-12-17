package com.zorroa.common.clients

import com.zorroa.archivist.service.IndexRoutingServiceImpl
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.io.IOException
import java.util.*

/**
 * An IndexRoute contains all the properties needed to route an organization's
 * ES requests to the right cluster, index, and shards.
 */
class IndexRoute (
        val clusterUrl: String,
        val indexName: String,
        val routingKey : String?=null
)
{
    val indexUrl = "$clusterUrl/$indexName"

    fun withKey(key: UUID) : IndexRoute {
        return IndexRoute(clusterUrl, indexName, key.toString())
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val route = o as IndexRoute
        return indexUrl == route.indexUrl
    }

    override fun hashCode(): Int {
        return Objects.hash(indexUrl)
    }
}

/**
 * Represents a versioned ElasticSearch mapping.
 */
class ElasticMapping(
        val name: String,
        val version: Int,
        val mapping: Map<String, Any>
)
{
    val indexName = "${name}_v$version"
    val alias = name
}

/**
 * ES 6.x took away the SearchBuilder class which was a convenient way to
 * build a search request.  This is similar class that just holds
 * a SearchRequest and a SearchSourceBuilder (the query)
 *
 */
class SearchBuilder {

    val request: SearchRequest
    val source: SearchSourceBuilder

    constructor() {
        this.request = SearchRequest()
        this.source = SearchSourceBuilder()
        this.request.source(source)
    }

    constructor(request: SearchRequest) {
        this.request = request
        this.source = SearchSourceBuilder()
        this.request.source(source)
    }

    constructor(request: SearchRequest, source: SearchSourceBuilder) {
        this.request = request
        this.source = source
        this.request.source(source)
    }
}

/**
 * EsRestClient is used for building pre-rerouted ES requests based on the
 * organization's routing settings.
 */
class EsRestClient(val route: IndexRoute, val client: RestHighLevelClient) {

    fun newSearchRequest() : SearchRequest {
        return SearchRequest(route.indexName)
                .apply {
                    if (route.routingKey != null) { this.routing(route.routingKey) }
                }
    }

    fun newSearchBuilder() : SearchBuilder {
        val builder = SearchBuilder()
        routeSearchRequest(builder.request)
        return builder
    }

    fun newSearchBuilder(req: SearchRequest, source: SearchSourceBuilder) : SearchBuilder {
        val builder = SearchBuilder(req, source)
        routeSearchRequest(builder.request)
        return builder
    }

    fun newGetRequest(id: String) : GetRequest {
        return GetRequest(route.indexName).id(id)
                .apply {
                    if (route.routingKey != null) { this.routing(route.routingKey) }
                }
    }

    fun newUpdateRequest(id: String) : UpdateRequest {
        return UpdateRequest(route.indexName, "asset", id)
                .apply {
                    if (route.routingKey != null) { this.routing(route.routingKey) }
                }
    }

    fun newIndexRequest(id: String) : IndexRequest {
        return IndexRequest(route.indexName, "asset", id)
                .apply {
                    if (route.routingKey != null) { this.routing(route.routingKey) }
                }
    }

    fun newDeleteRequest(id:String) : DeleteRequest {
        return DeleteRequest(route.indexName, "asset", id)
                .apply {
                    if (route.routingKey != null) { this.routing(route.routingKey) }
                }
    }

    fun routeSearchRequest(req: SearchRequest) : SearchRequest {
        req.indices(route.indexName)
        if (route.routingKey != null) {
            req.routing(route.routingKey)
        }
        return req
    }

    /**
     * Return true of the index referenced in the IndexRoute exists.
     *
     * @return Boolean: true on exists
     */
    fun indexExists(): Boolean {
        return client.lowLevelClient.performRequest("HEAD", route.indexUrl).statusLine.statusCode == 200
    }

    /**
     * Return true if the ES cluster is available.
     *
     * @return Boolean: true if server is up
     */
    fun isAvailable(): Boolean {
        return try {
            client.lowLevelClient.performRequest("HEAD", route.clusterUrl).statusLine.statusCode == 200
        } catch (e: IOException) {
            false
        }
    }
}
