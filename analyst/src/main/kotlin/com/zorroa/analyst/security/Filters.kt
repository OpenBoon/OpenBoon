package com.zorroa.analyst.security

import com.zorroa.common.server.JwtSecurityConstants.HEADER_STRING
import com.zorroa.common.server.JwtSecurityConstants.TOKEN_PREFIX
import com.zorroa.common.server.JwtValidator
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(authManager: AuthenticationManager, private val validator: JwtValidator) : BasicAuthenticationFilter(authManager) {

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

        val user =  UsernamePasswordAuthenticationToken(
                claims["ZORROA_USER"], null, ArrayList())
        user.details = claims
        return user
    }
}

