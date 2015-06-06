package com.zorroa.archivist;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class SecurityUtils {

    public static String createPasswordHash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

}
