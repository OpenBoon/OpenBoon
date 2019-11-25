package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.BatchAssetOpStatus
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.IdGen
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.elastic.ElasticSearchErrorTranslator
import com.zorroa.archivist.schema.ProxySchema
import com.zorroa.archivist.security.getProjectFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.FileUtils
import com.zorroa.archivist.util.Json
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.Strings
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.join.query.HasChildQueryBuilder
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.OutputStream

/**
 * AssetService contains the entry points for Asset CRUD operations. In general
 * you won't use IndexService directly, AssetService will call through for you.
 *
 * Note that, unfortunately, we update ES before the transactional datastore because
 * we rely on ES to merge upserts.  If we did not allow upserts and always overwrote
 * the full doc, we could switch this behavior.
 */
interface AssetService {

    /**
     * Execute an arbitrary ES search and send the result
     * to the given OutputStream.
     */
    fun search(query: Map<String, Any>, output: OutputStream)

    /**
     * Get an Asset by ID.
     */
    fun get(assetId: String): Asset

    fun getAll(ids: List<String>): List<Asset>

    fun getValidAssetIds(ids: List<String>): Set<String>

    fun getProxies(id: String): ProxySchema

    /**
     * Batch provision a list of assets.  Provisioning adds a base asset with
     * just source data to ElasticSearch.  A provisioned asset still needs
     * to be processed.
     *
     * @param request: A BatchCreateAssetsRequest
     * @return A BatchCreateAssetsResponse which contains the assets and their provision status.
     *
     * TODO: handle clips
     */
    fun batchCreate(request: BatchCreateAssetsRequest): BatchCreateAssetsResponse

    /**
     * Batch update the given batch of Assets. The fully composed asset must be
     * provided, not a partial update.
     *
     * @param request: A BatchUpdateAssetsRequest
     * @return A BatchUpdateAssetsResponse which contains success/fail status for each asst.
     *
     */
    fun batchUpdate(request: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse
}



@Service
class AssetServiceImpl : AssetService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    lateinit var fileStorageService: FileStorageService

    override fun get(id: String): Asset {
        val rest = indexRoutingService.getProjectRestClient()
        val rsp = rest.client.get(GetRequest(id), RequestOptions.DEFAULT)
        return Asset(rsp.id, rsp.sourceAsMap)
    }

    override fun getValidAssetIds(ids: List<String>): Set<String> {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchBuilder()
        val query = QueryBuilders.boolQuery()
            .must(QueryBuilders.termsQuery("_id", ids))
        req.source.size(ids.size)
        req.source.query(query)
        req.source.fetchSource(false)

        val result = mutableSetOf<String>()
        val r = rest.client.search(req.request, RequestOptions.DEFAULT)
        r.hits.forEach {
            result.add(it.id)
        }
        return result
    }

    override fun getAll(ids: List<String>): List<Asset> {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchBuilder()
        val query = QueryBuilders.boolQuery()
            .must(QueryBuilders.termsQuery("_id", ids))
        req.source.size(ids.size)
        req.source.query(query)

        val r = rest.client.search(req.request, RequestOptions.DEFAULT)
        return r.hits.map {
            Asset(it.id, it.sourceAsMap)
        }
    }

