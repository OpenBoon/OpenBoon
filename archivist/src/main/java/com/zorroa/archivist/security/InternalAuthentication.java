package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collection;

/**
 * InternalAuthentication is for internal server threads.
 */
public class InternalAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    public InternalAuthentication(User user, Collection<Permission> authorities) {
        super(InternalPermission.upcast(authorities));
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

