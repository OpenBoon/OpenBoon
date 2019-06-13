package com.zorroa.archivist.security

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.JwtSecurityConstants.HEADER_STRING
import com.zorroa.archivist.security.JwtSecurityConstants.ORGID_HEADER
import com.zorroa.archivist.security.JwtSecurityConstants.TOKEN_PREFIX
import com.zorroa.archivist.service.PermissionService
import com.zorroa.archivist.service.event
import com.zorroa.security.Groups
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(authManager: AuthenticationManager) :
    BasicAuthenticationFilter(authManager) {

    @Autowired
    private lateinit var validator: JwtValidator

    @Autowired
    private lateinit var permissionService: PermissionService

    @Autowired
    private lateinit var properties: ApplicationProperties

    @Autowired
    private lateinit var userRegistryService: UserRegistryService

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {

        val token = req.getHeader(HEADER_STRING)

        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(req, res)
            return
        }
        /**
         * Validate the JWT claims. Assuming they are valid, create a JwtAuthenticationToken which
         * will be converted into a proper UsernamePasswordAuthenticationToken by the
         * JwtAuthenticationProvider
         *
         * Not doing this 2 step process means the actuator endpoints can't be authed by a token.
         */
        val claims = validator.validate(token.replace(TOKEN_PREFIX, "")).toMutableMap()

        // TODO move this IRM specific code to IRM only location
        if (claims.containsKey("insightUser")) {
            claims["userId"]?.let {
                val user = userRegistryService.getUser(it)
                claims["userId"] = user.id.toString()
            }
        }

        val mapping =
            properties.parseToMap("archivist.security.permissions.map")

        val authorities = claims["permissions"]?.let { str ->
            str.split(",")
                .mapNotNull { token ->
                    mapping[token.trim()]?.let {
                        permissionService.getPermission(
                            it
                        )
                    }
                }
        }

        SecurityContextHolder.getContext().authentication =
            JwtAuthenticationToken(claims, req.getHeader(ORGID_HEADER), authorities ?: emptyList())

        req.setAttribute("authType", HttpServletRequest.CLIENT_CERT_AUTH)
        chain.doFilter(req, res)
    }
}

/**
 * JwtAuthenticationToken stores the validated claims
 */
class JwtAuthenticationToken constructor(
    claims: Map<String, String>,
    val organizationId: String? = null,
    authorities: Collection<GrantedAuthority> = emptyList()
) : AbstractAuthenticationToken(authorities) {

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
class JwtAuthenticationProvider : AuthenticationProvider {

    @Autowired
    private lateinit var userRegistryService: UserRegistryService

    override fun authenticate(auth: Authentication): Authentication {
        val token = auth as JwtAuthenticationToken

        val user = if (token.authorities.isEmpty()) {
            userRegistryService.getUser(UUID.fromString(token.userId))
        } else {
            UserAuthed(
                UUID.fromString(token.userId),
                UUID.fromString(token.organizationId),
                token.name,
                HashSet(token.authorities),
                emptyMap()
            )
        }

        if (token.organizationId != null) {
            if (user.authorities.map { it.authority == Groups.SUPERADMIN }.isNotEmpty()) {
                user.organizationId = UUID.fromString(token.organizationId)
                logger.event(
                    LogObject.USER, LogAction.ORGSWAP,
                    mapOf(
                        "username" to user.username,
                        "newOrganizationId" to token.organizationId
                    )
                )
            }
        }

        return UsernamePasswordAuthenticationToken(user, "", user.authorities)
    }

    override fun supports(cls: Class<*>): Boolean {
        return cls == JwtAuthenticationToken::class.java
    }

    companion object {

        private val logger = LoggerFactory.getLogger(JwtAuthenticationProvider::class.java)
    }
}
