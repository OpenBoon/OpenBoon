package com.zorroa.archivist.security;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.domain.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * InternalAuthentication is for internal server threads.
 */
public class InternalAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    /**
     * The empty constructor is used for server startup.  The main thread needs to be authenticated to
     * do certain operations.  Since we don't know if the password is changed or even a user even exists
     * we just give it a fake admin user.
     */
    public InternalAuthentication() {
        super(ImmutableList.of());
        User user = new User();
        user.setUsername("admin");
        /**
         * Same as admin.
         */
        user.setId(1);
        this.principal = user;

    }
    public InternalAuthentication(User user) {
        super(ImmutableList.of());
        this.principal = user;

    }
    public InternalAuthentication(Object user, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = user;
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
