package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.google.common.hash.Hashing
import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.archivist.security.createPasswordHash
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface UserDao {

    fun getAll(): List<User>

    fun getCount(): Long

    fun get(id: UUID): User

    fun get(username: String): User

    fun getByToken(token: String): User

    fun getHmacKey(username: String): String

    fun generateHmacKey(username: String): Boolean

    fun delete(user: User): Boolean

    fun getPassword(username: String): String

    fun setSettings(user: User, settings: UserSettings): Boolean

    fun getSettings(id: UUID): UserSettings

    fun setPassword(user: User, password: String): Boolean

    fun exists(name: String): Boolean

    fun setEnablePasswordRecovery(user: User): String

    fun resetPassword(user: User, token: String, password: String): Boolean

    fun setEnabled(user: User, value: Boolean): Boolean

    fun update(user: User, update: UserProfileUpdate): Boolean

    fun getAll(paging: Pager): PagedList<User>

    fun create(builder: UserSpec): User

    fun hasPermission(user: UserId, permission: Permission): Boolean

    fun hasPermission(user: UserId, type: String, name: String): Boolean

    fun setPermissions(user: UserId, perms: Collection<Permission>,source:String="local") : Int

    fun addPermission(user: UserId, perm: Permission, immutable: Boolean): Boolean

    fun removePermission(user: UserId, perm: Permission): Boolean

    fun incrementLoginCounter(user: UserId)
}

@Repository
class UserDaoImpl : AbstractDao(), UserDao {

    @Value("\${archivist.organization.domain}")
    private lateinit var domain: String

    private val hashFunc = Hashing.sha256()

    private fun generateKey() : String {
        return hashFunc.newHasher()
                .putString(UUID.randomUUID().toString(), Charsets.UTF_8)
                .putLong(System.nanoTime())
                .hash().toString()
    }

    override fun get(id: UUID): User {
        return jdbc.queryForObject<User>("SELECT * FROM users WHERE pk_user=?", MAPPER, id)
    }

    override fun get(username: String): User {
        return jdbc.queryForObject<User>("SELECT * FROM users WHERE (str_username=? OR str_email=?)",
                MAPPER, username, username)
    }

    override fun getByToken(token: String): User {
        val expireTime = (30 * 60 * 1000).toLong()
        try {
            return jdbc.queryForObject<User>(
                    "SELECT * FROM users WHERE str_reset_pass_token=? AND " + "? - time_reset_pass < ? LIMIT 1 ",
                    MAPPER, token, System.currentTimeMillis(), expireTime)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("This password change token has expired.", 1)
        }

    }

    override fun getAll(): List<User> {
        return jdbc.query(GET_ALL, MAPPER)
    }

    override fun getAll(paging: Pager): PagedList<User> {
        return PagedList(paging.setTotalCount(getCount()),
                jdbc.query<User>("$GET_ALL LIMIT ? OFFSET ?",
                        MAPPER, paging.size, paging.from))
    }

