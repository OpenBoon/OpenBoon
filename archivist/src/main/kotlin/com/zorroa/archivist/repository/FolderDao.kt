package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.*
import com.zorroa.archivist.util.event
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface FolderDao {

    fun get(id: UUID?): Folder

    fun get(parent: UUID?, name: String, ignorePerms: Boolean): Folder

    fun get(parent: Folder, name: String): Folder

    fun getAll(ids: Collection<UUID>): List<Folder>

    fun getChildren(parentId: UUID): List<Folder>

    fun getChildrenInsecure(parentId: UUID): List<Folder>

    fun getChildren(folder: Folder): List<Folder>

    fun getAllIds(dyhi: DyHierarchy): List<UUID>

    fun getRootFolder(): Folder

    fun exists(parentId: UUID, name: String): Boolean

    fun count(): Int

    fun count(d: DyHierarchy): Int

    fun exists(parent: Folder, name: String): Boolean

    fun create(spec: FolderSpec): Folder

    fun create(spec: TrashedFolder): Folder

    fun createRootFolder(org: Organization) : Folder

    fun update(id: UUID, folder: FolderUpdate): Boolean

    fun deleteAll(dyhi: DyHierarchy): Int

    fun delete(folder: Folder): Boolean

    fun deleteAll(ids: Collection<UUID>): Int

    fun hasAccess(folder: Folder, access: Access): Boolean

    fun setTaxonomyRoot(folder: Folder, value: Boolean): Boolean

    fun setDyHierarchyRoot(folder: Folder, field: String): Boolean

    fun removeDyHierarchyRoot(folder: Folder): Boolean

    fun updateAcl(folder: UUID, acl: Acl)

    fun setAcl(folder: UUID, acl: Acl?, replace: Boolean)

    fun setAcl(folder: UUID, acl: Acl)

    fun getAcl(folder: UUID): Acl

    fun renameUserFolder(user: User, newName:String): Boolean
}

@Repository
class FolderDaoImpl : AbstractDao(), FolderDao {

    @Autowired
    internal lateinit var userDaoCache: UserDaoCache


    private val MAPPER = RowMapper { rs, _ ->
        val id = rs.getObject("pk_folder") as UUID
        val dyhiField = rs.getString("str_dyhi_field")

        var search : String? = rs.getString("json_search")
        if (search == null && dyhiField != null) {
            search = "{}"
        }

        var assetSearch : AssetSearch? = null
        if (search != null) {
            assetSearch = Json.deserialize(search, AssetSearch::class.java)
            /**
             * The dyhi field is added to the search on the fly, not baked in.
             */
            if (dyhiField != null) {
                assetSearch.addToFilter().addToExists(dyhiField)
            }
        }

        Folder(
                id,
                rs.getString("str_name"),
                rs.getObject("pk_parent") as UUID?,
                rs.getObject("pk_organization") as UUID,
                rs.getObject("pk_dyhi") as UUID?,
                userDaoCache.getUser(rs.getObject("pk_user_created") as UUID),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getBoolean("bool_recursive"),
                rs.getBoolean("bool_dyhi_root"),
                rs.getString("str_dyhi_field"),
                rs.getInt("int_child_count"),
                getAcl(id),
                assetSearch,
                rs.getBoolean("bool_tax_root"),
                Json.deserialize(rs.getString("json_attrs"), Json.GENERIC_MAP))

    }

    override operator fun get(id: UUID?): Folder {
        if (id == null) {
            return getRootFolder()
        }
        return jdbc.queryForObject<Folder>(appendReadAccess("$GET WHERE pk_organization=? AND pk_folder=?"),
                MAPPER, *appendAclArgs(getOrgId(), id))
    }

    override operator fun get(parent: UUID?, name: String, ignorePerms: Boolean): Folder {
        val parentId =if (parent == null) {
            getRootFolder().id
        }
        else {
            parent
        }
        try {
            return if (true) {
                jdbc.queryForObject<Folder>("$GET WHERE pk_organization=? AND pk_parent=? and str_name=?",
                        MAPPER, getOrgId(), parentId, name)
            } else {
                jdbc.queryForObject<Folder>(
                        appendReadAccess("$GET WHERE pk_organization=? AND pk_parent=? AND str_name=?"), MAPPER,
                        *appendAclArgs(getOrgId(),parentId, name))
            }
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to find folder, parent: $parent name: $name", 1)
        }
    }

    override operator fun get(parent: Folder, name: String): Folder {
        return get(parent.id, name, false)
    }

    override fun getRootFolder(): Folder {
        return jdbc.queryForObject("$GET WHERE pk_organization=? AND pk_parent IS NULL and str_name='/'",
                MAPPER, getOrgId())
    }

