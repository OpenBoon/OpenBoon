package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.Sets
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.DyHierarchy
import com.zorroa.archivist.domain.DyHierarchyLevel
import com.zorroa.archivist.domain.DyHierarchySpec
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface DyHierarchyDao : GenericDao<DyHierarchy, DyHierarchySpec> {

    operator fun get(folder: Folder): DyHierarchy

    fun isWorking(d: DyHierarchy): Boolean

    fun setWorking(d: DyHierarchy, value: Boolean): Boolean
}

@Repository
class DyHeirarchyDaoImpl : AbstractDao(), DyHierarchyDao {

    @Autowired
    private var userDaoCache: UserDaoCache? = null

    /**
     * The current list of working dyhi processes.
     */
    private val WORKING = Sets.newConcurrentHashSet<DyHierarchy>()

    private val MAPPER = RowMapper<DyHierarchy> { rs, _ ->
        val h = DyHierarchy()
        h.folderId = rs.getObject("pk_folder") as UUID
        h.id = rs.getObject("pk_dyhi") as UUID
        h.timeCreated = rs.getLong("time_created")
        h.user = userDaoCache!!.getUser(rs.getObject("pk_user_created") as UUID)
        h.levels = Json.deserialize<List<DyHierarchyLevel>>(rs.getString("json_levels"),
                object : TypeReference<List<DyHierarchyLevel>>() {

                })
        h.isWorking = WORKING.contains(h)
        h
    }

    override fun create(spec: DyHierarchySpec): DyHierarchy {
        val id = uuid1.generate()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.folderId!!)
            ps.setObject(3, getUserId())
            ps.setLong(4, System.currentTimeMillis())
            ps.setInt(5, spec.levels.size)
            ps.setString(6, Json.serializeToString(spec.levels, "[]"))
            ps.setObject(7, getUser().organizationId)
            ps
        })
        return get(id)
    }

    override fun get(id: UUID): DyHierarchy {
        return jdbc.queryForObject<DyHierarchy>(GET + " WHERE pk_dyhi=?", MAPPER, id)
    }

    override fun get(folder: Folder): DyHierarchy {
        return jdbc.queryForObject<DyHierarchy>(GET + " WHERE pk_folder=?", MAPPER, folder.id)
    }

    override fun refresh(obj: DyHierarchy): DyHierarchy {
        return get(obj.id)
    }

    override fun getAll(): List<DyHierarchy> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getAll(paging: Pager): PagedList<DyHierarchy> {
        return PagedList(paging.setTotalCount(count()),
                jdbc.query<DyHierarchy>(GET + "ORDER BY pk_dyhi LIMIT ? OFFSET ?",
                        MAPPER, paging.size, paging.from))
    }

    override fun update(id: UUID, spec: DyHierarchy): Boolean {
        return jdbc.update(UPDATE, spec.folderId, spec.levels.size,
                Json.serializeToString(spec.levels, "[]"), id) == 1
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM dyhi WHERE pk_dyhi=?", id) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM dyhi", Long::class.java)
    }

    override fun isWorking(d: DyHierarchy): Boolean {
        return WORKING.contains(d)
    }

    override fun setWorking(d: DyHierarchy, value: Boolean): Boolean {
        return if (value) {
            WORKING.add(d)
        } else {
            WORKING.remove(d)
        }
    }

    companion object {

        private val INSERT = JdbcUtils.insert("dyhi",
                "pk_dyhi",
                "pk_folder",
                "pk_user_created",
                "time_created",
                "int_levels",
                "json_levels",
                "pk_organization")

        private val GET = "SELECT " +
                "pk_dyhi," +
                "pk_folder, " +
                "pk_user_created, " +
                "time_created," +
                "int_levels," +
                "json_levels " +
                "FROM " +
                "dyhi "

        private val UPDATE = JdbcUtils.update("dyhi", "pk_dyhi",
                "pk_folder",
                "int_levels",
                "json_levels")
    }
}
