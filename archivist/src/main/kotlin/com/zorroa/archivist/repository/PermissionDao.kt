package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface PermissionDao {

    fun getAll(): List<Permission>

    fun create(builder: PermissionSpec, immutable: Boolean): Permission

    fun update(permission: Permission): Permission

    fun updateUserPermission(oldName: String, newName: String): Boolean

    fun get(id: Int): Permission

    fun getId(name: String): Int

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

    fun getAll(ids: Array<Int>?): List<Permission>

    fun getAll(names: List<String>?): List<Permission>

    fun delete(perm: Permission): Boolean
}

@Repository
open class PermissionDaoImpl : AbstractDao(), PermissionDao {

    override fun create(builder: PermissionSpec, immutable: Boolean): Permission {

        val keyHolder = GeneratedKeyHolder()
        try {
            jdbc.update({ connection ->
                val ps = connection.prepareStatement(INSERT, arrayOf("pk_permission"))
                ps.setString(1, builder.name)
                ps.setString(2, builder.type)
                ps.setString(3, if (builder.description == null)
                    String.format("%s permission", builder.name)
                else
                    builder.description)
                ps.setBoolean(4, immutable)
                ps.setString(5, builder.type + "::" + builder.name)
                ps
            }, keyHolder)
        } catch (e: DuplicateKeyException) {
            throw DuplicateKeyException("The permission " + builder.name + " already exists")
        }

        val id = keyHolder.key.toInt()
        return get(id)
    }

    override fun update(permission: Permission): Permission {
        val authority = arrayOf(permission.type, permission.name).joinToString(Permission.JOIN)
        jdbc.update("UPDATE permission SET str_type=?, str_name=?,str_description=?,str_authority=? WHERE pk_permission=? AND bool_immutable=?",
                permission.type, permission.name, permission.description, authority, permission.id, false)
        return get(permission.id)
    }

    override fun updateUserPermission(oldName: String, newName: String): Boolean {
        return jdbc.update("UPDATE permission SET str_name=? WHERE str_type='user' AND str_name=? AND bool_immutable=?",
                newName, oldName, true) == 1
    }

    override operator fun get(id: Int): Permission {
        return jdbc.queryForObject("SELECT * FROM permission WHERE pk_permission=?", MAPPER, id)
    }

    override fun getId(name: String): Int {
        try {
            return jdbc.queryForObject("SELECT pk_permission FROM permission WHERE str_authority=?",
                    Int::class.java, name)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to find permission " + name, 1)
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

        val result = Acl()
        for (entry in acl) {
            if (entry.getPermissionId() == null) {
                try {
                    result.addEntry(getId(entry.permission), entry.getAccess())
                } catch (e: EmptyResultDataAccessException) {
                    if (createMissing) {
                        result.addEntry(create(PermissionSpec(entry.permission)
                                .apply { description="Auto created permission" }, false), entry.getAccess())
                    } else {
                        throw e
                    }
                }

            } else {
                result.add(entry)
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

    override fun getAll(ids: Array<Int>?): List<Permission> {
        return if (ids == null || ids.size == 0) {
            Lists.newArrayListWithCapacity(1)
        } else jdbc.query("SELECT * FROM permission WHERE " + JdbcUtils.`in`("pk_permission", ids.size), MAPPER, *ids)
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
                "str_name", "str_type", "str_description", "bool_immutable", "str_authority")

        private val MAPPER = RowMapper<Permission> { rs, _ ->
            val p = InternalPermission()
            p.id = rs.getInt("pk_permission")
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
