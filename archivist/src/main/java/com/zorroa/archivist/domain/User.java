package com.zorroa.archivist.domain;

import java.io.Serializable;

public class User extends UserBase implements Loggable<Integer>, Serializable {

    private Boolean enabled;
    private UserSettings settings;

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
