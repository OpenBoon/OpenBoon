package com.zorroa.archivist.domain;

import java.io.Serializable;

public class User extends UserBase implements Loggable<Integer>, Serializable {

    private Boolean enabled;
    private UserSettings settings;

    public User() { }

    public User(User user) {
        this.setFirstName(user.getFirstName());
        this.setLastName(user.getLastName());
        this.setSettings(user.getSettings());
        this.setEmail(user.getEmail());
        this.setEnabled(user.getEnabled());
        this.setId(user.getId());
        this.setUsername(user.getUsername());
        this.setSettings(user.getSettings());
        this.setPermissionId(user.getPermissionId());
        this.setHomeFolderId(user.getHomeFolderId());
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public User setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public UserSettings getSettings() {
        return settings;
    }

    public User setSettings(UserSettings settings) {
        this.settings = settings;
        return this;
    }

    @Override
    public Integer getTargetId() {
        return getId();
    }
}
