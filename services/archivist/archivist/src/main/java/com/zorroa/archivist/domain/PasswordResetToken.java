package com.zorroa.archivist.domain;

/**
 * Created by chambers on 4/17/17.
 */
public class PasswordResetToken {

    private String token;
    private boolean emailSent;

    public PasswordResetToken(String token) {
        this.token = token;
        this.emailSent = false;
    }

    public String getToken() {
        return token;
    }

    public PasswordResetToken setToken(String token) {
        this.token = token;
        return this;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public PasswordResetToken setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
        return this;
    }

    public String toString() {
        return token;
    }
}