    override fun create(builder: UserSpec): User {
        Preconditions.checkNotNull(builder.username, "The Username cannot be null")
        Preconditions.checkNotNull(builder.password, "The Password cannot be null")
        builder.password = createPasswordHash(builder.password)

        if (builder.source == null) {
            builder.source = SOURCE_LOCAL
        }

        val id = uuid1.generate()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, builder.username)
            ps.setString(3, builder.password)
            ps.setString(4, builder.email)
            ps.setString(5, builder.firstName)
            ps.setString(6, builder.lastName)
            ps.setBoolean(7, true)
            ps.setObject(8, generateKey())
            ps.setString(9, "{}")
            ps.setString(10, builder.source)
            ps.setObject(11, builder.userPermissionId)
            ps.setObject(12, builder.homeFolderId)
            ps
        })
        return get(id)
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM users WHERE str_username=? OR str_email=?",
                Boolean::class.java, name, name)
    }

    override fun setSettings(user: User, settings: UserSettings): Boolean {
        return jdbc.update(
                "UPDATE users SET json_settings=? WHERE pk_user=?",
                Json.serializeToString(settings, "{}"), user.id) == 1
    }

    override fun getSettings(id: UUID): UserSettings {
        return Json.deserialize(
                jdbc.queryForObject("SELECT json_settings FROM users WHERE pk_user=?",
                        String::class.java, id), UserSettings::class.java)
    }

    override fun setPassword(user: User, password: String): Boolean {
        return jdbc.update(
                "UPDATE users SET str_password=? WHERE pk_user=?",
                createPasswordHash(password), user.id) == 1
    }

    override fun setEnablePasswordRecovery(user: User): String {
        val token = HttpUtils.randomString(64)
        jdbc.update(
                "UPDATE users SET str_reset_pass_token=?, time_reset_pass=? WHERE pk_user=?",
                token, System.currentTimeMillis(), user.id)
        return token
    }

    override fun resetPassword(user: User, token: String, password: String): Boolean {
        return jdbc.update(RESET_PASSWORD, createPasswordHash(password),
                user.id, token) == 1
    }

    override fun setEnabled(user: User, value: Boolean): Boolean {
        return jdbc.update(
                "UPDATE users SET bool_enabled=? WHERE pk_user=? AND bool_enabled=?",
                value, user.id, !value) == 1
    }

    override fun update(user: User, update: UserProfileUpdate): Boolean {
        return jdbc.update(UPDATE, update.email, update.firstName,
                update.lastName, user.id) == 1
    }

    override fun incrementLoginCounter(user: UserId) {
        jdbc.update("UPDATE users SET int_login_count=int_login_count+1, time_last_login=? WHERE pk_user=?",
                System.currentTimeMillis(), user.id)
    }

    override fun delete(user: User): Boolean {
        return jdbc.update("DELETE FROM users WHERE pk_user=?", user.id) == 1
    }

    override fun getPassword(username: String): String {
        return jdbc.queryForObject("SELECT str_password FROM users WHERE (str_username=? OR str_email=?) AND bool_enabled=? AND str_source='local'",
                String::class.java, username, username, true)
    }

    override fun getHmacKey(username: String): String {
        val result =  jdbc.queryForObject("SELECT hmac_key FROM users WHERE (str_username=? OR str_email=?) AND bool_enabled=?",
                String::class.java, username, username, true)
        if (result == null) {
            return ""
        }
        else {
            return result
        }
    }

    override fun generateHmacKey(username: String): Boolean {
        return jdbc.update("UPDATE users SET hmac_key=? WHERE (str_username=? OR str_email=?) AND bool_enabled=?",
                generateKey(), username, username, true) == 1
    }

    override fun getCount(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM users", Int::class.java).toLong()
    }


    override fun hasPermission(user: UserId, permission: Permission): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM user_permission m WHERE m.pk_user=? AND m.pk_permission=?",
                Int::class.java, user.id, permission.id) == 1
    }

    override fun hasPermission(user: UserId, type: String, name: String): Boolean {
        return jdbc.queryForObject(HAS_PERM, Int::class.java, user.id, name, type) == 1
    }

    private fun clearPermissions(user: UserId, source:String="local") : Int {
        /*
         * Ensure the user's immutable permissions cannot be removed.
         */
        return jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND bool_immutable=? AND str_source=?",
                user.id, false, source)
    }

    override fun setPermissions(user: UserId, perms: Collection<Permission>, source:String): Int {

        clearPermissions(user, source)

        var result = 0
        for (p in perms) {
            if (hasPermission(user, p)) {
                continue
            }
            jdbc.update("INSERT INTO user_permission (pk_permission, pk_user, str_source) VALUES (?,?,?)",
                    p.id, user.id, source)
            result++
        }
        return result
    }

    override fun addPermission(user: UserId, perm: Permission, immutable: Boolean): Boolean {
        return if (hasPermission(user, perm)) {
            false
        } else jdbc.update("INSERT INTO user_permission (pk_permission, pk_user, bool_immutable) VALUES (?,?,?)",
                perm.id, user.id, immutable) == 1
    }

    override fun removePermission(user: UserId, perm: Permission): Boolean {
        return jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND pk_permission=? AND bool_immutable=0",
                user.id, perm.id) == 1
    }

    companion object {

        const val SOURCE_LOCAL = "local"

        private val MAPPER = RowMapper<User> { rs, _ ->
            User(rs.getObject("pk_user") as UUID,
                    rs.getString("str_username"),
                    rs.getString("str_email"),
                    rs.getObject("pk_permission") as UUID,
                    rs.getObject("pk_folder") as UUID,
                    rs.getString("str_firstname"),
                    rs.getString("str_lastname"),
                    rs.getBoolean("bool_enabled"),
                    Json.deserialize<UserSettings>(rs.getString("json_settings"), UserSettings::class.java),
                    rs.getInt("int_login_count"),
                    rs.getLong("time_last_login"))
        }

        private const val GET_ALL = "SELECT * FROM users ORDER BY str_username"

        private val INSERT = JdbcUtils.insert("users",
                "pk_user",
                "str_username",
                "str_password",
                "str_email",
                "str_firstname",
                "str_lastname",
                "bool_enabled",
                "hmac_key",
                "json_settings",
                "str_source",
                "pk_permission",
                "pk_folder")

        private val RESET_PASSWORD = "UPDATE " +
                "users " +
                "SET " +
                "str_password=?," +
                "str_reset_pass_token=null " +
                "WHERE " +
                "pk_user=? " +
                "AND " +
                "str_reset_pass_token=?"

        private val UPDATE = JdbcUtils.update("users", "pk_user",
                "str_email",
                "str_firstname",
                "str_lastname")

        private const val HAS_PERM = "SELECT " +
                "COUNT(1) " +
                "FROM " +
                "permission p," +
                "user_permission up " +
                "WHERE " +
                "p.pk_permission = up.pk_permission " +
                "AND " +
                "up.pk_user = ? " +
                "AND " +
                "p.str_name = ? " +
                "AND " +
                "p.str_type = ?"
    }
}
