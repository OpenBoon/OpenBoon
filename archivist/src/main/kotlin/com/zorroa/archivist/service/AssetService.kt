package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.repository.AuditLogDao
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
import org.slf4j.Logger
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
    fun get(assetId: String): Document
    fun delete(assetId: String): Boolean
    fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse
    fun batchCreateOrReplace(spec: BatchCreateAssetsRequest) : BatchCreateAssetsResponse
    fun createOrReplace(doc: Document) : Document
    fun update(assetId: String, attrs: Map<String, Any>) : Document
}

/**
 * PreppedAssets is the result of preparing assets to be indexed.
 *
 * @property assets a list of assets prepped and ready to be ingested.
 * @property auditLogs uncommitted field change logs detected for each asset
 */
class PreppedAssets(
        val assets: List<Document>,
        val auditLogs: List<AuditLogEntrySpec>)


open class AbstractAssetService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var assetDao: AssetDao

    @Autowired
    lateinit var auditLogDao: AuditLogDao

    @Autowired
    lateinit var permissionDao: PermissionDao

    @Autowired
    lateinit var dyHierarchyService: DyHierarchyService

    @Autowired
    lateinit var indexService: IndexService

    @Autowired
    lateinit var taxonomyService: TaxonomyService

    @Autowired
    lateinit var jobService: JobService


    /**
     * Prepare a list of assets to be replaced or replaced.  Handles:
     *
     * - Removing tmp/system namespaces
     * - Applying the organization Id
     * - Applying modified / created times
     * - Applying default permissions
     * - Applying links
     * - Detecting changes and watched fields
     *
     * Return a PreppedAssets object which contains the updated assets as well as
     * the field change audit logs.  The audit logs for successful assets are
     * added to the audit log table.
     *
     * @param assets The list of assets to prepare
     * @return PreppedAssets
     */
    fun prepAssets(assets: List<Document>) : PreppedAssets  {

        val existingDocs = assetDao.getMap(assets.map{it.id})
        val orgId = getOrgId()
        val defaultPermissions = Json.Mapper.convertValue<Map<String,Any>>(
                permissionDao.getDefaultPermissionSchema(), Json.GENERIC_MAP)
        val watchedFields = properties.getList("archivist.auditlog.watched-fields")
        val watchedFieldsLogs = mutableListOf<AuditLogEntrySpec>()

        logger.info("Prepping ${assets.size} assets, pulled ${existingDocs.size} existing assets. ")

        return PreppedAssets(assets.map { newSource->

            val existingSource : Document = existingDocs.getOrDefault(newSource.id, Document(newSource.id))

            /**
             * Remove parts protected by API.
             */
            PROTECTED_NAMESPACES.forEach { n -> newSource.removeAttr(n) }

            newSource.setAttr("system.organizationId", orgId)

            handleTimes(existingSource, newSource)
            handleHold(existingSource, newSource)
            handlePermissions(existingSource, newSource, defaultPermissions)
            handleLinks(existingSource, newSource)

            if (watchedFields.isNotEmpty()) {
                watchedFieldsLogs.addAll(handleWatchedFieldChanges(watchedFields, existingSource, newSource))
                println(watchedFields.size)
            }

             newSource
         }, watchedFieldsLogs)
    }

    /**
     * Detects if there are value changes on a watched field and returns them as a list of AuditLogEntrySpec
     *
     * @param fields the list of fields to watch
     * @param oldAsset the original asset
     * @param newAsset the new asset
     * @return a list of AuditLogEntrySpec to describe the changes
     */
    private fun handleWatchedFieldChanges(fields: List<String>, oldAsset: Document, newAsset: Document): List<AuditLogEntrySpec> {
        return fields.map {
            if (oldAsset == null && newAsset.attrExists(it)) {
                AuditLogEntrySpec(
                        oldAsset.id,
                        AuditLogType.Changed,
                        field=it,
                        value=newAsset.getAttr(it))
            }
            else if (oldAsset.getAttr(it, Any::class.java) != newAsset.getAttr(it, Any::class.java)) {
                AuditLogEntrySpec(
                        oldAsset.id,
                        AuditLogType.Changed,
                        field=it,
                        value=newAsset.getAttr(it))
            }
            else {
                null
            }
        }.filterNotNull()
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
        } else {
            newAsset.setAttr("system.timeModified", time)
            newAsset.setAttr("system.timeCreated", time)
        }
    }

    /**
     * Handles re-applying the hold if any.
     *
     * @param oldAsset the original asset
     * @param newAsset the new asset
     */
    private fun handleHold(oldAsset: Document, newAsset: Document) {
        if (oldAsset.attrExists("system.hold")) {
            newAsset.setAttr("system.hold", oldAsset.getAttr("system.hold"))
        }
    }

    /**
     * Handles checking the new asset for links and merging then with links from old asset.
     *
     * @param oldAsset the original asset
     * @param newAsset the new asset
     */
    private fun handleLinks(oldAsset: Document, newAsset: Document) {
        if (newAsset.links != null) {
            var links = oldAsset.getAttr("system.links", LinkSchema::class.java)
            if (links == null) {
                links = LinkSchema()
            }
            newAsset.links?.forEach {
                links.addLink(it.left, it.right)
            }
            newAsset.setAttr("system.links", links)
        }
    }

    /**
     * Handles checking the new asset for permissions and merging them with old asset.
     *
     * @param oldAsset the original asset
     * @param newAsset the new asset
     * @param defaultPermissions The default permissions as set in application.properties
     */
    private fun handlePermissions(oldAsset: Document, newAsset: Document, defaultPermissions: Map<String, Any>?) {

        var existingPerms = oldAsset.getAttr("system.permissions",
                PermissionSchema::class.java)

        if (existingPerms == null) {
            existingPerms = PermissionSchema()
        }

        if (newAsset.permissions != null) {
            newAsset.permissions?.forEach {
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
            newAsset.setAttr("system.permissions",
                    Json.Mapper.convertValue<Map<String, Any>>(existingPerms, Json.GENERIC_MAP))

        } else if (existingPerms.isEmpty) {
            /**
             * If the source didn't come with any permissions and the current perms
             * on the asset are empty, we apply the default permissions.
             */
            newAsset.setAttr("system.permissions", defaultPermissions)
        }
    }

    /**
     * Apply the watched field audit logs for any asset that was created or replaced.
     */
    fun auditLogChanges(prepped: PreppedAssets, rsp: BatchCreateAssetsResponse) {
        auditLogDao.batchCreate(prepped.auditLogs.filter {
            val strId = it.assetId.toString()
            strId in rsp.createdAssetIds || strId in rsp.replacedAssetIds
        })
        // Create audit logs for created and replaced entries.
        auditLogDao.batchCreate(rsp.createdAssetIds.map { AuditLogEntrySpec(it, AuditLogType.Created) })
        auditLogDao.batchCreate(rsp.replacedAssetIds.map { AuditLogEntrySpec(it, AuditLogType.Replaced) })
    }

    /**
     * Run Dyhis and taxons. This should be called if assets are added, removed, or updated.
     */
    fun runDyhiAndTaxons() {
        dyHierarchyService.submitGenerateAll(true)
        taxonomyService.runAllAsync()
    }

    /**
     * Increment any job counters for index requests coming from the job system.
     */
    fun incrementJobCounters(req: BatchCreateAssetsRequest, rsp: BatchCreateAssetsResponse) {
        req.taskId?.let {
            val task = jobService.getTask(it)
            jobService.incrementAssetCounts(task, rsp)
        }
    }

    companion object {

        val PROTECTED_NAMESPACES = setOf("system", "tmp")
        val logger : Logger = LoggerFactory.getLogger(AbstractAssetService::class.java)
    }

}

