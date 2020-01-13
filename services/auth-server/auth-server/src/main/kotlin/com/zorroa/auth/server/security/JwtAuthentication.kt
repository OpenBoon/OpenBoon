package com.zorroa.auth.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.zorroa.auth.client.Permission
import com.zorroa.auth.client.ZmlpActor
import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.repository.ApiKeyRepository
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
import java.io.IOException
import java.util.Date
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val TOKEN_PREFIX = "Bearer "
const val AUTH_HEADER = "Authorization"

class JWTAuthorizationFilter : OncePerRequestFilter() {

    @Autowired
    lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    lateinit var inceptionKey: ApiKey

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

            SecurityContextHolder.getContext().authentication =
                validateToken(token)

            chain.doFilter(req, res)
        } catch (e: Exception) {
            log.warn("JWT validation error: {}", e.message, e)
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
        }
    }

    fun validateToken(token: String): JwtAuthenticationToken {
        val jwt = JWT.decode(token)

        if (jwt.expiresAt == null) {
            throw RuntimeException("Token was missing an expiration date")
        }

        jwt.expiresAt?.let {
            val now = Date()
            if (it.time - now.time > MAX_EXPIRE_TIME_MILLIS) {
                throw RuntimeException("Expiration date too long")
            }

            if (now > it) {
                throw RuntimeException("Token was expired")
            }
        }

        val accessKey = jwt.claims.getValue("accessKey").asString()

        /**
         * Check to see if the key is the inception key.
         */
        val apiKey = (if (inceptionKey.accessKey == accessKey) {

            // The inception key is allowed to claim a projectId.
            val projectId = if (jwt.claims.containsKey("projectId")
            ) {
                UUID.fromString(jwt.claims.getValue("projectId").asString())
            } else {
                inceptionKey.projectId
            }

            ApiKey(
                inceptionKey.id,
                projectId,
                inceptionKey.accessKey,
                inceptionKey.secretKey,
                inceptionKey.name,
                INCEPTION_PERMISSIONS
            )
        } else {
            apiKeyRepository.findByAccessKey(accessKey)
        })
            ?: throw RuntimeException("Invalid JWT token")

        val alg = Algorithm.HMAC512(apiKey.secretKey)
        alg.verify(jwt)

        return JwtAuthenticationToken(
            apiKey.getZmlpActor(),
            apiKey.getGrantedAuthorities()
        )
    }

    companion object {
        /**
         * The inception permissions is everything except ability to
         * decrypt encrypted project data.
         */
        val INCEPTION_PERMISSIONS = Permission.values()
            .map { it.name }.toSet().minus(Permission.SystemProjectDecrypt.name)

        /**
         * Maximum TTL for a JWT token.
         */
        const val MAX_EXPIRE_TIME_MILLIS = 3600 * 1000L

        private val log = LoggerFactory.getLogger(JWTAuthorizationFilter::class.java)
    }
}

class JwtAuthenticationToken constructor(
    val user: ZmlpActor,
    permissions: List<GrantedAuthority>
) : AbstractAuthenticationToken(permissions) {

    override fun getCredentials(): Any {
        return user.id
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