    override fun getProxies(id: String): ProxySchema {
        val asset = get(id)
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)
        return proxies ?: ProxySchema()
    }

    override fun batchCreate(request: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        if (request.assets.size > 100) {
            throw IllegalArgumentException("Cannot provision more than 100 assets at a time.")
        }

        val rest = indexRoutingService.getProjectRestClient()

        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        val assets = request.assets.map { spec ->

            val id = IdGen.getId(spec.uri)
            val asset = Asset(id, spec.document ?: mutableMapOf())

            asset.setAttr("source.path", spec.uri)
            asset.setAttr("source.filename", FileUtils.filename(spec.uri))
            asset.setAttr("source.directory", FileUtils.dirname(spec.uri))
            asset.setAttr("source.extension", FileUtils.extension(spec.uri))

            val mediaType = FileUtils.getMediaType(spec.uri)
            asset.setAttr("source.mimetype", mediaType)
            asset.setAttr("source.type", mediaType.split("/")[0])
            asset.setAttr("source.subtype", mediaType.split("/")[1])

            asset.setAttr("system.projectId", getProjectId().toString())
            asset.setAttr("system.timeCreated",
                java.time.Clock.systemUTC().instant().toString())
            asset.setAttr("system.state", "provisioned")
            asset
        }

        assets.forEach {
            val ireq = rest.newIndexRequest(it.id)
            ireq.opType(DocWriteRequest.OpType.CREATE)
            ireq.source(it.document)
            bulkRequest.add(ireq)
        }

        val bulk = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
        val result =  BatchCreateAssetsResponse()
        for (item in bulk.items) {
            if (item.isFailed) {
                result.status.add(BatchAssetOpStatus(item.id,
                    ElasticSearchErrorTranslator.translate(item.failureMessage)))
            } else {
                result.status.add(BatchAssetOpStatus(item.id))
            }
        }

        // Launch analysis job.
        val jobId = if (request.analyze) {
            createAnalysisJob(assets).id
        } else {
            null
        }

        result.assets = assets
        result.jobId = jobId
        return result
    }

    override fun batchUpdate(request: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {
        val result = BatchUpdateAssetsResponse(request.assets.size)
        val assets = request.assets
        if (assets.isEmpty()) {
            return result
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()
        if (request.resfresh) {
            bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
        }
        val validAssetIds = getValidAssetIds(assets.map { it.id })
        var bulkRequestValid = false

        val time = java.time.Clock.systemUTC().instant().toString()
        assets.forEachIndexed { idx, asset ->

            if (asset.id !in validAssetIds) {
                result.status[idx] = BatchAssetOpStatus(asset.id, "Asset does not exist")
            }
            else {
                bulkRequestValid = true
                asset.setAttr("system.projectId", getProjectId().toString())
                asset.setAttr("system.timeModified", time)
                asset.setAttr("system.state", "processed")

                bulkRequest.add(
                    rest.newIndexRequest(asset.id)
                        .source(asset.document)
                        .opType(DocWriteRequest.OpType.INDEX)
                )
            }
        }

        if (!bulkRequestValid) {
            return result
        }

        val bulk = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
        var idxPlus = 0
        bulk.items.forEachIndexed { idx, item ->
            while (result.status[idx + idxPlus] != null) {
                idxPlus += 1
            }
            val status = if (item.isFailed) {
                BatchAssetOpStatus(item.id,
                        ElasticSearchErrorTranslator.translate(item.failureMessage))
            } else {
                BatchAssetOpStatus(item.id)
            }
            result.status[idx + idxPlus] = status
        }
        return result
    }

    fun createAnalysisJob(assets: List<Asset>): Job {
        val execute = listOf(
            ProcessorRef("pixml_core.image.importers.ImageImporter", "zmlp-plugins-core")
        )
        val name = "Analyze ${assets.size} assets"
        val script = ZpsScript(name, null, assets, execute)
        val spec = JobSpec(name, script)

        return jobService.create(spec)
    }

    fun getDeepQuery(search: Map<String, Any>): QueryBuilder? {
        return search["deep-query"]?.let {

            val parser = XContentFactory.xContent(XContentType.JSON).createParser(
                xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                Json.serializeToString(mapOf("query" to it))
            )
            val ssb = SearchSourceBuilder.fromXContent(parser)
            ssb.query()
        }

        null
    }

    fun prepSearch(search: Map<String, Any>): SearchSourceBuilder {

        val baseSearch = search.filterKeys { it != "deep-query" }
        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            Json.serializeToString(baseSearch)
        )
        val ssb = SearchSourceBuilder.fromXContent(parser)

        // Wrap the query in Org filter
        val query = QueryBuilders.boolQuery()
        query.filter(getProjectFilter())

        if (ssb.query() == null) {
            query.must(QueryBuilders.matchAllQuery())
        } else {
            query.must(ssb.query())
        }

        getDeepQuery(search)?.let {
            query.must(HasChildQueryBuilder("element", it, ScoreMode.Avg))
        }

        // Replace the query in the SearchSourceBuilder with wrapped versions
        ssb.query(query)

        if (properties.getBoolean("archivist.debug-mode.enabled")) {
            logger.debug("SEARCH : {}", Strings.toString(query, true, true))
        }

        return ssb
    }
    override fun search(query: Map<String, Any>, output: OutputStream) {
        val client = indexRoutingService.getProjectRestClient()

        val req = client.newSearchRequest()
        req.searchType(SearchType.DEFAULT)
        req.preference(getProjectId().toString())
        req.source(prepSearch(query))

        val rsp = client.client.search(req, RequestOptions.DEFAULT)
        val builder = XContentFactory.jsonBuilder(output)
        rsp.toXContent(builder, ToXContent.EMPTY_PARAMS)
        builder.close()
    }

    companion object {

        val searchModule = SearchModule(Settings.EMPTY, false, emptyList())

        val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)

        /**
         * Namespaces that are protected or unable to be set via the API.
         */
        val PROTECTED_NAMESPACES = setOf("system", "tmp")
        
        val logger: Logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    }
}
