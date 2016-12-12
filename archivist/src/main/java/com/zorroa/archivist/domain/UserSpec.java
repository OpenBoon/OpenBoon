package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

public class UserSpec {

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;
    private String firstName;
    private String lastName;

    @NotEmpty
    @Email
    private String email;

    private Integer[] permissionIds;
    private Integer userPresetId;

    @JsonIgnore
    private int homeFolderId;
    @JsonIgnore
    private int userPermissionId;

    public UserSpec() {
    }

    public String getPassword() {
        return password;
    }

    public UserSpec setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UserSpec setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserSpec setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserSpec setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public UserSpec setUsername(String username) {
        this.username = username;
        return this;
    }

    public Integer[] getPermissionIds() {
        return permissionIds;
    }

    public UserSpec setPermissionIds(Integer[] permissions) {
        this.permissionIds = permissions;
        return this;
    }

    public UserSpec setPermissions(Permission... permissions) {
        this.permissionIds = new Integer[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            this.permissionIds[i] = permissions[i].getId();
        }
        return this;
    }

    public Integer getUserPresetId() {
        return userPresetId;
    }

    public UserSpec setUserPresetId(Integer userPresetId) {
        this.userPresetId = userPresetId;
        return this;
    }

    public int getHomeFolderId() {
        return homeFolderId;
    }

    public UserSpec setHomeFolderId(int homeFolderId) {
        this.homeFolderId = homeFolderId;
        return this;
    }

    public int getUserPermissionId() {
        return userPermissionId;
    }

    public UserSpec setUserPermissionId(int userPermissionId) {
        this.userPermissionId = userPermissionId;
        return this;
    }
}
