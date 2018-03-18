package com.zorroa.archivist.security

import com.google.common.collect.ImmutableSet
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.service.UserService
import com.zorroa.sdk.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ZorroaAuthenticationProvider : AuthenticationProvider {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var  userRegistryService: UserRegistryService

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {

        var username = authentication.name
        if (!userService.exists(username)) {
            throw BadCredentialsException("Invalid username or password: $username")
        }
        userService.checkPassword(username, authentication.credentials.toString())
        val authed = userRegistryService.getUser(username)

        logger.info("authd: {}", authed)
        return UsernamePasswordAuthenticationToken(authed, "", authed.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return SUPPORTED_AUTHENTICATION.contains(authentication)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ZorroaAuthenticationProvider::class.java)

        private val SUPPORTED_AUTHENTICATION = ImmutableSet.of<Class<*>>(UsernamePasswordAuthenticationToken::class.java)
    }
}

class ResetPasswordSecurityFilter : OncePerRequestFilter() {

    @Autowired
    internal var userService: UserService? = null

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, filterChain: FilterChain) {
        /**
         * At this point we have to extract the crypted data.
         */
        val token = servletRequest.getHeader("X-Archivist-Recovery-Token")
        if (token != null && servletRequest.method == "POST") {
            val form = getBody(servletRequest)
            if (form != null && form.isValid) {
                val user = userService!!.resetPassword(token, form.getPassword()!!)
                if (user != null) {
                    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(user.username, form.getPassword())
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }

    class ResetPasswordRequest {
        private var password: String? = null

        val isValid: Boolean
            get() = password != null

        fun getPassword(): String? {
            return password
        }

        fun setPassword(password: String): ResetPasswordRequest {
            this.password = password
            return this
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ResetPasswordSecurityFilter::class.java)

        @Throws(IOException::class)
        fun getBody(request: HttpServletRequest): ResetPasswordRequest? {

            val stringBuilder = StringBuilder()
            var bufferedReader: BufferedReader? = null

            try {
                val inputStream = request.inputStream
                if (inputStream != null) {
                    bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    val charBuffer = CharArray(4096)

                    while (true) {
                        val bytesRead = bufferedReader.read(charBuffer)
                        if (bytesRead < 0) break
                        stringBuilder.append(charBuffer, 0, bytesRead)
                    }
                } else {
                    stringBuilder.append("")
                }
                return Json.deserialize(stringBuilder.toString(), ResetPasswordRequest::class.java)
            } catch (ex: IOException) {
                logger.warn("Error reading request body stream, stream already closed, {}", ex.message)
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close()
                    } catch (ignore: IOException) {
                        //ignore
                    }

                }
            }

            return null
        }
    }
}
