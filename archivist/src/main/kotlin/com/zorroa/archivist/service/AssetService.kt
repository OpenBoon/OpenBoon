package com.zorroa.archivist.service

import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.util.warnEvent
import com.zorroa.common.clients.CoreDataVaultClient
import com.zorroa.common.domain.ArchivistSecurityException
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.schema.LinkSchema
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.lang.Exception
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
    fun getDocument(assetId: String): Document
    fun setDocument(assetId: String, doc: Document)
    fun delete(assetId: String): Boolean
    fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse
    fun batchCreateOrReplace(spec: BatchCreateAssetsRequest) : BatchCreateAssetsResponse
    fun createOrReplace(doc: Document) : Document
    fun update(assetId: String, attrs: Map<String, Any>) : Document
}


open class AbstractAssetService {

    @Autowired
    lateinit var assetDao: AssetDao

    @Autowired
    lateinit var permissionDao: PermissionDao

    @Autowired
    lateinit var dyHierarchyService: DyHierarchyService

    @Autowired
    lateinit var indexService: IndexService

    @Autowired
    lateinit var taxonomyService: TaxonomyService


    fun prepAssets(assets: List<Document>) : List<Document>  {

        val existingDocs = assetDao.getMap(assets.map{it.id})
        val orgId = getOrgId()
        val defaultPermissions =   Json.Mapper.convertValue<Map<String,Any>>(
                permissionDao.getDefaultPermissionSchema(), Json.GENERIC_MAP)

        return assets.map { source->

             val existingSource : Document = existingDocs.getOrDefault(source.id, Document(source.id))

             /**
              * Remove parts protected by API.
              */
             PROTECTED_NAMESPACES.forEach { n -> source.removeAttr(n) }

             /**
              * Add the organization
              */
             source.setAttr("system.organizationId", orgId)

             /**
              * Update created and modified times.
              */
             val time = Date()

             if (existingSource.attrExists("system.timeCreated")) {
                 source.setAttr("system.timeModified", time)
             }
             else {
                 source.setAttr("system.timeModified", time)
                 source.setAttr("system.timeCreated", time)
             }


            var hold : Any? = existingSource.getAttr("system.hold", Any::class.java)
            if (hold != null) {
                source.setAttr("system.hold", hold)
            }

             /**
              * Handle permissions assigned from processing.
              */
             var existingPerms = existingSource.getAttr("system.permissions",
                     PermissionSchema::class.java)

             if (existingPerms == null) {
                 existingPerms = PermissionSchema()
             }

             if (source.permissions != null) {
                 source.permissions?.forEach {
                     val key = it.key
                     val value = it.value
                     try {
                         val perm = permissionDao.get(key)
                         if (value and 1 == 1) {
                             existingPerms.addToRead(perm.id)
                         } else {
                             existingPerms.removeFromRead(perm.id)
                         }

                         if (value and 2 == 2) {
                             existingPerms.addToWrite(perm.id)
                         } else {
                             existingPerms.removeFromWrite(perm.id)
                         }

                         if (value and 4 == 4) {
                             existingPerms.addToExport(perm.id)
                         } else {
                             existingPerms.removeFromExport(perm.id)
                         }
                     } catch (e: Exception) {
                         logger.warn("Permission not found: {}", key)
                     }
                 }
                 source.setAttr("system.permissions",
                         Json.Mapper.convertValue<Map<String,Any>>(existingPerms, Json.GENERIC_MAP))

             } else if (existingPerms.isEmpty) {
                 /**
                  * If the source didn't come with any permissions and the current perms
                  * on the asset are empty, we apply the default permissions.
                  */
                 source.setAttr("system.permissions", defaultPermissions)
             }

             if (source.links != null) {
                 var links = existingSource.getAttr("system.links", LinkSchema::class.java)
                 if (links == null) {
                     links = LinkSchema()
                 }
                 source.links?.forEach {
                     links.addLink(it.left, it.right)
                 }
                 source.setAttr("system.links", links)
             }

             source
         }
    }

    fun runDyhiAndTaxons() {
        dyHierarchyService.submitGenerateAll(true)
        taxonomyService.runAllAsync()
    }

    companion object {

        val PROTECTED_NAMESPACES = setOf("system", "tmp")

        val logger = LoggerFactory.getLogger(AbstractAssetService::class.java)
    }

}

/**
 * IrmAssetServiceImpl is a higher level wrapper around the CoreDataVaultClient. Authentication
 * is required.
 */
class IrmAssetServiceImpl constructor(private val cdvClient: CoreDataVaultClient) : AbstractAssetService(), AssetService {

