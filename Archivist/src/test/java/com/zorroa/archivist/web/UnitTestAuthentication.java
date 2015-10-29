package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.security.auth.Subject;
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
        super(authorities);
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
