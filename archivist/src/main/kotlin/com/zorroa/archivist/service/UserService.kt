package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.repository.UserDao
import com.zorroa.archivist.repository.UserDaoCache
import com.zorroa.archivist.sdk.security.AuthSource
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.archivist.security.generateRandomPassword
import com.zorroa.common.domain.DuplicateEntityException
import com.zorroa.common.repository.KPagedList
import com.zorroa.security.Groups
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.dao.DataAccessException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
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

    @Deprecated("see getAll(filter: UserFilter)")
    fun getAll(): List<User>

    fun getCount(): Long

    fun create(spec: UserSpec): User

    fun create(spec: LocalUserSpec): User

    fun get(username: String): User

    fun get(id: UUID): User

    fun exists(username: String, source:String?): Boolean

    @Deprecated("see getAll(filter: UserFilter)")
    fun getAll(page: Pager): PagedList<User>

    fun getPassword(username: String): String

    fun setPassword(user: User, password: String): Boolean

    fun getHmacKey(user: UserId): String

    fun getApiKey(spec: ApiKeySpec): ApiKey

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

    fun checkPassword(user: String, supplied: String)

    fun resetPassword(user: User, password: String)

    fun resetPassword(token: String, password: String): User?

    fun incrementLoginCounter(user: UserId)

    /**
     * Create the standard users for a new Organization.  The current thread must
     * be authed as an Organization super admin.
     *
     * @param: Organization - the organization to create the user for.
     */
    fun createStandardUsers(org: Organization)

    fun findOne(filter: UserFilter): User

    fun getAll(filter: UserFilter): KPagedList<User>
}


