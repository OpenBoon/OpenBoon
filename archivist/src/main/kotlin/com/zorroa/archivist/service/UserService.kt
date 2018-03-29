package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.repository.UserDao
import com.zorroa.archivist.repository.UserPresetDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.sdk.security.*
import com.zorroa.sdk.client.exception.DuplicateEntityException
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.dao.DataAccessException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * Created by chambers on 7/13/15.
 */
interface UserService {

    fun getAll(): List<User>

    fun getCount(): Long

    // Presets
    fun getUserPresets(): List<UserPreset>

    fun create(builder: UserSpec): User

    fun create(builder: UserSpec, source: String): User

    fun get(username: String): User

    fun get(id: UUID): User

    fun exists(username: String): Boolean

    fun getAll(page: Pager): PagedList<User>

    fun getPassword(username: String): String

    fun setPassword(user: User, password: String): Boolean

    fun getHmacKey(username: String): String

    fun generateHmacKey(username: String): String

    fun update(user: User, builder: UserProfileUpdate): Boolean

    fun delete(user: User): Boolean

    fun updateSettings(user: User, settings: UserSettings): Boolean

    fun setEnabled(user: User, value: Boolean): Boolean

    fun getPermissions(user: UserId): List<Permission>

    fun setPermissions(user: UserId, perms: Collection<Permission>, source:String)

    fun setPermissions(user: UserId, perms: Collection<Permission>)

    fun addPermissions(user: UserId, perms: Collection<Permission>)

    fun removePermissions(user: UserId, perms: Collection<Permission>)

    fun hasPermission(user: UserId, type: String, name: String): Boolean

    fun hasPermission(user: User, permission: Permission): Boolean

    fun getUserPreset(id: UUID): UserPreset

    fun updateUserPreset(id: UUID, preset: UserPreset): Boolean

    fun createUserPreset(preset: UserPresetSpec): UserPreset

    fun deleteUserPreset(preset: UserPreset): Boolean

    fun checkPassword(user: String, supplied: String)

    fun resetPassword(user: User, password: String)

    fun resetPassword(token: String, password: String): User?

    fun incrementLoginCounter(user: UserId)
}


@Service
class UserRegistryServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties
): UserRegistryService {

    @Autowired
    internal lateinit var userService: UserService

    @Autowired
    internal lateinit var permissionService: PermissionService

    /**
     * Register and external user from OAuth/SAML.
     */
    override fun registerUser(username: String, source: AuthSource, groups: List<String>?): UserAuthed {
        val user = if (!userService.exists(username)) {
            val spec = UserSpec()
            spec.username = username
            spec.password = UUID.randomUUID().toString() + UUID.randomUUID().toString()
            spec.email = username
            userService.create(spec, source.authSourceId)
        } else {
            userService.get(username)
        }

        if (properties.getBoolean("archivist.security.saml.permissions.import") &&
                groups != null) {
            importAndAssignPermissions(user, source, groups)
        }

        val perms = userService.getPermissions(user)
        return UserAuthed(user.id, user.username, perms.toSet())
    }

    @Transactional(readOnly = true)
    override fun getUser(username: String): UserAuthed {
        val user = userService.get(username)
        val perms = userService.getPermissions(user)
        return UserAuthed(user.id, user.username, perms.toSet())
    }

    fun importAndAssignPermissions(user: UserId, source: AuthSource, groups: List<String>) {

        val perms = mutableListOf<Permission>()
        for (group in groups) {
            val parts = group.split(Permission.JOIN, limit = 2)

            val spec = if (parts.size == 1) {
                PermissionSpec(source.permissionType, parts[0])
            } else {
                PermissionSpec(parts[0], parts[1])
            }
            spec.source = source.authSourceId

            val authority = spec.type + Permission.JOIN + spec.name
            perms.add(if (permissionService.permissionExists(authority)) {
                permissionService.getPermission(authority)
            } else {
                permissionService.createPermission(spec)
            })
        }

        userService.setPermissions(user, perms, source.authSourceId)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(UserRegistryServiceImpl::class.java)

    }
}

