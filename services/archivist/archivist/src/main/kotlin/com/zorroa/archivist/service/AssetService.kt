package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.*
import com.zorroa.common.util.Json
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.search.SearchType
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
import java.util.*

/**
 * AssetService contains the entry points for Asset CRUD operations. In general
 * you won't use IndexService directly, AssetService will call through for you.
 *
 * Note that, unfortunately, we update ES before the transactional datastore because
 * we rely on ES to merge upserts.  If we did not allow upserts and always overwrote
 * the full doc, we could switch this behavior.
 */
interface AssetService {
    fun handleAssetUpload(name: String, bytes: ByteArray): AssetUploadedResponse
    fun search(query: Map<String, Any>, output: OutputStream)
    fun createOrReplaceAssets(batch: BatchCreateAssetsRequest): BatchIndexAssetsResponse
    fun get(assetId: String): Document
}

/**
 * PreppedAssets is the result of preparing assets to be indexed.
 *
 * @property assets a list of assets prepped and ready to be ingested.
 * @property auditLogs uncommitted field change logs detected for each asset
 */
class PreppedAssets(
        val assets: List<Document>,
        val scope: String
)

@Service
class AssetServiceImpl : AssetService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var indexService: IndexService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    lateinit var fileStorageService: FileStorageService

    @Autowired
    lateinit var messagingService: MessagingService

    /**
     * Prepare a list of assets to be created.  Updated assets are not prepped.
     *
     * - Removing tmp/system namespaces
     * - Applying the project Id
     * - Applying modified / created times
     * - Applying default permissions
     * - Applying links
     *
     * Return a PreppedAssets object which contains the updated assets as well as
     * the field change audit logs.  The audit logs for successful assets are
     * added to the audit log table.
     *
     * @param assets The list of assets to prepare
     * @return PreppedAssets
     */
    fun prepAssets(req: BatchCreateAssetsRequest): PreppedAssets {
        if (req.skipAssetPrep) {
            return PreppedAssets(req.sources, req.scope)
        }

        val assets = req.sources
        val projectId = getProjectId()

        val prepped = PreppedAssets(assets.map { newSource ->

            val existingSource: Document = try {
                get(newSource.id)
            } catch (e: Exception) {
                Document(newSource.id)
            }

            /**
             * Remove parts protected by API.
             */
            PROTECTED_NAMESPACES.forEach { n -> newSource.removeAttr(n) }

            newSource.setAttr("system.projectId", projectId.toString())
            handleTimes(existingSource, newSource)
            newSource
        }, req.scope)

        return prepped
    }

    /**
     * Handles updating the system.timeCreated and system.timeModified fields.
     *
     * @param oldAsset the original asset
     * @param newAsset the new asset
     */
    private fun handleTimes(oldAsset: Document, newAsset: Document) {
        /**
         * Update created and modified times.
         */
        val time = Date()

        if (oldAsset.attrExists("system.timeCreated")) {
            newAsset.setAttr("system.timeModified", time)
            newAsset.setAttr("system.timeCreated", oldAsset.getAttr("system.timeCreated"))
        } else {
            newAsset.setAttr("system.timeModified", time)
            newAsset.setAttr("system.timeCreated", time)
        }
    }

    /**
     * Increment any job counters for index requests coming from the job system.
     */
    fun incrementJobCounters(req: BatchCreateAssetsRequest, rsp: BatchIndexAssetsResponse) {
        req.taskId?.let {
            val task = jobService.getTask(it)
            jobService.incrementAssetCounters(task, rsp.getAssetCounters())
        }
    }

    /**
     * Index a batch of PreppedAssets
     */
    fun batchIndexAssets(
            req: BatchCreateAssetsRequest?,
            prepped: PreppedAssets,
            batchUpdateResult: Map<String, Boolean>? = null
    ): BatchIndexAssetsResponse {

        val docsToIndex = if (batchUpdateResult != null) {
            // Filter out the docs that didn't make it into the DB, but default allow anything else to go in.
            prepped.assets.filter {
                batchUpdateResult.getOrDefault(it.id, true)
            }
        } else {
            prepped.assets
        }

        val rsp = indexService.index(docsToIndex)
        if (req != null) {
            incrementJobCounters(req, rsp)
        }

        if (rsp.createdAssetIds.isNotEmpty()) {
            messagingService.sendMessage(
                    actionType = ActionType.AssetsCreated,
                    projectId = getProjectId(),
                    data = mapOf("ids" to rsp.createdAssetIds)
            )
        }
        if (rsp.replacedAssetIds.isNotEmpty()) {
            messagingService.sendMessage(
                    actionType = ActionType.AssetsDeleted,
                    projectId = getProjectId(),
                    data = mapOf("ids" to rsp.replacedAssetIds)
            )
        }
        return rsp
    }

    override fun get(assetId: String): Document {
        return indexService.get(assetId)
    }

    override fun createOrReplaceAssets(batch: BatchCreateAssetsRequest): BatchIndexAssetsResponse {
        /**
         * We have to do this backwards here because we're relying on ES to
         * merge existing docs and updates together.
         */
        if (indexRoutingService.isReIndexRoute()) {
            batch.skipAssetPrep = true
        }

        val prepped = prepAssets(batch)
        return batchIndexAssets(batch, prepped)
    }

    override fun handleAssetUpload(name: String, bytes: ByteArray): AssetUploadedResponse {
        val id = UUID.randomUUID()
        val fss = fileStorageService.get(FileStorageSpec("asset", id, name))
        fileStorageService.write(fss.id, bytes)
        return AssetUploadedResponse(id, fss.uri)
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
        val client = indexRoutingService.getOrgRestClient()

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
