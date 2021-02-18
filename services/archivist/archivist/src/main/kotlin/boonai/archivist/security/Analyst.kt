package boonai.archivist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.Date
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AnalystAuthenticationFilter(val analystTokenValidator: AnalystTokenValidator) : OncePerRequestFilter() {

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
        }

        if (token == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
        } else {
            try {
                val auth = analystTokenValidator.validateJwtToken(token, req.remoteAddr)
                SecurityContextHolder.getContext().authentication = auth
                chain.doFilter(req, res)
            } catch (e: JWTVerificationException) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystAuthenticationFilter::class.java)
    }
}

@Component
class AnalystTokenValidator() {

    @Value("\${archivist.security.analyst.prefer-hostnames}")
    var preferHostnames: Boolean = true

    @Value("\${analyst.shared-key}")
    lateinit var sharedKey: String

    /**
     * Validate the JWT token, if any and return a [AnalystAuthentication]
     * instance.
     *
     * @throws JWTVerificationException
     */
    fun validateJwtToken(token: String, remoteAddr: String): AnalystAuthentication {
        val jwt = JWT.decode(token)
        if (jwt.expiresAt == null || Date() > jwt.expiresAt) {
            throw JWTVerificationException("Not Authorized")
        }

        val alg = Algorithm.HMAC256(sharedKey)
        alg.verify(jwt)

        val analystPort = jwt.getClaim("port").asInt()
        val analystHost = jwt.getClaim("host").asString()
        val version = jwt.getClaim("version").asString()

        if (analystPort == null || analystHost == null || version == null) {
            logger.warn("Analyst request from $remoteAddr rejected, missing host, port or version")
            throw JWTVerificationException("Not Authorized, invalid claims")
        }

        val endpoint = if (preferHostnames && analystHost != null) {
            "http://$analystHost:$analystPort"
        } else {
            "http://$remoteAddr:$analystPort"
        }
        return AnalystAuthentication(endpoint, version)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystTokenValidator::class.java)
    }
}

class AnalystAuthentication(val endpoint: String, val version: String) : Authentication {

    override fun getAuthorities(): Collection<out GrantedAuthority> {
        return setOf(SimpleGrantedAuthority("ANALYST"))
    }

    override fun setAuthenticated(p0: Boolean) {}

    override fun getName(): String {
        return endpoint
    }

    override fun getCredentials(): Any {
        return version
    }

    override fun getPrincipal(): Any {
        return endpoint
    }

    override fun isAuthenticated(): Boolean {
        return true
    }

    override fun getDetails(): Any {
        return endpoint
    }
}
