package com.zorroa.archivist.security;

import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.PermissionService;
import com.zorroa.archivist.service.UserService;
import com.zorroa.security.UserDetailsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by chambers on 7/13/17.
 */
public class UserDetailsPluginWrapper implements LdapAuthoritiesPopulator {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsPluginWrapper.class);

    @Autowired
    UserService userService;

    @Autowired
    PermissionService permissionService;

    UserDetailsPlugin plugin;

    public UserDetailsPluginWrapper(UserDetailsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations ctx, String username) {
        User user;
        try {

            user = userService.get(username);
        } catch (EmptyResultDataAccessException e) {

            String emailDomain = "@pool.zorroa.com";
            if (plugin != null) {
                emailDomain = "@" + plugin.getEmailDomain();
            }

            UserSpec spec = new UserSpec();
            spec.setUsername(username);
            spec.setFirstName(username);
            spec.setEmail(username + emailDomain);
            spec.setLastName(username);
            spec.setPassword(UUID.randomUUID().toString());
            user = userService.create(spec, "LDAP");
        }

        if (plugin != null) {
            importFromPlugin(user);
        }

        Collection<? extends GrantedAuthority> result = getNativeAuthorities(user);
        ctx.setAttributeValue("authorities", result);
        ctx.setAttributeValue("user", user);
        return result;
    }

    public Collection<? extends GrantedAuthority> getNativeAuthorities(User user) {
        return InternalPermission.upcast(userService.getPermissions(user));
    }

    public void importFromPlugin(User user) {
        try {
            List<String> groups = plugin.getGroups(user.getUsername());
            List<Permission> result = Lists.newArrayListWithExpectedSize(groups.size());
            for (String group : groups) {
                Permission perm;
                try {
                    perm = permissionService.getPermission(plugin.getGroupType() + Permission.JOIN + group);
                } catch (EmptyResultDataAccessException e) {
                    perm = permissionService.createPermission(new PermissionSpec()
                            .setType(plugin.getGroupType())
                            .setName(group)
                            .setDescription("Imported from plugin"));
                }
                result.add(perm);
            }

            userService.addPermissions(user, result);
        } catch (Exception e) {
            logger.warn("failed to transfer authorities ", e);
        }
    }
}
