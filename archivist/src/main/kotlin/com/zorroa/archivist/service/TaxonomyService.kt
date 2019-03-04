package com.zorroa.archivist.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.*
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.elastic.CountingBulkListener
import com.zorroa.archivist.elastic.ESUtils
import com.zorroa.archivist.repository.FolderDao
import com.zorroa.archivist.repository.TaxonomyDao
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.*
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.util.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.search.sort.FieldSortBuilder.DOC_FIELD_NAME
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import java.util.function.Predicate
import java.util.stream.Collectors

interface TaxonomyService {

    fun delete(tax: Taxonomy?, untag: Boolean): Boolean

    fun create(spec: TaxonomySpec): Taxonomy

    fun get(id: UUID): Taxonomy

    fun get(folder: Folder): Taxonomy

    fun tagAll()

    fun tagTaxonomyAsync(tax: Taxonomy, start: Folder?, force: Boolean)

    fun tagTaxonomy(tax: Taxonomy, start: Folder?, force: Boolean): Map<String, Long>

    fun untagTaxonomyAsync(tax: Taxonomy, updatedTime: Long)

    fun untagTaxonomyAsync(tax: Taxonomy)

    fun untagTaxonomyFoldersAsync(tax: Taxonomy, folders: List<Folder>)

    fun untagTaxonomyFoldersAsync(tax: Taxonomy, folder: Folder, assets: List<String>)

    fun untagTaxonomyFolders(tax: Taxonomy, folder: Folder, assets: List<String>)

    fun untagTaxonomyFolders(tax: Taxonomy, folders: List<Folder>)

    fun untagTaxonomy(tax: Taxonomy): Map<String, Long>

    fun untagTaxonomy(tax: Taxonomy, timestamp: Long): Map<String, Long>
}

