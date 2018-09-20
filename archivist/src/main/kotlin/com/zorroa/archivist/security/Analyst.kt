package com.zorroa.archivist.security

import com.zorroa.archivist.config.ApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.stereotype.Component
import java.io.IOException
import javax.annotation.PostConstruct
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val ANALYST_HEADER_STRING = "X-Analyst-Port"

@Component
class AnalystAuthenticationFilter @Autowired constructor(authManager: AuthenticationManager) : BasicAuthenticationFilter(authManager) {

    @Autowired
    private lateinit var properties: ApplicationProperties

    private val ipRegexes = mutableListOf<Regex>()

    @PostConstruct
    fun init() {
        for (regex in properties.getList("archivist.security.analyst.ipRanges")) {
            ipRegexes.add(Regex(regex))
        }
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(req: HttpServletRequest,
                                  res: HttpServletResponse,
                                  chain: FilterChain) {

        val analystPort = req.getHeader(ANALYST_HEADER_STRING)
        if (analystPort != null) {
            val addr = req.remoteHost
            if (req.requestURI.startsWith("/cluster")) {
                for (r in ipRegexes) {
                    if (r.matches(addr)) {
                        val port = analystPort.toInt()
                        val endpoint = "https://${req.remoteAddr}:$port"
                        SecurityContextHolder.getContext().authentication = AnalystAuthentication(endpoint)
                        if (logger.isDebugEnabled) {
                            logger.debug("Worker $addr is allowed, matches ${r.pattern}")
                        }
                        break
                    }
                }
            }
        }

        chain.doFilter(req, res)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystAuthenticationFilter::class.java)
    }
}

class AnalystAuthentication(private val endpoint: String): Authentication {

    override fun getAuthorities(): Collection<out GrantedAuthority> {
        return listOf()
    }

    override fun setAuthenticated(p0: Boolean) { }

    override fun getName(): String {
        return endpoint
    }

    override fun getCredentials(): Any {
        return endpoint
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
