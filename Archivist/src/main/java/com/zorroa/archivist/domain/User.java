package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;
import com.google.common.hash.HashCode;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;

    public User() { }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

}
