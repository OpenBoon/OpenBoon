package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.archivist.repository.FieldEditDao
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.security.*
import com.zorroa.common.clients.CoreDataVaultAssetSpec
import com.zorroa.common.clients.CoreDataVaultClient
import com.zorroa.common.clients.RestClientException
import com.zorroa.common.domain.ArchivistSecurityException
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.net.URI
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
    fun getAll(assetIds: List<String>): List<Document>
    fun delete(assetId: String): Boolean
    fun createFieldEdit(spec: FieldEditSpec): FieldEdit
    fun deleteFieldEdit(edit: FieldEdit): Boolean
    fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse
    fun batchUpdate(batch: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse
    fun batchUpdate(assets: List<Document>, reindex: Boolean=true, taxons: Boolean=true) : BatchUpdateAssetsResponse
    fun batchCreateOrReplace(spec: BatchCreateAssetsRequest) : BatchCreateAssetsResponse
    fun createOrReplace(doc: Document) : Document
    fun update(assetId: String, attrs: Map<String, Any>) : Document
    fun update(assetId: String, req: UpdateAssetRequest) : BatchUpdateAssetsResponse
    fun removeLinks(type: LinkType, value: UUID, assets: List<String>): UpdateLinksResponse
    fun addLinks(type: LinkType, value: UUID, req: BatchUpdateAssetLinks): UpdateLinksResponse
    fun setPermissions(spec: BatchUpdatePermissionsRequest) : BatchUpdatePermissionsResponse
    fun handleAssetUpload(name: String, bytes: ByteArray) : AssetUploadedResponse
    fun getFieldSets(assetId: String) : List<FieldSet>
}

/**
 * PreppedAssets is the result of preparing assets to be indexed.
 *
 * @property assets a list of assets prepped and ready to be ingested.
 * @property auditLogs uncommitted field change logs detected for each asset
 */
class PreppedAssets(
        val assets: List<Document>,
        val auditLogs: List<AuditLogEntrySpec>,
        val scope: String)


