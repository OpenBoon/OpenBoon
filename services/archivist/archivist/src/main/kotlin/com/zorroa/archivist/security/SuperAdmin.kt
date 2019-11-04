package com.zorroa.archivist.security

import com.zorroa.archivist.clients.ApiKey
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import java.util.UUID

class SuperAdminAuthority : GrantedAuthority {
    override fun getAuthority(): String {
        return Role.SUPERADMIN
    }
}

/**
 * The super-admin is both an admin for an org, and a super admin for the system overall
 */
class SuperAdminAuthentication constructor(projectId: UUID) :
    AbstractAuthenticationToken(listOf(SuperAdminAuthority())) {

    val apiKey: ApiKey = ApiKey(projectId,
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        listOf(Role.SUPERADMIN))

    override fun getDetails(): Any? {
        return apiKey
    }

    override fun getCredentials(): Any? {
        return ""
    }

    override fun getPrincipal(): Any {
        return apiKey
    }

    override fun isAuthenticated(): Boolean {
        return true
    }
}
