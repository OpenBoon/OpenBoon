package com.zorroa.archivist.security

import com.zorroa.archivist.clients.AuthServerClient
import com.zorroa.archivist.clients.ZmlpActor
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

val HEADER = "Authorization"
val PREFIX = "Bearer "

class ApiKeyAuthorizationFilter constructor(
        val authServerClient: AuthServerClient
) : OncePerRequestFilter() {

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
        if (token != null) {
            try {
                val apiToken = authServerClient.authenticate(token)
                SecurityContextHolder.getContext().authentication = ApiTokenAuthentication(apiToken)
            } catch (e: Exception) {
                log.warn("Invalid authentication token: ", e)
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
                return
            }
        }

        chain.doFilter(req, res)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApiKeyAuthorizationFilter::class.java)
    }
}


class ApiTokenAuthentication constructor(
        val zmlpActor : ZmlpActor
) : AbstractAuthenticationToken(listOf()) {

    override fun getCredentials(): Any {
        return zmlpActor.projectId
    }

    override fun getPrincipal(): Any {
        return zmlpActor.projectId
    }

    override fun isAuthenticated(): Boolean = false
}


class ApiKeyAuthenticationProvider : AuthenticationProvider {

    override fun authenticate(auth: Authentication): Authentication {
        val token = auth as ApiTokenAuthentication

        return UsernamePasswordAuthenticationToken(
                token.zmlpActor,
                token.credentials,
                token.zmlpActor.getAuthorities())
    }

    override fun supports(cls: Class<*>): Boolean {
        return cls == ApiTokenAuthentication::class.java
    }
}