    override fun getAll(ids: Collection<UUID>): List<Folder> {
        if (ids.isEmpty()) {
            return listOf()
        }
        val sb = StringBuilder(512)
        sb.append(GET)
        sb.append(" WHERE ")
        sb.append(JdbcUtils.`in`("pk_folder", ids.size))
        sb.append(" AND pk_organization=?")
        return jdbc.query(sb.toString(), MAPPER, *ids.toTypedArray().plus(getOrgId()))
    }

    override fun getChildren(parentId: UUID): List<Folder> {
        val sb = StringBuilder(512)
        sb.append(GET)
        sb.append(" WHERE pk_organization=? AND pk_parent=?")
        return jdbc.query(appendReadAccess(sb.toString()), MAPPER, *appendAclArgs(getOrgId(), parentId))
    }

    override fun getChildrenInsecure(parentId: UUID): List<Folder> {
        val sb = StringBuilder(512)
        sb.append(GET)
        sb.append(" WHERE pk_parent=?")
        return jdbc.query(sb.toString(), MAPPER, parentId)
    }

    override fun getChildren(folder: Folder): List<Folder> {
        return getChildren(folder.id)
    }

    override fun getAllIds(dyhi: DyHierarchy): List<UUID> {
        return jdbc.queryForList("SELECT pk_folder FROM folder WHERE pk_organization=? AND pk_dyhi=?",
                UUID::class.java, getOrgId(), dyhi.id)
    }

    override fun exists(parentId: UUID, name: String): Boolean {
        return jdbc.queryForObject("$COUNT folder WHERE pk_organization=? AND pk_parent=? AND str_name=?",
                Int::class.java, getOrgId(), parentId, name) == 1
    }

    override fun count(): Int {
        return jdbc.queryForObject("$COUNT WHERE pk_organization=?", Int::class.java, getOrgId())
    }

    override fun count(d: DyHierarchy): Int {
        return jdbc.queryForObject("$COUNT WHERE pk_organization=? AND pk_dyhi=?", Int::class.java, getOrgId(), d.id)
    }

    override fun exists(parent: Folder, name: String): Boolean {
        return exists(parent.id, name)
    }

