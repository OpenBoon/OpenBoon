package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.StaticUtils.UUID_REGEXP
import com.zorroa.common.domain.PagedList
import com.zorroa.common.domain.Pager
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface PermissionDao {

    fun getAll(): List<Permission>

    fun create(builder: PermissionSpec, immutable: Boolean): Permission

    fun update(permission: Permission): Permission

    fun renameUserPermission(user:User, newName: String): Boolean

    fun get(id: UUID): Permission

    fun getId(name: String): UUID

    fun get(authority: String): Permission

    fun resolveAcl(acl: Acl?, createMissing: Boolean): Acl

    fun getPaged(page: Pager, filter: DaoFilter): PagedList<Permission>

    fun getPaged(page: Pager): PagedList<Permission>

    fun count(): Long

    fun count(filter: DaoFilter): Long

    fun exists(name: String): Boolean

    fun getAll(user: UserId): List<Permission>

    fun getAll(type: String): List<Permission>

    fun get(type: String, name: String): Permission

    fun getAll(ids: Collection<UUID>?): List<Permission>

    fun getAll(names: List<String>?): List<Permission>

    fun delete(perm: Permission): Boolean
}

@Repository
class PermissionDaoImpl : AbstractDao(), PermissionDao {

    override fun create(builder: PermissionSpec, immutable: Boolean): Permission {

        val id = uuid1.generate()
        try {
            jdbc.update({ connection ->
                val ps = connection.prepareStatement(INSERT)
                ps.setObject(1, id)
                ps.setObject(2, getUser().organizationId)
                ps.setString(3, builder.name)
                ps.setString(4, builder.type)
                ps.setString(5, builder.type + "::" + builder.name)
                ps.setString(6, if (builder.description == null)
                    String.format("%s permission", builder.name)
                else
                    builder.description)
                ps.setString(7, builder.source)
                ps.setBoolean(8, immutable)
                ps
            })
        } catch (e: DuplicateKeyException) {
            throw DuplicateKeyException("The permission " + builder.name + " already exists")
        }

        return get(id)
    }

    override fun update(permission: Permission): Permission {
        val authority = arrayOf(permission.type, permission.name).joinToString(Permission.JOIN)
        jdbc.update("UPDATE permission SET str_type=?, str_name=?,str_description=?,str_authority=? WHERE pk_permission=? AND bool_immutable=?",
                permission.type, permission.name, permission.description, authority, permission.id, false)
        return get(permission.id)
    }

    override fun renameUserPermission(user: User, newName: String): Boolean {
        return jdbc.update("UPDATE permission SET str_name=?, str_authority=? WHERE pk_permission=?",
                newName, "user::$newName", user.permissionId) == 1
    }

    override operator fun get(id: UUID): Permission {
        return jdbc.queryForObject("SELECT * FROM permission WHERE pk_permission=?", MAPPER, id)
    }

    override fun getId(name: String): UUID {
        if (UUID_REGEXP.matches(name)) {
            return UUID.fromString(name)
        }
        try {
            return jdbc.queryForObject("SELECT pk_permission FROM permission WHERE str_authority=?",
                    UUID::class.java, name)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to find permission $name", 1)
        }
    }

    override operator fun get(authority: String): Permission {
        val parts = authority.split(Permission.JOIN.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return jdbc.queryForObject("SELECT * FROM permission WHERE str_name=? AND str_type=?", MAPPER,
                parts[1], parts[0])
    }

    override fun resolveAcl(acl: Acl?, createMissing: Boolean): Acl {
        if (acl == null) {
            return Acl()
        }

        val resolved = mutableSetOf<UUID>()

        val result = Acl()
        for (entry in acl) {

            if (entry.getPermissionId() == null) {
                // Get the permission ID
                val id = try {
                    getId(entry.permission)

                } catch (e: EmptyResultDataAccessException) {
                    if (createMissing) {
                        create(PermissionSpec(entry.permission)
                                .apply { description = "Auto created permission" }, false).id
                    } else {
                        throw e
                    }
                }
                if (!resolved.contains(id)) {
                    result.addEntry(id, entry.getAccess())
                    resolved.add(id)
                }
            } else {
                if(!resolved.contains(entry.permissionId)) {
                    result.add(entry)
                    resolved.add(entry.permissionId)
                }
            }
        }
        return result
    }

    override fun getAll(): List<Permission> {
        return jdbc.query(GET_ALL, MAPPER)
    }

    override fun getPaged(page: Pager): PagedList<Permission> {
        return getPaged(page, PermissionFilter())
    }

    override fun getPaged(page: Pager, filter: DaoFilter): PagedList<Permission> {
        if (filter.sort.isEmpty()) {
            filter.sort = ImmutableMap.of("type", "asc", "name", "asc")
        }
        return PagedList(page.setTotalCount(count(filter)),
                jdbc.query(filter.getQuery(
                        GET_ALL, page),
                        MAPPER, *filter.getValues(page)))
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM permission", Long::class.java)
    }

    override fun count(filter: DaoFilter): Long {
        return jdbc.queryForObject(filter.getCountQuery(GET_ALL),
                Long::class.java, *filter.getValues())
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM permission WHERE str_authority=?",
                Int::class.java, name) == 1
    }

    override fun getAll(user: UserId): List<Permission> {
        return jdbc.query(GET_BY_USER, MAPPER, user.id)
    }

    override fun getAll(type: String): List<Permission> {
        return jdbc.query("SELECT * FROM permission WHERE str_type=?", MAPPER, type)
    }

    override fun get(type: String, name: String): Permission {
        return jdbc.queryForObject("SELECT * FROM permission WHERE str_name=? AND str_type=?", MAPPER, name, type)
    }

    override fun getAll(ids: Collection<UUID>?): List<Permission> {
        return if (ids == null || ids.isEmpty()) {
            Lists.newArrayListWithCapacity(1)
        } else jdbc.query("SELECT * FROM permission WHERE " + JdbcUtils.`in`("pk_permission", ids.size), MAPPER, *ids.toTypedArray())
    }

    override fun getAll(names: List<String>?): List<Permission> {
        return if (names == null || names.isEmpty()) {
            Lists.newArrayListWithCapacity(1)
        } else jdbc.query("SELECT * FROM permission WHERE " + JdbcUtils.`in`("str_authority", names.size), MAPPER, *names.toTypedArray())

    }

    override fun delete(perm: Permission): Boolean {
        /*
         * Ensure immutable permissions cannot be deleted.
         */
        return jdbc.update("DELETE FROM permission WHERE pk_permission=? AND bool_immutable=?",
                perm.id, false) == 1
    }

    companion object {

        private val INSERT = JdbcUtils.insert("permission",
                "pk_permission",
                "pk_organization",
                "str_name",
                "str_type",
                "str_authority",
                "str_description",
                "str_source",
                "bool_immutable")

        private val MAPPER = RowMapper<Permission> { rs, _ ->
            val p = Permission()
            p.id = rs.getObject("pk_permission") as UUID
            p.name = rs.getString("str_name")
            p.type = rs.getString("str_type")
            p.description = rs.getString("str_description")
            p.isImmutable = rs.getBoolean("bool_immutable")
            p
        }

        var GET_ALL = "SELECT * FROM permission "

        private val GET_BY_USER = "SELECT p.* " +
                "FROM " +
                "permission p, " +
                "user_permission m " +
                "WHERE " +
                "p.pk_permission=m.pk_permission " +
                "AND " +
                "m.pk_user=?"
    }
}
