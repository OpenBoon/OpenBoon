package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetIdBuilder
import com.zorroa.archivist.domain.AssetSearch
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.BatchAssetOpStatus
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.Clip
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.AssetFileLocator
import com.zorroa.archivist.domain.Element
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.ElasticSearchErrorTranslator
import com.zorroa.archivist.util.FileUtils
import com.zorroa.zmlp.util.Json
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.Strings
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service

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
    fun search(search: AssetSearch): SearchResponse

    /**
     * Get an Asset by ID.
     */
    fun getAsset(assetId: String): Asset

    fun getAll(ids: List<String>): List<Asset>

    fun getValidAssetIds(ids: List<String>): Set<String>

    /**
     * Batch create a list of assets.  Creating adds a base asset with
     * just source data to ElasticSearch.  A created asset still needs
     * to be analyzed.
     *
     * @param request: A BatchCreateAssetsRequest
     * @return A BatchCreateAssetsResponse which contains the assets and their created status.
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

    /**
     * Augment the newAsset with the clip definition found in the [AssetSpec] used
     * to create it.
     *
     * @param newAsset The [Asset] we're creating
     * @param spec [AssetSpec] provided by the caller.
     */
    fun deriveClip(newAsset: Asset, spec: AssetSpec): Clip
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
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var pipelineResolverService: PipelineResolverService

    override fun getAsset(id: String): Asset {
        val rest = indexRoutingService.getProjectRestClient()
        val rsp = rest.client.get(rest.newGetRequest(id), RequestOptions.DEFAULT)
        if (!rsp.isExists) {
            throw EmptyResultDataAccessException("The asset '$id' does not exist.", 1)
        }
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

    override fun deriveClip(newAsset: Asset, spec: AssetSpec): Clip {

        val clip = spec.clip ?: throw java.lang.IllegalArgumentException("Cannot derive a clip with a null clip")

        // In this case we're deriving from another asset and a clip
        // has to be set.
        if (spec.uri.startsWith("asset:")) {
            // Fetch the source asset and reset our source spec.uri
            val clipSource = getAsset(spec.uri.substring(6))
            clip.putInPile(clipSource.id)
            spec.uri = clipSource.getAttr("source.path", String::class.java)
                ?: throw IllegalArgumentException("The source asset for a clip cannot have a null URI")

            // Copy over source files if any
            val files = clipSource.getAttr("files", FileStorage.JSON_LIST_OF) ?: listOf()
            val sourceFiles = files.let {
                it.filter { file ->
                    file.category == ProjectStorageCategory.SOURCE
                }
            }

            // We have to reference the source asset in the StorageFile
            // record so the client side storage system to find the file.
            sourceFiles.forEach { it.sourceAssetId = clipSource.id }

            // Set the files property
            newAsset.setAttr("files", sourceFiles)
        } else {
            clip.putInPile(newAsset.id)
        }
        newAsset.setAttr("clip", clip)
        return clip
    }

    fun assetSpecToAsset(id: String, spec: AssetSpec, task: InternalTask? = null): Asset {
        val asset = Asset(id)

        spec.attrs?.forEach { k, v ->
            val prefix = try {
                k.substring(0, k.indexOf('.'))
            } catch (e: StringIndexOutOfBoundsException) {
                k
            }
            if (prefix !in removeFieldsOnCreate) {
                asset.setAttr(k, v)
            }
        }

        if (spec.clip != null) {
            deriveClip(asset, spec)
        }

        asset.setAttr("source.path", spec.uri)
        asset.setAttr("source.filename", FileUtils.filename(spec.uri))
        asset.setAttr("source.extension", FileUtils.extension(spec.uri))

        val mediaType = FileUtils.getMediaType(spec.uri)
        asset.setAttr("source.mimetype", mediaType)

        asset.setAttr("system.projectId", getProjectId().toString())
        task?.let {
            asset.setAttr("system.dataSourceId", it.dataSourceId)
            asset.setAttr("system.jobId", it.jobId)
            asset.setAttr("system.taskId", it.taskId)
        }
        asset.setAttr(
            "system.timeCreated",
            java.time.Clock.systemUTC().instant().toString()
        )
        asset.setAttr("system.state", AssetState.Pending.toString())

        return asset
    }

    override fun batchUpload(req: BatchUploadAssetsRequest):
        BatchCreateAssetsResponse {

        val pipeline = if (req.analyze) {
            pipelineResolverService.resolve(req.pipeline, req.modules)
        } else {
            null
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        var assets = mutableListOf<Asset>()

        for ((idx, mpfile) in req.files.withIndex()) {
            val spec = req.assets[idx]
            val idgen = AssetIdBuilder(spec)
                .checksum(mpfile.bytes)
            val id = idgen.build()
            val asset = assetSpecToAsset(id, spec)
            asset.setAttr("source.filesize", mpfile.size)
            asset.setAttr("source.checksum", idgen.checksum)

            val locator = AssetFileLocator(
                id, ProjectStorageCategory.SOURCE, mpfile.originalFilename
            )

            val file = projectStorageService.store(
                ProjectStorageSpec(locator, mapOf(), mpfile.bytes)
            )
            asset.setAttr("files", listOf(file))

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
        val jobId = if (pipeline != null) {
            createAnalysisJob(assets, pipeline).id
        } else {
            null
        }
        result.jobId = jobId

        logger.event(LogObject.ASSET, LogAction.CREATE, mapOf("assetsUploaded" to bulk.items.size))

        return result
    }

    override fun batchCreate(request: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        if (request.assets.size > 100) {
            throw IllegalArgumentException("Cannot create more than 100 assets at a time.")
        }

        val pipeline = if (request.analyze && request.task == null) {
            pipelineResolverService.resolve(request.pipeline, request.modules)
        } else {
            null
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        // Make a list of Assets from the spec
        val assets = request.assets.map { spec ->
            val id = AssetIdBuilder(spec).build()
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
        val jobId = if (pipeline != null) {
            createAnalysisJob(assets, pipeline).id
        } else {
            null
        }
        result.jobId = jobId

        logger.event(LogObject.ASSET, LogAction.CREATE, mapOf("assetsCreated" to bulk.items.size))
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
            } else {
                bulkRequestValid = true

                // Remove these which are used for temp attrs
                removeFieldsOnUpdate.forEach {
                    asset.removeAttr(it)
                }

                // Got back a clip but it has no pile which means it's in its own pile.
                // This happens during deep analysis when a file is being clipped, the first
                // clip/page/scene will be augmented with clip start/stop points.
                if (asset.attrExists("clip") && (
                        !asset.attrExists("clip.sourceAssetId") || !asset.attrExists("clip.pile"))
                ) {
                    val clip = asset.getAttr("clip", Clip::class.java)
                        ?: throw IllegalStateException("Invalid clip data for asset ${asset.id}")
                    clip.putInPile(asset.id)
                    asset.setAttr("clip", clip)
                }

                // Uniquify the elments
                if (asset.attrExists("elements")) {
                    val elements = asset.getAttr("elements", Element.JSON_SET_OF)
                    if (elements != null && elements.size > maxElementCount) {
                        throw IllegalStateException(
                            "Asset ${asset.id} has to many elements, > $maxElementCount"
                        )
                    }
                    asset.setAttr("elements", elements)
                }

                // Update various system properties.
                asset.setAttr("system.projectId", getProjectId().toString())
                asset.setAttr("system.timeModified", time)
                asset.setAttr("system.state", AssetState.Analyzed.toString())

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
                BatchAssetOpStatus(
                    item.id,
                    ElasticSearchErrorTranslator.translate(item.failureMessage)
                )
            } else {
                BatchAssetOpStatus(item.id)
            }
            result.status[idx + idxPlus] = status
        }

        logger.event(
            LogObject.ASSET, LogAction.BATCH_INDEX, mapOf("assetsIndexed" to bulk.items.size)
        )

        return result
    }

    fun createAnalysisJob(assets: List<Asset>, processors: List<ProcessorRef>): Job {
        val name = "Analyze ${assets.size} created assets"
        val script = ZpsScript(name, null, assets, processors)
        val spec = JobSpec(name, script)

        return jobService.create(spec)
    }

    fun getElementQuery(search: AssetSearch): QueryBuilder? {
        return if (search.elementQuery == null) {
            null
        } else {
            val parser = XContentFactory.xContent(XContentType.JSON).createParser(
                xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                Json.serializeToString(mapOf("query" to search.elementQuery))
            )
            val ssb = SearchSourceBuilder.fromXContent(parser)
            ssb.query()
        }
    }

    fun prepSearch(search: AssetSearch): SearchSourceBuilder {

        // Filters out search options that are not supported.
        val searchSource = (search.search ?: mutableMapOf())
            .filterKeys { it in allowedSearchProperties }

        val parser = XContentFactory.xContent(XContentType.JSON).createParser(
            xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            Json.serializeToString(searchSource)
        )

        val ssb = SearchSourceBuilder.fromXContent(parser)
        val query = QueryBuilders.boolQuery()
        if (ssb.query() == null) {
            query.must(QueryBuilders.matchAllQuery())
        } else {
            query.must(ssb.query())
        }

        getElementQuery(search)?.let {
            query.must(QueryBuilders.nestedQuery("elements", it, ScoreMode.Avg))
        }

        // Replace the query in the SearchSourceBuilder with wrapped versions
        ssb.query(query)

        if (logger.isDebugEnabled) {
            logger.debug("SEARCH : {}", Strings.toString(ssb, true, true))
        }

        return ssb
    }

    override fun search(search: AssetSearch): SearchResponse {
        val client = indexRoutingService.getProjectRestClient()
        val req = client.newSearchRequest()
        req.source(prepSearch(search))
        req.preference(getProjectId().toString())

        return client.client.search(req, RequestOptions.DEFAULT)
    }

    companion object {

        /**
         * These namespaces get removed from [AssetSpec] at creation time.
         * tmp is allowed on create only, but the data is not indexed,
         * just stored on the document.
         */
        val removeFieldsOnCreate = setOf("system", "source", "files")

        /**
         * These namespaces get removed from [Asset] at update time.
         */
        val removeFieldsOnUpdate = setOf("tmp", "temp")

        /**
         * These SearchRequest Attributes are allowed.
         */
        val allowedSearchProperties = setOf(
            "query", "from", "size", "timeout",
            "post_filter", "minscore", "suggest",
            "highlight", "collapse",
            "slice", "aggs", "aggregations", "sort"
        )

        /**
         * Maximum number of elements you can have in an asset.
         */
        val maxElementCount = 25

        private val searchModule = SearchModule(Settings.EMPTY, false, emptyList())
        val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)
        val logger: Logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    }
}
