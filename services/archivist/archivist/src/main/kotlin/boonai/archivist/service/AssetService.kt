package boonai.archivist.service

import boonai.archivist.config.ApplicationProperties
import boonai.archivist.domain.Asset
import boonai.archivist.domain.AssetIdBuilder
import boonai.archivist.domain.AssetIterator
import boonai.archivist.domain.AssetMetrics
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.BatchCreateAssetsResponse
import boonai.archivist.domain.BatchDeleteAssetResponse
import boonai.archivist.domain.BatchIndexFailure
import boonai.archivist.domain.BatchIndexResponse
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
import boonai.archivist.domain.Task
import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.UpdateAssetLabelsRequest
import boonai.archivist.domain.UpdateAssetRequest
import boonai.archivist.domain.UpdateAssetsByQueryRequest
import boonai.archivist.domain.ZpsScript
import boonai.archivist.repository.ModelDao
import boonai.archivist.repository.ModelJdbcDao
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
import boonai.common.service.security.getZmlpActor
import boonai.common.util.Json
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.elasticsearch.action.DocWriteRequest
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
    fun batchIndex(docs: Map<String, MutableMap<String, Any>>, setAnalyzed: Boolean = false): BatchIndexResponse

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
    ): Task?

    /**
     * Update the Assets contained in the [UpdateAssetLabelsRequest] with provided [Model] labels.
     */
    fun updateLabels(req: UpdateAssetLabelsRequest): BulkResponse
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
    lateinit var modelDao: ModelDao

    @Autowired
    lateinit var modelJdbcDao: ModelJdbcDao

    @Autowired
    lateinit var clipService: ClipService

    @Autowired
    lateinit var webHookPublisher: WebHookPublisherService

    override fun getAsset(id: String): Asset {
        val rest = indexRoutingService.getProjectRestClient()
        val rsp = rest.client.get(rest.newGetRequest(id), RequestOptions.DEFAULT)
        if (!rsp.isExists) {
            throw EmptyResultDataAccessException("The asset '$id' does not exist.", 1)
        }
        return Asset(rsp.id, rsp.sourceAsMap)
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

        return bulkIndexAndAnalyzePendingAssets(assets, existingAssetIds, pipeline, req.credentials)
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

        return bulkIndexAndAnalyzePendingAssets(assets, existingAssetIds, pipeline, request.credentials)
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

    override fun batchIndex(docs: Map<String, MutableMap<String, Any>>, setAnalyzed: Boolean): BatchIndexResponse {
        if (docs.isEmpty()) {
            throw IllegalArgumentException("Nothing to batch index.")
        }

        val validAssetIds = getValidAssetIds(docs.keys)
        val notFound = docs.keys.minus(validAssetIds)
        if (notFound.isNotEmpty()) {
            throw IllegalArgumentException("The asset IDs '$notFound' were not found")
        }

        val rest = indexRoutingService.getProjectRestClient()
        val bulk = BulkRequest()

        // A set of IDs where the stat changed to Analyzed.
        val stateChangedIds = mutableSetOf<String>()
        val failedAssets = mutableListOf<BatchIndexFailure>()
        val postTimelines = mutableMapOf<String, List<String>>()

        docs.forEach { (id, doc) ->
            val asset = Asset(id, doc)
            try {

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

            // Send the webhooks.s
            for (assetId in indexedIds) {
                if (assetId in stateChangedIds) {
                    webHookPublisher.handleAssetTriggers(
                        Asset(assetId, docs.getValue(assetId)), TriggerType.ASSET_ANALYZED
                    )
                } else {
                    webHookPublisher.handleAssetTriggers(
                        Asset(assetId, docs.getValue(assetId)), TriggerType.ASSET_MODIFIED
                    )
                }
            }

            // To increment ingest counters we need to know if the state changed.
            if (stateChangedIds.isNotEmpty()) {
                incrementProjectIngestCounters(stateChangedIds.intersect(indexedIds), docs)
            }

            logger.info("Post processing ${postTimelines.size} assets for deep video search.")
            if (postTimelines.isNotEmpty()) {
                val jobId = getZmlpActor().getAttr("jobId")
                if (jobId == null) {
                    logger.warn("There was post timelines to process but not jobId was found.")
                } else {
                    logger.info("Launching deep video analysis on ${postTimelines.size} assets.")
                    jobLaunchService.addMultipleTimelineAnalysisTask(
                        UUID.fromString(jobId), postTimelines
                    )
                }
            }

            BatchIndexResponse(indexedIds, failedAssets)
        } else {
            BatchIndexResponse(emptyList(), failedAssets)
        }
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

        val projectId = getProjectId()
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

        // Background removal of files into a co-routine.
        GlobalScope.launch(Dispatchers.IO + CoroutineAuthentication(SecurityContextHolder.getContext())) {
            logger.info("Removing files for ${removed.size} assets")
            for (assetId in removed) {
                try {
                    projectStorageService.recursiveDelete(
                        ProjectDirLocator(ProjectStorageEntity.ASSETS, assetId, projectId)
                    )
                } catch (ex: ProjectStorageException) {
                    logger.warn("Failed to delete files asset $assetId", ex)
                }
            }

            logger.info("Removing Clips related to removed assets")
            clipService.deleteClips(removed)
        }

        return BatchDeleteAssetResponse(removed, failures)
    }

    /**
     * Create new analysis job with the given assets, created and existing.  If no additional
     * processing is required for these assets, then no job is launched and null is returned.
     */
    private fun createAnalysisJob(
        createdAssetIds: Collection<String>,
        existingAssetIds: Collection<String>,
        pipeline: ResolvedPipeline,
        creds: Set<String>?
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
            val name = "Analyze ${createdAssetIds.size} created assets, $reprocessAssetCount existing files."
            jobLaunchService.launchJob(
                name, finalAssetList, pipeline, creds = creds, settings = mapOf("index" to true)
            )
        }
    }

    override fun createAnalysisTask(
        parentTask: InternalTask,
        createdAssetIds: Collection<String>,
        existingAssetIds: Collection<String>
    ): Task? {

        val parentScript = jobService.getZpsScript(parentTask.taskId)
        val procCount = parentScript?.execute?.size ?: 0

        // Check what assets need reprocessing at all.
        val assetIds = getAll(existingAssetIds).filter {
            assetNeedsReprocessing(it, parentScript.execute ?: listOf())
        }.map { it.id }.plus(createdAssetIds)

        return if (assetIds.isEmpty()) {
            null
        } else {

            val name = "Expand with ${assetIds.size} assets, $procCount processors."
            val parentScript = jobService.getZpsScript(parentTask.taskId)
            val newScript = ZpsScript(name, null, null, parentScript.execute, assetIds = assetIds)

            newScript.globalArgs = parentScript.globalArgs
            newScript.settings = parentScript.settings

            val newTask = jobService.createTask(parentTask, newScript)

            logger.event(
                LogObject.JOB, LogAction.EXPAND,
                mapOf(
                    "assetCount" to assetIds.size,
                    "parentTaskId" to parentTask.taskId,
                    "taskId" to newTask.id,
                    "jobId" to newTask.jobId
                )
            )
            return newTask
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
        creds: Set<String>?
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
            createAnalysisJob(created, existingAssetIds, pipeline, creds)?.id
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

        spec.tmp?.forEach { (k, v) ->
            val key = if (k.startsWith("tmp.")) {
                k
            } else {
                "tmp.$k"
            }
            asset.setAttr(key, v)
        }

        val time = java.time.Clock.systemUTC().instant().toString()
        if (asset.isAnalyzed()) {
            asset.setAttr("system.timeModified", time)
            asset.setAttr("system.state", AssetState.Analyzed.toString())
        } else {

            derivePage(asset, spec)

            if (spec.label != null) {
                if (!modelDao.existsByProjectIdAndId(projectId, spec.label.modelId)) {
                    throw java.lang.IllegalArgumentException(
                        "The Model Id ${spec.label.modelId} does not exist."
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
            addLabels.addAll(labels.map { it.modelId })
        }

        // Validate the models we're adding are legit.
        val projectId = getProjectId()
        val models = mutableSetOf<UUID>()
        addLabels.forEach {
            if (!modelDao.existsByProjectIdAndId(projectId, it)) {
                throw IllegalArgumentException("ModelId $it not found")
            } else {
                models.add(it)
            }
        }
        models.forEach { modelJdbcDao.markAsReady(it, false) }

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
        return result
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

    fun handleYoutubeVideo(path: String) {
    }

    /**
     * A cache to store parent assets so we don't load them over and over again.
     */
    private val parentAssetCache = CacheBuilder.newBuilder()
        .maximumSize(50)
        .initialCapacity(10)
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
