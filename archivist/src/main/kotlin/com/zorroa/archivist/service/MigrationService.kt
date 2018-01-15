package com.zorroa.archivist.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.domain.Migration
import com.zorroa.archivist.domain.MigrationType
import com.zorroa.archivist.repository.MigrationDao
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.sdk.util.Json
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.repositories.RepositoryMissingException
import org.elasticsearch.search.sort.SortOrder
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.support.TransactionTemplate
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import java.util.regex.Pattern

/**
 * Created by chambers on 2/2/16.
 */
interface MigrationService {

    fun getAll(): List<Migration>

    fun processAll()

    fun getAll(type: MigrationType): List<Migration>

    fun processMigrations(migrations: List<Migration>, force: Boolean)

    fun processElasticMigration(m: Migration, force: Boolean)
}

class ElasticMigrationProperties {
    private var version = 1
    private var reindex = false
    private var reingest = false
    private var patch = 0
    private var mapping: Map<String, Any>? = null

    fun getVersion(): Int {
        return version
    }

    fun setVersion(version: Int): ElasticMigrationProperties {
        this.version = version
        return this
    }

    fun incrementVersion(version: Int, mapping: Map<String, Any>): ElasticMigrationProperties {
        if (version > this.version) {
            this.version = version
            this.mapping = mapping
        }
        return this
    }

    fun isReindex(): Boolean {
        return reindex
    }

    fun setReindex(reindex: Boolean): ElasticMigrationProperties {
        this.reindex = reindex
        return this
    }

    fun isReingest(): Boolean {
        return reingest
    }

    fun setReingest(reingest: Boolean): ElasticMigrationProperties {
        this.reingest = reingest
        return this
    }

    fun getMapping(): Map<String, Any>? {
        return mapping
    }

    fun setMapping(mapping: Map<String, Any>): ElasticMigrationProperties {
        this.mapping = mapping
        return this
    }

    fun getPatch(): Int {
        return patch
    }

    fun setPatch(patch: Int): ElasticMigrationProperties {
        this.patch = patch
        return this
    }
}

