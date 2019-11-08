package com.zorroa.archivist.security

import com.zorroa.archivist.clients.ZmlpUser
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import java.util.UUID

class SuperAdminAuthority : GrantedAuthority {
    override fun getAuthority(): String {
        return Role.SUPERADMIN
    }
}

/**
 * An Authentication class for authorizing background threads.
 */
class InternalThreadAuthentication constructor(projectId: UUID) :
    AbstractAuthenticationToken(listOf(SuperAdminAuthority())) {

    // TODO: security - need to fetch a real key.
    val zmlpUser: ZmlpUser = ZmlpUser(projectId,
        UUID.randomUUID(),
        "background-thread",
        listOf(Role.SUPERADMIN))

    override fun getDetails(): Any? {
        return zmlpUser
    }

    override fun getCredentials(): Any? {
        return zmlpUser.projectId
    }

    override fun getPrincipal(): Any {
        return zmlpUser
    }

    override fun isAuthenticated(): Boolean {
        return true
    }
}
