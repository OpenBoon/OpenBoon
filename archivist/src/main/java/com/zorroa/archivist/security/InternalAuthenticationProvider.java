package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.service.UserService;
import com.zorroa.sdk.domain.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

/**
 * Created by chambers on 1/21/16.
 */
public class InternalAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        List<Permission> perms = userService.getPermissions((User) authentication.getPrincipal());
        perms.add(userService.getPermission("internal::server"));
        return new InternalAuthentication(authentication.getPrincipal(), InternalPermission.upcast(perms));
    }
    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(InternalAuthentication.class);
    }
}
