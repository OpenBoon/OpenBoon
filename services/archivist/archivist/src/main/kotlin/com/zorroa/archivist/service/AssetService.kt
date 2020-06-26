package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetCounters
import com.zorroa.archivist.domain.AssetIdBuilder
import com.zorroa.archivist.domain.AssetIterator
import com.zorroa.archivist.domain.AssetMetrics
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchDeleteAssetResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.Clip
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileExtResolver
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.UpdateAssetLabelsRequest
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectQuotaCounters
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.domain.UpdateAssetsByQueryRequest
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.DataSetDao
import com.zorroa.archivist.security.CoroutineAuthentication
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.storage.ProjectStorageException
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.ElasticSearchErrorTranslator
import com.zorroa.archivist.util.FileUtils
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.service.logging.warnEvent
import com.zorroa.zmlp.util.Json
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
import java.util.UUID

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
    fun batchIndex(docs: Map<String, MutableMap<String, Any>>, setAnalyzed: Boolean = false): BulkResponse

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
    fun deriveClip(newAsset: Asset, spec: AssetSpec): Clip

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
     * Update the Assets contained in the [UpdateAssetLabelsRequest] with provided [DataSet] labels.
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
    lateinit var dataSetDao: DataSetDao

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

        for ((idx, mpfile) in req.files.withIndex()) {
            val spec = req.assets[idx]
            val idgen = AssetIdBuilder(spec)
                .checksum(mpfile.bytes)
            val id = idgen.build()
            val asset = assetSpecToAsset(id, spec)
            asset.setAttr("source.filesize", mpfile.size)
            asset.setAttr("source.checksum", idgen.checksum)

            val locator = ProjectFileLocator(
                ProjectStorageEntity.ASSETS, id, ProjectStorageCategory.SOURCE, mpfile.originalFilename
            )

            val file = projectStorageService.store(
                ProjectStorageSpec(locator, mapOf(), mpfile.bytes)
            )
            asset.setAttr("files", listOf(file))
            assets.add(asset)
        }

        val existingAssetIds = getValidAssetIds(assets.map { it.id })
        return bulkIndexAndAnalyzePendingAssets(assets, existingAssetIds, pipeline, req.credentials)
    }

    override fun batchCreate(request: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        if (request.assets.size > 100) {
            throw IllegalArgumentException("Cannot create more than 100 assets at a time.")
        }

        val pipeline = if (request.analyze && request.task == null) {
            pipelineResolverService.resolveModular(request.modules)
        } else {
            null
        }

        // Make a list of Assets from the spec
        val assetIds = mutableSetOf<String>()
        val assets = request.assets.map { spec ->
            val id = AssetIdBuilder(spec).build()
            assetIds.add(id)
            val asset = assetSpecToAsset(id, spec, request.task)
            asset.setAttr("system.state", request.state.name)
            asset
        }

        val existingAssetIds = getValidAssetIds(assetIds)
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

    override fun batchIndex(docs: Map<String, MutableMap<String, Any>>, setAnalyzed: Boolean): BulkResponse {
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

        docs.forEach { (id, doc) ->

            val asset = prepAssetForUpdate(id, doc)
            if (setAnalyzed && !asset.isAnalyzed()) {
                asset.setAttr("system.state", AssetState.Analyzed.name)
                stateChangedIds.add(id)
            }

            /*
             * Index here vs update because otherwise the new doc will
             * be merge of the old one and the new one.
             */
            bulk.add(
                rest.newIndexRequest(id)
                    .source(doc)
                    .opType(DocWriteRequest.OpType.INDEX)
            )
        }

        logger.event(
            LogObject.ASSET, LogAction.BATCH_INDEX, mapOf("assetsIndexed" to docs.size)
        )

        val rsp = rest.client.bulk(bulk, RequestOptions.DEFAULT)
        if (stateChangedIds.isNotEmpty()) {
            val successIds = rsp.filter { !it.isFailed }.map { it.id }
            incrementProjectIngestCounters(stateChangedIds.intersect(successIds), docs)
        }

        return rsp
    }

    override fun batchDelete(ids: Set<String>): BatchDeleteAssetResponse {

        val rest = indexRoutingService.getProjectRestClient()
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

        for (removed in removed) {
            logger.event(
                LogObject.ASSET, LogAction.DELETE, mapOf("assetId" to removed)
            )
        }

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
        processors: List<ProcessorRef>,
        creds: Set<String>?
    ): Job? {

        // Validate the assets need reprocessing
        val assetIds = getAll(existingAssetIds).filter {
            assetNeedsReprocessing(it, processors)
        }.map { it.id }

        val reprocessAssetCount = assetIds.size
        val finalAssetList = assetIds.plus(createdAssetIds)

        return if (finalAssetList.isEmpty()) {
            null
        } else {
            val name = "Analyze ${createdAssetIds.size} created assets, $reprocessAssetCount existing files."
            jobLaunchService.launchJob(
                name, finalAssetList, processors, creds = creds, settings = mapOf("index" to true)
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

            val newTask = jobService.createTask(parentTask, TaskSpec(name, newScript))

            /**
             * For the assets that failed to go into ES, add the ES error message
             */
            jobService.incrementAssetCounters(
                parentTask,
                AssetCounters(
                    replaced = existingAssetIds.size,
                    created = createdAssetIds.size
                )
            )

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
            // Set the files property. The source files will reference the
            // original asset id.
            newAsset.setAttr("files", sourceFiles)
        } else {
            clip.putInPile(newAsset.id)
        }
        newAsset.setAttr("clip", clip)
        return clip
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
        pipeline: List<ProcessorRef>?,
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
        val failures = mutableListOf<Map<String, String?>>()

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
                    failures.add(
                        mapOf(
                            "assetId" to it.id,
                            "path" to path,
                            "failureMessage" to msg
                        )
                    )
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

        val time = java.time.Clock.systemUTC().instant().toString()
        if (asset.isAnalyzed()) {
            asset.setAttr("system.timeModified", time)
            asset.setAttr("system.state", AssetState.Analyzed.toString())
        } else {
            if (spec.clip != null) {
                deriveClip(asset, spec)
            }

            if (spec.label != null) {
                if (!dataSetDao.existsByProjectIdAndId(projectId, spec.label.dataSetId)) {
                    throw java.lang.IllegalArgumentException(
                        "The DataSet Id ${spec.label.dataSetId} does not exist."
                    )
                }
                asset.addLabels(listOf(spec.label))
            }

            asset.setAttr("source.path", spec.uri)
            asset.setAttr("source.filename", FileUtils.filename(spec.uri))
            asset.setAttr("source.extension", FileUtils.extension(spec.uri))

            val mediaType = FileExtResolver.getMediaType(spec.uri)
            asset.setAttr("source.mimetype", mediaType)

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

    fun prepAssetForUpdate(id: String, map: MutableMap<String, Any>): Asset {
        val asset = Asset(id, map)
        return prepAssetForUpdate(asset)
    }

    fun prepAssetForUpdate(asset: Asset): Asset {

        val time = java.time.Clock.systemUTC().instant().toString()

        // Remove these which are used for temp attrs
        removeFieldsOnUpdate.forEach {
            asset.removeAttr(it)
        }

        // Got back a clip but it has no pile which means it's in its own pile.
        // This happens during deep analysis when a file is being clipped, the first
        // clip/page/scene will be augmented with clip start/stop points.
        if (asset.attrExists("clip") && (
            !asset.attrExists("clip.sourceAssetId") || !asset.attrExists("clip.pile")
            )
        ) {
            val clip = asset.getAttr("clip", Clip::class.java)
                ?: throw IllegalStateException("Invalid clip data for asset ${asset.id}")
            clip.putInPile(asset.id)
            asset.setAttr("clip", clip)
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
        val bulkSize = 50
        val maxAssets = 1000
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

        // Gather up unique Assets and DataSets
        val allAssetIds = (req.add?.keys ?: setOf()) + (req.remove?.keys ?: setOf())
        val addDataSets = mutableSetOf<UUID>()
        req.add?.values?.forEach { labels ->
            addDataSets.addAll(labels.map { it.dataSetId })
        }

        // Validate the datasets we're adding are legit.
        val projectId = getProjectId()
        addDataSets.forEach {
            if (!dataSetDao.existsByProjectIdAndId(projectId, it)) {
                throw IllegalArgumentException("DataSetId $it not found")
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
        bulk.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        for (asset in AssetIterator(rest.client, rsp, 5000)) {
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
            setOf("system", "source", "files", "elements", "metrics", "datasets", "analysis")
    }
}
