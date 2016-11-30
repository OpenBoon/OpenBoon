package com.zorroa.archivist.security;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserAuthed;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by chambers on 11/29/16.
 */
public class UserDetailsPopulator implements LdapAuthoritiesPopulator, UserDetailsContextMapper {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsPopulator.class);

    @Autowired
    UserService userService;

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        UserAuthed user = loadUserByUsername(username, "LDAP");
        return InternalPermission.upcast(userService.getPermissions(user));
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations dirContextOperations, String username, Collection<? extends GrantedAuthority> collection) {
        UserDetails user = loadUserByUsername(username, "LDAP");
        return user;
    }

    @Override
    public void mapUserToContext(UserDetails userDetails, DirContextAdapter dirContextAdapter) {}

    private final UserAuthed loadUserByUsername(String username, String source) throws UsernameNotFoundException {
        if (!JdbcUtils.isValid(username)) {
            throw new IllegalArgumentException("Invalid null or empty username.");
        }
        if (!JdbcUtils.isValid(source)) {
            throw new IllegalArgumentException("Invalid authentication source.");
        }

        try {
            User user = userService.get(username);
            return new UserAuthed(user, InternalPermission.upcast(userService.getPermissions(user)));
        } catch (EmptyResultDataAccessException e) {
            UserSpec spec = new UserSpec();
            spec.setUsername(username);
            spec.setFirstName(username);
            spec.setEmail("foo@user.com");
            spec.setLastName(username);
            spec.setPassword(UUID.randomUUID().toString());
            try {
                User user = userService.create(spec, source);
                UserAuthed authed =
                        new UserAuthed(user, InternalPermission.upcast(userService.getPermissions(user)));
                return authed;
            } catch (Exception ex) {
                logger.warn("Failed to make authed user: {}", ex);
                return null;
            }
        }
    }
}
