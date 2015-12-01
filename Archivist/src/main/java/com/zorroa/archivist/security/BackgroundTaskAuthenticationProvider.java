package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Created by chambers on 12/1/15.
 */
public class BackgroundTaskAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String username = authentication.getName();
        User user = (User) authentication.getPrincipal();
        return new RunAsUserToken(username,user, "",
                InternalPermission.upcast(userService.getPermissions(user)),
                null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(BackgroundTaskAuthentication.class);
    }
}
