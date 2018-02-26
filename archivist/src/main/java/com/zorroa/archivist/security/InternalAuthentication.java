package com.zorroa.archivist.security;

import com.zorroa.security.UserAuthed;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * InternalAuthentication is for internal server threads.
 */
public class InternalAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    public InternalAuthentication(UserAuthed user) {
        super(user.getAuthorities());
        this.principal = user;
        this.setAuthenticated(true);
    }

    public InternalAuthentication(UserAuthed user, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = user;
        this.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

}

