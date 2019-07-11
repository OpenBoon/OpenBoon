package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.google.common.hash.Hashing
import com.zorroa.archivist.domain.ApiKey
import com.zorroa.archivist.domain.ApiKeySpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.RegisteredUserUpdateSpec
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.domain.UserFilter
import com.zorroa.archivist.domain.UserProfileUpdate
import com.zorroa.archivist.domain.UserSettings
import com.zorroa.archivist.domain.UserSource
import com.zorroa.archivist.domain.UserSpec
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.archivist.security.createPasswordHash
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.event
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface UserDao {

    fun getAll(): List<User>

    fun getCount(): Long

    fun get(id: UUID): User

    fun get(username: String): User

    fun getByToken(token: String): User

    fun getApiKey(spec: ApiKeySpec): ApiKey

    fun getHmacKey(id: UUID): String

    fun generateAdminKey(): Boolean

    fun delete(user: User): Boolean

    fun getPassword(username: String): String

    fun setSettings(user: User, settings: UserSettings): Boolean

    fun getSettings(id: UUID): UserSettings

    fun setPassword(user: User, password: String): Boolean

    fun exists(name: String, source: String?): Boolean

    fun setEnablePasswordRecovery(user: User): String

    fun resetPassword(user: User, token: String, password: String): Boolean

    fun setEnabled(user: User, value: Boolean): Boolean

    fun update(user: User, update: UserProfileUpdate): Boolean

    fun update(user: User, spec: RegisteredUserUpdateSpec): User

    fun setLanguage(user: User, language: String): Boolean

    fun getAll(paging: Pager): PagedList<User>

    fun create(builder: UserSpec): User

    fun hasPermission(user: UserId, permission: Permission): Boolean

    fun hasPermission(user: UserId, type: String, name: String): Boolean

    fun setPermissions(user: UserId, perms: Collection<Permission>, source: String = "local"): Int

    fun addPermission(user: UserId, perm: Permission, immutable: Boolean): Boolean

    fun removePermission(user: UserId, perm: Permission): Boolean

    fun incrementLoginCounter(user: UserId)

    fun count(filter: UserFilter): Long

    fun getAll(filter: UserFilter): KPagedList<User>

    fun findOne(filter: UserFilter): User

    fun setAuthAttrs(user: User, attrs: Map<String, String>?): Boolean
}

@Repository
class UserDaoImpl : AbstractDao(), UserDao {

    private val hashFunc = Hashing.sha256()

    private fun generateKey(): String {
        return hashFunc.newHasher()
                .putString(UUID.randomUUID().toString(), Charsets.UTF_8)
                .putLong(System.nanoTime())
                .hash().toString()
    }

