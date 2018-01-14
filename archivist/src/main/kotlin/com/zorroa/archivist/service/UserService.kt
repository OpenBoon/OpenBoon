package com.zorroa.archivist.service

import com.google.common.base.Charsets
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.google.common.io.CharStreams
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.repository.UserDao
import com.zorroa.archivist.repository.UserPresetDao
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.sdk.client.exception.DuplicateElementException
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.dao.DataAccessException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.mail.MessagingException

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

    fun get(id: Int): User

    fun getByEmail(email: String): User

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

    fun getPermissions(user: User): List<Permission>

    fun setPermissions(user: User, perms: Collection<Permission>)

    fun addPermissions(user: User, perms: Collection<Permission>)

    fun removePermissions(user: User, perms: Collection<Permission>)

    fun hasPermission(user: User, type: String, name: String): Boolean

    fun hasPermission(user: User, permission: Permission): Boolean
    fun getUserPreset(id: Int): UserPreset
    fun updateUserPreset(id: Int, preset: UserPreset): Boolean
    fun createUserPreset(preset: UserPresetSpec): UserPreset
    fun deleteUserPreset(preset: UserPreset): Boolean

    fun sendSharedLinkEmail(fromUser: User, toUser: User, link: SharedLink)

    fun sendPasswordResetEmail(user: User): PasswordResetToken

    fun sendOnboardEmail(user: User): PasswordResetToken

    fun checkPassword(user: String, supplied: String)

    fun resetPassword(user: User, password: String)

    fun resetPassword(token: String, password: String): User?

}

