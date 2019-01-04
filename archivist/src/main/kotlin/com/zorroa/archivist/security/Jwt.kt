package com.zorroa.archivist.security

import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.JwtSecurityConstants.HEADER_STRING
import com.zorroa.archivist.security.JwtSecurityConstants.TOKEN_PREFIX
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.session.web.http.SessionRepositoryFilter
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(authManager: AuthenticationManager) : BasicAuthenticationFilter(authManager) {

    @Autowired
    private lateinit var validator: JwtValidator


    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(req: HttpServletRequest,
                                  res: HttpServletResponse,
                                  chain: FilterChain) {

        val token = req.getHeader(HEADER_STRING)

        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(req, res)
            return
        }
        /**
         * Validate the JWT claims. Asumming they are valid, create a JwtAuthenticationToken which
         * will be converted into a proper UsernamePasswordAuthenticationToken by the JwtAuthenticationProvider
         *
         * Not doing this 2 step process means the actuator endpoints can't be authed by a token.
         */
        val claims = validator.validate(token.replace(TOKEN_PREFIX, ""))
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(claims)
        req.setAttribute("authType", HttpServletRequest.CLIENT_CERT_AUTH)
        req.setAttribute(SessionRepositoryFilter::class.java.name.plus(".FILTERED"), "true")
        chain.doFilter(req, res)
    }
}

/**
 * JwtAuthenticationToken stores the validated claims
 */
class JwtAuthenticationToken constructor(
        claims: Map<String, String>
) : AbstractAuthenticationToken(listOf()) {

    val userId = claims.getValue("userId")

    override fun getCredentials(): Any {
        return userId
    }

    override fun getPrincipal(): Any {
       return userId
    }

    override fun isAuthenticated(): Boolean = false
}

/**
 * An AuthenticationProvider that specifically looks for JwtAuthenticationToken.  Pulls
 * the userId from the JwtAuthenticationToken and returns a UsernamePasswordAuthenticationToken.
 */
class JwtAuthenticationProvider: AuthenticationProvider {

    @Autowired
    private lateinit var userRegistryService: UserRegistryService

    override fun authenticate(auth: Authentication): Authentication {
        val token = auth as JwtAuthenticationToken
        val user = userRegistryService.getUser(UUID.fromString(token.userId))
        return UsernamePasswordAuthenticationToken(user, "", user.authorities)
    }

    override fun supports(cls: Class<*>): Boolean {
        return cls == JwtAuthenticationToken::class.java
    }
}

