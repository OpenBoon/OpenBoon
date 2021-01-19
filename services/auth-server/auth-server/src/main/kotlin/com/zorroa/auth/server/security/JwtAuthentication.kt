package com.zorroa.auth.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.domain.ValidationKey
import com.zorroa.auth.server.repository.ApiKeyCustomRepository
import com.zorroa.zmlp.apikey.AuthServerClient
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.apikey.ZmlpActor
import com.zorroa.zmlp.service.security.EncryptionService
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
    lateinit var apiKeyCustomRepository: ApiKeyCustomRepository

    @Autowired
    lateinit var inceptionKey: ValidationKey

    @Autowired
    lateinit var encryptionService: EncryptionService

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
                validateToken(token, getProjectIdOverride(req))

            chain.doFilter(req, res)
        } catch (e: Exception) {
            log.warn("JWT validation error: {}", e.message, e)
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
        }
    }

    fun getProjectIdOverride(req: HttpServletRequest): UUID? {
        val projectIdHeader = req.getHeader(AuthServerClient.PROJECT_ID_HEADER)
        val projectIdParam = req.getParameter(AuthServerClient.PROJECT_ID_PARAM)

        return when {
            projectIdHeader != null -> {
                UUID.fromString(projectIdHeader)
            }
            projectIdParam != null -> {
                UUID.fromString(projectIdParam)
            }
            else -> {
                null
            }
        }
    }

    fun validateToken(token: String, projectIdOverride: UUID? = null): JwtAuthenticationToken {
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
        val isInceptionKey = inceptionKey.accessKey == accessKey
        val apiKey = if (isInceptionKey) {
            ValidationKey(
                inceptionKey.id,
                inceptionKey.projectId,
                inceptionKey.accessKey,
                inceptionKey.secretKey,
                inceptionKey.name,
                INCEPTION_PERMISSIONS
            )
        } else {
            apiKeyCustomRepository.getValidationKey(accessKey)
        }

        /**
         * Decrypt the secret key if needed.
         */
        val secretKey = if (isInceptionKey) {
            inceptionKey.secretKey
        } else {
            encryptionService.decryptString(apiKey.projectId, apiKey.secretKey, ApiKey.CRYPT_VARIANCE)
        }

        /**
         * Validate the signage
         */
        val alg = Algorithm.HMAC512(secretKey)
        alg.verify(jwt)

        /**
         * If the key is running in our env then we accept some allowed attrs.
         */
        val attrs = if (apiKey.permissions.contains(Permission.SystemProjectDecrypt.name)) {
            jwt.claims?.filter {
                it.key in ALLOWED_ATTRS
            }?.map {
                it.key to it.value.asString()
            }?.toMap() ?: mapOf<String, String>()
        } else {
            mapOf()
        }

        /**
         * Allow SystemProjectOverride keys to override the project.
         */
        val actor = if (apiKey.permissions.contains(Permission.SystemProjectOverride.name)) {
            /**
             * Check for a project ID claim, otherwise a project ID header, and then
             * fallback on the key's project Id.
             */
            val projectId = if (jwt.claims.containsKey("projectId")) {
                UUID.fromString(jwt.claims.getValue("projectId").asString())
            } else {
                projectIdOverride ?: apiKey.projectId
            }
            apiKey.getZmlpActor(projectId, attrs)
        } else {
            apiKey.getZmlpActor(null, attrs)
        }

        return JwtAuthenticationToken(actor, apiKey.getGrantedAuthorities())
    }

    companion object {

        val ALLOWED_ATTRS = setOf("taskId", "jobId")

        /**
         * The inception permissions is everything except ability to
         * decrypt encrypted project data.
         */
        val INCEPTION_PERMISSIONS = Permission.values()
            .map { it.name }
            .minus(Permission.SystemProjectDecrypt.name)
            .toSet()
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
