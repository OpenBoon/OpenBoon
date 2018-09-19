package com.zorroa.archivist.security

import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.JwtSecurityConstants.HEADER_STRING
import com.zorroa.archivist.security.JwtSecurityConstants.TOKEN_PREFIX
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
        val userId = claims.getValue("userId")

        return if (userId == "superadmin" && claims.containsKey("org")) {
            SuperAdminAuthentication(UUID.fromString(claims["org"]))
        }
        else {
            val user = userRegistryService.getUser(UUID.fromString(userId))
            UsernamePasswordAuthenticationToken(user, "", user.authorities)
        }
    }
}