    override fun createRootFolder(org: Organization) : Folder {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, null)
            ps.setObject(3, org.id)
            ps.setString(4, "/")
            ps.setObject(5, user.id)
            ps.setLong(6, time)
            ps.setBoolean(7, false)
            ps.setObject(8, user.id)
            ps.setLong(9, time)
            ps.setString(10, null)
            ps.setObject(11, null)
            ps.setString(12, "{}")
            ps
        }

        return getAfterCreate(id)
    }

    override fun create(spec: FolderSpec): Folder {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        if (spec.userId == null) {
            val user = getUserId()
            spec.userId = user
        }

        if (spec.parentId == null) {
            spec.parentId = getRootFolder().id
        }

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.parentId)
            ps.setObject(3, getUser().organizationId)
            ps.setString(4, spec.name)
            ps.setObject(5, spec.userId)
            ps.setLong(6, time)
            ps.setBoolean(7, spec.recursive)
            ps.setObject(8, spec.userId)
            ps.setLong(9, time)
            ps.setString(10, Json.serializeToString(spec.search, null))
            ps.setObject(11, spec.dyhiId)
            ps.setString(12, Json.serializeToString(spec.attrs, "{}"))
            ps
        }

        logger.event("create Folder", mapOf(
                "folderId" to id,
                "folderName" to spec.name))

        return getAfterCreate(id)
    }

    override fun create(spec: TrashedFolder): Folder {
        val time = System.currentTimeMillis()

        if (spec.parentId == null) {
            spec.parentId = getRootFolder().id
        }

        jdbc.update { connection ->
            val ps = connection.prepareStatement(RESTORE)
            ps.setObject(1, spec.folderId)
            ps.setObject(2, spec.parentId)
            ps.setObject(3, getUser().organizationId)
            ps.setString(4, spec.name)
            ps.setObject(5, spec.user.id)
            ps.setLong(6, time)
            ps.setBoolean(7, spec.isRecursive)
            ps.setObject(8, spec.userDeleted.id)
            ps.setLong(9, time)
            ps.setString(10, Json.serializeToString(spec.search, null))
            ps.setString(11, Json.serializeToString(spec.attrs, "{}"))
            ps
        }

        return getAfterCreate(spec.folderId)
    }

    override fun renameUserFolder(user: User, newName:String): Boolean {
        return jdbc.update("UPDATE folder SET str_name=? WHERE pk_organization=? AND pk_folder=?",
                newName, getOrgId(), user.homeFolderId) == 1
    }

    override fun update(id: UUID, folder: FolderUpdate): Boolean {
        Preconditions.checkNotNull(folder.parentId, "Parent folder cannot be null")
        if (getRootFolder().id == id) {
            throw IllegalArgumentException("Cannot modify root folder")
        }

        /**
         * Skip updating the search if its a dyhi so the exists statement
         * doesn't get automatically updated.  Its also
         */
        return if (isDyHi(id)) {
            jdbc.update("$UPDATE_IS_DYHI AND pk_organization=?",
                    System.currentTimeMillis(),
                    getUserId(),
                    folder.parentId,
                    folder.name,
                    folder.recursive,
                    Json.serializeToString(folder.attrs, "{}"),
                    id, getOrgId()) == 1
        } else {
            jdbc.update("$UPDATE AND pk_organization=?",
                    System.currentTimeMillis(),
                    getUserId(),
                    folder.parentId,
                    folder.name,
                    folder.recursive,
                    Json.serializeToString(folder.search, null),
                    Json.serializeToString(folder.attrs, "{}"),
                    id, getOrgId()) == 1
        }
    }

    override fun deleteAll(dyhi: DyHierarchy): Int {
        return jdbc.update("DELETE FROM folder WHERE pk_organization=? AND pk_dyhi=?", getOrgId(), dyhi.id)
    }

    override fun delete(folder: Folder): Boolean {
        return jdbc.update("DELETE FROM folder WHERE pk_organization=? AND pk_folder=?", getOrgId(), folder.id) == 1
    }

    override fun deleteAll(ids: Collection<UUID>): Int {
        if (ids.isEmpty()) {
            return 0
        }
        /**
         * The list has to be sorted from lowest to highest.  A parent folder will
         * always have a lower ID than child folders.  Hopefully this is enough
         * for a clean delete.
         */
        val sorted = ids.sorted().plus(getOrgId())
        return jdbc.update("DELETE FROM folder WHERE " + JdbcUtils.`in`("pk_folder", ids.size) +
                " AND pk_organization=?", *sorted.toTypedArray())
    }

    override fun hasAccess(folder: Folder, access: Access): Boolean {
        return jdbc.queryForObject(appendAccess("$COUNT WHERE pk_organization=? AND pk_folder=?", access),
                Int::class.java, *appendAclArgs(getOrgId(), folder.id)) > 0
    }

    override fun setTaxonomyRoot(folder: Folder, value: Boolean): Boolean {
        if (getRootFolder().id == folder.id) {
            throw IllegalArgumentException("Cannot set taxonomy on root folder")
        }
        return jdbc.update("UPDATE folder SET bool_tax_root=? WHERE pk_organization=? AND pk_folder=? AND bool_tax_root=?",
                value, getOrgId(), folder.id, !value) == 1
    }

    override fun setDyHierarchyRoot(folder: Folder, field: String): Boolean {
        if (getRootFolder().id == folder.id) {
            throw IllegalArgumentException("Cannot set dyhi on root folder")
        }
        return jdbc.update("UPDATE folder SET bool_recursive=?, bool_dyhi_root=?, str_dyhi_field=? WHERE pk_organization=? AND pk_folder=?",
                false, true, field, getOrgId(), folder.id) == 1
    }

    override fun removeDyHierarchyRoot(folder: Folder): Boolean {
        if (getRootFolder().id == folder.id) {
            throw IllegalArgumentException("Cannot remove dyhi on root folder")
        }
        return jdbc.update("UPDATE folder SET bool_recursive=?, bool_dyhi_root=?,str_dyhi_field=null WHERE pk_organization=? AND pk_folder=?",
                true, false, getOrgId(), folder.id) == 1
    }

    override fun setAcl(folder: UUID, acl: Acl) {
        setAcl(folder, acl, true)
    }

    override fun updateAcl(folder: UUID, acl: Acl) {
        setAcl(folder, acl, false)
    }

    override fun setAcl(folder: UUID, acl: Acl?, replace: Boolean) {

        if (acl == null || acl.isEmpty()) {
            return
        }

        if (replace) {
            jdbc.update("DELETE FROM folder_acl WHERE pk_folder=?", folder)
            for (entry in acl) {
                if (entry.getAccess() == 0) {
                    continue
                }
                if (entry.getAccess() > 7) {
                    throw IllegalArgumentException("Invalid Access level "
                            + entry.getAccess() + " for permission ID " + entry.getPermissionId())
                }
                jdbc.update("INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (?,?,?)",
                        entry.getPermissionId(), folder, entry.getAccess())
            }
        } else {

            for (entry in acl) {
                if (entry.getAccess() > 7) {
                    throw IllegalArgumentException("Invalid Access level "
                            + entry.getAccess() + " for permission ID " + entry.getPermissionId())
                }
                if (entry.getAccess() <= 0) {
                    jdbc.update("DELETE FROM folder_acl WHERE pk_folder=? AND pk_permission=?",
                            folder, entry.getPermissionId())
                } else if (jdbc.update("UPDATE folder_acl SET int_access=? WHERE pk_folder=? AND pk_permission=?",
                        entry.getAccess(), folder, entry.getPermissionId()) != 1) {
                    jdbc.update("INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (?,?,?)",
                            entry.getPermissionId(), folder, entry.getAccess())
                }
            }
        }
    }

    override fun getAcl(folder: UUID): Acl {
        val result = Acl()
        jdbc.query("SELECT p.str_authority, p.pk_permission, f.int_access FROM folder_acl f, permission p WHERE " +
                "p.pk_organization=? AND f.pk_permission = p.pk_permission and f.pk_folder=?",
                RowCallbackHandler { rs ->
                    result.add(AclEntry(rs.getString("str_authority"),
                            rs.getObject("pk_permission") as UUID,
                            rs.getInt("int_access"))) }, getOrgId(), folder)
        return result
    }

    /**
     * Append the permissions check to the given query.
     *
     * @param query
     * @return
     */
    private fun appendAccess(query: String, access: Access): String {
        if (hasPermission(Groups.ADMIN)) {
            return query
        }

        val sb = StringBuilder(query.length + 256)
        sb.append(query)
        if (query.contains("WHERE")) {
            sb.append(" AND ")
        } else {
            sb.append(" WHERE ")
        }
        sb.append("((")
        sb.append("SELECT COUNT(1) FROM folder_acl WHERE folder_acl.pk_folder=folder.pk_folder AND ")
        sb.append(JdbcUtils.`in`("folder_acl.pk_permission", getPermissionIds().size))
        sb.append(" AND BITAND(")
        sb.append(access.value)
        sb.append(",int_access) = " + access.value + ") > 0 OR (")
        sb.append("SELECT COUNT(1) FROM folder_acl WHERE folder_acl.pk_folder=folder.pk_folder) = 0)")
        return sb.toString()
    }

    /**
     * Append the permissions check to the given query.
     *
     * @param query
     * @return
     */
    private fun appendWriteAccess(query: String): String {
        return appendAccess(query, Access.Write)
    }

    private fun appendReadAccess(query: String): String {
        return appendAccess(query, Access.Read)
    }

    fun appendAclArgs(vararg args: Any): Array<out Any> {
        if (hasPermission(Groups.ADMIN)) {
            return args
        }

        val result = mutableListOf<Any>()
        for (a in args) {
            result.add(a)
        }
        result.addAll(getPermissionIds())
        return result.toTypedArray()
    }

    /**
     * Called by create so we can return a folder object even if we are stupid and create
     * a folder we don't have access to.
     *
     * @param id
     * @return
     */
    private fun getAfterCreate(id: UUID): Folder {
        return jdbc.queryForObject("$GET WHERE pk_folder=?", MAPPER, id)
    }

    private fun isDyHi(id: UUID): Boolean {
        val row = jdbc.queryForRowSet("SELECT pk_dyhi, bool_dyhi_root FROM folder WHERE pk_folder=?", id)
        try {
            row.next()

            if (row.getBoolean("bool_dyhi_root")) {
                return true
            }
            if (row.getString("pk_dyhi") != null) {
                return true
            }
        } catch (e: Exception) {
            //ignore
        }

        return false

    }

    companion object {

        private const val COUNT = "SELECT COUNT(1) FROM folder "
        private const val GET = "SELECT " +
                "* " +
                "FROM " +
                "folder "

        private val INSERT = JdbcUtils.insert("folder",
                "pk_folder",
                "pk_parent",
                "pk_organization",
                "str_name",
                "pk_user_created",
                "time_created",
                "bool_recursive",
                "pk_user_modified",
                "time_modified",
                "json_search",
                "pk_dyhi",
                "json_attrs")

        private val RESTORE = JdbcUtils.insert("folder",
                "pk_folder",
                "pk_parent",
                "pk_organization",
                "str_name",
                "pk_user_created",
                "time_created",
                "bool_recursive",
                "pk_user_modified",
                "time_modified",
                "json_search",
                "json_attrs")

        private val UPDATE = JdbcUtils.update("folder", "pk_folder",
                "time_modified",
                "pk_user_modified",
                "pk_parent",
                "str_name",
                "bool_recursive",
                "json_search",
                "json_attrs")

        private val UPDATE_IS_DYHI = JdbcUtils.update("folder", "pk_folder",
                    "time_modified",
                    "pk_user_modified",
                    "pk_parent",
                    "str_name",
                    "bool_recursive",
                    "json_attrs")
    }
}
