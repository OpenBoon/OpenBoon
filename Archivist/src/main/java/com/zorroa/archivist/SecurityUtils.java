package com.zorroa.archivist;

import com.zorroa.archivist.domain.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.context.request.RequestContextHolder;

public class SecurityUtils {

    public static String createPasswordHash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static String getSessionId() {
        return RequestContextHolder.currentRequestAttributes().getSessionId();
    }

    public static String getUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return "admin";
        }
        else {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return user.getUsername();
        }
    }
}
