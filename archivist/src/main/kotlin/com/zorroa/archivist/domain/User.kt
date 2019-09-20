package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.MoreObjects
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.archivist.security.createPasswordHash
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotEmpty
import java.util.UUID

/**
 * A couple internal constants for user source.
 */
object UserSource {
    /**
     * The user is internal only, no folder or user perm
     */
    const val INTERNAL: String = "internal"

    /**
     * The user authenticates locally, ie we have a salted pass for this user.
     */
    const val LOCAL: String = "local"
}

/**
 * Return the organizations batch user.
 *
 * @param orgId The UUID for the org.
 * @return The name of the organizations batch user.
 */
fun getOrgBatchUserName(orgId: UUID): String {
    return "batch_user_$orgId"
}

@ApiModel("User Base", description = "Base user attributes returned with objects that reference a user.")
class UserBase(

    @ApiModelProperty("UUID of the User.")
    override val id: UUID,

    @ApiModelProperty("User's username.")
    val username: String,

    @ApiModelProperty("Email address for the User.")
    val email: String,

    @ApiModelProperty("UUID of the Permission assigned to the User.")
    val permissionId: UUID?,

    @ApiModelProperty("UUID of the User's home Folder.")
    val homeFolderId: UUID?,

    @ApiModelProperty("UUID of the Organization the User belongs to.")
    val organizationId: UUID?

) : UserId {

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
@ApiModel("User", description = "Describes a User of the application.")
class User(

    @ApiModelProperty("UUID of the User.")
    override val id: UUID,

    @ApiModelProperty("Username of the User.")
    val username: String,

    @ApiModelProperty("Email address of the User.")
    val email: String,

    @ApiModelProperty("Source that created the User.")
    val source: String,

    @ApiModelProperty("UUID of the Permission assigned to the User.")
    val permissionId: UUID?,

    @ApiModelProperty("UUID of the User's home Folder.")
    val homeFolderId: UUID?,

    @ApiModelProperty("UUID of the Organization the User belongs to.")
    val organizationId: UUID,

    @ApiModelProperty("User's first name.")
    val firstName: String?,

    @ApiModelProperty("User's last name.")
    val lastName: String?,

    @ApiModelProperty("If true the User is enabled and will be allowed to log in.")
    val enabled: Boolean,

    @ApiModelProperty("User's settings.")
    val settings: UserSettings,

    @ApiModelProperty("Numbner of times the User has logged in.")
    val loginCount: Int,

    @ApiModelProperty("Time the User last logged in.")
    val timeLastLogin: Long,

    @ApiModelProperty("Attributes for the User.")
    var attrs: Map<String, Any>,

    @ApiModelProperty("User's language")
    val language: String?,

    @ApiModelProperty("User's query string filter")
    @JsonIgnore
    val queryStringFilter: String? = null

) : UserId {

    @JsonIgnore
    override fun getName(): String = username

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("username", username)
            .toString()
    }
}

@ApiModel("User Profile Update", description = "Attributes to update a User Profile.")
class UserProfileUpdate(

    @ApiModelProperty("Username of the User.")
    @NotEmpty
    var username: String = "",

    @ApiModelProperty("Email address for the User.")
    @NotEmpty
    @Email
    var email: String = "",

    @ApiModelProperty("User's first name.")
    var firstName: String? = "",

    @ApiModelProperty("User's last name.")
    var lastName: String? = "",

    @ApiModelProperty("User's language")
    val language: String? = ""

)

@ApiModel(
    "Local User Spec", description = "LocalUserSpec is a new version of the UserSpec for the v2 create " +
        "user endpoint.  It accepts a organizationId but only pays attention to it if the user is a super admin."
)
class LocalUserSpec(

    @ApiModelProperty("Email address of the User")
    val email: String,

    @ApiModelProperty("Username for the User")
    val name: String? = null,

    @ApiModelProperty("Optional password for the User.")
    val password: String? = null,

    @ApiModelProperty("Optional set of permissions to assign the User.")
    var permissionIds: List<UUID>? = null

)

@ApiModel("User Spec", description = "Attributes required to create a User.")
class UserSpec(

    @ApiModelProperty("Username for the User.")
    val username: String,

    @ApiModelProperty("User's password.")
    val password: String,

    @ApiModelProperty("Email address for the User.")
    val email: String,

    @ApiModelProperty("Source that is creating the User.")
    var source: String = "local",

    @ApiModelProperty("User's first name.")
    var firstName: String? = null,

    @ApiModelProperty("User's last name.")
    var lastName: String? = null,

    @ApiModelProperty("UUIDs of Permissions to assign to the User.")
    var permissionIds: List<UUID>? = null,

    @JsonIgnore
    var homeFolderId: UUID? = null,

    @JsonIgnore
    var userPermissionId: UUID? = null,

    @JsonIgnore
    var authAttrs: Map<String, String>? = null,

    @ApiModelProperty("User's language")
    val language: String? = null,

    @JsonIgnore
    val id: UUID? = null,

    @JsonIgnore
    val queryStringFilter: String? = null

) {

    fun hashedPassword(): String {
        return createPasswordHash(password)
    }
}

/**
 * Used when updating an externally managed user (e.g.: SAML or JWT)
 */
class RegisteredUserUpdateSpec(val user: User, authAttrs: Map<String, String>) {
    val email: String = authAttrs["email"] ?: user.email
    var firstName: String? = authAttrs["first_name"] ?: user.firstName
    var lastName: String? = authAttrs["last_name"] ?: user.lastName
    var authAttrs: Map<String, String> = authAttrs
    val language: String? = authAttrs.getOrDefault("user_locale", user.language)
    val queryStringFilter: String? = authAttrs.getOrDefault("query_string_filter", authAttrs["queryStringFilter"])
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
@ApiModel("User Filter", description = "Search filter for finding Users.")
class UserFilter constructor(

    @ApiModelProperty("User UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Usernames to match.")
    val usernames: List<String>? = null,

    @ApiModelProperty("Email addresses to match.")
    val emails: List<String>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "id" to "users.pk_user",
        "username" to "users.str_username",
        "email" to "users.str_email"
    )

    override fun build() {
        if (sort == null) {
            sort = listOf("username:a")
        }

        addToWhere("users.pk_organization=?")
        addToValues(getOrgId())

        usernames?.let {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("users.str_username", it.size))
            addToValues(it)
        }

        emails?.let {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("users.str_email", it.size))
            addToValues(it)
        }

        ids?.let {
            addToWhere(com.zorroa.common.util.JdbcUtils.inClause("users.pk_user", it.size))
            addToValues(it)
        }
    }
}

@ApiModel("User Settings", description = "User-settings class for storing arbitrary user settings.")
class UserSettings(

    @ApiModelProperty("Search related settings.")
    var search: Map<String, Any>? = null,

    @ApiModelProperty("Metadata related settings.")
    var metadata: Map<String, Any>? = null

)

@ApiModel("User Password Update", description = "Defines the properties required to change a User's password.")
class UserPasswordUpdate(

    @ApiModelProperty("New password.")
    val newPassword: String,

    @ApiModelProperty("Old password.")
    val oldPassword: String? = null

)
