package com.zorroa.archivist.service

import com.google.common.base.Splitter
import com.google.common.collect.*
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.DyHierarchyDao
import com.zorroa.archivist.security.SecureRunnable
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.common.elastic.ElasticClientUtils
import com.zorroa.sdk.domain.Tuple
import com.zorroa.sdk.search.AssetScript
import com.zorroa.sdk.search.AssetSearch
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface DyHierarchyService {

    fun update(id: Int, spec: DyHierarchy): Boolean

    fun delete(dyhi: DyHierarchy): Boolean

    /**
     * Create a dynamic hierarchy generator.
     *
     * @param spec
     */
    fun create(spec: DyHierarchySpec): DyHierarchy

    /**
     * Generate a dynamic hierarchy.
     *
     * @param agg
     */
    fun generate(dyhi: DyHierarchy): Int

    fun get(id: Int): DyHierarchy

    fun get(folder: Folder): DyHierarchy

    /**
     * Generate all hierarchies.
     */
    fun generateAll()

    /**
     * Submit command to generate folders on all hierarchies.  This command
     * is throttled to a reasonable rate.  To bypass the throttle, set the
     * 'force' argument to true.
     *
     * @param refresh
     */
    fun submitGenerateAll(refresh: Boolean)

    /**
     * Generate a dynamic hierarchy.
     *
     * @param agg
     */
    fun submitGenerate(dyhi: DyHierarchy)
}

