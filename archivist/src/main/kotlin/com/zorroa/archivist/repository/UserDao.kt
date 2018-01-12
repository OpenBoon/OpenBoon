package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.util.*

interface UserDao {

    fun getAll(): List<User>

    fun getCount(): Long

    fun get(id: Int): User

    fun get(username: String): User

    fun getByEmail(email: String): User

    fun getByToken(token: String): User

    fun getHmacKey(username: String): String

    fun generateHmacKey(username: String): Boolean

    fun delete(user: User): Boolean

    fun getPassword(username: String): String

    fun setSettings(user: User, settings: UserSettings): Boolean

    fun getSettings(id: Int): UserSettings

    fun setPassword(user: User, password: String): Boolean

    fun exists(name: String): Boolean

    fun setEnablePasswordRecovery(user: User): String

    fun resetPassword(user: User, token: String, password: String): Boolean

    fun setEnabled(user: User, value: Boolean): Boolean

    fun update(user: User, update: UserProfileUpdate): Boolean

    fun getAll(paging: Pager): PagedList<User>

    fun create(builder: UserSpec): User

    fun create(builder: UserSpec, source: String): User

    fun hasPermission(user: User, permission: Permission): Boolean

    fun hasPermission(user: User, type: String, name: String): Boolean

    fun setPermissions(user: User, perms: Collection<Permission>): Int

    fun addPermission(user: User, perm: Permission, immutable: Boolean): Boolean

    fun removePermission(user: User, perm: Permission): Boolean
}

@Repository
open class UserDaoImpl : AbstractDao(), UserDao {

    override fun get(id: Int): User {
        return jdbc.queryForObject<User>("SELECT * FROM users WHERE pk_user=?", MAPPER, id)
    }

    override fun get(username: String): User {
        return jdbc.queryForObject<User>("SELECT * FROM users WHERE str_username=? OR str_email=?",
                MAPPER, username, username)
    }

