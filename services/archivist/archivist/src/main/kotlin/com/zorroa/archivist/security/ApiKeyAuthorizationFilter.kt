package com.zorroa.archivist.security

import com.zorroa.archivist.clients.AuthServerClient
import com.zorroa.archivist.clients.ZmlpUser
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

val HEADER = "Authorization"
val PREFIX = "Bearer "

@Component
class ApiKeyAuthorizationFilter constructor(
    val authServerClient: AuthServerClient,
    authenticatioinManager: AuthenticationManager
) : BasicAuthenticationFilter(authenticatioinManager) {

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {

        val token = req.getHeader(HEADER)?.let {
            if (it.startsWith(PREFIX)) {
                it.removePrefix(PREFIX)
            } else {
                null
            }
        } ?: req.getParameter("token")
        if (token == null) {
            log.warn("No authentication token")
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
            return
        }
        
        try {
            val apiToken = authServerClient.authenticate(token)
            SecurityContextHolder.getContext().authentication = ApiTokenAuthentication(apiToken)
            chain.doFilter(req, res)
        } catch (e: Exception) {
            log.warn("Invalid authentication token: ", e)
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApiKeyAuthorizationFilter::class.java)
    }
}


class ApiTokenAuthentication constructor(
    val zmlpUser : ZmlpUser
) : AbstractAuthenticationToken(listOf()) {

    override fun getCredentials(): Any {
        return zmlpUser.projectId
    }

    override fun getPrincipal(): Any {
        return zmlpUser.projectId
    }

    override fun isAuthenticated(): Boolean = false
}


class ApiKeyAuthenticationProvider : AuthenticationProvider {
    
    override fun authenticate(auth: Authentication): Authentication {
        val token = auth as ApiTokenAuthentication

        return UsernamePasswordAuthenticationToken(
                token.zmlpUser,
                token.credentials,
                token.zmlpUser.getAuthorities())
    }

    override fun supports(cls: Class<*>): Boolean {
        return cls == ApiTokenAuthentication::class.java
    }
}
