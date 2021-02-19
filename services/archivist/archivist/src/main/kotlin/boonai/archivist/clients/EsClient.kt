package boonai.archivist.clients

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.domain.EsClientCacheKey
import boonai.common.util.Json
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.reindex.UpdateByQueryRequest
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory
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
 * projects's routing settings.
 */
class EsRestClient(val route: EsClientCacheKey, val client: RestHighLevelClient) {

    fun newSearchRequest(): SearchRequest {
        return SearchRequest(route.indexName)
    }

    fun newSearchBuilder(): SearchBuilder {
        val builder = SearchBuilder()
        routeSearchRequest(builder.request)
        return builder
    }

    fun newSearchBuilder(req: SearchRequest, source: SearchSourceBuilder): SearchBuilder {
        val builder = SearchBuilder(req, source)
        routeSearchRequest(builder.request)
        return builder
    }

    fun newGetRequest(id: String): GetRequest {
        return GetRequest(route.indexName).id(id)
    }

    fun newUpdateRequest(id: String): UpdateRequest {
        return UpdateRequest(route.indexName, id)
    }

    fun newUpdateByQueryRequest(): UpdateByQueryRequest {
        return UpdateByQueryRequest(route.indexName)
    }

    fun newIndexRequest(id: String): IndexRequest {
        val req = IndexRequest(route.indexName)
        req.id(id)
        return req
    }

    fun newDeleteRequest(id: String): DeleteRequest {
        return DeleteRequest(route.indexName, id)
    }

    fun routeSearchRequest(req: SearchRequest): SearchRequest {
        return req.indices(route.indexName)
    }

    /**
     * Return true of the index referenced in the IndexRoute exists.
     *
     * @return Boolean: true on exists
     */
    fun indexExists(): Boolean {
        return client.lowLevelClient.performRequest(
            Request("HEAD", route.indexUrl)
        ).statusLine.statusCode == 200
    }

    /**
     * Return true if the ES cluster is available.
     *
     * @return Boolean: true if server is up
     */
    fun isAvailable(): Boolean {
        return try {
            client.lowLevelClient.performRequest(
                Request("HEAD", route.clusterUrl)
            ).statusLine.statusCode == 200
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Return the ES mapping as a Document.
     */
    fun getMapping(): Map<String, Any> {
        val stream = client.lowLevelClient.performRequest(
            Request("GET", "/${route.indexName}")
        ).entity.content
        return Json.Mapper.readValue(stream)
    }

    /**
     * Update ES asset mapping with given body.  Return true if the mapping update was
     * a success or the mapping already existed.s
     */
    fun updateMapping(body: Map<String, Any>): Boolean {
        val url = "${route.indexUrl}/_mapping"
        val req = Request("PUT", url)
        req.setJsonEntity(Json.serializeToString(body))

        val rsp = client.lowLevelClient.performRequest(req)
        return rsp.statusLine.statusCode == 200
    }

    /**
     * Refresh the index so new data is available.
     */
    fun refresh() {
        client.indices().refresh(RefreshRequest(route.indexName), RequestOptions.DEFAULT)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EsRestClient::class.java)
    }
}