    override fun get(id: UUID): User {
        return jdbc.queryForObject<User>("SELECT * FROM users WHERE pk_user=?",
                MAPPER, id)
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
            throw EmptyResultDataAccessException("The password change token has expired, request a new password reset.", 1)
        }
    }

    override fun getAll(): List<User> {
        return jdbc.query("$GET WHERE pk_organization=? AND str_source!='internal' " +
                "ORDER BY str_username", MAPPER, getOrgId())
    }

    override fun getAll(paging: Pager): PagedList<User> {
        return PagedList(paging.setTotalCount(getCount()),
                jdbc.query<User>("$GET WHERE pk_organization=? AND str_source!='internal' " +
                        "ORDER BY str_username LIMIT ? OFFSET ?",
                        MAPPER, getOrgId(), paging.size, paging.from))
    }

    override fun findOne(filter: UserFilter): User {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("User not found") {
            KPagedList(1L, KPage(0, 1), jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun getAll(filter: UserFilter): KPagedList<User> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun count(filter: UserFilter): Long {
        return jdbc.queryForObject(filter.getQuery(COUNT, forCount = true),
                Long::class.java, *filter.getValues(forCount = true))
    }

    override fun create(spec: UserSpec): User {
        Preconditions.checkNotNull(spec.username, "The Username cannot be null")
        Preconditions.checkNotNull(spec.password, "The Password cannot be null")

        if (spec.source == null) {
            spec.source = UserSource.LOCAL
        }

        val id = spec.id ?: uuid1.generate()
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.username)
            ps.setString(3, spec.hashedPassword())
            ps.setString(4, spec.email)
            ps.setString(5, spec.firstName)
            ps.setString(6, spec.lastName)
            ps.setBoolean(7, true)
            ps.setObject(8, generateKey())
            ps.setString(9, "{}")
            ps.setString(10, spec.source)
            ps.setObject(11, spec.userPermissionId)
            ps.setObject(12, spec.homeFolderId)
            ps.setObject(13, user.organizationId)
            ps.setString(14, Json.serializeToString(spec.authAttrs, "{}"))
            ps.setString(15, spec.language)
            ps
        }

        logger.event(
            LogObject.USER, LogAction.CREATE,
                mapOf("createdUser" to spec.username,
                        "createdOrgId" to user.organizationId))
        return get(id)
    }

    override fun exists(name: String, source: String?): Boolean {
        var append = ""
        var args = mutableListOf<Any>(name, name)

        if (source != null) {
            append = " AND str_source=?"
            args.add(source)
        }
        return jdbc.queryForObject("SELECT COUNT(1) FROM users " +
                "WHERE (str_username=? OR str_email=?)$append",
                Boolean::class.java, *args.toTypedArray())
    }

    override fun setSettings(user: User, settings: UserSettings): Boolean {
        return jdbc.update(
                "UPDATE users SET json_settings=? WHERE pk_organization=? AND pk_user=?",
                Json.serializeToString(settings, "{}"), getOrgId(), user.id) == 1
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
        return jdbc.update(UPDATE, update.username, update.email, update.firstName,
                update.lastName, user.id) == 1
    }

    override fun update(user: User, spec: RegisteredUserUpdateSpec): User {
        val jsonAttrs = Json.serializeToString(spec.authAttrs, "{}")

        jdbc.update(
            "UPDATE users " +
                "SET str_email=?, str_firstname=?, str_lastname=?, str_language=?, json_auth_attrs=? WHERE pk_user=?",
            spec.email,
            spec.firstName,
            spec.lastName,
            spec.language,
            jsonAttrs,
            user.id
        )
        return get(user.id)
    }

    override fun setLanguage(user: User, language: String): Boolean {
        return jdbc.update(
            "UPDATE users SET str_language=? WHERE pk_user=? AND str_language is distinct from ?",
            language, user.id, language
        ) == 1
    }

    override fun setAuthAttrs(user: User, attrs: Map<String, String>?): Boolean {
        val jsonAttrs = Json.serializeToString(attrs, "{}")
        return jdbc.update(
            "UPDATE users SET json_auth_attrs=? WHERE pk_user=? AND json_auth_attrs is distinct from ?",
            jsonAttrs, user.id, jsonAttrs
        ) == 1
    }

    override fun incrementLoginCounter(user: UserId) {
        jdbc.update("UPDATE users SET int_login_count=int_login_count+1, time_last_login=? WHERE pk_user=?",
                System.currentTimeMillis(), user.id)
    }

    override fun delete(user: User): Boolean {
        val result = jdbc.update("DELETE FROM users WHERE pk_organization=? AND pk_user=?",
                getOrgId(), user.id) == 1
        logger.event(LogObject.USER, LogAction.DELETE, mapOf("userName" to user.username, "result" to result))
        return result
    }

    override fun getPassword(username: String): String {
        return jdbc.queryForObject("SELECT str_password FROM users WHERE (str_username=? OR str_email=?) AND bool_enabled=? AND str_source='local'",
                String::class.java, username, username, true)
    }

    override fun getApiKey(spec: ApiKeySpec): ApiKey {
        val hmacKey = if (spec.replace) {
            val key = generateKey()
            if (jdbc.update("UPDATE users SET hmac_key=? WHERE pk_user=? AND bool_enabled=?", key, spec.userId, true) != 1) {
                throw EmptyResultDataAccessException("Unknown user", 1)
            }
            key
        } else {
            getHmacKey(spec.userId)
        }

        logger.event(LogObject.USER, LogAction.APIKEY, emptyMap())
        return ApiKey(spec.userId, spec.user, hmacKey, spec.server)
    }

    override fun getHmacKey(id: UUID): String {
        return jdbc.queryForObject("SELECT hmac_key FROM users WHERE pk_user=? AND bool_enabled=?",
                String::class.java, id, true)
    }

    override fun generateAdminKey(): Boolean {
        val key = generateKey()
        return jdbc.update("UPDATE users SET hmac_key=? WHERE str_username='admin' AND hmac_key IS NULL", key) == 1
    }

    override fun getCount(): Long {
        return jdbc.queryForObject("$COUNT WHERE pk_organization=?", Int::class.java, getOrgId()).toLong()
    }

    override fun hasPermission(user: UserId, permission: Permission): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM user_permission m WHERE m.pk_user=? AND m.pk_permission=?",
                Int::class.java, user.id, permission.id) == 1
    }

    override fun hasPermission(user: UserId, type: String, name: String): Boolean {
        return jdbc.queryForObject(HAS_PERM, Int::class.java, user.id, name, type) == 1
    }

    private fun clearPermissions(user: UserId, source: String = "local"): Int {
        /*
         * Ensure the user's immutable permissions cannot be removed.
         */
        return jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND bool_immutable=? AND str_source=?",
                user.id, false, source)
    }

    override fun setPermissions(user: UserId, perms: Collection<Permission>, source: String): Int {

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

        private val MAPPER = RowMapper { rs, _ ->
            User(rs.getObject("pk_user") as UUID,
                    rs.getString("str_username"),
                    rs.getString("str_email"),
                    rs.getString("str_source"),
                    rs.getObject("pk_permission") as UUID?,
                    rs.getObject("pk_folder") as UUID?,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_firstname"),
                    rs.getString("str_lastname"),
                    rs.getBoolean("bool_enabled"),
                    Json.deserialize(rs.getString("json_settings"), UserSettings::class.java),
                    rs.getInt("int_login_count"),
                    rs.getLong("time_last_login"),
                    Json.deserialize(rs.getString("json_auth_attrs"), Json.GENERIC_MAP),
                    rs.getString("str_language"))
        }

        private const val GET = "SELECT * FROM users"

        private const val COUNT = "SELECT COUNT(1) FROM users"

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
                "pk_folder",
                "pk_organization",
                "json_auth_attrs",
                "str_language")

        private const val RESET_PASSWORD = "UPDATE " +
                "users " +
                "SET " +
                "str_password=?," +
                "str_reset_pass_token=null " +
                "WHERE " +
                "pk_user=? " +
                "AND " +
                "str_reset_pass_token=?"

        private val UPDATE = JdbcUtils.update("users", "pk_user",
                "str_username",
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
