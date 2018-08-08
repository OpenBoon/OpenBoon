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

val superAdminId = UUID.fromString("00000000-1111-1111-1111-000000000000")
val superAdminUser = "internal-system"
val superAdminBase = UserBase(superAdminId, superAdminUser,
        "support@zorroa.com", null, null, null)

/**
 * The super-admin is both an admin for an org, and a super admin for the system overall
 */
class SuperAdminAuthentication : AbstractAuthenticationToken {

    val authed : UserAuthed

    constructor(orgId: UUID): super(listOf(SuperAdminAuthority(), AdminAuthority())) {
        authed = UserAuthed(superAdminId, orgId,superAdminUser, this.authorities.toSet())
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
