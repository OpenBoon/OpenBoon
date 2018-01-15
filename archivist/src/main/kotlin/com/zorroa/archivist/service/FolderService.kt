package com.zorroa.archivist.service

import com.google.common.base.Preconditions
import com.google.common.base.Splitter
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Lists
import com.google.common.collect.Queues
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.*
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.sdk.client.exception.ArchivistException
import com.zorroa.sdk.client.exception.ArchivistWriteException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

interface FolderService {

    fun getAll(): List<Folder>

    /**
     * Return all trashed folders for the current user.
     * @return
     */
    fun getTrashedFolders(): List<TrashedFolder>

    fun updateAcl(folder: Folder, acl: Acl)

    fun get(id: Int): Folder

    fun get(parent: Int, name: String): Folder

    fun exists(path: String): Boolean

    fun count(): Int

    fun count(dyhi: DyHierarchy): Int

    /**
     * Return all folders with the given unique IDs.
     *
     * @param ids
     * @return
     */
    fun getAll(ids: Collection<Int>): List<Folder>

    fun getAllIds(dyhi: DyHierarchy): List<Int>

    fun getChildren(folder: Folder): List<Folder>

    /**
     * Return a List of all descendant folders starting from the given start folders.  If
     * includeStartFolders is set to true, the starting folders are included in the result.
     *
     * @param startFolders
     * @param includeStartFolders
     * @return
     */
    fun getAllDescendants(startFolders: Collection<Folder>, includeStartFolders: Boolean, forSearch: Boolean): List<Folder>

    fun invalidate(folder: Folder?, vararg additional: Int)

    fun isInTaxonomy(folder: Folder): Boolean

    fun getParentTaxonomy(folder: Folder): Taxonomy?

    fun getAllAncestors(folder: Folder, includeStart: Boolean, taxOnly: Boolean): List<Folder>

    /**
     * Return a recursive list of all descendant folders from the current folder.
     *
     * @param folder
     * @param forSearch
     * @return
     */
    fun getAllDescendants(folder: Folder, forSearch: Boolean): List<Folder>

    fun update(folderId: Int, updated: Folder): Boolean

    fun deleteAll(ids: Collection<Int>): Int

    fun trash(folder: Folder): TrashedFolderOp

    fun restore(tf: TrashedFolder): TrashedFolderOp

    fun delete(folder: Folder): Boolean

    fun deleteAll(dyhi: DyHierarchy): Int

    fun get(path: String): Folder?

    fun removeDyHierarchyRoot(folder: Folder): Boolean

    fun setDyHierarchyRoot(folder: Folder, attribute: String): Boolean

    fun getAcl(folder: Folder): Acl

    fun setAcl(folder: Folder, acl: Acl, created: Boolean, autoCreate: Boolean)

    fun addAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>>

    fun removeAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>>

    /**
     * Asynchronously creata a new folder.  Return a future in case
     * you eventually need the result.
     *
     * @param spec
     * @param mightExist
     * @return
     */
    fun submitCreate(parent: Folder, spec: FolderSpec, mightExist: Boolean): Future<Folder>

    fun submitCreate(spec: FolderSpec, mightExist: Boolean): Future<Folder>

    fun create(spec: FolderSpec): Folder

    fun create(spec: FolderSpec, mightExist: Boolean): Folder

    fun create(parent: Folder, spec: FolderSpec, mightExist: Boolean): Folder

    fun createUserFolder(username: String, perm: Permission): Folder

    /**
     * Return deleted child folders in the given folder for the current user.
     *
     * @param folder
     * @return
     */
    fun getTrashedFolders(folder: Folder): List<TrashedFolder>

    /**
     * Get a trashed folder by its unique Id.
     *
     * @param id
     * @return
     */
    fun getTrashedFolder(id: Int): TrashedFolder

    fun emptyTrash(): List<Int>

    fun emptyTrash(ids: List<Int>): List<Int>

    fun trashCount(): Int

    fun isDescendantOf(target: Folder, moving: Folder): Boolean
}

