package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.domain.User;
import com.zorroa.archivist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

/**
 * BackgroundTaskAuthenticationProvider takes whatever permissions a user has and adds
 * the internal::server permission, which gives the thread the ability to write into
 * special areas.
 */
public class BackgroundTaskAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        List<Permission> perms = userService.getPermissions((User) authentication.getPrincipal());
        perms.add(userService.getPermission("internal::server"));
        return new InternalAuthentication(authentication.getPrincipal(), InternalPermission.upcast(perms));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(BackgroundTaskAuthentication.class);
    }
}
