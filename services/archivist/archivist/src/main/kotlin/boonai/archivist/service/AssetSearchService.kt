package boonai.archivist.service

import boonai.archivist.security.getProjectId
import boonai.common.util.Json
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.ClearScrollResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.Strings
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

interface AssetSearchService {
    fun search(search: Map<String, Any>, params: Map<String, Array<String>>): SearchResponse
    fun search(ssb: SearchSourceBuilder, params: Map<String, Array<String>>): SearchResponse
    fun search(search: Map<String, Any>): SearchResponse
    fun scroll(scroll: Map<String, String>): SearchResponse
    fun clearScroll(scroll: Map<String, String>): ClearScrollResponse
    fun count(search: Map<String, Any>): Long
    fun count(ssb: SearchSourceBuilder): Long
    fun mapToSearchSourceBuilder(search: Map<String, Any>): SearchSourceBuilder
    fun searchSourceBuilderToMap(ssb: SearchSourceBuilder): Map<String, Any>
}

@Service
class AssetSearchServiceImpl : AssetSearchService {

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    override fun search(search: Map<String, Any>, params: Map<String, Array<String>>): SearchResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()

        if (params.containsKey("scroll")) {
            req.scroll(params.getValue("scroll")[0])
        }

        req.source(mapToSearchSourceBuilder(search))
        req.preference(getProjectId().toString())

        return rest.client.search(req, RequestOptions.DEFAULT)
    }

    override fun search(ssb: SearchSourceBuilder, params: Map<String, Array<String>>): SearchResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()
        req.source(ssb)
        if (params.containsKey("scroll")) {
            req.scroll(params.getValue("scroll")[0])
        }
        req.preference(getProjectId().toString())
        return rest.client.search(req, RequestOptions.DEFAULT)
    }

    override fun search(search: Map<String, Any>): SearchResponse {
        return search(search, mapOf())
    }

    override fun count(search: Map<String, Any>): Long {
        val copy = search.toMutableMap()
        copy["size"] = 0
        return search(copy).hits.totalHits.value
    }

    override fun count(ssb: SearchSourceBuilder): Long {
        return search(ssb, mapOf()).hits.totalHits.value
    }

    override fun scroll(scroll: Map<String, String>): SearchResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val req = SearchScrollRequest(scroll.getValue("scroll_id"))
        req.scroll(scroll.getValue("scroll"))
        val rsp = rest.client.scroll(req, RequestOptions.DEFAULT)
        // Clear the scroll automatically if result is empty.
        if (rsp.hits.hits.isEmpty()) {
            clearScroll(scroll)
        }
        return rsp
    }

    override fun clearScroll(scroll: Map<String, String>): ClearScrollResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val req = ClearScrollRequest()
        req.addScrollId(scroll.getValue("scroll_id"))
        return rest.client.clearScroll(req, RequestOptions.DEFAULT)
    }

    override fun searchSourceBuilderToMap(ssb: SearchSourceBuilder): Map<String, Any> {
        return Json.Mapper.readValue(Strings.toString(ssb, true, true), Json.GENERIC_MAP)
    }

    override fun mapToSearchSourceBuilder(search: Map<String, Any>): SearchSourceBuilder {

        // Filters out search options that are not supported.
        val searchSource = search.filterKeys { it in allowedSearchProperties }
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            Json.serializeToString(searchSource)
        )

        val outerQuery = QueryBuilders.boolQuery()
        outerQuery.filter(QueryBuilders.termQuery("system.state", "Analyzed"))

        val ssb = SearchSourceBuilder.fromXContent(parser)
        if (ssb.query() != null) {
            outerQuery.must(ssb.query())
        }
        ssb.query(outerQuery)

        if (logger.isDebugEnabled) {
            logger.debug("SEARCH : {}", Strings.toString(ssb, true, true))
        }

        return ssb
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(AssetSearchServiceImpl::class.java)

        val searchModule = SearchModule(Settings.EMPTY, false, emptyList())

        val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)

        val allowedSearchProperties = setOf(
            "query", "from", "size", "timeout",
            "post_filter", "minscore", "suggest",
            "highlight", "collapse", "_source",
            "slice", "aggs", "aggregations", "sort",
            "search_after", "track_total_hits"
        )
    }
}
