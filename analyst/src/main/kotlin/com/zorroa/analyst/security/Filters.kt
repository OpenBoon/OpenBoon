package com.zorroa.analyst.security

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.zorroa.analyst.security.SecurityConstants.HEADER_STRING
import com.zorroa.analyst.security.SecurityConstants.TOKEN_PREFIX
import io.jsonwebtoken.Jwts
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import javax.annotation.PostConstruct
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(authManager: AuthenticationManager) : BasicAuthenticationFilter(authManager) {

    private lateinit var credentials: GoogleCredential

    @PostConstruct
    fun setup() {
        credentials = GoogleCredential.fromStream(FileInputStream("keys/credentials.json"))
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(req: HttpServletRequest,
                                  res: HttpServletResponse,
                                  chain: FilterChain) {
        val token = req.getHeader(HEADER_STRING)

        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(req, res)
            return
        }

        val authentication = getAuthentication(token)
        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(req, res)
    }

    private fun getAuthentication(token: String): UsernamePasswordAuthenticationToken? {
        val user = Jwts.parser()
                .setSigningKey(credentials.serviceAccountPrivateKey)
                .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                .body
                .subject
        return if (user != null) {
            UsernamePasswordAuthenticationToken(user, null, ArrayList())
        }
        else {
            null
        }
    }
}
