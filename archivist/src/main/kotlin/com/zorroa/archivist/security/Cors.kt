package com.zorroa.archivist.security

import org.slf4j.LoggerFactory
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CorsCredentialsFilter : GenericFilterBean() {

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {

        val req = servletRequest as HttpServletRequest
        val rsp = servletResponse as HttpServletResponse

        // UNCOMMENT TO DEBUG THIS SHIT
        /*
        Enumeration<String> e = req.getHeaderNames();
        logger.info("{}------------------------------", req.getMethod());
        while (e.hasMoreElements()) {
            String name =e.nextElement();
            logger.info("{}={}", name, req.getHeader(name));
        }
        logger.info("-------------------------------");
        */

        val auth = req.getHeader("authorization")
        if (auth != null) {
            rsp.addHeader("Access-Control-Allow-Credentials", "true")
            rsp.addHeader("Access-Control-Allow-Origin",
                    req.getHeader("origin") ?: "http://localhost:8066")
        } else {
            rsp.addHeader("Access-Control-Expose-Headers", "Content-Encoding, content-range, content-length, accept-ranges")
            rsp.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT, DELETE")
            rsp.addHeader("Access-Control-Allow-Headers", "authorization, content-type, x-requested-with, X-Archivist-Recovery-Token")
            rsp.addHeader("Access-Control-Allow-Credentials", "true")
            var origin: String? = req.getHeader("origin")
            if (origin == null) {
                origin = "http://localhost:8066"
            }
            rsp.addHeader("Access-Control-Allow-Origin", origin)
            if (req.method == "OPTIONS") {
                rsp.addHeader("Access-Control-Max-Age", "3600")
            }
        }

        filterChain.doFilter(servletRequest, servletResponse)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(CorsCredentialsFilter::class.java)
    }
}
