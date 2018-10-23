package com.zorroa.archivist.security

import com.zorroa.archivist.domain.UserBase
import com.zorroa.archivist.sdk.security.UserAuthed
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import java.util.*

class SuperAdminAuthority : GrantedAuthority {
    override fun getAuthority(): String {
        return "super::administrator"
    }
}

class AdminAuthority : GrantedAuthority {
    override fun getAuthority(): String {
        return "zorroa::administrator"
    }
}

/**
 * Some constants referring to super admin users. The super admin doesn't have access to
 * any data.  It's a temporary in-memory user used for creating organizations and SSO users.
 */
object SuperAdmin {
    val id : UUID = UUID.fromString("00000000-1111-1111-1111-000000000000")
    const val username = "organization-admin"
    val base = UserBase(id, username,
            "support@zorroa.com", null, null, null)
}


/**
 * The super-admin is both an admin for an org, and a super admin for the system overall
 */
class SuperAdminAuthentication : AbstractAuthenticationToken {

    val authed : UserAuthed

    constructor(orgId: UUID): super(listOf(SuperAdminAuthority(), AdminAuthority())) {
        authed = UserAuthed(SuperAdmin.id, orgId, SuperAdmin.username, this.authorities.toSet())
    }

    override fun getDetails(): Any? {
        return authed
    }

    override fun getCredentials(): Any? {
        return ""
    }

    override fun getPrincipal(): Any {
        return authed
    }

    override fun isAuthenticated(): Boolean {
        return true
    }
}