@Service
class DyHierarchyServiceImpl @Autowired constructor (
    val dyHierarchyDao: DyHierarchyDao,
    val client: Client,
    val transactionEventManager: TransactionEventManager,
    val folderTaskExecutor: UniqueTaskExecutor
) : DyHierarchyService {

    @Autowired
    private lateinit var folderService: FolderService;

    @Autowired
    private lateinit var logService: EventLogService

    @Autowired
    private lateinit var searchService: SearchService

    @Value("\${archivist.dyhi.maxFoldersPerLevel}")
    private val maxFoldersPerLevel: Int? = null

    internal var FORCE_RAW_TYPES: Set<DyHierarchyLevelType> = EnumSet.of(DyHierarchyLevelType.Attr)

    @Transactional
    override fun update(id: Int, spec: DyHierarchy): Boolean {
        val current = dyHierarchyDao.get(id)

        if (!dyHierarchyDao.update(id, spec)) {
            return false
        }

        val updated = dyHierarchyDao.get(id)

        /*
         * Folder ID change
         */
        val folderOld = folderService.get(current.folderId)
        val folderNew = folderService.get(updated.folderId)
        if (folderOld !== folderNew) {
            folderService.removeDyHierarchyRoot(folderOld)
        }

        /*
         * Even if the folder didn't change, we reset so the smart query
         * gets updated.
         */
        val field = updated.getLevel(0).field
        folderService.setDyHierarchyRoot(folderNew, field)

        /*
         * If this returns true, the dyhi was working, it should stop
         * pretty quickly.  Other transactions should be blocked at
         * the update() call, but there might be some races in here.
         */
        if (dyHierarchyDao.setWorking(updated, false)) {
            logger.info("DyHi {} was running, will wait on folders to be removed.")
        }

        /*
         * Queue up an event where after this transaction commits.
         */
        transactionEventManager.afterCommit(false, {
            logService.logAsync(UserLogSpec.build(LogAction.Update, updated))
            folderService.deleteAll(current)
            submitGenerate(dyHierarchyDao.get(current.id))
        })

        return true
    }

    @Transactional
    override fun delete(dyhi: DyHierarchy): Boolean {

        if (dyHierarchyDao.delete(dyhi.id)) {
            val folder = folderService.get(dyhi.folderId)
            folderService.removeDyHierarchyRoot(folder)

            if (dyHierarchyDao.setWorking(dyhi, false)) {
                logger.info("DyHi {} was running, will wait on folders to be removed.")
            } else {
                folderService.deleteAll(dyhi)
            }
            transactionEventManager.afterCommit(true,
                    { logService.logAsync(UserLogSpec.build(LogAction.Delete, dyhi)) })
            return true
        }
        return false
    }

    @Transactional
    override fun create(spec: DyHierarchySpec): DyHierarchy {

        val folder = folderService.get(spec.folderId)
        val dyhi = dyHierarchyDao.create(spec)
        folderService.setDyHierarchyRoot(folder, spec.levels[0].field)

        if (ArchivistConfiguration.unittest) {
            generate(dyhi)
        } else {
            /*
             * Generate the hierarchy in another thread
             * after this method returns.
             */
            transactionEventManager.afterCommit(true, {
                logService.logAsync(UserLogSpec.build(LogAction.Create, dyhi))
                submitGenerate(dyhi)
            })
        }

        return dyhi
    }

    override fun get(id: Int): DyHierarchy {
        return dyHierarchyDao.get(id)
    }

    override fun get(folder: Folder): DyHierarchy {
        return dyHierarchyDao[folder]
    }

    override fun generateAll() {
        for (dyhi in dyHierarchyDao.getAll()) {
            generate(dyhi)
        }
    }

    override fun submitGenerateAll(refresh: Boolean) {
        folderTaskExecutor.execute(
                UniqueRunnable("dyhi_run_all", SecureRunnable({
                    if (refresh) {
                        ElasticClientUtils.refreshIndex(client)
                    }
                    generateAll()
                }, SecurityContextHolder.getContext())))
    }

    override fun submitGenerate(dyhi: DyHierarchy) {
        folderTaskExecutor.execute(UniqueRunnable("dyhi_run_" + dyhi.id,
                SecureRunnable({ generate(dyhi) }, SecurityContextHolder.getContext())))
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun generate(dyhi: DyHierarchy): Int {
        if (dyhi.levels == null || dyhi.levels.isEmpty()) {
            return 0
        }

        if (!dyHierarchyDao.setWorking(dyhi, true)) {
            logger.warn("DyHi {} already running, skipping.", dyhi)
            return 0
        }

        val rf = folderService.get(dyhi.folderId)


        /**
         * TODO: allow some custom search options here, for example, maybe you
         * want to agg for the last 24 hours.
         */
        try {
            val srb = client.prepareSearch()
                    .setQuery(if (rf.search == null)
                        QueryBuilders.matchAllQuery()
                    else
                        searchService.getQuery(rf.search))
                    .setSize(0)

            /**
             * Fix the field name to take into account raw.
             */
            for (level in dyhi.levels) {
                resolveFieldName(level)
            }

            /**
             * Build a nested list of Elastic aggregations.
             */
            var terms: AggregationBuilder<*>? = elasticAggregationBuilder(dyhi.levels[0], 0)
            srb.addAggregation(terms)
            for (i in 1 until dyhi.levels.size) {
                val sub = elasticAggregationBuilder(dyhi.getLevel(i), i)
                terms!!.subAggregation(sub)
                terms = sub
            }

            /**
             * Breadth first walk of the aggregations which creates the folders as it goes.
             */
            val folders = FolderStack(folderService.get(dyhi.folderId), dyhi)
            val queue = Queues.newArrayDeque<Tuple<Aggregations, Int>>()
            queue.add(Tuple(srb.get().aggregations, 0))
            createDynamicHierarchy(queue, folders)

            /**
             * Delete all unmarked folders.
             */
            val unusedFolders = Sets.difference(ImmutableSet.copyOf(folderService.getAllIds(dyhi)), folders.folderIds)
            try {
                folderService.deleteAll(unusedFolders)
            } catch (e: Exception) {
                logger.warn("Failed to delete unused folders: {}, {}", unusedFolders, e)
            }

            logger.info("{} created by {}, {} folders", dyhi, SecurityUtils.getUsername(), folders.count)
            return folders.count
        } catch (e: Exception) {
            logger.warn("Failed to generate dynamic hierarchy,", e)
        } finally {
            if (!dyHierarchyDao.isWorking(dyhi)) {
                /*
                 * If we get here and the dyhi is set to not working,then
                 * that means someone deleted it while it was running.
                 * That means we're responsible for deleting the folders.
                 */
                logger.warn("The dynamic hierarchy {} was stopped, removing all folders.", dyhi)
                folderService.deleteAll(dyhi)
            } else {
                dyHierarchyDao.setWorking(dyhi, false)
            }
        }

        return 0
    }

    private fun elasticAggregationBuilder(level: DyHierarchyLevel, idx: Int): AggregationBuilder<*>? {
        when (level.type) {
            DyHierarchyLevelType.Attr, null -> {
                val terms = AggregationBuilders.terms(idx.toString())
                terms.field(level.field)
                terms.size(maxFoldersPerLevel!!)
                return terms
            }
            DyHierarchyLevelType.Year -> {
                val year = AggregationBuilders.dateHistogram(idx.toString())
                year.field(level.field)
                year.interval(DateHistogramInterval("1y"))
                year.format("yyyy")
                year.minDocCount(1)
                return year
            }
            DyHierarchyLevelType.Month -> {
                val month = AggregationBuilders.dateHistogram(idx.toString())
                month.field(level.field)
                month.interval(DateHistogramInterval("1M"))
                month.format("M")
                month.minDocCount(1)
                return month
            }
            DyHierarchyLevelType.Day -> {
                val day = AggregationBuilders.dateHistogram(idx.toString())
                day.field(level.field)
                day.interval(DateHistogramInterval("1d"))
                day.format("d")
                day.minDocCount(1)
                return day
            }
            DyHierarchyLevelType.Path -> {
                val pathTerms = AggregationBuilders.terms(idx.toString())
                pathTerms.field(level.field)
                pathTerms.size(maxFoldersPerLevel!!)
                return pathTerms
            }
        }
    }

    /**
     * Breadth first walk of aggregations which builds the folder structure as it walks.
     *
     * @param queue
     */
    private fun createDynamicHierarchy(queue: Queue<Tuple<Aggregations, Int>>, folders: FolderStack) {
        /*
         * A fast in memory check.
         */
        if (!dyHierarchyDao.isWorking(folders.dyhi)) {
            return
        }

        val item = queue.poll()
        val aggs = item.left
        val depth = item.right
        if (aggs == null) {
            return
        }

        val terms = aggs.get<MultiBucketsAggregation>(depth.toString()) ?: return

        val level = folders.dyhi.getLevel(depth)

        val buckets = terms.buckets
        for (bucket in buckets) {

            if (!dyHierarchyDao.isWorking(folders.dyhi)) {
                return
            }

            var popit = true

            val value: String
            var name: String
            when (level.type) {
                DyHierarchyLevelType.Attr -> {
                    value = bucket.keyAsString
                    name = value.replace('/', '_')
                    folders.push(name, level, value, null)
                }
                DyHierarchyLevelType.Path -> {
                    value = bucket.keyAsString
                    val delimiter = (level.options as MutableMap<String, Any>).getOrDefault("delimiter", "/").toString()

                    val peek = folders.peek()
                    folders.stash()
                    popit = false
                    for (`val` in PathIterator(value, delimiter)) {
                        name = Iterables.getLast(Splitter.on(delimiter).omitEmptyStrings().splitToList(`val`))
                        folders.push(name, level, `val`, peek)
                    }
                }
                else -> {
                    value = bucket.keyAsString
                    folders.push(value, level, null, null)
                }
            }

            val child = bucket.aggregations
            if (child.asList().size > 0) {
                queue.add(Tuple(child, depth + 1))
                createDynamicHierarchy(queue, folders)
            }
            if (popit) {
                folders.pop()
            } else {
                folders.unstash()
            }
        }
    }

    inner class FolderStack(val root: Folder, val dyhi: DyHierarchy) {
        private val stack = Stack<Folder>()
        private val stash = Stack<Folder>()
        var count = 0
        val folderIds: MutableSet<Int> = Sets.newHashSetWithExpectedSize(50)

        init {
            this.stack.push(root)
        }

        /**
         * Stash the current stack as the new root.
         */
        fun stash() {
            stash.clear()
            stash.addAll(stack)
        }

        fun unstash() {
            stack.clear()
            stack.addAll(stash)
        }

        fun pop() {
            stack.pop()
        }

        fun peek(): Folder? {
            try {
                return stack.peek()
            } catch (e: NullPointerException) {
            }

            return null
        }

        /**
         * Create a folder at a given level and pushes the result
         * onto the stack.
         *
         * @param value
         * @param level
         * @return
         */
        fun push(value: String, level: DyHierarchyLevel, queryValue: String?, searchParent: Folder?): Folder {
            val parent = stack.peek()
            val search = getSearch(queryValue ?: value, level)

            /*
             * The parent search is merged into the current folder's search.
             */
            if (searchParent != null) {
                if (searchParent.search != null) {
                    search.filter.merge(searchParent.search.filter)
                }
            } else if (parent.search != null) {
                search.filter.merge(parent.search.filter)
            }

            val name = getFolderName(value, level)

            /*
             * Create a non-recursive
             */
            val spec = FolderSpec()
                    .setName(name)
                    .setParentId(parent.id)
                    .setRecursive(false)
                    .setDyhiId(dyhi.id)
                    .setSearch(search)

            val folder = folderService.create(spec, true)
            if (level.acl != null && spec.created) {
                /**
                 * If the ACL is set, build an ACL based on the name.
                 */
                val acl = Acl()
                for (entry in level.acl) {
                    val perm = entry.getPermission().replace("%\\{name\\}".toRegex(), name)
                    acl.add(AclEntry().setPermission(perm).setAccess(entry.getAccess()))
                }
                folderService.setAcl(folder, acl, true, true)
            }

            count++
            stack.push(folder)
            // Make note of the folder ID.
            folderIds.add(folder.id)
            return folder
        }

        /**
         * Take a value and a level and return the proper folder name.  This
         * will eventually support more formatting options.
         *
         * @param value
         * @param level
         * @return
         */
        fun getFolderName(value: String, level: DyHierarchyLevel): String {
            when (level.type) {
                DyHierarchyLevelType.Attr, DyHierarchyLevelType.Year, DyHierarchyLevelType.Day -> return value
                DyHierarchyLevelType.Month -> return java.text.DateFormatSymbols().months[Integer.parseInt(value) - 1]
                else -> return value
            }
        }

        /**
         * Build a search from the given folder name and level.
         *
         * @param value
         * @param level
         * @return
         */
        fun getSearch(value: String, level: DyHierarchyLevel): AssetSearch {
            val search = AssetSearch()
            when (level.type) {
                DyHierarchyLevelType.Attr, null -> search.addToFilter().addToTerms(level.field, Lists.newArrayList<Any>(value))
                DyHierarchyLevelType.Year -> search.addToFilter().addToScripts(AssetScript(
                        String.format("doc['%s'].getYear() == value", level.field),
                        ImmutableMap.of<String, Any>("value", Integer.valueOf(value))))
                DyHierarchyLevelType.Month -> search.addToFilter().addToScripts(AssetScript(
                        String.format("doc['%s'].getMonth() == value", level.field),
                        ImmutableMap.of<String, Any>("value", Integer.valueOf(value) - 1)))
                DyHierarchyLevelType.Day -> search.addToFilter().addToScripts(AssetScript(
                        String.format("doc['%s'].getDayOfMonth() == value", level.field),
                        ImmutableMap.of<String, Any>("value", Integer.valueOf(value))))
                DyHierarchyLevelType.Path -> {
                    // chop off trailing /
                    val modifiedValue = value.substring(0, value.length - 1)
                    search.addToFilter().addToTerms(level.field, ImmutableList.of<Any>(modifiedValue))
                }
            }
            return search
        }
    }

    private fun resolveFieldName(level: DyHierarchyLevel) {
        if (level.field.endsWith(".raw") || !FORCE_RAW_TYPES.contains(level.type)) {
            return
        }

        val type = ElasticClientUtils.getFieldType(client, "archivist", "asset", level.field)
        if (type != "string") {
            return
        }

        level.field = level.field + ".raw"
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DyHierarchyServiceImpl::class.java)
    }
}

