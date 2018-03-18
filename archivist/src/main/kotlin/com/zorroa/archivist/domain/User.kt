package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.MoreObjects
import com.zorroa.archivist.sdk.security.UserId
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


