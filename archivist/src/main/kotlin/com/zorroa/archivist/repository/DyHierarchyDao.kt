package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.Sets
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.service.event
import com.zorroa.common.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface DyHierarchyDao : GenericDao<DyHierarchy, DyHierarchySpec> {

    operator fun get(folder: Folder): DyHierarchy
}

@Repository
class DyHeirarchyDaoImpl : AbstractDao(), DyHierarchyDao {

    @Autowired
    lateinit var userDaoCache: UserDaoCache

    private val MAPPER = RowMapper { rs, _ ->
        val h = DyHierarchy()
        h.folderId = rs.getObject("pk_folder") as UUID
        h.id = rs.getObject("pk_dyhi") as UUID
        h.timeCreated = rs.getLong("time_created")
        h.user = userDaoCache.getUser(rs.getObject("pk_user_created") as UUID)
        h.levels = Json.deserialize(rs.getString("json_levels"),
                object : TypeReference<List<DyHierarchyLevel>>() {

                })
        h
    }

    override fun create(spec: DyHierarchySpec): DyHierarchy {
        val id = uuid1.generate()
        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.folderId!!)
            ps.setObject(3, getUserId())
            ps.setLong(4, System.currentTimeMillis())
            ps.setInt(5, spec.levels.size)
            ps.setString(6, Json.serializeToString(spec.levels, "[]"))
            ps.setObject(7, getUser().organizationId)
            ps
        }

        logger.event(LogObject.DYHI, LogAction.CREATE,
                mapOf("dyhiId" to id, "folderId" to spec.folderId))

        return get(id)
    }

    override fun get(id: UUID): DyHierarchy {
        return jdbc.queryForObject<DyHierarchy>(
                "$GET WHERE pk_organization=? AND pk_dyhi=?", MAPPER, getOrgId(), id)
    }

    override fun get(folder: Folder): DyHierarchy {
        return jdbc.queryForObject<DyHierarchy>(
                "$GET WHERE pk_organization=? AND pk_folder=?", MAPPER, getOrgId(), folder.id)
    }

    override fun refresh(obj: DyHierarchy): DyHierarchy {
        return get(obj.id)
    }

    override fun getAll(): List<DyHierarchy> {
        return jdbc.query("$GET WHERE pk_organization=?" , MAPPER, getOrgId())
    }

    override fun getAll(paging: Pager): PagedList<DyHierarchy> {
        return PagedList(paging.setTotalCount(count()),
                jdbc.query<DyHierarchy>("$GET WHERE pk_organization=? ORDER BY pk_dyhi LIMIT ? OFFSET ?",
                        MAPPER, getOrgId(), paging.size, paging.from))
    }

    override fun update(id: UUID, spec: DyHierarchy): Boolean {
        return jdbc.update(UPDATE, spec.folderId, spec.levels.size,
                Json.serializeToString(spec.levels, "[]"), id) == 1
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM dyhi WHERE pk_organization=? AND pk_dyhi=?", getOrgId(), id) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("$COUNT WHERE pk_organization=?", Long::class.java, getOrgId())
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

        private const val COUNT = "SELECT COUNT(1) FROM dyhi"

        private const val GET = "SELECT " +
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
