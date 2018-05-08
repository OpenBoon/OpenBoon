package com.zorroa.archivist.security

import com.zorroa.archivist.sdk.security.UserAuthed
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class InternalAuthentication : AbstractAuthenticationToken {

    private val principal: Any

    constructor(user: UserAuthed) : super(user.authorities) {
        this.principal = user
        this.isAuthenticated = true
    }

    constructor(user: UserAuthed, authorities: Collection<GrantedAuthority>) : super(authorities) {
        this.principal = user
        this.isAuthenticated = true
    }

    override fun getCredentials(): Any {
        return ""
    }

    override fun getPrincipal(): Any {
        return principal
    }

}
