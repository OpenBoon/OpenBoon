package com.zorroa.analyst.security

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.zorroa.analyst.security.SecurityConstants.HEADER_STRING
import com.zorroa.analyst.security.SecurityConstants.TOKEN_PREFIX
import io.jsonwebtoken.Jwts
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

class JWTAuthorizationFilter(authManager: AuthenticationManager, credentials: GoogleCredential) : BasicAuthenticationFilter(authManager) {

    private val key: ByteArray

    init {
        val privateKey = credentials.serviceAccountPrivateKey
        key = if (privateKey == null) {
            "abc123".toByteArray()
        }
        else {
            String(Base64.getEncoder().encode(privateKey.encoded)).toByteArray()
        }

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

        val claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
        val jobId :Any? = claims.body["jobId"]
        val user = jobId?.toString() ?: "unknown-job"
        return UsernamePasswordAuthenticationToken(user, null, ArrayList())
    }
}

