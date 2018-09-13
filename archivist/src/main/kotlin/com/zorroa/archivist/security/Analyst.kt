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

        val addr = req.remoteAddr
        if (logger.isDebugEnabled) {
            logger.debug("Worker request from $addr")
        }

        if (req.requestURI.startsWith("/cluster")) {
            for (r in ipRegexes) {
                if (r.matches(addr)) {
                    SecurityContextHolder.getContext().authentication = WorkerAuthentication(req.remoteAddr)
                    if (logger.isDebugEnabled) {
                        logger.debug("Worker $addr is allowed, matches ${r.pattern}")
                    }
                    break
                }
            }
        }
        chain.doFilter(req, res)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystAuthenticationFilter::class.java)
    }
}

class WorkerAuthentication(private val name: String): Authentication {


    override fun getAuthorities(): Collection<out GrantedAuthority> {
        return listOf()
    }

    override fun setAuthenticated(p0: Boolean) { }

    override fun getName(): String {
        return name
    }

    override fun getCredentials(): Any {
        return name
    }

    override fun getPrincipal(): Any {
        return name
    }

    override fun isAuthenticated(): Boolean {
        return true
    }

    override fun getDetails(): Any {
        return name
    }

}
