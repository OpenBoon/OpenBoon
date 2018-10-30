package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AssetIndexResult
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.AssetSearchOrder
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.util.event
import com.zorroa.archivist.util.warnEvent
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.schema.LinkSchema
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.util.Json
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*


interface IndexService {

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

    fun batchDelete(assetId: List<String>): BatchDeleteAssetsResponse
}

@Component
class IndexServiceImpl  @Autowired  constructor (
        private val indexDao: IndexDao,
        private val permissionDao: PermissionDao,
        private val fileServerProvider: FileServerProvider,
        private val fileStorageService: FileStorageService,
        private val jobService: JobService

) : IndexService {

    @Autowired
    lateinit var dyHierarchyService: DyHierarchyService

    @Autowired
    lateinit var  taxonomyService: TaxonomyService

    @Autowired
    lateinit var logService: EventLogService

    @Autowired
    lateinit var searchService: SearchService

    override fun get(id: String): Document {
        return if (id.startsWith("/")) {
            get(Paths.get(id))
        } else {
            indexDao[id]
        }
    }

    override fun get(path: Path): Document {
        return indexDao[path]
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
        return indexDao.getAll(page)
    }

    override fun index(doc: Document): Document {
        val result = index(AssetIndexSpec(listOf(doc)))
        return indexDao.get(result.assetIds[0])
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
        val organizationId = getOrgId()

        for (source in spec.sources!!) {

            val managedValues = Document(indexDao.getManagedFields(source.id!!))

            /**
             * Remove parts protected by API.
             */
            NS_PROTECTED_API.forEach { n -> source.removeAttr(n) }

            /**
             * Re-add the organization
             */
            source.setAttr("system.organizationId", organizationId)

            /**
             * Update created and modified times.
             */
            val time = Date()

            if (managedValues.attrExists("system.timeCreated")) {
                source.setAttr("system.timeModified", time)
                /**
                 * If the document is being replaced, maintain the created time.
                 */
                //if (source.replace) {
                //    source.setAttr("system.timeCreated", managedValues.getAttr("system.timeCreated"))
                //}
            }
            else {
                source.setAttr("system.timeModified", time)
                source.setAttr("system.timeCreated", time)
            }

            var perms = managedValues.getAttr("system.permissions", PermissionSchema::class.java)
            if (perms == null) {
                perms = PermissionSchema()
            }

            if (source.permissions != null) {
                for ((key, value) in source.permissions!!) {
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
                source.setAttr("system.permissions", Json.Mapper.convertValue<Map<String,Any>>(perms, Json.GENERIC_MAP))
            } else if (perms.isEmpty) {

                /**
                 * If the source didn't come with any permissions and the current perms
                 * on the asset are empty, we apply the default permissions.
                 *
                 * If there is no permissions.
                 */
                // get the default perms for org.
                source.setAttr("system.permissions",
                        Json.Mapper.convertValue<Map<String,Any>>(permissionDao.getDefaultPermissionSchema(), Json.GENERIC_MAP))
            }

            if (source.links != null) {
                var links = managedValues.getAttr("system.links", LinkSchema::class.java)
                if (links == null) {
                    links = LinkSchema()
                }
                for (link in source.links!!) {
                    links.addLink(link.left, link.right)
                }
                source.setAttr("system.links", links)
            }
        }

        val result = indexDao.index(spec.sources!!)
        logger.info("Indexed result: {} task:{}", result, spec.taskId)

        spec.taskId?.let {
            val task = jobService.getTask(it)
            jobService.incrementAssetCounts(task, result)
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
        indexDao.removeFields(id, fields, false)
    }

    override fun removeLink(type: String, value: String, assets: List<String>): Map<String, List<Any>> {
        return indexDao.removeLink(type, value, assets)
    }

    override fun appendLink(type: String, value: String, assets: List<String>): Map<String, List<Any>> {
        return indexDao.appendLink(type, value, assets)
    }

    override fun exists(path: Path): Boolean {
        return indexDao.exists(path)
    }

    override fun exists(id: String): Boolean {
        return indexDao.exists(id)
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Long {

        val asset = indexDao[assetId]
        val write = asset.getAttr("system.permissions.write", Json.SET_OF_UUIDS)

        if (!hasPermission(write)) {
            throw ArchivistWriteException("You cannot make changes to this asset.")
        }

        val copy = Maps.newHashMap(attrs)
        /**
         * Remove keys which are maintained via other methods.
         */
        NS_PROTECTED_API.forEach { n -> copy.remove(n) }
        return indexDao.update(assetId, copy)
    }

    /**
     * Batch delete the the given asset IDs, along with their supporting files.
     * This method propagates to children as well.
     *
     * @param assetIds: the IDs of the assets the delete.
     */
    override fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse {
        /*
         * Setup an OR search where we target both the parents and children.
         */
        val search = AssetSearch()
        search.filter = AssetFilter()
        search.filter.should = listOf(
                AssetFilter().addToTerms("_id", assetIds).addToMissing("media.clip.parent"),
                AssetFilter().addToTerms("media.clip.parent", assetIds))

        /*
         * Iterate a scan and scroll and batch delete each batch.
         * Along the way queue up work to delete any files.
         */
        val rsp = BatchDeleteAssetsResponse()
        searchService.scanAndScroll(search, true) { hits->
            /*
             * Determine if any documents are on hold.
             */
            val okToDelete = mutableListOf<Document>()
            for (hit in hits.hits) {
                val doc = Document(hit.id, hit.sourceAsMap)
                if (doc.attrExists("system.hold") && doc.getAttr("system.hold", Boolean::class.java)) {
                    rsp.onHold = rsp.onHold + 1
                }
                else {
                    okToDelete.add(doc)
                }
            }

            rsp + indexDao.batchDelete(okToDelete.map { it.id })
            GlobalScope.launch {
                okToDelete.forEach {
                    deleteAssociatedFiles(it)
                }
            }
        }
        return rsp
    }

    override fun delete(assetId: String): Boolean {
        val doc = indexDao[assetId]
        deleteAssociatedFiles(doc)
        return indexDao.delete(assetId)
    }

    fun deleteAssociatedFiles(doc: Document) {
        logger.event("delete Proxy", mapOf("assetId" to doc.id))
        doc.getAttr("proxies", ProxySchema::class.java)?.let {
            it.proxies?.forEach { pr ->
                try {
                    val storage = fileStorageService.get(pr.id as String)
                    val ofile = fileServerProvider.getServableFile(storage.uri)
                    if (!ofile.delete()) {
                        logger.warnEvent("delete Proxy", "file did not exist", mapOf("proxyId" to pr.id))
                    }
                } catch (e: Exception) {
                    logger.warnEvent("delete Proxy", e.message!!, mapOf("proxyId" to pr.id), e)
                }
            }
        }
    }

    override fun getMapping(): Map<String, Any> {
        return indexDao.getMapping()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IndexServiceImpl::class.java)

        /**
         * Namespaces that are only populated via the API.  IF people manipulate these
         * wrong via the asset API then it would corrupt the asset.
         */
        private val NS_PROTECTED_API = ImmutableSet.of(
                "zorroa", "tmp")
    }

}

class AssetIndexSpec {

    var sources: List<Document>? = null
    var jobId: UUID? = null
    var taskId: UUID? = null

    constructor(sources: List<Document>) {
        this.sources = ImmutableList.copyOf(sources)
    }

    fun setJobId(jobId: UUID): AssetIndexSpec {
        this.jobId = jobId
        return this
    }

    fun setTaskId(taskId: UUID): AssetIndexSpec {
        this.taskId = taskId
        return this
    }

    fun setSources(sources: List<Document>): AssetIndexSpec {
        this.sources = sources
        return this
    }
}
