package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.google.common.hash.HashCode;


import java.io.Serializable;

public class User implements Loggable<Integer>, Serializable {

    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
    private UserSettings settings;

    public int getId() {
        return id;
    }

    public User setId(int id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public User setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public User setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
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
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (!(obj instanceof User)) {
            return false;
        }
        User other = (User) obj;
        return other.getId() == getId();
    }

    @Override
    public int hashCode() {
        return HashCode.fromInt(getId()).hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("username", getUsername())
                .toString();
    }

    @Override
    public Integer getTargetId() {
        return id;
    }
}
