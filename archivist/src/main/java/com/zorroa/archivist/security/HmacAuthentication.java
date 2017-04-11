package com.zorroa.archivist.security;

import com.google.common.collect.ImmutableList;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Created by chambers on 1/21/16.
 */
public class HmacAuthentication extends AbstractAuthenticationToken  {

    private String username;
    private String data;
    private String hmac;

    public HmacAuthentication() {
        super(ImmutableList.of());
    }

    public HmacAuthentication(String username, String data, String hmac) {
        super(ImmutableList.of());
        this.username = username;
        this.data = data;
        this.hmac = hmac;
    }

    @Override
    public Object getDetails() {
        return data;
    }

    @Override
    public Object getCredentials() {
        return hmac;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }
}
