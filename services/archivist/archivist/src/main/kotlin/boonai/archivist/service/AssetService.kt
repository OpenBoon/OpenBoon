package boonai.archivist.service

import boonai.archivist.config.ApplicationProperties
import boonai.archivist.config.ArchivistConfiguration
import boonai.archivist.domain.Asset
import boonai.archivist.domain.AssetIdBuilder
import boonai.archivist.domain.AssetIterator
import boonai.archivist.domain.AssetMetrics
import boonai.archivist.domain.AssetMetricsEvent
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.BatchCreateAssetsResponse
import boonai.archivist.domain.BatchDeleteAssetResponse
import boonai.archivist.domain.BatchIndexFailure
import boonai.archivist.domain.BatchIndexResponse
import boonai.archivist.domain.BatchLabelBySearchRequest
import boonai.archivist.domain.BatchUpdateCustomFieldsRequest
import boonai.archivist.domain.BatchUpdateResponse
import boonai.archivist.domain.BatchUploadAssetsRequest
import boonai.archivist.domain.Field
import boonai.archivist.domain.FileExtResolver
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.InternalTask
import boonai.archivist.domain.Job
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.ProjectDirLocator
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectQuotaCounters
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.ResolvedPipeline
import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.UpdateAssetLabelsRequest
import boonai.archivist.domain.UpdateAssetLabelsRequestV4
import boonai.archivist.domain.UpdateAssetRequest
import boonai.archivist.domain.UpdateAssetsByQueryRequest
import boonai.archivist.domain.ZpsScript
import boonai.archivist.repository.DatasetDao
import boonai.archivist.security.CoroutineAuthentication
import boonai.archivist.security.getProjectId
import boonai.archivist.storage.ProjectStorageException
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.ElasticSearchErrorTranslator
import boonai.archivist.util.FileUtils
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.logging.warnEvent
import boonai.common.util.Json
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.bulk.BackoffPolicy
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.Response
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.search.sort.FieldSortBuilder
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

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
    fun getAll(ids: Collection<String>): List<Asset>

    /**
     * Take a list of asset ids and return a set of valid ones.
     */
    fun getValidAssetIds(ids: Collection<String>): Set<String>

    /**
     * Return existing asset IDs by the path/page in the AssetSpec
     */
    fun getExistingAssetIds(specs: Collection<Asset>): Set<String>

    /**
     * Get the Asset spec's existing Id.
     */
    fun getExistingAssetId(spec: AssetSpec): String?

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
    fun batchIndex(
        docs: Map<String, MutableMap<String, Any>>,
        setAnalyzed: Boolean = false,
        refresh: Boolean = false,
        create: Boolean = false
    ): BatchIndexResponse

    /**
     * Reindex a single asset.  The fully composed asset metadata must be provided,
     * not a partial update.
     */
    fun index(id: String, doc: MutableMap<String, Any>, setAnalyzed: Boolean = false): Response

    /**
     * Update a group of assets utilizing a query and a script.
     */
    fun updateByQuery(req: UpdateAssetsByQueryRequest): Response

    /**
     * Batch update the the given assets.  The [UpdateAssetRequest] can
     * utilize either a script or a document, but not both.
     */
    fun batchUpdate(batch: Map<String, UpdateAssetRequest>): BulkResponse

    fun batchUpdateCustomFields(batch: BatchUpdateCustomFieldsRequest): BatchUpdateResponse

    /**
     * Update the the given assets.  The [UpdateAssetRequest] can
     * utilize either a script or a document, but not both.
     */
    fun update(assetId: String, req: UpdateAssetRequest): Response

    /**
     * Batch delete a Set of asset ids.
     */
    fun batchDelete(ids: Set<String>): BatchDeleteAssetResponse

    /**
     * Augment the newAsset with the clip definition found in the [AssetSpec] used
     * to create it.
     *
     * @param newAsset The [Asset] we're creating
     * @param spec [AssetSpec] provided by the caller.
     */
    fun derivePage(newAsset: Asset, spec: AssetSpec)

    /**
     * Return true of the given asset would need reprocessing with the given Pipeline.
     */
    fun assetNeedsReprocessing(asset: Asset, pipeline: List<ProcessorRef>): Boolean

    /**
     * Create new child task to the given task.
     */
    fun createAnalysisTask(
        parentTask: InternalTask,
        createdAssetIds: Collection<String>,
        existingAssetIds: Collection<String>
    ): List<UUID>

    /**
     * Update the Assets contained in the [UpdateAssetLabelsRequest] with provided [Model] labels.
     */
    fun updateLabels(req: UpdateAssetLabelsRequest): BulkResponse
    fun updateLabelsV4(req: UpdateAssetLabelsRequestV4): BulkResponse
    fun batchLabelAssetsBySearch(lreq: BatchLabelBySearchRequest): Int

    /**
     * Manually set languages on an existing asset.
     */
    fun setLanguages(id: String, languages: List<String>?): Boolean
}

