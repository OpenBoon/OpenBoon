package com.zorroa.archivist.security;

import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.service.UserService;
import com.zorroa.security.UserAuthed;
import com.zorroa.security.UserRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Set;

public class ZorroaAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(ZorroaAuthenticationProvider.class);

    @Autowired
    UserService userService;

    @Autowired
    UserRegistryService userRegistryService;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();
        if (!userService.exists(username)) {
            throw new BadCredentialsException("Invalid username or password");
        }
        userService.checkPassword(username, authentication.getCredentials().toString());
        UserAuthed authed = userRegistryService.getUser(username);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(authed, "", authed.getAuthorities());
        return auth;
    }

    private static final Set<Class<?>> SUPPORTED_AUTHENTICATION =
            ImmutableSet.of(UsernamePasswordAuthenticationToken.class);

    @Override
    public boolean supports(Class<?> authentication) {
        return SUPPORTED_AUTHENTICATION.contains(authentication);
    }
}
