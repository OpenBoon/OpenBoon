package com.zorroa.archivist.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.base.Stopwatch
import com.google.common.collect.*
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.FolderDao
import com.zorroa.archivist.repository.TaxonomyDao
import com.zorroa.archivist.security.InternalAuthentication
import com.zorroa.archivist.security.InternalRunnable
import com.zorroa.common.elastic.CountingBulkListener
import com.zorroa.common.elastic.ElasticClientUtils
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.search.AssetFilter
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.sort.SortParseElement
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.LongAdder
import java.util.function.Predicate
import java.util.stream.Collectors

interface TaxonomyService {

    fun delete(tax: Taxonomy?, untag: Boolean): Boolean

    fun create(spec: TaxonomySpec): Taxonomy

    fun get(id: Int): Taxonomy

    fun get(folder: Folder): Taxonomy

    fun runAllAsync()

    fun runAll()

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
        private val client: Client,
        private val folderTaskExecutor: UniqueTaskExecutor
): TaxonomyService {

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var searchService: SearchService

    @Autowired
    internal lateinit var userService: UserService

    @Value("\${zorroa.cluster.index.alias}")
    private val alias: String? = null

    internal var EXCLUDE_FOLDERS: Set<String> = ImmutableSet.of("Library", "Users")

    private val auth: Authentication
        get() {
            val user = userService.get("admin")
            return InternalAuthentication(user, userService.getPermissions(user))
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
            if (an.isTaxonomyRoot) {
                throw ArchivistWriteException("The folder is already in a taxonomy")
            }
        }

        val tax = taxonomyDao.create(spec)

        if (folder.parentId == null) {
            throw ArchivistWriteException("The root folder cannot be a taxonomy.")
        }

        if (EXCLUDE_FOLDERS.contains(folder.name) && folder.parentId == 0) {
            throw ArchivistWriteException("This folder cannot hold a taxonomy.")
        }

        val result = folderDao.setTaxonomyRoot(folder, true)
        if (result) {
            folderService.invalidate(folder)
            folder.isTaxonomyRoot = true

            // force is false because there won't be folders with this tax id.
            tagTaxonomyAsync(tax, folder, false)
            return tax
        } else {
            throw ArchivistWriteException(
                    "Failed to create taxonomy, unable to set taxonomy on folder: " + folder)
        }
    }

    override fun get(id: Int): Taxonomy {
        return taxonomyDao.get(id)
    }

    override fun get(folder: Folder): Taxonomy {
        return taxonomyDao[folder]
    }

    override fun runAllAsync() {
        folderTaskExecutor.execute(
                UniqueRunnable("tax_run_all",
                        InternalRunnable(auth) { runAll() }))
    }

    override fun runAll() {
        for (tax in taxonomyDao.all) {
            tagTaxonomy(tax, null, false)
        }
    }

    override fun tagTaxonomyAsync(tax: Taxonomy, start: Folder?, force: Boolean) {
        if (ArchivistConfiguration.unittest) {
            tagTaxonomy(tax, start, force)
        } else {
            folderTaskExecutor.execute(UniqueRunnable("tax_run_" + tax.taxonomyId,
                    InternalRunnable(auth) { tagTaxonomy(tax, start, force) }))
        }
    }

    override fun tagTaxonomy(tax: Taxonomy, start: Folder?, force: Boolean): Map<String, Long> {
        var start = start

        logger.info("Tagging taxonomy: {}", tax)

        if (start == null) {
            start = folderService.get(tax.folderId)
        }
        val updateTime = System.currentTimeMillis()

        val folderTotal = LongAdder()
        val assetTotal = LongAdder()

        taxonomyDao.setActive(tax, true)
        try {
            for (folder in folderService.getAllDescendants(Lists.newArrayList(start), true, false)) {
                folderTotal.increment()

                /**
                 * Walking back is currently the only way to determine the keyword list,
                 * but most calls to are cached.
                 */
                val ancestors = folderService.getAllAncestors(folder, true, true)
                val keywords = Lists.newArrayList<String>()

                var foundRoot = false
                if (!folder.isTaxonomyRoot) {
                    for (f in ancestors) {
                        keywords.add(f.name)
                        if (f.isTaxonomyRoot) {
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

                val cbl = CountingBulkListener()
                val bulkProcessor = BulkProcessor.builder(
                        client, cbl)
                        .setBulkActions(BULK_SIZE)
                        .setFlushInterval(TimeValue.timeValueSeconds(10))
                        .setConcurrentRequests(0)
                        .build()


                var search: AssetSearch? = folder.search
                if (search == null) {
                    search = AssetSearch(AssetFilter()
                            .addToTerms("links.folder", folder.id)
                            .setRecursive(false))
                }

                // If it is not a force, then skip over fields already written.
                if (!force) {
                    search.filter.mustNot = ImmutableList.of(
                            AssetFilter().addToTerms(ROOT_FIELD + ".taxId", tax.taxonomyId))
                }

                val timer = Stopwatch.createStarted()
                var rsp = searchService.buildSearch(search, "asset")
                        .setScroll(TimeValue(60000))
                        .setFetchSource(true)
                        .setSize(PAGE_SIZE).execute().actionGet()
                logger.info("tagging taxonomy {} batch 1 : {}", tax, timer)

                val taxy = TaxonomySchema()
                        .setFolderId(folder.id)
                        .setTaxId(tax.taxonomyId)
                        .setUpdatedTime(updateTime)
                        .setKeywords(keywords)
                        .setSuggest(keywords)

                var batchCounter = 1
                try {
                    do {
                        for (hit in rsp.hits.hits) {
                            val doc = Document(hit.source)
                            var taxies: MutableSet<TaxonomySchema>? = doc.getAttr(ROOT_FIELD, object : TypeReference<MutableSet<TaxonomySchema>>() {})
                            if (taxies == null) {
                                taxies = Sets.newHashSet()
                            }
                            taxies!!.add(taxy)
                            doc.setAttr(ROOT_FIELD, taxies)
                            bulkProcessor.add(client.prepareIndex(alias, "asset", hit.id)
                                    .setOpType(IndexRequest.OpType.INDEX)
                                    .setSource(Json.serialize(doc.document)).request())
                        }

                        rsp = client.prepareSearchScroll(rsp.scrollId).setScroll(
                                TimeValue(60000)).execute().actionGet()
                        batchCounter++
                        logger.info("tagging {} batch {} : {}", tax, batchCounter, timer)


                    } while (rsp.hits.hits.size != 0)
                } catch (e: Exception) {
                    logger.warn("Failed to tag taxonomy assets: {}", tax)
                } finally {
                    bulkProcessor.close()
                }
                assetTotal.add(cbl.successCount)
            }

            if (force) {
                untagTaxonomyAsync(tax, updateTime)
            }
        } finally {
            taxonomyDao.setActive(tax, false)
        }


        logger.info("Taxonomy {} executed, {} assets updated in {} folders",
                tax.folderId, assetTotal.toLong(), folderTotal.toInt())

        if (assetTotal.toLong() > 0) {
            searchService.invalidateFields()
        }

        return ImmutableMap.of(
                "assetCount", assetTotal.toLong(),
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
        logger.warn("Untagging {} on {} assets {}", tax, folder, assets)

        val cbl = CountingBulkListener()
        val bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(BULK_SIZE)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
                .build()

        val search = AssetSearch()
        search.filter = AssetFilter()
                .addToTerms("_id", assets)
                .addToTerms("zorroa.taxonomy.folderId", folder.id)

        val rsp = client.prepareSearch("archivist")
                .setScroll(TimeValue(60000))
                .setFetchSource(true)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet()

        processBulk(bulkProcessor, rsp, Predicate { ts -> ts.taxId == tax.taxonomyId })

        bulkProcessor.close()
        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.successCount, cbl.errorCount)

    }

    /**
     * Untag specific taxonomy folders folders.  This happens when
     * the folders are deleted.
     *
     * @param tax
     * @param folders
     */
    override fun untagTaxonomyFolders(tax: Taxonomy, folders: List<Folder>) {
        logger.warn("Untagging {} on {} folders", tax, folders.size)

        val folderIds = folders.stream().map { f -> f.id }.collect(Collectors.toList())

        ElasticClientUtils.refreshIndex(client, 1)
        for (list in Lists.partition(folderIds, 500)) {

            val cbl = CountingBulkListener()
            val bulkProcessor = BulkProcessor.builder(
                    client, cbl)
                    .setBulkActions(BULK_SIZE)
                    .setFlushInterval(TimeValue.timeValueSeconds(10))
                    .setConcurrentRequests(0)
                    .build()

            val search = AssetSearch()
            search.filter = AssetFilter()
                    .addToTerms("zorroa.taxonomy.folderId", list as MutableList<Any>)

            val rsp = client.prepareSearch("archivist")
                    .setScroll(TimeValue(60000))
                    .setFetchSource(false)
                    .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                    .setQuery(searchService.getQuery(search))
                    .setSize(PAGE_SIZE).execute().actionGet()

            processBulk(bulkProcessor, rsp, Predicate { ts -> ts.taxId == tax.taxonomyId })

            bulkProcessor.close()
            logger.info("Untagged: {} success:{} errors: {}", tax,
                    cbl.successCount, cbl.errorCount)
        }
    }

    /**
     * Called to untag an entire taxonomy if the taxonomy is deleted.
     *
     * @param tax
     * @return
     */
    override fun untagTaxonomy(tax: Taxonomy): Map<String, Long> {
        logger.info("Untagging entire taxonomy {}", tax)
        val cbl = CountingBulkListener()
        val bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(BULK_SIZE)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
                .build()

        val search = AssetSearch()
        search.filter = AssetFilter()
                .addToTerms("zorroa.taxonomy.taxId", tax.taxonomyId)

        val rsp = client.prepareSearch("archivist")
                .setScroll(TimeValue(60000))
                .setFetchSource(true)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet()

        processBulk(bulkProcessor, rsp, Predicate { ts -> ts.taxId == tax.taxonomyId })

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.successCount, cbl.errorCount)

        return ImmutableMap.of(
                "assetCount", cbl.successCount,
                "errorCount", cbl.errorCount)
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

        logger.info("Untagging assets no longer tagged tagged: {} {}", tax, timestamp)
        ElasticClientUtils.refreshIndex(client, 1)

        val cbl = CountingBulkListener()
        val bulkProcessor = BulkProcessor.builder(
                client, cbl)
                .setBulkActions(BULK_SIZE)
                .setFlushInterval(TimeValue.timeValueSeconds(10))
                .setConcurrentRequests(0)
                .build()

        /**
         * This filters out assets with a new timestamp.
         */
        val search = AssetSearch()
        search.filter = AssetFilter().addToTerms("zorroa.taxonomy.taxId", tax.taxonomyId)

        val rsp = client.prepareSearch("archivist")
                .setScroll(TimeValue(60000))
                .setFetchSource(true)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(searchService.getQuery(search))
                .setSize(PAGE_SIZE).execute().actionGet()

        processBulk(bulkProcessor, rsp,
                Predicate { ts -> ts.taxId == tax.taxonomyId && ts.updatedTime != timestamp })

        logger.info("Untagged: {} success:{} errors: {}", tax,
                cbl.successCount, cbl.errorCount)

        return ImmutableMap.of(
                "assetCount", cbl.successCount,
                "errorCount", cbl.errorCount,
                "timestamp", timestamp)
    }

    private fun processBulk(bulkProcessor: BulkProcessor, rsp: SearchResponse, pred: Predicate<TaxonomySchema>) {
        var rsp = rsp
        try {
            do {
                for (hit in rsp.hits.hits) {
                    val doc = Document(hit.source)
                    val taxies = doc.getAttr(ROOT_FIELD, object : TypeReference<MutableSet<TaxonomySchema>>() {})
                    if (taxies != null) {
                        if (taxies.removeIf(pred)) {
                            doc.setAttr(ROOT_FIELD, taxies)
                            bulkProcessor.add(client.prepareIndex("archivist", "asset", hit.id)
                                    .setOpType(IndexRequest.OpType.INDEX)
                                    .setSource(Json.serialize(doc.document)).request())
                        }
                    }
                }
                rsp = client.prepareSearchScroll(rsp.scrollId).setScroll(
                        TimeValue(60000)).execute().actionGet()

            } while (rsp.hits.hits.size != 0)

        } catch (e: Exception) {
            logger.warn("Failed to untag taxonomy assets, ", e)

        } finally {
            bulkProcessor.close()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TaxonomyServiceImpl::class.java)
        /**
         * Number of entries to write at one time.
         */
        private val BULK_SIZE = 100

        /**
         * Number of assets to pull on each page.
         */
        private val PAGE_SIZE = 100


        private val ROOT_FIELD = "zorroa.taxonomy"
    }
}
