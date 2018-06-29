package com.zorroa.common.clients

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.common.domain.IndexRoute
import org.apache.http.HttpHost
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

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
}

interface IndexRoutingService {
    fun getIndexRoute(orgId: UUID) : IndexRoute
}

/**
 * A stand-in routing service that returns the same ES route for every organization.
 */
class FakeIndexRoutingServiceImpl constructor(val url: String) : IndexRoutingService {

    val route = IndexRoute(UUID.fromString("00000000-9998-8888-7777-666666666666"),
            "100",
            url,
            "zorroa_v10",
            null)

    override fun getIndexRoute(orgId: UUID): IndexRoute {
        return route
    }
}

/**
 * A cache for storing EsRestClient instances keyed on an organizations IndexRoute.
 * An EsRestClient instance contains a RestHighLevelClient and routing info.
 */
class EsClientCache constructor(private val routingService: IndexRoutingService) {

    private val cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .initialCapacity(20)
            .concurrencyLevel(4)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(object : CacheLoader<IndexRoute, EsRestClient>() {
                @Throws(Exception::class)
                override fun load(route: IndexRoute): EsRestClient {
                    val uri = URI.create(route.clusterUrl)
                    return EsRestClient(route, RestHighLevelClient(
                            RestClient.builder(HttpHost(uri.host, uri.port, uri.scheme))))
                }
            })

    operator fun get(orgId: UUID): EsRestClient {
        val route = routingService.getIndexRoute(orgId)
        return get(route)
    }

    fun get(route: IndexRoute): EsRestClient {
        return cache.get(route)
    }
}