@Service
class UserRegistryServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties
): UserRegistryService {

    @Autowired
    internal lateinit var organizationService: OrganizationService

    @Autowired
    internal lateinit var userService: UserService

    @Autowired
    internal lateinit var permissionService: PermissionService

    @Value("\${archivist.organization.multiTenant}")
    var multiTentant: Boolean = false

    /**
     * Register and external user from OAuth/SAML.
     */
    override fun registerUser(username: String, source: AuthSource): UserAuthed {

        logger.info("Registering external user, multi-tenant enabled: {}", multiTentant)
        val org = getOrganization(source)

        val user = if (!userService.exists(username, null)) {
            logger.info("User not found, creating user: {}", username)

            // We must become the super admin to add new users.
            SecurityContextHolder.getContext().authentication = SuperAdminAuthentication(org.id)

            val spec = UserSpec(
                    username,
                    UUID.randomUUID().toString() + UUID.randomUUID().toString(),
                    source.attrs.getOrDefault("mail", "$username@zorroa.com"),
                    source.authSourceId,
                    firstName = source.attrs.getOrDefault("first_name", "First"),
                    lastName = source.attrs.getOrDefault("last_name", "Last"),
                    authAttrs = source.attrs)
            userService.create(spec)
        } else {
            userService.get(username)
        }
        userService.incrementLoginCounter(user)

        if (properties.getBoolean("archivist.security.saml.permissions.import")) {
            SecurityContextHolder.getContext().authentication  = SuperAdminAuthentication(org.id)
            try {
                source.groups?.let {
                    importAndAssignPermissions(user, source, it)
                }
            } finally {
                SecurityContextHolder.getContext().authentication = null
            }
        }

        return toUserAuthed(user)
    }

    @Transactional(readOnly = true)
    override fun getUser(username: String): UserAuthed {
        return toUserAuthed(userService.get(username))
    }

    @Transactional(readOnly = true)
    override fun getUser(id: UUID): UserAuthed {
        return toUserAuthed(userService.get(id))
    }

    fun getOrganization(source: AuthSource) : Organization {
        return if (multiTentant) {
            if (source.organizationName != null) {
                organizationService.get(source.organizationName!!)
            } else {
                throw BadCredentialsException("Unable to determine organization, organization was null")
            }
        } else {
            organizationService.get(Organization.DEFAULT_ORG_ID)
        }
    }

    fun importAndAssignPermissions(user: UserId, source: AuthSource, groups: List<String>) {
        val perms = getAssignedPermissions(user, source, groups)
        userService.setPermissions(user, perms, source.authSourceId)
    }

    fun getAssignedPermissions(user: UserId, source: AuthSource, groups: List<String>) : List<Permission> {
        val mapping = properties.parseToMap("archivist.security.saml.permissions.map")
        val perms = mutableListOf<Permission>()
        for (group in groups) {

            val parts = if (mapping.containsKey(group)) {
                mapping.getValue(group).split(Permission.JOIN, limit = 2)
            }
            else {
                group.split(Permission.JOIN, limit = 2)
            }

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
        return perms
    }

    /**
     * Convert an internal User into a  UserAuthed, which is something Spring understands.
     *
     * @param user: The User who has been authed
     * @return UserAuthed The authed user object
     */
    fun toUserAuthed(user: User) : UserAuthed {
        val perms = userService.getPermissions(user)
        return UserAuthed(user.id, user.organizationId, user.username, perms.toSet(), user.attrs)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(UserRegistryServiceImpl::class.java)

    }
}

@Service
@Transactional
class UserServiceImpl @Autowired constructor(
        private val userDao: UserDao,
        private val userDaoCache: UserDaoCache,
        private val permissionDao: PermissionDao,
        private val tx: TransactionEventManager,
        private val properties: ApplicationProperties
): UserService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var emailService: EmailService

    private val PASS_MIN_LENGTH = 8

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        try {

            if (userDao.generateAdminKey()) {
                logger.info("Regenerated admin key on first startup")
            }
        }
        catch (e:Exception) {
            logger.warn("Failed to generate admin HMAC key: {}", e.message)
        }
    }

    override fun createStandardUsers(org: Organization) {
        val username = getOrgBatchUserName(org.id)
        val spec = UserSpec(username,
                UUID.randomUUID().toString(),
                "$username@zorroa.com",
                UserSource.INTERNAL,
                "Batch",
                "Job")

        val batchUser = userDao.create(spec)
        /**
         * This user gets everyone and write. The batch user cannot iterate assets or
         * do searches, just processing.
         */
        userDao.addPermission(batchUser, permissionDao.get(Groups.EVERYONE), true)
        userDao.addPermission(batchUser, permissionDao.get(Groups.WRITE), true)
    }

    override fun create(spec: LocalUserSpec): User {
        if (!EmailValidator().isValid(spec.email, null)) {
            throw java.lang.IllegalArgumentException("Invalid email address: ${spec.email}")
        }

        val name = spec.name ?: spec.email.split("@")[0]
        val nameParts = name.split(Regex("\\s+"), limit=2)
        val user = create(UserSpec(
                spec.email,
                spec.password ?: generateRandomPassword(10),
                spec.email,
                UserSource.LOCAL,
                nameParts.first(),
                if (nameParts.size == 1) {
                    ""
                } else {
                    nameParts.last()
                },
                spec.permissionIds))


        tx.afterCommit(sync=false) {
            emailService.sendPasswordResetEmail(user)
        }

        return user
    }

    override fun create(spec: UserSpec): User {

        if (spec.source == null) {
            spec.source = UserSource.LOCAL
        }

        if (spec.username.length < MIN_USERNAME_SIZE) {
            throw IllegalArgumentException("User names must be at least $MIN_USERNAME_SIZE characters")
        }

        if (userDao.exists(spec.username, null)) {
            throw DuplicateEntityException("The user '" +
                    spec.username + "' already exists.")
        }

        val userPerm = permissionDao.create(
                PermissionSpec("user", spec.username), true)
        val userFolder = folderService.createUserFolder(
                spec.username, userPerm)


        spec.homeFolderId = userFolder.id
        spec.userPermissionId = userPerm.id

        val user = userDao.create(spec)

        /*
         * Get the permissions specified with the builder and add our
         * user permission to the list.
         */
        val perms = permissionDao.getAll(spec.permissionIds).toSet()
        if (!perms.isEmpty()) {
            setPermissions(user, perms)
        }

        userDao.addPermission(user, userPerm, true)
        userDao.addPermission(user, permissionDao.get(Groups.EVERYONE), true)
        return userDao.get(user.id)
    }

    override fun get(username: String): User {
        return userDao.get(username)
    }

    override fun get(id: UUID): User {
        return userDao.get(id)
    }

    override fun exists(username: String, source: String?): Boolean {
        return userDao.exists(username, source)
    }

    override fun getAll(): List<User> {
        return userDao.getAll()
    }

    override fun getAll(page: Pager): PagedList<User> {
        return userDao.getAll(page)
    }

    @Transactional(readOnly=true)
    override fun getAll(filter: UserFilter): KPagedList<User> {
        return userDao.getAll(filter)
    }

    @Transactional(readOnly=true)
    override fun findOne(filter: UserFilter): User {
        return userDao.findOne(filter)
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

    override fun getHmacKey(user: UserId): String {
        return userDao.getHmacKey(user.id)
    }

    override fun getApiKey(spec: ApiKeySpec): ApiKey {
        return userDao.getApiKey(spec)
    }

    override fun update(user: User, form: UserProfileUpdate): Boolean {

        if (form.username.isBlank()) {
            form.username = user.username
        }

        if (form.username.length < MIN_USERNAME_SIZE) {
            throw IllegalArgumentException("User names must be at least $MIN_USERNAME_SIZE characters")
        }

        val updatePermsAndFolders = user.username != form.username
        if (!userDao.exists(user.username, UserSource.LOCAL)
                && (updatePermsAndFolders
                || user.email != form.email)) {
            throw IllegalArgumentException("Users from external sources cannot change their username or email address.")
        }

        val result = userDao.update(user, form)
        if (result) {
            if (updatePermsAndFolders) {
                permissionDao.renameUserPermission(user, form.username)
                folderService.renameUserFolder(user, form.username)
            }
            tx.afterCommit(false) {
                userDaoCache.invalidate(user.id)
            }
        }
        return result
    }

    override fun delete(user: User): Boolean {
        val result = userDao.delete(user)

        if (result) {

            try {
                if (user.permissionId != null) {
                    permissionDao.delete(permissionDao.get(user.permissionId))
                }
            } catch (e: Exception) {
                logger.warn("Failed to delete user permission for {}", user)
            }

            try {
                if (user.homeFolderId != null) {
                    folderService.delete(folderService.get(user.homeFolderId))
                }
            } catch (e: Exception) {
                logger.warn("Failed to delete home folder for {}", user)
            }

            tx.afterCommit(false) {
                userDaoCache.invalidate(user.id)
            }
        }
        return result
    }

    override fun updateSettings(user: User, settings: UserSettings): Boolean {
        return userDao.setSettings(user, settings)
    }

    override fun setEnabled(user: User, value: Boolean): Boolean {
        return userDao.setEnabled(user, value)
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
    }

    override fun hasPermission(user: UserId, type: String, name: String): Boolean {
        return userDao.hasPermission(user, type, name)
    }

    override fun hasPermission(user: User, permission: Permission): Boolean {
        return userDao.hasPermission(user, permission)
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
            throw IllegalArgumentException(issues.joinToString(" "))
        }
        userDao.setPassword(user, password)
    }

    override fun resetPassword(token: String, password: String): User? {
        val issues = validatePassword(password)
        if (!issues.isEmpty()) {
            throw IllegalArgumentException(issues.joinToString(" "))
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
        private val PASS_HAS_NUMBER = Pattern.compile("\\d")
        private val PASS_HAS_UPPER = Pattern.compile("[A-Z]")
        private const val MIN_USERNAME_SIZE = 1
    }
}

