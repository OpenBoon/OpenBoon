package com.zorroa.archivist.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by chambers on 1/21/16.
 */
public class HmacSecurityFilter extends GenericFilterBean {

    private final boolean enabled;

    public HmacSecurityFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        /**
         * At this point we have to extract the crypted data.
         */
        if (enabled && isAuthenticationRequired()) {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            if (req.getHeader("X-Archivist-User") != null) {
                HmacAuthentication token = new HmacAuthentication(
                        req.getHeader("X-Archivist-User"),
                        req.getHeader("X-Archivist-Data"),
                        req.getHeader("X-Archivist-Hmac"));
                SecurityContextHolder.getContext().setAuthentication(token);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }


    private boolean isAuthenticationRequired() {
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if ((existingAuth == null) || !existingAuth.isAuthenticated()) {
            //logger.info("Existing auth is null or not authed");
            return true;
        }

        return false;
    }
}
