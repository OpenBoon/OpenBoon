package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

/**
 * Created by chambers on 10/29/15.
 */
public class UnitTestAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;
    private final Object credentials;
    private final Object principal;
    private final String username;

    public UnitTestAuthentication(User principal, Object credentials, List<Permission> authorities) {
        super(InternalPermission.upcast(authorities));
        this.principal = principal;
        this.credentials = credentials;
        this.username = principal.getUsername();
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return username;
    }
}
