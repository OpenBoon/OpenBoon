package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserAuthed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;

/**
 * Created by chambers on 11/29/16.
 */
public class UserDetailsPopulator implements UserDetailsContextMapper {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsPopulator.class);

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> collection) {
        return new UserAuthed((User) ctx.getObjectAttribute("user"),
                (Collection<? extends GrantedAuthority>) ctx.getObjectAttribute("authorities"));
    }

    @Override
    public void mapUserToContext(UserDetails userDetails, DirContextAdapter dirContextAdapter) {}
}
