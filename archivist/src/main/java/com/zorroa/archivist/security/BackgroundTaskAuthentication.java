package com.zorroa.archivist.security;

import com.google.common.collect.Lists;
import com.zorroa.sdk.domain.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Created by chambers on 12/1/15.
 */
public class BackgroundTaskAuthentication extends AbstractAuthenticationToken {

    private User user;

    public BackgroundTaskAuthentication(User user) {
        super(Lists.newArrayList());
        this.user = user;
    }

    @Override
    public Object getCredentials() {
        return user.getUsername();
    }

    @Override
    public Object getPrincipal() {
        return user;
    }
}