@Component
class MigrationServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val migrationDao: MigrationDao,
        private val client: Client,
        private val transactionManager: PlatformTransactionManager,
        private val flyway: Flyway
): MigrationService {

    override fun processAll() {
        processMigrations(migrationDao.getAll(), false)
    }

    override fun getAll(): List<Migration> {
        return migrationDao.getAll()
    }

    override fun getAll(type: MigrationType): List<Migration> {
        return migrationDao.getAll(type)
    }

    override fun processMigrations(migrations: List<Migration>, force: Boolean) {

        val snapshotName = System.getenv("ARCHIVIST_RESTORE_INDEX")
        if (snapshotName != null) {
            restoreSnapshot(snapshotName)
        }

        /**
         * TODO: Don't let ingests run during migrations.
         */

        if (!properties.getBoolean("archivist.index.migrateEnabled")) {
            logger.warn("Auto index migration is disabled!")
            return
        }

        logger.info("Processing migrations")
        for (m in migrations) {
            when (m.type) {
                MigrationType.ElasticSearchIndex -> processElasticMigration(m, force)
            }
        }
    }

    fun setRefreshInterval(index: String, value: String) {
        logger.info("setting refresh interval to: {}", value)
        val usrb = client.admin().indices()
                .prepareUpdateSettings()
        usrb.setIndices(index)
        usrb.setSettings(ImmutableMap.of<String, Any>("index.refresh_interval", value))
        usrb.get()
    }

    /**
     * Wait for a yellow cluster status before starting a reindex.
     */
    fun waitOnClusterStatus(index: String) {
        val status = properties.getString("archivist.index.migrateStatus")
        logger.info("Waiting on {} cluster status for index: {}", status, index)
        if ("green" == status) {
            client.admin().cluster().prepareHealth(index)
                    .setWaitForGreenStatus()
                    .get()
        } else {
            client.admin().cluster().prepareHealth(index)
                    .setWaitForYellowStatus()
                    .get()
        }
    }

    /**
     * This method opens the elastic mapping supplied at the given
     * path and compares the embedded version number with the version number
     * we're running.  If the numbers are different, then a migration is
     * kicked off.
     *
     * No Ingests should be scheduled at this time.
     *
     * @param m
     */
    override fun processElasticMigration(m: Migration, force: Boolean) {

        val props: ElasticMigrationProperties
        try {
            props = getLatestVersion(m)
        } catch (e: IOException) {
            logger.warn("Failed to migration elastic index, unable to load elastic mapping: {}", m.path)
            throw RuntimeException("Failed to setup ElasticSearch index, ", e)
        }

        val oldIndex = String.format("%s_%02d", m.name, m.version)
        val newIndex = String.format("%s_%02d", m.name, props.getVersion())
        val oldIndexExists = client.admin().indices().prepareExists(oldIndex).get().isExists
        val newIndexExists = client.admin().indices().prepareExists(newIndex).get().isExists

        /**
         * If we already have the current version then check for patch versions.
         */
        if (props.getVersion() == m.version && newIndexExists) {
            logger.info("'{}' mapping V{} is the current version", m.name, m.version)
            applyMappingPatches(m)
            return
        }

        /**
         * If neither index exists then its the first time the index has been created.
         */
        if (!oldIndexExists && !newIndexExists) {
            logger.info("No indexes exist, {} will be created", newIndex)
        } else if (m.version > props.getVersion()) {
            logger.warn("Version {} is higher than version {}, not downgrading.",
                    m.version, props.getVersion())
            return
        }

        if (newIndexExists) {
            logger.warn("New index '{}' already exists, may not be latest version", newIndex)
            client.admin().indices().prepareOpen(newIndex).get()
        } else {
            logger.info("Processing migration: {}, path={}, force={}", m.name, m.path, force)
            client.admin()
                    .indices()
                    .prepareCreate(newIndex)
                    .setSource(props.getMapping())
                    .get()
        }

        /**
         * If there is an old index and the new index is a different name than
         * the old index, AND the new index props say we must reindex, then
         * do a reindex.
         */
        if (oldIndexExists && oldIndex != newIndex && props.isReindex()) {
            /**
             * Wait on the cluster status to at least be yellow.
             */
            waitOnClusterStatus(oldIndex)

            /**
             * Get a document count.
             */
            val count = client.prepareSearch(oldIndex)
                    .setSize(0)
                    .setQuery(QueryBuilders.matchAllQuery())
                    .get().hits.totalHits()
            val batchTotal = count / BULK_SIZE + 1
            val batchFinished = LongAdder()
            logger.info("Processing {} docs in {} batches", count, batchTotal)

            /**
             * Setup a bulk processor
             */

            val bulkProcessor = BulkProcessor.builder(
                    client,
                    object : BulkProcessor.Listener {
                        override fun beforeBulk(executionId: Long,
                                                request: BulkRequest) {
                        }

                        override fun afterBulk(executionId: Long,
                                               request: BulkRequest,
                                               response: BulkResponse) {
                            batchFinished.increment()
                            logger.info("{} progress: {}/{} ({}ms)",
                                    newIndex, batchFinished.toLong(), batchTotal, response.tookInMillis / 1000.0)
                        }

                        override fun afterBulk(executionId: Long,
                                               request: BulkRequest,
                                               failure: Throwable) {
                            logger.warn("Bulk index failure, ", failure)
                        }
                    })
                    .setBulkActions(BULK_SIZE)
                    .setConcurrentRequests(1)
                    .build()

            /**
             * Now scan/scroll over everything and copy from new to old.
             */
            setRefreshInterval(newIndex, "-1")
            try {
                var scrollResp = client.prepareSearch(oldIndex)
                        .setSearchType(SearchType.DEFAULT)
                        .setScroll(TimeValue(BULK_TIMEOUT))
                        .addSort("_doc", SortOrder.ASC)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setSize(BULK_SIZE).execute().actionGet()

                while (true) {
                    for (hit in scrollResp.hits.hits) {
                        bulkProcessor.add(client.prepareIndex(
                                newIndex, hit.type, hit.id).setSource(hit.source()).request())
                    }

                    scrollResp = client.prepareSearchScroll(scrollResp.scrollId).setScroll(
                            TimeValue(BULK_TIMEOUT)).execute().actionGet()
                    if (scrollResp.hits.hits.size == 0) {
                        break
                    }
                }
            } finally {
                try {
                    logger.info("Waiting on bulk processing to complete")
                    bulkProcessor.awaitClose(60, TimeUnit.MINUTES)
                } catch (e: InterruptedException) {
                    logger.warn("Bulk processor interrupted, it's possible some data was not moved")
                }

                setRefreshInterval(newIndex, "1s")
            }
        }
        /**
         * Flip the alias.
         *
         * The index we're removing it from might not exist for some reason.
         */
        try {
            val req = client.admin().indices().prepareAliases()
            logger.info("old index: {} exists: {}, removing alias.", oldIndex, oldIndexExists)
            if (oldIndexExists) {
                req.removeAlias(oldIndex, m.name)
            }
            req.addAlias(newIndex, m.name).execute().actionGet()
        } catch (e: ElasticsearchException) {
            logger.warn("Could not remove alias from {}, error was: '{}'. (this is ok)", oldIndex, e.message)
        }

        /**
         * For unit tests, suspend the unit test transaction and execute the update
         * in a separate transaction, that we're not starting at V1 every time.
         */
        val tt = TransactionTemplate(transactionManager)
        tt.propagationBehavior = Propagation.REQUIRES_NEW.ordinal
        val updated = tt.execute { _ ->
            val result = migrationDao.setVersion(m, props.getVersion(), props.getPatch())
            result
        }

        if (!updated) {
            logger.warn("Could not update migration record to version: " + props.getVersion() +
                    ", already set to that version.")
        }

        /**
         * Now close the old index so we don't waste time on it.
         */
        if (oldIndexExists) {
            logger.info("Closing index: {}", oldIndex)
            client.admin().indices().prepareClose(oldIndex).get()
        }
    }

    fun applyMappingPatches(m: Migration) {
        val mappingPatches = Maps.newHashMap<Int, Map<String, Any>>()
        try {
            val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
            val resources = resolver.getResources(m.path)
            for (resource in resources) {
                val matcher = MAPPING_PATCH_CONV.matcher(resource.filename)
                if (!matcher.matches()) {
                    continue
                }

                val majorVersion = Integer.valueOf(matcher.group(1))
                val patchVersion = Integer.valueOf(matcher.group(2))

                if (majorVersion != m.version) {
                    continue
                }

                // Check if its been already applied
                if (patchVersion <= m.patch) {
                    logger.info("Patch {}.{} has already been applied", majorVersion, patchVersion)
                    continue
                }

                mappingPatches.put(patchVersion, Json.Mapper.readValue(resource.inputStream,
                        Json.GENERIC_MAP))
            }
        } catch (e: Exception) {
            logger.warn("Unable to location elastic migration patch files", e)
            return
        }

        /**
         * TODO: patches can only be applied to assets right now.
         */
        val keys = Lists.newArrayList(mappingPatches.keys)
        Collections.sort(keys)
        logger.info("Applying {} patch versions to {}", keys.size, m)
        for (key in keys) {
            try {
                client.admin()
                        .indices()
                        .preparePutMapping(m.name)
                        .setType("asset")
                        .setSource(mappingPatches[key])
                        .get()
                migrationDao.setPatch(m, key)
            } catch (e: Exception) {
                logger.warn("Failed to apply elastic mapping patch version: {}", m, e)
            }

        }
    }

    @Throws(IOException::class)
    fun getLatestVersion(m: Migration): ElasticMigrationProperties {
        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
        val resources = resolver.getResources(m.path)
        val allVersions = Lists.newArrayList<ElasticMigrationProperties>()
        for (resource in resources) {

            val matcher = MAPPING_NAMING_CONV.matcher(resource.filename)
            if (!matcher.matches()) {
                continue
            }

            val emp = ElasticMigrationProperties()
            val version = Integer.valueOf(matcher.group(1))
            logger.info("Found embedded mapping in {} version {}", m.path, version)

            val mapping = Json.Mapper.readValue<Map<String, Any>>(resource.inputStream, Json.GENERIC_MAP)
            emp.setMapping(mapping)
            emp.setVersion(version)

            /**
             * Only versions with the migration header are added.
             */
            if (mapping.containsKey("migration")) {
                val props = Json.Mapper.convertValue<Map<String, Any>>(mapping["migration"],
                        object : TypeReference<Map<String, Any>>() {

                        })

                val reindex = props["reindex"] as Boolean
                emp.setReindex(reindex)

                val patch = props["patch"]
                if (patch != null) {
                    emp.setPatch(patch as Int)
                }

                allVersions.add(emp)
            } else {
                logger.warn("Unable to find version/migration info in mapping {}", resource.filename)
            }
        }

        if (allVersions.isEmpty()) {
            throw IOException("Failed to find latest mapping for migration " + m.path)
        }

        Collections.sort(allVersions) { o1, o2 -> Integer.compare(o2.getVersion(), o1.getVersion()) }
        val result = allVersions[0]
        logger.info("latest '{}' mapping ver: {} (source='{}')", m.name, result.getVersion(), m.path)
        return result
    }

    private fun restoreSnapshot(name: String) {
        /*
         * Additional info:
         * https://www.elastic.co/guide/en/elasticsearch/reference/2.4/modules-snapshots.html
         */

        val file = properties.getPath("archivist.path.backups")
                .resolve("index").resolve("snap-$name.dat").toFile()

        val snapshotExists = file.exists()
        if (!snapshotExists) {
            throw RuntimeException("Invalid snapshot name " + name + ", a snapshot file does" +
                    "not exist: " + file.toString())
        }

        logger.warn("Restoring from snapshot: {}", name)
        try {
            client.admin().indices().prepareClose("_all").get()
        } catch (e: Exception) {
            logger.warn("Failed to close all indexes, this is probably OK", e)
        }

        try {
            client.admin().cluster().prepareRestoreSnapshot("archivist", name).get()
        } catch (rme: RepositoryMissingException) {
            /*
             * If the index folder was deleted then we lost the snapshot, so we make an entry
             * for the same one and then try to restore it.
             */
            /**
             * TODO: I think we have to iterate through all snapshots and re-create
             * records for them.
             */
            try {
                logger.info("Recreating snapshot record for: {}", name)
                client.admin().cluster().prepareCreateSnapshot("archivist", name).get()

                logger.warn("Restoring from snapshot: {}", name)
                client.admin().cluster().prepareRestoreSnapshot("archivist", name).get()
            } catch (e: Exception) {
                logger.warn("Failed to recover and restore snapshot: ", e)
            }

        }

    }

    companion object {

        private val logger = LoggerFactory.getLogger(MigrationServiceImpl::class.java)

        private val BULK_SIZE = 200
        private val BULK_TIMEOUT: Long = 60000


        private val MAPPING_NAMING_CONV = Pattern.compile("^V(\\d+)__(.*?).json$")

        private val MAPPING_PATCH_CONV = Pattern.compile("^V(\\d+)\\.(\\d)+__(.*?).json$")
    }
}