    override fun getByEmail(email: String): User {
        return jdbc.queryForObject<User>("SELECT * FROM users WHERE str_email=?", MAPPER, email)
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

    override fun getAll(page: Pager): PagedList<User> {
        return PagedList(page.setTotalCount(getCount()),
                jdbc.query<User>(GET_ALL + " LIMIT ? OFFSET ?",
                        MAPPER, page.size, page.from))
    }

    override fun create(builder: UserSpec, source: String): User {
        Preconditions.checkNotNull(builder.username, "The Username cannot be null")
        Preconditions.checkNotNull(builder.password, "The Password cannot be null")
        builder.password = SecurityUtils.createPasswordHash(builder.password)

        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_user"))
            ps.setString(1, builder.username)
            ps.setString(2, builder.password)
            ps.setString(3, builder.email)
            ps.setString(4, builder.firstName)
            ps.setString(5, builder.lastName)
            ps.setBoolean(6, true)
            ps.setObject(7, UUID.randomUUID())
            ps.setString(8, "{}")
            ps.setString(9, source)
            ps.setInt(10, builder.userPermissionId)
            ps.setInt(11, builder.homeFolderId)
            ps
        }, keyHolder)
        val id = keyHolder.key.toInt()
        return get(id)
    }

    override fun create(builder: UserSpec): User {
        return create(builder, "local")
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM users WHERE str_username=?", Boolean::class.java, name)
    }

    override fun setSettings(user: User, settings: UserSettings): Boolean {
        return jdbc.update(
                "UPDATE users SET json_settings=? WHERE pk_user=?",
                Json.serializeToString(settings, "{}"), user.id) == 1
    }

    override fun getSettings(id: Int): UserSettings {
        return Json.deserialize(
                jdbc.queryForObject("SELECT json_settings FROM users WHERE pk_user=?",
                        String::class.java, id), UserSettings::class.java)
    }

    override fun setPassword(user: User, password: String): Boolean {
        return jdbc.update(
                "UPDATE users SET str_password=? WHERE pk_user=?",
                SecurityUtils.createPasswordHash(password), user.id) == 1
    }

    override fun setEnablePasswordRecovery(user: User): String {
        val token = HttpUtils.randomString(64)
        jdbc.update(
                "UPDATE users SET str_reset_pass_token=?, time_reset_pass=? WHERE pk_user=?",
                token, System.currentTimeMillis(), user.id)
        return token
    }

    override fun resetPassword(user: User, token: String, password: String): Boolean {
        return jdbc.update(RESET_PASSWORD, SecurityUtils.createPasswordHash(password),
                user.id, token) == 1
    }

    override fun setEnabled(user: User, value: Boolean): Boolean {
        return jdbc.update(
                "UPDATE users SET bool_enabled=? WHERE pk_user=? AND bool_enabled=?",
                value, user.id, !value) == 1
    }

    override fun update(user: User, builder: UserProfileUpdate): Boolean {
        return jdbc.update(UPDATE, builder.email, builder.firstName,
                builder.lastName, user.id) == 1
    }

    override fun delete(user: User): Boolean {
        return jdbc.update("DELETE FROM users WHERE pk_user=?", user.id) == 1
    }

    override fun getPassword(username: String): String {
        return jdbc.queryForObject("SELECT str_password FROM users WHERE (str_username=? OR str_email=?) AND bool_enabled=? AND str_source='local'",
                String::class.java, username, username, true)
    }

    override fun getHmacKey(username: String): String {
        return jdbc.queryForObject("SELECT hmac_key FROM users WHERE str_username=? AND bool_enabled=?",
                String::class.java, username, true)
    }

    override fun generateHmacKey(username: String): Boolean {
        val key = UUID.randomUUID()
        return jdbc.update("UPDATE users SET hmac_key=? WHERE str_username=? AND bool_enabled=?",
                key, username, true) == 1
    }

    override fun getCount(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM users", Int::class.java).toLong()
    }


    override fun hasPermission(user: User, permission: Permission): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM user_permission m WHERE m.pk_user=? AND m.pk_permission=?",
                Int::class.java, user.id, permission.id) == 1
    }

    override fun hasPermission(user: User, type: String, name: String): Boolean {
        return jdbc.queryForObject(HAS_PERM, Int::class.java, user.id, name, type) == 1
    }

    private fun clearPermissions(user: User) {
        /*
         * Ensure the user's immutable permissions cannot be removed.
         */
        jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND bool_immutable=?",
                user.id, false)
    }

    override fun setPermissions(user: User, perms: Collection<Permission>): Int {
        /*
         * Does not remove immutable permissions.
         */
        clearPermissions(user)

        var result = 0
        for (p in perms) {
            if (hasPermission(user, p)) {
                continue
            }
            jdbc.update("INSERT INTO user_permission (pk_permission, pk_user) VALUES (?,?)",
                    p.id, user.id)
            result++
        }
        return result
    }

    override fun addPermission(user: User, perm: Permission, immutable: Boolean): Boolean {
        return if (hasPermission(user, perm)) {
            false
        } else jdbc.update("INSERT INTO user_permission (pk_permission, pk_user, bool_immutable) VALUES (?,?,?)",
                perm.id, user.id, immutable) == 1
    }

    override fun removePermission(user: User, perm: Permission): Boolean {
        return jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND pk_permission=? AND bool_immutable=0",
                user.id, perm.id) == 1
    }

    companion object {

        private val MAPPER = RowMapper<User> { rs, _ ->
            val user = User()
            user.id = rs.getInt("pk_user")
            user.username = rs.getString("str_username")
            user.email = rs.getString("str_email")
            user.firstName = rs.getString("str_firstname")
            user.lastName = rs.getString("str_lastname")
            user.enabled = rs.getBoolean("bool_enabled")
            user.settings = Json.deserialize<UserSettings>(rs.getString("json_settings"), UserSettings::class.java)
            user.permissionId = rs.getInt("pk_permission")
            user.homeFolderId = rs.getInt("pk_folder")
            user
        }

        private val GET_ALL = "SELECT * FROM users ORDER BY str_username"

        private val INSERT = JdbcUtils.insert("users",
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

        private val HAS_PERM = "SELECT " +
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
