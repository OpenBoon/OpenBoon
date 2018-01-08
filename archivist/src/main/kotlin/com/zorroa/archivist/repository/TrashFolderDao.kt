package com.zorroa.archivist.repository

import com.google.common.collect.Sets
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.TrashedFolder
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface TrashFolderDao {

    fun create(folder: Folder, opid: String, primary: Boolean, order: Int): Int

    operator fun get(id: Int, userId: Int): TrashedFolder

    fun getAll(userId: Int): List<TrashedFolder>

    fun count(user: Int): Int

    /**
     * Return all primary deleted folders for the given parent folder.
     * from the specified user.
     *
     * @param parent
     * @param userId
     * @return
     */
    fun getAll(parent: Folder, userId: Int): List<TrashedFolder>

    /**
     * Return all folders deleted by the given OP id.
     * @param opId
     * @return
     */
    fun getAll(opId: String): List<TrashedFolder>

    fun getAllIds(opId: String): List<Int>

    fun getAllIds(user: Int): List<Int>

    /**
     * Delete's all folders from a given OP.  Returns the number of
     * trash folders deleted.
     *
     * @param opid
     * @return
     */
    fun removeAll(opid: String): List<Int>

    fun removeAll(ids: List<Int>, user: Int): List<Int>

    fun removeAll(userId: Int): List<Int>
}


@Repository
open class TrashFolderDaoImpl : AbstractDao(), TrashFolderDao {

    @Autowired
    internal var userDaoCache: UserDaoCache? = null

    private val MAPPER = RowMapper<TrashedFolder> { rs, _ ->
        val folder = TrashedFolder()
        folder.opId = rs.getString("str_opid")
        folder.id = rs.getInt("pk_folder_trash")
        folder.folderId = rs.getInt("pk_folder")
        folder.name = rs.getString("str_name")
        folder.user = userDaoCache!!.getUser(rs.getInt("user_created"))
        folder.userDeleted = userDaoCache!!.getUser(rs.getInt("user_deleted"))
        folder.isRecursive = rs.getBoolean("bool_recursive")
        folder.timeCreated = rs.getLong("time_created")
        folder.timeModified = rs.getLong("time_modified")
        folder.timeModified = rs.getLong("time_deleted")

        val parent = rs.getObject("pk_parent")
        if (parent != null) {
            folder.parentId = parent as Int
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

    override fun create(folder: Folder, opid: String, primary: Boolean, order: Int): Int {
        val time = System.currentTimeMillis()
        val user = SecurityUtils.getUser().id

        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_folder_trash"))
            ps.setInt(1, folder.id)
            ps.setInt(2, folder.parentId!!)
            ps.setString(3, opid)
            ps.setString(4, folder.name)
            ps.setString(5, Json.serializeToString(folder.search, null))
            ps.setString(6, Json.serializeToString(folder.acl, null))
            ps.setBoolean(7, folder.isRecursive)
            ps.setInt(8, folder.user.id)
            ps.setLong(9, folder.timeCreated)
            ps.setLong(10, folder.timeModified)
            ps.setInt(11, user)
            ps.setLong(12, time)
            ps.setBoolean(13, primary)
            ps.setInt(14, order)
            ps.setString(15, Json.serializeToString(folder.attrs, "{}"))
            ps
        }, keyHolder)

        return keyHolder.key.toInt()
    }

    override fun get(id: Int, user: Int): TrashedFolder {
        return jdbc.queryForObject<TrashedFolder>(
                GET + " WHERE pk_folder_trash=? AND user_deleted=?", MAPPER, id, user)
    }

    override fun getAll(user: Int): List<TrashedFolder> {
        return jdbc.query<TrashedFolder>(
                GET + " WHERE user_deleted=? AND bool_primary=?", MAPPER, user, true)
    }

    override fun count(user: Int): Int {
        return jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash WHERE user_deleted=? AND bool_primary=?",
                Int::class.java, user, true)
    }

    override fun getAll(parent: Folder, user: Int): List<TrashedFolder> {
        return jdbc.query<TrashedFolder>(
                GET + " WHERE pk_parent=? AND user_deleted=? AND bool_primary=?", MAPPER,
                parent.id, user, true)
    }

    override fun getAll(opId: String): List<TrashedFolder> {
        return jdbc.query<TrashedFolder>(GET + " WHERE str_opid=? ORDER BY int_order DESC", MAPPER, opId)
    }

    override fun getAllIds(opId: String): MutableList<Int> {
        return jdbc.queryForList(
                "SELECT pk_folder_trash FROM folder_trash WHERE str_opid=? ORDER BY int_order DESC", Int::class.java, opId)
    }

    override fun getAllIds(user: Int): MutableList<Int> {
        return jdbc.queryForList(
                "SELECT pk_folder_trash FROM folder_trash WHERE user_deleted=? ORDER BY int_order DESC", Int::class.java, user)
    }

    override fun removeAll(opId: String): List<Int> {
        val ids = getAllIds(opId)
        if (jdbc.update("DELETE FROM folder_trash WHERE str_opid=?", opId) == ids.size) {
            return ids
        } else {
            val leftOver = getAllIds(opId)
            leftOver.removeAll(ids)
            return leftOver
        }
    }

    override fun removeAll(ids: List<Int>, user: Int): List<Int> {
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

    override fun removeAll(user: Int): List<Int> {
        val ids = getAllIds(user)
        if (jdbc.update("DELETE FROM folder_trash WHERE user_deleted=?", user) == ids.size) {
            return ids
        } else {
            val leftOver = getAllIds(user)
            leftOver.removeAll(ids)
            return leftOver
        }
    }

    companion object {

        private val INSERT = JdbcUtils.insert("folder_trash",
                "pk_folder",
                "pk_parent",
                "str_opid",
                "str_name",
                "json_search",
                "json_acl",
                "bool_recursive",
                "user_created",
                "time_created",
                "time_modified",
                "user_deleted",
                "time_deleted",
                "bool_primary",
                "int_order",
                "json_attrs")

        private val GET = "SELECT * FROM folder_trash "
    }
}
