package com.zorroa.common.clients

import com.zorroa.archivist.domain.EsClientCacheKey
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.io.IOException


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
class EsRestClient(val route: EsClientCacheKey, val client: RestHighLevelClient) {

    fun newSearchRequest() : SearchRequest {
        return SearchRequest(route.indexName).apply {
            route.routingKey?.let { routing(it) }
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
        return GetRequest(route.indexName).id(id).apply {
            route.routingKey?.let { routing(it) }
        }
    }

    fun newUpdateRequest(id: String) : UpdateRequest {
        return UpdateRequest(route.indexName, "asset", id).apply {
            route.routingKey?.let { routing(it) }
        }
    }

    fun newIndexRequest(id: String) : IndexRequest {
        return IndexRequest(route.indexName, "asset", id).apply {
            route.routingKey?.let { routing(it) }
        }
    }

    fun newDeleteRequest(id:String) : DeleteRequest {
        return DeleteRequest(route.indexName, "asset", id).apply {
            route.routingKey?.let { routing(it) }
        }
    }

    fun routeSearchRequest(req: SearchRequest) : SearchRequest {
        if (route.routingKey != null) {
            req.routing(route.routingKey)
        }
        return req.indices(route.indexName)
    }

    /**
     * Return true of the index referenced in the IndexRoute exists.
     *
     * @return Boolean: true on exists
     */
    fun indexExists(): Boolean {
        return client.lowLevelClient.performRequest(
                Request("HEAD", route.indexUrl)).statusLine.statusCode == 200
    }

    /**
     * Return true if the ES cluster is available.
     *
     * @return Boolean: true if server is up
     */
    fun isAvailable(): Boolean {
        return try {
            client.lowLevelClient.performRequest(
                    Request("HEAD", route.clusterUrl)).statusLine.statusCode == 200
        } catch (e: IOException) {
            false
        }
    }
}
