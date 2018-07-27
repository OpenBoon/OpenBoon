package com.zorroa.archivist.security

import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.common.server.JwtSecurityConstants.HEADER_STRING
import com.zorroa.common.server.JwtSecurityConstants.TOKEN_PREFIX
import com.zorroa.common.server.JwtValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
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

    private fun getAuthentication(token: String): UsernamePasswordAuthenticationToken? {
        val claims = validator.validate(token)
        val user = userRegistryService.getUser(claims.getValue("ZORROA_USER"))
        return UsernamePasswordAuthenticationToken(user, "",
                user.authorities)
    }
}
