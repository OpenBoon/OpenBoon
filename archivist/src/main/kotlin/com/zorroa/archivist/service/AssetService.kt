package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.elastic.ESUtils
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.repository.CommandDao
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.hasPermission
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.*
import com.zorroa.sdk.filesystem.ObjectFileSystem
import com.zorroa.sdk.schema.LinkSchema
import com.zorroa.sdk.schema.PermissionSchema
import com.zorroa.sdk.schema.ProxySchema
import com.zorroa.sdk.search.AssetFilter
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.search.AssetSearchOrder
import com.zorroa.sdk.util.Json
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.LongAdder

interface AssetService {

    fun getMapping(): Map<String, Any>

    fun get(id: String): Document

    fun get(path: Path): Document

    /**
     * Return the proxy schema for the given asset.  If the asset does not have a proxy
     * schema, check to see if it has children and choose the first child.
     *
     * If there is no proxy schema anywhere, return an empty one.
     *
     * @param id
     * @return
     */
    fun getProxies(id: String): ProxySchema

    /**
     * Fetch the first page of assets.
     *
     * @return
     */
    fun getAll(page: Pager): PagedList<Document>

    fun index(spec: AssetIndexSpec): AssetIndexResult

    fun index(doc: Document): Document

    fun removeFields(id: String, fields: MutableSet<String>)

    fun removeLink(type: String, value: String, assets: List<String>): Map<String, List<Any>>
    fun appendLink(type: String, value: String, assets: List<String>): Map<String, List<Any>>

    fun exists(path: Path): Boolean

    fun exists(id: String): Boolean

    /**
     * Update the given assetId with the supplied Map of attributes.  Return
     * the new version number of the asset.
     *
     * @param id
     * @param attrs
     * @return
     */
    fun update(assetId: String, attrs: Map<String, Any>): Long

    fun delete(assetId: String): Boolean

    fun setPermissions(command: Command, search: AssetSearch, acl: Acl)
}

