package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.ClipIdBuilder
import com.zorroa.archivist.domain.CreateTimelineResponse
import com.zorroa.archivist.domain.FileExtResolver
import com.zorroa.archivist.domain.TimelineSpec
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.bd
import com.zorroa.zmlp.util.Json
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.search.SearchResponse
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
import org.springframework.stereotype.Service
import java.math.BigDecimal

interface ClipService {
    fun createClips(timeline: TimelineSpec): CreateTimelineResponse

    fun searchClips(asset: Asset, search: Map<String, Any>, params: Map<String, Array<String>>): SearchResponse

    fun mapToSearchSourceBuilder(asset: Asset, search: Map<String, Any>): SearchSourceBuilder
}

@Service
class ClipServiceImpl(
    val indexRoutingService: IndexRoutingService,
    val assetService: AssetService
) : ClipService {

    override fun createClips(timeline: TimelineSpec): CreateTimelineResponse {
        val asset = assetService.getAsset(timeline.assetId)

        if (FileExtResolver.getType(asset) != "video") {
            throw IllegalArgumentException("Non-video assets cannot have video clips")
        }

        val response = CreateTimelineResponse(asset.id)
        val rest = indexRoutingService.getProjectRestClient()
        var bulkRequest = BulkRequest()
        bulkRequest.routing(asset.id)

        for (track in timeline.tracks) {
            val scoreCache = mutableMapOf<String, BigDecimal>()

            for (clip in track.clips) {
                val id = ClipIdBuilder(asset, timeline.name, track.name, clip).buildId()
                val start = clip.start.setScale(3)
                val stop = clip.stop.setScale(3)
                val length = stop.subtract(start)
                val score = clip.score.bd()

                // Cache the scores we've handled for each clip id.
                // If we've handled a higher score don't add to the batch.
                if ((scoreCache[id] ?: BigDecimal.ZERO) < score) {
                    scoreCache[id] = score
                } else {
                    continue
                }

                val doc = mapOf(
                    "clip" to mapOf(
                        "assetId" to asset.id,
                        "timeline" to timeline.name,
                        "track" to track.name,
                        "content" to clip.content,
                        "score" to scoreCache[id],
                        "start" to start,
                        "stop" to stop,
                        "length" to length
                    ),
                    "deepSearch" to mapOf("name" to "clip", "parent" to asset.id)
                )

                val req = rest.newIndexRequest(id)
                req.source(doc)
                bulkRequest.add(req)

                if (bulkRequest.numberOfActions() >= 100) {
                    val rsp = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
                    response.handleBulkResponse(rsp)
                    bulkRequest = BulkRequest()
                    bulkRequest.routing(asset.id)
                }
            }
        }

        if (bulkRequest.numberOfActions() > 0) {
            val rsp = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
            response.handleBulkResponse(rsp)
        }

        return response
    }

    override fun searchClips(asset: Asset, search: Map<String, Any>, params: Map<String, Array<String>>): SearchResponse {
        logger.info("Searching clips")
        val rest = indexRoutingService.getProjectRestClient()
        val ssb = mapToSearchSourceBuilder(asset, search)
        val req = rest.newSearchRequest()
        req.source(ssb)
        if (params.containsKey("scroll")) {
            req.scroll(params.getValue("scroll")[0])
        }
        req.preference(getProjectId().toString())
        return rest.client.search(req, RequestOptions.DEFAULT)
    }

    override fun mapToSearchSourceBuilder(asset: Asset, search: Map<String, Any>): SearchSourceBuilder {

        // Filters out search options that are not supported.
        val searchSource = search.filterKeys { it in allowedSearchProperties }
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            Json.serializeToString(searchSource)
        )

        val outerQuery = QueryBuilders.boolQuery()
        outerQuery.filter(QueryBuilders.existsQuery("clip.timeline"))
        outerQuery.filter(QueryBuilders.termQuery("clip.assetId", asset.id))

        val ssb = SearchSourceBuilder.fromXContent(parser)
        if (ssb.query() != null) {
            outerQuery.must(ssb.query())
        }
        ssb.query(outerQuery)
        ssb.fetchSource("clip.*", null)

        if (logger.isDebugEnabled) {
            logger.debug("Clip Search : {}", Strings.toString(ssb, true, true))
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
