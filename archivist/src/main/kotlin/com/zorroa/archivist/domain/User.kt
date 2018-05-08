package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.MoreObjects
import com.zorroa.archivist.sdk.security.UserId
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotEmpty
import java.util.*

/**
 * The base user attributes returned with objects that reference a user.
 */
data class UserBase (
        override val id: UUID,
        val username: String,
        val email: String,
        val permissionId: UUID,
        val homeFolderId: UUID) : UserId {

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
data class User (
        override val id: UUID,
        val username: String,
        val email: String,
        val permissionId: UUID,
        val homeFolderId: UUID,
        val firstName: String?,
        val lastName: String?,
        val enabled: Boolean,
        val settings: UserSettings,
        val loginCount: Int,
        val timeLastLogin: Long) : UserId {

    @JsonIgnore
    override fun getName(): String = username

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("username", username)
                .toString()
    }
}

data class UserProfileUpdate (
        @NotEmpty var username: String = "",
        @NotEmpty @Email var email: String = "",
        var firstName : String? = "",
        var lastName: String? = "")



