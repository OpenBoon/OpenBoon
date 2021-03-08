package boonai.archivist.service

import boonai.archivist.clients.EsRestClient
import boonai.archivist.config.ApplicationProperties
import boonai.archivist.domain.Asset
import boonai.archivist.domain.BatchUpdateClipProxyRequest
import boonai.archivist.domain.BatchUpdateResponse
import boonai.archivist.domain.Clip
import boonai.archivist.domain.ClipIdBuilder
import boonai.archivist.domain.ClipSpec
import boonai.archivist.domain.CreateTimelineResponse
import boonai.archivist.domain.FileExtResolver
import boonai.archivist.domain.TimelineSpec
import boonai.archivist.domain.UpdateClipProxyRequest
import boonai.archivist.domain.WebVTTFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.archivist.util.bd
import boonai.archivist.util.formatDuration
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.logging.warnEvent
import boonai.common.util.Json
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.search.SearchRequest
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
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.annotation.PostConstruct

interface ClipService {

    /**
     * Create a map of collapse keys used for searching.
     */
    fun getCollapseKeys(assetId: String, time: BigDecimal): Map<String, String>

    /**
     * Bulk create a bunch of clips using a TimelineSpec.
     */
    fun createClips(timeline: TimelineSpec): CreateTimelineResponse

    /**
     * Search for clips using an ES REST DSL query. An Asset can be optionally provided.
     */
    fun searchClips(asset: Asset?, search: Map<String, Any>, params: Map<String, Array<String>>): SearchResponse

    /**
     * Stream a WebVTT file that matches the search.
     */
    fun streamWebvtt(asset: Asset, search: Map<String, Any>, outputStream: OutputStream)

    /**
     * Stream a WebVTT file that matchess the WebVTTFilter.
     */
    fun streamWebvtt(filter: WebVTTFilter, outputStream: OutputStream)

    /**
     * Stream a WebVTT file that matches a particular timeline.
     */
    fun streamWebvttByTimeline(asset: Asset, timeline: String, outputStream: OutputStream)

    /**
     * Converts a ES search DSL request into a SearchSourceBuilder
     */
    fun mapToSearchSourceBuilder(asset: Asset?, search: Map<String, Any>): SearchSourceBuilder

    /**
     * Bulk delete the given clips.
     */
    fun deleteClips(ids: List<String>)

    /**
     * Create a singel clip.
     */
    fun createClip(spec: ClipSpec): Clip

    /**
     * Delete a single clip.
     */
    fun deleteClip(id: String): Boolean

    /**
     * Set the proxy info ont the clip which contains the simhash and file data
     */
    fun setProxy(id: String, proxy: UpdateClipProxyRequest): Boolean

    /**
     * Set proxy data for multiple clips at one time.
     */
    fun batchSetProxy(req: BatchUpdateClipProxyRequest): BatchUpdateResponse

    /**
     * Get a clip by ID
     */
    fun getClip(id: String): Clip
}

