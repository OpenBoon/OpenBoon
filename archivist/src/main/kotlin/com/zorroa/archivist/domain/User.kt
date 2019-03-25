package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.MoreObjects
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.archivist.security.createPasswordHash
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotEmpty
import java.util.*

/**
 * A couple internal constants for user source.
 */
object UserSource {
    /**
     * The user is internal only, no folder or user perm
     */
    const val INTERNAL : String = "internal"

    /**
     * The user authenticates locally, ie we have a salted pass for this user.
     */
    const val LOCAL : String = "local"
}

/**
 * Return the organizations batch user.
 *
 * @param orgId The UUID for the org.
 * @return The name of the organizations batch user.
 */
fun getOrgBatchUserName(orgId: UUID) : String {
    return "batch_user_$orgId"
}

/**
 * The base user attributes returned with objects that reference a user.
 */
class UserBase (
        override val id: UUID,
        val username: String,
        val email: String,
        val permissionId: UUID?,
        val homeFolderId: UUID?,
        val organizationId: UUID?) : UserId {

    @JsonIgnore
    override fun getName(): String = username

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("username", username)
                .toString()
    }
}

/**
 * The UserCore is all the user properties.
 */
class User (
        override val id: UUID,
        val username: String,
        val email: String,
        val source: String,
        val permissionId: UUID?,
        val homeFolderId: UUID?,
        val organizationId: UUID,
        val firstName: String?,
        val lastName: String?,
        val enabled: Boolean,
        val settings: UserSettings,
        val loginCount: Int,
        val timeLastLogin: Long,
        var attrs: Map<String,Any>) : UserId {

    @JsonIgnore
    override fun getName(): String = username

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("username", username)
                .toString()
    }
}

class UserProfileUpdate (
        @NotEmpty var username: String = "",
        @NotEmpty @Email var email: String = "",
        var firstName : String? = "",
        var lastName: String? = "")


/**
 * LocalUserSpec is a new version of the UserSpec for the v2 create
 * user endpoint.  It accepts a organizationId but only pays attention to it
 * if the user is a super admin.
 *
 * This spec also enforces the username as the email address.
 *
 * @property email - the email address of the user
 * @property name - A name for the user
 * @property password - an optional password for the user.
 * @property organizationId - An optional organization ID, default's to the current users.
 * @property permissionIds - An optional set of permissions.
 */
class LocalUserSpec (
        val email: String,
        val name: String? = null,
        val password: String? = null,
        var permissionIds: List<UUID>? = null
)

class UserSpec (
        val username: String,
        val password: String,
        val email: String,
        var source : String = "local",
        var firstName: String? = null,
        var lastName: String? = null,
        var permissionIds: List<UUID>? = null,
        @JsonIgnore
        var homeFolderId: UUID? = null,
        @JsonIgnore
        var userPermissionId: UUID? = null,
        @JsonIgnore
        var authAttrs :Map<String,String>? =  null) {

    fun hashedPassword(): String {
        return createPasswordHash(password)
    }
}

/**
 * Necessary properties for creating an API key.
 */
class ApiKeySpec(
        val userId: UUID,
        val user: String,
        val replace: Boolean,
        var server: String
)

/**
 * Structure for storing a users API key.
 */
class ApiKey(
        val userId: UUID,
        val user: String,
        val key: String,
        val server: String
)

/**
 * A class for filtering users.
 */
class UserFilter constructor(
        val ids : List<UUID>? = null,
        val usernames: List<String>? = null,
        val emails : List<String>? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
            "id" to "users.pk_user",
            "username" to "users.str_username",
            "email" to "users.str_email")

    override fun build() {
        if (sort == null) {
            sort = listOf("username:a")
        }

        addToWhere("users.pk_organization=?")
        addToValues(getOrgId())

        usernames?.let  {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("users.str_username", it.size))
            addToValues(it)
        }

        emails?.let  {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("users.str_email", it.size))
            addToValues(it)
        }

        ids?.let  {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("users.pk_user", it.size))
            addToValues(it)
        }
    }
}

/**
 * A loosely defined user-settings class for storing arbitrary user settings.
 *
 * @property search Search related settings.
 * @property metadata Metadata related settings.
 */
class UserSettings(
        var search: Map<String,Any>? = null,
        var metadata: Map<String,Any>? = null
)

/**
 * UserPasswordUpdate defines the properties required to chnage a user's password.
 *
 * @property newPassword The new password.
 * @property oldPassword The old password.
 */
class UserPasswordUpdate(
        val newPassword: String,
        val oldPassword: String
)