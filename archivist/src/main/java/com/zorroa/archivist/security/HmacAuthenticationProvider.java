package com.zorroa.archivist.security;

import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserAuthed;
import com.zorroa.archivist.service.UserService;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;

/**
 * Created by chambers on 1/21/16.
 */
public class HmacAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(HmacAuthenticationProvider.class);

    @Autowired
    UserService userService;

    private final boolean trustMode;

    public HmacAuthenticationProvider(boolean trustMode) {
        this.trustMode = trustMode;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = (String) authentication.getPrincipal();
        String msgClear = (String) authentication.getDetails();
        String msgCrypt = (String) authentication.getCredentials();

        if (trustMode) {
            try {
                User user = userService.get(username);
                return new UsernamePasswordAuthenticationToken(new UserAuthed(user), "",
                        InternalPermission.upcast(userService.getPermissions(user)));

            } catch (Exception e) {
                logger.warn("password authentication failed for user: {}", username, e);
                throw new BadCredentialsException("Invalid username or password");
            }
        }
        else {
            try {
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(new SecretKeySpec(getKey(username).getBytes(), "HmacSHA1"));
                String crypted = Hex.encodeHexString(mac.doFinal(msgClear.getBytes()));

                if (crypted.equals(msgCrypt)) {
                    User user = userService.get(username);
                    return new UsernamePasswordAuthenticationToken(new UserAuthed(user), "",
                            InternalPermission.upcast(userService.getPermissions(user)));
                } else {
                    logger.warn("password authentication failed for user: {}", username);
                    throw new BadCredentialsException("Invalid username or password");
                }


            } catch (Exception e) {
                logger.warn("password authentication failed for user: {}", username, e);
                throw new BadCredentialsException("Invalid username or password");
            }
        }
    }

    public String getKey(String user) throws IOException {
        String key =  userService.getHmacKey(user);
        return key;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(HmacAuthentication.class);
    }
}
