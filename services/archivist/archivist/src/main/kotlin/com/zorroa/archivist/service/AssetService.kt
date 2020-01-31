package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetFileLocator
import com.zorroa.archivist.domain.AssetIdBuilder
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.Clip
import com.zorroa.archivist.domain.Element
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.domain.UpdateAssetsByQueryRequest
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.search.SearchSourceMapper
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.ElasticSearchErrorTranslator
import com.zorroa.archivist.util.FileUtils
import com.zorroa.zmlp.util.Json
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.service.logging.warnEvent
import org.elasticsearch.client.Response
import org.elasticsearch.common.Strings
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.BulkByScrollResponse
import org.elasticsearch.index.reindex.DeleteByQueryRequest
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
     * Get an Asset by ID.
     */
    fun getAsset(assetId: String): Asset

    /**
     * Get all assets by their unique ID.
     */
    fun getAll(ids: List<String>): List<Asset>

    /**
     * Take a list of asset ids and return a set of valid ones.
     */
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
     * Handle a batch upload request and return a BatchCreateAssetsResponse
     *
     * @param req: a BatchUploadAssetsRequest
     * @returns a BatchCreateAssetsResponse
     */
    fun batchUpload(req: BatchUploadAssetsRequest): BatchCreateAssetsResponse

    /**
     * Batch re-indexes the given batch of Assets. The fully composed asset must be
     * provided, not a partial update.
     *
     * The request is a map, formatted as:
     *
     * {
     *    "id1": { document },
     *    "id2": { document },
     *    etc, etc
     * }
     *
     * @param req: A Map<String, Map<String, Any>>
     * @return An ES [BulkResponse] which contains the result of the operation.
     *
     */
    fun batchIndex(docs: Map<String, MutableMap<String, Any>>): BulkResponse

    /**
     * Reindex a single asset.  The fully composed asset metadata must be provided,
     * not a partial update.
     */
    fun index(id: String, doc: MutableMap<String, Any>): Response

    /**
     * Update a group of assets utilizing a query and a script.
     */
    fun updateByQuery(req: UpdateAssetsByQueryRequest): Response

    /**
     * Batch update the the given assets.  The [UpdateAssetRequest] can
     * utilize either a script or a document, but not both.
     */
    fun batchUpdate(batch: Map<String, UpdateAssetRequest>): BulkResponse

    /**
     * Update the the given assets.  The [UpdateAssetRequest] can
     * utilize either a script or a document, but not both.
     */
    fun update(assetId: String, req: UpdateAssetRequest): Response

    /**
     * Delete the given asset id.
     */
    fun delete(assetId: String): Response

    /**
     * Delete assets by query.
     */
    fun deleteByQuery(req: Map<String, Any>): BulkByScrollResponse

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

    override fun batchUpload(req: BatchUploadAssetsRequest): BatchCreateAssetsResponse {

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

        return processBulkRequest(bulkRequest, assets, pipeline, req.credentials)
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

        return processBulkRequest(bulkRequest, assets, pipeline, request.credentials)
    }

    override fun update(assetId: String, req: UpdateAssetRequest): Response {
        val rest = indexRoutingService.getProjectRestClient()
        val request = Request("POST", "/${rest.route.indexName}/_update/$assetId")
        request.setJsonEntity(Json.serializeToString(req))

        logger.event(
            LogObject.ASSET, LogAction.UPDATE, mapOf("assetId" to assetId)
        )

        return rest.client.lowLevelClient.performRequest(request)
    }

    override fun batchUpdate(batch: Map<String, UpdateAssetRequest>): BulkResponse {
        if (batch.size > 1000) {
            throw IllegalArgumentException("Batch size must be under 1000")
        }
        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()
        batch.forEach { (id, req) ->
            bulkRequest.add(rest.newUpdateRequest(id).doc(req.doc))
        }

        logger.event(
            LogObject.ASSET, LogAction.BATCH_UPDATE, mapOf("assetsUpdated" to batch.size)
        )

        return rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
    }

    override fun updateByQuery(req: UpdateAssetsByQueryRequest): Response {
        val rest = indexRoutingService.getProjectRestClient()
        val request = Request("POST", "/${rest.route.indexName}/_update_by_query")
        request.setJsonEntity(Json.serializeToString(req))
        return rest.client.lowLevelClient.performRequest(request)
    }

    override fun index(id: String, doc: MutableMap<String, Any>): Response {
        val rest = indexRoutingService.getProjectRestClient()
        val request = Request("PUT", "/${rest.route.indexName}/_doc/$id")
        prepAssetForUpdate(id, doc)
        request.setJsonEntity(Json.serializeToString(doc))
        return rest.client.lowLevelClient.performRequest(request)
    }

    override fun batchIndex(docs: Map<String, MutableMap<String, Any>>): BulkResponse {
        if (docs.isEmpty()) {
            throw IllegalArgumentException("Nothing to batch index.")
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulk = BulkRequest()

        docs.forEach { (id, doc) ->
            prepAssetForUpdate(id, doc)
            bulk.add(
                rest.newIndexRequest(id)
                    .source(doc)
                    .opType(DocWriteRequest.OpType.INDEX)
            )
        }

        logger.event(
            LogObject.ASSET, LogAction.BATCH_INDEX, mapOf("assetsIndexed" to docs.size)
        )

        return rest.client.bulk(bulk, RequestOptions.DEFAULT)
    }

    override fun delete(id: String): Response {
        val rest = indexRoutingService.getProjectRestClient()
        val request = Request("DELETE", "/${rest.route.indexName}/_doc/$id")

        logger.event(
            LogObject.ASSET, LogAction.DELETE, mapOf("assetId" to id)
        )

        return rest.client.lowLevelClient.performRequest(request)
    }

    override fun deleteByQuery(req: Map<String, Any>): BulkByScrollResponse {
        val rest = indexRoutingService.getProjectRestClient()

        return rest.client.deleteByQuery(
            DeleteByQueryRequest(rest.route.indexName)
                .setQuery(SearchSourceMapper.convert(req).query()), RequestOptions.DEFAULT)
    }

    fun createAnalysisJob(assetIds: List<String>, processors: List<ProcessorRef>, creds: Set<String>?): Job {
        val name = "Analyze ${assetIds.size} created assets"
        val script = ZpsScript(name, null, getAll(assetIds), processors)
        val spec = JobSpec(name, script, credentials = creds)

        return jobService.create(spec)
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

    private fun processBulkRequest(
        bulkRequest: BulkRequest,
        assets: List<Asset>,
        procs: List<ProcessorRef>?,
        creds: Set<String>?
    ):
        BatchCreateAssetsResponse {
        val rest = indexRoutingService.getProjectRestClient()
        val bulk = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)

        val created = mutableListOf<String>()
        val failures = mutableListOf<Map<String, String?>>()

        bulk.items.forEachIndexed { idx, it ->

            if (it.isFailed) {
                val path = assets[idx].getAttr<String?>("source.path")
                val msg = ElasticSearchErrorTranslator.translate(it.failureMessage)
                logger.warnEvent(
                    LogObject.ASSET, LogAction.CREATE, "failed to create asset $path, $msg"
                )
                failures.add(
                    mapOf(
                        "path" to path,
                        "failureMessage" to msg)
                )
            } else {
                created.add(it.id)
                logger.event(
                    LogObject.ASSET, LogAction.CREATE,
                    mapOf("uploaded" to true, "createdAssetId" to it.id)
                )
            }
        }

        // Launch analysis job.
        val jobId = if (procs != null && created.size > 0) {
            createAnalysisJob(created, procs, creds).id
        } else {
            null
        }

        val response = Json.Mapper.readValue(Strings.toString(bulk), Json.MUTABLE_MAP)
        return BatchCreateAssetsResponse(response, failures, created, jobId)
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

        if (!asset.attrExists("source.path") || asset.getAttr<String?>("source.path") == null) {
            throw java.lang.IllegalStateException("The source.path attribute cannot be null")
        }

        return asset
    }

    fun prepAssetForUpdate(id: String, map: MutableMap<String, Any>?) {
        if (map == null) {
            return
        }

        val time = java.time.Clock.systemUTC().instant().toString()
        val asset = Asset(id, map)

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

        // Uniquify the elements
        if (asset.attrExists("elements")) {
            val elements = asset.getAttr("elements", Element.JSON_SET_OF)
            if (elements != null && elements.size > AssetServiceImpl.maxElementCount) {
                throw IllegalStateException(
                    "Asset ${asset.id} has to many elements, > ${AssetServiceImpl.maxElementCount}"
                )
            }
            asset.setAttr("elements", elements)
        }

        // Update various system properties.
        asset.setAttr("system.projectId", getProjectId().toString())
        asset.setAttr("system.timeModified", time)
        asset.setAttr("system.state", AssetState.Analyzed.toString())
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)

        /**
         * Files to remove before update/index.
         */
        val removeFieldsOnUpdate = setOf("tmp", "temp")

        /**
         * These namespaces get removed from [AssetSpec] at creation time.
         * tmp is allowed on create only, but the data is not indexed,
         * just stored on the document.
         */
        val removeFieldsOnCreate = setOf("system", "source", "files")

        /**
         * Maximum number of elements you can have in an asset.
         */
        const val maxElementCount = 25
    }
}
