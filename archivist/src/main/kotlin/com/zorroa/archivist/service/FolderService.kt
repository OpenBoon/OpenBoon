package com.zorroa.archivist.service

import com.google.common.base.Splitter
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Lists
import com.google.common.collect.Queues
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.*
import com.zorroa.archivist.security.canSetAclOnFolder
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.util.whenNullOrEmpty
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.search.AssetSearch
import com.zorroa.security.Groups
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

    fun get(folder: Folder): Folder

    fun get(id: UUID?): Folder

    fun get(parent: UUID?, name: String): Folder

    fun getRoot(): Folder

    fun exists(path: String): Boolean

    fun count(): Int

    fun count(dyhi: DyHierarchy): Int

    /**
     * Return all folders with the given unique IDs.
     *
     * @param ids
     * @return
     */
    fun getAll(ids: Collection<UUID>): List<Folder>

    fun getAllIds(dyhi: DyHierarchy): List<UUID>

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

    fun invalidate(folder: Folder?, vararg additional: UUID?)

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

    fun update(folderId: UUID, updated: FolderUpdate): Boolean

    fun deleteAll(ids: Collection<UUID>): Int

    fun trash(folder: Folder): TrashedFolderOp

    fun restore(tf: TrashedFolder): TrashedFolderOp

    fun delete(folder: Folder): Boolean

    fun deleteAll(dyhi: DyHierarchy): Int

    fun get(path: String): Folder?

    fun getPath(folder: Folder): String

    fun removeDyHierarchyRoot(folder: Folder): Boolean

    fun setDyHierarchyRoot(folder: Folder, attribute: String): Boolean

    fun getAcl(folder: Folder): Acl

    fun setAcl(folder: Folder, acl: Acl?, created: Boolean, autoCreate: Boolean)

    fun updateAcl(folder: Folder, acl: Acl?)

    fun addAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>>

    fun removeAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>>

    fun setFoldersForAsset(assetId: String, folders: List<UUID>);

    fun submitCreate(parent: Folder, spec: FolderSpec): Future<Folder>

    fun submitCreate(spec: FolderSpec): Future<Folder>

    fun create(spec: FolderSpec): Folder

    fun create(spec: FolderSpec, errorIfExists: Boolean) : Folder

    fun create(parent: Folder, spec: FolderSpec, errorIfExists: Boolean = true): Folder

    fun createStandardFolders(org: Organization): Folder

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
    fun getTrashedFolder(id: UUID): TrashedFolder

    fun emptyTrash(): List<UUID>

    fun emptyTrash(ids: List<UUID>): List<UUID>

    fun trashCount(): Int

    fun isDescendantOf(target: Folder, moving: Folder): Boolean

    fun renameUserFolder(user:User, newName: String): Boolean
}

