package boonai.archivist.service

import boonai.archivist.domain.DataSetQuery
import boonai.archivist.security.getProjectId
import boonai.common.util.Json
import com.fasterxml.jackson.module.kotlin.convertValue
import org.apache.lucene.search.join.ScoreMode
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
import org.elasticsearch.client.Request
import org.elasticsearch.client.Response
import org.elasticsearch.index.query.BoolQueryBuilder
import java.lang.IllegalArgumentException

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
    fun sqlSearch(query: String): Response
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

    fun handleSpecialFilters(search: Map<String, Any>): BoolQueryBuilder {

        val outerQuery = QueryBuilders.boolQuery()
        outerQuery.filter(QueryBuilders.termQuery("system.state", "Analyzed"))

        // exclude training sets
        val excludeLabeled = search.getOrDefault("exclude_all_datasets", false) as Boolean
        if (excludeLabeled) {
            outerQuery.mustNot(
                QueryBuilders.nestedQuery(
                    "labels",
                    QueryBuilders.boolQuery().filter(QueryBuilders.existsQuery("labels.datasetId")), ScoreMode.None
                )
            )
        }

        if (search.containsKey("exclude_dataset")) {
            val tsq = Json.Mapper.convertValue<DataSetQuery>(search.getValue("exclude_dataset"))
            val innerBool = QueryBuilders.boolQuery()
            innerBool.must(QueryBuilders.termQuery("labels.datasetId", tsq.datasetId.toString()))
            if (tsq.scopes != null) {
                innerBool.must(QueryBuilders.termsQuery("labels.scope", tsq.scopes.map { it.toString() }))
            }
            if (tsq.labels != null) {
                innerBool.must(QueryBuilders.termsQuery("labels.label", tsq.labels.map { it }))
            }

            outerQuery.mustNot(
                QueryBuilders.nestedQuery(
                    "labels",
                    innerBool,
                    ScoreMode.None
                )
            )
        }

        // Training set query
        if (search.containsKey("dataset")) {

            val tsq = Json.Mapper.convertValue<DataSetQuery>(search.getValue("dataset"))
            val innerBool = QueryBuilders.boolQuery()
            innerBool.must(QueryBuilders.termQuery("labels.datasetId", tsq.datasetId.toString()))
            if (tsq.scopes != null) {
                innerBool.must(QueryBuilders.termsQuery("labels.scope", tsq.scopes.map { it.toString() }))
            }
            if (tsq.labels != null) {
                innerBool.must(QueryBuilders.termsQuery("labels.label", tsq.labels.map { it }))
            }

            outerQuery.filter(
                QueryBuilders.nestedQuery(
                    "labels",
                    innerBool,
                    ScoreMode.None
                )
            )
        }

        return outerQuery
    }

    override fun mapToSearchSourceBuilder(search: Map<String, Any>): SearchSourceBuilder {

        val outerQuery = handleSpecialFilters(search)

        // Filters out search options that are not supported.
        val searchSource = search.filterKeys { it in allowedSearchProperties }
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            Json.serializeToString(searchSource)
        )

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

    override fun sqlSearch(query: String): Response {
        val rest = indexRoutingService.getProjectRestClient()

        // Not sure if this is secure enough but if the query does not reference
        // the boonai index the it will be an error.
        if (!query.contains(Regex("^SELECT (.*?) FROM boonai(\\s+|$)", setOf(RegexOption.IGNORE_CASE)))) {
            throw IllegalArgumentException("Your query must contain a reference to the boonai index.")
        }

        val queryfixed = query.replace("boonai", "\"${rest.route.indexName}\"")
        val request = Request("GET", "/_sql")
        request.setJsonEntity(Json.serializeToString(mapOf("query" to queryfixed)))

        return rest.client.lowLevelClient.performRequest(request)
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
