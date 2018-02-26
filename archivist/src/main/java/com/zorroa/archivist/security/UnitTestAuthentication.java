package com.zorroa.archivist.security;

import com.zorroa.security.UserAuthed;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Created by chambers on 10/29/15.
 */
public class UnitTestAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;
    private final Object principal;
    private final String username;

    public UnitTestAuthentication(UserAuthed principal,
                                  Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.username = principal.getUsername();
    }

    @Override
    public Object getCredentials() {
        return "";
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