    override fun getDocument(assetId: String): Document {
        return cdvClient.getIndexedMetadata(getCompanyId(), assetId)
    }

    override fun setDocument(assetId: String, doc: Document) {
        cdvClient.updateIndexedMetadata(getCompanyId(), assetId, doc)
    }

    override fun delete(assetId: String): Boolean {
        /**
         * Relying on IRM's security to know if the asset can be deleted.
         */
        if (cdvClient.delete(getCompanyId(), assetId)) {
            return indexService.delete(assetId)
        }
        return false
    }

    override fun batchDelete(ids: List<String>): BatchDeleteAssetsResponse {
        /**
         * Relying on IRM's security to know if the assets can be deleted.
         */
        val deleted = cdvClient.batchDelete(getCompanyId(), ids)
        // Only delete from index stuff we deleted from CDV?
        val result =  indexService.batchDelete(ids.minus(deleted.filterValues { v-> v }.keys))
        if (result.totalDeleted > 0) {
            runDyhiAndTaxons()
        }
        return result
    }

    override fun batchCreateOrReplace(spec: BatchCreateAssetsRequest) : BatchCreateAssetsResponse {
        val prepped = prepAssets(spec.sources)
        cdvClient.batchUpdateIndexedMetadata(getCompanyId(), prepped)
        val result = indexService.index(prepped)
        if (result.replaced > 0 || result.created > 0) {
            runDyhiAndTaxons()
        }
        return result
    }

    override fun createOrReplace(doc: Document) : Document {
        val prepped = prepAssets(listOf(doc))
        cdvClient.batchUpdateIndexedMetadata(getCompanyId(), prepped)
        indexService.index(prepped)
        runDyhiAndTaxons()
        return prepped[0]
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Document {
        val asset = indexService.get(assetId)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("update access denied")
        }

        indexService.update(assetId, attrs)
        val updated = indexService.get(assetId)
        setDocument(assetId, updated)
        runDyhiAndTaxons()
        return updated
    }

    /**
     * Pull the company ID from the authed user Attrs
     */
    fun getCompanyId() : Int {
        try {
            return getUser().attrs["company_id"].toString().toInt()
        }
        catch (e: Exception) {
            throw ArchivistSecurityException("Invalid company Id")
        }
    }
}

@Transactional
class AssetServiceImpl : AbstractAssetService(), AssetService {

    @Autowired
    lateinit var searchService: SearchService

    @Autowired
    lateinit var jobService: JobService


    override fun getDocument(assetId: String): Document {
        return indexService.get(assetId)
    }

    override fun setDocument(id: String, doc: Document) {
        indexService.index(doc)
    }

    override fun delete(id: String): Boolean {
        val asset = indexService.get(id)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("delete access denied")
        }
        val result = indexService.delete(id)
        if (result) {
            runDyhiAndTaxons()
        }
        return result
    }

    override fun batchDelete(ids: List<String>): BatchDeleteAssetsResponse {
       val result =  indexService.batchDelete(ids)
        if (result.totalDeleted > 0) {
            runDyhiAndTaxons()
        }
        return result
    }

    override fun batchCreateOrReplace(spec: BatchCreateAssetsRequest) : BatchCreateAssetsResponse {
        /**
         * We have to do this backwards here because we're relying on ES to
         * merge existing docs and updates together.
         */
        val prepped = prepAssets(spec.sources)
        val txResult  = assetDao.batchCreateOrReplace(prepped)

        if (txResult != prepped.size) {
            logger.warnEvent("batchUpsert Asset",
                    "Number of assets indexed did not match number in DB.",
                    mapOf())
        }

        val rsp = indexService.index(prepped)
        spec.taskId?.let {
            val task = jobService.getTask(it)
            jobService.incrementAssetCounts(task, rsp)
        }

        if (rsp.created > 0 || rsp.replaced > 0) {
            runDyhiAndTaxons()
        }
        return rsp
    }

    override fun createOrReplace(doc: Document): Document {
        val prepped = prepAssets(listOf(doc))
        assetDao.createOrReplace(prepped[0])
        indexService.index(prepped)
        val result =  prepped[0]
        runDyhiAndTaxons()
        return result
    }

    override fun update(assetId: String, attrs: Map<String, Any>) : Document {
        val asset = indexService.get(assetId)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("update access denied")
        }

        indexService.update(assetId, attrs)
        val updated = indexService.get(assetId)
        assetDao.createOrReplace(updated)
        runDyhiAndTaxons()
        return asset
    }
}




