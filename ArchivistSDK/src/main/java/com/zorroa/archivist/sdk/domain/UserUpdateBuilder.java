package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 7/13/15.
 */
public class UserUpdateBuilder {

    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private Integer[] permissionIds;

    private Boolean enabled;

    public String getUsername() {
        return username;
    }

    public UserUpdateBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserUpdateBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UserUpdateBuilder setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserUpdateBuilder setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserUpdateBuilder setEmail(String email) {
        this.email = email;
        return this;
    }

    public Integer[] getPermissionIds() {
        return permissionIds;
    }

    public UserUpdateBuilder setPermissionIds(Integer[] permissionIds) {
        this.permissionIds = permissionIds;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public UserUpdateBuilder setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