@Service
@Transactional
class UserServiceImpl @Autowired constructor(
        private val mailSender: JavaMailSender,
        private val networkEnv: NetworkEnvironment,
        private val userDao: UserDao,
        private val permissionDao: PermissionDao,
        private val userPresetDao: UserPresetDao,
        private val txem: TransactionEventManager,
        private val properties: ApplicationProperties
): UserService {

    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var logService: EventLogService

    private val PASS_MIN_LENGTH = 8

    override fun create(builder: UserSpec): User {
        return create(builder, SOURCE_LOCAL)
    }

    override fun create(builder: UserSpec, source: String): User {

        if (userDao.exists(builder.username)) {
            throw DuplicateElementException("The user '" +
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
        userDao.addPermission(user, permissionDao.get("group", "everyone"), true)

        txem.afterCommit(false, { logService.logAsync(UserLogSpec.build(LogAction.Create, user)) })

        return userDao.get(user.id)
    }

    override fun get(username: String): User {
        return userDao.get(username)
    }

    override fun get(id: Int): User {
        return userDao.get(id)
    }

    override fun getByEmail(email: String): User {
        return userDao.getByEmail(email)
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
            txem.afterCommit(true,
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

            txem.afterCommit(true,  { logService.logAsync(UserLogSpec.build(LogAction.Update, user)) })
        }
        return result
    }

    override fun updateSettings(user: User, settings: UserSettings): Boolean {
        val result = userDao.setSettings(user, settings)
        if (result) {
            txem.afterCommit(true, { logService.logAsync(UserLogSpec.build(LogAction.Update, user)) })
        }
        return result
    }

    override fun setEnabled(user: User, value: Boolean): Boolean {
        val result = userDao.setEnabled(user, value)

        if (result) {
            if (result) {
                txem.afterCommit(true, {
                    logService.logAsync(UserLogSpec.build(if (value) "enable" else "disable", user)) })
            }
        }

        return result
    }

    override fun getPermissions(user: User): List<Permission> {
        return permissionDao.getAll(user)
    }

    override fun setPermissions(user: User, perms: Collection<Permission>) {
        /*
         * Don't let setPermissions set immutable permission types which can never
         * be added or removed via the external API.
         */
        val filtered = perms.stream().filter { p -> !PERMANENT_TYPES.contains(p.type) }.collect(Collectors.toList())
        userDao.setPermissions(user, filtered)

        txem.afterCommit(true, {
            logService.logAsync(UserLogSpec.build("set_permission", user)
                    .putToAttrs("perms", perms.stream().map { ps -> ps.name }.collect(Collectors.toList())))
        })
    }

    override fun addPermissions(user: User, perms: Collection<Permission>) {
        for (p in perms) {
            if (PERMANENT_TYPES.contains(p.type)) {
                continue
            }
            if (!userDao.hasPermission(user, p)) {
                userDao.addPermission(user, p, false)
            }
        }
        txem.afterCommit(false, {
            logService.logAsync(UserLogSpec.build("add_permission", user)
                    .putToAttrs("perms", perms.stream().map { ps -> ps.name }.collect(Collectors.toList())))
        })
    }

    override fun removePermissions(user: User, perms: Collection<Permission>) {
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
        txem.afterCommit(true, {
            logService.logAsync(UserLogSpec.build("remove_permission", user)
                    .putToAttrs("perms", perms.stream().map { ps -> ps.name }.collect(Collectors.toList())))
        })
    }

    override fun hasPermission(user: User, type: String, name: String): Boolean {
        return userDao.hasPermission(user, type, name)
    }

    override fun hasPermission(user: User, permission: Permission): Boolean {
        return userDao.hasPermission(user, permission)
    }

    override fun getUserPresets(): List<UserPreset> {
        return userPresetDao.getAll()
    }

    override fun getUserPreset(id: Int): UserPreset {
        return userPresetDao.get(id)
    }

    override fun updateUserPreset(id: Int, preset: UserPreset): Boolean {
        return userPresetDao.update(id, preset)
    }

    override fun createUserPreset(preset: UserPresetSpec): UserPreset {
        return userPresetDao.create(preset)
    }

    override fun deleteUserPreset(preset: UserPreset): Boolean {
        return userPresetDao.delete(preset.presetId)
    }

    override fun sendSharedLinkEmail(fromUser: User, toUser: User, link: SharedLink) {

        val toName = if (toUser.firstName == null) toUser.username else toUser.firstName
        val fromName = if (fromUser.firstName == null) fromUser.username else fromUser.firstName
        val url = networkEnv.publicUri.toString() + "/search?id=" + link.id

        val text = StringBuilder(1024)
        text.append("Hello ")
        text.append(toName)
        text.append(",\n\n")
        text.append(fromName)
        text.append(" has sent you a link.")
        text.append("\n\n" + url)

        var htmlMsg: String? = null
        try {
            htmlMsg = getTextResourceFile("emails/SharedLink.html")
            htmlMsg = htmlMsg.replace("*|URL|*", url)
            htmlMsg = htmlMsg.replace("*|TO_USER|*", toName)
            htmlMsg = htmlMsg.replace("*|FROM_USER|*", fromName)
        } catch (e: IOException) {
            logger.warn("Failed to open HTML template for sharing links.. Sending text only.", e)
        }

        try {
            sendHTMLEmail(toUser, fromName + " has shared a link with you.", text.toString(), htmlMsg)
        } catch (e: MessagingException) {
            logger.warn("Email for sendPasswordResetEmail not sent, unexpected ", e)
        }

    }

    override fun sendPasswordResetEmail(user: User): PasswordResetToken {
        val token = PasswordResetToken(userDao.setEnablePasswordRecovery(user))

        if (token != null) {
            val name = if (user.firstName == null) user.username else user.firstName
            val url = networkEnv.publicUri.toString() + "/password?token=" + token.toString()

            val text = StringBuilder(1024)
            text.append("Hello ")
            text.append(name)
            text.append(",\n\nClick on the link below to change your Zorroa login credentials.")
            text.append("\n\n" + url)
            text.append("\n\nIf you are not trying to change your Zorroa login credentials, please ignore this email.")

            var htmlMsg: String? = null
            try {
                htmlMsg = getTextResourceFile("emails/PasswordReset.html")
                htmlMsg = htmlMsg.replace("*|RESET_PASSWORD_URL|*", url + "&source=file_server")
                htmlMsg = htmlMsg.replace("*|FIRST_NAME|*", name)
            } catch (e: IOException) {
                logger.warn("Failed to open HTML template for onboarding. Sending text only.", e)
            }

            try {
                sendHTMLEmail(user, "Zorroa Account Verification", text.toString(), htmlMsg)
                token.isEmailSent = true
            } catch (e: MessagingException) {
                logger.warn("Email for sendPasswordResetEmail not sent, unexpected ", e)
            }

        }
        return token
    }

    override fun sendOnboardEmail(user: User): PasswordResetToken {
        val token = PasswordResetToken(userDao.setEnablePasswordRecovery(user))
        val name = if (user.firstName == null) user.username else user.firstName

        if (token != null) {
            val url = networkEnv.publicUri.toString() + "/onboard?token=" + token.toString()

            val text = StringBuilder(1024)
            text.append("Hello ")
            text.append(name)
            text.append(",\n\nWelcome to Zorroa. Let's get your stuff!")
            text.append(",\n\nClick on the link below to import your assets.")
            text.append("\n\n" + url)

            var htmlMsg: String? = null
            try {
                htmlMsg = getTextResourceFile("emails/Onboarding.html")
                htmlMsg = htmlMsg.replace("*|FIRST_NAME|*", name)
                htmlMsg = htmlMsg.replace("*|FILE_SERVER_URL|*", url + "&source=file_server")
                htmlMsg = htmlMsg.replace("*|MY_COMPUTER_URL|*", url + "&source=my_computer")
                htmlMsg = htmlMsg.replace("*|CLOUD_SOURCE_URL|*", url + "&source=cloud")
            } catch (e: IOException) {
                logger.warn("Failed to open HTML template for onboarding, Sending text only.", e)
            }

            try {
                sendHTMLEmail(user, "Welcome to Zorroa", text.toString(), htmlMsg)
                token.isEmailSent = true
            } catch (e: MessagingException) {
                logger.warn("Email for sendOnboardEmail not sent, unexpected ", e)
            }

        }
        return token
    }

    @Throws(MessagingException::class)
    private fun sendHTMLEmail(user: User, subject: String, text: String, htmlMsg: String?) {
        var email = user.email
        if (ArchivistConfiguration.unittest) {
            email = System.getProperty("user.name") + "@zorroa.com"
        }

        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, true, "utf-8")
        helper.setFrom("Zorroa Account Bot <noreply@zorroa.com>")
        helper.setReplyTo("Zorroa Account Bot <noreply@zorroa.com>")
        helper.setTo(email)
        helper.setSubject(subject)
        if (htmlMsg != null) {
            helper.setText(text, htmlMsg)
        } else {
            helper.setText(text)
        }
        mailSender.send(mimeMessage)

    }

    @Throws(IOException::class)
    private fun getTextResourceFile(fileName: String): String {
        return CharStreams.toString(InputStreamReader(
                ClassPathResource(fileName).inputStream, Charsets.UTF_8))
    }

    override fun checkPassword(username: String, supplied: String) {
        val storedPassword = getPassword(username)
        if (!BCrypt.checkpw(supplied, storedPassword)) {
            throw BadCredentialsException("Invalid username or password")
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

        private val SOURCE_LOCAL = "local"

        private val PASS_HAS_NUMBER = Pattern.compile("\\d")
        private val PASS_HAS_UPPER = Pattern.compile("[A-Z]")
    }
}