@Service
@Transactional
class FolderServiceImpl @Autowired constructor(
        val folderDao: FolderDao,
        val trashFolderDao: TrashFolderDao,
        val assetDao: AssetDao,
        val userDao: UserDao,
        val permissionDao: PermissionDao,
        val transactionEventManager: TransactionEventManager
) : FolderService {

    /**
     * Circular dependencies must be lateinit
     */
    @Autowired
    private lateinit var logService: EventLogService

    @Autowired
    private lateinit var dyHierarchyService: DyHierarchyService

    @Autowired
    private lateinit var taxonomyService: TaxonomyService

    private val childCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(object : CacheLoader<Int, List<Folder>>() {
                @Throws(Exception::class)
                override fun load(key: Int): List<Folder> {
                    return folderDao.getChildrenInsecure(key)
                }
            })

    private val folderCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(object : CacheLoader<Int, Folder>() {
                @Throws(Exception::class)
                override fun load(key: Int): Folder {
                    return folderDao.get(key)
                }
            })

    private val folderExecutor = Executors.newSingleThreadExecutor()

    override fun removeDyHierarchyRoot(folder: Folder): Boolean {
        val result = folderDao.removeDyHierarchyRoot(folder)
        transactionEventManager.afterCommit { invalidate(folder) }
        return result
    }

    override fun setDyHierarchyRoot(folder: Folder, attribute: String): Boolean {
        if (folder.id == 0) {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }
        val result = folderDao.setDyHierarchyRoot(folder, attribute)
        transactionEventManager.afterCommit { invalidate(folder) }
        return result
    }

    override fun getAcl(folder: Folder): Acl {
        return folderDao.getAcl(folder.id)
    }

    override fun setAcl(folder: Folder, acl: Acl, created: Boolean, autoCreate: Boolean) {
        SecurityUtils.canSetAclOnFolder(acl, folder.acl, created)

        val resolvedAcl = permissionDao.resolveAcl(acl, autoCreate)
        folderDao.setAcl(folder.id, resolvedAcl, true)
        folder.acl = acl

        transactionEventManager.afterCommit {
            invalidate(folder)
            logService.logAsync(UserLogSpec.build(LogAction.Update, folder)
                    .setMessage("permissions set"))
        }
    }

    override fun updateAcl(folder: Folder, acl: Acl) {
        SecurityUtils.canSetAclOnFolder(acl, folder.acl, false)

        val resolvedAcl = permissionDao.resolveAcl(acl, false)
        folderDao.updateAcl(folder.id, resolvedAcl)

        transactionEventManager.afterCommit {
            invalidate(folder)
            logService.logAsync(UserLogSpec.build(LogAction.Update, folder)
                    .setMessage("permissions updated"))
        }
    }

    override fun get(id: Int): Folder {
        val f: Folder
        try {
            f = folderCache.get(id)
            if (!SecurityUtils.hasPermission(f.acl, Access.Read)) {
                throw EmptyResultDataAccessException("Failed to find folder: " + id, 1)
            }
            return f

        } catch (e: Exception) {
            throw EmptyResultDataAccessException("Failed to find folder: " + id, 1)
        }

    }

    override fun get(parent: Int, name: String): Folder {
        return folderDao.get(parent, name, false)
    }

    override fun get(path: String): Folder? {
        var parentId = Folder.ROOT_ID
        var current: Folder? = null

        if ("/" == path) {
            return folderDao.get(0)
        }

        // Just throw the exception to the caller,don't return null
        // as none of the other 'get' functions do.
        for (name in Splitter.on("/").omitEmptyStrings().trimResults().split(path)) {
            current = folderDao.get(parentId, name, false)
            parentId = current.id
        }
        return current
    }

    override fun exists(path: String): Boolean {
        return try {
            get(path)
            true
        } catch (e: Exception) {
            false
        }

    }

    override fun count(): Int {
        return folderDao.count()
    }

    override fun count(dyhi: DyHierarchy): Int {
        return folderDao.count(dyhi)
    }

    override fun getAll(): List<Folder> {
        return folderDao.getChildren(Folder.ROOT_ID)
    }

    override fun getAll(ids: Collection<Int>): List<Folder> {
        return folderDao.getAll(ids)
    }

    override fun getAllIds(dyhi: DyHierarchy): List<Int> {
        return folderDao.getAllIds(dyhi)
    }

    override fun getChildren(folder: Folder): List<Folder> {
        return folderDao.getChildren(folder)
    }

    override fun update(folderId : Int, updated: Folder): Boolean {
        if (!SecurityUtils.hasPermission(folderDao.getAcl(folderId), Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        if (folderId == 0 || updated.id == 0) {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }

        val current = folderDao.get(folderId)
        val parentSwap = current.parentId !== updated.parentId

        if (parentSwap) {

            if (isDescendantOf(folderDao.get(updated.parentId), current)) {
                throw ArchivistWriteException("You cannot move a folder into one of its descendants.")
            }

            if (updated.dyhiId != null) {
                throw ArchivistWriteException("You cannot move a DyHi folder")
            }
        }

        val result = folderDao.update(folderId, updated)

        /**
         * If there is a parent swap then the folder gets perms from the new parent.
         * This has to be recursive.  Some people might lose access.
         */
        if (result && parentSwap) {
            val targetFolder = folderDao.get(updated.parentId)
            setAcl(updated, targetFolder.acl, true, false)

            for (child in getAllDescendants(updated, false)) {
                setAcl(child, targetFolder.acl, true, false)
            }
        }

        if (result) {
            transactionEventManager.afterCommit(true, {
                invalidate(current, current.parentId)
                logService.logAsync(UserLogSpec.build(LogAction.Update, updated))

                val folder = get(updated.id)
                val tax = getParentTaxonomy(folder)
                if (tax != null) {
                    /**
                     * In this case we force tag/untag the taxonomy.
                     */
                    taxonomyService.tagTaxonomyAsync(tax, folder, true)
                }
            })
        }
        return result
    }

    override fun deleteAll(dyhi: DyHierarchy): Int {
        return folderDao.deleteAll(dyhi)
    }

    override fun deleteAll(ids: Collection<Int>): Int {
        return folderDao.deleteAll(ids)
    }

    override fun trash(folder: Folder): TrashedFolderOp {

        if (!SecurityUtils.hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You don't have the permissions to delete this folder")
        }

        if (folder.id == 0) {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }
        /**
         * Don't allow trashing of dyhi folders
         */
        if (folder.dyhiId != null) {
            throw ArchivistWriteException("Cannot deleted dynamic hierarchy folder.")
        }

        if (folder.isDyhiRoot) {
            removeDyHierarchyRoot(folder)
        }

        /**
         * The Operation ID keeps track of all folders deleted by this specific
         * transaction.
         */
        val op = UUID.randomUUID().toString()

        val children = getAllDescendants(folder, false)
        var order = 0

        var i = children.size
        while (--i >= 0) {
            val child = children[i]

            if (!SecurityUtils.hasPermission(child.acl, Access.Write)) {
                throw ArchivistWriteException(
                        "You don't have the permissions to delete the subfolder " + child.name)
            }

            if (folderDao.delete(child)) {
                order++
                trashFolderDao.create(child, op, false, order)
                transactionEventManager.afterCommit(false, {
                    invalidate(child)
                    logService.logAsync(UserLogSpec.build(LogAction.Delete, child))
                })
            }
        }

        val tax = getParentTaxonomy(folder)
        if (folder.isTaxonomyRoot) {
            taxonomyService.delete(tax, false)
        }

        val result = folderDao.delete(folder)

        if (result) {
            transactionEventManager.afterCommit(true, {
                invalidate(folder)
                logService.logAsync(UserLogSpec.build(LogAction.Delete, folder))
            })

            if (tax != null) {
                transactionEventManager.afterCommit(false, {
                    if (folder.isTaxonomyRoot) {
                        taxonomyService.untagTaxonomyAsync(tax, 0)
                    } else {
                        taxonomyService.untagTaxonomyFoldersAsync(tax, children)
                        taxonomyService.untagTaxonomyFoldersAsync(tax, Lists.newArrayList(folder))
                    }
                })
            }

            order++
            val id = trashFolderDao.create(folder, op, true, order)
            return TrashedFolderOp(id, op, order)
        } else {
            throw ArchivistWriteException("Failed to trash the given folder, already deleted.")
        }
    }

    override fun restore(tf: TrashedFolder): TrashedFolderOp {
        val folders = trashFolderDao.getAll(tf.opId)

        var count = 0
        for (folder in folders) {
            folderDao.create(folder)
            count++
        }

        trashFolderDao.removeAll(tf.opId)
        return TrashedFolderOp(tf.id, tf.opId, count)
    }

    @Deprecated("")
    override fun delete(folder: Folder): Boolean {

        if (!SecurityUtils.hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        if (folder.id == 0) {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }

        if (folder.isDyhiRoot) {
            dyHierarchyService.delete(dyHierarchyService.get(folder))
        }

        /**
         * Delete all children in reverse order.
         */
        val children = getAllDescendants(folder, false)
        var i = children.size
        while (--i >= 0) {
            if (folderDao.delete(children[i])) {
                transactionEventManager.afterCommit(true, {
                    invalidate(folder)
                    logService.logAsync(UserLogSpec.build(LogAction.Delete, folder))
                })
            }
        }

        val result = folderDao.delete(folder)
        if (result) {
            transactionEventManager.afterCommit(true, {
                invalidate(folder)
                logService.logAsync(UserLogSpec.build(LogAction.Delete, folder))
            })
        }
        return result
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun addAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>> {

        if (!SecurityUtils.hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        if (folder.search != null) {
            throw ArchivistWriteException("Cannot add assets to a smart folder.  Remove the search first.")
        }

        val result = assetDao.appendLink("folder", folder.id, assetIds)
        invalidate(folder)
        logService.logAsync(UserLogSpec.build("add_assets", folder).putToAttrs("count", assetIds.size))

        val tax = getParentTaxonomy(folder)
        if (tax != null) {
            taxonomyService.tagTaxonomyAsync(tax, folder, false)
        }

        return result
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun removeAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>> {

        if (!SecurityUtils.hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        val result = assetDao.removeLink("folder", folder.id, assetIds)
        logService.logAsync(UserLogSpec.build("remove_assets", folder).putToAttrs("assetIds", assetIds))
        invalidate(folder)

        val tax = getParentTaxonomy(folder)
        if (tax != null) {
            taxonomyService.untagTaxonomyFoldersAsync(tax, folder, assetIds)
        }

        return result
    }

    override fun invalidate(folder: Folder?, vararg additional: Int) {
        if (folder != null) {
            if (folder.parentId != null) {
                childCache.invalidate(folder.parentId)
                folderCache.invalidate(folder.parentId)
            }
            childCache.invalidate(folder.id)
            folderCache.invalidate(folder.id)
        }

        for (id in additional) {
            childCache.invalidate(id)
            folderCache.invalidate(id)
        }
    }

    override fun isInTaxonomy(folder: Folder): Boolean {
        var current = folder

        if (folder.isTaxonomyRoot) {
            return true
        }
        while (current.parentId != 0) {
            current = get(folder.parentId)
            if (current.isTaxonomyRoot) {
                return true
            }
        }
        return false
    }

    override fun getParentTaxonomy(folder: Folder): Taxonomy? {
        var current = folder
        if (current.isTaxonomyRoot) {
            return taxonomyService.get(folder)
        }
        while (current.parentId != 0) {
            current = get(current.parentId)
            if (current.isTaxonomyRoot) {
                return taxonomyService.get(current)
            }
        }
        return null
    }

    override fun getAllAncestors(folder: Folder, includeStart: Boolean, taxOnly: Boolean): List<Folder> {
        val result = Lists.newArrayList<Folder>()
        if (includeStart) {
            result.add(folder)
        }
        var start = folder
        while (start.parentId != 0) {
            start = get(start.parentId)
            result.add(start)
            if (start.isTaxonomyRoot && taxOnly) {
                break
            }
        }
        return result
    }

    override fun getAllDescendants(folder: Folder, forSearch: Boolean): List<Folder> {
        return getAllDescendants(Lists.newArrayList(folder), false, forSearch)
    }

    override fun getAllDescendants(startFolders: Collection<Folder>, includeStartFolders: Boolean, forSearch: Boolean): List<Folder> {
        val result = Lists.newArrayListWithCapacity<Folder>(32)
        val queue = Queues.newLinkedBlockingQueue<Folder>()

        if (includeStartFolders) {
            result.addAll(startFolders)
        }

        queue.addAll(startFolders)
        getChildFoldersRecursive(result, queue, forSearch)
        return result
    }

    /**
     * A non-recursion based search for finding all child folders
     * of a folder.
     *
     * @param result
     * @param toQuery
     */
    private fun getChildFoldersRecursive(result: MutableList<Folder>, toQuery: Queue<Folder>, forSearch: Boolean) {

        while (true) {
            val current = toQuery.poll() ?: return
            if (Folder.isRoot(current)) {
                continue
            }

            /*
             * This is a potential optimization to try out that limits the need to traverse into all
             * child folders from a root.  For example, if /exports is set with a query that searches
             * for all assets that have an export ID, then there is no need to traverse all the sub
             * folders.
             */
            if (!current.isRecursive && forSearch) {
                continue
            }
            try {
                val children = childCache.get(current.id)
                        .stream()
                        .filter { f -> SecurityUtils.hasPermission(f.acl, Access.Read) }
                        .collect(Collectors.toList())

                if (children == null || children.isEmpty()) {
                    continue
                }

                toQuery.addAll(children)
                result.addAll(children)

            } catch (e: Exception) {
                logger.warn("Failed to obtain child folders for {}", current, e)
            }

        }
    }

    override fun submitCreate(spec: FolderSpec, mightExist: Boolean): Future<Folder> {
        return folderExecutor.submit<Folder> { create(spec, mightExist) }
    }

    override fun submitCreate(parent: Folder, spec: FolderSpec, mightExist: Boolean): Future<Folder> {
        return folderExecutor.submit<Folder> { create(parent, spec, mightExist) }
    }

    override fun create(parent: Folder, spec: FolderSpec, mightExist: Boolean): Folder {
        if (!SecurityUtils.hasPermission(parent.acl, Access.Write)) {
            throw ArchivistException("You cannot make changes to this folder")
        }

        // If there is no acl, use the parent acl.
        if (spec.acl == null) {
            spec.acl = parent.acl
        }

        var result: Folder
        if (mightExist) {
            try {
                result = get(spec.parentId, spec.name)
            } catch (e: EmptyResultDataAccessException) {
                result = folderDao.create(spec)
                spec.created = true
                setAcl(result, spec.acl, true, false)
                emitFolderCreated(result)
            }

        } else {
            //TODO: this won't work with postgres since the transaction
            // will be dead once the DuplicateKeyException is thrown.
            // Look into using ON CONFLICT / Merge
            try {
                result = folderDao.create(spec)
                spec.created = true
                setAcl(result, spec.acl, true, false)
                emitFolderCreated(result)
            } catch (e: DuplicateKeyException) {
                result = get(spec.parentId, spec.name)
            }

        }
        return result
    }

    private fun emitFolderCreated(folder: Folder) {
        transactionEventManager.afterCommit(true, {
            invalidate(null, folder.parentId)
            logService.logAsync(UserLogSpec.build(LogAction.Create, folder))

            if (folder.dyhiId == null && folder.search != null) {
                val tax = getParentTaxonomy(folder)
                if (tax != null) {
                    taxonomyService.tagTaxonomy(tax, folder, true)
                }
            }
        })
    }

    override fun create(spec: FolderSpec, mightExist: Boolean): Folder {
        Preconditions.checkNotNull(spec.parentId, "Parent cannot be null")
        return create(folderDao.get(spec.parentId), spec, mightExist)
    }

    override fun create(spec: FolderSpec): Folder {
        return create(folderDao.get(spec.parentId), spec, false)
    }

    override fun createUserFolder(username: String, perm: Permission): Folder {
        val adminUser = userDao.get("admin")

        val rootFolder = folderDao.get(Folder.ROOT_ID, "Users", true)
        val folder = folderDao.create(FolderSpec()
                .setName(username)
                .setParentId(rootFolder.id)
                .setUserId(adminUser.id))
        folderDao.setAcl(folder.id, Acl().addEntry(perm, Access.Read, Access.Write), true)
        return folder
    }

    override fun getTrashedFolders(): List<TrashedFolder> {
        return trashFolderDao.getAll(SecurityUtils.getUser().id)
    }

    override fun getTrashedFolders(folder: Folder): List<TrashedFolder> {
        return trashFolderDao.getAll(folder, SecurityUtils.getUser().id)
    }

    override fun getTrashedFolder(id: Int): TrashedFolder {
        return trashFolderDao[id, SecurityUtils.getUser().id]
    }

    override fun emptyTrash(): List<Int> {
        return trashFolderDao.removeAll(SecurityUtils.getUser().id)
    }

    override fun emptyTrash(ids: List<Int>): List<Int> {
        return trashFolderDao.removeAll(ids, SecurityUtils.getUser().id)
    }

    override fun trashCount(): Int {
        return trashFolderDao.count(SecurityUtils.getUser().id)
    }

    override fun isDescendantOf(target: Folder, moving: Folder): Boolean {
        var target = target
        if (target.id == moving.id) {
            return true
        }
        while (target.parentId != null) {
            target = folderDao.get(target.parentId)
            if (target.id == moving.id) {
                return true
            }
        }
        return false
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FolderServiceImpl::class.java)
    }

}