@Service
class AssetServiceImpl : AssetService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var projectService: ProjectService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var pipelineResolverService: PipelineResolverService

    @Autowired
    lateinit var jobLaunchService: JobLaunchService

    @Autowired
    lateinit var datasetDao: DatasetDao

    @Autowired
    lateinit var clipService: ClipService

    @Autowired
    lateinit var webHookPublisher: WebHookPublisherService

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var publisherService: PublisherService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    override fun getAsset(id: String): Asset {
        val rest = indexRoutingService.getProjectRestClient()
        val rsp = rest.client.get(rest.newGetRequest(id), RequestOptions.DEFAULT)
        if (!rsp.isExists) {
            throw EmptyResultDataAccessException("The asset '$id' does not exist.", 1)
        }
        return Asset(rsp.id, rsp.sourceAsMap)
    }

    fun getVideoAssetIds(ids: Collection<String>): Set<String> {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()
        val query = QueryBuilders.boolQuery()
            .filter(QueryBuilders.termsQuery("_id", ids))
            .filter(QueryBuilders.prefixQuery("source.mimetype", "video/"))
        req.source().size(ids.size)
        req.source().query(query)
        req.source().fetchSource(false)

        val result = mutableSetOf<String>()
        val r = rest.client.search(req, RequestOptions.DEFAULT)
        r.hits.forEach {
            result.add(it.id)
        }
        return result
    }

    override fun getValidAssetIds(ids: Collection<String>): Set<String> {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()
        val query = QueryBuilders.boolQuery()
            .filter(QueryBuilders.termsQuery("_id", ids))
        req.source().size(ids.size)
        req.source().query(query)
        req.source().fetchSource(false)

        val result = mutableSetOf<String>()
        val r = rest.client.search(req, RequestOptions.DEFAULT)
        r.hits.forEach {
            result.add(it.id)
        }
        return result
    }

    override fun getExistingAssetIds(specs: Collection<Asset>): Set<String> {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()
        val bool = QueryBuilders.boolQuery()

        specs.forEach {
            val path = it.getAttr("source.path", String::class.java)
            val subBool = QueryBuilders.boolQuery()
            subBool.must(QueryBuilders.termQuery("source.path", path))

            if (FileExtResolver.isMultiPage(FileUtils.extension(path))) {
                val page = it.getAttr("media.pageNumber", Int::class.java)
                subBool.must(QueryBuilders.termQuery("media.pageNumber", page))
            }
            bool.should(subBool)
        }

        req.source().size(specs.size)
        req.source().query(bool)
        req.source().fetchSource(false)

        val result = mutableSetOf<String>()
        val r = rest.client.search(req, RequestOptions.DEFAULT)
        r.hits.forEach {
            result.add(it.id)
        }
        return result
    }

    override fun getExistingAssetId(spec: AssetSpec): String? {

        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()
        val bool = QueryBuilders.boolQuery()

        if (spec.id == null) {
            bool.must(QueryBuilders.termQuery("source.path", spec.uri))

            // We need to know a page number here, it can be in the attrs sometimes.

            if (FileExtResolver.isMultiPage(FileUtils.extension(spec.uri))) {
                bool.must(QueryBuilders.termQuery("media.pageNumber", spec.getPageNumber()))
            }
            spec.getChecksumValue()?.let {
                bool.must(QueryBuilders.termQuery("source.checksum", it))
            }
        } else {
            bool.must(QueryBuilders.termQuery("_id", spec.id))
        }

        req.source().size(1)
        req.source().query(bool)
        req.source().fetchSource(false)

        val r = rest.client.search(req, RequestOptions.DEFAULT)
        r.hits.forEach {
            return it.id
        }
        return null
    }

    override fun getAll(ids: Collection<String>): List<Asset> {
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newSearchRequest()
        val query = QueryBuilders.boolQuery()
            .must(QueryBuilders.termsQuery("_id", ids))
        req.source().size(ids.size)
        req.source().query(query)

        val r = rest.client.search(req, RequestOptions.DEFAULT)
        return r.hits.map {
            Asset(it.id, it.sourceAsMap)
        }
    }

    override fun setLanguages(id: String, languages: List<String>?): Boolean {
        validateLanguages(languages)
        val rest = indexRoutingService.getProjectRestClient()
        val req = rest.newUpdateRequest(id)

        // Don't allow empty lists.
        val langs = if (languages.isNullOrEmpty()) {
            null
        } else {
            languages
        }

        req.doc(mapOf("media" to mapOf("languages" to langs)))
        return try {
            rest.client.update(req, RequestOptions.DEFAULT).result == DocWriteResponse.Result.UPDATED
        } catch (e: ElasticsearchStatusException) {
            logger.warn("Failed to update language for Asset ID: $id", e)
            throw EmptyResultDataAccessException("The Asset $id was not found", 1)
        }
    }

    override fun batchUpload(req: BatchUploadAssetsRequest): BatchCreateAssetsResponse {

        val pipeline = if (req.analyze) {
            pipelineResolverService.resolveModular(req.modules)
        } else {
            null
        }

        val assets = mutableListOf<Asset>()
        val existingAssetIds = mutableSetOf<String>()

        for ((idx, mpfile) in req.files.withIndex()) {
            val spec = req.assets[idx]
            // Have to make checksum before
            spec.makeChecksum(mpfile.bytes)

            val existingId = getExistingAssetId(spec)
            existingId?.let { existingAssetIds.add(it) }

            val id = existingId ?: AssetIdBuilder(spec).build()
            val asset = assetSpecToAsset(id, spec)
            asset.setAttr("source.filesize", mpfile.size)
            asset.setAttr("source.checksum", spec.getChecksumValue())

            val locator = ProjectFileLocator(
                ProjectStorageEntity.ASSETS, id, ProjectStorageCategory.SOURCE, mpfile.originalFilename
            )

            val file = projectStorageService.store(
                ProjectStorageSpec(locator, mapOf(), mpfile.bytes)
            )
            asset.setAttr("files", listOf(file))
            assets.add(asset)
        }

        return bulkIndexAndAnalyzePendingAssets(assets, existingAssetIds, pipeline, req.credentials, req.jobName)
    }

    override fun batchCreate(request: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        if (request.assets.size > 128) {
            throw IllegalArgumentException("Cannot create more than 100 assets at a time.")
        }

        val pipeline = if (request.analyze && request.task == null) {
            pipelineResolverService.resolveModular(request.modules)
        } else {
            null
        }

        // Make a list of Assets from the spec
        val assetIds = mutableSetOf<String>()
        val existingAssetIds = mutableSetOf<String>()

        val assets = request.assets.map { spec ->
            val existingId = getExistingAssetId(spec)
            existingId?.let { existingAssetIds.add(it) }

            val id = existingId ?: AssetIdBuilder(spec).build()
            assetIds.add(id)
            val asset = assetSpecToAsset(id, spec, request.task)
            asset.setAttr("system.state", request.state.name)
            asset
        }

        return bulkIndexAndAnalyzePendingAssets(
            assets,
            existingAssetIds, pipeline, request.credentials, request.jobName
        )
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

    override fun batchUpdateCustomFields(batch: BatchUpdateCustomFieldsRequest): BatchUpdateResponse {
        if (batch.size() > 1000) {
            throw IllegalArgumentException("Batch size must be under 1000")
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()

        batch.update.forEach { (id, data) ->
            val newData = mapOf(
                "custom" to
                    data.map { (k, v) ->
                        val key = if (k.startsWith("custom.")) {
                            k.substring(7)
                        } else {
                            k
                        }
                        key to v
                    }.toMap()
            )
            bulkRequest.add(rest.newUpdateRequest(id).doc(newData).retryOnConflict(10))
        }

        val rsp = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)

        return BatchUpdateResponse(
            rsp.filter {
                it.isFailed
            }.map {
                logger.warn("Failed to update asset: ${it.id}, ${it.failureMessage}")
                it.id to "Error updating asset, check your field name or data type."
            }.toMap()
        )
    }

    override fun updateByQuery(req: UpdateAssetsByQueryRequest): Response {
        val rest = indexRoutingService.getProjectRestClient()
        val request = Request("POST", "/${rest.route.indexName}/_update_by_query")
        request.setJsonEntity(Json.serializeToString(req))
        return rest.client.lowLevelClient.performRequest(request)
    }

    override fun index(id: String, doc: MutableMap<String, Any>, setAnalyzed: Boolean): Response {
        val rest = indexRoutingService.getProjectRestClient()
        val request = Request("PUT", "/${rest.route.indexName}/_doc/$id")
        val asset = Asset(id, doc)
        prepAssetForUpdate(asset)
        if (setAnalyzed) {
            asset.setAttr("system.state", AssetState.Analyzed.name)
        }
        request.setJsonEntity(Json.serializeToString(asset.document))
        return rest.client.lowLevelClient.performRequest(request)
    }

    override fun batchIndex(
        docs: Map<String, MutableMap<String, Any>>,
        setAnalyzed: Boolean,
        refresh: Boolean,
        create: Boolean
    ): BatchIndexResponse {
        if (docs.isEmpty()) {
            throw IllegalArgumentException("Nothing to batch index.")
        }

        val validAssetIds = getValidAssetIds(docs.keys)
        val notFound = docs.keys.minus(validAssetIds)
        if (!create && notFound.isNotEmpty()) {
            throw IllegalArgumentException("The asset IDs '$notFound' were not found")
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulk = BulkRequest()
        if (refresh) {
            bulk.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
        }

        // A set of IDs where the stat changed to Analyzed.
        val stateChangedIds = mutableSetOf<String>()
        val failedAssets = mutableListOf<BatchIndexFailure>()
        val postTimelines = mutableMapOf<String, List<String>>()
        val tempAssets = mutableListOf<String>()
        val assetMetrics = mutableMapOf<String, AssetMetricsEvent>()

        docs.forEach { (id, doc) ->
            val asset = Asset(id, doc)

            try {

                val modules = asset.getAttr("tmp.produced_analysis", Json.SET_OF_STRING)
                if (!modules.isNullOrEmpty()) {

                    val type = asset.getAttr("media.type", String::class.java) ?: "image"
                    val length = if (type == "video") {
                        asset.getAttr("media.length", Double::class.java) ?: 1.0
                    } else {
                        1.0
                    }

                    assetMetrics[asset.id] = AssetMetricsEvent(
                        asset.id,
                        asset.getAttr("source.path"),
                        asset.getAttr("media.type"),
                        modules,
                        length
                    )
                }

                val timelines = asset.getAttr("tmp.timelines", Json.LIST_OF_STRING)
                timelines?.let {
                    postTimelines[id] = timelines
                }

                prepAssetForUpdate(asset)
                if (setAnalyzed && !asset.isAnalyzed()) {
                    asset.setAttr("system.state", AssetState.Analyzed.name)
                    stateChangedIds.add(id)
                }

                bulk.add(
                    rest.newIndexRequest(id)
                        .create(id in notFound)
                        .source(doc)
                        .opType(DocWriteRequest.OpType.INDEX)
                )
            } catch (ex: Exception) {
                failedAssets.add(
                    BatchIndexFailure(id, asset.getAttr("source.path"), ex.message ?: "Unknown error")
                )
                logger.event(
                    LogObject.ASSET,
                    LogAction.ERROR,
                    mapOf(
                        "assetId" to id,
                        "cause" to ex.message
                    )
                )
            }

            logger.event(
                LogObject.ASSET, LogAction.BATCH_INDEX, mapOf("assetsIndexed" to bulk.numberOfActions())
            )
        }

        return if (bulk.numberOfActions() > 0) {
            val rsp = rest.client.bulk(bulk, RequestOptions.DEFAULT)
            val indexedIds = mutableListOf<String>()

            rsp.forEach {
                if (it.isFailed) {
                    logger.event(
                        LogObject.ASSET,
                        LogAction.ERROR,
                        mapOf(
                            "assetId" to it.id,
                            "cause" to it.failureMessage
                        )
                    )
                    failedAssets.add(BatchIndexFailure(it.id, null, it.failureMessage))

                    // Remove from timeline processing if the asset
                    // can't be indexed.
                    postTimelines.remove(it.id)
                } else {
                    indexedIds.add(it.id)
                }
            }

            // Emit metrics
            failedAssets.forEach { assetMetrics.remove(it.assetId) }
            emitMetrics(assetMetrics.values)

            for (assetId in indexedIds) {
                if (assetId in stateChangedIds) {
                    webHookPublisher.handleAssetTriggers(
                        Asset(assetId, docs.getValue(assetId)), TriggerType.AssetAnalyzed
                    )
                } else {
                    webHookPublisher.handleAssetTriggers(
                        Asset(assetId, docs.getValue(assetId)), TriggerType.AssetModified
                    )
                }
            }

            // To increment ingest counters we need to know if the state changed.3
            if (stateChangedIds.isNotEmpty()) {
                incrementProjectIngestCounters(stateChangedIds.intersect(indexedIds), docs)
            }

            BatchIndexResponse(indexedIds, failedAssets)
        } else {
            BatchIndexResponse(emptyList(), failedAssets)
        }
    }

    fun emitMetrics(metrics: Collection<AssetMetricsEvent>) {
        val projectId = getProjectId().toString()
        val msg = PubsubMessage.newBuilder()
            .putAttributes("project_id", projectId)
            .putAttributes("type", "assets-indexed")
            .setData(ByteString.copyFromUtf8(Json.serializeToString(metrics)))
            .build()

        publisherService.publish("metrics", msg)
    }

    private fun deleteTemporaryAssets(
        indexedIds: Set<String>
    ): BatchDeleteAssetResponse {
        return batchDelete(indexedIds.toSet())
    }

    override fun batchDelete(ids: Set<String>): BatchDeleteAssetResponse {

        val maximumBatchSize = properties.getInt("archivist.assets.deletion-max-batch-size")
        if (ids.size > maximumBatchSize) {
            throw IllegalArgumentException("Maximum allowed size exceeded. Maximum batch size for delete: $maximumBatchSize")
        }

        val rest = indexRoutingService.getProjectRestClient()
        var deletedAssets = getAll(ids).map { it.id to it }.toMap()
        val query = QueryBuilders.termsQuery("_id", ids)
        val rsp = rest.client.deleteByQuery(
            DeleteByQueryRequest(rest.route.indexName)
                .setQuery(query),
            RequestOptions.DEFAULT
        )

        val failures = rsp.bulkFailures.map { it.id }
        val removed = ids.subtract(failures).toList()

        for (failure in rsp.bulkFailures) {
            logger.warnEvent(
                LogObject.ASSET, LogAction.DELETE, failure.message,
                mapOf("assetId" to failure.id)
            )
        }

        val projectQuotaCounters = ProjectQuotaCounters()
        for (removed in removed) {
            projectQuotaCounters.countForDeletion(deletedAssets.getValue(removed))
            logger.event(
                LogObject.ASSET, LogAction.DELETE, mapOf("assetId" to removed)
            )
        }
        projectService.incrementQuotaCounters(projectQuotaCounters)

        if (removed.isNotEmpty()) {
            if (ArchivistConfiguration.unittest) {
                deleteAssociatedFiles(removed)
            } else {
                GlobalScope.launch(Dispatchers.IO + CoroutineAuthentication(SecurityContextHolder.getContext())) {
                    deleteAssociatedFiles(removed)
                }
            }
        }
        return BatchDeleteAssetResponse(removed, failures)
    }

    fun deleteAssociatedFiles(assetIds: List<String>) {
        val projectId = getProjectId()
        // This will fail in tests because the indexRoute is not committed.

        logger.info("Removing files for ${assetIds.size} assets")
        for (assetId in assetIds) {
            try {
                projectStorageService.recursiveDelete(
                    ProjectDirLocator(ProjectStorageEntity.ASSETS, assetId, projectId)
                )
            } catch (ex: ProjectStorageException) {
                logger.warn("Failed to delete files asset $assetId", ex)
            }
        }
        clipService.deleteClips(assetIds)
    }

    /**
     * Create new analysis job with the given assets, created and existing.  If no additional
     * processing is required for these assets, then no job is launched and null is returned.
     */
    private fun createAnalysisJob(
        createdAssetIds: Collection<String>,
        existingAssetIds: Collection<String>,
        pipeline: ResolvedPipeline,
        creds: Set<String>?,
        jobName: String?
    ): Job? {

        // Validate the assets need reprocessing
        val assetIds = getAll(existingAssetIds).filter {
            assetNeedsReprocessing(it, pipeline.execute)
        }.map { it.id }

        val reprocessAssetCount = assetIds.size
        val finalAssetList = assetIds.plus(createdAssetIds)

        return if (finalAssetList.isEmpty()) {
            null
        } else {
            val merge = jobName != null
            val name = "Analyze ${createdAssetIds.size} created assets, $reprocessAssetCount existing files."

            jobLaunchService.launchJob(
                jobName ?: name,
                finalAssetList,
                pipeline,
                merge = merge,
                creds = creds,
                settings = mapOf("index" to true)
            )
        }
    }

    override fun createAnalysisTask(
        parentTask: InternalTask,
        createdAssetIds: Collection<String>,
        existingAssetIds: Collection<String>
    ): List<UUID> {

        val parentScript = jobService.getZpsScript(parentTask.taskId)
        val procCount = parentScript?.execute?.size ?: 0

        val assetIds = getAll(existingAssetIds).filter {
            assetNeedsReprocessing(it, parentScript.execute ?: listOf())
        }.map { it.id }.plus(createdAssetIds)

        return if (assetIds.isEmpty()) {
            return emptyList()
        } else {

            val result = mutableListOf<UUID>()
            val videos = getVideoAssetIds(assetIds)
            for (videoAsset in videos) {
                val name = "Processing 1 video assets with $procCount processors."
                val parentScript = jobService.getZpsScript(parentTask.taskId)
                val newScript = ZpsScript(name, null, null, parentScript.execute, assetIds = listOf(videoAsset))
                newScript.globalArgs = parentScript.globalArgs
                newScript.settings = parentScript.settings
                val newTask = jobService.createTask(parentTask, newScript)
                result.add(newTask.id)

                logger.event(
                    LogObject.JOB, LogAction.EXPAND,
                    mapOf(
                        "assetCount" to assetIds.size,
                        "parentTaskId" to parentTask.taskId,
                        "taskId" to newTask.id,
                        "jobId" to newTask.jobId
                    )
                )
            }

            val otherTypes = assetIds.minus(videos)
            if (otherTypes.isNotEmpty()) {

                val name = "Processing ${otherTypes.size} assets with $procCount processors."
                val parentScript = jobService.getZpsScript(parentTask.taskId)
                val newScript = ZpsScript(name, null, null, parentScript.execute, assetIds = otherTypes)

                newScript.globalArgs = parentScript.globalArgs
                newScript.settings = parentScript.settings
                val newTask = jobService.createTask(parentTask, newScript)
                result.add(newTask.id)

                logger.event(
                    LogObject.JOB, LogAction.EXPAND,
                    mapOf(
                        "assetCount" to assetIds.size,
                        "parentTaskId" to parentTask.taskId,
                        "taskId" to newTask.id,
                        "jobId" to newTask.jobId
                    )
                )
            }

            return result
        }
    }

    override fun derivePage(newAsset: Asset, spec: AssetSpec) {
        if (spec.uri.startsWith("asset:")) {
            spec.parentAsset = parentAssetCache.get(spec.uri.substring(6))
        }

        spec.parentAsset?.also { parentAsset ->
            val realFilePath = parentAsset.getAttr("source.path", String::class.java)
            spec.uri = realFilePath
                ?: throw IllegalArgumentException("The source asset for page cannot have a null URI.")

            // Not a multipage asset.
            if (!FileExtResolver.isMultiPage(FileUtils.extension(realFilePath))) {
                throw IllegalArgumentException("The source file does not support multiple pages.")
            }

            // Copy over source files if any
            val files = parentAsset.getAttr("files", FileStorage.JSON_LIST_OF) ?: listOf()
            val sourceFiles = files.let {
                it.filter { file ->
                    file.category == ProjectStorageCategory.SOURCE
                }
            }

            if (parentAsset.attrExists("source.filesize")) {
                newAsset.setAttr("source.filesize", parentAsset.getAttr("source.filesize"))
            }

            if (parentAsset.attrExists("source.checksum")) {
                newAsset.setAttr("source.checksum", parentAsset.getAttr("source.checksum"))
            }

            // Set the files property. The source files will reference the
            // original asset id.
            newAsset.setAttr("files", sourceFiles)

            if (indexRoutingService.getProjectRestClient().route.majorVersion > 4) {
                newAsset.setAttr("media.pageNumber", spec.getPageNumber())
                newAsset.setAttr("media.pageStack", parentAsset.id)
            }
        } ?: run {
            if (FileExtResolver.isMultiPage(FileUtils.extension(spec.getRealPath()))) {
                if (indexRoutingService.getProjectRestClient().route.majorVersion > 4) {
                    newAsset.setAttr("media.pageNumber", spec.getPageNumber())
                    newAsset.setAttr("media.pageStack", AssetIdBuilder(spec.makePageOne()).build())
                }
            }
        }
    }

    /**
     * Indexes newly created assets and passes the results on
     * to [createAnalysisJob].  If there are no new assets and all existing assets
     * are already processed, then this is basically a no-op and no processing jobs
     * is launched.
     *
     * @param newAssets The newly created assets.
     * @param existingAssetIds The asset Ids that already existed.
     * @param pipeline The pipeline to execute as a in List<ProcessorRef>.
     * @param creds Any credentials that should be associated with the running job.
     * @return BatchCreateAssetsResponse
     */
    private fun bulkIndexAndAnalyzePendingAssets(
        newAssets: List<Asset>,
        existingAssetIds: Collection<String>,
        pipeline: ResolvedPipeline?,
        creds: Set<String>?,
        jobName: String?,
    ): BatchCreateAssetsResponse {

        val rest = indexRoutingService.getProjectRestClient()
        val bulkRequest = BulkRequest()
        bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        // Add new assets to the bulk request.
        var validBulkRequest = false
        newAssets.forEach {
            if (it.id !in existingAssetIds) {
                val ireq = rest.newIndexRequest(it.id)
                ireq.opType(DocWriteRequest.OpType.CREATE)
                ireq.source(it.document)
                bulkRequest.add(ireq)
                validBulkRequest = true
            }
        }

        val created = mutableListOf<String>()
        val failures = mutableListOf<BatchIndexFailure>()

        // If there is a valid bulk request, commit assets to ES.
        if (validBulkRequest) {
            val bulk = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)
            bulk.items.forEachIndexed { idx, it ->

                if (it.isFailed) {
                    val path = newAssets[idx].getAttr<String?>("source.path")
                    val msg = ElasticSearchErrorTranslator.translate(it.failureMessage)
                    logger.warnEvent(
                        LogObject.ASSET, LogAction.CREATE, "failed to create asset $path, $msg"
                    )
                    failures.add(BatchIndexFailure(it.id, path, msg))
                } else {
                    created.add(it.id)
                    logger.event(
                        LogObject.ASSET, LogAction.CREATE,
                        mapOf("uploaded" to true, "createdAssetId" to it.id)
                    )
                }
            }
        }

        // Launch analysis job.
        val jobId = if (pipeline != null) {
            createAnalysisJob(created, existingAssetIds, pipeline, creds, jobName)?.id
        } else {
            null
        }

        return BatchCreateAssetsResponse(failures, created, existingAssetIds, jobId)
    }

    fun assetSpecToAsset(id: String, spec: AssetSpec, task: InternalTask? = null): Asset {
        val asset = Asset(id)
        val projectId = getProjectId()

        spec.custom?.forEach { (k, v) ->
            if (!k.matches(Field.NAME_REGEX)) {
                throw IllegalArgumentException(
                    "Field names '$k' must be alpha-numeric, underscores/dashes are allowed."
                )
            }
            asset.setAttr("custom.$k", v)
        }

        // Spec.attrs can only be set locally via tests, not via REST endpoints.
        spec.attrs?.forEach { (k, v) ->
            asset.setAttr(k, v)
        }

        // Set temp vars
        spec.tmp?.forEach { (k, v) ->
            val key = if (k.startsWith("tmp.")) {
                k
            } else {
                "tmp.$k"
            }
            asset.setAttr(key, v)
        }

        // Set language
        spec.languages?.let {
            validateLanguages(it)
            asset.setAttr("media.languages", it)
        }

        val time = java.time.Clock.systemUTC().instant().toString()
        if (asset.isAnalyzed()) {
            asset.setAttr("system.timeModified", time)
            asset.setAttr("system.state", AssetState.Analyzed.toString())
        } else {

            derivePage(asset, spec)

            if (spec.label != null) {
                if (!datasetDao.existsByProjectIdAndId(projectId, spec.label.datasetId)) {
                    throw java.lang.IllegalArgumentException(
                        "The Dataset ${spec.label.datasetId} does not exist."
                    )
                }
                asset.addLabels(listOf(spec.label))
            }

            if (spec.getRealPath().startsWith("https://www.youtube.com/watch?")) {
                val ytUri = URI.create(spec.uri)
                val ytCmp = UriComponentsBuilder.fromUri(ytUri).build()
                val tyId = ytCmp.queryParams["v"]?.get(0) ?: throw IllegalArgumentException("Invalid Youtube URL")

                asset.setAttr("source.path", "https://www.youtube.com/watch/$tyId.mp4")
                asset.setAttr("source.filename", "$tyId.mp4")
                asset.setAttr("source.extension", "mp4")
                asset.setAttr("source.mimetype", "video/mp4")
            } else {
                asset.setAttr("source.path", spec.getRealPath())
                asset.setAttr("source.filename", FileUtils.filename(spec.getRealPath()))

                val ext = FileUtils.extension(spec.getRealPath())
                asset.setAttr("source.extension", ext)

                if (indexRoutingService.getProjectRestClient().route.majorVersion > 4) {
                    if (FileExtResolver.getType(ext) == "video") {
                        asset.setAttr("deepSearch", "video")
                    }
                }

                val mediaType = FileExtResolver.getMediaType(spec.uri)
                asset.setAttr("source.mimetype", mediaType)
            }

            asset.setAttr("system.projectId", projectId)
            task?.let {
                asset.setAttr("system.dataSourceId", it.dataSourceId)
                asset.setAttr("system.jobId", it.jobId)
                asset.setAttr("system.taskId", it.taskId)
            }
            asset.setAttr("system.timeCreated", time)
            asset.setAttr("system.state", AssetState.Pending.toString())
        }
        if (!asset.attrExists("source.path") || asset.getAttr<String?>("source.path") == null) {
            throw java.lang.IllegalStateException("The source.path attribute cannot be null")
        }

        return asset
    }

    fun prepAssetForUpdate(asset: Asset): Asset {

        val time = java.time.Clock.systemUTC().instant().toString()

        // Remove these which are used for temp attrs
        removeFieldsOnUpdate.forEach {
            asset.removeAttr(it)
        }

        // Assets must have media type in order to Increment Project Ingest Counters
        if (!asset.attrExists("media.type")) {
            val ext = FileUtils.extension(
                (asset.getAttr<String>("source.path"))
            )
            asset.setAttr("media.type", FileExtResolver.getType(ext))
        }

        // Update various system properties.
        asset.setAttr("system.projectId", getProjectId().toString())
        asset.setAttr("system.timeModified", time)

        return asset
    }

    override fun assetNeedsReprocessing(asset: Asset, pipeline: List<ProcessorRef>): Boolean {
        // If the asset has no metrics, we need reprocessing
        if (!asset.attrExists("metrics")) {
            return true
        }

        val metrics = asset.getAttr("metrics", AssetMetrics::class.java)
        val oldPipeline = metrics?.pipeline

        // If the old pipeline is somehow null or empty, needs preprocessing.
        if (oldPipeline.isNullOrEmpty()) {
            return true
        } else {

            // If there was an error, we'll reprocess.
            if (oldPipeline.any { e -> e.error != null }) {
                logger.info("Reprocessing asset ${asset.id}, errors detected.")
                return true
            }

            if (pipeline.any { m -> m.force }) {
                logger.info("Reprocessing asset ${asset.id}, forced modules detected.")
                return true
            }

            // Now comes the slow check.  Compare the new processing to the metrics
            // and determine if new processing needs to be done
            val existing = oldPipeline.map { m -> "${m.processor}${m.checksum}" }.toSet()
            val future = pipeline.map { m -> "${m.className}${m.getChecksum()}" }.toSet()

            val newProcessing = future.subtract(existing)
            return if (newProcessing.isNotEmpty()) {
                logger.info("Reprocessing asset ${asset.id}, requires: $newProcessing")
                true
            } else {
                logger.info("Not reprocessing Asset ${asset.id}, all requested metadata exists.")
                false
            }
        }
    }

    override fun updateLabels(req: UpdateAssetLabelsRequest): BulkResponse {
        val bulkSize = 100
        val maxAssets = 1000L
        if (req.add?.size ?: 0 > maxAssets) {
            throw IllegalArgumentException(
                "Cannot add labels to more than $maxAssets assets at a time."
            )
        }

        if (req.remove?.size ?: 0 > maxAssets) {
            throw IllegalArgumentException(
                "Cannot remove labels from more than $maxAssets assets at a time."
            )
        }

        // Gather up unique Assets and Model
        val allAssetIds = (req.add?.keys ?: setOf()) + (req.remove?.keys ?: setOf())
        val addLabels = mutableSetOf<UUID>()
        req.add?.values?.forEach { labels ->
            addLabels.addAll(labels.map { it.datasetId })
        }

        // Validate the models we're adding are legit.
        val projectId = getProjectId()
        val datasets = mutableSetOf<UUID>()
        addLabels.forEach {
            if (!datasetDao.existsByProjectIdAndId(projectId, it)) {
                throw IllegalArgumentException("Dataset $it not found")
            } else {
                datasets.add(it)
            }
        }

        // Build a search for assets.
        val rest = indexRoutingService.getProjectRestClient()
        val builder = rest.newSearchBuilder()

        builder.source.query(QueryBuilders.termsQuery("_id", allAssetIds))
        builder.source.sort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
        builder.source.size(bulkSize)
        builder.request.scroll(TimeValue(60000))
        builder.source.fetchSource("labels", null)

        // Build a bulk update.
        val rsp = rest.client.search(builder.request, RequestOptions.DEFAULT)
        val bulk = BulkRequest()
        // Need an IMMEDIATE refresh policy or else we could end
        // up losing labels with subsequent calls.
        bulk.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        for (asset in AssetIterator(rest.client, rsp, maxAssets)) {
            val removeLabels = req.remove?.get(asset.id)
            removeLabels?.let {
                asset.removeLabels(it)
                bulk.add(rest.newUpdateRequest(asset.id).doc(asset.document))
            }

            val addLabels = req.add?.get(asset.id)
            addLabels?.let {
                asset.addLabels(it)
                bulk.add(rest.newUpdateRequest(asset.id).doc(asset.document))
            }
        }
        val result = rest.client.bulk(bulk, RequestOptions.DEFAULT)
        if (result.hasFailures()) {
            logger.warn("Some failures occurred during asset labeling operation {}")
            for (f in result.items) {
                if (f.isFailed) {
                    logger.warn("Asset ${f.id} failed to update label ${f.failureMessage}")
                }
            }
        }
        datasets.forEach {
            datasetService.markModelsUnready(it)
        }
        return result
    }

    override fun updateLabelsV4(req: UpdateAssetLabelsRequestV4): BulkResponse {
        val bulkSize = 100
        val maxAssets = 1000L
        if (req.add?.size ?: 0 > maxAssets) {
            throw IllegalArgumentException(
                "Cannot add labels to more than $maxAssets assets at a time."
            )
        }

        if (req.remove?.size ?: 0 > maxAssets) {
            throw IllegalArgumentException(
                "Cannot remove labels from more than $maxAssets assets at a time."
            )
        }

        // Gather up unique Assets and Model
        val allAssetIds = (req.add?.keys ?: setOf()) + (req.remove?.keys ?: setOf())
        val allDataSets = mutableSetOf<UUID>()
        req.add?.values?.forEach { label ->
            allDataSets.add(label.datasetId)
        }
        req.remove?.values?.forEach { label ->
            allDataSets.add(label.datasetId)
        }

        // Validate the datasets we're adding are legit.
        val projectId = getProjectId()
        allDataSets.forEach {
            if (!datasetDao.existsByProjectIdAndId(projectId, it)) {
                throw IllegalArgumentException("Dataset $it not found")
            }
        }

        // Build a search for assets.
        val rest = indexRoutingService.getProjectRestClient()
        val builder = rest.newSearchBuilder()

        builder.source.query(QueryBuilders.termsQuery("_id", allAssetIds))
        builder.source.sort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
        builder.source.size(bulkSize)
        builder.request.scroll(TimeValue(60000))
        builder.source.fetchSource("labels", null)

        // Build a bulk update.
        val rsp = rest.client.search(builder.request, RequestOptions.DEFAULT)
        val bulk = BulkRequest()

        // Need an IMMEDIATE refresh policy or else we could end
        // up losing labels with subsequent calls.
        bulk.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        for (asset in AssetIterator(rest.client, rsp, maxAssets)) {
            val removeLabels = req.remove?.get(asset.id)
            removeLabels?.let {
                asset.removeLabel(it)
            }

            val addLabels = req.add?.get(asset.id)
            addLabels?.let {
                asset.addLabel(it)
            }

            bulk.add(rest.newUpdateRequest(asset.id).doc(asset.document))
        }

        val result = rest.client.bulk(bulk, RequestOptions.DEFAULT)
        if (result.hasFailures()) {
            logger.warn("Some failures occurred during asset labeling operation {}")
            for (f in result.items) {
                if (f.isFailed) {
                    logger.warn("Asset ${f.id} failed to update label ${f.failureMessage}")
                }
            }
        }

        allDataSets.forEach {
            datasetService.markModelsUnready(it)
        }

        return result
    }

    override fun batchLabelAssetsBySearch(lreq: BatchLabelBySearchRequest): Int {

        if (lreq.maxAssets > 10000 || lreq.maxAssets < 1) {
            throw IllegalArgumentException("Invalid maxAsssts value, must be between 1 and 10000")
        }

        val rest = indexRoutingService.getProjectRestClient()

        // Make sure not to touch the sort or else you'll end up
        // tagging the wrong assets.
        val req = rest.newSearchRequest()
        req.source(assetSearchService.mapToSearchSourceBuilder(lreq.search))
        req.scroll(TimeValue(60000))
        req.source().size(100)
        req.source().fetchSource("labels", null)

        val listener: BulkProcessor.Listener = object : BulkProcessor.Listener {
            override fun beforeBulk(executionId: Long, request: BulkRequest) {}
            override fun afterBulk(
                executionId: Long,
                request: BulkRequest,
                response: BulkResponse
            ) {
            }

            override fun afterBulk(
                executionId: Long,
                request: BulkRequest,
                failure: Throwable
            ) {
                logger.warn("Bulk labeling error ", failure)
            }
        }

        val builder = BulkProcessor.builder(
            { request, bulkListener -> rest.client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener) },
            listener
        )
        builder.setBulkActions(100)
        builder.setConcurrentRequests(2)
        builder.setBackoffPolicy(
            BackoffPolicy
                .constantBackoff(TimeValue.timeValueSeconds(1L), 3)
        )
        val bulk = builder.build()

        var count = 0
        for (
            asset in AssetIterator(
                rest.client,
                rest.client.search(req, RequestOptions.DEFAULT), lreq.maxAssets.toLong()
            )
        ) {
            asset.addLabel(lreq.label)
            bulk.add(rest.newUpdateRequest(asset.id).doc(asset.document))
            count += 1
        }
        bulk.close()
        return count
    }

    /**
     * Increment the project counters for the given collection of asset ids.
     */
    fun incrementProjectIngestCounters(ids: Collection<String>, docs: Map<String, MutableMap<String, Any>>) {
        val counters = ProjectQuotaCounters()
        ids.forEach {
            counters.count(Asset(it, docs.getValue(it)))
        }
        projectService.incrementQuotaCounters(counters)
    }

    fun validateLanguages(langs: List<String>?) {
        langs?.forEach {
            if (!it.matches(Regex("^[a-z]{2}-[A-Z]{2}$"))) {
                throw IllegalArgumentException("Invalid language code format: $it")
            }
        }
    }

    /**
     * A cache to store parent assets so we don't load them over and over again.
     */
    private val parentAssetCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .initialCapacity(20)
        .concurrencyLevel(4)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, Asset>() {
            @Throws(Exception::class)
            override fun load(assetId: String): Asset {
                return getAsset(assetId)
            }
        })

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
        val removeFieldsOnCreate =
            setOf("system", "source", "files", "elements", "metrics", "labels", "analysis")
    }
}
