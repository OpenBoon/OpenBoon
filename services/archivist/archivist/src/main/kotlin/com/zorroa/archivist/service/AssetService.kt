package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetIdBuilder
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.BatchAssetOpStatus
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.STANDARD_PIPELINE
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.elastic.ElasticSearchErrorTranslator
import com.zorroa.archivist.schema.ProxySchema
import com.zorroa.archivist.security.getProjectFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.FileUtils
import com.zorroa.archivist.util.Json
import com.zorroa.archivist.util.randomString
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.bulk.BulkRequest
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
     * Batch create a list of assets.  Creating adds a base asset with
     * just source data to ElasticSearch.  A created asset still needs
     * to be analyed.
     *
     * @param request: A BatchCreateAssetsRequest
     * @return A BatchCreateAssetsResponse which contains the assets and their created status.
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

    /**
     * Handle a batch upload request and return a BatchCreateAssetsResponse
     *
     * @param req: a BatchUploadAssetsRequest
     * @returns a BatchCreateAssetsResponse
     */
    fun batchUpload(req: BatchUploadAssetsRequest): BatchCreateAssetsResponse
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
        val rsp = rest.client.get(rest.newGetRequest(id), RequestOptions.DEFAULT)
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

    fun assetSpecToAsset(id: String, spec: AssetSpec, task: InternalTask? = null): Asset {
        val asset = Asset(id, spec.document ?: mutableMapOf())

        asset.setAttr("source.path", spec.uri)
        asset.setAttr("source.filename", FileUtils.filename(spec.uri))
        asset.setAttr("source.extension", FileUtils.extension(spec.uri))

        val mediaType = FileUtils.getMediaType(spec.uri)
        asset.setAttr("source.mimetype", mediaType)
        asset.setAttr("source.type", mediaType.split("/")[0])
        asset.setAttr("source.subtype", mediaType.split("/")[1])

        asset.setAttr("system.projectId", getProjectId().toString())
        task?.let {
            asset.setAttr("system.dataSourceId", it.dataSourceId)
            asset.setAttr("system.jobId", it.jobId)
        }
        asset.setAttr(
            "system.timeCreated",
            java.time.Clock.systemUTC().instant().toString()
        )
        asset.setAttr("system.state", AssetState.CREATED.toString())

        return asset
    }

    override fun batchUpload(req: BatchUploadAssetsRequest)
        : BatchCreateAssetsResponse {

        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        var assets = mutableListOf<Asset>()

        for ((idx, mpfile) in req.files.withIndex()) {
            val spec = req.assets[idx]
            val id = AssetIdBuilder(spec, randomString(16)).build()

            val asset = assetSpecToAsset(id, spec)
            val storageSpec = FileStorageSpec("asset", asset.id, mpfile.originalFilename)
            val storage = fileStorageService.get(storageSpec)
            fileStorageService.write(storage.id, mpfile.inputStream.readAllBytes())

            asset.setAttr("source.originPath", asset.getAttr("source.path"))
            asset.setAttr("source.path", "pixml:///$id/source/${mpfile.originalFilename}")
            asset.setAttr("source.fileSize", mpfile.size)

            val ireq = rest.newIndexRequest(asset.id)
            ireq.opType(DocWriteRequest.OpType.CREATE)
            ireq.source(asset.document)
            bulkRequest.add(ireq)

            assets.add(asset)
        }

        val bulk = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
        val result = BatchCreateAssetsResponse(assets)
        for (item in bulk.items) {
            if (item.isFailed) {
                result.status.add(
                    BatchAssetOpStatus(
                        item.id,
                        ElasticSearchErrorTranslator.translate(item.failureMessage)
                    )
                )
            } else {
                result.status.add(BatchAssetOpStatus(item.id))
            }
        }

        // Launch analysis job.
        val jobId = if (req.analyze) {
            createAnalysisJob(assets).id
        } else {
            null
        }
        result.jobId = jobId
        return result
    }

    override fun batchCreate(request: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        if (request.assets.size > 100) {
            throw IllegalArgumentException("Cannot create more than 100 assets at a time.")
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        val assets = request.assets.map { spec ->
            val id = AssetIdBuilder(spec).dataSource(request.task?.dataSourceId).build()
            assetSpecToAsset(id, spec, request.task)
        }

        assets.forEach {
            val ireq = rest.newIndexRequest(it.id)
            ireq.opType(DocWriteRequest.OpType.CREATE)
            ireq.source(it.document)
            bulkRequest.add(ireq)
        }

        val bulk = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
        val result = BatchCreateAssetsResponse(assets)
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

                // Remove these which are used for temp attrs
                asset.removeAttr("tmp")
                asset.removeAttr("temp")

                // Update various system properties.
                asset.setAttr("system.projectId", getProjectId().toString())
                asset.setAttr("system.timeModified", time)
                asset.setAttr("system.state", AssetState.ANALYZED.toString())

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
        val name = "Analyze ${assets.size} created assets"
        val script = ZpsScript(name, null, assets, STANDARD_PIPELINE)
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

        val logger: Logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    }
}
