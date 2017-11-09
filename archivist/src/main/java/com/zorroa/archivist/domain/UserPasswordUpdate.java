package com.zorroa.archivist.domain;

public class UserPasswordUpdate {

    private String newPassword;

    private String oldPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public UserPasswordUpdate setNewPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public UserPasswordUpdate setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
        return this;
    }
}
