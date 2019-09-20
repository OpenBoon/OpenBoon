package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.AssetUploadedResponse
import com.zorroa.archivist.domain.AuditLogEntrySpec
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
import com.zorroa.archivist.domain.BatchIndexAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetLinks
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdatePermissionsRequest
import com.zorroa.archivist.domain.BatchUpdatePermissionsResponse
import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FieldEdit
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.FieldEditSpecInternal
import com.zorroa.archivist.domain.FieldSet
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.LinkSchema
import com.zorroa.archivist.domain.LinkType
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.domain.UpdateLinksResponse
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.archivist.repository.FieldEditDao
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.security.AccessResolver
import com.zorroa.archivist.security.CoroutineAuthentication
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getSecurityContext
import com.zorroa.common.domain.ArchivistSecurityException
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Collections
import java.util.Date
import java.util.UUID
import kotlin.coroutines.CoroutineContext

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
    fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse
    fun batchRemoveLinks(type: LinkType, value: List<UUID>, assets: List<String>): UpdateLinksResponse
    fun batchUpdateLinks(type: LinkType, value: List<UUID>, req: BatchUpdateAssetLinks): UpdateLinksResponse
    fun setPermissions(spec: BatchUpdatePermissionsRequest): BatchUpdatePermissionsResponse
    fun handleAssetUpload(name: String, bytes: ByteArray): AssetUploadedResponse
    fun getFieldSets(assetId: String): List<FieldSet>

    fun createFieldEdit(spec: FieldEditSpec): FieldEdit
    fun deleteFieldEdit(edit: FieldEdit): Boolean

    /**
     * Set all the folders for a given asset.  This is only used by ARL
     * for their import script.
     */
    fun batchSetLinks(assetId: String, folders: List<UUID>)

    /**
     * Create or replace a batch of assets and return a [BatchIndexAssetsResponse]. Assets
     * that exist already will be replaced with a new asset.
     *
     * @param batch A BatchCreateAssetsRequest with fully composed assets to create.
     */
    fun createOrReplaceAssets(batch: BatchCreateAssetsRequest): BatchIndexAssetsResponse

    /**
     * Update a batch of assets and return a [BatchUpdateAssetsResponse]
     *
     * @param batch A [BatchUpdateAssetsRequest] which contains modifications to each asset.
     */
    fun updateAssets(batch: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse

    /**
     * Update a list of [Document] instances.
     *
     * @param assets The list of [Document]s to udpate.
     * @param reindex Set true of docs should be reindexed. Defaults to true.
     * @param taxons Set to true to run taxons and dyhis.  Defaults to true.
     */
    fun updateAssets(assets: List<Document>, taxons: Boolean = true): BatchUpdateAssetsResponse
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

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    lateinit var fileStorageService: FileStorageService

    @Autowired
    lateinit var accessResolver: AccessResolver

    @Autowired
    lateinit var messagingService: MessagingService

    /**
     * Prepare a list of assets to be created.  Updated assets are not prepped.
     *
     * - Removing tmp/system namespaces
     * - Applying the organization Id
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
        val orgId = getOrgId()
        val defaultPermissions = permissionDao.getDefaultPermissionSchema()

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

            newSource.setAttr("system.organizationId", orgId.toString())
            handleTimes(existingSource, newSource)
            handleHold(existingSource, newSource)
            handlePermissions(existingSource, newSource, defaultPermissions.copy())
            handleLinks(existingSource, newSource)
            fieldSystemService.applyFieldEdits(newSource)

            newSource
        }, req.scope)

        fieldSystemService.applySuggestions(prepped.assets)
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
            links.addLinks(
                LinkType.valueOf(
                    it.left.toLowerCase().capitalize()
                ), listOf(UUID.fromString(it.right.toString()))
            )
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
    private fun handlePermissions(oldAsset: Document, newAsset: Document, defaultPermissions: PermissionSchema) {

        /**
         * If the existing asset has no permissions, we assume the defaults.
         */
        var existingPerms = oldAsset.getAttr(
            "system.permissions",
            PermissionSchema::class.java
        ) ?: defaultPermissions

        newAsset.permissions?.forEach { key, value ->
            try {
                val perm = permissionDao.get(key)
                applyAclToPermissions(perm.id, value, existingPerms)
            } catch (e: Exception) {
                throw IllegalArgumentException("Permission not found: $key")
            }
        }

        newAsset.setAttr(
            "system.permissions", existingPerms
        )
    }

    /**
     * Apply the audit logs for any asset that was created or replaced.
     */
    fun auditLogChanges(prepped: PreppedAssets, rsp: BatchIndexAssetsResponse) {
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
        clusterLockExecutor.execute(
            ClusterLockSpec.combineLock("dyhi-taxons-$orgId")
                .apply { authentication = getAuthentication() }) {
            dyHierarchyService.generateAll()
            taxonomyService.tagAll()
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
        if (rsp.assetsChanged()) {
            auditLogChanges(prepped, rsp)
            runDyhiAndTaxons()
        }
        if (rsp.createdAssetIds.isNotEmpty()) {
            messagingService.sendMessage(
                actionType = ActionType.AssetsCreated,
                organizationId = getOrgId(),
                data = mapOf("ids" to rsp.createdAssetIds)
            )
        }
        if (rsp.replacedAssetIds.isNotEmpty()) {
            messagingService.sendMessage(
                actionType = ActionType.AssetsDeleted,
                organizationId = getOrgId(),
                data = mapOf("ids" to rsp.replacedAssetIds)
            )
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
     * Overrides all existing links for the given link type with the provided list.
     */
    fun setLinksToDocument(doc: Document, type: LinkType, target: List<UUID>) {
        var links: LinkSchema? = doc.getAttr("system.links", LinkSchema::class.java)
        if (links == null) {
            links = LinkSchema()
        }
        links.setLinks(type, target)
        doc.setAttr("system.links", links)
    }

    /**
     * Add link to the given Document.
     */
    fun addLinksToDocument(doc: Document, type: LinkType, target: List<UUID>): Boolean {
        var links: LinkSchema? = doc.getAttr("system.links", LinkSchema::class.java)
        if (links == null) {
            links = LinkSchema()
        }
        return if (links.addLinks(type, target)) {
            doc.setAttr("system.links", links)
            true
        } else {
            false
        }
    }

    /**
     * Remove link to the given Document.
     */
    fun removeLinksFromDocument(doc: Document, type: LinkType, target: List<UUID>): Boolean {
        var links: LinkSchema? = doc.getAttr("system.links", LinkSchema::class.java)
        if (links == null) {
            links = LinkSchema()
        }
        return if (links.removeLinks(type, target)) {
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
    override fun updateAssets(batch: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {

        if (batch.batch.size > 1000) {
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
                logger.warnEvent(
                    LogObject.ASSET,
                    LogAction.BATCH_UPDATE,
                    "Skipping setting $attr, cannot set system values on batch update",
                    mapOf("assetId" to assetId)
                )
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
        val futures = batch.batch.keys.chunked(UPDATE_BATCH_SIZE).map { ids ->

            GlobalScope.async(getCoroutineContext()) {
                val rsp = BatchUpdateAssetsResponse()
                val docs: List<Document> = getAll(ids).mapNotNull { doc ->

                    if (!accessResolver.hasAccess(Access.Write, doc)) {
                        logger.warnEvent(
                            LogObject.ASSET,
                            LogAction.BATCH_UPDATE,
                            "Skipping updating asset, access denied",
                            mapOf("assetId" to doc.id)
                        )
                        rsp.accessDeniedAssetIds.add(doc.id)
                        null
                    } else {

                        val req = batch.batch.getValue(doc.id)
                        var changed = false
                        req.update?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.setAttr(t, u)
                                changed = true
                            }
                        }

                        req.remove?.forEach {
                            if (checkAttr(it, doc.id, req.allowSystem)) {
                                doc.removeAttr(it)
                                changed = true
                            }
                        }

                        req.appendToList?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.addToAttr(t, u, unique = false)
                                changed = true
                            }
                        }

                        req.appendToUniqueList?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.addToAttr(t, u, unique = true)
                                changed = true
                            }
                        }

                        req.removeFromList?.forEach { t, u ->
                            if (checkAttr(t, doc.id, req.allowSystem)) {
                                doc.removeFromAttr(t, u)
                                changed = true
                            }
                        }

                        if (changed) {
                            doc.setAttr("system.timeModified", now)
                            doc
                        } else {
                            null
                        }
                    }
                }

                fieldSystemService.applySuggestions(docs)
                rsp.plus(updateAssets(docs, taxons = false))
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
                logger.event(
                    LogObject.ASSET, LogAction.BATCH_UPDATE,
                    mapOf(
                        "updateCount" to r.updatedAssetIds.size,
                        "errorCount" to r.erroredAssetIds.size
                    )
                )
                rsp.plus(r)
            }
        }

        runDyhiAndTaxons()
        if (rsp.updatedAssetIds.isNotEmpty()) {
            messagingService.sendMessage(
                actionType = ActionType.AssetsUpdated,
                organizationId = getOrgId(),
                data = mapOf("ids" to rsp.updatedAssetIds)
            )
        }
        return rsp
    }

    override fun batchRemoveLinks(
        type: LinkType,
        value: List<UUID>,
        assets: List<String>
    ): UpdateLinksResponse = runBlocking {

        val errorAssetIds = Collections.synchronizedSet(mutableSetOf<String>())
        val successAssetIds = Collections.synchronizedSet(mutableSetOf<String>())

        async(getCoroutineContext()) {
            assets.chunked(UPDATE_BATCH_SIZE).forEach {
                launch {
                    val docs = getAll(it).mapNotNull { doc ->
                        if (removeLinksFromDocument(doc, type, value)) {
                            doc
                        } else {
                            // already removed
                            null
                        }
                    }
                    val update = updateAssets(docs, taxons = false)
                    if (update.erroredAssetIds.isNotEmpty()) {
                        errorAssetIds.addAll(update.erroredAssetIds)
                    }
                    successAssetIds.addAll(update.updatedAssetIds)
                }
            }
        }
        UpdateLinksResponse(successAssetIds, errorAssetIds)
    }

    override fun batchUpdateLinks(
        type: LinkType,
        value: List<UUID>,
        req: BatchUpdateAssetLinks
    ): UpdateLinksResponse = runBlocking {

        val errors = Collections.synchronizedSet(mutableSetOf<String>())
        val success = Collections.synchronizedSet(mutableSetOf<String>())

        async(getCoroutineContext()) {

            req.assetIds?.chunked(UPDATE_BATCH_SIZE)?.forEach {
                launch {
                    val docs = getAll(it).mapNotNull { doc ->
                        when {
                            req.replace -> {
                                setLinksToDocument(doc, type, value)
                                doc
                            }
                            addLinksToDocument(doc, type, value) -> doc
                            else -> null
                        }
                    }
                    println(Json.prettyString(docs))
                    val update = updateAssets(docs, taxons = false)
                    if (update.erroredAssetIds.isNotEmpty()) {
                        errors.addAll(update.erroredAssetIds)
                    }
                    success.addAll(update.updatedAssetIds)
                }
            }

            if (!req.parentIds.isNullOrEmpty() && req.search != null) {

                val search = req.search
                search.addToFilter().must = mutableListOf(
                    AssetFilter()
                        .addToTerms("media.clip.parent", req.parentIds)
                )

                searchService.scanAndScroll(search, true) { hits ->
                    launch {
                        val docs = hits.hits.map { hit ->
                            val doc = Document(hit.id, hit.sourceAsMap)
                            doc
                        }
                        val update = updateAssets(docs, taxons = false)
                        if (update.erroredAssetIds.isNotEmpty()) {
                            errors.addAll(update.erroredAssetIds)
                        }
                        success.addAll(update.updatedAssetIds)
                    }
                }
            }
        }.join()
        UpdateLinksResponse(success, errors)
    }

    override fun deleteFieldEdit(edit: FieldEdit): Boolean {
        val asset = get(edit.assetId.toString())
        val field = fieldSystemService.getField(edit.fieldId)

        if (!accessResolver.hasAccess(Access.Write, asset)) {
            throw ArchivistSecurityException("update access denied")
        }

        val updateReq = if (edit.oldValue == null) {
            UpdateAssetRequest(
                remove = listOf(field.attrName),
                removeFromList = mapOf("system.fieldEdits" to field.attrName),
                allowSystem = true
            )
        } else {
            UpdateAssetRequest(
                mapOf(field.attrName to edit.oldValue),
                removeFromList = mapOf("system.fieldEdits" to field.attrName),
                allowSystem = true
            )
        }

        if (fieldEditDao.delete(edit.id)) {
            val req = BatchUpdateAssetsRequest(mapOf(asset.id to updateReq))
            val rsp = updateAssets(req)
            if (rsp.isSuccess()) {
                val aspec = AuditLogEntrySpec(
                    asset.id,
                    AuditLogType.Changed,
                    fieldId = field.id,
                    attrName = field.attrName,
                    scope = "undo edit",
                    newValue = edit.oldValue
                )
                auditLogDao.create(aspec)
                return true
            } else {
                throw rsp.getThrowableError()
            }
        }

        throw ArchivistWriteException("Unable to find field edit: ${edit.id}")
    }

    override fun createFieldEdit(spec: FieldEditSpec): FieldEdit {
        val assetId = spec.assetId.toString()
        val asset = get(assetId)
        val field = fieldSystemService.getField(spec)

        if (!field.editable) {
            throw IllegalStateException("The field ${field.name} is not editable")
        }

        if (!field.attrType.isValid(spec.newValue)) {
            throw java.lang.IllegalArgumentException(
                "The value ${spec.newValue} " +
                    "for field ${field.name} is not the correct type"
            )
        }

        if (field.requireList) {
            if (spec.newValue != null) {
                if (spec.newValue !is Collection<*>) {
                    throw java.lang.IllegalArgumentException(
                        "The value ${spec.newValue} " +
                            "for field ${field.name} must be a list."
                    )
                }
            }
        }

        val updateReq = if (spec.newValue == null) {
            UpdateAssetRequest(
                remove = listOf(field.attrName),
                appendToUniqueList = mapOf("system.fieldEdits" to field.attrName),
                allowSystem = true
            )
        } else {
            UpdateAssetRequest(
                mapOf(field.attrName to spec.newValue),
                appendToUniqueList = mapOf("system.fieldEdits" to field.attrName),
                allowSystem = true
            )
        }

        val req = BatchUpdateAssetsRequest(mapOf(asset.id to updateReq))
        val rsp = updateAssets(req)
        if (rsp.isSuccess()) {
            val oldValue = asset.getAttr(field.attrName, Any::class.java)
            val ispec = FieldEditSpecInternal(
                UUID.fromString(asset.id),
                field.id,
                spec.newValue,
                oldValue
            )
            val fieldEdit = fieldEditDao.create(ispec)

            val aspec = AuditLogEntrySpec(
                assetId,
                AuditLogType.Changed,
                fieldId = field.id,
                attrName = field.attrName,
                scope = "manual edit",
                oldValue = oldValue,
                newValue = spec.newValue
            )
            auditLogDao.create(aspec)
            return fieldEdit
        } else {
            throw rsp.getThrowableError()
        }
    }

    /**
     * Return a co-routine context specific to the particular runtime environment.
     * For unittests, co-routines run in the main thread because other threads will
     * block on the Transaction surrounding the unittest.
     *
     * For production mode, all IO is run in the Dispatchers.IO pool.
     *
     */
    private fun getCoroutineContext(): CoroutineContext {
        return if (ArchivistConfiguration.unittest) {
            CoroutineAuthentication(getSecurityContext()) + Dispatchers.Main.immediate
        } else {
            Dispatchers.IO + CoroutineAuthentication(getSecurityContext())
        }
    }

    override fun get(assetId: String): Document {
        return indexService.get(assetId)
    }

    override fun getAll(assetIds: List<String>): List<Document> {
        return indexService.getAll(assetIds)
    }

    override fun delete(assetId: String): Boolean {
        val asset = indexService.get(assetId)
        if (!accessResolver.hasAccess(Access.Write, asset)) {
            throw ArchivistSecurityException("delete access denied")
        }
        val result = indexService.delete(assetId)
        if (result) {
            runDyhiAndTaxons()
            messagingService.sendMessage(
                actionType = ActionType.AssetsDeleted,
                organizationId = getOrgId(),
                data = mapOf("ids" to listOf(assetId))
            )
        }

        return result
    }

    override fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse {
        val result = indexService.batchDelete(assetIds)
        if (result.deletedAssetIds.isNotEmpty()) {
            runDyhiAndTaxons()
            messagingService.sendMessage(
                actionType = ActionType.AssetsDeleted,
                organizationId = getOrgId(),
                data = mapOf("ids" to result.deletedAssetIds)
            )
        }
        return result
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

    override fun updateAssets(assets: List<Document>, taxons: Boolean): BatchUpdateAssetsResponse {

        val result = BatchUpdateAssetsResponse()
        result.plus(indexService.index(assets))
        if (taxons) {
            runDyhiAndTaxons()
        }
        if (result.updatedAssetIds.isNotEmpty()) {
            messagingService.sendMessage(
                actionType = ActionType.AssetsUpdated,
                organizationId = getOrgId(),
                data = mapOf("ids" to result.updatedAssetIds)
            )
        }
        return result
    }

    override fun setPermissions(spec: BatchUpdatePermissionsRequest): BatchUpdatePermissionsResponse {
        val rAcl = permissionDao.resolveAcl(spec.acl, false)

        spec.search.access = Access.Write
        val size = searchService.count(spec.search)
        if (size > 1000) {
            throw IllegalArgumentException(
                "Cannot set permissions on over 1000 assets at a time. " +
                    "Large permission changes should be done with a batch job."
            )
        }

        val combinedRep = BatchUpdatePermissionsResponse()

        searchService.scanAndScroll(spec.search, true) { hits ->
            val docs = hits.map {
                val doc = Document(it.id, it.sourceAsMap)
                applyAcl(doc, spec.replace, rAcl)
                doc
            }

            val req = BatchCreateAssetsRequest(docs, skipAssetPrep = true, scope = "setPermissions")
            combinedRep.plus(createOrReplaceAssets(req))
        }

        return combinedRep
    }

    override fun handleAssetUpload(name: String, bytes: ByteArray): AssetUploadedResponse {
        val id = UUID.randomUUID()
        val fss = fileStorageService.get(FileStorageSpec("asset", id, name))
        fileStorageService.write(fss.id, bytes)
        return AssetUploadedResponse(id, fss.uri)
    }

    override fun batchSetLinks(assetId: String, folders: List<UUID>) {
        batchUpdateLinks(LinkType.Folder, folders, BatchUpdateAssetLinks(listOf(assetId), replace = true))
    }

    companion object {

        /**
         * Namespaces that are protected or unable to be set via the API.
         */
        val PROTECTED_NAMESPACES = setOf("system", "tmp")

        /**
         * The number of assets each IO thread will handle during a batch update.
         */
        const val UPDATE_BATCH_SIZE = 10

        val logger: Logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    }
}
