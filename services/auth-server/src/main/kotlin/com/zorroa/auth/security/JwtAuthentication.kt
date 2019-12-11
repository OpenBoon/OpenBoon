package com.zorroa.auth.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ZmlpActor
import com.zorroa.auth.repository.ApiKeyRepository
import java.io.IOException
import java.util.Date
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val TOKEN_PREFIX = "Bearer "
const val AUTH_HEADER = "Authorization"

class JWTAuthorizationFilter : OncePerRequestFilter() {

    @Autowired
    lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    lateinit var externalApiKey: ApiKey

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            val token = req.getHeader(AUTH_HEADER)?.let {
                if (it.startsWith(TOKEN_PREFIX)) {
                    it.removePrefix(TOKEN_PREFIX)
                } else {
                    null
                }
            } ?: req.getParameter("token")
            ?: throw RuntimeException("No token specified")

            val jwt = JWT.decode(token)
            jwt.expiresAt?.let {
                if (Date() > it) {
                    throw RuntimeException("Token was expired")
                }
            }

            val keyId = UUID.fromString(jwt.claims.getValue("keyId").asString())

            /**
             * Check to see if the key is the internal admin key.
             */
            val apiKey = if (externalApiKey.keyId == keyId) {
                val projectId = if (jwt.claims.containsKey("projectId")
                ) {
                    UUID.fromString(jwt.claims.getValue("projectId").asString())
                } else {
                    externalApiKey.projectId
                }
                ApiKey(
                    externalApiKey.keyId,
                    projectId,
                    externalApiKey.sharedKey,
                    externalApiKey.name, externalApiKey.permissions
                )
            } else {
                apiKeyRepository.findById(keyId).get()
            }

            val alg = Algorithm.HMAC512(apiKey.sharedKey)
            alg.verify(jwt)

            SecurityContextHolder.getContext().authentication =
                JwtAuthenticationToken(apiKey.getZmlpActor(), apiKey.getGrantedAuthorities())
            chain.doFilter(req, res)
        } catch (e: Exception) {
            log.warn("JWT validation error: {}", e.message, e)
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(JWTAuthorizationFilter::class.java)
    }
}

class JwtAuthenticationToken constructor(
    val user: ZmlpActor,
    permissions: List<GrantedAuthority>
) : AbstractAuthenticationToken(permissions) {

    override fun getCredentials(): Any {
        return user.keyId
    }

    override fun getPrincipal(): Any {
        return user
    }

    override fun isAuthenticated(): Boolean = false
}

/**
 * An AuthenticationProvider that specifically looks for JwtAuthenticationToken.  Pulls
 * the userId from the JwtAuthenticationToken and returns a UsernamePasswordAuthenticationToken.
 */
@Component
class JwtAuthenticationProvider : AuthenticationProvider {

    override fun authenticate(auth: Authentication): Authentication {
        val token = auth as JwtAuthenticationToken
        return UsernamePasswordAuthenticationToken(
            token.principal, token.credentials, token.authorities
        )
    }

    override fun supports(cls: Class<*>): Boolean {
        return cls == JwtAuthenticationToken::class.java
    }
}
