package com.zorroa.archivist.web;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthenticationMocks {

    private AuthenticationMocks() {
    }

    public static Authentication userAuthentication(Integer id) {
        return new TestingAuthenticationToken("user", "user", "ROLE_USER");
    }

    public static Authentication adminAuthentication(Integer id) {
        return new TestingAuthenticationToken("admin", "admin", "ROLE_ADMIN");
    }
}