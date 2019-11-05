package com.zorroa.archivist.security

import com.zorroa.archivist.clients.ApiKey
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class InternalAuthentication : AbstractAuthenticationToken {

    private val principal: Any

    constructor(apiKey: ApiKey) : super(apiKey.getAuthorities()) {
        this.principal = apiKey
        this.isAuthenticated = true
    }

    constructor(apiKey: ApiKey, authorities: Collection<GrantedAuthority>) : super(authorities) {
        this.principal = apiKey
        this.isAuthenticated = true
    }

    override fun getCredentials(): Any {
        return ""
    }

    override fun getPrincipal(): Any {
        return principal
    }
}
