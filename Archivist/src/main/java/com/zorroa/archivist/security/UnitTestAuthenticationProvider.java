package com.zorroa.archivist.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Created by chambers on 1/26/16.
 */
public class UnitTestAuthenticationProvider implements AuthenticationProvider {
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return authentication;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(UnitTestAuthentication.class);
    }
}
