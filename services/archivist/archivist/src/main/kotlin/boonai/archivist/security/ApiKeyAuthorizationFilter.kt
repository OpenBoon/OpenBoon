package boonai.archivist.security

import boonai.archivist.service.ProjectService
import boonai.common.apikey.AuthServerClient
import boonai.common.apikey.Permission
import boonai.common.apikey.ZmlpActor
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

val HEADER = "Authorization"
val PREFIX = "Bearer "

class ApiKeyAuthorizationFilter constructor(
    val authServerClient: AuthServerClient,
    val projectService: ProjectService
) : OncePerRequestFilter() {

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {
        val token = req.getHeader(HEADER)?.let {
            if (it.startsWith(PREFIX)) {
                it.removePrefix(PREFIX)
            } else {
                null
            }
        } ?: req.getParameter("token")

        if (token != null) {
            try {
                validate(token, getProjectIdOverride(req))
            } catch (e: Exception) {
                log.warn("Invalid authentication token: ", e)
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
                return
            }
        }

        chain.doFilter(req, res)
    }

    fun validate(token: String, projectId: UUID? = null) {
        val actor = authServerClient.authenticate(token, projectId)
        if (!actor.hasAnyPermission(Permission.SystemServiceKey)) {
            if (!projectService.isEnabled(actor.projectId)) {
                throw RuntimeException("Project does not exist")
            }
        }

        SecurityContextHolder.getContext().authentication = ApiTokenAuthentication(actor)
    }

    /**
     * Get a project Id param or header and pass it on to auth server.
     * Only the inception key can override a project.
     */
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

    companion object {
        private val log = LoggerFactory.getLogger(ApiKeyAuthorizationFilter::class.java)
    }
}

class ApiTokenAuthentication constructor(
    val zmlpActor: ZmlpActor
) : AbstractAuthenticationToken(listOf()) {

    override fun getCredentials(): Any {
        return zmlpActor.projectId
    }

    override fun getPrincipal(): Any {
        return zmlpActor.projectId
    }

    override fun isAuthenticated(): Boolean = false
}

class ApiKeyAuthenticationProvider : AuthenticationProvider {

    override fun authenticate(auth: Authentication): Authentication {
        val token = auth as ApiTokenAuthentication
        // extension function.
        return token.zmlpActor.getAuthentication()
    }

    override fun supports(cls: Class<*>): Boolean {
        return cls == ApiTokenAuthentication::class.java
    }
}
