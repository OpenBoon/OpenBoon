package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.service.UserService;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by chambers on 1/21/16.
 */
public class ResetPasswordSecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordSecurityFilter.class);

    @Autowired
    UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        /**
         * At this point we have to extract the crypted data.
         */
        String token = servletRequest.getHeader("X-Archivist-Recovery-Token");
        if (token != null && servletRequest.getMethod().equals("POST")) {
            ResetPasswordRequest form = getBody(servletRequest);
            if (form != null && form.isValid()) {
                User user = userService.resetPassword(token, form.getPassword());
                if (user != null) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user.getUsername(), form.getPassword()));
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public static ResetPasswordRequest getBody(HttpServletRequest request) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
            return Json.deserialize(stringBuilder.toString(), ResetPasswordRequest.class);
        } catch (IOException ex) {
            logger.warn("Error reading request body stream, stream already closed, {}", ex.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignore) {
                    //ignore
                }
            }
        }

        return null;
    }

    public static class ResetPasswordRequest {
        private String password;

        public String getPassword() {
            return password;
        }

        public ResetPasswordRequest setPassword(String password) {
            this.password = password;
            return this;
        }

        public boolean isValid() {
            return (password!=null);
        }
    }
}