@Component
class AssetServiceImpl  @Autowired  constructor (
        private val assetDao: AssetDao,
        private val commandDao: CommandDao,
        private val permissionDao: PermissionDao,
        private val dyHierarchyService: DyHierarchyService,
        private val taxonomyService: TaxonomyService,
        private val logService: EventLogService,
        private val searchService: SearchService,
        private val properties: ApplicationProperties,
        private val jobService: JobService,
        private val client: RestHighLevelClient,
        private val ofs: ObjectFileSystem

) : AssetService, ApplicationListener<ContextRefreshedEvent> {

    private var defaultPerms = PermissionSchema()

    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        setDefaultPermissions()
    }

    override fun get(id: String): Document {
        return if (id.startsWith("/")) {
            get(Paths.get(id))
        } else {
            assetDao[id]
        }
    }

    override fun get(path: Path): Document {
        return assetDao[path]
    }

    override fun getProxies(id: String): ProxySchema {
        val asset = get(id)
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)

        if (proxies != null) {
            return proxies
        } else {

            for (hit in searchService.search(Pager.first(1), AssetSearch(AssetFilter()
                    .addToTerms("media.clip.parent", id))
                    .setFields(arrayOf("proxies"))
                    .setOrder(ImmutableList.of(AssetSearchOrder("_id"))))) {
                return hit.getAttr("proxies", ProxySchema::class.java)
            }

            return ProxySchema()
        }
    }

    override fun getAll(page: Pager): PagedList<Document> {
        return assetDao.getAll(page)
    }

    override fun index(doc: Document): Document {
        val result = index(AssetIndexSpec(doc))
        return assetDao[result.getAssetIds()[0]]
    }

    override fun index(spec: AssetIndexSpec): AssetIndexResult {

        /**
         * Clear out any protected name spaces, this lets us ensure people
         * can't manipulate them with the attr API.
         *
         * There is no guarantee the document is the full document, so we can't
         * modify the permissions/links right here since the might not exist,
         * and if they do exist we'll remove them so they don't overwrite
         * the proper value.
         */
        val organizationId = getUser().organizationId

        for (source in spec.sources) {

            /**
             * Remove parts protected by API.
             */
            NS_PROTECTED_API.forEach { n -> source.removeAttr(n) }

            /**
             * Re-add the organization
             */
            source.setAttr("zorroa.organizationId", organizationId)

            val managedValues = Document(assetDao.getManagedFields(source.id))
            var perms = managedValues.getAttr("zorroa.permissions", PermissionSchema::class.java)
            if (perms == null) {
                perms = PermissionSchema()
            }

            if (source.permissions != null) {
                for ((key, value) in source.permissions) {
                    try {
                        val perm = permissionDao.get(key)
                        if (value and 1 == 1) {
                            perms.addToRead(perm.id)
                        } else {
                            perms.removeFromRead(perm.id)
                        }

                        if (value and 2 == 2) {
                            perms.addToWrite(perm.id)
                        } else {
                            perms.removeFromWrite(perm.id)
                        }

                        if (value and 4 == 4) {
                            perms.addToExport(perm.id)
                        } else {
                            perms.removeFromExport(perm.id)
                        }
                    } catch (e: Exception) {
                        logger.warn("Permission not found: {}", key)
                    }

                }
                source.setAttr("zorroa.permissions", Json.Mapper.convertValue(perms, Json.GENERIC_MAP))
            } else if (perms.isEmpty) {
                /**
                 * If the source didn't come with any permissions and the current perms
                 * on the asset are empty, we apply the default permissions.
                 *
                 * If there is no permissions.
                 */
                source.setAttr("zorroa.permissions",
                        Json.Mapper.convertValue(defaultPerms, Json.GENERIC_MAP))
            }

            if (source.links != null) {
                var links = managedValues.getAttr("zorroa.links", LinkSchema::class.java)
                if (links == null) {
                    links = LinkSchema()
                }
                for (link in source.links) {
                    links.addLink(link.left, link.right)
                }
                source.setAttr("zorroa.links", links)
            }

            val time = Date()

            if (managedValues.attrExists("zorroa.timeCreated") && !source.isReplace) {
                source.setAttr("zorroa.timeModified", time)
            }
            else {
                source.setAttr("zorroa.timeModified", time)
                source.setAttr("zorroa.timeCreated", time)
            }
        }

        logger.info("{}", Json.prettyString(spec.sources))
        val result = assetDao.index(spec.sources)
        val addr = TaskStatsAdder(result)

        if (spec.taskId != null && spec.jobId != null) {
            jobService.incrementStats(object : TaskId {
                override fun getJobId(): UUID? {
                    return spec.jobId
                }

                override fun getTaskId(): UUID? {
                    return spec.taskId
                }

                override fun getParentTaskId(): UUID? {
                    return null
                }
            }, addr)
        }

        if (result.created + result.updated + result.replaced > 0) {

            /**
             * TODO: make these 1 thread pool
             */
            dyHierarchyService.submitGenerateAll(true)
            taxonomyService.runAllAsync()
        }
        return result
    }

    override fun removeFields(id: String, fields: MutableSet<String>) {
        // remove fields from list the can't remove.
        fields.removeAll(NS_PROTECTED_API)
        assetDao.removeFields(id, fields, false)
    }

    override fun removeLink(type: String, value: String, assets: List<String>): Map<String, List<Any>> {
        return assetDao.removeLink(type, value, assets)
    }

    override fun appendLink(type: String, value: String, assets: List<String>): Map<String, List<Any>> {
        return assetDao.appendLink(type, value, assets)
    }

    override fun exists(path: Path): Boolean {
        return assetDao.exists(path)
    }

    override fun exists(id: String): Boolean {
        return assetDao.exists(id)
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Long {

        val asset = assetDao[assetId]
        val write = asset.getAttr("zorroa.permissions.write", Json.SET_OF_UUIDS)

        if (!hasPermission(write)) {
            throw ArchivistWriteException("You cannot make changes to this asset.")
        }

        val copy = Maps.newHashMap(attrs)
        /**
         * Remove keys which are maintained via other methods.
         */
        NS_PROTECTED_API.forEach { n -> copy.remove(n) }

        val version = assetDao.update(assetId, copy)
        logService.logAsync(UserLogSpec.build(LogAction.Update, "asset", assetId))
        return version
    }

    override fun delete(assetId: String): Boolean {
        val doc = assetDao[assetId]
        val proxySchema = doc.getAttr("proxies", ProxySchema::class.java)
        if (proxySchema != null) {
            for (proxy in proxySchema.proxies) {
                try {
                    if (!Files.deleteIfExists(ofs.get(proxy.id).file.toPath())) {
                        logger.warn("Did not delete {}, ofs file did not exist", proxy.id)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to delete OFS file: {}", e)
                }
            }
        }
        return assetDao.delete(assetId)
    }

    override fun setPermissions(command: Command, search: AssetSearch, acl: Acl) {

        val totalCount = searchService.count(search)
        if (totalCount == 0L) {
            return
        }

        val add = PermissionSchema()
        val remove = PermissionSchema()

        /**
         * Convert the ACL to a PermissionSchema.
         */
        for (entry in permissionDao.resolveAcl(acl, false)) {

            if (entry.getAccess() and 1 != 0) {
                add.addToRead(entry.getPermissionId())
            } else {
                remove.addToRead(entry.getPermissionId())
            }

            if (entry.getAccess() and 2 != 0) {
                add.addToWrite(entry.getPermissionId())
            } else {
                remove.addToWrite(entry.getPermissionId())
            }

            if (entry.getAccess() and 4 != 0) {
                add.addToExport(entry.getPermissionId())
            } else {
                remove.addToExport(entry.getPermissionId())
            }
        }

        logger.info("Adding permissions: {}", Json.serializeToString(add))
        logger.info("Removing permissions: {}", Json.serializeToString(remove))

        val totalSuccess = LongAdder()
        val totalFailed = LongAdder()
        val error = AtomicBoolean(false)

        val bulkListener = object : BulkProcessor.Listener {
            override fun afterBulk(executionId: Long, request: BulkRequest?, response: BulkResponse?) {
                var failureCount = 0
                var successCount = 0
                for (bir in response!!.items) {
                    if (bir.isFailed) {
                        logger.warn("update permissions bulk failed: {}", bir.failureMessage)
                        failureCount++
                    } else {
                        successCount++
                    }
                }
                commandDao.updateProgress(command, totalCount, successCount.toLong(), failureCount.toLong())
                totalSuccess.add(successCount.toLong())
                totalFailed.add(failureCount.toLong())
            }

            override fun afterBulk(executionId: Long, request: BulkRequest?, failure: Throwable?) {
                error.set(true)
                logger.warn("Failed to set permissions, ", failure)
            }

            override fun beforeBulk(executionId: Long, request: BulkRequest?) {

            }

        }

        val bulkProcessor = ESUtils.create(client, bulkListener)
                .setBulkActions(250)
                .setConcurrentRequests(0)
                .build()


        for (asset in searchService.scanAndScroll(
                search.setFields(arrayOf("zorroa")), 0)) {

            if (command.state == JobState.Cancelled) {
                logger.warn("setPermissions was canceled")
                break
            }

            if (error.get()) {
                logger.warn("Encountered error while setting permissions, exiting")
                break
            }


            val update = UpdateRequest("archivist", "asset", asset.id)
            var current: PermissionSchema? = asset.getAttr("zorroa.permissions", PermissionSchema::class.java)
            if (current == null) {
                current = PermissionSchema()
            }

            /**
             * Add all permissions specified by ACL
             */
            current.read.addAll(add.read)
            current.write.addAll(add.write)
            current.export.addAll(add.export)

            /**
             * Remove all permissions set to 0 in ACL.
             */
            current.read.removeAll(remove.read)
            current.write.removeAll(remove.write)
            current.export.removeAll(remove.export)

            update.doc(Json.serializeToString(ImmutableMap.of("zorroa",
                    ImmutableMap.of("permissions", current))))
            bulkProcessor.add(update)
        }

        try {
            logger.info("Waiting for bulk permission change to complete on {} assets.", totalCount)
            bulkProcessor.awaitClose(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
            logService.log(UserLogSpec()
                    .setUser(command.user)
                    .setMessage("Bulk permission change complete.")
                    .setAction(LogAction.BulkUpdate)
                    .setAttrs(ImmutableMap.of<String, Any>("zorroa.permissions", add)))
        } catch (e: InterruptedException) {
            logService.log(UserLogSpec()
                    .setUser(command.user)
                    .setMessage("Bulk update failure setting permissions on assets, interrupted.")
                    .setAction(LogAction.BulkUpdate))
        }

        logger.info("Bulk permission change complete, total: {} success: {} failed: {}",
                totalCount, totalSuccess.toLong(), totalFailed.toLong())
    }

    override fun getMapping(): Map<String, Any> {
        return assetDao.getMapping()
    }

    private fun setDefaultPermissions() {
        val defaultReadPerms = properties.getList("archivist.security.permissions.defaultRead")
        val defaultWritePerms = properties.getList("archivist.security.permissions.defaultWrite")
        val defaultExportPerms = properties.getList("archivist.security.permissions.defaultExport")

        for (p in permissionDao.getAll(defaultReadPerms)) {
            defaultPerms.addToRead(p.id)
        }

        for (p in permissionDao.getAll(defaultWritePerms)) {
            defaultPerms.addToWrite(p.id)
        }

        for (p in permissionDao.getAll(defaultExportPerms)) {
            defaultPerms.addToExport(p.id)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)

        /**
         * Namespaces that are only populated via the API.  IF people manipulate these
         * wrong via the asset API then it would corrupt the asset.
         */
        private val NS_PROTECTED_API = ImmutableSet.of(
                "zorroa", "tmp")
    }

}
