package com.zorroa.archivist.repository

import com.google.common.collect.Lists
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.sdk.security.Groups
import com.zorroa.archivist.security.getPermissionIds
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.sdk.domain.Access
import com.zorroa.sdk.util.Json
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface BlobDao {

    fun create(app: String, feature: String, name: String, data: Any): Blob

    fun update(bid: BlobId, data: Any): Boolean

    fun delete(blob: BlobId): Boolean

    operator fun get(id: UUID): Blob

    operator fun get(app: String, feature: String, name: String): Blob

    fun getId(app: String, feature: String, name: String, forAccess: Access): BlobId

    fun getAll(app: String, feature: String): List<Blob>

    fun getPermissions(blob: BlobId): Acl

    fun setPermissions(blob: BlobId, req: SetPermissions): Acl
}

@Repository
class BlobDaoImpl : AbstractDao(), BlobDao {

    override fun create(app: String, feature: String, name: String, data: Any): Blob {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, app)
            ps.setString(3, feature)
            ps.setString(4, name)
            ps.setString(5, Json.serializeToString(data, "{}"))
            ps.setObject(6, getUserId())
            ps.setObject(7, getUserId())
            ps.setLong(8, time)
            ps.setLong(9, time)
            ps.setObject(10, getUser().organizationId)
            ps
        })

        return get(id)
    }

    override fun update(bid: BlobId, data: Any): Boolean {
        return jdbc.update(UPDATE,
                Json.serializeToString(data, "{}"), getUserId(), System.currentTimeMillis(),
                bid.getBlobId()) == 1
    }

    override fun delete(blob: BlobId): Boolean {
        return jdbc.update("DELETE FROM jblob WHERE pk_jblob=?", blob.getBlobId()) == 1
    }

    override fun get(id: UUID): Blob {
        return jdbc.queryForObject<Blob>(appendAccess(GET + "WHERE pk_jblob=?", Access.Read), MAPPER,
                *appendAccessArgs(id))
    }

    override fun get(app: String, feature: String, name: String): Blob {
        return jdbc.queryForObject<Blob>(appendAccess(
                GET + "WHERE str_app=? AND str_feature=? AND str_name=?", Access.Read), MAPPER,
                *appendAccessArgs(app, feature, name))
    }

    override fun getId(app: String, feature: String, name: String, forAccess: Access): BlobId {
        return jdbc.queryForObject<BlobId>(
                appendAccess("SELECT pk_jblob FROM jblob WHERE str_app=? AND str_feature=? AND str_name=?", forAccess),
                RowMapper<BlobId> { rs, _ ->
                    val blobId = rs.getObject(1) as UUID
                    object : BlobId {
                        override fun getBlobId(): UUID {
                            return blobId
                        }
                    }
                }, *appendAccessArgs(app, feature, name))
    }

    override fun getAll(app: String, feature: String): List<Blob> {
        return jdbc.query<Blob>(GET + "WHERE str_app=? AND str_feature=?", MAPPER,
                app, feature)
    }

    override fun getPermissions(blob: BlobId): Acl {
        val acl = Acl()
        jdbc.query(GET_PERMS,
                RowCallbackHandler {
                    rs -> acl.addEntry(rs.getObject("pk_permission") as UUID, rs.getInt("int_access"))
                }, blob.getBlobId())
        return acl
    }

    override fun setPermissions(blob: BlobId, req: SetPermissions): Acl {
        if (req.acl != null) {
            if (req.replace) {
                jdbc.update("DELETE FROM jblob_acl WHERE pk_jblob=?", blob.getBlobId())
                for (entry in req.acl!!) {
                    addPermission(blob, entry)
                }
            } else {
                for (entry in req.acl!!) {
                    if (entry.getAccess() > 7) {
                        throw IllegalArgumentException("Invalid Access level "
                                + entry.getAccess() + " for permission ID " + entry.getPermissionId())
                    }
                    if (entry.access <= 0) {
                        jdbc.update("DELETE FROM jblob_acl WHERE pk_permission=?", entry.permissionId)
                    } else {
                        if (jdbc.update("UPDATE jblob_acl SET int_access=? WHERE pk_jblob=? AND pk_permission=?",
                                entry.access, blob.getBlobId(), entry.getPermissionId()) != 1) {
                            addPermission(blob, entry)
                        }
                    }
                }
            }
        }

        return getPermissions(blob)
    }

    private fun addPermission(blob: BlobId, entry: AclEntry) {
        jdbc.update("INSERT INTO jblob_acl (pk_jblob, pk_permission, int_access) VALUES (?,?,?)",
                blob.getBlobId(), entry.permissionId, entry.access)
    }

    fun appendAccessArgs(vararg args: Any): Array<out Any> {
        if (hasPermission(Groups.ADMIN)) {
            return args
        }

        val result = Lists.newArrayListWithCapacity<Any>(args.size + getPermissionIds().size)
        for (a in args) {
            result.add(a)
        }
        result.add(getUserId())
        result.addAll(getPermissionIds())
        return result.toTypedArray()
    }

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
        sb.append("(jblob.pk_user_created = ? OR (")
        sb.append("SELECT COUNT(1) FROM jblob_acl WHERE jblob_acl.pk_jblob=jblob.pk_jblob AND ")
        sb.append(JdbcUtils.`in`("jblob_acl.pk_permission", getPermissionIds().size))
        sb.append(" AND BITAND(")
        sb.append(access.value)
        sb.append(",int_access) = " + access.value + ") > 0 OR (")
        sb.append("SELECT COUNT(1) FROM jblob_acl WHERE jblob_acl.pk_jblob=jblob.pk_jblob) = 0)")
        return sb.toString()
    }

    companion object {

        private val MAPPER = RowMapper<Blob> { rs, _ ->
            val blob = Blob(
                    rs.getObject("pk_jblob") as UUID,
                    rs.getLong("int_version"),
                    rs.getString("str_app"),
                    rs.getString("str_feature"),
                    rs.getString("str_name"),
                    Json.deserialize<Map<String, Any>>(rs.getString("json_data"), Json.GENERIC_MAP))
            blob
        }

        private val INSERT = JdbcUtils.insert("jblob",
                "pk_jblob",
                "str_app",
                "str_feature",
                "str_name",
                "json_data",
                "pk_user_created",
                "pk_user_modified",
                "time_created",
                "time_modified",
                "pk_organization")

        private val UPDATE = "UPDATE " +
                "jblob " +
                "SET " +
                "json_data=?," +
                "int_version=int_version+1," +
                "pk_user_modified=?, " +
                "time_modified=? " +
                "WHERE " +
                "pk_jblob=?"

        private val GET = "SELECT " +
                "pk_jblob," +
                "str_app," +
                "str_feature," +
                "str_name," +
                "json_data," +
                "int_version " +
                "FROM " +
                "jblob "

        private val GET_PERMS =
                "SELECT " +
                    "jblob_acl.pk_permission, " +
                    "jblob_acl.int_access, " +
                    "permission.str_name " +
                "FROM " +
                    "jblob_acl," +
                    "permission " +
                "WHERE " +
                    "permission.pk_permission = jblob_acl.pk_permission " +
                "AND " +
                    "pk_jblob=?"

    }
}
