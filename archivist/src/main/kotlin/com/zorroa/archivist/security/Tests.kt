package com.zorroa.archivist.security

import com.zorroa.archivist.sdk.security.UserAuthed
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority

class UnitTestAuthentication(principal: UserAuthed,
                             authorities: Collection<GrantedAuthority>) : AbstractAuthenticationToken(authorities) {
    private val principal: Any
    private val username: String

    init {
        this.principal = principal
        this.username = principal.username
    }

    override fun getCredentials(): Any {
        return ""
    }

    override fun getPrincipal(): Any {
        return principal
    }

    override fun getName(): String {
        return username
    }

    companion object {

        private val serialVersionUID = 1L
    }
}

class UnitTestAuthenticationProvider : AuthenticationProvider {
    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {
        return authentication
    }

    override fun supports(aClass: Class<*>): Boolean {
        return aClass == UnitTestAuthentication::class.java
    }
}

