package com.zorroa.archivist.security;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.sdk.domain.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Created by chambers on 1/21/16.
 */
public class InternalAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;
    private final Object principal;

    public InternalAuthentication() {
        super(ImmutableList.of());
        User user = new User();
        user.setUsername("internal");
        /**
         * Same as admin.
         */
        user.setId(1);
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