@Service
class ClipServiceImpl(
    val indexRoutingService: IndexRoutingService,
) : ClipService {

    @Autowired
    private lateinit var assetService: AssetService

    @Autowired
    private lateinit var jobLaunchService: JobLaunchService

    @Autowired
    private lateinit var properties: ApplicationProperties

    @PostConstruct
    fun startup() {
        logger.info(
            "Deep video analysis enabled: {}",
            properties.getBoolean("archivist.deep-video-analysis.enabled")
        )
    }

    override fun createClip(spec: ClipSpec): Clip {
        val rest = indexRoutingService.getProjectRestClient()
        val asset = assetService.getAsset(spec.assetId)

        val start = spec.start.setScale(3, RoundingMode.HALF_UP)
        val stop = spec.stop.setScale(3, RoundingMode.HALF_UP)
        val length = stop.subtract(start)
        val score = spec.score.bd()

        // Make the document.
        val doc = mapOf(
            "clip" to mapOf(
                "assetId" to asset.id,
                "timeline" to spec.timeline,
                "track" to spec.track,
                "content" to spec.content,
                "score" to score,
                "start" to start,
                "stop" to stop,
                "length" to length,
                "collapseKey" to getCollapseKeys(asset.id, start),
                "bbox" to spec.bbox
            ),
            "deepSearch" to mapOf("name" to "clip", "parent" to asset.id)
        )

        val id = ClipIdBuilder(asset, spec.timeline, spec.track, start, stop).buildId()
        val req = rest.newIndexRequest(id)
        req.routing(asset.id)
        req.source(doc)

        rest.client.index(req, RequestOptions.DEFAULT)
        val clip = Clip(id, asset.id, spec.timeline, spec.track, start, stop, spec.content, score.toDouble())

        if (properties.getBoolean("archivist.deep-video-analysis.enabled")) {
            jobLaunchService.launchCipAnalysisJob(clip)
        }

        return clip
    }

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
                val start = clip.start.setScale(3, RoundingMode.HALF_UP)
                val stop = clip.stop.setScale(3, RoundingMode.HALF_UP)
                val id = ClipIdBuilder(asset, timeline.name, track.name, start, stop).buildId()
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
                        "length" to length,
                        "collapseKey" to getCollapseKeys(asset.id, start),
                        "bbox" to clip.bbox
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

        if (properties.getBoolean("archivist.deep-video-analysis.enabled") && timeline.deepAnalysis) {
            val jobId = getZmlpActor().getAttr("jobId")
            if (jobId != null) {
                val task =
                    jobLaunchService.addTimelineAnalysisTask(UUID.fromString(jobId), timeline.assetId, timeline.name)
                response.taskId = task.id
            } else {
                val job = jobLaunchService.launchTimelineAnalysisJob(timeline.assetId, timeline.name)
                response.jobId = job.id
            }
        }

        return response
    }

    override fun searchClips(
        asset: Asset?,
        search: Map<String, Any>,
        params: Map<String, Array<String>>
    ): SearchResponse {
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

    override fun streamWebvttByTimeline(asset: Asset, timeline: String, outputStream: OutputStream) {
        val search = mapOf("query" to mapOf("term" to mapOf("clip.timeline" to timeline)))
        return streamWebvtt(asset, search, outputStream)
    }

    override fun streamWebvtt(filter: WebVTTFilter, outputStream: OutputStream) {

        val rest = indexRoutingService.getProjectRestClient()
        val ssb = SearchSourceBuilder()
        ssb.query(filter.getQuery())
        val req = rest.newSearchRequest()
        ssb.size(100)
        ssb.sort("_doc")
        req.source(ssb)
        req.preference(getProjectId().toString())
        req.scroll("5s")

        val buffer = StringBuilder(512)
        buffer.append("WEBVTT\n\n")

        outputWebvtt(rest, req, buffer, outputStream)
    }

    override fun streamWebvtt(asset: Asset, search: Map<String, Any>, outputStream: OutputStream) {

        val rest = indexRoutingService.getProjectRestClient()
        val ssb = mapToSearchSourceBuilder(asset, search)
        val req = rest.newSearchRequest()
        ssb.size(100)
        ssb.sort("_doc")
        req.source(ssb)
        req.preference(getProjectId().toString())
        req.scroll("5s")

        val buffer = StringBuilder(512)
        buffer.append("WEBVTT\n\n")

        outputWebvtt(rest, req, buffer, outputStream)
    }

    private fun outputWebvtt(
        rest: EsRestClient,
        req: SearchRequest,
        buffer: StringBuilder,
        outputStream: OutputStream
    ) {
        var rsp = rest.client.search(req, RequestOptions.DEFAULT)
        do {

            for (hit in rsp.hits.hits) {
                val clip = hit.sourceAsMap["clip"] as Map<String, Any>
                buffer.append(formatDuration(clip["start"] as Double))
                buffer.append(" --> ")
                buffer.append(formatDuration(clip["stop"] as Double))

                val obj = mapOf(
                    "timeline" to clip["timeline"].toString(),
                    "track" to clip["track"].toString(),
                    "content" to clip["content"],
                    "score" to clip["score"]
                )
                buffer.append("\n")
                buffer.append(Json.prettyString(obj))
                buffer.append("\n\n")
                outputStream.write(buffer.toString().toByteArray())
                buffer.clear()
            }

            val sr = SearchScrollRequest()
            sr.scrollId(rsp.scrollId)
            sr.scroll("10s")
            rsp = rest.client.scroll(sr, RequestOptions.DEFAULT)
        } while (rsp.hits.hits.isNotEmpty())
    }

    override fun mapToSearchSourceBuilder(asset: Asset?, search: Map<String, Any>): SearchSourceBuilder {

        // Filters out search options that are not supported.
        val searchSource = search.filterKeys { it in allowedSearchProperties }
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            Json.serializeToString(searchSource)
        )

        val outerQuery = QueryBuilders.boolQuery()
        outerQuery.filter(QueryBuilders.existsQuery("clip.timeline"))
        asset?.let {
            outerQuery.filter(QueryBuilders.termQuery("clip.assetId", it.id))
        }

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

    override fun deleteClip(id: String): Boolean {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newDeleteRequest(id)
        val rsp = rest.client.delete(req, RequestOptions.DEFAULT).result
        val deleted = rsp.name == "DELETED"

        if (deleted) {
            logger.event(LogObject.CLIP, LogAction.DELETE, mapOf("clipId" to rsp))
        }
        return rsp.name == "DELETED"
    }

    override fun deleteClips(ids: List<String>) {
        val rest = indexRoutingService.getProjectRestClient()

        val query = QueryBuilders.termsQuery(
            "clip.assetId", ids
        )

        val rsp = rest.client.deleteByQuery(
            DeleteByQueryRequest(rest.route.indexName)
                .setQuery(query),
            RequestOptions.DEFAULT
        )

        for (failure in rsp.bulkFailures) {
            logger.warnEvent(
                LogObject.CLIP, LogAction.DELETE, failure.message,
                mapOf("clipId" to failure.id)
            )
        }

        logger.event(LogObject.CLIP, LogAction.DELETE, mapOf("total" to rsp.deleted))
    }

    override fun setProxy(id: String, proxy: UpdateClipProxyRequest): Boolean {
        val clip = getClip(id)
        val rest = indexRoutingService.getProjectRestClient()

        val req = rest.newUpdateRequest(id)
        req.doc(Json.serializeToString(mapOf("clip" to proxy)), XContentType.JSON)
        req.routing(clip.assetId)

        return rest.client.update(
            req,
            RequestOptions.DEFAULT
        ).result == DocWriteResponse.Result.UPDATED
    }

    override fun batchSetProxy(req: BatchUpdateClipProxyRequest): BatchUpdateResponse {
        val rest = indexRoutingService.getProjectRestClient()
        if (req.updates.size > 1000) {
            throw IllegalArgumentException("Batch size too large (1001+)")
        }
        val asset = assetService.getAsset(req.assetId)
        val bulk = BulkRequest()
        for ((id, prx) in req.updates) {
            val req = rest.newUpdateRequest(id)
            req.routing(asset.id)
            req.doc(Json.serializeToString(mapOf("clip" to prx)), XContentType.JSON)
            bulk.add(req)
        }

        val rsp = rest.client.bulk(bulk, RequestOptions.DEFAULT)

        return if (rsp.hasFailures()) {
            BatchUpdateResponse(rsp.filter { it.isFailed }.map { it.id to it.failureMessage }.toMap())
        } else {
            BatchUpdateResponse(mapOf())
        }
    }

    override fun getClip(id: String): Clip {
        // We have to use a search here because the clip is routed to the asset.
        val rest = indexRoutingService.getProjectRestClient()
        val ssb = SearchSourceBuilder()
        ssb.query(QueryBuilders.termQuery("_id", id))
        val req = rest.newSearchRequest()
        ssb.size(1)
        req.source(ssb)

        val rsp = rest.client.search(req, RequestOptions.DEFAULT)
        if (rsp.hits.hits.isEmpty()) {
            throw EmptyResultDataAccessException("The Clip '$id' does not exist.", 1)
        }

        return Clip.fromMap(id, rsp.hits.hits[0].sourceAsMap["clip"] as Map<String, Any>)
    }

    override fun getCollapseKeys(assetId: String, time: BigDecimal): Map<String, String> {
        return collapseKeyWindows.map {
            val key = if (it.value == BigDecimal.ZERO) {
                time.toString().replace('.', '_')
            } else {
                time.divide(it.value).setScale(0, RoundingMode.DOWN)
            }
            it.key to "${assetId}_$key"
        }.toMap()
    }

    companion object {

        val collapseKeyWindows = mapOf(
            "startTime" to BigDecimal.ZERO,
            "1secWindow" to BigDecimal.valueOf(1),
            "5secWindow" to BigDecimal.valueOf(5),
            "10secWindow" to BigDecimal.valueOf(10)
        )

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

        val rightCurly = "{\n".toByteArray()
        val leftCurly = "}\n".toByteArray()
    }
}
