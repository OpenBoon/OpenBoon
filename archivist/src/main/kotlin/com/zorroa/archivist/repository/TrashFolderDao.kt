package com.zorroa.archivist.repository

import com.google.common.collect.Sets
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.TrashedFolder
import com.zorroa.archivist.security.getUserId
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface TrashFolderDao {

    fun create(folder: Folder, opId: String, primary: Boolean, order: Int): UUID

    operator fun get(id: UUID, userId: UUID): TrashedFolder

    fun getAll(userId: UUID): List<TrashedFolder>

    fun count(userId: UUID): Int

    /**
     * Return all primary deleted folders for the given parent folder.
     * from the specified user.
     *
     * @param parent
     * @param userId
     * @return
     */
    fun getAll(parent: Folder, userId: UUID): List<TrashedFolder>

    /**
     * Return all folders deleted by the given OP id.
     * @param opId
     * @return
     */
    fun getAll(opId: String): List<TrashedFolder>

    fun getAllIds(opId: String): List<UUID>

    fun getAllIds(user: UUID): List<UUID>

    /**
     * Delete's all folders from a given OP.  Returns the number of
     * trash folders deleted.
     *
     * @param opid
     * @return
     */
    fun removeAll(opId: String): List<UUID>

    fun removeAll(ids: List<UUID>, user: UUID): List<UUID>

    fun removeAll(userId: UUID): List<UUID>
}


@Repository
class TrashFolderDaoImpl : AbstractDao(), TrashFolderDao {

    @Autowired
    internal lateinit var userDaoCache: UserDaoCache

    private val MAPPER = RowMapper<TrashedFolder> { rs, _ ->
        val folder = TrashedFolder()
        folder.opId = rs.getString("str_opid")
        folder.id = rs.getObject("pk_folder_trash") as UUID
        folder.folderId = rs.getObject("pk_folder") as UUID
        folder.name = rs.getString("str_name")
        folder.user = userDaoCache.getUser(rs.getObject("pk_user_created") as UUID)
        folder.userDeleted = userDaoCache.getUser(rs.getObject("pk_user_deleted") as UUID)
        folder.isRecursive = rs.getBoolean("bool_recursive")
        folder.timeCreated = rs.getLong("time_created")
        folder.timeModified = rs.getLong("time_modified")
        folder.timeModified = rs.getLong("time_deleted")

        val parent = rs.getObject("pk_parent")
        if (parent != null) {
            folder.parentId = parent as UUID
        }

        val searchString = rs.getString("json_search")
        if (searchString != null) {
            folder.search = Json.deserialize<AssetSearch>(searchString, AssetSearch::class.java)
        }

        val aclString = rs.getString("json_acl")
        if (aclString != null) {
            folder.acl = Json.deserialize<Acl>(aclString, Acl::class.java)
        }

        val attrs = rs.getString("json_attrs")
        attrs?.let {
            folder.attrs = Json.deserialize(attrs, Json.GENERIC_MAP)
        }

        folder
    }

    override fun create(folder: Folder, opId: String, primary: Boolean, order: Int): UUID {
        val time = System.currentTimeMillis()
        val user = getUserId()
        val id = uuid1.generate()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, folder.id)
            ps.setObject(3, folder.parentId)
            ps.setString(4, opId)
            ps.setString(5, folder.name)
            ps.setString(6, Json.serializeToString(folder.search, null))
            ps.setString(7, Json.serializeToString(folder.acl, null))
            ps.setBoolean(8, folder.recursive)
            ps.setObject(9, folder.user.id)
            ps.setLong(10, folder.timeCreated)
            ps.setLong(11, folder.timeModified)
            ps.setObject(12, user)
            ps.setLong(13, time)
            ps.setBoolean(14, primary)
            ps.setInt(15, order)
            ps.setString(16, Json.serializeToString(folder.attrs, "{}"))
            ps
        })
        return id
    }

    override fun get(id: UUID, userId: UUID): TrashedFolder {
        return jdbc.queryForObject<TrashedFolder>(
                "$GET WHERE pk_folder_trash=? AND pk_user_deleted=?", MAPPER, id, userId)
    }

    override fun getAll(userId: UUID): List<TrashedFolder> {
        return jdbc.query<TrashedFolder>(
                "$GET WHERE pk_user_deleted=? AND bool_primary=?", MAPPER, userId, true)
    }

    override fun count(userId: UUID): Int {
        return jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash WHERE pk_user_deleted=? AND bool_primary=?",
                Int::class.java, userId, true)
    }

    override fun getAll(parent: Folder, userId: UUID): List<TrashedFolder> {
        return jdbc.query<TrashedFolder>(
                "$GET WHERE pk_parent=? AND pk_user_deleted=? AND bool_primary=?", MAPPER,
                parent.id, userId, true)
    }

    override fun getAll(opId: String): List<TrashedFolder> {
        return jdbc.query<TrashedFolder>("$GET WHERE str_opid=? ORDER BY int_order DESC", MAPPER, opId)
    }

    override fun getAllIds(opId: String): MutableList<UUID> {
        return jdbc.queryForList(
                "SELECT pk_folder_trash FROM folder_trash WHERE str_opid=? ORDER BY int_order DESC", UUID::class.java, opId)
    }

    override fun getAllIds(user: UUID): MutableList<UUID> {
        return jdbc.queryForList(
                "SELECT pk_folder_trash FROM folder_trash WHERE pk_user_deleted=? ORDER BY int_order DESC", UUID::class.java, user)
    }

    override fun removeAll(opId: String): List<UUID> {
        val ids = getAllIds(opId)
        return if (jdbc.update("DELETE FROM folder_trash WHERE str_opid=?", opId) == ids.size) {
            ids
        } else {
            val leftOver = getAllIds(opId)
            leftOver.removeAll(ids)
            leftOver
        }
    }

    override fun removeAll(ids: List<UUID>, user: UUID): List<UUID> {
        val opIds = Sets.newHashSet<String>()
        for (id in ids) {
            try {
                val folder = get(id, user)
                if (!opIds.contains(folder.opId)) {
                    opIds.add(folder.opId)
                    removeAll(folder.opId)
                }
            } catch (e: EmptyResultDataAccessException) {
                logger.warn("Unable to find trash folder id: {}", id)
            }

        }
        return ids
    }

    override fun removeAll(userId: UUID): List<UUID> {
        val ids = getAllIds(userId)
        return if (jdbc.update("DELETE FROM folder_trash WHERE pk_user_deleted=?", userId) == ids.size) {
            ids
        } else {
            val leftOver = getAllIds(userId)
            leftOver.removeAll(ids)
            leftOver
        }
    }

    companion object {

        private val INSERT = JdbcUtils.insert("folder_trash",
                "pk_folder_trash",
                "pk_folder",
                "pk_parent",
                "str_opid",
                "str_name",
                "json_search",
                "json_acl",
                "bool_recursive",
                "pk_user_created",
                "time_created",
                "time_modified",
                "pk_user_deleted",
                "time_deleted",
                "bool_primary",
                "int_order",
                "json_attrs")

        private const val GET = "SELECT * FROM folder_trash "
    }
}