@Service
@Transactional
class TaxonomyServiceImpl @Autowired constructor(
        private val taxonomyDao: TaxonomyDao,
        private val folderDao: FolderDao,
        private val indexRoutingService: IndexRoutingService,
        private val folderTaskExecutor: UniqueTaskExecutor,
        private val clusterLockExecutor: ClusterLockExecutor
): TaxonomyService {

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var searchService: SearchService

    @Autowired
    internal lateinit var fieldService: FieldService

    @Autowired
    internal lateinit var userRegistryService: UserRegistryService

    internal var EXCLUDE_FOLDERS: Set<String> = ImmutableSet.of("Library", "Users")

    private val auth: Authentication
        get() {
            val user = userRegistryService.getUser("admin")
            return InternalAuthentication(user)
        }

    override fun delete(tax: Taxonomy?, untag: Boolean): Boolean {
        tax?.let {
            if (taxonomyDao.delete(tax.taxonomyId)) {
                // MHide the tax field, appending the . is needed for everything
                // under the field

                val folder = folderDao.get(tax.folderId)
                folderDao.setTaxonomyRoot(folder, false)
                folderService.invalidate(folder)
                if (untag) {
                    untagTaxonomyAsync(tax, 0)
                }
                return true
            }
        }
        return false
    }

    override fun create(spec: TaxonomySpec): Taxonomy {
        val folder = folderService.get(spec.folderId)
        val ancestors = folderService.getAllAncestors(folder, true, true)
        for (an in ancestors) {
            if (an.taxonomyRoot) {
                throw ArchivistWriteException("The folder is already in a taxonomy")
            }
        }

        if (EXCLUDE_FOLDERS.contains(folder.name) || folder.name == "/") {
            throw ArchivistWriteException("This folder cannot hold a taxonomy.")
        }

        val tax = taxonomyDao.create(spec)
        if (folder.parentId == null) {
            throw ArchivistWriteException("The root folder cannot be a taxonomy.")
        }

        val result = folderDao.setTaxonomyRoot(folder, true)
        if (result) {
            folderService.invalidate(folder)
            folder.taxonomyRoot = true

            // force is false because there won't be folders with this tax id.
            tagTaxonomyAsync(tax, folder, false)
            return tax
        } else {
            throw ArchivistWriteException(
                    "Failed to create taxonomy, unable to set taxonomy on folder: $folder")
        }
    }

    override fun get(id: UUID): Taxonomy {
        return taxonomyDao.get(id)
    }

    override fun get(folder: Folder): Taxonomy {
        return taxonomyDao[folder]
    }

    override fun tagAll() {
        for (tax in taxonomyDao.getAll()) {
            tagTaxonomy(tax, null, false)
        }
    }

    override fun tagTaxonomy(tax: Taxonomy, start: Folder?, force: Boolean) : Map<String, Long> {
        return if (ArchivistConfiguration.unittest) {
            tagTaxonomyInternal(tax, start, force)
        }
        else {
            val lock = ClusterLockSpec.combineLock(tax.clusterLockId()).apply { timeout = 5 }
            val result = clusterLockExecutor.async(lock) {
                try {
                    tagTaxonomyInternal(tax, start, force)
                } catch (e: Exception) {
                    logger.warn("Failed to run taxon ${tax.taxonomyId}", e)
                    null
                }
            }
            result ?: emptyMap()
        }
    }

    override fun tagTaxonomyAsync(tax: Taxonomy, start: Folder?, force: Boolean) {
        if (ArchivistConfiguration.unittest) {
            tagTaxonomy(tax, start, force)
        }
        else {
            val auth = getAuthentication()
            GlobalScope.launch {
                withAuth(auth) {
                    tagTaxonomy(tax, start, force)
                }
            }
        }
    }

    fun tagTaxonomyInternal(tax: Taxonomy, start: Folder?, force: Boolean): Map<String, Long> {
        var start = start

        if (start == null) {
            start = folderService.get(tax.folderId, cached = false)
        }
        val updateTime = System.currentTimeMillis()
        val folderTotal = LongAdder()
        val assetTotal = LongAdder()

        val rest = indexRoutingService[getOrgId()]
        val cbl = CountingBulkListener()
        val bulkProcessor = ESUtils.create(rest.client, cbl)
                .setBulkActions(BULK_SIZE)
                .setConcurrentRequests(0)
                .build()

        try {
            for (folder in folderService.getAllDescendants(listOf(start), true, false)) {
                folderTotal.increment()

                /**
                 * Walking back is currently the only way to determine the keyword list,
                 * but most calls to are cached.
                 */
                val ancestors = folderService.getAllAncestors(folder, true, true)
                val keywords = mutableListOf<String>()

                var foundRoot = false
                if (!folder.taxonomyRoot) {
                    for (f in ancestors) {
                        keywords.add(f.name)
                        if (f.taxonomyRoot) {
                            foundRoot = true
                            break
                        }
                    }
                } else {
                    keywords.add(folder.name)
                    foundRoot = true
                }

                if (!foundRoot) {
                    logger.warn("Unable to find taxonomy root for folder: {}", folder)
                    break
                }

                var search: AssetSearch? = folder.search
                if (search == null) {
                    search = AssetSearch(AssetFilter()
                            .addToTerms("system.links.folder", folder.id)
                            .setRecursive(false))
                }

                // If it is not a force, then skip over fields already written.
                if (!force) {
                    search.filter.mustNot = listOf(
                            AssetFilter().addToTerms("$ROOT_FIELD.taxId", tax.taxonomyId))
                }

                var req = searchService.buildSearch(search, "asset")
                rest.routeSearchRequest(req.request)
                req.request.scroll(TimeValue(60000))
                req.source.fetchSource(true)
                req.source.size(PAGE_SIZE)

                var rsp = rest.client.search(req.request)
                val taxy = TaxonomySchema()
                        .setFolderId(folder.id)
                        .setTaxId(tax.taxonomyId)
                        .setUpdatedTime(updateTime)
                        .setKeywords(keywords)

                var batchCounter = 0
                try {
                    do {
                        for (hit in rsp.hits.hits) {

                            val doc = Document(hit.sourceAsMap)
                            var taxies: MutableSet<TaxonomySchema>? = doc.getAttr(ROOT_FIELD, object : TypeReference<MutableSet<TaxonomySchema>>() {})
                            if (taxies == null) {
                                taxies = mutableSetOf()
                            }
                            taxies.add(taxy)
                            doc.setAttr(ROOT_FIELD, taxies)

                            bulkProcessor.add(rest.newIndexRequest(hit.id)
                                    .opType(DocWriteRequest.OpType.INDEX)
                                    .source(Json.serialize(doc.document), XContentType.JSON))
                        }

                        val scroll = SearchScrollRequest()
                        scroll.scrollId(rsp.scrollId)
                        scroll.scroll(TimeValue(60000))
                        rsp = rest.client.searchScroll(scroll)
                        batchCounter++

                    } while (rsp.hits.hits.isNotEmpty())

                    logger.event(LogObject.TAXONOMY, LogAction.TAG,
                            mapOf("taxonomyId" to tax.taxonomyId,
                                    "folderId" to tax.folderId,
                                    "assetCount" to assetTotal.toInt(),
                                    "folderCount" to folderTotal.toInt(),
                                    "batchCount" to batchCounter))

                } catch (e: Exception) {
                    logger.warnEvent(LogObject.TAXONOMY, LogAction.TAG, "Failed to tag taxon, ${e.message}",
                            mapOf("taxonomyId" to tax.taxonomyId))
                }
            }

            if (force) {
                untagTaxonomyAsync(tax, updateTime)
            }

        } finally {
            bulkProcessor.awaitClose(1000, TimeUnit.HOURS)
        }

        if (assetTotal.toLong() > 0) {
            fieldService.invalidateFields()
        }

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "folderCount", folderTotal.toLong(),
                "timestamp", updateTime)
    }

    override fun untagTaxonomyAsync(tax: Taxonomy, timestamp: Long) {
        folderTaskExecutor.execute(InternalRunnable(auth) { untagTaxonomy(tax, timestamp) })
    }

    override fun untagTaxonomyAsync(tax: Taxonomy) {
        folderTaskExecutor.execute(InternalRunnable(auth) { untagTaxonomy(tax) })
    }

    override fun untagTaxonomyFoldersAsync(tax: Taxonomy, folders: List<Folder>) {
        folderTaskExecutor.execute(InternalRunnable(auth) { untagTaxonomyFolders(tax, folders) })
    }

    override fun untagTaxonomyFoldersAsync(tax: Taxonomy, folder: Folder, assets: List<String>) {
        folderTaskExecutor.execute(InternalRunnable(auth) { untagTaxonomyFolders(tax, folder, assets) })
    }

    /**
     * Untag specific assets in a given folder.  This is run when asstets
     * are removed from a folder.
     *
     * @param tax
     * @param folder
     * @param assets
     */
    override fun untagTaxonomyFolders(tax: Taxonomy, folder: Folder, assets: List<String>) {
        logger.event(LogObject.TAXONOMY, LogAction.UNTAG,
                mapOf("untagType" to "asset", "taxonomyId" to tax.taxonomyId))

        val rest = indexRoutingService[getOrgId()]
        val cbl = CountingBulkListener()
        val bulkProcessor = ESUtils.create(rest.client, cbl)
                .setBulkActions(BULK_SIZE)
                .setConcurrentRequests(0)
                .build()

        val search = AssetSearch()
        search.filter = AssetFilter()
                .addToTerms("_id", assets)
                .addToTerms("system.taxonomy.folderId", folder.id)

        val sb = rest.newSearchBuilder()
        sb.request.scroll(SCROLL_TIME)
        sb.source.query(searchService.getQuery(search))
        sb.source.fetchSource(true)
        sb.source.sort(DOC_FIELD_NAME, SortOrder.ASC)
        sb.source.size(PAGE_SIZE)

        val rsp = rest.client.search(rest.routeSearchRequest(sb.request))
        processBulk(tax, bulkProcessor, rsp, Predicate { ts -> ts.taxId == tax.taxonomyId })

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount())
    }

    /**
     * Untag specific taxonomy folders folders.  This happens when
     * the folders are deleted.
     *
     * @param tax
     * @param folders
     */
    override fun untagTaxonomyFolders(tax: Taxonomy, folders: List<Folder>) {
        logger.event(LogObject.TAXONOMY, LogAction.UNTAG,
                mapOf("untagType" to "folder", "taxonomyId" to tax.taxonomyId))

        val rest = indexRoutingService[getOrgId()]
        val folderIds = folders.stream().map { f -> f.id }.collect(Collectors.toList())
        for (list in Lists.partition(folderIds, 500)) {

            val cbl = CountingBulkListener()
            val bulkProcessor = ESUtils.create(rest.client, cbl)
                    .setBulkActions(BULK_SIZE)
                    .setConcurrentRequests(0)
                    .build()

            val search = AssetSearch()
            search.filter = AssetFilter()
                    .addToTerms("system.taxonomy.folderId", list as MutableList<Any>)

            val sb = rest.newSearchBuilder()
            sb.request.scroll(SCROLL_TIME)
            sb.source.query(searchService.getQuery(search))
            sb.source.fetchSource(true)
            sb.source.sort(DOC_FIELD_NAME, SortOrder.ASC)
            sb.source.size(PAGE_SIZE)

            val rsp = rest.client.search(rest.routeSearchRequest(sb.request))
            processBulk(tax, bulkProcessor, rsp, Predicate { ts -> ts.taxId == tax.taxonomyId })

            logger.info("Untagged: {} success:{} errors: {}", tax,
                    cbl.getSuccessCount(), cbl.getErrorCount())
        }

    }

    /**
     * Called to untag an entire taxonomy if the taxonomy is deleted.
     *
     * @param tax
     * @return
     */
    override fun untagTaxonomy(tax: Taxonomy): Map<String, Long> {
        logger.event(LogObject.TAXONOMY, LogAction.UNTAG, mapOf("taxonomyId" to tax.taxonomyId))

        val rest = indexRoutingService[getOrgId()]
        val cbl = CountingBulkListener()
        val bulkProcessor = ESUtils.create(rest.client, cbl)
                .setBulkActions(BULK_SIZE)
                .setConcurrentRequests(0)
                .build()

        val search = AssetSearch()
        search.filter = AssetFilter()
                .addToTerms("system.taxonomy.taxId", tax.taxonomyId)

        val sb = rest.newSearchBuilder()
        sb.request.scroll(SCROLL_TIME)
        sb.source.query(searchService.getQuery(search))
        sb.source.fetchSource(true)
        sb.source.sort(DOC_FIELD_NAME, SortOrder.ASC)
        sb.source.size(PAGE_SIZE)
        val rsp = rest.client.search(sb.request)
        processBulk(tax, bulkProcessor, rsp, Predicate { ts -> ts.taxId == tax.taxonomyId })

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getErrorCount(), cbl.getErrorCount())

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "errorCount", cbl.getErrorCount())

        return mapOf()
    }

    /**
     * Untags assets which were part of a taxonomy but are no longer.  Pass
     * in a 0 for timestamp to mean all assets get untagged.
     *
     * @param tax
     * @param timestamp
     * @return
     */
    override fun untagTaxonomy(tax: Taxonomy, timestamp: Long): Map<String, Long> {
        logger.event(LogObject.TAXONOMY, LogAction.UNTAG,
                mapOf("untagType" to "time", "taxonomyId" to tax.taxonomyId))
        val rest = indexRoutingService[getOrgId()]
        val cbl = CountingBulkListener()
        val bulkProcessor = ESUtils.create(rest.client, cbl)
                .setBulkActions(BULK_SIZE)
                .setConcurrentRequests(0)
                .build()

        /**
         * This filters out assets with a new timestamp.
         */
        val search = AssetSearch()
        search.filter = AssetFilter().addToTerms("system.taxonomy.taxId", tax.taxonomyId)

        val sb = rest.newSearchBuilder()
        sb.request.scroll(SCROLL_TIME)
        sb.source.query(searchService.getQuery(search))
        sb.source.fetchSource(true)
        sb.source.sort(DOC_FIELD_NAME, SortOrder.ASC)
        sb.source.size(PAGE_SIZE)

        val rsp = rest.client.search(sb.request)
        processBulk(tax, bulkProcessor, rsp,
                Predicate { ts -> ts.taxId == tax.taxonomyId && ts.updatedTime != timestamp })

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.getSuccessCount(), cbl.getErrorCount())

        return ImmutableMap.of(
                "assetCount", cbl.getSuccessCount(),
                "errorCount", cbl.getErrorCount(),
                "timestamp", timestamp)

        return mapOf()
    }

    private fun processBulk(tax: Taxonomy, bulkProcessor: BulkProcessor, rsp: SearchResponse, pred: Predicate<TaxonomySchema>) {
        var rsp = rsp
        val rest = indexRoutingService[getOrgId()]

        // Use a non combined hard lock but the same lock ID.
        clusterLockExecutor.async(ClusterLockSpec.hardLock(tax.clusterLockId())) {
            withAuth(auth) {
                try {
                    do {
                        for (hit in rsp.hits.hits) {
                            val doc = Document(hit.sourceAsMap)
                            val taxies =
                                    doc.getAttr(ROOT_FIELD, object : TypeReference<MutableSet<TaxonomySchema>>() {})
                            if (taxies != null) {
                                if (taxies.removeIf(pred)) {
                                    doc.setAttr(ROOT_FIELD, taxies)
                                    val indexReq = rest.newIndexRequest(hit.id)
                                    indexReq.opType(DocWriteRequest.OpType.INDEX)
                                    indexReq.source(Json.serialize(doc.document), XContentType.JSON)
                                    bulkProcessor.add(indexReq)
                                }
                            }
                        }
                        val scrollReq = SearchScrollRequest()
                        scrollReq.scrollId(rsp.scrollId)
                        scrollReq.scroll(SCROLL_TIME)
                        rsp = rest.client.searchScroll(scrollReq)

                    } while (rsp.hits.hits.isNotEmpty())

                } catch (e: Exception) {
                    logger.warn("Failed to untag taxonomy assets, ", e)

                } finally {
                    bulkProcessor.close()
                }
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TaxonomyServiceImpl::class.java)

        private val SCROLL_TIME = TimeValue.timeValueSeconds(60)

        /**
         * Number of entries to write at one time.
         */
        private const val BULK_SIZE = 100

        /**
         * Number of assets to pull on each page.
         */
        private const val PAGE_SIZE = 100

        private const val ROOT_FIELD = "system.taxonomy"
    }
}
