package com.zorroa.archivist.web;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.repository.UserDao;

public class ElasticAuthenticationProvider implements AuthenticationProvider {

	private static final Logger logger = LoggerFactory.getLogger(ElasticAuthenticationProvider.class);
			 
    @Autowired
    UserDao userDao;

    public ElasticAuthenticationProvider() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();
        String storedPassword;
        User user;

        try {
            storedPassword = userDao.getPassword(username);
            user = userDao.get(username);
        } catch (Exception e ){
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!BCrypt.checkpw(authentication.getCredentials().toString(), storedPassword)) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Set<SimpleGrantedAuthority> authorities =
                Sets.newHashSetWithExpectedSize(user.getRoles().size());
        user.getRoles().forEach(a->authorities.add(new SimpleGrantedAuthority(a)));

        logger.info("{}", authorities);
        
        return new UsernamePasswordAuthenticationToken(username, storedPassword, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }

}
