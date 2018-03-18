package com.zorroa.archivist.security

import com.google.common.collect.ImmutableList
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.service.UserService
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class HmacAuthentication : AbstractAuthenticationToken {

    private val username: String
    private val data: String?
    private val hmac: String?

    constructor(username: String, data: String?, hmac: String?) : super(ImmutableList.of<GrantedAuthority>()) {
        this.username = username
        this.data = data
        this.hmac = hmac
    }

    override fun getDetails(): Any? {
        return data
    }

    override fun getCredentials(): Any? {
        return hmac
    }

    override fun getPrincipal(): Any {
        return username
    }

    override fun isAuthenticated(): Boolean {
        return false
    }
}

class HmacAuthenticationProvider(private val trustMode: Boolean) : AuthenticationProvider {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRegistryService: UserRegistryService

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.principal as String
        val msgClear = authentication.details as String?
        val msgCrypt = authentication.credentials as String?

        return if (trustMode) {
            try {
                val authed = userRegistryService.getUser(username)
                UsernamePasswordAuthenticationToken(authed, "",
                        authed.authorities)

            } catch (e: Exception) {
                logger.warn("password authentication failed for user: {}", username, e)
                throw BadCredentialsException("Invalid username or password")
            }

        } else if (msgClear != null && msgCrypt != null) {
            try {
                val mac = Mac.getInstance("HmacSHA1")
                mac.init(SecretKeySpec(getKey(username).toByteArray(), "HmacSHA1"))
                val crypted = Hex.encodeHexString(mac.doFinal(msgClear.toByteArray()))

                if (crypted == msgCrypt) {
                    val authed = userRegistryService.getUser(username)
                    UsernamePasswordAuthenticationToken(authed, "",
                            authed.authorities)
                } else {
                    logger.warn("password authentication failed for user: {}", username)
                    throw BadCredentialsException("Invalid username or password")
                }


            } catch (e: Exception) {
                logger.warn("password authentication failed for user: {}", username, e)
                throw BadCredentialsException("Invalid username or password")
            }
        }
        else {
            throw BadCredentialsException("Invalid username or password")
        }
    }

    @Throws(IOException::class)
    fun getKey(user: String): String {
        return userService.getHmacKey(user)
    }

    override fun supports(aClass: Class<*>): Boolean {
        return aClass == HmacAuthentication::class.java
    }

    companion object {

        private val logger = LoggerFactory.getLogger(HmacAuthenticationProvider::class.java)
    }
}

class HmacSecurityFilter(private val enabled: Boolean) : GenericFilterBean() {


    private//logger.info("Existing auth is null or not authed");
    val isAuthenticationRequired: Boolean
        get() {
            val existingAuth = SecurityContextHolder.getContext().authentication
            return if (existingAuth == null || !existingAuth.isAuthenticated) {
                true
            } else false

        }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        /**
         * At this point we have to extract the crypted data.
         */
        if (enabled && isAuthenticationRequired) {
            val req = servletRequest as HttpServletRequest
            if (req.getHeader("X-Archivist-User") != null) {
                val token = HmacAuthentication(
                        req.getHeader("X-Archivist-User"),
                        req.getHeader("X-Archivist-Data"),
                        req.getHeader("X-Archivist-Hmac"))
                SecurityContextHolder.getContext().authentication = token
            }
        }

        filterChain.doFilter(servletRequest, servletResponse)
    }
}