/**
 * IrmAssetServiceImpl is a higher level wrapper around the CoreDataVaultClient. Authentication
 * is required.
 */
class IrmAssetServiceImpl constructor(private val cdvClient: CoreDataVaultClient) : AbstractAssetService(), AssetService {

    override fun get(assetId: String): Document {
        return cdvClient.getIndexedMetadata(getCompanyId(), assetId)
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
        cdvClient.batchUpdateIndexedMetadata(getCompanyId(), prepped.assets)
        val result = indexService.index(prepped.assets)
        incrementJobCounters(spec, result)
        if (result.assetsChanged()) {
            auditLogChanges(prepped, result)
            runDyhiAndTaxons()
        }
        return result
    }

    override fun createOrReplace(doc: Document) : Document {
        val prepped = prepAssets(listOf(doc))
        cdvClient.batchUpdateIndexedMetadata(getCompanyId(), prepped.assets)
        val result = indexService.index(prepped.assets)
        auditLogChanges(prepped, result)
        runDyhiAndTaxons()
        return get(doc.id)
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Document {

        val asset = cdvClient.getIndexedMetadata(getCompanyId(), assetId)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("update access denied")
        }
        val updated = indexService.update(asset, attrs)
        cdvClient.updateIndexedMetadata(getCompanyId(), assetId, updated)
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

    override fun get(assetId: String): Document {
        return assetDao.get(assetId)
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
        val txResult  = assetDao.batchCreateOrReplace(prepped.assets)

        if (txResult != prepped.assets.size) {
            logger.warnEvent("batchUpsert Asset",
                    "Number of assets indexed did not match number in DB.",
                    mapOf())
        }

        val rsp = indexService.index(prepped.assets)
        incrementJobCounters(spec, rsp)
        if (rsp.assetsChanged()) {
            auditLogChanges(prepped, rsp)
            runDyhiAndTaxons()
        }
        return rsp
    }

    override fun createOrReplace(doc: Document): Document {
        val prepped = prepAssets(listOf(doc))
        assetDao.createOrReplace(prepped.assets[0])
        val rsp = indexService.index(prepped.assets)
        val result =  prepped.assets[0]
        auditLogChanges(prepped, rsp)
        runDyhiAndTaxons()
        return result
    }

    override fun update(assetId: String, attrs: Map<String, Any>) : Document {
        val asset = get(assetId)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("update access denied")
        }

        val updated = indexService.update(asset, attrs)
        assetDao.createOrReplace(updated)
        runDyhiAndTaxons()
        return asset
    }
}