@Service
@Transactional
class FolderServiceImpl @Autowired constructor(
        val folderDao: FolderDao,
        val trashFolderDao: TrashFolderDao,
        val indexDao: IndexDao,
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
            .build(object : CacheLoader<UUID, List<Folder>>() {
                @Throws(Exception::class)
                override fun load(key: UUID): List<Folder> {
                    return folderDao.getChildrenInsecure(key)
                }
            })

    private val folderCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(object : CacheLoader<UUID, Folder>() {
                @Throws(Exception::class)
                override fun load(key: UUID): Folder {
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
        if (folder.name == "/") {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }
        val result = folderDao.setDyHierarchyRoot(folder, attribute)
        transactionEventManager.afterCommit { invalidate(folder) }
        return result
    }

    override fun getAcl(folder: Folder): Acl {
        return folderDao.getAcl(folder.id)
    }

    override fun setAcl(folder: Folder, acl: Acl?, created: Boolean, autoCreate: Boolean) {
        if (acl == null) {
            logger.warn("Ignoring null ACL on folder: {}", folder)
            return
        }

        canSetAclOnFolder(acl, folder.acl, created)

        val resolvedAcl = permissionDao.resolveAcl(acl, autoCreate)
        folderDao.setAcl(folder.id, resolvedAcl, true)

        transactionEventManager.afterCommit(true) {
            invalidate(folder)
            //logService.logAsync(UserLogSpec.build(LogAction.Update, folder)
            //        .setMessage("permissions set"))
        }
    }

    override fun updateAcl(folder: Folder, acl: Acl?) {
        if (acl == null) {
            logger.warn("Ignoring null ACL on folder: {}", folder)
            return
        }
        canSetAclOnFolder(acl, folder.acl, false)

        val resolvedAcl = permissionDao.resolveAcl(acl, false)
        folderDao.updateAcl(folder.id, resolvedAcl)

        transactionEventManager.afterCommit(true) {
            invalidate(folder)
            //logService.logAsync(UserLogSpec.build(LogAction.Update, folder)
            //        .setMessage("permissions updated"))
        }
    }

    override fun get(folder: Folder): Folder {
        return get(folder.id)
    }


    override fun get(id: UUID?): Folder {
        if (id != null) {
            if (id==ROOT_ID) {
                return getRoot()
            }
        }
        val f: Folder
        try {
            f = folderCache.get(id)
            if (!hasPermission(f.acl, Access.Read)) {
                throw EmptyResultDataAccessException("Failed to find folder: $id", 1)
            }
            return f

        } catch (e: Exception) {
            throw EmptyResultDataAccessException("Failed to find folder: $id", 1)
        }
    }

    override fun getRoot(): Folder {
        return folderDao.getRootFolder()
    }


    override fun get(parent: UUID?, name: String): Folder {
        return folderDao.get(parent, name, false)
    }

    override fun get(path: String): Folder? {
        val rootFolder = folderDao.getRootFolder()
        var parentId = rootFolder.id
        var current: Folder? = null

        if ("/" == path) {
            return  rootFolder
        }

        // Just throw the exception to the caller,don't return null
        // as none of the other 'get' functions do.
        for (name in Splitter.on("/").omitEmptyStrings().split(path)) {
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
        return folderDao.getChildren(folderDao.getRootFolder().id)
    }

    override fun getAll(ids: Collection<UUID>): List<Folder> {
        return folderDao.getAll(ids)
    }

    override fun getAllIds(dyhi: DyHierarchy): List<UUID> {
        return folderDao.getAllIds(dyhi)
    }

    override fun getChildren(folder: Folder): List<Folder> {
        return folderDao.getChildren(folder)
    }

    override fun renameUserFolder(user:User, newName: String): Boolean {
        if (folderDao.renameUserFolder(user, newName)) {
            transactionEventManager.afterCommit(true) {
                val folder = get(user.homeFolderId)
                invalidate(folder, folder.parentId)
            }
            return true
        }
        return false
    }

    override fun update(folderId : UUID, updated: FolderUpdate): Boolean {
        if (!hasPermission(folderDao.getAcl(folderId), Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        if (folderId == folderDao.getRootFolder().id) {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }

        var current = folderDao.get(folderId)
        val parentSwap = !Objects.equals(current.parentId, updated.parentId);

        if (parentSwap) {

            if (isDescendantOf(folderDao.get(updated.parentId), current)) {
                throw ArchivistWriteException("You cannot move a folder into one of its descendants.")
            }

            if (current.dyhiId != null) {
                throw ArchivistWriteException("You cannot move a DyHi folder")
            }
        }

        val result = folderDao.update(folderId, updated)
        current = folderDao.get(folderId)

        /**
         * If there is a parent swap then the folder gets perms from the new parent.
         * This has to be recursive.  Some people might lose access.
         */
        if (result && parentSwap) {
            val targetFolder = folderDao.get(updated.parentId)
            setAcl(current, targetFolder.acl, true, false)

            for (child in getAllDescendants(current, false)) {
                setAcl(child, targetFolder.acl, true, false)
            }
        }

        if (result) {
            transactionEventManager.afterCommit(true, {
                invalidate(current, current.parentId)
                //logService.logAsync(UserLogSpec.build(LogAction.Update, updated))

                val folder = get(folderId)
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

    override fun deleteAll(ids: Collection<UUID>): Int {
        return folderDao.deleteAll(ids)
    }

    override fun trash(folder: Folder): TrashedFolderOp {

        if (!hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You don't have the permissions to delete this folder")
        }

        if (folder.id == folderDao.getRootFolder().id) {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }
        /**
         * Don't allow trashing of dyhi folders
         */
        if (folder.dyhiId != null) {
            throw ArchivistWriteException("Cannot deleted dynamic hierarchy folder.")
        }

        if (folder.dyhiRoot) {
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

            if (!hasPermission(child.acl, Access.Write)) {
                throw ArchivistWriteException(
                        "You don't have the permissions to delete the subfolder " + child.name)
            }

            if (folderDao.delete(child)) {
                order++
                trashFolderDao.create(child, op, false, order)
                transactionEventManager.afterCommit(false, {
                    invalidate(child)
                    //logService.logAsync(UserLogSpec.build(LogAction.Delete, child))
                })
            }
        }

        val tax = getParentTaxonomy(folder)
        if (folder.taxonomyRoot) {
            taxonomyService.delete(tax, false)
        }

        val result = folderDao.delete(folder)

        if (result) {
            transactionEventManager.afterCommit(true, {
                invalidate(folder)
                //logService.logAsync(UserLogSpec.build(LogAction.Delete, folder))
            })

            if (tax != null) {
                transactionEventManager.afterCommit(false, {
                    if (folder.taxonomyRoot) {
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

        if (!hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        if (folder.id == folderDao.getRootFolder().id) {
            throw ArchivistWriteException("You cannot make changes to the root folder")
        }

        if (folder.dyhiRoot) {
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
                    //logService.logAsync(UserLogSpec.build(LogAction.Delete, folder))
                })
            }
        }

        val result = folderDao.delete(folder)
        if (result) {
            transactionEventManager.afterCommit(true, {
                invalidate(folder)
                //logService.logAsync(UserLogSpec.build(LogAction.Delete, folder))
            })
        }
        return result
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun setFoldersForAsset(assetId: String, folders: List<UUID>) {
        indexDao.setLinks(assetId, "folder", folders)
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun addAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>> {

        if (!hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        if (folder.search != null) {
            throw ArchivistWriteException("Cannot add assets to a smart folder.  Remove the search first.")
        }

        val result = indexDao.appendLink("folder", folder.id, assetIds)
        invalidate(folder)
        //logService.logAsync(UserLogSpec.build("add_assets", folder).putToAttrs("count", assetIds.size))

        val tax = getParentTaxonomy(folder)
        if (tax != null) {
            taxonomyService.tagTaxonomyAsync(tax, folder, false)
        }

        return result
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun removeAssets(folder: Folder, assetIds: List<String>): Map<String, List<Any>> {

        if (!hasPermission(folder.acl, Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        val result = indexDao.removeLink("folder", folder.id, assetIds)
        //logService.logAsync(UserLogSpec.build("remove_assets", folder).putToAttrs("assetIds", assetIds))
        invalidate(folder)

        val tax = getParentTaxonomy(folder)
        if (tax != null) {
            taxonomyService.untagTaxonomyFoldersAsync(tax, folder, assetIds)
        }

        return result
    }

    override fun invalidate(folder: Folder?, vararg additional: UUID?) {
        if (folder != null) {
            if (folder.parentId != null) {
                childCache.invalidate(folder.parentId)
                folderCache.invalidate(folder.parentId)
            }
            childCache.invalidate(folder.id)
            folderCache.invalidate(folder.id)
        }

        for (id in additional) {
            if (id != null) {
                childCache.invalidate(id)
                folderCache.invalidate(id)
            }
        }
    }

    override fun isInTaxonomy(folder: Folder): Boolean {
        val root = folderDao.getRootFolder()
        var current = folder

        if (folder.taxonomyRoot) {
            return true
        }
        while (current.parentId != root.id) {
            current = get(folder.parentId)
            if (current.taxonomyRoot) {
                return true
            }
        }
        return false
    }

    override fun getParentTaxonomy(folder: Folder): Taxonomy? {
        val root = folderDao.getRootFolder()
        var current = folder
        if (current.taxonomyRoot) {
            return taxonomyService.get(folder)
        }
        while (current.parentId != root.id) {
            current = get(current.parentId)
            if (current.taxonomyRoot) {
                return taxonomyService.get(current)
            }
        }
        return null
    }

    override fun getAllAncestors(folder: Folder, includeStart: Boolean, taxOnly: Boolean): List<Folder> {
        val root = folderDao.getRootFolder()
        val result = Lists.newArrayList<Folder>()
        if (includeStart) {
            result.add(folder)
        }
        var start = folder
        while (start.parentId != root.id) {
            start = get(start.parentId)
            result.add(start)
            if (start.taxonomyRoot && taxOnly) {
                break
            }
        }
        return result
    }

    override fun getPath(folder: Folder) : String {
        val ancestors = getAllAncestors(folder, true, false)

        val sb = StringBuilder(256)
        for (f in ancestors.reversed()) {
            sb.append("/").append(f.name)
        }
        return sb.toString()
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
            if (isRootFolder(current)) {
                continue
            }

            /*
             * This is a potential optimization to try out that limits the need to traverse into all
             * child folders from a root.  For example, if /exports is set with a query that searches
             * for all assets that have an export ID, then there is no need to traverse all the sub
             * folders.
             */
            if (!current.recursive && forSearch) {
                continue
            }
            try {
                val children = childCache.get(current.id)
                        .stream()
                        .filter { f -> hasPermission(f.acl, Access.Read) }
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

    override fun submitCreate(spec: FolderSpec): Future<Folder> {
        return folderExecutor.submit<Folder> { create(spec) }
    }

    override fun submitCreate(parent: Folder, spec: FolderSpec): Future<Folder> {
        return folderExecutor.submit<Folder> { create(parent, spec) }
    }

    override fun create(parent: Folder, spec: FolderSpec, errorIfExists: Boolean): Folder {
        if (!hasPermission(parent.acl, Access.Write)) {
            throw ArchivistWriteException("You cannot make changes to this folder")
        }

        if (spec.name!!.contains("/", true)) {
            throw ArchivistWriteException("You cannot have slashes in folder names")
        }

        var result: Folder

        try {
            result = get(spec.parentId, spec.name as String)
            if (errorIfExists) {
                throw ArchivistWriteException("A folder with the same name already exists. Please choose a different name.")
            }
        } catch (e: EmptyResultDataAccessException) {
            spec.acl.whenNullOrEmpty {
                spec.acl = parent.acl
            }
            result = folderDao.create(spec)
            spec.created = true
            setAcl(result, spec.acl, true, false)
            emitFolderCreated(result)
            result = get(result.id)
        }

        return result
    }

    private fun emitFolderCreated(folder: Folder) {
        transactionEventManager.afterCommit(true, {
            invalidate(null, folder.parentId)
            //logService.logAsync(UserLogSpec.build(LogAction.Create, folder))

            if (folder.dyhiId == null && folder.search != null) {
                val tax = getParentTaxonomy(folder)
                if (tax != null) {
                    taxonomyService.tagTaxonomy(tax, folder, true)
                }
            }
        })
    }

    override fun create(spec: FolderSpec, errorIfExists: Boolean): Folder {
        return create(folderDao.get(spec.parentId), spec, errorIfExists)
    }

    override fun create(spec: FolderSpec): Folder {
        return create(folderDao.get(spec.parentId), spec, false)
    }

    override fun createStandardFolders(org: Organization): Folder {
        val root = folderDao.createRootFolder(org)
        val userSpec = FolderSpec("Users", root)
        userSpec.recursive = false

        val libSpec = FolderSpec("Library", root)
        libSpec.recursive = false
        libSpec.search = AssetSearch()

        create(userSpec)
        create(libSpec)
        return root
    }

    override fun createUserFolder(username: String, perm: Permission): Folder {
        // This folder can be created before the user is actually fully
        // authenticated in the case of external auth systems.
        val adminUser = userDao.get("admin")
        val everyone = permissionDao.get(Groups.EVERYONE)

        val rootFolder = folderDao.getRootFolder()
        val userFolder = folderDao.get(rootFolder.id, "Users", true)
        val spec = FolderSpec(
                username,
                userFolder.id)
        spec.userId = adminUser.id
        val folder = folderDao.create(spec)
        folderDao.setAcl(folder.id, Acl()
                .addEntry(perm, Access.Read, Access.Write)
                .addEntry(everyone, Access.Read.value), true)
        return folder
    }

    override fun getTrashedFolders(): List<TrashedFolder> {
        return trashFolderDao.getAll(getUserId())
    }

    override fun getTrashedFolders(folder: Folder): List<TrashedFolder> {
        return trashFolderDao.getAll(folder, getUserId())
    }

    override fun getTrashedFolder(id: UUID): TrashedFolder {
        return trashFolderDao[id, getUserId()]
    }

    override fun emptyTrash(): List<UUID> {
        return trashFolderDao.removeAll(getUserId())
    }

    override fun emptyTrash(ids: List<UUID>): List<UUID> {
        return trashFolderDao.removeAll(ids, getUserId())
    }

    override fun trashCount(): Int {
        return trashFolderDao.count(getUserId())
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

        val ROOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        private val logger = LoggerFactory.getLogger(FolderServiceImpl::class.java)
    }

}

