package com.zorroa.archivist.security

import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.common.server.JwtSecurityConstants.HEADER_STRING
import com.zorroa.common.server.JwtSecurityConstants.TOKEN_PREFIX
import com.zorroa.common.server.JwtValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(authManager: AuthenticationManager) : BasicAuthenticationFilter(authManager) {

    @Autowired
    private lateinit var validator: JwtValidator

    @Autowired
    private lateinit var userRegistryService: UserRegistryService

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(req: HttpServletRequest,
                                  res: HttpServletResponse,
                                  chain: FilterChain) {
        val token = req.getHeader(HEADER_STRING)

        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(req, res)
            return
        }

        val authentication = getAuthentication(token.replace(TOKEN_PREFIX, ""))
        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(req, res)
    }

    private fun getAuthentication(token: String): Authentication? {
        val claims = validator.validate(token)

        return when {
            claims.containsKey("ZORROA_USER") -> {
                val user = userRegistryService.getUser(claims.getValue("ZORROA_USER"))
                UsernamePasswordAuthenticationToken(user, "",
                        user.authorities)
            }
            claims.containsKey("ZORROA_ORGANIZATION_ID") -> {
                SuperAdminAuthentication(UUID.fromString(claims["ZORROA_ORGANIZATION_ID"]))
            }
            else -> {
                logger.warn("Not enough claim information to provide JWT auth")
                null
            }
        }
    }
}