open abstract class AbstractAssetService : AssetService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var auditLogDao: AuditLogDao

    @Autowired
    lateinit var permissionDao: PermissionDao

    @Autowired
    lateinit var fieldEditDao: FieldEditDao

    @Autowired
    lateinit var dyHierarchyService: DyHierarchyService

    @Autowired
    lateinit var indexService: IndexService

    @Autowired
    lateinit var taxonomyService: TaxonomyService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var searchService: SearchService

    @Autowired
    lateinit var fieldSystemService: FieldSystemService

    @Autowired
    lateinit var fieldService: FieldService

    @Autowired
    lateinit var clusterLockExecutor: ClusterLockExecutor

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
    fun prepAssets(req: BatchCreateAssetsRequest): PreppedAssets {
        if (req.skipAssetPrep) {
            return PreppedAssets(req.sources, listOf(), req.scope)
        }

        val assets = req.sources
        val orgId = getOrgId()
        val defaultPermissions = Json.Mapper.convertValue<Map<String, Any>>(
                permissionDao.getDefaultPermissionSchema(), Json.GENERIC_MAP)
        val watchedFields = properties.getList("archivist.auditlog.watched-fields")
        val watchedFieldsLogs = mutableListOf<AuditLogEntrySpec>()

        return PreppedAssets(assets.map { newSource ->

            val existingSource: Document = try {
                get(newSource.id)
            } catch (e: Exception) {
                Document(newSource.id)
            }

            /**
             * Remove parts protected by API.
             */
            PROTECTED_NAMESPACES.forEach { n -> newSource.removeAttr(n) }

            newSource.setAttr("system.organizationId", orgId)

            handleTimes(existingSource, newSource)
            handleHold(existingSource, newSource)
            handlePermissions(existingSource, newSource, defaultPermissions)
            handleLinks(existingSource, newSource)
            fieldSystemService.applyFieldEdits(newSource)

            if (watchedFields.isNotEmpty()) {
                watchedFieldsLogs.addAll(handleWatchedFieldChanges(watchedFields, existingSource, newSource))
            }

            newSource
        }, watchedFieldsLogs, req.scope)
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
                        attrName = it,
                        value = newAsset.getAttr(it))
            } else if (oldAsset.getAttr(it, Any::class.java) != newAsset.getAttr(it, Any::class.java)) {
                AuditLogEntrySpec(
                        oldAsset.id,
                        AuditLogType.Changed,
                        attrName = it,
                        value = newAsset.getAttr(it))
            } else {
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
        var links = oldAsset.getAttr("system.links", LinkSchema::class.java) ?: LinkSchema()
        newAsset.links?.forEach {
            links.addLink(it.left, it.right.toString())
        }
        newAsset.setAttr("system.links", links)
    }

    /**
     * Handles checking the new asset for permissions and merging them with old asset.
     *
     * @param oldAsset the original asset
     * @param newAsset the new asset
     * @param defaultPermissions The default permissions as set in application.properties
     */
    private fun handlePermissions(oldAsset: Document, newAsset: Document, defaultPermissions: Map<String, Any>?) {

        val existingPerms = oldAsset.getAttr("system.permissions",
                PermissionSchema::class.java) ?: PermissionSchema()

        if (newAsset.permissions != null) {
            newAsset.permissions?.forEach {
                val key = it.key
                val value = it.value
                try {
                    val perm = permissionDao.get(key)
                    applyAclToPermissions(perm.id, value, existingPerms)
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
        auditLogDao.batchCreate(rsp.createdAssetIds.map {
            AuditLogEntrySpec(it, AuditLogType.Created, scope = prepped.scope)
        })
        auditLogDao.batchCreate(rsp.replacedAssetIds.map {
            AuditLogEntrySpec(it, AuditLogType.Replaced, scope = prepped.scope)
        })
    }

    /**
     * Run Dyhis and taxons. This should be called if assets are added, removed, or updated.
     */
    fun runDyhiAndTaxons() {
        val orgId = getOrgId()
        clusterLockExecutor.execute(ClusterLockSpec.combineLock("dyhi-taxons-$orgId")
                .apply { authentication = getAuthentication() }) {
            dyHierarchyService.generateAll()
            taxonomyService.tagAll()
        }
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

    /**
     * Index a batch of PreppedAssets
     */
    fun indexAssets(req: BatchCreateAssetsRequest?, prepped: PreppedAssets,
                    batchUpdateResult: Map<String, Boolean> = mapOf()): BatchCreateAssetsResponse {

        // Filter out the docs that didn't make it into the DB, but default allow anything else to go in.
        val docsToIndex = prepped.assets.filter {
            batchUpdateResult.getOrDefault(it.id, true)
        }

        val rsp = indexService.index(docsToIndex)
        if (req != null) {
            incrementJobCounters(req, rsp)
        }
        if (rsp.assetsChanged()) {
            auditLogChanges(prepped, rsp)
            runDyhiAndTaxons()
        }
        return rsp
    }

    /**
     * Apply a permission and access level to a PermissionSchema
     */
    fun applyAclToPermissions(permissionId: UUID, access: Int, perms: PermissionSchema) {

        if (access == 0) {
            perms.removeFromRead(permissionId)
            perms.removeFromWrite(permissionId)
            perms.removeFromExport(permissionId)
        } else {
            if (access and 1 == 1) {
                perms.addToRead(permissionId)
            } else {
                perms.removeFromRead(permissionId)
            }

            if (access and 2 == 2) {
                perms.addToWrite(permissionId)
            } else {
                perms.removeFromWrite(permissionId)
            }

            if (access and 4 == 4) {
                perms.addToExport(permissionId)
            } else {
                perms.removeFromExport(permissionId)
            }
        }
    }

    /**
     * Add link to the given Document.
     */
    fun addLink(doc: Document, type: LinkType, target: UUID): Boolean {
        var links: LinkSchema? = doc.getAttr("system.links", LinkSchema::class.java)
        if (links == null) {
            links = LinkSchema()
        }
        return if (links.addLink(type, target)) {
            doc.setAttr("system.links", links)
            true
        } else {
            false
        }
    }

    /**
     * Remove link to the given Document.
     */
    fun removeLink(doc: Document, type: LinkType, target: UUID): Boolean {
        var links: LinkSchema? = doc.getAttr("system.links", LinkSchema::class.java)
        if (links == null) {
            links = LinkSchema()
        }
        return if (links.removeLink(type, target)) {
            doc.setAttr("system.links", links)
            true
        } else {
            false
        }
    }

    /**
     * Apply ACL to a given Document.
     */
    fun applyAcl(doc: Document, replace: Boolean, acl: Acl) {
        val perms = if (replace) {
            PermissionSchema()
        } else {
            var existingPerms: PermissionSchema? = doc.getAttr("system.permissions", PermissionSchema::class.java)
            existingPerms ?: PermissionSchema()
        }

        for (e in acl) {
            applyAclToPermissions(e.permissionId, e.access, perms)
        }
        doc.setAttr("system.permissions", perms)
    }

    override fun getFieldSets(assetId: String): List<FieldSet> {
        return fieldSystemService.getAllFieldSets(get(assetId))
    }

    /**
     * Batch update a list of assetIds with the given attributes.
     *
     * The nice thing about this method is that it works for both CDV and Postgres. The not nice
     * thing about this method is that is slower with CDV since it doesn't have any
     * batch operations.
     */
    override fun batchUpdate(assets: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {

        if (assets.batch.size > 1000) {
            throw java.lang.IllegalArgumentException("Cannot batch update more than 1000 assets at one time.")
        }

        /**
         * A utility function for checking if the attribute is not protected.
         * @param attr The attribute name in dot notation.
         * @param assetId The ID of the asset.
         * @param allowSystem If setting system attrs is allowed.
         */
        fun checkAttr(attr: String, assetId: String, allowSystem: Boolean): Boolean {
            if (!allowSystem && (attr == "system" || attr.startsWith("system."))) {
                logger.warnEvent(LogObject.ASSET,
                        LogAction.BATCH_UPDATE,
                        "Skipping setting $attr, cannot set system values on batch update",
                        mapOf("assetId" to assetId))
                return false
            }
            return true
        }

        /**
         * For setting modified time
         */
        val now = Date()

        /**
         * Iterate over our list of assets and grab the hard copy of each one.
         * Modify the copy and push it back into the DB.
         */
        val futures = assets.batch.keys.chunked(50).map { ids ->
            GlobalScope.async(CoroutineAuthentication(getSecurityContext())) {
                val rsp = BatchUpdateAssetsResponse()
                val docs: List<Document> = getAll(ids).mapNotNull { doc ->

                    if (!hasPermission("write", doc)) {
                        logger.warnEvent(LogObject.ASSET,
                                LogAction.BATCH_UPDATE,
                                "Skipping updating asset, access denied",
                                mapOf("assetId" to doc.id))
                        rsp.accessDeniedAssetIds.add(doc.id)
                        null
                    } else {

                        val req = assets.batch.getValue(doc.id)
                        var updated = false
                        req.update?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.setAttr(t, u)
                                updated = true
                            }
                        }

                        req.remove?.forEach {
                            if (checkAttr(it, doc.id, req.allowSystem)) {
                                doc.removeAttr(it)
                                updated = true
                            }
                        }

                        req.appendToList?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.addToAttr(t, u, unique = false)
                                updated = true
                            }
                        }

                        req.appendToUniqueList?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.addToAttr(t, u, unique = true)
                                updated = true
                            }
                        }

                        req.removeFromList?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.removeFromAttr(t, u)
                                updated = true
                            }
                        }

                        if (updated) {
                            doc.setAttr("system.timeModified", now)
                            doc
                        } else {
                            null
                        }
                    }
                }

                rsp.plus(batchUpdate(docs, reindex = true, taxons = false))
                rsp
            }
        }

        /**
         * Setup our response object
         */
        val rsp = BatchUpdateAssetsResponse()

        /**
         * Wait for all the batches to complete, then combine results.
         */
        runBlocking {
            futures.map {
                val r = it.await()
                logger.event(LogObject.ASSET, LogAction.BATCH_UPDATE,
                        mapOf("updateCount" to r.updatedAssetIds.size,
                                "errorCount" to r.erroredAssetIds.size))
                rsp.plus(r)
            }
        }

        runDyhiAndTaxons()
        return rsp
    }

    override fun update(assetId: String, req: UpdateAssetRequest): BatchUpdateAssetsResponse {
        val breq = BatchUpdateAssetsRequest(mapOf(assetId to req))
        return batchUpdate(breq)
    }


    override fun removeLinks(type: LinkType, value: UUID, assets: List<String>): UpdateLinksResponse {

        val auth = getAuthentication()
        val errorAssetIds = Collections.synchronizedSet(mutableSetOf<String>())
        val successAssetIds = Collections.synchronizedSet(mutableSetOf<String>())

        runBlocking(CoroutineAuthentication(getSecurityContext())) {
            assets.chunked(50) {
                launch {

                    val docs = getAll(it).mapNotNull { doc ->
                        if (removeLink(doc, type, value)) {
                            doc
                        } else {
                            // already removed
                            null
                        }
                    }
                    val update = batchUpdate(docs, reindex = true, taxons = false)
                    if (update.erroredAssetIds.isNotEmpty()) {
                        errorAssetIds.addAll(update.erroredAssetIds)
                    }
                    successAssetIds.addAll(update.updatedAssetIds)
                }
            }
        }

        return UpdateLinksResponse(successAssetIds, errorAssetIds)
    }

    override fun addLinks(type: LinkType, value: UUID, req: BatchUpdateAssetLinks): UpdateLinksResponse {
        val auth = getAuthentication()
        val errors = Collections.synchronizedSet(mutableSetOf<String>())
        val success = Collections.synchronizedSet(mutableSetOf<String>())

        runBlocking(CoroutineAuthentication(getSecurityContext())) {
            req.assetIds?.chunked(50) {
                launch {
                    val docs = getAll(it).mapNotNull { doc ->
                        if (addLink(doc, type, value)) {
                            doc
                        } else {
                            null
                        }
                    }
                    val update = batchUpdate(docs, reindex = true, taxons = false)
                    if (update.erroredAssetIds.isNotEmpty()) {
                        errors.addAll(update.erroredAssetIds)
                    }
                    success.addAll(update.updatedAssetIds)
                }
            }

            if (!req.parentIds.isNullOrEmpty() && req.search != null) {

                val search = req.search
                search.addToFilter().must = mutableListOf(AssetFilter()
                        .addToTerms("media.clip.parent", req.parentIds))

                searchService.scanAndScroll(search, false) { hits ->
                    launch {

                        val ids = hits.hits.map { hit -> hit.id }
                        val docs = getAll(ids).mapNotNull { doc ->
                            if (addLink(doc, type, value)) {
                                doc
                            } else {

                                null
                            }
                        }
                        logger.info("updating docs with links: {}", docs.size)
                        val update = batchUpdate(docs, reindex = true, taxons = false)
                        if (update.erroredAssetIds.isNotEmpty()) {
                            errors.addAll(update.erroredAssetIds)
                        }
                        success.addAll(update.updatedAssetIds)
                    }
                }
            }
        }
        return UpdateLinksResponse(success, errors)
    }

    @Transactional
    override fun deleteFieldEdit(edit: FieldEdit): Boolean {
        val asset = get(edit.assetId.toString())
        val field = fieldSystemService.getField(edit.fieldId)

        if (!hasPermission("write", asset)) {
            throw ArchivistSecurityException("update access denied")
        }

        val updateReq = if (edit.oldValue == null) {
            UpdateAssetRequest(remove = listOf(field.attrName),
                    removeFromList = mapOf("system.fieldEdits" to field.attrName),
                    allowSystem = true)
        } else {
            UpdateAssetRequest(mapOf(field.attrName to edit.oldValue),
                    removeFromList = mapOf("system.fieldEdits" to field.attrName),
                    allowSystem = true)
        }

        if (fieldEditDao.delete(edit.id)) {
            val rsp = update(asset.id, updateReq)
            if (rsp.isSuccess()) {
                val aspec = AuditLogEntrySpec(asset.id,
                        AuditLogType.Changed,
                        fieldId = field.id,
                        attrName = field.attrName,
                        scope = "undo edit",
                        value = edit.oldValue)
                auditLogDao.create(aspec)
                return true
            }
            else {
                throw rsp.getThrowableError()
            }
        }

        throw ArchivistWriteException("Unable to find field edit: ${edit.id}")
    }

    @Transactional
    override fun createFieldEdit(spec: FieldEditSpec): FieldEdit {
        val assetId = spec.assetId.toString()
        val asset = get(assetId)
        val field = fieldSystemService.getField(spec)

        if (!field.editable) {
            throw IllegalStateException("The field ${field.name} is not editable")
        }

        if (!field.attrType.isValid(spec.newValue)) {
            throw java.lang.IllegalArgumentException("The value ${spec.newValue} " +
                    "for field ${field.name} is not the correct type")
        }

        val updateReq = if (spec.newValue == null) {
            UpdateAssetRequest(
                    remove = listOf(field.attrName),
                    appendToUniqueList = mapOf("system.fieldEdits" to field.attrName),
                    allowSystem = true)
        } else {
            UpdateAssetRequest(mapOf(field.attrName to spec.newValue),
                    appendToUniqueList = mapOf("system.fieldEdits" to field.attrName),
                    allowSystem = true)
        }

        val rsp = update(assetId, updateReq)
        if (rsp.isSuccess()) {
            val oldValue = asset.getAttr(field.attrName, Any::class.java)
            val ispec = FieldEditSpecInternal(
                    UUID.fromString(asset.id),
                    field.id,
                    spec.newValue,
                    oldValue)
            val fieldEdit = fieldEditDao.create(ispec)

            val aspec = AuditLogEntrySpec(assetId,
                    AuditLogType.Changed,
                    fieldId = field.id,
                    attrName = field.attrName,
                    scope = "manual edit",
                    value = spec.newValue)
            auditLogDao.create(aspec)
            return fieldEdit
        }
        else {
            throw rsp.getThrowableError()
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
open class IrmAssetServiceImpl constructor(
        private val cdvClient: CoreDataVaultClient) : AbstractAssetService(), AssetService {

    @Autowired
    lateinit var organizationService: OrganizationService

    override fun get(assetId: String): Document {
        logger.event(LogObject.ASSET, LogAction.GET, mapOf("assetId" to assetId, "datastore" to "CDV"))
        return cdvClient.getIndexedMetadata(getCompanyId(), assetId)
    }

    override fun delete(assetId: String): Boolean {

        val asset = indexService.get(assetId)
        /**
         * If the asset is a clip, then just delete it from the index.
         * No need to contact CDV.
         */
        if (asset.attrExists("media.clip.parent")) {
            return indexService.delete(assetId)
        }
        else {
            /**
             * Relying on IRM's security to know if the asset can be deleted.
             */
            if (cdvClient.delete(getCompanyId(), assetId)) {
                return indexService.delete(assetId)
            }

            return false
        }
    }

    override fun batchDelete(ids: List<String>): BatchDeleteAssetsResponse {
        /**
         * Relying on IRM's security to know if the assets can be deleted.
         */
        val deleted = cdvClient.batchDelete(getCompanyId(), ids)
        val result =  indexService.batchDelete(deleted.keys.toList())
        if (result.deletedAssetIds.isNotEmpty()) {
            runDyhiAndTaxons()
        }
        return result
    }

    override fun batchCreateOrReplace(spec: BatchCreateAssetsRequest) : BatchCreateAssetsResponse {
        val prepped = prepAssets(spec)
        val parentsOnly = prepped.assets.filter { !it.attrExists("media.clip.parent") }

        // Only parents go into the CDV
        val result = cdvClient.batchUpdateIndexedMetadata(getCompanyId(), parentsOnly)
        return indexAssets(spec, prepped, result)
    }

    override fun createOrReplace(doc: Document) : Document {
        val prepped = prepAssets(BatchCreateAssetsRequest(listOf(doc)))
        // Only send parent assets to CDV

        val updated = if (!doc.attrExists("media.clip.parent")) {
            cdvClient.updateIndexedMetadata(getCompanyId(), prepped.assets[0])
        }
        else {
            true
        }
        if (updated) {
            indexAssets(null, prepped)
        }
        return get(doc.id)
    }

    override fun batchUpdate(assets: List<Document>, reindex: Boolean, taxons: Boolean): BatchUpdateAssetsResponse {
        val rsp = BatchUpdateAssetsResponse()
        for (asset in assets) {
            // Skip assets with a parent.
            if (asset.attrExists("media.clip.parent")) {
                rsp.updatedAssetIds.add(asset.id)
                continue
            }
            // doesn't throw any exceptions.
            if (cdvClient.updateIndexedMetadata(getCompanyId(), asset)) {
                rsp.updatedAssetIds.add(asset.id)
            }
            else {
                rsp.erroredAssetIds.add(asset.id)
                logger.warnEvent(LogObject.ASSET,
                        LogAction.BATCH_UPDATE, "Asset not found", mapOf("assetId" to asset.id))
            }
        }
        if (reindex) {
            indexService.index(assets)
        }
        if (taxons) {
            runDyhiAndTaxons()
        }
        return rsp
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Document {

        val asset = cdvClient.getIndexedMetadata(getCompanyId(), assetId)
        if (!hasPermission("write", asset)) {
            throw ArchivistSecurityException("update access denied")
        }

        val updated = indexService.update(asset, attrs)
        cdvClient.updateIndexedMetadata(getCompanyId(), updated)
        runDyhiAndTaxons()
        return updated
    }

    /**
     * Pull the company ID from the authed user Attrs
     */
    fun getCompanyId() : Int {
        return try {
            // Check for the user's company ID. This works for users logged
            // in via SAML.
            getUser().attrs["company_id"].toString().toInt()
        }
        catch (e: Exception) {
            // Check the magical org name.  This works for magical users that don't
            // have SAML attributes.
            try {
                val org = organizationService.get(getOrgId())
                org.name.split("-", limit = 2)[1].toInt()
            } catch (e2: Exception) {
                throw ArchivistSecurityException("Cannot determine a valid IRM company ID.")
            }
        }
    }


    override fun setPermissions(spec: BatchUpdatePermissionsRequest) : BatchUpdatePermissionsResponse {
        val rAcl = permissionDao.resolveAcl(spec.acl, false)
        spec.search.access = Access.Write
        val size = searchService.count(spec.search)
        if (size > 1000) {
            throw IllegalArgumentException("Cannot set permissions on over 1000 assets at a time. " +
                    "Large permission changes should be done with a batch job.")
        }

        val combinedRsp = BatchUpdatePermissionsResponse()
        searchService.scanAndScroll(spec.search, false) { hits->
            val ids = hits.map { it.id }

            val req = BatchCreateAssetsRequest(getAll(ids).map { doc ->
                applyAcl(doc, spec.replace, rAcl)
                doc
            }, skipAssetPrep = true, scope="setPermissions")
            combinedRsp.plus(batchCreateOrReplace(req))
        }
        return combinedRsp
    }

    override fun getAll(ids: List<String>) : List<Document> {
        logger.event(LogObject.ASSET, LogAction.SEARCH, mapOf("requested_count" to ids.size, "datastore" to "CDV"))
        return ids.map {
            try {
                cdvClient.getIndexedMetadata(getCompanyId(), it)
            }
            catch (e: RestClientException) {
                // ATTENTION: The CDV does not have child assets, but we can get them from ES.
                // TODO: Check that organization ID is passed here.
                indexService.get(it)
            }
        }
    }

    override fun handleAssetUpload(name: String, bytes: ByteArray) : AssetUploadedResponse {
        val types = cdvClient.getDocumentTypes(getCompanyId())
        val id = UUID.randomUUID()
        val spec = CoreDataVaultAssetSpec(id, types[0]["documentTypeId"] as String, name)
        val result = cdvClient.createAsset(getCompanyId(), spec)
        val uri = URI(result["imageUploadURL"] as String)
        cdvClient.uploadSource(uri, bytes)
        logger.event(LogObject.ASSET, LogAction.UPLOAD, mapOf("uri" to uri))
        return AssetUploadedResponse(id, uri)
    }
}

@Transactional
class AssetServiceImpl : AbstractAssetService(), AssetService {

    @Autowired
    lateinit var assetDao: AssetDao

    @Autowired
    lateinit var fileStorageService: FileStorageService

    override fun get(assetId: String): Document {
        logger.event(LogObject.ASSET, LogAction.GET, mapOf("assetId" to assetId))
        return assetDao.get(assetId)
    }

    override fun getAll(assetIds: List<String>): List<Document> {

        val list = assetDao.getAll(assetIds)
        logger.event(
            LogObject.ASSET,
            LogAction.SEARCH,
            mapOf("requested_count" to assetIds.size, "result_count" to list.size)
        )
        return list
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
        val result = indexService.batchDelete(ids)
        if (result.deletedAssetIds.isNotEmpty()) {
            runDyhiAndTaxons()
        }
        return result
    }

    override fun batchCreateOrReplace(spec: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        /**
         * We have to do this backwards here because we're relying on ES to
         * merge existing docs and updates together.
         */
        val prepped = prepAssets(spec)
        val txResult = assetDao.batchCreateOrReplace(prepped.assets)

        if (txResult != prepped.assets.size) {
            logger.warnEvent(LogObject.ASSET, LogAction.BATCH_CREATE,
                    "Number of assets indexed did not match number in DB.",
                    mapOf())
        }

        return indexAssets(spec, prepped)
    }

    override fun createOrReplace(doc: Document): Document {
        val prepped = prepAssets(BatchCreateAssetsRequest(listOf(doc)))
        assetDao.createOrReplace(prepped.assets[0])
        indexAssets(null, prepped)
        return prepped.assets[0]
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Document {
        val asset = get(assetId)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("update access denied")
        }

        val updated = indexService.update(asset, attrs)
        assetDao.createOrReplace(updated)
        runDyhiAndTaxons()
        return asset
    }

    override fun batchUpdate(assets: List<Document>, reindex: Boolean, taxons: Boolean): BatchUpdateAssetsResponse {
        val rsp = BatchUpdateAssetsResponse()
        val updated = assetDao.batchUpdate(assets)

        val batch = assets.filterIndexed { index, doc ->
            val r = updated[index] == 1
            if (r) {
                rsp.updatedAssetIds.add(doc.id)

            }
            else {
                rsp.erroredAssetIds.add(doc.id)
            }
            r
        }

        if (reindex) {
            indexService.index(batch)
        }
        if (taxons) {
            runDyhiAndTaxons()
        }
        return rsp
    }

    override fun setPermissions(spec: BatchUpdatePermissionsRequest): BatchUpdatePermissionsResponse {
        val rAcl = permissionDao.resolveAcl(spec.acl, false)

        spec.search.access = Access.Write
        val size = searchService.count(spec.search)
        if (size > 1000) {
            throw IllegalArgumentException("Cannot set permissions on over 1000 assets at a time. " +
                    "Large permission changes should be done with a batch job.")
        }

        val combinedRep = BatchUpdatePermissionsResponse()

        searchService.scanAndScroll(spec.search, false) { hits ->
            val ids = hits.map { it.id }
            val req = BatchCreateAssetsRequest(assetDao.getMap(ids).map {

                val doc = it.value
                applyAcl(doc, spec.replace, rAcl)
                doc
            }, skipAssetPrep = true, scope = "setPermissions")
            combinedRep.plus(batchCreateOrReplace(req))
        }

        return combinedRep
    }

    override fun handleAssetUpload(name: String, bytes: ByteArray) : AssetUploadedResponse {
        val id = UUID.randomUUID()
        val fss = fileStorageService.get(FileStorageSpec("asset", id, name))
        fileStorageService.write(fss.id, bytes)
        logger.event(LogObject.ASSET, LogAction.UPLOAD, mapOf("uri" to fss.uri))
        return AssetUploadedResponse(id, fss.uri)
    }
}