@Service
@Transactional
class UserServiceImpl @Autowired constructor(
        private val userDao: UserDao,
        private val permissionDao: PermissionDao,
        private val userPresetDao: UserPresetDao,
        private val tx: TransactionEventManager,
        private val properties: ApplicationProperties
): UserService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var logService: EventLogService

    private val PASS_MIN_LENGTH = 8

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        try {
            val key = userDao.getHmacKey("admin")
            if (key.isEmpty()) {
                logger.info("Regenerating admin key")
                userDao.generateHmacKey("admin")
            }
        }
        catch (e:Exception) {
            logger.warn("Failed to generate admin HMAC key: {}", e.message)
        }
    }

    override fun create(builder: UserSpec): User {
        return create(builder, SOURCE_LOCAL)
    }

    override fun create(builder: UserSpec, source: String): User {
        if (userDao.exists(builder.username)) {
            throw DuplicateEntityException("The user '" +
                    builder.username + "' already exists.")
        }

        val userPerm = permissionDao.create(
                PermissionSpec("user", builder.username), true)

        val userFolder = folderService.createUserFolder(
                builder.username, userPerm)

        builder.homeFolderId = userFolder.id
        builder.userPermissionId = userPerm.id

        val user = userDao.create(builder, source)

        /*
         * Grab the preset, if any.
         */
        var preset: UserPreset? = null
        if (builder.userPresetId != null) {
            preset = userPresetDao.get(builder.userPresetId)
            userDao.setSettings(user, preset.settings)
        }

        /*
         * Get the permissions specified with the builder and add our
         * user permission to the list.q
         */
        val perms = Sets.newHashSet(permissionDao.getAll(builder.permissionIds))
        if (preset != null && preset.permissionIds != null) {
            perms.addAll(permissionDao.getAll(preset.permissionIds.toTypedArray()))
        }

        if (!perms.isEmpty()) {
            setPermissions(user, perms)
        }

        userDao.addPermission(user, userPerm, true)
        userDao.addPermission(user, permissionDao.get(Groups.EVERYONE), true)

        tx.afterCommit(false, { logService.logAsync(UserLogSpec.build(LogAction.Create, user)) })

        return userDao.get(user.id)
    }

    override fun get(username: String): User {
        return userDao.get(username)
    }

    override fun get(id: UUID): User {
        return userDao.get(id)
    }

    override fun exists(username: String): Boolean {
        return userDao.exists(username)
    }

    override fun getAll(): List<User> {
        return userDao.getAll()
    }

    override fun getAll(page: Pager): PagedList<User> {
        return userDao.getAll(page)
    }

    override fun getCount(): Long {
        return userDao.getCount()
    }

    override fun setPassword(user: User, password: String): Boolean {
        return userDao.setPassword(user, password)
    }

    override fun getPassword(username: String): String {
        try {
            return userDao.getPassword(username)
        } catch (e: DataAccessException) {
            throw BadCredentialsException("Invalid username or password")
        }
    }

    override fun getHmacKey(username: String): String {
        try {
            return userDao.getHmacKey(username)
        } catch (e: DataAccessException) {
            throw BadCredentialsException("Invalid username or password")
        }
    }

    override fun generateHmacKey(username: String): String {
        return if (userDao.generateHmacKey(username)) {
            userDao.getHmacKey(username)
        } else {
            throw BadCredentialsException("Invalid username or password")
        }
    }

    override fun update(user: User, form: UserProfileUpdate): Boolean {
        val result = userDao.update(user, form)
        if (result) {
            tx.afterCommit(true,
                    { logService.logAsync(UserLogSpec.build(LogAction.Update, user)) })
        }
        return result
    }

    override fun delete(user: User): Boolean {
        val result = userDao.delete(user)
        if (result) {
            try {
                permissionDao.delete(permissionDao.get(user.permissionId))
            } catch (e: Exception) {
                logger.warn("Failed to delete user permission for {}", user)
            }

            try {
                folderService.delete(folderService.get(user.homeFolderId))
            } catch (e: Exception) {
                logger.warn("Failed to delete home folder for {}", user)
            }

            tx.afterCommit(true,  { logService.logAsync(UserLogSpec.build(LogAction.Update, user)) })
        }
        return result
    }

    override fun updateSettings(user: User, settings: UserSettings): Boolean {
        val result = userDao.setSettings(user, settings)
        if (result) {
            tx.afterCommit(true, { logService.logAsync(UserLogSpec.build(LogAction.Update, user)) })
        }
        return result
    }

    override fun setEnabled(user: User, value: Boolean): Boolean {
        val result = userDao.setEnabled(user, value)

        if (result) {
            if (result) {
                tx.afterCommit(true, {
                    logService.logAsync(UserLogSpec.build(if (value) "enable" else "disable", user)) })
            }
        }

        return result
    }

    override fun incrementLoginCounter(user: UserId) {
        return userDao.incrementLoginCounter(user)
    }

    override fun getPermissions(user: UserId): List<Permission> {
        return permissionDao.getAll(user)
    }

    override fun setPermissions(user: UserId, perms: Collection<Permission>) {
        setPermissions(user, perms, "local")
    }

    override fun setPermissions(user: UserId, perms: Collection<Permission>, source:String) {
        /*
         * Don't let setPermissions set immutable permission types which can never
         * be added or removed via the external API.
         */
        val filtered = perms.stream().filter { p -> !PERMANENT_TYPES.contains(p.type) }.collect(Collectors.toList())
        userDao.setPermissions(user, filtered, source)

        tx.afterCommit(true, {
            logService.logAsync(UserLogSpec.build("set_permission", user)
                    .putToAttrs("perms", perms.stream().map { ps -> ps.name }.collect(Collectors.toList())))
        })
    }

    override fun addPermissions(user: UserId, perms: Collection<Permission>) {
        for (p in perms) {
            if (PERMANENT_TYPES.contains(p.type)) {
                continue
            }
            if (!userDao.hasPermission(user, p)) {
                userDao.addPermission(user, p, false)
            }
        }
        tx.afterCommit(false, {
            logService.logAsync(UserLogSpec.build("add_permission", user)
                    .putToAttrs("perms", perms.stream().map { ps -> ps.name }.collect(Collectors.toList())))
        })
    }

    override fun removePermissions(user: UserId, perms: Collection<Permission>) {
        /**
         * Check to see if the permissions we are
         */
        for (p in perms) {
            // Don't allow removal of user permission.
            if (PERMANENT_TYPES.contains(p.type)) {
                continue
            }
            userDao.removePermission(user, p)
        }
        tx.afterCommit(true, {
            logService.logAsync(UserLogSpec.build("remove_permission", user)
                    .putToAttrs("perms", perms.stream().map { ps -> ps.name }.collect(Collectors.toList())))
        })
    }

    override fun hasPermission(user: UserId, type: String, name: String): Boolean {
        return userDao.hasPermission(user, type, name)
    }

    override fun hasPermission(user: User, permission: Permission): Boolean {
        return userDao.hasPermission(user, permission)
    }

    override fun getUserPresets(): List<UserPreset> {
        return userPresetDao.getAll()
    }

    override fun getUserPreset(id: UUID): UserPreset {
        return userPresetDao.get(id)
    }

    override fun updateUserPreset(id: UUID, preset: UserPreset): Boolean {
        return userPresetDao.update(id, preset)
    }

    override fun createUserPreset(preset: UserPresetSpec): UserPreset {
        return userPresetDao.create(preset)
    }

    override fun deleteUserPreset(preset: UserPreset): Boolean {
        return userPresetDao.delete(preset.presetId)
    }

    override fun checkPassword(username: String, supplied: String) {
        val storedPassword = getPassword(username)
        if (!BCrypt.checkpw(supplied, storedPassword)) {
            throw BadCredentialsException("Invalid username or password: $username")
        }
    }

    override fun resetPassword(user: User, password: String) {
        val issues = validatePassword(password)
        if (!issues.isEmpty()) {
            throw IllegalArgumentException(issues.joinToString(","))
        }
        userDao.setPassword(user, password)
    }

    override fun resetPassword(token: String, password: String): User? {
        val issues = validatePassword(password)
        if (!issues.isEmpty()) {
            throw IllegalArgumentException(issues.joinToString(","))
        }

        val user = userDao.getByToken(token)
        return if (userDao.resetPassword(user, token, password)) {
            user
        } else null
    }

    fun validatePassword(password: String?): List<String> {
        val issues = Lists.newArrayList<String>()
        if (password == null) {
            return ImmutableList.of("Password cannot be null")
        }

        val minLength = properties.getInt("archivist.security.password.minLength")

        if (password.length < minLength) {
            issues.add("The password must be $PASS_MIN_LENGTH or more characters.")
        }

        if (properties.getBoolean("archivist.security.password.requireStrong")) {

            if (!PASS_HAS_NUMBER.matcher(password).find()) {
                issues.add("The password must contain at least 1 number.")
            }

            if (!PASS_HAS_UPPER.matcher(password).find()) {
                issues.add("The password must contain at least 1 upper case character.")
            }
        }

        return issues
    }

    companion object {

        private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

        internal val PERMANENT_TYPES: Set<String> = ImmutableSet.of("user", "internal")

        private const val SOURCE_LOCAL = "local"

        private val PASS_HAS_NUMBER = Pattern.compile("\\d")
        private val PASS_HAS_UPPER = Pattern.compile("[A-Z]")
    }
}

